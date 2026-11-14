package ir.hesabyar.data.repository

import android.content.Context
import androidx.room.withTransaction
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.hesabyar.core.database.HesabyarDatabase
import ir.hesabyar.core.database.dao.BackupDao
import ir.hesabyar.core.database.dao.BackupExportDao
import ir.hesabyar.core.database.entity.BackupEntity
import ir.hesabyar.core.export.XlsxWriter
import ir.hesabyar.data.backup.BackupFile
import ir.hesabyar.data.backup.BackupPayload
import ir.hesabyar.domain.calendar.JalaliDate
import ir.hesabyar.domain.format.PersianText
import ir.hesabyar.domain.repository.BackupRepository
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json

@Singleton
class BackupRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: HesabyarDatabase,
    private val exportDao: BackupExportDao,
    private val backupDao: BackupDao
) : BackupRepository {

    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    private fun appVersion(): String = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
    }.getOrDefault("1.0.0")

    private suspend fun readPayload(): BackupPayload = BackupPayload(
        businesses = exportDao.businesses(),
        accounts = exportDao.accounts(),
        contacts = exportDao.contacts(),
        categories = exportDao.categories(),
        products = exportDao.products(),
        transactions = exportDao.transactions(),
        sales = exportDao.sales(),
        saleItems = exportDao.saleItems(),
        purchases = exportDao.purchases(),
        purchaseItems = exportDao.purchaseItems(),
        expenses = exportDao.expenses(),
        saleReturns = exportDao.saleReturns(),
        saleReturnItems = exportDao.saleReturnItems(),
        stockMovements = exportDao.stockMovements(),
        contactLedger = exportDao.contactLedger(),
        accountMovements = exportDao.accountMovements(),
        reminders = exportDao.reminders(),
        subscriptions = exportDao.subscriptions()
    )

    override suspend fun exportJson(): String {
        val payload = readPayload()
        val payloadJson = json.encodeToString(BackupPayload.serializer(), payload)
        val file = BackupFile(
            createdAt = System.currentTimeMillis(),
            appVersion = appVersion(),
            checksum = sha256(payloadJson),
            payload = payload
        )
        val text = json.encodeToString(BackupFile.serializer(), file)
        payload.businesses.firstOrNull()?.let { business ->
            backupDao.insert(
                BackupEntity(
                    businessId = business.id,
                    backupDate = System.currentTimeMillis(),
                    fileLocation = "manual-export",
                    status = "CREATED",
                    sizeBytes = text.toByteArray(Charsets.UTF_8).size.toLong()
                )
            )
        }
        return text
    }

    override suspend fun importJson(jsonText: String): Result<Unit> = runCatching {
        val file = json.decodeFromString(BackupFile.serializer(), jsonText)
        require(file.format == "hesabyar-backup") { "فایل انتخاب‌شده پشتیبان حسابیار نیست" }
        require(file.version == 1) { "نسخه فایل پشتیبان پشتیبانی نمی‌شود" }
        val payloadJson = json.encodeToString(BackupPayload.serializer(), file.payload)
        require(sha256(payloadJson) == file.checksum) { "فایل پشتیبان کامل یا سالم نیست" }
        require(file.payload.businesses.isNotEmpty()) { "اطلاعات فروشگاه در فایل وجود ندارد" }

        db.withTransaction {
            clearAll()
            exportDao.insertBusinesses(file.payload.businesses)
            exportDao.insertAccounts(file.payload.accounts)
            exportDao.insertContacts(file.payload.contacts)
            exportDao.insertCategories(file.payload.categories)
            exportDao.insertProducts(file.payload.products)
            exportDao.insertTransactions(file.payload.transactions)
            exportDao.insertSales(file.payload.sales)
            exportDao.insertSaleItems(file.payload.saleItems)
            exportDao.insertPurchases(file.payload.purchases)
            exportDao.insertPurchaseItems(file.payload.purchaseItems)
            exportDao.insertExpenses(file.payload.expenses)
            exportDao.insertSaleReturns(file.payload.saleReturns)
            exportDao.insertSaleReturnItems(file.payload.saleReturnItems)
            exportDao.insertStockMovements(file.payload.stockMovements)
            exportDao.insertContactLedger(file.payload.contactLedger)
            exportDao.insertAccountMovements(file.payload.accountMovements)
            exportDao.insertReminders(file.payload.reminders)
            exportDao.insertSubscriptions(file.payload.subscriptions)
        }
        file.payload.businesses.firstOrNull()?.let { business ->
            backupDao.insert(
                BackupEntity(
                    businessId = business.id,
                    backupDate = System.currentTimeMillis(),
                    fileLocation = "restore",
                    status = "RESTORED",
                    sizeBytes = jsonText.toByteArray(Charsets.UTF_8).size.toLong()
                )
            )
        }
    }

    override suspend fun exportCsvZip(): ByteArray {
        val payload = readPayload()
        val contacts = payload.contacts.associateBy { it.id }
        val accounts = payload.accounts.associateBy { it.id }

        val transactionsCsv = buildString {
            append(csvLine("شماره تراکنش", "تاریخ", "نوع", "مبلغ کل", "پرداخت‌شده", "مانده", "شخص", "حساب", "وضعیت", "وضعیت پرداخت", "توضیحات"))
            payload.transactions.sortedBy { it.date }.forEach { tx ->
                append(
                    csvLine(
                        tx.code,
                        JalaliDate.formatDate(tx.date, persianDigits = false),
                        tx.type,
                        tx.amount.toString(),
                        tx.paidAmount.toString(),
                        tx.remainingAmount.toString(),
                        tx.contactId?.let { contacts[it]?.name }.orEmpty(),
                        tx.accountId?.let { accounts[it]?.title }.orEmpty(),
                        tx.status,
                        tx.paymentStatus.orEmpty(),
                        tx.description.orEmpty()
                    )
                )
            }
        }

        val contactsCsv = buildString {
            append(csvLine("نام", "موبایل", "نوع", "مانده حساب", "مانده اولیه", "توضیحات"))
            payload.contacts.forEach { c ->
                append(csvLine(c.name, c.phone.orEmpty(), c.contactType, c.balance.toString(), c.initialBalance.toString(), c.description.orEmpty()))
            }
        }

        val productsCsv = buildString {
            append(csvLine("نام کالا", "کد", "دسته‌بندی", "قیمت خرید", "قیمت فروش", "بهای تمام‌شده", "موجودی", "حداقل موجودی"))
            val categories = payload.categories.associateBy { it.id }
            payload.products.forEach { p ->
                append(
                    csvLine(
                        p.name,
                        p.code.orEmpty(),
                        p.categoryId?.let { categories[it]?.name }.orEmpty(),
                        p.purchasePrice.toString(),
                        p.sellingPrice.toString(),
                        p.averageCost.toString(),
                        PersianText.formatQuantity(p.stockQuantity, persianDigits = false),
                        PersianText.formatQuantity(p.minimumStock, persianDigits = false)
                    )
                )
            }
        }

        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            fun entry(name: String, content: String) {
                zip.putNextEntry(ZipEntry(name))
                zip.write("\uFEFF$content".toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
            entry("transactions.csv", transactionsCsv)
            entry("contacts.csv", contactsCsv)
            entry("products.csv", productsCsv)
            entry(
                "README.txt",
                "خروجی اطلاعات حسابیار\n" +
                    "فایل‌های CSV با کدگذاری UTF-8 و سازگار با Excel هستند.\n" +
                    "تاریخ‌ها به تقویم شمسی و مبالغ به تومان ثبت شده‌اند.\n"
            )
        }
        return out.toByteArray()
    }

    override suspend fun exportXlsx(): ByteArray {
        val payload = readPayload()
        val contacts = payload.contacts.associateBy { it.id }
        val accounts = payload.accounts.associateBy { it.id }
        val categories = payload.categories.associateBy { it.id }

        val transactionSheet = XlsxWriter.Sheet(
            name = "تراکنش‌ها",
            headers = listOf("شماره", "تاریخ", "نوع", "مبلغ", "پرداخت‌شده", "مانده", "شخص", "حساب", "وضعیت", "توضیحات"),
            rows = payload.transactions.sortedBy { it.date }.map { tx ->
                listOf(
                    XlsxWriter.Cell.Text(tx.code),
                    XlsxWriter.Cell.Text(JalaliDate.formatDate(tx.date, persianDigits = false)),
                    XlsxWriter.Cell.Text(tx.type),
                    XlsxWriter.Cell.Number(tx.amount),
                    XlsxWriter.Cell.Number(tx.paidAmount),
                    XlsxWriter.Cell.Number(tx.remainingAmount),
                    XlsxWriter.Cell.Text(tx.contactId?.let { contacts[it]?.name }.orEmpty()),
                    XlsxWriter.Cell.Text(tx.accountId?.let { accounts[it]?.title }.orEmpty()),
                    XlsxWriter.Cell.Text(tx.status),
                    XlsxWriter.Cell.Text(tx.description.orEmpty())
                )
            }
        )
        val contactSheet = XlsxWriter.Sheet(
            name = "اشخاص",
            headers = listOf("نام", "موبایل", "نوع", "مانده حساب", "مانده اولیه"),
            rows = payload.contacts.map {
                listOf(
                    XlsxWriter.Cell.Text(it.name),
                    XlsxWriter.Cell.Text(it.phone.orEmpty()),
                    XlsxWriter.Cell.Text(it.contactType),
                    XlsxWriter.Cell.Number(it.balance),
                    XlsxWriter.Cell.Number(it.initialBalance)
                )
            }
        )
        val productSheet = XlsxWriter.Sheet(
            name = "کالاها",
            headers = listOf("نام", "کد", "دسته", "قیمت خرید", "قیمت فروش", "بهای تمام‌شده", "موجودی", "حداقل موجودی"),
            rows = payload.products.map {
                listOf(
                    XlsxWriter.Cell.Text(it.name),
                    XlsxWriter.Cell.Text(it.code.orEmpty()),
                    XlsxWriter.Cell.Text(it.categoryId?.let { id -> categories[id]?.name }.orEmpty()),
                    XlsxWriter.Cell.Number(it.purchasePrice),
                    XlsxWriter.Cell.Number(it.sellingPrice),
                    XlsxWriter.Cell.Number(it.averageCost),
                    XlsxWriter.Cell.Decimal(it.stockQuantity),
                    XlsxWriter.Cell.Decimal(it.minimumStock)
                )
            }
        )
        val totalSales = payload.sales.sumOf { it.netAmount }
        val totalExpenses = payload.expenses.sumOf { it.amount }
        val totalPurchases = payload.purchases.sumOf { it.netAmount }
        val summarySheet = XlsxWriter.Sheet(
            name = "خلاصه",
            headers = listOf("عنوان", "مقدار"),
            rows = listOf(
                listOf(XlsxWriter.Cell.Text("تعداد تراکنش‌ها"), XlsxWriter.Cell.Number(payload.transactions.size.toLong())),
                listOf(XlsxWriter.Cell.Text("تعداد اشخاص"), XlsxWriter.Cell.Number(payload.contacts.size.toLong())),
                listOf(XlsxWriter.Cell.Text("تعداد کالاها"), XlsxWriter.Cell.Number(payload.products.size.toLong())),
                listOf(XlsxWriter.Cell.Text("مجموع فروش (تومان)"), XlsxWriter.Cell.Number(totalSales)),
                listOf(XlsxWriter.Cell.Text("مجموع خرید (تومان)"), XlsxWriter.Cell.Number(totalPurchases)),
                listOf(XlsxWriter.Cell.Text("مجموع هزینه (تومان)"), XlsxWriter.Cell.Number(totalExpenses))
            )
        )
        return XlsxWriter.write(listOf(transactionSheet, contactSheet, productSheet, summarySheet))
    }

    private fun clearAll() {
        val writable = db.openHelper.writableDatabase
        listOf(
            "backups",
            "sale_return_items",
            "sale_returns",
            "sale_items",
            "sales",
            "purchase_items",
            "purchases",
            "expenses",
            "stock_movements",
            "contact_ledger",
            "account_movements",
            "transactions",
            "products",
            "product_categories",
            "reminders",
            "subscriptions",
            "accounts",
            "contacts",
            "businesses"
        ).forEach { writable.execSQL("DELETE FROM $it") }
    }

    private fun csvLine(vararg fields: String): String =
        fields.joinToString(",") { field ->
            val needsQuotes = field.contains(',') || field.contains('"') || field.contains('\n')
            if (needsQuotes) "\"" + field.replace("\"", "\"\"") + "\"" else field
        } + "\n"

    private fun sha256(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(text.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }
}
