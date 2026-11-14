package ir.hesabyar.domain.usecase

import ir.hesabyar.domain.model.Account
import ir.hesabyar.domain.model.AccountType
import ir.hesabyar.domain.model.AppSettings
import ir.hesabyar.domain.model.Business
import ir.hesabyar.domain.model.Contact
import ir.hesabyar.domain.model.OperationResult
import ir.hesabyar.domain.model.PlanLimits
import ir.hesabyar.domain.model.PlanType
import ir.hesabyar.domain.model.Product
import ir.hesabyar.domain.model.Reminder
import ir.hesabyar.domain.model.ReminderStatus
import ir.hesabyar.domain.model.Subscription
import ir.hesabyar.domain.model.SubscriptionStatus
import ir.hesabyar.domain.repository.AccountRepository
import ir.hesabyar.domain.repository.BackupRepository
import ir.hesabyar.domain.repository.BusinessRepository
import ir.hesabyar.domain.repository.ContactRepository
import ir.hesabyar.domain.repository.ProductRepository
import ir.hesabyar.domain.repository.ReminderRepository
import ir.hesabyar.domain.repository.SettingsRepository
import ir.hesabyar.domain.repository.SubscriptionRepository
import ir.hesabyar.domain.validation.Validate
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetBusinessUseCase @Inject constructor(private val repository: BusinessRepository) {
    operator fun invoke(): Flow<Business?> = repository.observeBusiness()
    suspend fun current(): Business? = repository.getBusiness()
    suspend fun hasBusiness(): Boolean = repository.hasBusiness()
}

class CreateBusinessUseCase @Inject constructor(private val repository: BusinessRepository) {
    suspend operator fun invoke(business: Business, cashBalance: Long): OperationResult<Long> {
        Validate.requiredName(business.name)?.let { return OperationResult.Failure(it) }
        if (cashBalance < 0) return OperationResult.Failure("موجودی اولیه صندوق نامعتبر است")
        return OperationResult.Success(repository.createBusiness(business, cashBalance))
    }
}

class UpdateBusinessUseCase @Inject constructor(private val repository: BusinessRepository) {
    suspend operator fun invoke(business: Business): OperationResult<Unit> {
        Validate.requiredName(business.name)?.let { return OperationResult.Failure(it) }
        Validate.optionalPhone(business.phone)?.let { return OperationResult.Failure(it) }
        repository.updateBusiness(business)
        return OperationResult.Success(Unit)
    }
}

class SaveContactUseCase @Inject constructor(private val repository: ContactRepository) {
    suspend operator fun invoke(contact: Contact): OperationResult<Long> {
        Validate.requiredName(contact.name)?.let { return OperationResult.Failure(it) }
        Validate.optionalPhone(contact.phone)?.let { return OperationResult.Failure(it) }
        return if (contact.id == 0L) {
            OperationResult.Success(repository.createContact(contact))
        } else {
            repository.updateContact(contact)
            OperationResult.Success(contact.id)
        }
    }
}

class AddOpeningBalanceUseCase @Inject constructor(private val repository: ContactRepository) {
    suspend operator fun invoke(contactId: Long, amount: Long, note: String?): OperationResult<Unit> {
        if (amount == 0L) return OperationResult.Failure("مبلغ مانده اولیه نامعتبر است")
        return repository.addOpeningBalance(contactId, amount, note)
    }
}

class SaveProductUseCase @Inject constructor(private val repository: ProductRepository) {
    suspend operator fun invoke(product: Product): OperationResult<Long> {
        Validate.requiredName(product.name)?.let { return OperationResult.Failure(it) }
        if (product.sellingPrice < 0 || product.purchasePrice < 0) {
            return OperationResult.Failure("قیمت‌ها نمی‌توانند منفی باشند")
        }
        if (product.minimumStock < 0 || product.stockQuantity < 0) {
            return OperationResult.Failure("موجودی نمی‌تواند منفی باشد")
        }
        return OperationResult.Success(repository.saveProduct(product))
    }
}

class CreateAccountUseCase @Inject constructor(private val repository: AccountRepository) {
    suspend operator fun invoke(title: String, type: AccountType, initialBalance: Long): OperationResult<Long> {
        if (title.isBlank()) return OperationResult.Failure("عنوان حساب را وارد کنید")
        if (initialBalance < 0) return OperationResult.Failure("موجودی اولیه نامعتبر است")
        return OperationResult.Success(repository.createAccount(title.trim(), type, initialBalance))
    }
}

