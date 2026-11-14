package ir.hesabyar.data.repository

import androidx.room.withTransaction
import ir.hesabyar.core.database.HesabyarDatabase
import ir.hesabyar.core.database.dao.BusinessDao
import ir.hesabyar.core.database.dao.ProductDao
import ir.hesabyar.core.database.entity.ProductCategoryEntity
import ir.hesabyar.core.database.entity.ProductEntity
import ir.hesabyar.core.database.entity.StockMovementEntity
import ir.hesabyar.data.mapper.toDomain
import ir.hesabyar.data.posting.PostingExecutor
import ir.hesabyar.domain.accounting.AccountingEngine
import ir.hesabyar.domain.accounting.AdjustStockCommand
import ir.hesabyar.domain.model.OperationResult
import ir.hesabyar.domain.model.Product
import ir.hesabyar.domain.model.ProductCategory
import ir.hesabyar.domain.model.ProductFilter
import ir.hesabyar.domain.model.StockMovement
import ir.hesabyar.domain.model.StockMovementType
import ir.hesabyar.domain.repository.ProductRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

@Singleton
class ProductRepositoryImpl @Inject constructor(
    private val db: HesabyarDatabase,
    private val productDao: ProductDao,
    private val businessDao: BusinessDao,
    private val postingExecutor: PostingExecutor
) : ProductRepository {

    override fun observeProducts(filter: ProductFilter): Flow<List<Product>> =
        combine(productDao.observeAll(), productDao.observeCategories()) { products, categories ->
            val categoryNames = categories.associate { it.id to it.name }
            var list = products.map { it.toDomain(categoryNames[it.categoryId]) }
            val query = filter.query.trim()
            if (query.isNotEmpty()) {
                list = list.filter {
                    it.name.contains(query, ignoreCase = true) ||
                        it.code?.contains(query, ignoreCase = true) == true
                }
            }
            filter.categoryId?.let { categoryId -> list = list.filter { it.categoryId == categoryId } }
            if (filter.onlyLowStock) list = list.filter { it.isLowStock }
            list
        }

    override fun observeProduct(id: Long): Flow<Product?> =
        combine(productDao.observeById(id), productDao.observeCategories()) { entity, categories ->
            val categoryName = entity?.categoryId?.let { categoryId -> categories.firstOrNull { it.id == categoryId }?.name }
            entity?.toDomain(categoryName)
        }

    override suspend fun getProduct(id: Long): Product? {
        val entity = productDao.byId(id) ?: return null
        val categoryName = entity.categoryId?.let { categoryId ->
            productDao.getCategories().firstOrNull { it.id == categoryId }?.name
        }
        return entity.toDomain(categoryName)
    }

    override suspend fun getProducts(): List<Product> {
        val categories = productDao.getCategories().associate { it.id to it.name }
        return productDao.getAll().map { it.toDomain(categories[it.categoryId]) }
    }

    override suspend fun saveProduct(product: Product): Long = db.withTransaction {
        val now = System.currentTimeMillis()
        val businessId = product.businessId.takeIf { it > 0 }
            ?: businessDao.first()?.id
            ?: error("اطلاعات فروشگاه ثبت نشده است")
        if (product.id == 0L) {
            val averageCost = if (product.purchasePrice > 0) product.purchasePrice else product.averageCost
            val id = productDao.insert(
                ProductEntity(
                    businessId = businessId,
                    name = product.name.trim(),
                    code = product.code?.takeIf { it.isNotBlank() },
                    categoryId = product.categoryId,
                    purchasePrice = product.purchasePrice,
                    sellingPrice = product.sellingPrice,
                    averageCost = averageCost,
                    stockQuantity = product.stockQuantity,
                    minimumStock = product.minimumStock,
                    description = product.description?.takeIf { it.isNotBlank() },
                    createdAt = now,
                    updatedAt = now
                )
            )
            if (product.stockQuantity > 0) {
                productDao.insertMovement(
                    StockMovementEntity(
                        productId = id,
                        type = StockMovementType.OPENING.name,
                        quantityChange = product.stockQuantity,
                        unitCost = averageCost,
                        referenceTransactionId = null,
                        note = "موجودی اولیه کالا",
                        createdAt = now
                    )
                )
            }
            id
        } else {
            val existing = productDao.byId(product.id) ?: error("کالا یافت نشد")
            productDao.update(
                existing.copy(
                    name = product.name.trim(),
                    code = product.code?.takeIf { it.isNotBlank() },
                    categoryId = product.categoryId,
                    purchasePrice = product.purchasePrice,
                    sellingPrice = product.sellingPrice,
                    minimumStock = product.minimumStock,
                    description = product.description?.takeIf { it.isNotBlank() },
                    updatedAt = now
                )
            )
            product.id
        }
    }

    override fun observeCategories(): Flow<List<ProductCategory>> =
        productDao.observeCategories().map { rows -> rows.map { it.toDomain() } }

    override suspend fun getCategories(): List<ProductCategory> =
        productDao.getCategories().map { it.toDomain() }

    override suspend fun createCategory(name: String): Long = db.withTransaction {
        val businessId = businessDao.first()?.id ?: error("اطلاعات فروشگاه ثبت نشده است")
        val trimmed = name.trim()
        val existing = productDao.categoryIdByName(businessId, trimmed)
        existing ?: productDao.insertCategory(ProductCategoryEntity(businessId = businessId, name = trimmed))
    }

    override fun observeMovements(productId: Long): Flow<List<StockMovement>> =
        productDao.observeMovements(productId).map { rows -> rows.map { it.toDomain() } }

    override suspend fun adjustStock(cmd: AdjustStockCommand): OperationResult<Long> {
        val plan = AccountingEngine.stockAdjustment(cmd)
        return when (plan) {
            is OperationResult.Failure -> plan
            is OperationResult.Success -> postingExecutor.execute(plan.data)
        }
    }

    override suspend fun countProducts(): Int = productDao.count()

    override fun observeLowStockCount(): Flow<Int> = productDao.observeLowStockCount()
}
