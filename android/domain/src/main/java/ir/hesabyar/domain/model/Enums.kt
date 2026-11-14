package ir.hesabyar.domain.model

/** Persian labels live in the domain because the product is single-locale (fa-IR). */

enum class BusinessType(val label: String) {
    SUPERMARKET("سوپرمارکت"),
    CLOTHING("پوشاک"),
    HOME_APPLIANCE("لوازم خانگی"),
    MOBILE_DIGITAL("موبایل و دیجیتال"),
    TOOLS_PARTS("ابزار و قطعات"),
    FOOD("مواد غذایی"),
    SERVICES("خدماتی"),
    OTHER("سایر");

    companion object {
        fun fromName(value: String?): BusinessType =
            entries.firstOrNull { it.name == value } ?: OTHER
    }
}

enum class AccountType(val label: String) {
    CASH("صندوق نقدی"),
    BANK("حساب بانکی"),
    POS("حساب کارتخوان"),
    OTHER("سایر حساب‌ها");

    companion object {
        fun fromName(value: String?): AccountType =
            entries.firstOrNull { it.name == value } ?: CASH
    }
}

enum class ContactType(val label: String) {
    CUSTOMER("مشتری"),
    SUPPLIER("تأمین‌کننده"),
    BOTH("مشتری و تأمین‌کننده");

    val isCustomer: Boolean get() = this == CUSTOMER || this == BOTH
    val isSupplier: Boolean get() = this == SUPPLIER || this == BOTH

    companion object {
        fun fromName(value: String?): ContactType =
            entries.firstOrNull { it.name == value } ?: CUSTOMER
    }
}

enum class TransactionType(val label: String) {
    CASH_SALE("فروش نقدی"),
    CREDIT_SALE("فروش نسیه"),
    MIXED_SALE("فروش ترکیبی"),
    CUSTOMER_RECEIPT("دریافت طلب"),
    EXPENSE("هزینه"),
    PURCHASE("خرید کالا"),
    SUPPLIER_PAYMENT("پرداخت بدهی"),
    SALE_RETURN("برگشت از فروش"),
    CASH_ADJUSTMENT("اصلاح صندوق"),
    STOCK_ADJUSTMENT("اصلاح موجودی"),
    ACCOUNT_TRANSFER("انتقال بین حساب‌ها"),
    REVERSAL("عملیات معکوس");

    val isSale: Boolean get() = this == CASH_SALE || this == CREDIT_SALE || this == MIXED_SALE

    companion object {
        fun fromName(value: String?): TransactionType =
            entries.firstOrNull { it.name == value } ?: EXPENSE
    }
}

enum class TransactionStatus(val label: String) {
    COMPLETED("ثبت‌شده"),
    REVERSED("برگشت‌خورده");

    companion object {
        fun fromName(value: String?): TransactionStatus =
            entries.firstOrNull { it.name == value } ?: COMPLETED
    }
}

enum class PaymentStatus(val label: String) {
    PAID("تسویه‌شده"),
    PARTIAL("پرداخت جزئی"),
    UNPAID("پرداخت‌نشده");

    companion object {
        fun fromName(value: String?): PaymentStatus? =
            entries.firstOrNull { it.name == value }
    }
}

enum class PaymentMethod(val label: String) {
    CASH("نقد"),
    CARD_READER("کارتخوان"),
    BANK_TRANSFER("انتقال بانکی"),
    OTHER("سایر");

    companion object {
        fun fromName(value: String?): PaymentMethod =
            entries.firstOrNull { it.name == value } ?: CASH
    }
}

enum class ExpenseCategory(val label: String) {
    RENT("اجاره"),
    SALARY("حقوق"),
    UTILITIES("آب و برق و گاز"),
    TRANSPORT("حمل‌ونقل"),
    REPAIRS("تعمیرات"),
    ADVERTISING("تبلیغات"),
    SUPPLIES("ملزومات"),
    SERVICES("خدمات"),
    OTHER("سایر");

    companion object {
        fun fromName(value: String?): ExpenseCategory =
            entries.firstOrNull { it.name == value } ?: OTHER
    }
}

enum class StockMovementType(val label: String) {
    OPENING("موجودی اولیه"),
    PURCHASE_IN("خرید"),
    SALE_OUT("فروش"),
    RETURN_IN("برگشت از فروش"),
    DEFECTIVE_RETURN("برگشت غیرقابل فروش"),
    MANUAL_IN("ورود دستی"),
    MANUAL_OUT("خروج دستی"),
    ADJUSTMENT("اصلاح موجودی");

    companion object {
        fun fromName(value: String?): StockMovementType =
            entries.firstOrNull { it.name == value } ?: ADJUSTMENT
    }
}

enum class AccountMovementType(val label: String) {
    SALE_PAYMENT("دریافت فروش"),
    CUSTOMER_RECEIPT("دریافت از مشتری"),
    EXPENSE_PAYMENT("پرداخت هزینه"),
    PURCHASE_PAYMENT("پرداخت خرید"),
    SUPPLIER_PAYMENT("پرداخت به تأمین‌کننده"),
    SALE_REFUND("بازپرداخت برگشت"),
    TRANSFER_IN("انتقال ورودی"),
    TRANSFER_OUT("انتقال خروجی"),
    ADJUSTMENT("اصلاح موجودی"),
    OPENING("مانده افتتاحیه"),
    OTHER("سایر");

    companion object {
        fun fromName(value: String?): AccountMovementType =
            entries.firstOrNull { it.name == value } ?: OTHER
    }
}

enum class ReminderType(val label: String) {
    CUSTOMER_DEBT("بدهی مشتری"),
    SUPPLIER_DUE("سررسید تأمین‌کننده"),
    LOW_STOCK("کالای کم‌موجود"),
    GENERAL("یادآوری عمومی");

    companion object {
        fun fromName(value: String?): ReminderType =
            entries.firstOrNull { it.name == value } ?: GENERAL
    }
}

enum class ReminderStatus(val label: String) {
    PENDING("در انتظار"),
    DONE("انجام‌شده"),
    CANCELLED("لغو‌شده");

    companion object {
        fun fromName(value: String?): ReminderStatus =
            entries.firstOrNull { it.name == value } ?: PENDING
    }
}

enum class PlanType(val label: String) {
    FREE("رایگان"),
    PRO("حرفه‌ای"),
    BUSINESS("تجاری");

    companion object {
        fun fromName(value: String?): PlanType =
            entries.firstOrNull { it.name == value } ?: FREE
    }
}

enum class SubscriptionStatus(val label: String) {
    ACTIVE("فعال"),
    EXPIRED("منقضی"),
    PENDING("در انتظار پرداخت"),
    NONE("بدون اشتراک");

    companion object {
        fun fromName(value: String?): SubscriptionStatus =
            entries.firstOrNull { it.name == value } ?: NONE
    }
}

/** How a sale is settled. */
enum class SalePaymentMode(val label: String) {
    CASH("نقدی"),
    CREDIT("نسیه"),
    MIXED("ترکیبی")
}

/** Result state of a financial operation. */
sealed interface OperationResult<out T> {
    data class Success<T>(val data: T) : OperationResult<T>
    data class Failure(val message: String) : OperationResult<Nothing>
}
