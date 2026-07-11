import Foundation
import StoreKit
import ComposeApp

/// Thin StoreKit 2 host. Business logic (mapping, DI, cache) lives in Kotlin.
enum FlatZenStoreKit2 {
    private static let premiumIds: Set<String> = [
        "flatzen_premium_week",
        "flatzen_premium_month",
        "flatzen_premium_quarter",
    ]

    static func install() {
        StoreKit2InstallerKt.installStoreKit2(
            fetchProducts: { productIdsCsv, onSuccess, onError in
                Task {
                    do {
                        let ids = Set(
                            productIdsCsv
                                .split(separator: ",")
                                .map { String($0).trimmingCharacters(in: .whitespaces) }
                                .filter { !$0.isEmpty }
                        )
                        let products = try await Product.products(for: ids)
                        let payload: [[String: Any]] = products.map { product in
                            [
                                "id": product.id,
                                "title": product.displayName,
                                "priceFormatted": product.displayPrice,
                                "priceAmount": NSDecimalNumber(decimal: product.price).doubleValue,
                                "currencyCode": currencyCode(for: product),
                            ]
                        }
                        let data = try JSONSerialization.data(withJSONObject: payload)
                        let json = String(data: data, encoding: .utf8) ?? "[]"
                        onSuccess(json)
                    } catch {
                        onError(error.localizedDescription)
                    }
                }
            },
            purchase: { productId, onResult in
                Task {
                    do {
                        guard let product = try await Product.products(for: [productId]).first else {
                            onResult("error:Product not found")
                            return
                        }
                        let result = try await product.purchase()
                        switch result {
                        case .success(let verification):
                            let transaction = try checkVerified(verification)
                            await transaction.finish()
                            onResult("success")
                        case .userCancelled:
                            onResult("cancelled")
                        case .pending:
                            onResult("error:Pending approval")
                        @unknown default:
                            onResult("error:Unknown purchase result")
                        }
                    } catch {
                        onResult("error:\(error.localizedDescription)")
                    }
                }
            },
            currentStatus: { onResult in
                Task {
                    onResult(await statusJson())
                }
            },
            restore: { onResult in
                Task {
                    try? await AppStore.sync()
                    onResult(await statusJson())
                }
            },
            startUpdates: { onStatus in
                Task.detached(priority: .background) {
                    for await update in Transaction.updates {
                        if case .verified(let transaction) = update {
                            await transaction.finish()
                        }
                        onStatus(await statusJson())
                    }
                }
            }
        )
    }

    private static func currencyCode(for product: Product) -> String {
        if #available(iOS 16.0, *) {
            return product.priceFormatStyle.currencyCode
        }
        return Locale.current.currencyCode ?? "USD"
    }

    private static func statusJson() async -> String {
        var bestExpiry: Date?
        var productId: String?
        var hasNonExpiring = false

        for await result in Transaction.currentEntitlements {
            guard let transaction = try? checkVerified(result) else { continue }
            guard premiumIds.contains(transaction.productID) else { continue }
            productId = transaction.productID
            if let expiry = transaction.expirationDate {
                if bestExpiry == nil || expiry > bestExpiry! {
                    bestExpiry = expiry
                }
            } else {
                hasNonExpiring = true
            }
        }

        let now = Date()
        let active: Bool
        if hasNonExpiring {
            active = productId != nil
        } else if let bestExpiry {
            active = bestExpiry > now
        } else {
            active = false
        }

        var dict: [String: Any] = [
            "tier": active ? "PREMIUM" : "FREE",
        ]
        if let productId {
            dict["productId"] = productId
        }
        if let bestExpiry, !hasNonExpiring {
            dict["expiresAtEpochMs"] = Int64(bestExpiry.timeIntervalSince1970 * 1000)
        }

        guard let data = try? JSONSerialization.data(withJSONObject: dict),
              let json = String(data: data, encoding: .utf8) else {
            return #"{"tier":"FREE"}"#
        }
        return json
    }

    private static func checkVerified<T>(_ result: VerificationResult<T>) throws -> T {
        switch result {
        case .unverified(_, let error):
            throw error
        case .verified(let value):
            return value
        }
    }
}
