package ir.hesabyar.domain.calendar

import ir.hesabyar.domain.format.PersianText
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Solar Hijri (Jalali) calendar.
 *
 * Dates are stored in the database as epoch milliseconds of the local start of day,
 * completely independent from the display calendar. Conversion happens only at the edges.
 * Algorithm ported from the well-known jalaali-js implementation.
 */
data class JalaliDate(val year: Int, val month: Int, val day: Int) : Comparable<JalaliDate> {

    init {
        require(month in 1..12) { "month must be 1..12" }
        require(day in 1..daysInMonth(year, month)) { "invalid day for month" }
    }

    val epochDay: Long get() = toJdn(year, month, day) - EPOCH_JDN

    fun toGregorian(): LocalDate {
        val jdn = epochDay + EPOCH_JDN
        val g = d2g(jdn)
        return LocalDate.of(g.first, g.second, g.third)
    }

    fun plusDays(days: Long): JalaliDate = fromEpochDay(epochDay + days)

    fun plusMonths(months: Int): JalaliDate {
        val total = year * 12 + (month - 1) + months
        val newYear = Math.floorDiv(total, 12)
        val newMonth = Math.floorMod(total, 12) + 1
        val newDay = day.coerceAtMost(daysInMonth(newYear, newMonth))
        return JalaliDate(newYear, newMonth, newDay)
    }

    fun startOfMonth(): JalaliDate = JalaliDate(year, month, 1)

    fun daysInCurrentMonth(): Int = daysInMonth(year, month)

    fun numeric(persianDigits: Boolean = true): String {
        val raw = "%04d/%02d/%02d".format(year, month, day)
        return if (persianDigits) PersianText.toPersianDigits(raw) else raw
    }

    fun long(persianDigits: Boolean = true): String {
        val raw = "${PersianText.toPersianDigits(day.toString())} ${monthName(month)} ${PersianText.toPersianDigits(year.toString())}"
        return if (persianDigits) raw else "$day ${monthName(month)} $year"
    }

    fun monthYear(persianDigits: Boolean = true): String {
        val raw = "${monthName(month)} ${PersianText.toPersianDigits(year.toString())}"
        return if (persianDigits) raw else "${monthName(month)} $year"
    }

    fun withWeekday(persianDigits: Boolean = true): String =
        "${weekDayName(weekDayIndex())} ${long(persianDigits)}"

    fun weekDayIndex(): Int = Math.floorMod(epochDay + 5, 7).toInt() // 0 = Saturday

    fun weekDayName(): String = WEEK_DAYS[weekDayIndex()]

    fun toEpochMillis(zone: ZoneId = ZoneId.systemDefault()): Long =
        toGregorian().atStartOfDay(zone).toInstant().toEpochMilli()

    override fun compareTo(other: JalaliDate): Int = epochDay.compareTo(other.epochDay)

    override fun toString(): String = numeric(false)

