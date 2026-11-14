package ir.hesabyar.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import ir.hesabyar.core.database.entity.ExpenseEntity
import ir.hesabyar.core.database.entity.PurchaseEntity
import ir.hesabyar.core.database.entity.PurchaseItemEntity
import ir.hesabyar.core.database.entity.SaleEntity
import ir.hesabyar.core.database.entity.SaleItemEntity
import ir.hesabyar.core.database.entity.SaleReturnEntity
import ir.hesabyar.core.database.entity.SaleReturnItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SaleDao {
    @Insert
    suspend fun insertSale(sale: SaleEntity): Long

    @Insert
    suspend fun insertSaleItems(items: List<SaleItemEntity>)

    @Query("SELECT * FROM sales WHERE transaction_id = :transactionId")
    suspend fun saleByTransaction(transactionId: Long): SaleEntity?

    @Query("SELECT * FROM sale_items WHERE sale_id = :saleId ORDER BY id")
    suspend fun items(saleId: Long): List<SaleItemEntity>

    @Query("SELECT * FROM sale_items WHERE id = :id")
    suspend fun item(id: Long): SaleItemEntity?

    @Query("UPDATE sale_items SET returned_quantity = returned_quantity + :delta WHERE id = :id")
    suspend fun addReturnedQuantity(id: Long, delta: Double)

    @Insert
    suspend fun insertReturn(saleReturn: SaleReturnEntity): Long

    @Insert
    suspend fun insertReturnItems(items: List<SaleReturnItemEntity>)

    @Query("SELECT * FROM sale_returns WHERE transaction_id = :transactionId")
    suspend fun returnByTransaction(transactionId: Long): SaleReturnEntity?

    @Query("SELECT * FROM sale_return_items WHERE sale_return_id = :saleReturnId ORDER BY id")
    suspend fun returnItems(saleReturnId: Long): List<SaleReturnItemEntity>
}

@Dao
interface PurchaseDao {
    @Insert
    suspend fun insertPurchase(purchase: PurchaseEntity): Long

    @Insert
    suspend fun insertItems(items: List<PurchaseItemEntity>)

    @Query("SELECT * FROM purchases WHERE transaction_id = :transactionId")
    suspend fun byTransaction(transactionId: Long): PurchaseEntity?

    @Query("SELECT * FROM purchase_items WHERE purchase_id = :purchaseId ORDER BY id")
    suspend fun items(purchaseId: Long): List<PurchaseItemEntity>
}

@Dao
interface ExpenseDao {
    @Insert
    suspend fun insert(expense: ExpenseEntity): Long

    @Query("SELECT * FROM expenses WHERE transaction_id = :transactionId")
    suspend fun byTransaction(transactionId: Long): ExpenseEntity?

    @Query(
        """
        SELECT t.*, c.name AS contact_name, a.title AS account_title
        FROM transactions t
        LEFT JOIN contacts c ON t.contact_id = c.id
        LEFT JOIN accounts a ON t.account_id = a.id
        WHERE t.type = 'EXPENSE'
        ORDER BY t.created_at DESC
        LIMIT :limit
        """
    )
    fun observeRecentExpenses(limit: Int): Flow<List<TransactionRow>>
}
