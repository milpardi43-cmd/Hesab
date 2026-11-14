package ir.hesabyar.domain.usecase

import ir.hesabyar.domain.accounting.AdjustCashCommand
import ir.hesabyar.domain.accounting.AdjustStockCommand
import ir.hesabyar.domain.accounting.CreateExpenseCommand
import ir.hesabyar.domain.accounting.CreatePurchaseCommand
import ir.hesabyar.domain.accounting.CreateSaleCommand
import ir.hesabyar.domain.accounting.PaySupplierCommand
import ir.hesabyar.domain.accounting.ReceivePaymentCommand
import ir.hesabyar.domain.accounting.ReturnSaleCommand
import ir.hesabyar.domain.accounting.TransferFundsCommand
import ir.hesabyar.domain.model.OperationResult
import ir.hesabyar.domain.repository.AccountRepository
import ir.hesabyar.domain.repository.ContactRepository
import ir.hesabyar.domain.repository.ExpenseRepository
import ir.hesabyar.domain.repository.ProductRepository
import ir.hesabyar.domain.repository.PurchaseRepository
import ir.hesabyar.domain.repository.SaleRepository
import ir.hesabyar.domain.repository.TransactionRepository
import javax.inject.Inject

class CreateSaleUseCase @Inject constructor(private val repository: SaleRepository) {
    suspend operator fun invoke(command: CreateSaleCommand): OperationResult<Long> =
        repository.create(command)
}

class ReturnSaleUseCase @Inject constructor(private val repository: SaleRepository) {
    suspend operator fun invoke(command: ReturnSaleCommand): OperationResult<Long> =
        repository.returnSale(command)
}

class CreateExpenseUseCase @Inject constructor(private val repository: ExpenseRepository) {
    suspend operator fun invoke(command: CreateExpenseCommand): OperationResult<Long> =
        repository.create(command)
}

class CreatePurchaseUseCase @Inject constructor(private val repository: PurchaseRepository) {
    suspend operator fun invoke(command: CreatePurchaseCommand): OperationResult<Long> =
        repository.create(command)
}

class ReceivePaymentUseCase @Inject constructor(private val repository: ContactRepository) {
    suspend operator fun invoke(command: ReceivePaymentCommand): OperationResult<Long> =
        repository.receive(command)
}

class PaySupplierUseCase @Inject constructor(private val repository: ContactRepository) {
    suspend operator fun invoke(command: PaySupplierCommand): OperationResult<Long> =
        repository.pay(command)
}

class AdjustCashUseCase @Inject constructor(private val repository: AccountRepository) {
    suspend operator fun invoke(command: AdjustCashCommand): OperationResult<Long> =
        repository.adjustCash(command)
}

class TransferFundsUseCase @Inject constructor(private val repository: AccountRepository) {
    suspend operator fun invoke(command: TransferFundsCommand): OperationResult<Long> =
        repository.transfer(command)
}

class AdjustStockUseCase @Inject constructor(private val repository: ProductRepository) {
    suspend operator fun invoke(command: AdjustStockCommand): OperationResult<Long> =
        repository.adjustStock(command)
}

class ReverseTransactionUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    suspend operator fun invoke(transactionId: Long, reason: String?): OperationResult<Long> =
        repository.reverse(transactionId, reason)
}
