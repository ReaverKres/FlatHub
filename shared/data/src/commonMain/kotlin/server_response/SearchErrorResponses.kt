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