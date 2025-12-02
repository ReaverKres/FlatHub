package api

import de.jensklingenberg.ktorfit.http.Body
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.POST
import de.jensklingenberg.ktorfit.http.Path
import server_request.UseReferralCodeRequest
import server_response.flathub.ReferralStatsResponse
import server_response.flathub.UseReferralCodeResponse

interface ReferralsApi {
    @GET("api/v1/referrals/{userId}/stats")
    suspend fun stats(@Path("userId") userId: String): ReferralStatsResponse

    @POST("api/v1/referrals/use-code")
    suspend fun useCode(@Body body: UseReferralCodeRequest): UseReferralCodeResponse
}


