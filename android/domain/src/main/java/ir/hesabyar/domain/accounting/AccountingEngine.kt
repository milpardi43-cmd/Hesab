package ir.hesabyar.domain.accounting

import ir.hesabyar.domain.model.AccountMovementType
import ir.hesabyar.domain.model.ExpenseCategory
import ir.hesabyar.domain.model.OperationResult
import ir.hesabyar.domain.model.PaymentStatus
import ir.hesabyar.domain.model.SalePaymentMode
import ir.hesabyar.domain.model.StockMovementType
import ir.hesabyar.domain.model.TransactionStatus
import ir.hesabyar.domain.model.TransactionType
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * The single source of truth for financial rules.
 *
 * The engine is pure: it receives a validated command plus the current state of products and
 * returns a PostingPlan describing every row that must be written. It never touches the database,
 * so the same rules are unit-tested and can later run unchanged on iOS.
 *
 * Money is Long (Toman) everywhere. Quantities are Double but every monetary derivation is rounded
 * once to Long, never accumulated as floating point.
 */
object AccountingEngine {

    fun sale(cmd: CreateSaleCommand): OperationResult<PostingPlan> {
        if (cmd.items.isEmpty()) return fail("حداقل یک کالا به فروش اضافه کنید")
        cmd.items.forEach { item ->
            if (item.quantity <= 0.0) return fail("تعداد «${item.productName}» باید بیشتر از صفر باشد")
            if (item.unitPrice < 0 || item.discount < 0) return fail("قیمت یا تخفیف «${item.productName}» نامعتبر است")
            if (item.discount > lineGross(item)) return fail("تخفیف «${item.productName}» از مبلغ آن بیشتر است")
        }
        if (cmd.invoiceDiscount < 0) return fail("تخفیف فاکتور نامعتبر است")

        val gross = cmd.items.sumOf { lineGross(it) }
        val lineDiscounts = cmd.items.sumOf { it.discount }
        val net = gross - lineDiscounts - cmd.invoiceDiscount
        if (net <= 0) return fail("مبلغ نهایی فروش باید بیشتر از صفر باشد")

        val paid = when (cmd.paymentMode) {
            SalePaymentMode.CASH -> net
            SalePaymentMode.CREDIT -> 0L
            SalePaymentMode.MIXED -> cmd.paidAmount
        }
        if (paid < 0 || paid > net) return fail("مبلغ دریافتی نامعتبر است")
        if (cmd.paymentMode == SalePaymentMode.CREDIT && cmd.contactId == null) {
            return fail("برای فروش نسیه انتخاب مشتری الزامی است")
        }
        if (cmd.paymentMode == SalePaymentMode.MIXED) {
            if (paid <= 0) return fail("در فروش ترکیبی مبلغ دریافتی باید بیشتر از صفر باشد")
            if (paid >= net) return fail("در فروش ترکیبی مبلغ دریافتی باید کمتر از کل باشد")
        }
        if (paid > 0 && cmd.accountId == null) return fail("حساب دریافت وجه را انتخاب کنید")
        val remaining = net - paid
        if (remaining > 0 && cmd.contactId == null) {
            return fail("برای ثبت بدهی، انتخاب مشتری الزامی است")
        }

        val type = when (cmd.paymentMode) {
            SalePaymentMode.CASH -> TransactionType.CASH_SALE
            SalePaymentMode.CREDIT -> TransactionType.CREDIT_SALE
            SalePaymentMode.MIXED -> TransactionType.MIXED_SALE
        }
        val paymentStatus = when {
            paid == net -> PaymentStatus.PAID
            paid == 0L -> PaymentStatus.UNPAID
            else -> PaymentStatus.PARTIAL
        }
        val cost = cmd.items.sumOf { round(it.quantity * it.averageCost) }

        val saleItems = cmd.items.map { item ->
            SaleItemDraft(
                productId = item.productId,
                productName = item.productName,
                quantity = item.quantity,
                unitPrice = item.unitPrice,
                unitCostSnapshot = item.averageCost,
                discountAmount = item.discount,
                totalAmount = lineGross(item) - item.discount
            )
        }

        val plan = PostingPlan(
            transaction = NewTransaction(
                code = "",
                type = type,
                amount = net,
                paidAmount = paid,
                remainingAmount = remaining,
                contactId = cmd.contactId,
                accountId = cmd.accountId,
                date = cmd.date,
                description = cmd.description?.takeIf { it.isNotBlank() } ?: type.label,
                status = TransactionStatus.COMPLETED,
                paymentStatus = paymentStatus,
                operationId = cmd.operationId,
                referenceId = null,
                createdAt = cmd.now
            ),
            accountMovements = if (paid > 0) listOf(
                AccountMovementDraft(cmd.accountId!!, paid, AccountMovementType.SALE_PAYMENT, "دریافت فروش")
            ) else emptyList(),
            ledger = if (remaining > 0 && cmd.contactId != null) listOf(
                LedgerDraft(cmd.contactId, debit = remaining, credit = 0, note = "فروش نسیه")
            ) else emptyList(),
            stockMovements = cmd.items.map { item ->
                StockMovementDraft(
                    productId = item.productId,
                    type = StockMovementType.SALE_OUT,
                    quantityChange = -item.quantity,
                    unitCost = item.averageCost,
                    note = "فروش"
                )
            },
            stockUpdates = cmd.items.map { item ->
                StockLevelUpdate(
                    productId = item.productId,
                    newQuantity = item.currentStock - item.quantity,
                    newAverageCost = item.averageCost
                )
            },
            sale = SaleDraft(
                contactId = cmd.contactId,
                grossAmount = gross,
                discountAmount = lineDiscounts + cmd.invoiceDiscount,
                netAmount = net,
                paidAmount = paid,
                remainingAmount = remaining,
                costAmount = cost
            ),
            saleItems = saleItems
        )
        return OperationResult.Success(plan)
    }