    companion object {
        private const val EPOCH_JDN = 2440588L // JDN of 1970-01-01
        private val MONTHS = listOf(
            "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
            "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
        )
        private val WEEK_DAYS = listOf(
            "شنبه", "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنجشنبه", "جمعه"
        )
        private val BREAKS = intArrayOf(
            -61, 9, 38, 199, 426, 686, 756, 818, 1111, 1181, 1210,
            1635, 2060, 2097, 2192, 2262, 2324, 2394, 2456, 3178
        )

        fun monthName(month: Int): String = MONTHS[month - 1]

        fun weekDayName(index: Int): String = WEEK_DAYS[Math.floorMod(index, 7)]

        fun daysInMonth(year: Int, month: Int): Int = when (month) {
            in 1..6 -> 31
            in 7..11 -> 30
            12 -> if (isLeapYear(year)) 30 else 29
            else -> throw IllegalArgumentException("invalid month")
        }

        fun isLeapYear(year: Int): Boolean = jalCal(year).leap == 0

        fun fromEpochDay(epochDay: Long): JalaliDate {
            val jdn = epochDay + EPOCH_JDN
            val gy = d2g(jdn).first
            var jy = gy - 621
            val r = jalCal(jy)
            val jdn1f = g2d(gy.toLong(), 3, r.march.toLong())
            var k = jdn - jdn1f
            var jm: Int
            var jd: Int
            if (k >= 0) {
                if (k <= 185) {
                    jm = 1 + (k / 31).toInt()
                    jd = (mod(k, 31) + 1).toInt()
                    return JalaliDate(jy, jm, jd)
                }
                k -= 186
            } else {
                jy -= 1
                k += 179
                if (r.leap == 1) k += 1
            }
            jm = 7 + (k / 30).toInt()
            jd = (mod(k, 30) + 1).toInt()
            return JalaliDate(jy, jm, jd)
        }

        fun fromEpochMillis(millis: Long, zone: ZoneId = ZoneId.systemDefault()): JalaliDate {
            val date = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
            return fromGregorian(date)
        }

        fun fromGregorian(date: LocalDate): JalaliDate = fromEpochDay(date.toEpochDay())

        fun today(zone: ZoneId = ZoneId.systemDefault()): JalaliDate = fromGregorian(LocalDate.now(zone))

        fun parse(input: String): JalaliDate? {
            val normalized = PersianText.toEnglishDigits(input).replace('-', '/').replace('.', '/')
            val parts = normalized.split('/').filter { it.isNotBlank() }
            if (parts.size != 3) return null
            val year = parts[0].trim().toIntOrNull() ?: return null
            val month = parts[1].trim().toIntOrNull() ?: return null
            val day = parts[2].trim().toIntOrNull() ?: return null
            if (month !in 1..12 || day < 1 || day > daysInMonth(year, month)) return null
            return JalaliDate(year, month, day)
        }

        /** Converts a Jalali date to the epoch millis of the local start of day. */
        fun startOfDayMillis(year: Int, month: Int, day: Int, zone: ZoneId = ZoneId.systemDefault()): Long =
            JalaliDate(year, month, day).toEpochMillis(zone)

        fun startOfMonthMillis(year: Int, month: Int, zone: ZoneId = ZoneId.systemDefault()): Long =
            JalaliDate(year, month, 1).toEpochMillis(zone)

        fun startOfNextMonthMillis(year: Int, month: Int, zone: ZoneId = ZoneId.systemDefault()): Long =
            JalaliDate(year, month, 1).plusMonths(1).toEpochMillis(zone)

        fun startOfDayMillis(millis: Long, zone: ZoneId = ZoneId.systemDefault()): Long =
            Instant.ofEpochMilli(millis).atZone(zone).toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli()

        fun formatDate(millis: Long, persianDigits: Boolean = true, zone: ZoneId = ZoneId.systemDefault()): String =
            fromEpochMillis(millis, zone).numeric(persianDigits)

        fun formatLongDate(millis: Long, persianDigits: Boolean = true, zone: ZoneId = ZoneId.systemDefault()): String =
            fromEpochMillis(millis, zone).long(persianDigits)

        fun formatDateTime(millis: Long, persianDigits: Boolean = true, zone: ZoneId = ZoneId.systemDefault()): String {
            val zoned = Instant.ofEpochMilli(millis).atZone(zone)
            val time = "%02d:%02d".format(zoned.hour, zoned.minute)
            return "${fromGregorian(zoned.toLocalDate()).numeric(persianDigits)} - ${PersianText.toPersianDigits(time)}"
        }

        fun monthDays(year: Int, month: Int): List<JalaliDate> =
            (1..daysInMonth(year, month)).map { JalaliDate(year, month, it) }

        // ---------------------------------------------------------------- internals

        private fun div(a: Long, b: Long): Long = a / b
        private fun mod(a: Long, b: Long): Long = a - div(a, b) * b

        private data class JalCalResult(val leap: Int, val march: Int)

        private fun jalCal(jy: Int): JalCalResult {
            val bl = BREAKS.size
            val gy = jy + 621
            var leapJ = -14L
            var jp = BREAKS[0].toLong()
            var jump = 0L
            require(jy >= BREAKS[0] && jy < BREAKS[bl - 1]) { "Invalid Jalali year $jy" }
            var i = 1
            while (i < bl) {
                val jm = BREAKS[i].toLong()
                jump = jm - jp
                if (jy < jm) break
                leapJ += div(jump, 33) * 8 + div(mod(jump, 33), 4)
                jp = jm
                i += 1
            }
            var n = jy - jp
            leapJ += div(n, 33) * 8 + div(mod(n, 33) + 3, 4)
            if (mod(jump, 33) == 4L && jump - n == 4L) leapJ += 1
            val leapG = div(gy.toLong(), 4) - div((div(gy.toLong(), 100) + 1) * 3, 4) - 150
            val march = 20 + leapJ - leapG
            if (jump - n < 6) n -= jump - div(jump + 4, 33) * 33
            var leap = mod(mod(n + 1, 33) - 1, 4)
            if (leap == -1L) leap = 4
            return JalCalResult(leap.toInt(), march.toInt())
        }

        private fun g2d(gy: Long, gm: Long, gd: Long): Long {
            var d = div((gy + div(gm - 8, 6) + 100100) * 1461, 4) +
                div(153 * mod(gm + 9, 12) + 2, 5) + gd - 34840408
            d = d - div(div(gy + 100100 + div(gm - 8, 6), 100) * 3, 4) + 752
            return d
        }

        private fun d2g(jdn: Long): Triple<Int, Int, Int> {
            var j = 4 * jdn + 139361631
            j += div(div(4 * jdn + 183187720, 146097) * 3, 4) * 4 - 3908
            val i = div(mod(j, 1461), 4) * 5 + 308
            val gd = div(mod(i, 153), 5) + 1
            val gm = mod(div(i, 153), 12) + 1
            val gy = div(j, 1461) - 100100 + div(8 - gm, 6)
            return Triple(gy.toInt(), gm.toInt(), gd.toInt())
        }

        private fun toJdn(jy: Int, jm: Int, jd: Int): Long {
            val r = jalCal(jy)
            val g = g2d((jy + 621).toLong(), 3, r.march.toLong())
            return g + (jm - 1) * 31 - div(jm.toLong(), 7) * (jm - 7) + jd - 1
        }
    }
}
