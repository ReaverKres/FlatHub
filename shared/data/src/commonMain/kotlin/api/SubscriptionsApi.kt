package api

import de.jensklingenberg.ktorfit.http.Body
import de.jensklingenberg.ktorfit.http.DELETE
import de.jensklingenberg.ktorfit.http.POST
import de.jensklingenberg.ktorfit.http.Path
import kotlinx.serialization.Serializable

interface SubscriptionsApi {

    @POST("api/v1/devices/register")
    suspend fun register(@Body req: RegisterDeviceRequest): DeviceDocument

    @POST("api/v1/subscriptions/save-and-list")
    suspend fun saveAndList(@Body body: CreateSubscriptionRequest): List<SubscriptionDocument>

    @DELETE("api/v1/subscriptions/{id}")
    suspend fun delete(@Path("id") id: String)
}

@Serializable
data class RegisterDeviceRequest(
    val deviceToken: String,
    val platform: String, // android|ios
    val userId: String? = null
)

@Serializable
data class CreateSubscriptionRequest(
    val deviceId: String? = null,
    val name: String? = null,
    val filter: CommonFilterRequestDto
)

@Serializable
data class DeviceDocument(
    val deviceToken: String,
    val platform: String,
    val userId: String? = null,
)

@Serializable
data class SubscriptionDocument(
    val id: String,
    val userId: String? = null,
    val name: String? = null
)