    fun expense(cmd: CreateExpenseCommand): OperationResult<PostingPlan> {
        if (cmd.title.isBlank()) return fail("عنوان هزینه را وارد کنید")
        if (cmd.amount <= 0) return fail("مبلغ هزینه باید بیشتر از صفر باشد")
        if (cmd.isPaid && cmd.accountId == null) return fail("حساب پرداخت را انتخاب کنید")
        if (!cmd.isPaid && cmd.contactId == null) return fail("برای هزینه پرداخت‌نشده، شخص بستانکار را انتخاب کنید")

        val plan = PostingPlan(
            transaction = NewTransaction(
                code = "",
                type = TransactionType.EXPENSE,
                amount = cmd.amount,
                paidAmount = if (cmd.isPaid) cmd.amount else 0,
                remainingAmount = if (cmd.isPaid) 0 else cmd.amount,
                contactId = cmd.contactId,
                accountId = cmd.accountId,
                date = cmd.date,
                description = cmd.description?.takeIf { it.isNotBlank() } ?: cmd.title,
                status = TransactionStatus.COMPLETED,
                paymentStatus = if (cmd.isPaid) PaymentStatus.PAID else PaymentStatus.UNPAID,
                operationId = cmd.operationId,
                referenceId = null,
                createdAt = cmd.now
            ),
            accountMovements = if (cmd.isPaid) listOf(
                AccountMovementDraft(cmd.accountId!!, -cmd.amount, AccountMovementType.EXPENSE_PAYMENT, cmd.title)
            ) else emptyList(),
            ledger = if (!cmd.isPaid) listOf(
                LedgerDraft(cmd.contactId!!, debit = 0, credit = cmd.amount, note = "هزینه پرداخت‌نشده: ${cmd.title}")
            ) else emptyList(),
            expense = ExpenseDraft(
                title = cmd.title,
                category = cmd.category,
                amount = cmd.amount,
                paymentMethod = cmd.paymentMethod,
                contactId = cmd.contactId,
                description = cmd.description,
                imagePath = cmd.imagePath,
                isPaid = cmd.isPaid
            )
        )
        return OperationResult.Success(plan)
    }

