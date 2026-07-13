package io.flatzen.monetization.time

/**
 * Client-side trust level for wall-clock time used by premium expiry.
 * Best-effort only — paid store subscriptions are still validated by Play / StoreKit.
 */
enum class TrustStatus {
    /** No successful network sync yet; app stays usable with device clock. */
    UNVERIFIED,

    /** Synced with server and no rollback detected. */
    TRUSTED,

    /** Device clock was rolled back relative to the trusted timeline. */
    SUSPECT,
}

data class TrustedTimeState(
    val nowEpochMs: Long,
    val status: TrustStatus,
)

data class TimeAnchor(
    val serverTimeMs: Long,
    val deviceTimeMs: Long,
)
