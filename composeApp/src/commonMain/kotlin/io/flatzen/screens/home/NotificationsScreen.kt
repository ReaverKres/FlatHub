package io.flatzen.screens.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import flatzen.composeapp.generated.resources.Res
import flatzen.composeapp.generated.resources.notifications_is_empty
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import io.flatzen.kmpapp.screens.EmptyScreenContent
import io.flatzen.viewmodel.notifications.NotificationListScreenAction
import io.flatzen.viewmodel.notifications.NotificationListViewModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    navigateToDetails: (flatPlatform: FlatPlatform, objectId: Long) -> Unit,
    navigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = koinViewModel<NotificationListViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = { Text("Уведомления", style = MaterialTheme.typography.headlineMedium) },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }) { paddingValues ->
        Box(modifier.fillMaxSize().padding(paddingValues)) {
            when {
                state.flatList.isEmpty() -> EmptyScreenContent(
                    modifier = Modifier.fillMaxSize(),
                    stringResource = Res.string.notifications_is_empty
                )

                else -> FlatList(
                    isLoadingMore = false,
                    flats = state.flatList,
                    onFlatClick = { navigateToDetails(it.flatPlatform, it.adId) },
                    clickOnFavorite = {
                        viewModel.onIntent(
                            NotificationListScreenAction.ClickOnFavorite(
                                it.flatPlatform,
                                it.adId
                            )
                        )
                    },
                    onLoadMore = { }
                )
            }
        }
    }
}