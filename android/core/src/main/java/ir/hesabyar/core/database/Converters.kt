package ir.hesabyar.core.database

import androidx.room.TypeConverter
import ir.hesabyar.domain.model.AccountMovementType
import ir.hesabyar.domain.model.AccountType
import ir.hesabyar.domain.model.BusinessType
import ir.hesabyar.domain.model.ContactType
import ir.hesabyar.domain.model.ExpenseCategory
import ir.hesabyar.domain.model.PaymentMethod
import ir.hesabyar.domain.model.PaymentStatus
import ir.hesabyar.domain.model.PlanType
import ir.hesabyar.domain.model.ReminderStatus
import ir.hesabyar.domain.model.ReminderType
import ir.hesabyar.domain.model.StockMovementType
import ir.hesabyar.domain.model.SubscriptionStatus
import ir.hesabyar.domain.model.TransactionStatus
import ir.hesabyar.domain.model.TransactionType

class Converters {
    @TypeConverter fun businessTypeToString(value: BusinessType): String = value.name
    @TypeConverter fun stringToBusinessType(value: String): BusinessType = BusinessType.fromName(value)

    @TypeConverter fun accountTypeToString(value: AccountType): String = value.name
    @TypeConverter fun stringToAccountType(value: String): AccountType = AccountType.fromName(value)

    @TypeConverter fun contactTypeToString(value: ContactType): String = value.name
    @TypeConverter fun stringToContactType(value: String): ContactType = ContactType.fromName(value)

    @TypeConverter fun transactionTypeToString(value: TransactionType): String = value.name
    @TypeConverter fun stringToTransactionType(value: String): TransactionType = TransactionType.fromName(value)

    @TypeConverter fun transactionStatusToString(value: TransactionStatus): String = value.name
    @TypeConverter fun stringToTransactionStatus(value: String): TransactionStatus = TransactionStatus.fromName(value)

    @TypeConverter fun paymentStatusToString(value: PaymentStatus?): String? = value?.name
    @TypeConverter fun stringToPaymentStatus(value: String?): PaymentStatus? = PaymentStatus.fromName(value)

    @TypeConverter fun paymentMethodToString(value: PaymentMethod): String = value.name
    @TypeConverter fun stringToPaymentMethod(value: String): PaymentMethod = PaymentMethod.fromName(value)

    @TypeConverter fun expenseCategoryToString(value: ExpenseCategory): String = value.name
    @TypeConverter fun stringToExpenseCategory(value: String): ExpenseCategory = ExpenseCategory.fromName(value)

    @TypeConverter fun stockMovementTypeToString(value: StockMovementType): String = value.name
    @TypeConverter fun stringToStockMovementType(value: String): StockMovementType = StockMovementType.fromName(value)

    @TypeConverter fun accountMovementTypeToString(value: AccountMovementType): String = value.name
    @TypeConverter fun stringToAccountMovementType(value: String): AccountMovementType = AccountMovementType.fromName(value)

    @TypeConverter fun reminderTypeToString(value: ReminderType): String = value.name
    @TypeConverter fun stringToReminderType(value: String): ReminderType = ReminderType.fromName(value)

    @TypeConverter fun reminderStatusToString(value: ReminderStatus): String = value.name
    @TypeConverter fun stringToReminderStatus(value: String): ReminderStatus = ReminderStatus.fromName(value)

    @TypeConverter fun planTypeToString(value: PlanType): String = value.name
    @TypeConverter fun stringToPlanType(value: String): PlanType = PlanType.fromName(value)

    @TypeConverter fun subscriptionStatusToString(value: SubscriptionStatus): String = value.name
    @TypeConverter fun stringToSubscriptionStatus(value: String): SubscriptionStatus = SubscriptionStatus.fromName(value)
}
