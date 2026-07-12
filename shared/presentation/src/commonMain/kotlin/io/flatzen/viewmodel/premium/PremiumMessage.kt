package io.flatzen.viewmodel.premium

import androidx.compose.runtime.Immutable
import io.flatzen.commoncomponents.localization.LocalizationKeys
import io.flatzen.monetization.billing.BillingResponseCode
import io.flatzen.monetization.billing.PurchaseResult

@Immutable
sealed interface PremiumMessage {
    data class Localized(val key: LocalizationKeys) : PremiumMessage
    data class Raw(val text: String) : PremiumMessage
}

fun PurchaseResult.Error.toPremiumMessage(): PremiumMessage {
    val localizedKey = billingResponseCode?.let(::billingErrorKey)
    if (localizedKey != null) return PremiumMessage.Localized(localizedKey)
    val storeMessage = message?.trim().orEmpty()
    return if (storeMessage.isNotEmpty()) {
        PremiumMessage.Raw(storeMessage)
    } else {
        PremiumMessage.Localized(LocalizationKeys.PREMIUM_ERROR_GENERIC)
    }
}

private fun billingErrorKey(code: Int): LocalizationKeys? = when (code) {
    BillingResponseCode.SERVICE_UNAVAILABLE -> LocalizationKeys.PREMIUM_ERROR_SERVICE_UNAVAILABLE
    BillingResponseCode.BILLING_UNAVAILABLE -> LocalizationKeys.PREMIUM_ERROR_BILLING_UNAVAILABLE
    BillingResponseCode.ITEM_UNAVAILABLE -> LocalizationKeys.PREMIUM_ERROR_ITEM_UNAVAILABLE
    BillingResponseCode.NETWORK_ERROR -> LocalizationKeys.PREMIUM_ERROR_NETWORK
    BillingResponseCode.ITEM_ALREADY_OWNED -> LocalizationKeys.PREMIUM_ERROR_ITEM_ALREADY_OWNED
    else -> null
}
