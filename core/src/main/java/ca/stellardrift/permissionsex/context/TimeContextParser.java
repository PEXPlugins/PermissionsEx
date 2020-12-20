/*
 * PermissionsEx
 * Copyright (C) zml and PermissionsEx contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ca.stellardrift.permissionsex.context;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parsers to resolve a time from user input.
 */
public interface TimeContextParser {

    /**
     * Get a list of parse
     * @param zone the time zone to parse in
     * @return parser candidates
     */
    static TimeContextParser[] parsersForZone(final ZoneId zone) {
        return new TimeContextParser[] {
                new ByDateTimeFormatter(DateTimeFormatter.ISO_DATE_TIME.withZone(zone)),
                new ByDateTimeFormatter(DateTimeFormatter.ISO_TIME.withZone(zone)),
                new ByDateTimeFormatter(DateTimeFormatter.ISO_DATE.withZone(zone)),
                new ByDateTimeFormatter(DateTimeFormatter.RFC_1123_DATE_TIME.withZone(zone)),
                new Relative(zone),
                new ByEpochTime(zone)
        };
    }

    @Nullable ZonedDateTime parse(final String input);

    /**
     * Attemp to parse using an existing {@link DateTimeFormatter}.
     */
    final class ByDateTimeFormatter implements TimeContextParser {
        private final DateTimeFormatter formatter;

        public ByDateTimeFormatter(final DateTimeFormatter formatter) {
            this.formatter = formatter;
        }

        @Override
        public @Nullable ZonedDateTime parse(final String input) {
            try {
                return ZonedDateTime.parse(input, this.formatter);
            } catch (final DateTimeParseException ex) {
                return null;
            }
        }
    }

    /**
     * Given a second since the epoch, create a time in the local time zone.
     */
    final class ByEpochTime implements TimeContextParser {
        private final ZoneId zone;

        public ByEpochTime(final ZoneId zone) {
            this.zone = zone;
        }

        @Override
        public @Nullable ZonedDateTime parse(final String input) {
            try {
                return ZonedDateTime.ofInstant(Instant.ofEpochSecond(Long.parseLong(input)), this.zone);
            } catch (final NumberFormatException ex) {
                return null;
            }
        }
    }

    /**
     * Parse a time using relative time syntax.
     */
    final class Relative implements TimeContextParser {
        private static final Pattern RELATIVE_TIME_PART = Pattern.compile("^(((?:\\+|-)?)[0-9\\.]*[0-9])(seconds?|minutes?|hours?|days?|weeks?|months?|years?|s|m|h|d|w|y)");
        private final ZoneId zone;

        public Relative(final ZoneId zone) {
            this.zone = zone;
        }

        private static ChronoUnit unit(final String spec) {
            switch (spec) {
                case "second":
                case "seconds":
                case "s":
                    return ChronoUnit.SECONDS;
                case "minute":
                case "minutes":
                case "m":
                    return ChronoUnit.MINUTES;
                case "hour":
                case "hours":
                case "h":
                    return ChronoUnit.HOURS;
                case "day":
                case "days":
                case "d":
                    return ChronoUnit.DAYS;
                case "week":
                case "weeks":
                case "w":
                    return ChronoUnit.WEEKS;
                case "month":
                case "months":
                    return ChronoUnit.MONTHS;
                case "year":
                case "years":
                case "y":
                    return ChronoUnit.YEARS;
                // This shouldn't be reached -- it can't be matched in the regex
                default: throw new IllegalStateException("Hit datetime opcode which was unspecified");
            }
        }

        @Override
        public @Nullable ZonedDateTime parse(final String input) {
            if (input.length() < 3) {
                // This will never result in a successful parse
                return null;
            }

            // validate the expression begins with either + or -
            final char initial = input.charAt(0);
            if (initial != '+' && initial != '-') {
                // not a relative time
                return null;
            }

            ZonedDateTime working = ZonedDateTime.now(zone);
            final Matcher match = RELATIVE_TIME_PART.matcher(input);
            int index = 0;
            boolean positive = true;
            while (match.find(index)) {
                positive = !"-".equals(match.group(1)); // two options are plus or minus, no value is interpreted as a positive

                long quantity;
                try {
                    quantity = Long.parseLong(match.group(2));
                } catch (final NumberFormatException ex) {
                    // its possible the user can enter an extremely large number not representable by a long - do _not_
                    // change this into an exception!
                    return null;
                }

                if (!positive) {
                    quantity = -quantity;
                }

                final ChronoUnit unit = unit(match.group(3));
                working = working.plus(quantity, unit);
                index += match.group().length();
            }

            if (index == input.length()) { // if we matched the whole string
                return working;
            }
            return null;
        }
    }
}
