package server_response

import kotlinx.serialization.Serializable

@Serializable
data class OnlinerErrorResponse(
    val message: String,
    val errors: Map<String, List<String>>
)
