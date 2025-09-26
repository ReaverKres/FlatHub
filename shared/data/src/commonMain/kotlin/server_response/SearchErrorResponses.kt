package server_response

import kotlinx.serialization.Serializable

@Serializable
data class OnlinerSearchErrorResponses(
    val message: String,
    val errors: Map<String, List<String>>
)

@Serializable
data class DomovitaErrorResponse(
    val name: String? = null,
    val message: String? = null,
    val code: Int? = null,
    val status: Int? = null
) {
    fun errorMessages(): List<String> {
        return listOfNotNull(message, name).filter { it.isNotBlank() }
    }
}

@Serializable
data class KufarErrorResponse(
    val error: KufarErrorDetail
) {
    @Serializable
    data class KufarErrorDetail(
        val message: String,
        val code: String? = null,
        val http: HttpError? = null,
        val details: Detail? = null
    )

    @Serializable
    data class HttpError(
        val code: Int,
        val message: String
    )

    @Serializable
    data class Detail(
        val message: String? = null,
        val reason: String? = null,
        val type: String? = null
    )

    fun errorMessages(): List<String> {
        val messages = mutableListOf<String>()
//        messages.add(error.message)
//        error.details?.message?.let { messages.add(it) }
        error.details?.reason?.let { messages.add(it) }
        return messages
    }
}