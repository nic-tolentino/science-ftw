package com.complexly.academy.util

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.plus
import kotlinx.datetime.DateTimeUnit

/**
 * Developer time override utility.
 *
 * Allows shifting the "effective today" by N days for testing spaced repetition.
 * When offset is 0, returns the real current date.
 * When offset is positive, returns that many days in the future.
 * When offset is negative, returns that many days in the past.
 */
object TimeOverride {
    private var dayOffset: Int = 0

    fun setOffset(offset: Int) {
        dayOffset = offset
    }

    fun getOffset(): Int = dayOffset

    fun reset() {
        dayOffset = 0
    }

    /** Returns the effective "today" date (real date + offset). */
    fun effectiveToday(): LocalDate {
        val realToday = kotlinx.datetime.Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        return if (dayOffset == 0) {
            realToday
        } else {
            realToday.plus(dayOffset.toLong(), DateTimeUnit.DAY)
        }
    }
}
