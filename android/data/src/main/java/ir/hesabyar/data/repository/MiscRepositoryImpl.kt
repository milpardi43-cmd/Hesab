package ir.hesabyar.data.repository

import ir.hesabyar.core.database.dao.ReminderDao
import ir.hesabyar.core.database.dao.SubscriptionDao
import ir.hesabyar.core.database.entity.ReminderEntity
import ir.hesabyar.core.database.entity.SubscriptionEntity
import ir.hesabyar.data.mapper.toDomain
import ir.hesabyar.domain.model.PlanType
import ir.hesabyar.domain.model.Reminder
import ir.hesabyar.domain.model.ReminderStatus
import ir.hesabyar.domain.model.Subscription
import ir.hesabyar.domain.model.SubscriptionStatus
import ir.hesabyar.domain.repository.ReminderRepository
import ir.hesabyar.domain.repository.SubscriptionRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class ReminderRepositoryImpl @Inject constructor(
    private val reminderDao: ReminderDao
) : ReminderRepository {

    override fun observeAll(): Flow<List<Reminder>> =
        reminderDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun save(reminder: Reminder): Long = reminderDao.insert(
        ReminderEntity(
            id = reminder.id,
            businessId = reminder.businessId,
            contactId = reminder.contactId,
            type = reminder.type.name,
            amount = reminder.amount,
            dueDate = reminder.dueDate,
            status = reminder.status.name,
            description = reminder.description,
            createdAt = if (reminder.createdAt == 0L) System.currentTimeMillis() else reminder.createdAt
        )
    )

    override suspend fun setStatus(id: Long, status: ReminderStatus) =
        reminderDao.updateStatus(id, status.name)

    override suspend fun pendingDue(now: Long): List<Reminder> =
        reminderDao.pendingDue(now).map { it.toDomain() }

    override suspend fun countPending(): Int = reminderDao.pendingCount()
}

@Singleton
class SubscriptionRepositoryImpl @Inject constructor(
    private val subscriptionDao: SubscriptionDao
) : SubscriptionRepository {

    override fun observeSubscription(): Flow<Subscription> =
        subscriptionDao.observeLatest().map { entity ->
            entity?.toDomain() ?: Subscription(
                businessId = 0,
                plan = PlanType.FREE,
                status = SubscriptionStatus.NONE
            )
        }

    override suspend fun currentPlan(): PlanType =
        subscriptionDao.latest()?.let { PlanType.fromName(it.planType) } ?: PlanType.FREE

    override suspend fun save(subscription: Subscription) {
        subscriptionDao.insert(
            SubscriptionEntity(
                businessId = subscription.businessId,
                planType = subscription.plan.name,
                startDate = subscription.startDate,
                endDate = subscription.endDate,
                status = subscription.status.name,
                paymentReference = subscription.paymentReference
            )
        )
    }
}
