package io.flatzen.navigation

import io.flatzen.commoncomponents.commonentities.FlatPlatform
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.receiveAsFlow

sealed interface FlatHubCommand {
    data class OpenDetail(val platform: FlatPlatform, val objectId: Long) : FlatHubCommand
    data object OpenFilter : FlatHubCommand
    data class OpenNotifications(val filterJson: String? = null) : FlatHubCommand
    data class OpenMap(val selectedMarker: Long? = null) : FlatHubCommand
    data object OpenFaq : FlatHubCommand
    data object OpenReferral : FlatHubCommand
    data object OpenLocation : FlatHubCommand
    data object OpenCitySelect : FlatHubCommand
    data object OpenMetroSelect : FlatHubCommand
    data object OpenDistrictSelect : FlatHubCommand
    data object NavigateBack : FlatHubCommand
}

interface NavigationEmitter<C> {
    val commands: Flow<C>
    fun navigate(command: C)
}

class ChannelNavigationEmitter<C> : NavigationEmitter<C> {
    private val channel = Channel<C>(capacity = Channel.UNLIMITED)

    override val commands: Flow<C> = channel.receiveAsFlow()

    override fun navigate(command: C) {
        channel.trySend(command)
    }
}

open class DelegatedNavigationEmitter<C> : NavigationEmitter<C> {
    private var delegate: ChannelNavigationEmitter<C>? = null

    fun attach(emitter: ChannelNavigationEmitter<C>) {
        delegate = emitter
    }

    fun detach() {
        delegate = null
    }

    override val commands: Flow<C>
        get() = delegate?.commands ?: emptyFlow()

    override fun navigate(command: C) {
        delegate?.navigate(command)
    }
}

interface FlatHubNavigator : NavigationEmitter<FlatHubCommand>

class FlatHubNavigatorDelegate :
    DelegatedNavigationEmitter<FlatHubCommand>(),
    FlatHubNavigator
