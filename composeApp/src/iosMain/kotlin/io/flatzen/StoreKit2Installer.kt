package io.flatzen

import io.flatzen.monetization.billing.installStoreKit2 as monetizationInstallStoreKit2

/**
 * Re-export for Swift (`ComposeApp.StoreKit2InstallerKt.installStoreKit2`).
 * Thin StoreKit 2 host is registered from `iosApp` via [FlatZenStoreKit2.install].
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
    monetizationInstallStoreKit2(
        fetchProducts = fetchProducts,
        purchase = purchase,
        currentStatus = currentStatus,
        restore = restore,
        startUpdates = startUpdates,
    )
}
