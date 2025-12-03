package repository.referrals

import api.ReferralsApi
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import server_request.UseReferralCodeRequest
import server_response.flathub.ReferralStatsResponse
import server_response.flathub.UseReferralCodeResponse

class ReferralsRepositoryImpl(
    private val api: ReferralsApi
) : ReferralsRepository {

    override suspend fun getStats(userId: String): Flow<ReferralStatsResponse> = flow {
        emit(api.stats(userId))
    }

    override suspend fun useCode(hostUserId: String, invitedUserId: String): Result<UseReferralCodeResponse> {
        return runCatching {
            api.useCode(UseReferralCodeRequest(hostUserId, invitedUserId))
        }.mapError()
    }

    private fun <T> Result<T>.mapError(): Result<T> {
        return this.mapCatching { it }.recoverCatching { t ->
            when (t) {
                is ClientRequestException -> {
                    when (t.response.status) {
                        HttpStatusCode.BadRequest -> throw ReferralException(ReferralError.SameUserIds)
                        HttpStatusCode.Conflict -> throw ReferralException(ReferralError.CodeAlreadyUsed)
                        HttpStatusCode.NotAcceptable -> throw ReferralException(ReferralError.UserNotFound)
                        else -> throw ReferralException(ReferralError.Unknown(t.message))
                    }
                }

                is ServerResponseException, is ResponseException -> {
                    throw ReferralException(ReferralError.Unknown(t.message))
                }

                else -> throw t
            }
        }
    }
}