    fun purchase(cmd: CreatePurchaseCommand): OperationResult<PostingPlan> {
        if (cmd.items.isEmpty()) return fail("حداقل یک کالا به خرید اضافه کنید")
        cmd.items.forEach { item ->
            if (item.quantity <= 0.0) return fail("تعداد «${item.productName}» باید بیشتر از صفر باشد")
            if (item.unitPrice < 0) return fail("قیمت خرید «${item.productName}» نامعتبر است")
        }
        if (cmd.discountAmount < 0 || cmd.freightAmount < 0) return fail("تخفیف یا هزینه حمل نامعتبر است")

        val gross = cmd.items.sumOf { lineGross(it) }
        if (cmd.discountAmount > gross) return fail("تخفیف خرید از مبلغ خرید بیشتر است")
        val net = gross - cmd.discountAmount
        val paid = cmd.paidAmount
        if (paid < 0 || paid > net) return fail("مبلغ پرداختی نامعتبر است")
        val remaining = net - paid
        if (remaining > 0 && cmd.supplierId == null) return fail("برای خرید نسیه، تأمین‌کننده را انتخاب کنید")
        if ((paid > 0 || cmd.freightAmount > 0) && cmd.accountId == null) {
            return fail("حساب پرداخت را انتخاب کنید")
        }

        // Allocate freight to line cost proportionally to line value.
        val shares = LongArray(cmd.items.size)
        if (cmd.freightToCost && cmd.freightAmount > 0 && gross > 0) {
            var allocated = 0L
            cmd.items.forEachIndexed { index, item ->
                shares[index] = round(cmd.freightAmount.toDouble() * lineGross(item) / gross)
                allocated += shares[index]
            }
            val diff = cmd.freightAmount - allocated
            if (diff != 0L) shares[shares.lastIndex] += diff
        }

        val stockMovements = mutableListOf<StockMovementDraft>()
        val stockUpdates = mutableListOf<StockLevelUpdate>()
        cmd.items.forEachIndexed { index, item ->
            val share = shares[index]
            val lineCost = lineGross(item) + share
            val unitCost = round(lineCost.toDouble() / item.quantity)
            val currentStock = item.currentStock.coerceAtLeast(0.0)
            val newQuantity = currentStock + item.quantity
            val newAverage = round(
                (currentStock * item.averageCost + lineCost) / newQuantity
            )
            stockMovements += StockMovementDraft(
                productId = item.productId,
                type = StockMovementType.PURCHASE_IN,
                quantityChange = item.quantity,
                unitCost = unitCost,
                note = "خرید کالا"
            )
            stockUpdates += StockLevelUpdate(
                productId = item.productId,
                newQuantity = newQuantity,
                newAverageCost = newAverage,
                newPurchasePrice = item.unitPrice
            )
        }

        val movements = mutableListOf<AccountMovementDraft>()
        if (paid > 0) {
            movements += AccountMovementDraft(cmd.accountId!!, -paid, AccountMovementType.PURCHASE_PAYMENT, "پرداخت خرید")
        }
        if (cmd.freightAmount > 0) {
            movements += AccountMovementDraft(cmd.accountId!!, -cmd.freightAmount, AccountMovementType.EXPENSE_PAYMENT, "هزینه حمل خرید")
        }

        val paymentStatus = when {
            paid == net -> PaymentStatus.PAID
            paid == 0L -> PaymentStatus.UNPAID
            else -> PaymentStatus.PARTIAL
        }

        val plan = PostingPlan(
            transaction = NewTransaction(
                code = "",
                type = TransactionType.PURCHASE,
                amount = net,
                paidAmount = paid,
                remainingAmount = remaining,
                contactId = cmd.supplierId,
                accountId = cmd.accountId,
                date = cmd.date,
                description = cmd.description?.takeIf { it.isNotBlank() } ?: "خرید کالا",
                status = TransactionStatus.COMPLETED,
                paymentStatus = paymentStatus,
                operationId = cmd.operationId,
                referenceId = null,
                createdAt = cmd.now
            ),
            accountMovements = movements,
            ledger = if (remaining > 0) listOf(
                LedgerDraft(cmd.supplierId!!, debit = 0, credit = remaining, note = "خرید نسیه")
            ) else emptyList(),
            stockMovements = stockMovements,
            stockUpdates = stockUpdates,
            purchase = PurchaseDraft(
                supplierId = cmd.supplierId,
                grossAmount = gross,
                discountAmount = cmd.discountAmount,
                netAmount = net,
                paidAmount = paid,
                remainingAmount = remaining,
                freightAmount = cmd.freightAmount,
                freightToCost = cmd.freightToCost
            ),
            purchaseItems = cmd.items.map { item ->
                PurchaseItemDraft(
                    productId = item.productId,
                    productName = item.productName,
                    quantity = item.quantity,
                    unitPrice = item.unitPrice,
                    totalAmount = lineGross(item)
                )
            },
            expense = if (!cmd.freightToCost && cmd.freightAmount > 0) {
                ExpenseDraft(
                    title = "هزینه حمل خرید",
                    category = ExpenseCategory.TRANSPORT,
                    amount = cmd.freightAmount,
                    paymentMethod = cmd.paymentMethod,
                    contactId = null,
                    description = "هزینه حمل مرتبط با خرید کالا (خارج از بهای تمام‌شده)",
                    imagePath = null,
                    isPaid = true
                )
            } else null
        )
        return OperationResult.Success(plan)
    }

