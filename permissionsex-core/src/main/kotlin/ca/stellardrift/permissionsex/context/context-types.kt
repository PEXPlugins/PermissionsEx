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

package ca.stellardrift.permissionsex.context

import ca.stellardrift.permissionsex.config.PermissionsExConfiguration
import ca.stellardrift.permissionsex.subject.CalculatedSubject
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

/**
 * A placeholder context definition for implementations to use when a context query comes in for an unknown context
 */
class FallbackContextDefinition(name: String) : SimpleContextDefinition(name)

object ServerTagContextDefinition : PEXContextDefinition<String>("server-tag") {
    private var activeTags: List<String> = listOf()

    override fun serialize(userValue: String): String = userValue
    override fun deserialize(canonicalValue: String): String = canonicalValue
    override fun matches(ownVal: String, testVal: String): Boolean =
        ownVal == testVal

    override fun update(config: PermissionsExConfiguration<*>) {
        activeTags = config.serverTags
    }

    override fun accumulateCurrentValues(subject: CalculatedSubject, consumer: (value: String) -> Unit) {
        activeTags.forEach(consumer)
    }
}

interface TimeContextParser {
    fun parse(s: String): ZonedDateTime?
}

class DateTimeFormatterParser(val d: DateTimeFormatter) : TimeContextParser {
    override fun parse(s: String): ZonedDateTime? {
        return try {
            ZonedDateTime.parse(s, d)
        } catch (ex: DateTimeParseException) {
            null
        }
    }
}

class EpochTimeContextParser(val z: ZoneId) : TimeContextParser {
    override fun parse(s: String): ZonedDateTime? {
        return try {
            ZonedDateTime.from(Instant.ofEpochSecond(s.toLong())).withZoneSameLocal(z)
        } catch (ex: NumberFormatException) {
            null
        }
    }
}

class RelativeTimeContextParser(val z: ZoneId) : TimeContextParser {
    // TODO: This needs localized (somehow...)
    private val relativeTimePartRegex = Regex.fromLiteral("^(((?:\\+|-)?)[0-9\\.]*[0-9])(seconds?|minutes?|hours?|days?|weeks?|months?|years?|s|m|h|d|w|y)")

    override fun parse(s: String): ZonedDateTime? {
        if (s.length < 3)
            // This will never result in a successful parse
            return null

        // validate the expression begins with either + or -
        when (s[0]) {
            '+', '-' -> { /* ignored */ }
            else -> return null
        }

        var working = ZonedDateTime.now(z)
        val match = relativeTimePartRegex.find(s) ?: return null
        var index = 0
        var positive = true
        do {
            positive = when (match.groups[1]!!.value) {
                "+" -> true
                "-" -> false
                else -> positive
            }

            var quantity = try {
                match.groups[2]!!.value.toLong()
            } catch (ex: NumberFormatException) {
                // its possible the user can enter an extremely large number not representable by a long - do _not_
                // change this into an exception!
                return null
            }

            if (!positive) {
                quantity = -quantity
            }

            val unit = when (match.groups[3]!!.value) {
                "second", "seconds", "s" -> ChronoUnit.SECONDS
                "minute", "minutes", "m" -> ChronoUnit.MINUTES
                "hour", "hours", "h" -> ChronoUnit.HOURS
                "day", "days", "d" -> ChronoUnit.DAYS
                "week", "weeks", "w" -> ChronoUnit.WEEKS
                "month", "months" -> ChronoUnit.MONTHS
                "year", "years", "y" -> ChronoUnit.YEARS
                // This shouldn't be reached
                else -> throw IllegalStateException("Hit datetime opcode which was unspecified")
            }

            working = working.plus(quantity, unit)
            index += match.groups[0]!!.value.length
        } while (index < s.length)
        return null
    }
}

internal fun buildTimeParsers(z: ZoneId): List<TimeContextParser> = listOf(
    DateTimeFormatterParser(DateTimeFormatter.ISO_DATE_TIME.withZone(z)),
    DateTimeFormatterParser(DateTimeFormatter.ISO_TIME.withZone(z)),
    DateTimeFormatterParser(DateTimeFormatter.ISO_DATE.withZone(z)),
    DateTimeFormatterParser(DateTimeFormatter.RFC_1123_DATE_TIME.withZone(z)),
    RelativeTimeContextParser(z),
    EpochTimeContextParser(z)
)

open class TimeContextDefinition internal constructor(name: String) : PEXContextDefinition<ZonedDateTime>(name) {
    private var currentTimeZone: ZoneId = ZoneId.systemDefault()
    private var timeParsers: List<TimeContextParser> = buildTimeParsers(currentTimeZone)

    override fun update(config: PermissionsExConfiguration<*>) {
        // TODO: implement timezone configuration option
    }

    override fun accumulateCurrentValues(subject: CalculatedSubject, consumer: (value: ZonedDateTime) -> Unit) {
        consumer(ZonedDateTime.now(currentTimeZone).truncatedTo(ChronoUnit.SECONDS))
    }

    override fun serialize(userValue: ZonedDateTime): String {
        return userValue.format(DateTimeFormatter.ISO_DATE_TIME)
    }

    override fun deserialize(canonicalValue: String): ZonedDateTime {
        for (parser in timeParsers) {
            return parser.parse(canonicalValue) ?: continue
        }

        throw IllegalArgumentException("Could not deserialize using any known methods")
    }

    override fun matches(ownVal: ZonedDateTime, testVal: ZonedDateTime): Boolean {
        return testVal.truncatedTo(ChronoUnit.SECONDS)
            .isEqual(ownVal.truncatedTo(ChronoUnit.SECONDS))
    }
}

object BeforeTimeContextDefinition : TimeContextDefinition("before-time") {
    override fun matches(ownVal: ZonedDateTime, testVal: ZonedDateTime): Boolean {
        return testVal.truncatedTo(ChronoUnit.SECONDS)
            .isBefore(ownVal.truncatedTo(ChronoUnit.SECONDS))
    }
}

object AfterTimeContextDefinition : TimeContextDefinition("after-time") {
    override fun matches(ownVal: ZonedDateTime, testVal: ZonedDateTime): Boolean {
        return testVal.truncatedTo(ChronoUnit.SECONDS)
            .isAfter(ownVal.truncatedTo(ChronoUnit.SECONDS))
    }
}
