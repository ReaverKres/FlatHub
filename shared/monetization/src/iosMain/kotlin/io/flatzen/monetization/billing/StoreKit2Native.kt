package io.flatzen.monetization.billing

import kotlinx.serialization.Serializable
import kotlin.concurrent.Volatile

/**
 * JSON DTOs exchanged with the thin Swift StoreKit 2 host.
 * All mapping / business logic stays in Kotlin.
 */
@Serializable
data class StoreKit2ProductDto(
    val id: String,
    val title: String,
    val priceFormatted: String,
    val priceAmount: Double,
    val currencyCode: String,
)

@Serializable
data class StoreKit2StatusDto(
    val tier: String,
    val expiresAtEpochMs: Long? = null,
    val productId: String? = null,
)

/**
 * Callback-based API implemented in Swift (StoreKit 2 is Swift-only).
 * Prefer [installStoreKit2] from `iosApp` at launch.
 */
interface StoreKit2NativeApi {
    fun fetchProductsJson(
        productIdsCsv: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit,
    )

    fun purchase(
        productId: String,
        onResult: (String) -> Unit,
    )

    fun currentStatusJson(onResult: (String) -> Unit)

    fun restoreStatusJson(onResult: (String) -> Unit)

    fun startUpdatesListener(onStatusJson: (String) -> Unit)
}

object StoreKit2Native {
    @Volatile
    var api: StoreKit2NativeApi? = null
        private set

    fun install(api: StoreKit2NativeApi) {
        this.api = api
    }

    fun isInstalled(): Boolean = api != null
}

/**
 * Install from Swift with closures — keeps StoreKit 2 calls in Swift,
 * everything else (parse, map, DI) in Kotlin.
 *
 * Purchase [onResult] values: `success` | `cancelled` | `error:<message>`
 */
fun installStoreKit2(
    fetchProducts: (
        productIdsCsv: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit,
    ) -> Unit,
    purchase: (
        productId: String,
        onResult: (String) -> Unit,
    ) -> Unit,
    currentStatus: (onResult: (String) -> Unit) -> Unit,
    restore: (onResult: (String) -> Unit) -> Unit,
    startUpdates: (onStatus: (String) -> Unit) -> Unit,
) {
    StoreKit2Native.install(
        object : StoreKit2NativeApi {
            override fun fetchProductsJson(
                productIdsCsv: String,
                onSuccess: (String) -> Unit,
                onError: (String) -> Unit,
            ) = fetchProducts(productIdsCsv, onSuccess, onError)

            override fun purchase(
                productId: String,
                onResult: (String) -> Unit,
            ) = purchase(productId, onResult)

            override fun currentStatusJson(onResult: (String) -> Unit) =
                currentStatus(onResult)

            override fun restoreStatusJson(onResult: (String) -> Unit) =
                restore(onResult)

            override fun startUpdatesListener(onStatusJson: (String) -> Unit) =
                startUpdates(onStatusJson)
        },
    )
}
