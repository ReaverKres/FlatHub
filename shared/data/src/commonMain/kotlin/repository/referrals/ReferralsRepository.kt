package repository.referrals

import kotlinx.coroutines.flow.Flow
import server_response.flathub.ReferralStatsResponse
import server_response.flathub.UseReferralCodeResponse

sealed interface ReferralError {
    data object SameUserIds : ReferralError
    data object CodeAlreadyUsed : ReferralError
    data object DuplicateLink : ReferralError
    data class Unknown(val message: String?) : ReferralError
}

class ReferralException(val error: ReferralError) : Exception(error.toString())

interface ReferralsRepository {
    suspend fun getStats(userId: String):  Flow<ReferralStatsResponse>
    suspend fun useCode(hostUserId: String, invitedUserId: String): Result<UseReferralCodeResponse>
}


