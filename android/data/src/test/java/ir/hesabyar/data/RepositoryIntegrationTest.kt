package ir.hesabyar.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import ir.hesabyar.core.database.HesabyarDatabase
import ir.hesabyar.data.posting.PostingExecutor
import ir.hesabyar.data.repository.AccountRepositoryImpl
import ir.hesabyar.data.repository.BusinessRepositoryImpl
import ir.hesabyar.data.repository.ContactRepositoryImpl
import ir.hesabyar.data.repository.DashboardRepositoryImpl
import ir.hesabyar.data.repository.ProductRepositoryImpl
import ir.hesabyar.data.repository.ReportRepositoryImpl
import ir.hesabyar.data.repository.SaleRepositoryImpl
import ir.hesabyar.data.repository.TransactionRepositoryImpl
import ir.hesabyar.domain.accounting.CreateSaleCommand
import ir.hesabyar.domain.accounting.ReceivePaymentCommand
import ir.hesabyar.domain.accounting.ReturnItemInput
import ir.hesabyar.domain.accounting.ReturnSaleCommand
import ir.hesabyar.domain.accounting.SaleItemInput
import ir.hesabyar.domain.calendar.JalaliDate
import ir.hesabyar.domain.model.AccountType
import ir.hesabyar.domain.model.Business
import ir.hesabyar.domain.model.Contact
import ir.hesabyar.domain.model.ContactType
import ir.hesabyar.domain.model.OperationResult
import ir.hesabyar.domain.model.PaymentMethod
import ir.hesabyar.domain.model.Product
import ir.hesabyar.domain.model.SalePaymentMode
import ir.hesabyar.domain.model.TransactionStatus
import ir.hesabyar.domain.util.OperationIds
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RepositoryIntegrationTest {

    private lateinit var db: HesabyarDatabase
    private lateinit var posting: PostingExecutor
    private lateinit var businessRepo: BusinessRepositoryImpl
    private lateinit var accountRepo: AccountRepositoryImpl
    private lateinit var contactRepo: ContactRepositoryImpl
    private lateinit var productRepo: ProductRepositoryImpl
    private lateinit var saleRepo: SaleRepositoryImpl
    private lateinit var dashboardRepo: DashboardRepositoryImpl
    private lateinit var reportRepo: ReportRepositoryImpl
    private lateinit var transactionRepo: TransactionRepositoryImpl

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, HesabyarDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        posting = PostingExecutor(
            db = db,
            businessDao = db.businessDao(),
            transactionDao = db.transactionDao(),
            accountDao = db.accountDao(),
            contactDao = db.contactDao(),
            productDao = db.productDao(),
            saleDao = db.saleDao(),
            purchaseDao = db.purchaseDao(),
            expenseDao = db.expenseDao()
        )
        businessRepo = BusinessRepositoryImpl(db, db.businessDao(), db.accountDao(), db.subscriptionDao())
        accountRepo = AccountRepositoryImpl(db, db.accountDao(), db.businessDao(), posting)
        contactRepo = ContactRepositoryImpl(db, db.contactDao(), posting)
        productRepo = ProductRepositoryImpl(db, db.productDao(), db.businessDao(), posting)
        saleRepo = SaleRepositoryImpl(db.transactionDao(), posting)
        dashboardRepo = DashboardRepositoryImpl(
            db.businessDao(), db.accountDao(), db.contactDao(), db.reportDao(),
            db.transactionDao(), db.productDao(), db.reminderDao()
        )
        reportRepo = ReportRepositoryImpl(db.reportDao(), db.productDao())
        transactionRepo = TransactionRepositoryImpl(
            db.transactionDao(), db.saleDao(), db.purchaseDao(), db.expenseDao(),
            db.contactDao(), db.accountDao(), db.productDao(), posting
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun saleFlowUpdatesCashContactStockAndReports() = runTest {
        val businessId = businessRepo.createBusiness(Business(name = "فروشگاه آفتاب"), 1_000_000)
        val cash = accountRepo.getFirstAccount(AccountType.CASH)
        assertNotNull(cash)

        val productId = productRepo.saveProduct(
            Product(
                businessId = businessId,
                name = "شکر",
                purchasePrice = 100_000,
                sellingPrice = 150_000,
                averageCost = 100_000,
                stockQuantity = 10.0
            )
        )
        val contactId = contactRepo.createContact(
            Contact(businessId = businessId, name = "علی رضایی", type = ContactType.CUSTOMER)
        )

        val today = JalaliDate.startOfDayMillis(System.currentTimeMillis())
        val now = System.currentTimeMillis()
        val sale = saleRepo.create(
            CreateSaleCommand(
                businessId = businessId,
                operationId = OperationIds.new(),
                date = today,
                items = listOf(
                    SaleItemInput(
                        productId = productId,
                        productName = "شکر",
                        quantity = 2.0,
                        unitPrice = 150_000,
                        discount = 0,
                        averageCost = 100_000,
                        currentStock = 10.0
                    )
                ),
                invoiceDiscount = 0,
                paymentMode = SalePaymentMode.MIXED,
                paidAmount = 100_000,
                paymentMethod = PaymentMethod.CASH,
                accountId = cash!!.id,
                contactId = contactId,
                description = "فروش تستی",
                now = now
            )
        )
        assertTrue(sale is OperationResult.Success)
        val saleId = (sale as OperationResult.Success).data

        assertEquals(1_100_000, accountRepo.getFirstAccount(AccountType.CASH)!!.currentBalance)
        assertEquals(200_000, contactRepo.getContact(contactId)!!.balance)
        assertEquals(8.0, productRepo.getProduct(productId)!!.stockQuantity, 0.001)

        val dashboard = dashboardRepo.observeDashboard().first()
        assertEquals(300_000, dashboard.todaySales)
        assertEquals(200_000, dashboard.receivable)
        assertEquals(1_100_000, dashboard.cashBalance)

        val daily = reportRepo.daily(today)
        assertEquals(1, daily.salesCount)
        assertEquals(300_000, daily.netSales)
        assertEquals(200_000, daily.costOfGoodsSold)
        assertEquals(100_000, daily.grossProfit)
        assertEquals(100_000, daily.cashReceived)
        assertEquals(200_000, daily.creditSales)

        val detail = transactionRepo.getDetail(saleId)
        assertNotNull(detail)
        assertEquals(1, detail!!.saleItems.size)
        assertEquals(1, detail.ledgerEntries.size)
        assertEquals(1, detail.accountMovements.size)

        val receipt = contactRepo.receive(
            ReceivePaymentCommand(
                businessId = businessId,
                operationId = OperationIds.new(),
                date = today,
                contactId = contactId,
                amount = 200_000,
                paymentMethod = PaymentMethod.CASH,
                accountId = cash.id,
                description = null,
                now = now
            )
        )
        assertTrue(receipt is OperationResult.Success)
        assertEquals(0, contactRepo.getContact(contactId)!!.balance)
        assertEquals(1_300_000, accountRepo.getFirstAccount(AccountType.CASH)!!.currentBalance)
    }

    @Test
    fun partialReturnRestoresStockAndReducesReceivable() = runTest {
        val businessId = businessRepo.createBusiness(Business(name = "فروشگاه تست"), 500_000)
        val cash = accountRepo.getFirstAccount(AccountType.CASH)!!
        val productId = productRepo.saveProduct(
            Product(
                businessId = businessId,
                name = "روغن",
                purchasePrice = 200_000,
                sellingPrice = 250_000,
                averageCost = 200_000,
                stockQuantity = 5.0
            )
        )
        val contactId = contactRepo.createContact(
            Contact(businessId = businessId, name = "مریم", type = ContactType.CUSTOMER)
        )
        val today = JalaliDate.startOfDayMillis(System.currentTimeMillis())
        val now = System.currentTimeMillis()
        val sale = saleRepo.create(
            CreateSaleCommand(
                businessId = businessId,
                operationId = OperationIds.new(),
                date = today,
                items = listOf(
                    SaleItemInput(productId, "روغن", 2.0, 250_000, 0, 200_000, 5.0)
                ),
                invoiceDiscount = 0,
                paymentMode = SalePaymentMode.CREDIT,
                paidAmount = 0,
                paymentMethod = PaymentMethod.CASH,
                accountId = null,
                contactId = contactId,
                description = null,
                now = now
            )
        ) as OperationResult.Success
        val detail = transactionRepo.getDetail(sale.data)!!
        val item = detail.saleItems.first()

        val returned = saleRepo.returnSale(
            ReturnSaleCommand(
                businessId = businessId,
                operationId = OperationIds.new(),
                date = today,
                saleTransactionId = sale.data,
                saleId = detail.sale!!.id,
                saleRemaining = detail.sale!!.remainingAmount,
                salePaid = detail.sale!!.paidAmount,
                items = listOf(
                    ReturnItemInput(
                        saleItemId = item.id,
                        productId = item.productId,
                        productName = item.productName,
                        quantity = 1.0,
                        unitPrice = item.unitPrice,
                        unitCost = item.unitCostSnapshot,
                        lineDiscount = item.discountAmount,
                        soldQuantity = item.quantity,
                        alreadyReturned = 0.0,
                        currentStock = 3.0,
                        currentAverageCost = 200_000
                    )
                ),
                refundCash = false,
                sellable = true,
                reason = "کالای اضافه",
                accountId = null,
                contactId = contactId,
                now = now + 1000
            )
        )
        assertTrue(returned is OperationResult.Success)

        assertEquals(4.0, productRepo.getProduct(productId)!!.stockQuantity, 0.001)
        assertEquals(250_000, contactRepo.getContact(contactId)!!.balance)
        assertEquals(500_000, accountRepo.getFirstAccount(AccountType.CASH)!!.currentBalance)

        val updatedDetail = transactionRepo.getDetail(sale.data)!!
        assertEquals(1.0, updatedDetail.saleItems.first().returnedQuantity, 0.001)
        assertTrue(updatedDetail.hasReturns)
    }

    @Test
    fun reversingSaleRestoresEverything() = runTest {
        val businessId = businessRepo.createBusiness(Business(name = "فروشگاه برگشت"), 300_000)
        val cash = accountRepo.getFirstAccount(AccountType.CASH)!!
        val productId = productRepo.saveProduct(
            Product(
                businessId = businessId,
                name = "چای",
                purchasePrice = 50_000,
                sellingPrice = 80_000,
                averageCost = 50_000,
                stockQuantity = 4.0
            )
        )
        val today = JalaliDate.startOfDayMillis(System.currentTimeMillis())
        val now = System.currentTimeMillis()
        val sale = saleRepo.create(
            CreateSaleCommand(
                businessId = businessId,
                operationId = OperationIds.new(),
                date = today,
                items = listOf(SaleItemInput(productId, "چای", 1.0, 80_000, 0, 50_000, 4.0)),
                invoiceDiscount = 0,
                paymentMode = SalePaymentMode.CASH,
                paidAmount = 80_000,
                paymentMethod = PaymentMethod.CASH,
                accountId = cash.id,
                contactId = null,
                description = null,
                now = now
            )
        ) as OperationResult.Success

        assertEquals(380_000, accountRepo.getFirstAccount(AccountType.CASH)!!.currentBalance)
        assertEquals(3.0, productRepo.getProduct(productId)!!.stockQuantity, 0.001)

        val reversed = transactionRepo.reverse(sale.data, "ثبت اشتباهی")
        assertTrue(reversed is OperationResult.Success)

        assertEquals(300_000, accountRepo.getFirstAccount(AccountType.CASH)!!.currentBalance)
        assertEquals(4.0, productRepo.getProduct(productId)!!.stockQuantity, 0.001)
        assertEquals(TransactionStatus.REVERSED, transactionRepo.getDetail(sale.data)!!.transaction.status)
    }
}
