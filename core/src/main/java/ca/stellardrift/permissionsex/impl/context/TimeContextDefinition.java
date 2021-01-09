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
package ca.stellardrift.permissionsex.impl.context;

import ca.stellardrift.permissionsex.impl.config.PermissionsExConfiguration;
import ca.stellardrift.permissionsex.subject.CalculatedSubject;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

public final class TimeContextDefinition extends PEXContextDefinition<ZonedDateTime>  {
    public static final TimeContextDefinition BEFORE_TIME = new TimeContextDefinition("before-time", ZonedDateTime::isBefore);
    public static final TimeContextDefinition AFTER_TIME = new TimeContextDefinition("after-time", ZonedDateTime::isAfter);

    private final ZoneId currentTimeZone = ZoneId.systemDefault();
    private final TimeContextParser[] timeParsers = TimeContextParser.parsersForZone(currentTimeZone);
    private final BiPredicate<ZonedDateTime, ZonedDateTime> comparisonFunc;

    private TimeContextDefinition(final String name, final BiPredicate<ZonedDateTime, ZonedDateTime> comparisonFunc) {
        super(name);
        this.comparisonFunc = comparisonFunc;
    }

    // Perform a comparison with second precision
    static boolean compare(final ZonedDateTime a, final ZonedDateTime b, final BiPredicate<ZonedDateTime, ZonedDateTime> test) {
        return test.test(a.truncatedTo(ChronoUnit.SECONDS), b.truncatedTo(ChronoUnit.SECONDS));
    }

    @Override
    public String serialize(final ZonedDateTime canonicalValue) {
        return canonicalValue.format(DateTimeFormatter.ISO_DATE_TIME);
    }

    @Override
    public @Nullable ZonedDateTime deserialize(final String userValue) {
        @Nullable ZonedDateTime attempt;
        for (final TimeContextParser parser : this.timeParsers) {
            attempt = parser.parse(userValue);
            if (attempt != null) {
                return attempt;
            }
        }

        return null;
        // throw new IllegalArgumentException("Could not deserialize time from input " + userValue + " using any known methods.");
    }

    @Override
    public boolean matches(final ZonedDateTime ownVal, final ZonedDateTime testVal) {
        return compare(testVal, ownVal, this.comparisonFunc);
    }

    @Override
    public void accumulateCurrentValues(final CalculatedSubject subject, final Consumer<ZonedDateTime> consumer) {
        consumer.accept(ZonedDateTime.now(this.currentTimeZone).truncatedTo(ChronoUnit.SECONDS));
    }

    @Override
    public void update(PermissionsExConfiguration<?> config) {
        // TODO: Update timezone from configuration
    }
}