    fun receipt(cmd: ReceivePaymentCommand): OperationResult<PostingPlan> {
        if (cmd.amount <= 0) return fail("مبلغ دریافت باید بیشتر از صفر باشد")
        val plan = PostingPlan(
            transaction = NewTransaction(
                code = "",
                type = TransactionType.CUSTOMER_RECEIPT,
                amount = cmd.amount,
                paidAmount = cmd.amount,
                remainingAmount = 0,
                contactId = cmd.contactId,
                accountId = cmd.accountId,
                date = cmd.date,
                description = cmd.description?.takeIf { it.isNotBlank() } ?: "دریافت طلب",
                status = TransactionStatus.COMPLETED,
                paymentStatus = null,
                operationId = cmd.operationId,
                referenceId = null,
                createdAt = cmd.now
            ),
            accountMovements = listOf(
                AccountMovementDraft(cmd.accountId, cmd.amount, AccountMovementType.CUSTOMER_RECEIPT, "دریافت طلب")
            ),
            ledger = listOf(LedgerDraft(cmd.contactId, debit = 0, credit = cmd.amount, note = "دریافت وجه"))
        )
        return OperationResult.Success(plan)
    }

    fun supplierPayment(cmd: PaySupplierCommand): OperationResult<PostingPlan> {
        if (cmd.amount <= 0) return fail("مبلغ پرداخت باید بیشتر از صفر باشد")
        val plan = PostingPlan(
            transaction = NewTransaction(
                code = "",
                type = TransactionType.SUPPLIER_PAYMENT,
                amount = cmd.amount,
                paidAmount = cmd.amount,
                remainingAmount = 0,
                contactId = cmd.contactId,
                accountId = cmd.accountId,
                date = cmd.date,
                description = cmd.description?.takeIf { it.isNotBlank() } ?: "پرداخت بدهی",
                status = TransactionStatus.COMPLETED,
                paymentStatus = null,
                operationId = cmd.operationId,
                referenceId = null,
                createdAt = cmd.now
            ),
            accountMovements = listOf(
                AccountMovementDraft(cmd.accountId, -cmd.amount, AccountMovementType.SUPPLIER_PAYMENT, "پرداخت بدهی")
            ),
            ledger = listOf(LedgerDraft(cmd.contactId, debit = cmd.amount, credit = 0, note = "پرداخت وجه"))
        )
        return OperationResult.Success(plan)
    }