class UpdateAccountUseCase @Inject constructor(private val repository: AccountRepository) {
    suspend operator fun invoke(account: Account): OperationResult<Unit> {
        if (account.title.isBlank()) return OperationResult.Failure("عنوان حساب را وارد کنید")
        repository.updateAccount(account)
        return OperationResult.Success(Unit)
    }
}

class GetSettingsUseCase @Inject constructor(private val repository: SettingsRepository) {
    operator fun invoke(): Flow<AppSettings> = repository.settings
    suspend fun current(): AppSettings = repository.current()
}

class UpdateSettingsUseCase @Inject constructor(private val repository: SettingsRepository) {
    suspend operator fun invoke(transform: (AppSettings) -> AppSettings) = repository.update(transform)
}

class ExportBackupUseCase @Inject constructor(private val repository: BackupRepository) {
    suspend operator fun invoke(): String = repository.exportJson()
}

class ImportBackupUseCase @Inject constructor(private val repository: BackupRepository) {
    suspend operator fun invoke(json: String): Result<Unit> = repository.importJson(json)
}

class ExportCsvUseCase @Inject constructor(private val repository: BackupRepository) {
    suspend operator fun invoke(): ByteArray = repository.exportCsvZip()
}

class ExportXlsxUseCase @Inject constructor(private val repository: BackupRepository) {
    suspend operator fun invoke(): ByteArray = repository.exportXlsx()
}

class GetRemindersUseCase @Inject constructor(private val repository: ReminderRepository) {
    operator fun invoke(): Flow<List<Reminder>> = repository.observeAll()
}

class SaveReminderUseCase @Inject constructor(private val repository: ReminderRepository) {
    suspend operator fun invoke(reminder: Reminder): OperationResult<Long> {
        if (reminder.dueDate <= 0) return OperationResult.Failure("تاریخ سررسید را انتخاب کنید")
        if (reminder.description.isNullOrBlank() && reminder.contactId == null) {
            return OperationResult.Failure("توضیحات یا شخص مرتبط را مشخص کنید")
        }
        return OperationResult.Success(repository.save(reminder))
    }
}

class CompleteReminderUseCase @Inject constructor(private val repository: ReminderRepository) {
    suspend operator fun invoke(id: Long, done: Boolean) =
        repository.setStatus(id, if (done) ReminderStatus.DONE else ReminderStatus.PENDING)
}

class GetSubscriptionUseCase @Inject constructor(private val repository: SubscriptionRepository) {
    operator fun invoke(): Flow<Subscription> = repository.observeSubscription()
    suspend fun plan(): PlanType = repository.currentPlan()
}

class GetPlanLimitsUseCase @Inject constructor() {
    operator fun invoke(plan: PlanType): PlanLimits = PlanLimits.of(plan)
}

class ActivateSubscriptionUseCase @Inject constructor(private val repository: SubscriptionRepository) {
    suspend operator fun invoke(plan: PlanType, months: Int = 1): Subscription {
        val businessId = 0L
        val now = System.currentTimeMillis()
        val end = now + months * 30L * 24 * 60 * 60 * 1000
        val subscription = Subscription(
            businessId = businessId,
            plan = plan,
            startDate = now,
            endDate = end,
            status = if (plan == PlanType.FREE) SubscriptionStatus.NONE else SubscriptionStatus.ACTIVE,
            paymentReference = if (plan == PlanType.FREE) null else "trial-$now"
        )
        repository.save(subscription)
        return subscription
    }
}

class CheckContactLimitUseCase @Inject constructor(
    private val contacts: ContactRepository,
    private val subscription: SubscriptionRepository
) {
    suspend operator fun invoke(): String? {
        val plan = subscription.currentPlan()
        val max = PlanLimits.of(plan).maxContacts ?: return null
        return if (contacts.countContacts() >= max) {
            "در نسخه رایگان حداکثر $max شخص قابل ثبت است. برای افزودن بیشتر، نسخه حرفه‌ای را فعال کنید."
        } else null
    }
}

class CheckProductLimitUseCase @Inject constructor(
    private val products: ProductRepository,
    private val subscription: SubscriptionRepository
) {
    suspend operator fun invoke(): String? {
        val plan = subscription.currentPlan()
        val max = PlanLimits.of(plan).maxProducts ?: return null
        return if (products.countProducts() >= max) {
            "در نسخه رایگان حداکثر $max کالا قابل ثبت است. برای افزودن بیشتر، نسخه حرفه‌ای را فعال کنید."
        } else null
    }
}
