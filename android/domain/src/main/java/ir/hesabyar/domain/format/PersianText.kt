package ir.hesabyar.domain.format

import kotlin.math.abs

/**
 * Persian digit conversion and money formatting.
 * Stored amounts stay canonical Long values; only the presentation layer converts them.
 */
object PersianText {

    private val PERSIAN_DIGITS = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
    private val ARABIC_DIGITS = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
    private const val THOUSANDS_SEPARATOR = '٬' // U+066C arabic thousands separator

    fun toPersianDigits(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            sb.append(
                when (ch) {
                    in '0'..'9' -> PERSIAN_DIGITS[ch - '0']
                    else -> ch
                }
            )
        }
        return sb.toString()
    }

    fun toEnglishDigits(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            val persianIndex = PERSIAN_DIGITS.indexOf(ch)
            val arabicIndex = ARABIC_DIGITS.indexOf(ch)
            sb.append(
                when {
                    persianIndex >= 0 -> ('0' + persianIndex)
                    arabicIndex >= 0 -> ('0' + arabicIndex)
                    ch == THOUSANDS_SEPARATOR || ch == ',' -> ' '
                    else -> ch
                }
            )
        }
        return sb.toString()
    }

    fun groupDigits(value: Long): String {
        val raw = abs(value).toString()
        val sb = StringBuilder()
        for ((index, ch) in raw.withIndex()) {
            if (index > 0 && (raw.length - index) % 3 == 0) sb.append(THOUSANDS_SEPARATOR)
            sb.append(ch)
        }
        return sb.toString()
    }

    fun formatNumber(value: Long, persianDigits: Boolean = true): String {
        val grouped = groupDigits(value)
        val signed = if (value < 0) "−$grouped" else grouped
        return if (persianDigits) toPersianDigits(signed) else signed.replace(THOUSANDS_SEPARATOR, ',')
    }

    fun formatMoney(value: Long, persianDigits: Boolean = true): String =
        "${formatNumber(value, persianDigits)} تومان"

    fun formatQuantity(quantity: Double, persianDigits: Boolean = true): String {
        val text = if (quantity % 1.0 == 0.0) {
            quantity.toLong().toString()
        } else {
            val rounded = (quantity * 1000).toLong() / 1000.0
            rounded.toString().trimEnd('0').trimEnd('.')
        }
        return if (persianDigits) toPersianDigits(text) else text
    }

    /** Short money form for charts: ۱٫۲ میلیون */
    fun formatCompact(value: Long, persianDigits: Boolean = true): String {
        val absolute = abs(value)
        val text = when {
            absolute >= 1_000_000_000L -> "%.1f".format(absolute / 1_000_000_000.0) + " میلیارد"
            absolute >= 1_000_000L -> "%.1f".format(absolute / 1_000_000.0) + " میلیون"
            absolute >= 1_000L -> "%.0f".format(absolute / 1_000.0) + " هزار"
            else -> absolute.toString()
        }.replace(".", "٫")
        val signed = if (value < 0) "−$text" else text
        return if (persianDigits) toPersianDigits(signed) else signed
    }

    /** Parses user input such as "۱٬۲۰۰٬۰۰۰" or "1200000" to Long. Returns null for invalid input. */
    fun parseMoney(input: String): Long? {
        val cleaned = toEnglishDigits(input).replace(" ", "").replace("\u200c", "")
        if (cleaned.isBlank()) return null
        val negative = cleaned.startsWith("-")
        val digitsOnly = cleaned.removePrefix("-").filter { it in '0'..'9' }
        if (digitsOnly.isEmpty()) return null
        val value = digitsOnly.toLongOrNull() ?: return null
        return if (negative) -value else value
    }

    fun parseQuantity(input: String): Double? {
        val cleaned = toEnglishDigits(input).replace(" ", "").replace("٫", ".").replace(",", ".")
        if (cleaned.isBlank()) return null
        return cleaned.toDoubleOrNull()
    }

    fun maskPhone(phone: String?): String = phone.orEmpty()
}
