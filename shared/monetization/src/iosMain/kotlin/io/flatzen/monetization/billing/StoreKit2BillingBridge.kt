package io.flatzen.monetization.billing

import io.flatzen.monetization.MonetizationDefaults
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import kotlin.coroutines.resume

/**
 * StoreKit 2 billing via a thin Swift host ([StoreKit2Native]).
 * Product mapping, purchase result parsing, and entitlement handling live in Kotlin.
 */
class StoreKit2BillingBridge : PlatformBillingBridge {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override fun isConfigured(): Boolean =
        !MonetizationDefaults.PREMIUM_FALLBACK_ENABLED && StoreKit2Native.isInstalled()

    override suspend fun queryProducts(): List<SubscriptionProduct> {
        val api = StoreKit2Native.api ?: return emptyList()
        val ids = listOf(
            MonetizationDefaults.PRODUCT_WEEK,
            MonetizationDefaults.PRODUCT_MONTH,
            MonetizationDefaults.PRODUCT_QUARTER,
        ).joinToString(",")
        val raw = suspendCancellableCoroutine { cont ->
            api.fetchProductsJson(
                productIdsCsv = ids,
                onSuccess = { cont.resume(it) },
                onError = { cont.resume("[]") },
            )
        }
        return runCatching {
            json.decodeFromString<List<StoreKit2ProductDto>>(raw).map { it.toSubscriptionProduct() }
        }.getOrDefault(emptyList())
    }

    override suspend fun purchase(productId: String): PurchaseResult {
        val api = StoreKit2Native.api ?: return PurchaseResult.NotConfigured
        val result = suspendCancellableCoroutine { cont ->
            api.purchase(productId) { cont.resume(it) }
        }
        return when {
            result == "success" -> PurchaseResult.Success
            result == "cancelled" -> PurchaseResult.Cancelled
            result.startsWith("error:") -> PurchaseResult.Error(message = result.removePrefix("error:"))
            else -> PurchaseResult.Error(message = result)
        }
    }

    override suspend fun restore(): SubscriptionStatus {
        val api = StoreKit2Native.api
            ?: return SubscriptionStatus(
                SubscriptionTier.FREE,
                source = SubscriptionStatus.StatusSource.STORE
            )
        val raw = suspendCancellableCoroutine { cont ->
            api.restoreStatusJson { cont.resume(it) }
        }
        return parseStatus(raw)
    }

    override suspend fun currentExpiryEpochMs(): Long? = currentStatus().expiresAtEpochMs

    override fun observeEntitlementChanges(): Flow<SubscriptionStatus> {
        val api = StoreKit2Native.api ?: return emptyFlow()
        return callbackFlow {
            api.startUpdatesListener { raw ->
                trySend(parseStatus(raw))
            }
            // Also emit current snapshot once
            api.currentStatusJson { raw ->
                trySend(parseStatus(raw))
            }
            awaitClose { }
        }
    }

    private suspend fun currentStatus(): SubscriptionStatus {
        val api = StoreKit2Native.api
            ?: return SubscriptionStatus(
                SubscriptionTier.FREE,
                source = SubscriptionStatus.StatusSource.STORE
            )
        val raw = suspendCancellableCoroutine { cont ->
            api.currentStatusJson { cont.resume(it) }
        }
        return parseStatus(raw)
    }

    private fun parseStatus(raw: String): SubscriptionStatus {
        val dto = runCatching { json.decodeFromString<StoreKit2StatusDto>(raw) }.getOrNull()
            ?: return SubscriptionStatus(
                SubscriptionTier.FREE,
                source = SubscriptionStatus.StatusSource.STORE
            )
        val tier = when (dto.tier.uppercase()) {
            "PREMIUM" -> SubscriptionTier.PREMIUM
            else -> SubscriptionTier.FREE
        }
        return SubscriptionStatus(
            tier = tier,
            expiresAtEpochMs = dto.expiresAtEpochMs,
            productId = dto.productId,
            source = SubscriptionStatus.StatusSource.STORE,
        )
    }

    private fun StoreKit2ProductDto.toSubscriptionProduct(): SubscriptionProduct {
        val weeks = when (id) {
            MonetizationDefaults.PRODUCT_WEEK -> 1
            MonetizationDefaults.PRODUCT_MONTH -> 4
            MonetizationDefaults.PRODUCT_QUARTER -> 12
            else -> 1
        }
        val mappedTitle = title
        return SubscriptionProduct(
            id = id,
            title = mappedTitle,
            priceFormatted = priceFormatted,
            priceAmount = priceAmount,
            currencyCode = currencyCode,
            periodWeeks = weeks,
            savingsPercent = null,
        )
    }
}