    fun saleReturn(cmd: ReturnSaleCommand): OperationResult<PostingPlan> {
        if (cmd.items.isEmpty()) return fail("حداقل یک قلم برای برگشت انتخاب کنید")
        cmd.items.forEach { item ->
            val available = item.soldQuantity - item.alreadyReturned
            if (item.quantity <= 0.0) return fail("تعداد برگشتی «${item.productName}» نامعتبر است")
            if (item.quantity > available + 1e-9) {
                return fail("تعداد برگشتی «${item.productName}» از تعداد فروش رفته بیشتر است")
            }
        }
        if (cmd.reason.isBlank()) return fail("علت برگشت را وارد کنید")

        val returnItems = cmd.items.map { item ->
            val gross = round(item.quantity * item.unitPrice)
            val discountShare = if (item.soldQuantity > 0) {
                round(item.lineDiscount * item.quantity / item.soldQuantity)
            } else 0L
            ReturnItemDraft(
                saleItemId = item.saleItemId,
                productId = item.productId,
                productName = item.productName,
                quantity = item.quantity,
                unitPrice = item.unitPrice,
                unitCost = item.unitCost,
                amount = gross - discountShare
            )
        }
        val total = returnItems.sumOf { it.amount }
        if (total <= 0) return fail("مبلغ برگشتی نامعتبر است")

        val debtReduction = if (cmd.refundCash) minOf(cmd.saleRemaining, total) else total
        val cashRefund = if (cmd.refundCash) total - debtReduction else 0L
        if (cashRefund > 0 && cmd.accountId == null) return fail("حساب بازپرداخت را انتخاب کنید")
        if (debtReduction > 0 && cmd.contactId == null) return fail("برای کاهش بدهی، مشتری فروش را مشخص کنید")

        val costReversed = cmd.items.sumOf { round(it.quantity * it.unitCost) }

        val stockMovements = mutableListOf<StockMovementDraft>()
        val stockUpdates = mutableListOf<StockLevelUpdate>()
        cmd.items.forEach { item ->
            if (cmd.sellable) {
                val newQuantity = item.currentStock + item.quantity
                val newAverage = if (newQuantity > 0) {
                    round(
                        (item.currentStock.coerceAtLeast(0.0) * item.currentAverageCost +
                            item.quantity * item.unitCost) / newQuantity
                    )
                } else item.currentAverageCost
                stockMovements += StockMovementDraft(
                    productId = item.productId,
                    type = StockMovementType.RETURN_IN,
                    quantityChange = item.quantity,
                    unitCost = item.unitCost,
                    note = "برگشت از فروش"
                )
                stockUpdates += StockLevelUpdate(
                    productId = item.productId,
                    newQuantity = newQuantity,
                    newAverageCost = newAverage
                )
            } else {
                stockMovements += StockMovementDraft(
                    productId = item.productId,
                    type = StockMovementType.DEFECTIVE_RETURN,
                    quantityChange = 0.0,
                    unitCost = item.unitCost,
                    note = "برگشت غیرقابل فروش: ${item.productName}"
                )
            }
        }

        val plan = PostingPlan(
            transaction = NewTransaction(
                code = "",
                type = TransactionType.SALE_RETURN,
                amount = total,
                paidAmount = cashRefund,
                remainingAmount = debtReduction,
                contactId = cmd.contactId,
                accountId = cmd.accountId,
                date = cmd.date,
                description = cmd.reason,
                status = TransactionStatus.COMPLETED,
                paymentStatus = null,
                operationId = cmd.operationId,
                referenceId = cmd.saleTransactionId,
                createdAt = cmd.now
            ),
            accountMovements = if (cashRefund > 0) listOf(
                AccountMovementDraft(cmd.accountId!!, -cashRefund, AccountMovementType.SALE_REFUND, "بازپرداخت برگشت")
            ) else emptyList(),
            ledger = if (debtReduction > 0 && cmd.contactId != null) listOf(
                LedgerDraft(cmd.contactId, debit = 0, credit = debtReduction, note = "برگشت از فروش")
            ) else emptyList(),
            stockMovements = stockMovements,
            stockUpdates = stockUpdates,
            saleReturn = SaleReturnDraft(
                saleId = cmd.saleId,
                cashRefund = cashRefund,
                creditReduction = debtReduction,
                costReversed = costReversed,
                reason = cmd.reason,
                sellable = cmd.sellable,
                items = returnItems
            ),
            returnedQuantityUpdates = cmd.items.map {
                ReturnedQuantityUpdate(it.saleItemId, it.quantity)
            }
        )
        return OperationResult.Success(plan)
    }

