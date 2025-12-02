package server_response.flathub

import kotlinx.serialization.Serializable

@Serializable
data class ReferralStatsResponse(
    val userId: String,
    val invitesCount: Int,
    val usedReferralCode: Boolean,
    val requiredInvites: Int,
    val remainingInvites: Int,
    val isNotificationAvailable: Boolean
)

@Serializable
data class UseReferralCodeResponse(
    val hostStats: ReferralStatsResponse,
    val invitedStats: ReferralStatsResponse
)
