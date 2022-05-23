package auzug.common

import kotlinx.serialization.Serializable

@Serializable
data class TransactionResponse(val tranId: Long, val readOnly: Boolean)

@Serializable
data class FailureResponse(val message: String)

val API_PATH = "/v1"
val API_AUTH_REALM = "Storage API"