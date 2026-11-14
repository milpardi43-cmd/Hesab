package ir.hesabyar.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
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

/**
 * Full-database access used only by backup/restore and CSV/Excel export.
 * Restore runs inside one Room transaction and validates the payload first.
 */
@Dao
interface BackupExportDao {
    @Query("SELECT * FROM businesses") suspend fun businesses(): List<BusinessEntity>
    @Query("SELECT * FROM accounts") suspend fun accounts(): List<AccountEntity>
    @Query("SELECT * FROM contacts") suspend fun contacts(): List<ContactEntity>
    @Query("SELECT * FROM product_categories") suspend fun categories(): List<ProductCategoryEntity>
    @Query("SELECT * FROM products") suspend fun products(): List<ProductEntity>
    @Query("SELECT * FROM transactions") suspend fun transactions(): List<TransactionEntity>
    @Query("SELECT * FROM sales") suspend fun sales(): List<SaleEntity>
    @Query("SELECT * FROM sale_items") suspend fun saleItems(): List<SaleItemEntity>
    @Query("SELECT * FROM purchases") suspend fun purchases(): List<PurchaseEntity>
    @Query("SELECT * FROM purchase_items") suspend fun purchaseItems(): List<PurchaseItemEntity>
    @Query("SELECT * FROM expenses") suspend fun expenses(): List<ExpenseEntity>
    @Query("SELECT * FROM sale_returns") suspend fun saleReturns(): List<SaleReturnEntity>
    @Query("SELECT * FROM sale_return_items") suspend fun saleReturnItems(): List<SaleReturnItemEntity>
    @Query("SELECT * FROM stock_movements") suspend fun stockMovements(): List<StockMovementEntity>
    @Query("SELECT * FROM contact_ledger") suspend fun contactLedger(): List<ContactLedgerEntity>
    @Query("SELECT * FROM account_movements") suspend fun accountMovements(): List<AccountMovementEntity>
    @Query("SELECT * FROM reminders") suspend fun reminders(): List<ReminderEntity>
    @Query("SELECT * FROM subscriptions") suspend fun subscriptions(): List<SubscriptionEntity>

    @Insert suspend fun insertBusinesses(rows: List<BusinessEntity>)
    @Insert suspend fun insertAccounts(rows: List<AccountEntity>)
    @Insert suspend fun insertContacts(rows: List<ContactEntity>)
    @Insert suspend fun insertCategories(rows: List<ProductCategoryEntity>)
    @Insert suspend fun insertProducts(rows: List<ProductEntity>)
    @Insert suspend fun insertTransactions(rows: List<TransactionEntity>)
    @Insert suspend fun insertSales(rows: List<SaleEntity>)
    @Insert suspend fun insertSaleItems(rows: List<SaleItemEntity>)
    @Insert suspend fun insertPurchases(rows: List<PurchaseEntity>)
    @Insert suspend fun insertPurchaseItems(rows: List<PurchaseItemEntity>)
    @Insert suspend fun insertExpenses(rows: List<ExpenseEntity>)
    @Insert suspend fun insertSaleReturns(rows: List<SaleReturnEntity>)
    @Insert suspend fun insertSaleReturnItems(rows: List<SaleReturnItemEntity>)
    @Insert suspend fun insertStockMovements(rows: List<StockMovementEntity>)
    @Insert suspend fun insertContactLedger(rows: List<ContactLedgerEntity>)
    @Insert suspend fun insertAccountMovements(rows: List<AccountMovementEntity>)
    @Insert suspend fun insertReminders(rows: List<ReminderEntity>)
    @Insert suspend fun insertSubscriptions(rows: List<SubscriptionEntity>)
}
