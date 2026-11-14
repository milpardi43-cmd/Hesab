package ir.hesabyar.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import ir.hesabyar.core.database.entity.ProductCategoryEntity
import ir.hesabyar.core.database.entity.ProductEntity
import ir.hesabyar.core.database.entity.StockMovementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Insert
    suspend fun insert(product: ProductEntity): Long

    @Update
    suspend fun update(product: ProductEntity)

    @Query(
        """
        UPDATE products
        SET stock_quantity = :quantity, average_cost = :averageCost,
            purchase_price = COALESCE(:purchasePrice, purchase_price), updated_at = :now
        WHERE id = :id
        """
    )
    suspend fun updateStock(id: Long, quantity: Double, averageCost: Long, purchasePrice: Long?, now: Long)

    @Query("SELECT * FROM products ORDER BY name")
    fun observeAll(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products ORDER BY name")
    suspend fun getAll(): List<ProductEntity>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun byId(id: Long): ProductEntity?

    @Query("SELECT * FROM products WHERE id = :id")
    fun observeById(id: Long): Flow<ProductEntity?>

    @Query("SELECT COUNT(*) FROM products")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM products WHERE minimum_stock > 0 AND stock_quantity <= minimum_stock")
    fun observeLowStockCount(): Flow<Int>

    @Query("SELECT CAST(COALESCE(SUM(stock_quantity * average_cost), 0) AS INTEGER) FROM products")
    suspend fun totalStockValue(): Long

    @Insert
    suspend fun insertCategory(category: ProductCategoryEntity): Long

    @Query("SELECT * FROM product_categories ORDER BY name")
    fun observeCategories(): Flow<List<ProductCategoryEntity>>

    @Query("SELECT * FROM product_categories ORDER BY name")
    suspend fun getCategories(): List<ProductCategoryEntity>

    @Query("SELECT id FROM product_categories WHERE business_id = :businessId AND name = :name LIMIT 1")
    suspend fun categoryIdByName(businessId: Long, name: String): Long?

    @Insert
    suspend fun insertMovement(movement: StockMovementEntity): Long

    @Query("SELECT * FROM stock_movements WHERE product_id = :productId ORDER BY created_at DESC, id DESC")
    fun observeMovements(productId: Long): Flow<List<StockMovementEntity>>

    @Query("SELECT * FROM stock_movements WHERE reference_transaction_id = :transactionId ORDER BY id")
    suspend fun movementsForTransaction(transactionId: Long): List<StockMovementEntity>
}
