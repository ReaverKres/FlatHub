package server_request

import kotlinx.serialization.Serializable

@Serializable
data class UseReferralCodeRequest(
    val hostUserId: String,
    val invitedUserId: String
)