    fun transfer(cmd: TransferFundsCommand): OperationResult<PostingPlan> {
        if (cmd.fromAccountId == cmd.toAccountId) return fail("حساب مبدأ و مقصد باید متفاوت باشند")
        if (cmd.amount <= 0) return fail("مبلغ انتقال باید بیشتر از صفر باشد")
        if (cmd.amount > cmd.fromBalance) return fail("موجودی حساب مبدأ کافی نیست")
        val plan = PostingPlan(
            transaction = NewTransaction(
                code = "",
                type = TransactionType.ACCOUNT_TRANSFER,
                amount = cmd.amount,
                paidAmount = 0,
                remainingAmount = 0,
                contactId = null,
                accountId = cmd.fromAccountId,
                date = cmd.date,
                description = cmd.note?.takeIf { it.isNotBlank() }
                    ?: "انتقال از ${cmd.fromAccountTitle} به ${cmd.toAccountTitle}",
                status = TransactionStatus.COMPLETED,
                paymentStatus = null,
                operationId = cmd.operationId,
                referenceId = null,
                createdAt = cmd.now
            ),
            accountMovements = listOf(
                AccountMovementDraft(cmd.fromAccountId, -cmd.amount, AccountMovementType.TRANSFER_OUT, "انتقال به ${cmd.toAccountTitle}"),
                AccountMovementDraft(cmd.toAccountId, cmd.amount, AccountMovementType.TRANSFER_IN, "انتقال از ${cmd.fromAccountTitle}")
            )
        )
        return OperationResult.Success(plan)
    }

    fun cashAdjustment(cmd: AdjustCashCommand): OperationResult<PostingPlan> {
        if (cmd.actualBalance < 0) return fail("موجودی واقعی نمی‌تواند منفی باشد")
        if (cmd.reason.isBlank()) return fail("دلیل اصلاح موجودی را وارد کنید")
        val delta = cmd.actualBalance - cmd.currentBalance
        if (delta == 0L) return fail("موجودی تغییری نکرده است")
        val plan = PostingPlan(
            transaction = NewTransaction(
                code = "",
                type = TransactionType.CASH_ADJUSTMENT,
                amount = abs(delta),
                paidAmount = 0,
                remainingAmount = 0,
                contactId = null,
                accountId = cmd.accountId,
                date = cmd.date,
                description = cmd.reason,
                status = TransactionStatus.COMPLETED,
                paymentStatus = null,
                operationId = cmd.operationId,
                referenceId = null,
                createdAt = cmd.now
            ),
            accountMovements = listOf(
                AccountMovementDraft(cmd.accountId, delta, AccountMovementType.ADJUSTMENT, cmd.reason)
            )
        )
        return OperationResult.Success(plan)
    }

    fun stockAdjustment(cmd: AdjustStockCommand): OperationResult<PostingPlan> {
        if (cmd.delta == 0.0) return fail("تعداد موجودی تغییری نکرده است")
        if (cmd.reason.isBlank()) return fail("دلیل اصلاح موجودی را وارد کنید")
        val newQuantity = cmd.currentStock + cmd.delta
        if (newQuantity < -1e-9) return fail("موجودی کالا نمی‌تواند منفی شود")
        val plan = PostingPlan(
            transaction = NewTransaction(
                code = "",
                type = TransactionType.STOCK_ADJUSTMENT,
                amount = abs(round(cmd.delta * cmd.averageCost)),
                paidAmount = 0,
                remainingAmount = 0,
                contactId = null,
                accountId = null,
                date = cmd.date,
                description = "اصلاح موجودی ${cmd.productName}: ${cmd.reason}",
                status = TransactionStatus.COMPLETED,
                paymentStatus = null,
                operationId = cmd.operationId,
                referenceId = null,
                createdAt = cmd.now
            ),
            stockMovements = listOf(
                StockMovementDraft(
                    productId = cmd.productId,
                    type = StockMovementType.ADJUSTMENT,
                    quantityChange = cmd.delta,
                    unitCost = cmd.averageCost,
                    note = cmd.reason
                )
            ),
            stockUpdates = listOf(
                StockLevelUpdate(
                    productId = cmd.productId,
                    newQuantity = newQuantity,
                    newAverageCost = cmd.averageCost
                )
            )
        )
        return OperationResult.Success(plan)
    }

