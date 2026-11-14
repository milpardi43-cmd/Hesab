package ir.hesabyar.app.ui.common

import ir.hesabyar.domain.calendar.JalaliDate
import ir.hesabyar.domain.format.PersianText
import ir.hesabyar.domain.model.Contact
import ir.hesabyar.domain.model.ContactLedgerEntry
import ir.hesabyar.domain.model.ContactType
import ir.hesabyar.domain.model.TransactionDetail
import ir.hesabyar.domain.repository.ContactTotals

data class UiEvent(val message: String, val isError: Boolean = false)

object Receipts {

    fun transactionReceipt(detail: TransactionDetail, businessName: String): String {
        val tx = detail.transaction
        val builder = StringBuilder()
        builder.appendLine("رسید ${tx.type.label} — حسابیار")
        builder.appendLine("فروشگاه: $businessName")
        builder.appendLine("شماره تراکنش: ${tx.code}")
        builder.appendLine("تاریخ: ${JalaliDate.formatLongDate(tx.date)}")
        detail.contact?.let { builder.appendLine("شخص: ${it.name}") }
        detail.account?.let { builder.appendLine("حساب: ${it.title}") }
        if (detail.saleItems.isNotEmpty()) {
            builder.appendLine("اقلام:")
            detail.saleItems.forEach { item ->
                builder.appendLine(
                    "- ${item.productName} × ${PersianText.formatQuantity(item.quantity)} = ${PersianText.formatMoney(item.totalAmount)}"
                )
            }
        }
        if (detail.purchaseItems.isNotEmpty()) {
            builder.appendLine("اقلام خرید:")
            detail.purchaseItems.forEach { item ->
                builder.appendLine(
                    "- ${item.productName} × ${PersianText.formatQuantity(item.quantity)} = ${PersianText.formatMoney(item.totalAmount)}"
                )
            }
        }
        detail.sale?.let {
            builder.appendLine("جمع کل: ${PersianText.formatMoney(it.grossAmount)}")
            builder.appendLine("تخفیف: ${PersianText.formatMoney(it.discountAmount)}")
            builder.appendLine("مبلغ نهایی: ${PersianText.formatMoney(it.netAmount)}")
        }
        detail.purchase?.let {
            builder.appendLine("مبلغ خرید: ${PersianText.formatMoney(it.netAmount)}")
            builder.appendLine("هزینه حمل: ${PersianText.formatMoney(it.freightAmount)}")
        }
        detail.expense?.let {
            builder.appendLine("عنوان هزینه: ${it.title}")
            builder.appendLine("دسته: ${it.category.label}")
        }
        builder.appendLine("مبلغ: ${PersianText.formatMoney(tx.amount)}")
        builder.appendLine("پرداخت‌شده: ${PersianText.formatMoney(tx.paidAmount)}")
        if (tx.remainingAmount > 0) {
            builder.appendLine("مانده: ${PersianText.formatMoney(tx.remainingAmount)}")
        }
        tx.description?.takeIf { it.isNotBlank() }?.let { builder.appendLine("توضیحات: $it") }
        if (tx.status.name == "REVERSED") builder.appendLine("وضعیت: برگشت‌خورده")
        builder.appendLine()
        builder.append("ساخته‌شده با حسابیار — حساب‌وکتاب کسب‌وکار شما")
        return builder.toString()
    }

    fun contactStatement(
        contact: Contact,
        ledger: List<ContactLedgerEntry>,
        totals: ContactTotals,
        businessName: String
    ): String {
        val builder = StringBuilder()
        builder.appendLine("صورت‌حساب ${contact.name}")
        builder.appendLine("فروشگاه: $businessName")
        contact.phone?.takeIf { it.isNotBlank() }?.let { builder.appendLine("موبایل: $it") }
        builder.appendLine("نوع: ${contact.type.label}")
        val balanceLabel = when {
            contact.balance > 0 -> "بدهکار"
            contact.balance < 0 -> "بستانکار"
            else -> "تسویه"
        }
        builder.appendLine("مانده فعلی: ${PersianText.formatMoney(kotlin.math.abs(contact.balance))} ($balanceLabel)")
        if (contact.type.isCustomer) {
            builder.appendLine("مجموع فروش: ${PersianText.formatMoney(totals.totalSales)}")
            builder.appendLine("مجموع دریافتی: ${PersianText.formatMoney(totals.totalReceipts)}")
        }
        if (contact.type.isSupplier) {
            builder.appendLine("مجموع خرید: ${PersianText.formatMoney(totals.totalPurchases)}")
            builder.appendLine("مجموع پرداختی: ${PersianText.formatMoney(totals.totalPayments)}")
        }
        builder.appendLine()
        builder.appendLine("گردش حساب:")
        ledger.take(100).forEach { entry ->
            val sign = when {
                entry.debit > 0 -> "بدهکار ${PersianText.formatNumber(entry.debit)}"
                entry.credit > 0 -> "بستانکار ${PersianText.formatNumber(entry.credit)}"
                else -> ""
            }
            builder.appendLine(
                "${JalaliDate.formatDate(entry.createdAt)} | ${entry.note ?: entry.transactionCode ?: "ثبت"} | $sign | مانده: ${PersianText.formatNumber(entry.balanceAfter)}"
            )
        }
        builder.appendLine()
        builder.append("ساخته‌شده با حسابیار")
        return builder.toString()
    }

    fun contactTypeLabel(type: ContactType): String = type.label
}
