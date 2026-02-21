package io.flatzen.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import org.koin.compose.currentKoinScope
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.ParametersDefinition
import org.koin.core.qualifier.qualifier
import org.koin.core.scope.Scope
import org.koin.viewmodel.defaultExtras
import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.FlowMVIDSL
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState

/**
 * Composable function for getting FlowMVI Container from Koin.
 * Container will be bound to ViewModelStoreOwner lifecycle.
 *
 * Usage:
 * ```kotlin
 * @Composable
 * fun AuthScreen() {
 *     val container: AuthContainer = container()
 *     // or with parameters
 *     val container: AuthContainer = container { parametersOf("param") }
 * }
 * ```
 *
 * @param key Optional key for ViewModel
 * @param scope Koin scope
 * @param viewModelStoreOwner ViewModel store owner
 * @param extras Additional creation parameters
 * @param params Parameters for Container
 * @return FlowMVI Container
 */
@FlowMVIDSL
@NonRestartableComposable
@Composable
inline fun <reified T : Container<S, I, A>, S : MVIState, I : MVIIntent, A : MVIAction> container(
    key: String? = null,
    scope: Scope = currentKoinScope(),
    viewModelStoreOwner: ViewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current),
    extras: CreationExtras = defaultExtras(viewModelStoreOwner),
    noinline params: ParametersDefinition? = null,
): T = koinViewModel<ContainerViewModel<T, S, I, A>>(
    qualifier = qualifier<T>(),
    parameters = params,
    key = key,
    scope = scope,
    viewModelStoreOwner = viewModelStoreOwner,
    extras = extras
).container
