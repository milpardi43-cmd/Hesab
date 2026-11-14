package ir.hesabyar.domain.util

import java.util.UUID

/**
 * Idempotency keys. A screen generates one key per attempt; the transactions table has a unique
 * index on operation_id so a double tap or a retried submit can never create two records.
 */
object OperationIds {
    fun new(): String = UUID.randomUUID().toString()
}
