package org.multipaz.records.request

import io.ktor.http.ContentType
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respondText
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import org.multipaz.records.payment.PaymentAccount

suspend fun paymentAccount(call: ApplicationCall, accountId: String) {
    val account = PaymentAccount.get(accountId)
    val transactions =
        PaymentAccount.getTransactions(accountId).sortedByDescending { it.second.time }
    call.respondText (
        contentType = ContentType.Application.Json,
        text = buildJsonObject {
            put("balance", account.balance)
            put("holder_id", account.holderId)
            put("holder_name", PaymentAccount.lookupAccountName(accountId))
            putJsonArray("transactions") {
                for ((transactionId, transaction) in transactions) {
                    addJsonObject {
                        put("id", transactionId)
                        put("amount", transaction.amount)
                        if (transaction.payerAccount == accountId) {
                            putJsonObject("to") {
                                put("account", transaction.payeeAccount)
                                put("name", transaction.payeeName)
                            }
                        } else {
                            putJsonObject("from") {
                                put("account", transaction.payerAccount)
                                put("name", transaction.payerName)
                            }
                        }
                        put("time", transaction.time.toString())
                        transaction.description?.let { put("description", it)}
                    }
                }
            }
        }.toString()
    )
}