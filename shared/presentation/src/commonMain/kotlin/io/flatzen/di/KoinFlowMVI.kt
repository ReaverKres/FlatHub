package io.flatzen.di

import org.koin.core.definition.Definition
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.qualifier
import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.FlowMVIDSL
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState

@FlowMVIDSL
inline fun <reified T : Container<S, I, A>, S : MVIState, I : MVIIntent, A : MVIAction> Module.container(
    crossinline definition: Definition<T>,
) = viewModel(qualifier<T>()) { params ->
    ContainerViewModel<T, _, _, _>(container = definition(params))
}