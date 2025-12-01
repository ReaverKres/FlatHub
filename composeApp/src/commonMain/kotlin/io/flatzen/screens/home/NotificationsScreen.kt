package io.flatzen.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import flatzen.composeapp.generated.resources.Res
import flatzen.composeapp.generated.resources.notifications_is_empty
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import io.flatzen.kmpapp.screens.EmptyScreenContent
import io.flatzen.viewmodel.notifications.NotificationListEffect
import io.flatzen.viewmodel.notifications.NotificationListScreenAction
import io.flatzen.viewmodel.notifications.NotificationListScreenState
import io.flatzen.viewmodel.notifications.NotificationListViewModel
import io.flatzen.widgets.dialogs.SimpleAlertDialog
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

    var noFlatsBoxHeight by remember { mutableStateOf(0.dp) }
    val lazyListState = rememberLazyListState()
    val firstVisibleItemIndex by remember {
        derivedStateOf { lazyListState.firstVisibleItemIndex }
    }
    val showScrollToTopBtn by remember {
        val count = if (state.isListView) 6 else 8
        derivedStateOf { firstVisibleItemIndex >= count && state.flatList.isNotEmpty() }
    }
    val scrollToTopBtnSize: Dp = 48.dp
    val localDensity = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.onIntent(NotificationListScreenAction.ScreenVisible)
        viewModel.effect.collect {
            when (it) {
                is NotificationListEffect.ScrollToTopEffect -> {
                    lazyListState.scrollToItem(0)
                }
            }
        }
    }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = { Text("Уведомления", style = MaterialTheme.typography.headlineSmall) },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }) { paddingValues ->
        PullToRefreshBox(
            onRefresh = {
                if (state.isLoading.not()) {
                    viewModel.onIntent(
                        NotificationListScreenAction.SearchFlats(
                            isLoadMore = false,
                            isRefreshing = true
                        )
                    )
                }
            },
            isRefreshing = state.isRefreshing,
            modifier = Modifier.padding(paddingValues)
        ) {
            Box(Modifier.fillMaxSize()) {

                if (state.errorText != null) {
                    SimpleAlertDialog(
                        title = "Ошибка",
                        message = state.errorText ?: "",
                        onDismiss = { viewModel.onIntent(NotificationListScreenAction.HideNetworkErrorDialog) }
                    )
                }

                if (state.paramsDialogText != null) {
                    SimpleAlertDialog(
                        title = "Параметры фильтра",
                        message = state.paramsDialogText ?: "",
                        onDismiss = { viewModel.onIntent(NotificationListScreenAction.HideParams) }
                    )
                }

                when {
                    state.isLoading.not() && state.flatList.isEmpty() && state.subscriptions.isEmpty() -> {
                        Column(
                            modifier = modifier.fillMaxSize()
                                .padding(12.dp)
                        ) {
                            EmptyScreenContent(
                                modifier = Modifier.fillMaxSize(),
                                Res.string.notifications_is_empty
                            )
                        }
                    }

                    state.isLoading && state.isLoadingMore.not() -> {
                        LazyColumn(
                            modifier = modifier.fillMaxSize(),
                            contentPadding = PaddingValues(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            flatListSkeletons(state.isListView)
                        }
                    }

                    else -> FlatList(
                        modifier = Modifier.align(Alignment.TopCenter),
                        lazyListState = lazyListState,
                        isLoadingMore = state.isLoadingMore,
                        flats = state.flatList,
                        isListView = state.isListView,
                        onFlatClick = { navigateToDetails(it.flatPlatform, it.adId) },
                        clickOnFavorite = {
                            viewModel.onIntent(
                                NotificationListScreenAction.ClickOnFavorite(
                                    it.flatPlatform,
                                    it.adId
                                )
                            )
                        },
                        onLoadMore = {
                            viewModel.onIntent(NotificationListScreenAction.SearchFlats(true))
                        },
                        topContent = {
                            topSubscriptionsContent(
                                state = state,
                                onSelect = { id ->
                                    viewModel.onIntent(
                                        NotificationListScreenAction.SelectSubscription(
                                            id
                                        )
                                    )
                                },
                                onShowParams = { id ->
                                    viewModel.onIntent(
                                        NotificationListScreenAction.ShowParams(id)
                                    )
                                },
                                onDelete = { id ->
                                    viewModel.onIntent(
                                        NotificationListScreenAction.DeleteSubscription(
                                            id
                                        )
                                    )
                                },
                                onToggleView = {
                                    viewModel.onIntent(NotificationListScreenAction.ToggleView)
                                }
                            )
                        },
                        bottomContent = {
                            if (state.noFlatsToLoadMore) {
                                item {
                                    LoadMoreForce(state.currentSearchPage) {
                                        clickOnLoadMore(viewModel)
                                    }
                                    Spacer(Modifier.height(noFlatsBoxHeight))
                                    Spacer(Modifier.height(16.dp))
                                    if (showScrollToTopBtn) {
                                        Spacer(Modifier.height(scrollToTopBtnSize + 24.dp))
                                    }
                                }
                            }
                        }
                    )
                }
            }
            ScrollToTopBtn(
                showScrollToTopBtn,
                coroutineScope,
                firstVisibleItemIndex,
                lazyListState,
                scrollToTopBtnSize
            )

            if (state.noFlatsToLoadMore) {
                NoFlatsToLoadMoreText(
                    showScrollToTopBtn = showScrollToTopBtn,
                    onSizeChanged = {
                        noFlatsBoxHeight = with(localDensity) {
                            it.height.toDp()
                        }
                    })
            }
        }
    }
}

private fun clickOnLoadMore(viewModel: NotificationListViewModel) {
    viewModel.onIntent(
        NotificationListScreenAction.SearchFlats(
            isLoadMore = true,
            isLoadMoreForce = true
        )
    )
}


private fun LazyListScope.topSubscriptionsContent(
    state: NotificationListScreenState,
    onToggleView: () -> Unit,
    onSelect: (String) -> Unit,
    onShowParams: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    item {
        FlowRow(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            itemVerticalAlignment = Alignment.CenterVertically
        ) {
            state.subscriptions.forEach { sub ->
                var menuExpanded by remember { mutableStateOf(false) }
                FilterChip(
                    onClick = { onSelect(sub.id) },
                    label = { Text(sub.filterUi.name.orEmpty()) },
                    trailingIcon = {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.size(16.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text("Параметры") },
                                onClick = { menuExpanded = false; onShowParams(sub.id) }
                            )
                            DropdownMenuItem(
                                text = { Text("Удалить") },
                                onClick = { menuExpanded = false; onDelete(sub.id) }
                            )
                        }
                    },
                    selected = sub.selected
                )
            }
        }
    }
    item {
        val activeColor = MaterialTheme.colorScheme.primary
        val unSelectedColor = Color.LightGray

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ListTypeSwitches(onToggleView, state.isListView, unSelectedColor, activeColor)
        }
    }
}