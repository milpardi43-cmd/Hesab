package ir.hesabyar.domain.validation

import ir.hesabyar.domain.format.PersianText

/**
 * Field validation. Every method returns a Persian error message or null when valid.
 * Screens call these directly, so messages stay consistent across the app.
 */
object Validate {

    fun requiredName(value: String?): String? =
        if (value.isNullOrBlank()) "این فیلد الزامی است" else null

    fun optionalPhone(value: String?): String? {
        val digits = PersianText.toEnglishDigits(value.orEmpty()).filter { it in '0'..'9' }
        if (digits.isEmpty()) return null
        return if (digits.length == 11 && digits.startsWith("09")) null
        else "شماره موبایل باید ۱۱ رقم و با ۰۹ شروع شود"
    }

    fun requiredPhone(value: String?): String? =
        if (value.isNullOrBlank()) "شماره موبایل الزامی است" else optionalPhone(value)

    fun positiveAmount(value: Long?): String? =
        if (value == null || value <= 0) "مبلغ باید بزرگ‌تر از صفر باشد" else null

    fun nonNegativeAmount(value: Long?): String? =
        if (value == null || value < 0) "مبلغ نمی‌تواند منفی باشد" else null

    fun amountWithin(value: Long, max: Long, message: String = "مبلغ نمی‌تواند از کل بیشتر باشد"): String? =
        if (value > max) message else null

    fun positiveQuantity(value: Double?): String? =
        if (value == null || value <= 0.0) "تعداد باید بزرگ‌تر از صفر باشد" else null

    fun quantityWithin(value: Double, max: Double, message: String = "تعداد بیشتر از موجودی مجاز است"): String? =
        if (value > max + 1e-9) message else null

    fun nonNegativeQuantity(value: Double?): String? =
        if (value == null || value < 0.0) "تعداد نمی‌تواند منفی باشد" else null

    fun positivePrice(value: Long?): String? =
        if (value == null || value <= 0) "قیمت باید بزرگ‌تر از صفر باشد" else null

    fun saleItemsNonEmpty(count: Int): String? =
        if (count == 0) "حداقل یک کالا به فروش اضافه کنید" else null

    fun creditRequiresContact(modeLabel: String, contactSelected: Boolean): String? =
        if (!contactSelected) "برای $modeLabel انتخاب مشتری الزامی است" else null

    fun reasonRequired(value: String?): String? =
        if (value.isNullOrBlank()) "دلیل عملیات را وارد کنید" else null

    fun futureDate(value: Long, todayStart: Long): String? =
        if (value > todayStart) "تاریخ نمی‌تواند در آینده باشد" else null
}
