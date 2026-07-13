package io.flatzen.monetization.time

import io.flatzen.commoncomponents.network.ConnectionMonitor
import io.flatzen.monetization.datastore.EncryptedSecureStore
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.abs
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * Singleton trusted clock: network sync + encrypted anchor + monotonic session progress.
 * Client-side best-effort protection against clock rollback for premium expiry.
 */
class TrustedTimeRepository(
    private val secureStore: EncryptedSecureStore,
    private val connectionMonitor: ConnectionMonitor,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val syncMutex = Mutex()
    private val httpClient = HttpClient {
        install(HttpTimeout) {
            requestTimeoutMillis = 10_000
            connectTimeoutMillis = 10_000
        }
    }

    private var anchor: TimeAnchor? = null
    private var lastTrustedNowMs: Long = 0L
    private var suspect: Boolean = false
    private var lastObservedDeviceMs: Long? = null
    private var sessionServerMs: Long? = null
    private var sessionMark: TimeMark? = null

    private val _state = MutableStateFlow(
        TrustedTimeState(
            nowEpochMs = Clock.System.now().toEpochMilliseconds(),
            status = TrustStatus.UNVERIFIED,
        ),
    )
    val state: StateFlow<TrustedTimeState> = _state.asStateFlow()

    init {
        scope.launch(Dispatchers.IO) {
            restorePersisted()
            publish()
            syncFromNetwork()
        }
        scope.launch(Dispatchers.IO) {
            while (true) {
                delay(SYNC_INTERVAL)
                syncFromNetwork()
            }
        }
        scope.launch(Dispatchers.IO) {
            connectionMonitor.isNetworkAvailable
                .distinctUntilChanged()
                .filter { it }
                .collect { syncFromNetwork() }
        }
        scope.launch {
            while (true) {
                delay(TICK_INTERVAL)
                publish()
            }
        }
    }

    fun nowEpochMs(): Long = publish().nowEpochMs

    suspend fun syncFromNetwork(): Boolean = syncMutex.withLock {
        val serverTimeMs = fetchServerTimeMs() ?: return false
        val deviceTimeMs = Clock.System.now().toEpochMilliseconds()
        val currentTrusted = computeTrustedNow(deviceTimeMs)

        // Never replace the anchor with a server sample that goes backward vs our timeline.
        if (anchor != null && serverTimeMs + MAX_CLOCK_SKEW_MS < currentTrusted) {
            markSuspect()
            publish()
            return false
        }

        val newAnchor = TimeAnchor(serverTimeMs = serverTimeMs, deviceTimeMs = deviceTimeMs)
        anchor = newAnchor
        secureStore.saveTimeAnchor(newAnchor)
        beginSession(serverTimeMs)

        val skew = abs(deviceTimeMs - serverTimeMs)
        if (suspect && skew <= MAX_CLOCK_SKEW_MS) {
            suspect = false
            secureStore.saveSuspectFlag(false)
        }

        lastObservedDeviceMs = deviceTimeMs
        publish()
        true
    }

    private suspend fun restorePersisted() {
        anchor = secureStore.loadTimeAnchor()
        lastTrustedNowMs = secureStore.loadLastTrustedNowMs() ?: 0L
        suspect = secureStore.loadSuspectFlag()
        // Advance trusted time monotonically from the last known point so rewarded expiry
        // keeps running even when the device clock is rolled back (no network sync yet).
        if (lastTrustedNowMs > 0L) {
            beginSession(lastTrustedNowMs)
        }
        lastObservedDeviceMs = Clock.System.now().toEpochMilliseconds()
        detectRollback(lastObservedDeviceMs!!)
    }

    private fun beginSession(serverTimeMs: Long) {
        sessionServerMs = serverTimeMs
        sessionMark = TimeSource.Monotonic.markNow()
    }

    private fun publish(): TrustedTimeState {
        val deviceNow = Clock.System.now().toEpochMilliseconds()
        detectRollback(deviceNow)
        val trustedNow = computeTrustedNow(deviceNow).coerceAtLeast(lastTrustedNowMs)
        if (trustedNow > lastTrustedNowMs) {
            lastTrustedNowMs = trustedNow
            scope.launch(Dispatchers.IO) {
                runCatching { secureStore.saveLastTrustedNowMs(trustedNow) }
            }
        }
        val status = when {
            suspect -> TrustStatus.SUSPECT
            anchor == null -> TrustStatus.UNVERIFIED
            else -> TrustStatus.TRUSTED
        }
        val next = TrustedTimeState(nowEpochMs = trustedNow, status = status)
        _state.value = next
        lastObservedDeviceMs = deviceNow
        return next
    }

    private fun computeTrustedNow(deviceNowMs: Long): Long {
        val mark = sessionMark
        val sessionServer = sessionServerMs
        if (mark != null && sessionServer != null) {
            return sessionServer + mark.elapsedNow().inWholeMilliseconds
        }
        val stored = anchor
        if (stored != null) {
            val offset = stored.serverTimeMs - stored.deviceTimeMs
            return deviceNowMs + offset
        }
        return deviceNowMs
    }

    private fun detectRollback(deviceNowMs: Long) {
        val previousDevice = lastObservedDeviceMs
        if (previousDevice != null && deviceNowMs + ROLLBACK_THRESHOLD_MS < previousDevice) {
            markSuspect()
        }
        val stored = anchor
        if (stored != null && deviceNowMs + ROLLBACK_THRESHOLD_MS < stored.deviceTimeMs) {
            // Device clock jumped behind the last sync anchor (cross-process rollback).
            markSuspect()
        }
        val raw = when {
            sessionMark != null && sessionServerMs != null ->
                sessionServerMs!! + sessionMark!!.elapsedNow().inWholeMilliseconds

            stored != null -> deviceNowMs + (stored.serverTimeMs - stored.deviceTimeMs)
            else -> deviceNowMs
        }
        if (lastTrustedNowMs > 0 && raw + ROLLBACK_THRESHOLD_MS < lastTrustedNowMs) {
            markSuspect()
        }
    }

    private fun markSuspect() {
        if (suspect) return
        suspect = true
        scope.launch(Dispatchers.IO) {
            runCatching { secureStore.saveSuspectFlag(true) }
        }
    }

    private suspend fun fetchServerTimeMs(): Long? {
        fetchFromTimeApi()?.let { return it }
        return fetchFromWorldTimeApi()
    }

    private suspend fun fetchFromTimeApi(): Long? = runCatching {
        val response: HttpResponse = httpClient.get(TIME_API_URL)
        if (!response.status.isSuccess()) return null
        val body = json.decodeFromString<TimeApiUtcResponse>(response.bodyAsText())
        val local = LocalDateTime(
            year = body.year,
            month = body.month,
            day = body.day,
            hour = body.hour,
            minute = body.minute,
            second = body.seconds,
            nanosecond = body.milliSeconds.coerceAtLeast(0) * 1_000_000,
        )
        local.toInstant(TimeZone.UTC).toEpochMilliseconds()
    }.getOrNull()

    private suspend fun fetchFromWorldTimeApi(): Long? = runCatching {
        val response: HttpResponse = httpClient.get(WORLD_TIME_API_URL)
        if (!response.status.isSuccess()) return null
        val body = json.decodeFromString<WorldTimeUtcResponse>(response.bodyAsText())
        body.unixtime * 1_000L
    }.getOrNull()

    companion object {
        private val SYNC_INTERVAL = 1.minutes
        private val TICK_INTERVAL = 30.seconds
        private const val MAX_CLOCK_SKEW_MS = 60_000L
        private const val ROLLBACK_THRESHOLD_MS = 5_000L
        private const val TIME_API_URL =
            "https://timeapi.io/api/Time/current/zone?timeZone=UTC"
        private const val WORLD_TIME_API_URL =
            "https://worldtimeapi.org/api/timezone/Etc/UTC"
    }
}

@Serializable
private data class TimeApiUtcResponse(
    val year: Int,
    val month: Int,
    val day: Int,
    val hour: Int,
    val minute: Int,
    val seconds: Int,
    val milliSeconds: Int = 0,
)

@Serializable
private data class WorldTimeUtcResponse(
    val unixtime: Long,
)