    /**
     * Reversal ("عملیات معکوس") negates every effect of a completed transaction and records an
     * audit transaction. The original record is never deleted.
     */
    fun reversal(cmd: ReverseTransactionCommand): OperationResult<PostingPlan> {
        val detail = cmd.detail
        val original = detail.transaction
        if (original.status != TransactionStatus.COMPLETED) return fail("این تراکنش قبلاً برگشت خورده است")
        if (original.type == TransactionType.REVERSAL) return fail("تراکنش معکوس قابل برگشت نیست")
        if (detail.hasReturns) return fail("برای این فروش برگشت ثبت شده است؛ ابتدا برگشت‌ها را اصلاح کنید")

        val movements = detail.accountMovements
            .filter { it.delta != 0L }
            .map {
                AccountMovementDraft(
                    accountId = it.accountId,
                    delta = -it.delta,
                    type = AccountMovementType.ADJUSTMENT,
                    note = "معکوس تراکنش ${original.code}"
                )
            }
        val ledger = detail.ledgerEntries
            .filter { it.debit != 0L || it.credit != 0L }
            .map {
                LedgerDraft(
                    contactId = it.contactId,
                    debit = it.credit,
                    credit = it.debit,
                    note = "معکوس تراکنش ${original.code}"
                )
            }
        val stockMovements = detail.stockMovements
            .filter { it.quantityChange != 0.0 }
            .map {
                StockMovementDraft(
                    productId = it.productId,
                    type = StockMovementType.ADJUSTMENT,
                    quantityChange = -it.quantityChange,
                    unitCost = it.unitCost,
                    note = "معکوس تراکنش ${original.code}"
                )
            }
        val stockDeltas = stockMovements.groupBy { it.productId }
            .mapValues { entry -> entry.value.sumOf { it.quantityChange } }
        val stockUpdates = stockDeltas.map { (productId, delta) ->
            StockLevelUpdate(
                productId = productId,
                newQuantity = (cmd.currentStocks[productId] ?: 0.0) + delta,
                newAverageCost = cmd.currentAverageCosts[productId] ?: 0
            )
        }
        val returnedUpdates = detail.returnItems.map {
            ReturnedQuantityUpdate(it.saleItemId, -it.quantity)
        }

        val plan = PostingPlan(
            transaction = NewTransaction(
                code = "",
                type = TransactionType.REVERSAL,
                amount = original.amount,
                paidAmount = 0,
                remainingAmount = 0,
                contactId = original.contactId,
                accountId = original.accountId,
                date = cmd.date,
                description = cmd.reason?.takeIf { it.isNotBlank() }
                    ?: "معکوس تراکنش ${original.code}",
                status = TransactionStatus.COMPLETED,
                paymentStatus = null,
                operationId = cmd.operationId,
                referenceId = original.id,
                createdAt = cmd.now
            ),
            accountMovements = movements,
            ledger = ledger,
            stockMovements = stockMovements,
            stockUpdates = stockUpdates,
            returnedQuantityUpdates = returnedUpdates,
            reversedTransactionId = original.id
        )
        return OperationResult.Success(plan)
    }

    // ------------------------------------------------------------------ helpers

    private fun lineGross(item: SaleItemInput): Long = round(item.quantity * item.unitPrice)

    private fun lineGross(item: PurchaseItemInput): Long = round(item.quantity * item.unitPrice)

    private fun round(value: Double): Long = value.roundToLong()

    private fun <T> fail(message: String): OperationResult<T> = OperationResult.Failure(message)
}
