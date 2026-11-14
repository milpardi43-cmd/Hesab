package ir.hesabyar.domain.accounting

import ir.hesabyar.domain.model.ExpenseCategory
import ir.hesabyar.domain.model.PaymentMethod
import ir.hesabyar.domain.model.SalePaymentMode

/** Inputs for the accounting engine. Screens build these after validation. */

data class SaleItemInput(
    val productId: Long,
    val productName: String,
    val quantity: Double,
    val unitPrice: Long,
    val discount: Long,
    val averageCost: Long,
    val currentStock: Double
)

data class CreateSaleCommand(
    val businessId: Long,
    val operationId: String,
    val date: Long,
    val items: List<SaleItemInput>,
    val invoiceDiscount: Long,
    val paymentMode: SalePaymentMode,
    val paidAmount: Long,
    val paymentMethod: PaymentMethod,
    val accountId: Long?,
    val contactId: Long?,
    val description: String?,
    val now: Long
)

data class CreateExpenseCommand(
    val businessId: Long,
    val operationId: String,
    val date: Long,
    val title: String,
    val category: ExpenseCategory,
    val amount: Long,
    val paymentMethod: PaymentMethod,
    val accountId: Long?,
    val contactId: Long?,
    val description: String?,
    val imagePath: String?,
    val isPaid: Boolean,
    val now: Long
)

data class PurchaseItemInput(
    val productId: Long,
    val productName: String,
    val quantity: Double,
    val unitPrice: Long,
    val currentStock: Double,
    val averageCost: Long
)

data class CreatePurchaseCommand(
    val businessId: Long,
    val operationId: String,
    val date: Long,
    val items: List<PurchaseItemInput>,
    val discountAmount: Long,
    val freightAmount: Long,
    val freightToCost: Boolean,
    val paidAmount: Long,
    val paymentMethod: PaymentMethod,
    val accountId: Long?,
    val supplierId: Long?,
    val description: String?,
    val now: Long
)

data class ReceivePaymentCommand(
    val businessId: Long,
    val operationId: String,
    val date: Long,
    val contactId: Long,
    val amount: Long,
    val paymentMethod: PaymentMethod,
    val accountId: Long,
    val description: String?,
    val now: Long
)

data class PaySupplierCommand(
    val businessId: Long,
    val operationId: String,
    val date: Long,
    val contactId: Long,
    val amount: Long,
    val paymentMethod: PaymentMethod,
    val accountId: Long,
    val description: String?,
    val now: Long
)

data class ReturnItemInput(
    val saleItemId: Long,
    val productId: Long,
    val productName: String,
    val quantity: Double,
    val unitPrice: Long,
    val unitCost: Long,
    val lineDiscount: Long,
    val soldQuantity: Double,
    val alreadyReturned: Double,
    val currentStock: Double,
    val currentAverageCost: Long
)

data class ReturnSaleCommand(
    val businessId: Long,
    val operationId: String,
    val date: Long,
    val saleTransactionId: Long,
    val saleId: Long,
    val saleRemaining: Long,
    val salePaid: Long,
    val items: List<ReturnItemInput>,
    val refundCash: Boolean,
    val sellable: Boolean,
    val reason: String,
    val accountId: Long?,
    val contactId: Long?,
    val now: Long
)

data class TransferFundsCommand(
    val businessId: Long,
    val operationId: String,
    val date: Long,
    val fromAccountId: Long,
    val fromAccountTitle: String,
    val fromBalance: Long,
    val toAccountId: Long,
    val toAccountTitle: String,
    val amount: Long,
    val note: String?,
    val now: Long
)

data class AdjustCashCommand(
    val businessId: Long,
    val operationId: String,
    val date: Long,
    val accountId: Long,
    val accountTitle: String,
    val currentBalance: Long,
    val actualBalance: Long,
    val reason: String,
    val now: Long
)

data class AdjustStockCommand(
    val businessId: Long,
    val operationId: String,
    val date: Long,
    val productId: Long,
    val productName: String,
    val currentStock: Double,
    val averageCost: Long,
    val delta: Double,
    val reason: String,
    val now: Long
)

data class ReverseTransactionCommand(
    val businessId: Long,
    val operationId: String,
    val date: Long,
    val detail: ir.hesabyar.domain.model.TransactionDetail,
    val currentStocks: Map<Long, Double>,
    val currentAverageCosts: Map<Long, Long>,
    val reason: String?,
    val now: Long
)
