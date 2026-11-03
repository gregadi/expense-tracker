package com.gregoryadi.expenses.data.port

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.gregoryadi.expenses.data.db.AppDatabase
import com.gregoryadi.expenses.data.db.CategoryEntity
import com.gregoryadi.expenses.data.db.ExpenseEntity
import com.gregoryadi.expenses.data.db.PaymentMethodEntity
import com.gregoryadi.expenses.domain.Kind
import com.gregoryadi.expenses.domain.MethodType
import com.gregoryadi.expenses.domain.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class ImportResult(val added: Int, val skipped: Int)

/**
 * Backup and restore through files the user picks (Downloads, Google Drive...).
 * JSON is the full backup; CSV is a read-only export for spreadsheets.
 *
 * Entries and categories carry a "kind" ("expense" or "income"). Backups made before income existed
 * have no kind and import as expenses.
 */
class DataPorter(
    private val context: Context,
    private val db: AppDatabase,
    /** Runs after an import, so the widget picks up restored entries. */
    private val onChange: () -> Unit = {},
) {

    /** Writes everything to [uri] as JSON and returns the number of entries saved. */
    suspend fun exportJson(uri: Uri): Int = withContext(Dispatchers.IO) {
        val methods = db.paymentMethodDao().getAll()
        val categories = db.categoryDao().getAll()
        val expenses = db.expenseDao().getAll()
        val methodNames = methods.associate { it.id to it.name }
        val categoryNames = categories.associate { it.id to it.name }

        val root = JSONObject()
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("paymentMethods", JSONArray().also { array ->
            methods.forEach { m ->
                array.put(
                    JSONObject()
                        .put("name", m.name)
                        .put("type", m.type)
                        .put("color", m.color)
                        .put("archived", m.archived)
                )
            }
        })
        root.put("categories", JSONArray().also { array ->
            categories.forEach { c ->
                array.put(
                    JSONObject()
                        .put("name", c.name)
                        .put("kind", c.kind)
                        .put("color", c.color)
                        .put("archived", c.archived)
                )
            }
        })
        root.put("expenses", JSONArray().also { array ->
            expenses.forEach { e ->
                array.put(
                    JSONObject()
                        .put("kind", e.kind)
                        .put("amount", e.amount)
                        .put("occurredAt", e.occurredAt)
                        .put("method", methodNames[e.paymentMethodId].orEmpty())
                        .put("category", categoryNames[e.categoryId].orEmpty())
                        .put("note", e.note)
                )
            }
        })

        val stream = context.contentResolver.openOutputStream(uri, "wt") ?: error("Could not open the file for writing")
        stream.use { it.write(root.toString(2).toByteArray(Charsets.UTF_8)) }
        expenses.size
    }

    /** Writes a spreadsheet-friendly CSV and returns the number of entries saved. */
    suspend fun exportCsv(uri: Uri): Int = withContext(Dispatchers.IO) {
        val methodNames = db.paymentMethodDao().getAll().associate { it.id to it.name }
        val categoryNames = db.categoryDao().getAll().associate { it.id to it.name }
        val expenses = db.expenseDao().getAll()
        val zone = ZoneId.systemDefault()
        val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val timeFormat = DateTimeFormatter.ofPattern("HH:mm")

        val text = buildString {
            append("date,time,type,amount_idr,payment_method,category,note\n")
            expenses.forEach { e ->
                val at = Instant.ofEpochMilli(e.occurredAt).atZone(zone)
                append(dateFormat.format(at)).append(',')
                append(timeFormat.format(at)).append(',')
                append(e.kind).append(',')
                append(e.amount).append(',')
                append(csv(methodNames[e.paymentMethodId].orEmpty())).append(',')
                append(csv(categoryNames[e.categoryId].orEmpty())).append(',')
                append(csv(e.note)).append('\n')
            }
        }
        val stream = context.contentResolver.openOutputStream(uri, "wt") ?: error("Could not open the file for writing")
        stream.use { it.write(text.toByteArray(Charsets.UTF_8)) }
        expenses.size
    }

    /**
     * Restores a JSON backup. Methods are matched by name and categories by kind + name (created when missing),
     * and entries already in the database are skipped, so importing the same file twice is safe.
     */
    suspend fun importJson(uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        val stream = context.contentResolver.openInputStream(uri) ?: error("Could not open the file")
        val text = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        val root = JSONObject(text)
        require(root.optInt("version", 0) == 1) { "This is not an Expenses backup file" }

        db.withTransaction {
            val methodDao = db.paymentMethodDao()
            val categoryDao = db.categoryDao()
            val expenseDao = db.expenseDao()

            val methodIds = methodDao.getAll().associate { it.name.lowercase() to it.id }.toMutableMap()
            val categoryIds = categoryDao.getAll().associate { categoryKey(it.kind, it.name) to it.id }.toMutableMap()

            val methodsJson = root.optJSONArray("paymentMethods") ?: JSONArray()
            for (i in 0 until methodsJson.length()) {
                val o = methodsJson.getJSONObject(i)
                val name = o.getString("name").trim()
                if (name.isEmpty() || methodIds.containsKey(name.lowercase())) continue
                val id = methodDao.insert(
                    PaymentMethodEntity(
                        name = name,
                        type = o.optString("type", MethodType.OTHER.key),
                        color = o.optLong("color", Palette.colors[0]),
                        sortOrder = methodDao.nextSortOrder(),
                        archived = o.optBoolean("archived", false),
                    )
                )
                methodIds[name.lowercase()] = id
            }

            val categoriesJson = root.optJSONArray("categories") ?: JSONArray()
            for (i in 0 until categoriesJson.length()) {
                val o = categoriesJson.getJSONObject(i)
                val name = o.getString("name").trim()
                val kind = kindOf(o)
                if (name.isEmpty() || categoryIds.containsKey(categoryKey(kind, name))) continue
                val id = categoryDao.insert(
                    CategoryEntity(
                        name = name,
                        color = o.optLong("color", Palette.colors[0]),
                        sortOrder = categoryDao.nextSortOrder(),
                        archived = o.optBoolean("archived", false),
                        kind = kind,
                    )
                )
                categoryIds[categoryKey(kind, name)] = id
            }

            var added = 0
            var skipped = 0
            val now = System.currentTimeMillis()
            val expensesJson = root.optJSONArray("expenses") ?: JSONArray()
            for (i in 0 until expensesJson.length()) {
                val o = expensesJson.getJSONObject(i)
                val kind = kindOf(o)
                val methodName = o.optString("method", "").trim().ifEmpty { "Other" }
                val categoryName = o.optString("category", "").trim()
                    .ifEmpty { if (kind == Kind.INCOME) "Other income" else "Other" }

                val methodId = methodIds.getOrPut(methodName.lowercase()) {
                    methodDao.insert(
                        PaymentMethodEntity(
                            name = methodName,
                            type = MethodType.OTHER.key,
                            color = Palette.colors[7],
                            sortOrder = methodDao.nextSortOrder(),
                        )
                    )
                }
                val categoryId = categoryIds.getOrPut(categoryKey(kind, categoryName)) {
                    categoryDao.insert(
                        CategoryEntity(
                            name = categoryName,
                            color = Palette.colors[7],
                            sortOrder = categoryDao.nextSortOrder(),
                            kind = kind,
                        )
                    )
                }

                val amount = o.getLong("amount")
                val occurredAt = o.getLong("occurredAt")
                val note = o.optString("note", "")
                if (expenseDao.countMatching(kind, occurredAt, amount, methodId, categoryId, note) > 0) {
                    skipped++
                } else {
                    expenseDao.insert(
                        ExpenseEntity(
                            amount = amount,
                            occurredAt = occurredAt,
                            paymentMethodId = methodId,
                            categoryId = categoryId,
                            note = note,
                            createdAt = now,
                            updatedAt = now,
                            kind = kind,
                        )
                    )
                    added++
                }
            }
            ImportResult(added, skipped)
        }.also { onChange() }
    }

    private fun kindOf(o: JSONObject): String =
        if (o.optString("kind", Kind.EXPENSE) == Kind.INCOME) Kind.INCOME else Kind.EXPENSE

    private fun categoryKey(kind: String, name: String): String = kind + "|" + name.trim().lowercase()

    private fun csv(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
}
