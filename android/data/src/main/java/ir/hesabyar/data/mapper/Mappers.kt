package ir.hesabyar.data.mapper

import ir.hesabyar.core.database.dao.LedgerRow
import ir.hesabyar.core.database.dao.ReminderRow
import ir.hesabyar.core.database.dao.TransactionRow
import ir.hesabyar.core.database.entity.AccountEntity
import ir.hesabyar.core.database.entity.AccountMovementEntity
import ir.hesabyar.core.database.entity.BusinessEntity
import ir.hesabyar.core.database.entity.ContactEntity
import ir.hesabyar.core.database.entity.ContactLedgerEntity
import ir.hesabyar.core.database.entity.ExpenseEntity
import ir.hesabyar.core.database.entity.ProductCategoryEntity
import ir.hesabyar.core.database.entity.ProductEntity
import ir.hesabyar.core.database.entity.PurchaseEntity
import ir.hesabyar.core.database.entity.PurchaseItemEntity
import ir.hesabyar.core.database.entity.ReminderEntity
import ir.hesabyar.core.database.entity.SaleEntity
import ir.hesabyar.core.database.entity.SaleItemEntity
import ir.hesabyar.core.database.entity.SaleReturnEntity
import ir.hesabyar.core.database.entity.SaleReturnItemEntity
import ir.hesabyar.core.database.entity.StockMovementEntity
import ir.hesabyar.core.database.entity.SubscriptionEntity
import ir.hesabyar.core.database.entity.TransactionEntity
import ir.hesabyar.domain.model.Account
import ir.hesabyar.domain.model.AccountMovement
import ir.hesabyar.domain.model.AccountMovementType
import ir.hesabyar.domain.model.AccountType
import ir.hesabyar.domain.model.Business
import ir.hesabyar.domain.model.BusinessType
import ir.hesabyar.domain.model.Contact
import ir.hesabyar.domain.model.ContactLedgerEntry
import ir.hesabyar.domain.model.ContactType
import ir.hesabyar.domain.model.Expense
import ir.hesabyar.domain.model.ExpenseCategory
import ir.hesabyar.domain.model.PaymentMethod
import ir.hesabyar.domain.model.PaymentStatus
import ir.hesabyar.domain.model.PlanType
import ir.hesabyar.domain.model.Product
import ir.hesabyar.domain.model.ProductCategory
import ir.hesabyar.domain.model.Purchase
import ir.hesabyar.domain.model.PurchaseItem
import ir.hesabyar.domain.model.Reminder
import ir.hesabyar.domain.model.ReminderStatus
import ir.hesabyar.domain.model.ReminderType
import ir.hesabyar.domain.model.Sale
import ir.hesabyar.domain.model.SaleItem
import ir.hesabyar.domain.model.SaleReturn
import ir.hesabyar.domain.model.SaleReturnItem
import ir.hesabyar.domain.model.StockMovement
import ir.hesabyar.domain.model.StockMovementType
import ir.hesabyar.domain.model.Subscription
import ir.hesabyar.domain.model.SubscriptionStatus
import ir.hesabyar.domain.model.Transaction
import ir.hesabyar.domain.model.TransactionStatus
import ir.hesabyar.domain.model.TransactionType

fun BusinessEntity.toDomain() = Business(
    id = id,
    name = name,
    ownerName = ownerName,
    phone = phone,
    type = BusinessType.fromName(businessType),
    city = city,
    createdAt = createdAt
)

fun Business.toEntity() = BusinessEntity(
    id = id,
    name = name,
    ownerName = ownerName,
    phone = phone,
    businessType = type.name,
    city = city,
    createdAt = createdAt
)

fun AccountEntity.toDomain() = Account(
    id = id,
    businessId = businessId,
    title = title,
    type = AccountType.fromName(accountType),
    initialBalance = initialBalance,
    currentBalance = currentBalance,
    createdAt = createdAt
)

fun ContactEntity.toDomain() = Contact(
    id = id,
    businessId = businessId,
    name = name,
    phone = phone,
    type = ContactType.fromName(contactType),
    initialBalance = initialBalance,
    balance = balance,
    description = description,
    createdAt = createdAt
)

fun ProductCategoryEntity.toDomain() = ProductCategory(id = id, businessId = businessId, name = name)

