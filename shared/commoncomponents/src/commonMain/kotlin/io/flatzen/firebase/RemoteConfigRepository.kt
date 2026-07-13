package io.flatzen.firebase

import io.flatzen.commoncomponents.commonentities.more.FaqConfigData
import io.flatzen.commoncomponents.commonentities.more.MoreConfigData
import io.flatzen.commoncomponents.network.ConnectionMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

sealed interface RemoteConfigLoadState {
    data object Idle : RemoteConfigLoadState
    data object Loading : RemoteConfigLoadState
    data class Ready(val fetchedAtEpochMs: Long) : RemoteConfigLoadState
    data class Error(val cause: Throwable?) : RemoteConfigLoadState
}

interface RemoteConfigRepository {
    val loadState: StateFlow<RemoteConfigLoadState>
    val faqConfig: Flow<FaqConfigData?>
    val moreConfig: Flow<MoreConfigData?>
    fun isLoaded(): Boolean
    suspend fun refresh()
    suspend fun awaitFirstLoadAttempt()
}

class RemoteConfigRepositoryImpl(
    private val configManager: ConfigManager,
    private val configFieldsChecker: ConfigFieldsChecker,
    private val connectionMonitor: ConnectionMonitor,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) : RemoteConfigRepository {

    private val refreshMutex = Mutex()

    private val _loadState = MutableStateFlow<RemoteConfigLoadState>(RemoteConfigLoadState.Idle)
    override val loadState: StateFlow<RemoteConfigLoadState> = _loadState.asStateFlow()

    override val faqConfig: Flow<FaqConfigData?> = loadState
        .map { state ->
            if (state is RemoteConfigLoadState.Ready) {
                configFieldsChecker.checkJson<FaqConfigData>(ConfigFields.FaqConfigData)
            } else {
                null
            }
        }
        .distinctUntilChanged()

    override val moreConfig: Flow<MoreConfigData?> = loadState
        .map { state ->
            if (state is RemoteConfigLoadState.Ready) {
                configFieldsChecker.checkJson<MoreConfigData>(ConfigFields.MoreConfigData)
            } else {
                null
            }
        }
        .distinctUntilChanged()

    init {
        configManager.init()
        scope.launch { start() }
    }

    override fun isLoaded(): Boolean =
        _loadState.value is RemoteConfigLoadState.Ready

    override suspend fun refresh() {
        refreshMutex.withLock {
            _loadState.value = RemoteConfigLoadState.Loading
            val result = runCatching {
                configManager.fetchAndActivate().first { true }
            }.getOrElse { throwable ->
                ConfigResult(
                    isSuccess = false,
                    exception = throwable as? Exception
                        ?: Exception(throwable.message, throwable),
                )
            }

            _loadState.value = if (result.isSuccess) {
                RemoteConfigLoadState.Ready(
                    fetchedAtEpochMs = Clock.System.now().toEpochMilliseconds(),
                )
            } else {
                RemoteConfigLoadState.Error(result.exception)
            }
        }
    }

    override suspend fun awaitFirstLoadAttempt() {
        withTimeoutOrNull(FIRST_LOAD_TIMEOUT_MS) {
            loadState.first { it !is RemoteConfigLoadState.Idle && it !is RemoteConfigLoadState.Loading }
        }
    }

    private suspend fun start() {
        refresh()

        scope.launch {
            connectionMonitor.isNetworkAvailable
                .distinctUntilChanged()
                .filter { available -> available }
                .collect {
                    if (!isLoaded()) {
                        refresh()
                    }
                }
        }

        scope.launch {
            while (isActive) {
                delay(REFRESH_INTERVAL_MS)
                refresh()
            }
        }
    }

    private companion object {
        val REFRESH_INTERVAL_MS = 5.minutes.inWholeMilliseconds
        const val FIRST_LOAD_TIMEOUT_MS = 10_000L
    }
}
