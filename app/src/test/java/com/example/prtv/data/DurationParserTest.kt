package com.example.prtv.data

import org.junit.Assert.assertEquals
import org.junit.Test

class DurationParserTest {
    @Test fun hoursMinutesSeconds() = assertEquals(3723L, DurationParser.parseSeconds("PT1H2M3S"))
    @Test fun secondsOnly() = assertEquals(45L, DurationParser.parseSeconds("PT45S"))
    @Test fun minutesOnly() = assertEquals(600L, DurationParser.parseSeconds("PT10M"))
    @Test fun zeroForms() {
        assertEquals(0L, DurationParser.parseSeconds("PT0S"))
        assertEquals(0L, DurationParser.parseSeconds("P0D"))
    }
    @Test fun daysAndHours() = assertEquals(93_600L, DurationParser.parseSeconds("P1DT2H"))
    @Test fun invalidInputIsZero() {
        assertEquals(0L, DurationParser.parseSeconds(null))
        assertEquals(0L, DurationParser.parseSeconds(""))
        assertEquals(0L, DurationParser.parseSeconds("garbage"))
    }
}
