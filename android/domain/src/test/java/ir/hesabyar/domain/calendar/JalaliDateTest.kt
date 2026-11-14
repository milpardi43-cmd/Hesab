package ir.hesabyar.domain.calendar

import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JalaliDateTest {

    private val tehran = ZoneId.of("Asia/Tehran")

    @Test
    fun `converts known nowruz dates`() {
        assertEquals(JalaliDate(1400, 1, 1), JalaliDate.fromGregorian(LocalDate.of(2021, 3, 21)))
        assertEquals(JalaliDate(1402, 1, 1), JalaliDate.fromGregorian(LocalDate.of(2023, 3, 21)))
        assertEquals(JalaliDate(1403, 1, 1), JalaliDate.fromGregorian(LocalDate.of(2024, 3, 20)))
        assertEquals(JalaliDate(1399, 12, 30), JalaliDate.fromGregorian(LocalDate.of(2021, 3, 20)))
    }

    @Test
    fun `round trips gregorian dates`() {
        var date = LocalDate.of(1990, 1, 1)
        repeat(1500) {
            val jalali = JalaliDate.fromGregorian(date)
            assertEquals(date, jalali.toGregorian())
            date = date.plusDays(17)
        }
    }

    @Test
    fun `month lengths follow the solar hijri rule`() {
        for (month in 1..6) assertEquals(31, JalaliDate.daysInMonth(1403, month))
        for (month in 7..11) assertEquals(30, JalaliDate.daysInMonth(1403, month))
        assertTrue(JalaliDate.daysInMonth(1403, 12) in 29..30)
    }

    @Test
    fun `weekday names are correct`() {
        val newYear = JalaliDate(1403, 1, 1) // Wednesday 20 March 2024
        assertEquals(4, newYear.weekDayIndex())
        assertEquals("چهارشنبه", newYear.weekDayName())
    }

    @Test
    fun `parses and formats persian dates`() {
        val parsed = JalaliDate.parse("۱۴۰۳/۰۵/۱۲")
        assertEquals(JalaliDate(1403, 5, 12), parsed)
        assertEquals("۱۴۰۳/۰۵/۱۲", parsed!!.numeric())
        assertEquals("۱۲ مرداد ۱۴۰۳", parsed.long())
        assertEquals("1403/05/12", parsed.numeric(false))
    }

    @Test
    fun `epoch millis start at local midnight`() {
        val millis = JalaliDate.startOfDayMillis(1403, 5, 12, tehran)
        val back = JalaliDate.fromEpochMillis(millis, tehran)
        assertEquals(JalaliDate(1403, 5, 12), back)
        assertEquals(millis, back.toEpochMillis(tehran))
    }

    @Test
    fun `month navigation respects month length`() {
        val end = JalaliDate(1403, 6, 31)
        assertEquals(JalaliDate(1403, 7, 30), end.plusMonths(1))
        assertEquals(JalaliDate(1404, 1, 29), JalaliDate(1403, 12, 29).plusMonths(1))
    }

    @Test
    fun `start of day normalises any timestamp`() {
        val millis = JalaliDate.startOfDayMillis(1403, 5, 12, tehran) + 13 * 3600_000L
        assertEquals(JalaliDate.startOfDayMillis(1403, 5, 12, tehran), JalaliDate.startOfDayMillis(millis, tehran))
    }
}
