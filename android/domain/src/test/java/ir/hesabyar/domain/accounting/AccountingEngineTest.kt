package ir.hesabyar.domain.accounting

import ir.hesabyar.domain.model.Account
import ir.hesabyar.domain.model.AccountMovement
import ir.hesabyar.domain.model.AccountMovementType
import ir.hesabyar.domain.model.AccountType
import ir.hesabyar.domain.model.Contact
import ir.hesabyar.domain.model.ContactLedgerEntry
import ir.hesabyar.domain.model.ExpenseCategory
import ir.hesabyar.domain.model.OperationResult
import ir.hesabyar.domain.model.PaymentMethod
import ir.hesabyar.domain.model.PaymentStatus
import ir.hesabyar.domain.model.SalePaymentMode
import ir.hesabyar.domain.model.StockMovement
import ir.hesabyar.domain.model.StockMovementType
import ir.hesabyar.domain.model.Transaction
import ir.hesabyar.domain.model.TransactionDetail
import ir.hesabyar.domain.model.TransactionStatus
import ir.hesabyar.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountingEngineTest {

    private val now = 1_725_000_000_000L
    private val date = 1_724_995_200_000L

    private fun success(result: OperationResult<PostingPlan>): PostingPlan {
        assertTrue("expected success but was $result", result is OperationResult.Success)
        return (result as OperationResult.Success).data
    }

    private fun sale(
        mode: SalePaymentMode,
        amount: Long,
        paid: Long = 0,
        contactId: Long? = 10,
        averageCost: Long = 100_000
    ): OperationResult<PostingPlan> = AccountingEngine.sale(
        CreateSaleCommand(
            businessId = 1,
            operationId = "op-sale",
            date = date,
            items = listOf(
                SaleItemInput(
                    productId = 5,
                    productName = "کالای نمونه",
                    quantity = 1.0,
                    unitPrice = amount,
                    discount = 0,
                    averageCost = averageCost,
                    currentStock = 10.0
                )
            ),
            invoiceDiscount = 0,
            paymentMode = mode,
            paidAmount = paid,
            paymentMethod = PaymentMethod.CASH,
            accountId = 2,
            contactId = contactId,
            description = null,
            now = now
        )
    )

    @Test
    fun `cash sale increases cash and records sale`() {
        val plan = success(sale(SalePaymentMode.CASH, 200_000))
        assertEquals(TransactionType.CASH_SALE, plan.transaction.type)
        assertEquals(200_000, plan.transaction.paidAmount)
        assertEquals(0, plan.transaction.remainingAmount)
        assertEquals(200_000, plan.accountMovements.single().delta)
        assertTrue(plan.ledger.isEmpty())
        assertEquals(-1.0, plan.stockMovements.single().quantityChange, 0.0001)
        assertEquals(100_000, plan.sale!!.costAmount)
    }

    @Test
    fun `credit sale increases receivables without cash`() {
        val plan = success(sale(SalePaymentMode.CREDIT, 300_000))
        assertEquals(TransactionType.CREDIT_SALE, plan.transaction.type)
        assertEquals(0, plan.transaction.paidAmount)
        assertEquals(300_000, plan.transaction.remainingAmount)
        assertTrue(plan.accountMovements.isEmpty())
        assertEquals(300_000, plan.ledger.single().debit)
    }

    @Test
    fun `credit sale requires a contact`() {
        val result = sale(SalePaymentMode.CREDIT, 300_000, contactId = null)
        assertTrue(result is OperationResult.Failure)
    }

    @Test
    fun `receiving an old debt is not new income`() {
        val plan = success(
            AccountingEngine.receipt(
                ReceivePaymentCommand(
                    businessId = 1,
                    operationId = "op-receipt",
                    date = date,
                    contactId = 10,
                    amount = 100_000,
                    paymentMethod = PaymentMethod.CASH,
                    accountId = 2,
                    description = null,
                    now = now
                )
            )
        )
        assertEquals(TransactionType.CUSTOMER_RECEIPT, plan.transaction.type)
        assertEquals(100_000, plan.accountMovements.single().delta)
        assertEquals(100_000, plan.ledger.single().credit)
        assertNull(plan.sale)
    }

    @Test
    fun `purchase increases stock and is not an operating expense`() {
        val plan = success(
            AccountingEngine.purchase(
                CreatePurchaseCommand(
                    businessId = 1,
                    operationId = "op-purchase",
                    date = date,
                    items = listOf(
                        PurchaseItemInput(
                            productId = 5,
                            productName = "کالای نمونه",
                            quantity = 10.0,
                            unitPrice = 50_000,
                            currentStock = 0.0,
                            averageCost = 0
                        )
                    ),
                    discountAmount = 0,
                    freightAmount = 0,
                    freightToCost = true,
                    paidAmount = 500_000,
                    paymentMethod = PaymentMethod.CASH,
                    accountId = 2,
                    supplierId = 20,
                    description = null,
                    now = now
                )
            )
        )
        assertEquals(TransactionType.PURCHASE, plan.transaction.type)
        assertEquals(10.0, plan.stockUpdates.single().newQuantity, 0.0001)
        assertEquals(50_000, plan.stockUpdates.single().newAverageCost)
        assertEquals(-500_000, plan.accountMovements.single().delta)
        assertNull(plan.expense)
    }

    @Test
    fun `sale profit uses weighted average cost`() {
        val plan = success(sale(SalePaymentMode.CASH, 150_000, averageCost = 100_000))
        assertEquals(150_000, plan.sale!!.netAmount)
        assertEquals(100_000, plan.sale!!.costAmount)
        assertEquals(50_000, plan.sale!!.netAmount - plan.sale!!.costAmount)
    }

    @Test
    fun `paying a supplier debt is not a new expense`() {
        val plan = success(
            AccountingEngine.supplierPayment(
                PaySupplierCommand(
                    businessId = 1,
                    operationId = "op-pay",
                    date = date,
                    contactId = 20,
                    amount = 100_000,
                    paymentMethod = PaymentMethod.CASH,
                    accountId = 2,
                    description = null,
                    now = now
                )
            )
        )
        assertEquals(TransactionType.SUPPLIER_PAYMENT, plan.transaction.type)
        assertEquals(-100_000, plan.accountMovements.single().delta)
        assertEquals(100_000, plan.ledger.single().debit)
        assertNull(plan.expense)
    }

    @Test
    fun `mixed sale splits cash and receivable`() {
        val plan = success(sale(SalePaymentMode.MIXED, 500_000, paid = 200_000))
        assertEquals(TransactionType.MIXED_SALE, plan.transaction.type)
        assertEquals(200_000, plan.accountMovements.single().delta)
        assertEquals(300_000, plan.ledger.single().debit)
        assertEquals(PaymentStatus.PARTIAL, plan.transaction.paymentStatus)
    }

    @Test
    fun `invoice discount reduces net amount and receivable`() {
        val plan = success(
            AccountingEngine.sale(
                CreateSaleCommand(
                    businessId = 1,
                    operationId = "op-discount",
                    date = date,
                    items = listOf(
                        SaleItemInput(5, "کالا", 2.0, 100_000, 10_000, 60_000, 10.0)
                    ),
                    invoiceDiscount = 5_000,
                    paymentMode = SalePaymentMode.CREDIT,
                    paidAmount = 0,
                    paymentMethod = PaymentMethod.CASH,
                    accountId = 2,
                    contactId = 10,
                    description = null,
                    now = now
                )
            )
        )
        assertEquals(200_000, plan.sale!!.grossAmount)
        assertEquals(15_000, plan.sale!!.discountAmount)
        assertEquals(185_000, plan.sale!!.netAmount)
        assertEquals(185_000, plan.ledger.single().debit)
    }

    @Test
    fun `unpaid expense creates a payable instead of reducing cash`() {
        val plan = success(
            AccountingEngine.expense(
                CreateExpenseCommand(
                    businessId = 1,
                    operationId = "op-expense-unpaid",
                    date = date,
                    title = "حقوق کارگر",
                    category = ExpenseCategory.SALARY,
                    amount = 400_000,
                    paymentMethod = PaymentMethod.CASH,
                    accountId = null,
                    contactId = 30,
                    description = null,
                    imagePath = null,
                    isPaid = false,
                    now = now
                )
            )
        )
        assertTrue(plan.accountMovements.isEmpty())
        assertEquals(400_000, plan.ledger.single().credit)
        assertEquals(PaymentStatus.UNPAID, plan.transaction.paymentStatus)
    }

    @Test
    fun `paid expense reduces the account`() {
        val plan = success(
            AccountingEngine.expense(
                CreateExpenseCommand(
                    businessId = 1,
                    operationId = "op-expense-paid",
                    date = date,
                    title = "اجاره",
                    category = ExpenseCategory.RENT,
                    amount = 1_000_000,
                    paymentMethod = PaymentMethod.BANK_TRANSFER,
                    accountId = 2,
                    contactId = null,
                    description = null,
                    imagePath = null,
                    isPaid = true,
                    now = now
                )
            )
        )
        assertEquals(-1_000_000, plan.accountMovements.single().delta)
        assertTrue(plan.ledger.isEmpty())
    }

    @Test
    fun `transfer moves money without income or expense`() {
        val plan = success(
            AccountingEngine.transfer(
                TransferFundsCommand(
                    businessId = 1,
                    operationId = "op-transfer",
                    date = date,
                    fromAccountId = 2,
                    fromAccountTitle = "صندوق",
                    fromBalance = 1_000_000,
                    toAccountId = 3,
                    toAccountTitle = "بانک",
                    amount = 300_000,
                    note = null,
                    now = now
                )
            )
        )
        assertEquals(TransactionType.ACCOUNT_TRANSFER, plan.transaction.type)
        assertEquals(-300_000, plan.accountMovements[0].delta)
        assertEquals(300_000, plan.accountMovements[1].delta)
    }

    @Test
    fun `transfer cannot exceed the source balance`() {
        val result = AccountingEngine.transfer(
            TransferFundsCommand(
                businessId = 1,
                operationId = "op-transfer-2",
                date = date,
                fromAccountId = 2,
                fromAccountTitle = "صندوق",
                fromBalance = 100_000,
                toAccountId = 3,
                toAccountTitle = "بانک",
                amount = 300_000,
                note = null,
                now = now
            )
        )
        assertTrue(result is OperationResult.Failure)
    }

    @Test
    fun `cash adjustment records the difference`() {
        val plan = success(
            AccountingEngine.cashAdjustment(
                AdjustCashCommand(
                    businessId = 1,
                    operationId = "op-adjust",
                    date = date,
                    accountId = 2,
                    accountTitle = "صندوق",
                    currentBalance = 1_000_000,
                    actualBalance = 950_000,
                    reason = "کمبود صندوق",
                    now = now
                )
            )
        )
        assertEquals(-50_000, plan.accountMovements.single().delta)
        assertEquals(TransactionType.CASH_ADJUSTMENT, plan.transaction.type)
    }

    @Test
    fun `stock adjustment cannot go negative`() {
        val result = AccountingEngine.stockAdjustment(
            AdjustStockCommand(
                businessId = 1,
                operationId = "op-stock",
                date = date,
                productId = 5,
                productName = "کالا",
                currentStock = 2.0,
                averageCost = 10_000,
                delta = -3.0,
                reason = "شمارش انبار",
                now = now
            )
        )
        assertTrue(result is OperationResult.Failure)
    }

    @Test
    fun `sale return reverses revenue cost and stock proportionally`() {
        val plan = success(
            AccountingEngine.saleReturn(
                ReturnSaleCommand(
                    businessId = 1,
                    operationId = "op-return",
                    date = date,
                    saleTransactionId = 100,
                    saleId = 50,
                    saleRemaining = 0,
                    salePaid = 300_000,
                    items = listOf(
                        ReturnItemInput(
                            saleItemId = 500,
                            productId = 5,
                            productName = "کالا",
                            quantity = 1.0,
                            unitPrice = 150_000,
                            unitCost = 100_000,
                            lineDiscount = 10_000,
                            soldQuantity = 2.0,
                            alreadyReturned = 0.0,
                            currentStock = 9.0,
                            currentAverageCost = 100_000
                        )
                    ),
                    refundCash = true,
                    sellable = true,
                    reason = "کالای معیوب",
                    accountId = 2,
                    contactId = 10,
                    now = now
                )
            )
        )
        assertEquals(TransactionType.SALE_RETURN, plan.transaction.type)
        assertEquals(145_000, plan.transaction.amount)
        assertEquals(145_000, plan.transaction.paidAmount)
        assertEquals(-145_000, plan.accountMovements.single().delta)
        assertEquals(1.0, plan.stockMovements.single().quantityChange, 0.0001)
        assertEquals(100_000, plan.saleReturn!!.costReversed)
        assertEquals(1.0, plan.returnedQuantityUpdates.single().delta, 0.0001)
    }

    @Test
    fun `return quantity cannot exceed sold minus already returned`() {
        val result = AccountingEngine.saleReturn(
            ReturnSaleCommand(
                businessId = 1,
                operationId = "op-return-2",
                date = date,
                saleTransactionId = 100,
                saleId = 50,
                saleRemaining = 0,
                salePaid = 300_000,
                items = listOf(
                    ReturnItemInput(500, 5, "کالا", 2.0, 150_000, 100_000, 0, 2.0, 1.0, 10.0, 100_000)
                ),
                refundCash = true,
                sellable = true,
                reason = "برگشت",
                accountId = 2,
                contactId = 10,
                now = now
            )
        )
        assertTrue(result is OperationResult.Failure)
    }

    @Test
    fun `reversal negates every effect and keeps the original record`() {
        val original = Transaction(
            id = 77,
            businessId = 1,
            code = "T-14030512-0001",
            type = TransactionType.CASH_SALE,
            amount = 200_000,
            paidAmount = 200_000,
            remainingAmount = 0,
            contactId = 10,
            accountId = 2,
            date = date,
            description = "فروش",
            status = TransactionStatus.COMPLETED,
            paymentStatus = PaymentStatus.PAID,
            operationId = "op-1",
            createdAt = now,
            updatedAt = now
        )
        val detail = TransactionDetail(
            transaction = original,
            accountMovements = listOf(
                AccountMovement(
                    id = 1,
                    accountId = 2,
                    transactionId = 77,
                    type = AccountMovementType.SALE_PAYMENT,
                    delta = 200_000,
                    balanceAfter = 1_200_000,
                    createdAt = now
                )
            ),
            stockMovements = listOf(
                StockMovement(
                    id = 1,
                    productId = 5,
                    type = StockMovementType.SALE_OUT,
                    quantityChange = -1.0,
                    unitCost = 100_000,
                    referenceTransactionId = 77,
                    createdAt = now
                )
            )
        )
        val plan = success(
            AccountingEngine.reversal(
                ReverseTransactionCommand(
                    businessId = 1,
                    operationId = "op-reverse",
                    date = date,
                    detail = detail,
                    currentStocks = mapOf(5L to 9.0),
                    currentAverageCosts = mapOf(5L to 100_000L),
                    reason = "اشتباه در ثبت",
                    now = now
                )
            )
        )
        assertEquals(TransactionType.REVERSAL, plan.transaction.type)
        assertEquals(-200_000, plan.accountMovements.single().delta)
        assertEquals(1.0, plan.stockMovements.single().quantityChange, 0.0001)
        assertEquals(10.0, plan.stockUpdates.single().newQuantity, 0.0001)
        assertEquals(77L, plan.reversedTransactionId)
    }

    @Test
    fun `reversed transaction cannot be reversed again`() {
        val original = Transaction(
            id = 77,
            businessId = 1,
            code = "T-14030512-0001",
            type = TransactionType.CASH_SALE,
            amount = 200_000,
            paidAmount = 200_000,
            remainingAmount = 0,
            date = date,
            status = TransactionStatus.REVERSED,
            operationId = "op-1",
            createdAt = now,
            updatedAt = now
        )
        val result = AccountingEngine.reversal(
            ReverseTransactionCommand(
                businessId = 1,
                operationId = "op-reverse-2",
                date = date,
                detail = TransactionDetail(transaction = original),
                currentStocks = emptyMap(),
                currentAverageCosts = emptyMap(),
                reason = null,
                now = now
            )
        )
        assertTrue(result is OperationResult.Failure)
    }

    @Test
    fun `freight allocated to cost increases average cost`() {
        val plan = success(
            AccountingEngine.purchase(
                CreatePurchaseCommand(
                    businessId = 1,
                    operationId = "op-purchase-freight",
                    date = date,
                    items = listOf(PurchaseItemInput(5, "کالا", 10.0, 50_000, 0.0, 0)),
                    discountAmount = 0,
                    freightAmount = 50_000,
                    freightToCost = true,
                    paidAmount = 500_000,
                    paymentMethod = PaymentMethod.CASH,
                    accountId = 2,
                    supplierId = 20,
                    description = null,
                    now = now
                )
            )
        )
        assertEquals(55_000, plan.stockUpdates.single().newAverageCost)
        assertEquals(2, plan.accountMovements.size)
        assertEquals(-550_000, plan.accountMovements.sumOf { it.delta })
        assertNull(plan.expense)
    }

    @Test
    fun `freight outside cost becomes an operating expense`() {
        val plan = success(
            AccountingEngine.purchase(
                CreatePurchaseCommand(
                    businessId = 1,
                    operationId = "op-purchase-freight-2",
                    date = date,
                    items = listOf(PurchaseItemInput(5, "کالا", 10.0, 50_000, 0.0, 0)),
                    discountAmount = 0,
                    freightAmount = 50_000,
                    freightToCost = false,
                    paidAmount = 500_000,
                    paymentMethod = PaymentMethod.CASH,
                    accountId = 2,
                    supplierId = 20,
                    description = null,
                    now = now
                )
            )
        )
        assertEquals(50_000, plan.expense!!.amount)
        assertEquals(ExpenseCategory.TRANSPORT, plan.expense!!.category)
        assertEquals(50_000, plan.stockUpdates.single().newAverageCost)
    }
}