fun ProductEntity.toDomain(categoryName: String? = null) = Product(
    id = id,
    businessId = businessId,
    name = name,
    code = code,
    categoryId = categoryId,
    categoryName = categoryName,
    purchasePrice = purchasePrice,
    sellingPrice = sellingPrice,
    averageCost = averageCost,
    stockQuantity = stockQuantity,
    minimumStock = minimumStock,
    description = description,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun TransactionEntity.toDomain(contactName: String? = null, accountTitle: String? = null) = Transaction(
    id = id,
    businessId = businessId,
    code = code,
    type = TransactionType.fromName(type),
    amount = amount,
    paidAmount = paidAmount,
    remainingAmount = remainingAmount,
    contactId = contactId,
    contactName = contactName,
    accountId = accountId,
    accountTitle = accountTitle,
    date = date,
    description = description,
    status = TransactionStatus.fromName(status),
    paymentStatus = PaymentStatus.fromName(paymentStatus),
    operationId = operationId,
    referenceId = referenceId,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun TransactionRow.toDomain() = transaction.toDomain(contactName, accountTitle)

fun SaleEntity.toDomain() = Sale(
    id = id,
    transactionId = transactionId,
    contactId = contactId,
    grossAmount = grossAmount,
    discountAmount = discountAmount,
    netAmount = netAmount,
    paidAmount = paidAmount,
    remainingAmount = remainingAmount,
    costAmount = costAmount,
    createdAt = createdAt
)

fun SaleItemEntity.toDomain() = SaleItem(
    id = id,
    saleId = saleId,
    productId = productId,
    productName = productName,
    quantity = quantity,
    unitPrice = unitPrice,
    unitCostSnapshot = unitCostSnapshot,
    discountAmount = discountAmount,
    totalAmount = totalAmount,
    returnedQuantity = returnedQuantity
)

fun PurchaseEntity.toDomain() = Purchase(
    id = id,
    transactionId = transactionId,
    supplierId = supplierId,
    grossAmount = grossAmount,
    discountAmount = discountAmount,
    netAmount = netAmount,
    paidAmount = paidAmount,
    remainingAmount = remainingAmount,
    freightAmount = freightAmount,
    freightToCost = freightToCost,
    createdAt = createdAt
)

fun PurchaseItemEntity.toDomain() = PurchaseItem(
    id = id,
    purchaseId = purchaseId,
    productId = productId,
    productName = productName,
    quantity = quantity,
    unitPrice = unitPrice,
    totalAmount = totalAmount
)

fun ExpenseEntity.toDomain() = Expense(
    id = id,
    transactionId = transactionId,
    title = title,
    category = ExpenseCategory.fromName(category),
    amount = amount,
    paymentMethod = PaymentMethod.fromName(paymentMethod),
    contactId = contactId,
    description = description,
    imagePath = imagePath,
    isPaid = isPaid,
    createdAt = createdAt
)

fun SaleReturnEntity.toDomain() = SaleReturn(
    id = id,
    transactionId = transactionId,
    saleId = saleId,
    cashRefund = cashRefund,
    creditReduction = creditReduction,
    costReversed = costReversed,
    reason = reason,
    sellable = sellable,
    createdAt = createdAt
)

fun SaleReturnItemEntity.toDomain() = SaleReturnItem(
    id = id,
    saleReturnId = saleReturnId,
    saleItemId = saleItemId,
    productId = productId,
    productName = productName,
    quantity = quantity,
    unitPrice = unitPrice,
    unitCost = unitCost,
    amount = amount
)

fun StockMovementEntity.toDomain() = StockMovement(
    id = id,
    productId = productId,
    type = StockMovementType.fromName(type),
    quantityChange = quantityChange,
    unitCost = unitCost,
    referenceTransactionId = referenceTransactionId,
    note = note,
    createdAt = createdAt
)

fun ContactLedgerEntity.toDomain() = ContactLedgerEntry(
    id = id,
    contactId = contactId,
    transactionId = transactionId,
    debit = debit,
    credit = credit,
    balanceAfter = balanceAfter,
    note = note,
    createdAt = createdAt
)

fun LedgerRow.toDomain() = entry.toDomain().copy(
    transactionCode = transactionCode,
    transactionType = transactionType?.let { TransactionType.fromName(it) }
)

fun AccountMovementEntity.toDomain() = AccountMovement(
    id = id,
    accountId = accountId,
    transactionId = transactionId,
    type = AccountMovementType.fromName(type),
    delta = delta,
    balanceAfter = balanceAfter,
    note = note,
    createdAt = createdAt
)

fun ReminderEntity.toDomain(contactName: String? = null) = Reminder(
    id = id,
    businessId = businessId,
    contactId = contactId,
    contactName = contactName,
    type = ReminderType.fromName(type),
    amount = amount,
    dueDate = dueDate,
    status = ReminderStatus.fromName(status),
    description = description,
    createdAt = createdAt
)

fun ReminderRow.toDomain() = reminder.toDomain(contactName)

fun SubscriptionEntity.toDomain() = Subscription(
    id = id,
    businessId = businessId,
    plan = PlanType.fromName(planType),
    startDate = startDate,
    endDate = endDate,
    status = SubscriptionStatus.fromName(status),
    paymentReference = paymentReference
)
