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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import dev.icerock.moko.permissions.PermissionsController
import dev.icerock.moko.permissions.compose.BindEffect
import dev.icerock.moko.permissions.compose.rememberPermissionsControllerFactory
import flatzen.composeapp.generated.resources.Res
import flatzen.composeapp.generated.resources.back
import flatzen.composeapp.generated.resources.close
import flatzen.composeapp.generated.resources.delete
import flatzen.composeapp.generated.resources.error
import flatzen.composeapp.generated.resources.notifications_allow
import flatzen.composeapp.generated.resources.notifications_is_empty
import flatzen.composeapp.generated.resources.notifications_params
import flatzen.composeapp.generated.resources.notifications_permission_message
import flatzen.composeapp.generated.resources.notifications_title
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import io.flatzen.di.container
import io.flatzen.kmpapp.screens.EmptyScreenContent
import io.flatzen.viewmodel.notifications.NotificationListAction
import io.flatzen.viewmodel.notifications.NotificationListContainer
import io.flatzen.viewmodel.notifications.NotificationListIntent
import io.flatzen.viewmodel.notifications.NotificationListScreenState
import io.flatzen.widgets.MessageSnackbar
import io.flatzen.widgets.dialogs.SimpleAlertDialog
import io.flatzen.widgets.dialogs.SystemSettingsDialog
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.core.parameter.parametersOf
import pro.respawn.flowmvi.compose.dsl.subscribe

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    navigateToDetails: (flatPlatform: FlatPlatform, objectId: Long) -> Unit,
    navigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    filterFromNotification: String? = null
) {

    val factory = rememberPermissionsControllerFactory()
    val permissionsController: PermissionsController =
        remember(factory) { factory.createPermissionsController() }
    BindEffect(permissionsController)
    val container: NotificationListContainer = container {
        parametersOf(permissionsController, filterFromNotification)
    }
    var noFlatsBoxHeight by remember { mutableStateOf(0.dp) }
    val lazyListState = rememberLazyListState()
    var notifPermSnackBarIsVisible by remember { mutableStateOf(false) }
    var showNotificationsSettingsDialog by rememberSaveable { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val state by container.store.subscribe { action ->
        when (action) {
            NotificationListAction.ScrollToTopEffect -> {
                coroutineScope.launch { lazyListState.scrollToItem(0) }
            }

            is NotificationListAction.NotifPermGrantedEffect -> {
                notifPermSnackBarIsVisible = !action.isGranted
            }

            NotificationListAction.ShowSettingsEffect -> {
                showNotificationsSettingsDialog = true
            }
        }
    }
    val firstVisibleItemIndex by remember {
        derivedStateOf { lazyListState.firstVisibleItemIndex }
    }
    val showScrollToTopBtn by remember {
        val count = if (state.isListView) 6 else 8
        derivedStateOf { firstVisibleItemIndex >= count && state.flatList.isNotEmpty() }
    }
    val scrollToTopBtnSize: Dp = 48.dp
    val localDensity = LocalDensity.current

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        container.store.intent(NotificationListIntent.IsNotificationPermissionGranted)
    }

    LaunchedEffect(Unit) {
        container.store.intent(NotificationListIntent.LoadSubscriptions)
        container.store.intent(NotificationListIntent.ListViewCheck)
    }

    if (showNotificationsSettingsDialog) {
        SystemSettingsDialog(
            onDismissRequest = { showNotificationsSettingsDialog = false },
            onConfirmClick = {
                showNotificationsSettingsDialog = false
                permissionsController.openAppSettings()
            },
            onCloseClick = { showNotificationsSettingsDialog = false }
        )
    }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = { Text(stringResource(Res.string.notifications_title), style = MaterialTheme.typography.headlineSmall) },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.back))
                    }
                }
            )
        }) { paddingValues ->
        PullToRefreshBox(
            onRefresh = {
                if (state.isLoading.not()) {
                    container.store.intent(
                        NotificationListIntent.SearchFlats(
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
                        title = stringResource(Res.string.error),
                        message = state.errorText ?: "",
                        onDismiss = { container.store.intent(NotificationListIntent.HideNetworkErrorDialog) }
                    )
                }

                if (state.paramsDialogText != null) {
                    SimpleAlertDialog(
                        title = stringResource(Res.string.notifications_params),
                        message = state.paramsDialogText ?: "",
                        onDismiss = { container.store.intent(NotificationListIntent.HideParams) }
                    )
                }

                when {
                    state.isLoading.not() && state.flatList.isEmpty() && state.subscriptions.isEmpty() -> {
                        Column(
                            modifier = modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(12.dp)
                        ) {
                            if (notifPermSnackBarIsVisible) {
                                NotifPermissionSnackbar(
                                    onCloseSnackbarBtnClick = {
                                        notifPermSnackBarIsVisible = false
                                        container.store.intent(NotificationListIntent.CloseNotifPermMessage)
                                    },
                                    onActionSnackbarBtnClick = {
                                        container.store.intent(NotificationListIntent.ProvideNotifPermission)
                                    }
                                )
                            }

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
                            item {
                                if (notifPermSnackBarIsVisible) {
                                    NotifPermissionSnackbar(
                                        onCloseSnackbarBtnClick = {
                                            notifPermSnackBarIsVisible = false
                                            container.store.intent(NotificationListIntent.CloseNotifPermMessage)
                                        },
                                        onActionSnackbarBtnClick = {
                                            container.store.intent(NotificationListIntent.ProvideNotifPermission)
                                        }
                                    )
                                }
                            }

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
                            container.store.intent(
                                NotificationListIntent.ClickOnFavorite(
                                    it.flatPlatform,
                                    it.adId
                                )
                            )
                        },
                        onLoadMore = {
                            container.store.intent(NotificationListIntent.SearchFlats(true))
                        },
                        topContent = {
                            topSubscriptionsContent(
                                state = state,
                                notifPermSnackBarIsVisible = notifPermSnackBarIsVisible,
                                onSelect = { id ->
                                    container.store.intent(
                                        NotificationListIntent.SelectSubscription(
                                            id
                                        )
                                    )
                                },
                                onShowParams = { id ->
                                    container.store.intent(
                                        NotificationListIntent.ShowParams(id)
                                    )
                                },
                                onDelete = { id ->
                                    container.store.intent(
                                        NotificationListIntent.DeleteSubscription(
                                            id
                                        )
                                    )
                                },
                                onToggleView = {
                                    container.store.intent(NotificationListIntent.ToggleView)
                                },
                                onCloseSnackbarBtnClick = {
                                    notifPermSnackBarIsVisible = false
                                    container.store.intent(NotificationListIntent.CloseNotifPermMessage)
                                },
                                onActionSnackbarBtnClick = {
                                    container.store.intent(NotificationListIntent.ProvideNotifPermission)
                                },
                            )
                        },
                        bottomContent = {
                            if (state.noFlatsToLoadMore) {
                                item {
                                    LoadMoreForce(state.currentSearchPage) {
                                        clickOnLoadMore(container)
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

private fun clickOnLoadMore(container: NotificationListContainer) {
    container.store.intent(
        NotificationListIntent.SearchFlats(
            isLoadMore = true,
            isLoadMoreForce = true
        )
    )
}


private fun LazyListScope.topSubscriptionsContent(
    state: NotificationListScreenState,
    notifPermSnackBarIsVisible: Boolean,
    onToggleView: () -> Unit,
    onSelect: (String) -> Unit,
    onShowParams: (String) -> Unit,
    onDelete: (String) -> Unit,
    onCloseSnackbarBtnClick: () -> Unit = {},
    onActionSnackbarBtnClick: () -> Unit = {},
) {
    item {
        if (notifPermSnackBarIsVisible) {
            NotifPermissionSnackbar(onCloseSnackbarBtnClick, onActionSnackbarBtnClick)
        }
    }

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
                                text = { Text(stringResource(Res.string.notifications_params)) },
                                onClick = { menuExpanded = false; onShowParams(sub.id) }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.delete)) },
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

@Composable
private fun NotifPermissionSnackbar(
    onCloseSnackbarBtnClick: () -> Unit,
    onActionSnackbarBtnClick: () -> Unit
) {
    MessageSnackbar(
        modifier = Modifier
            .padding(horizontal = 6.dp)
            .clip(RoundedCornerShape(10.dp)),
        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f),
        message = stringResource(Res.string.notifications_permission_message),
        closeBtnText = stringResource(Res.string.close),
        actionBtnText = stringResource(Res.string.notifications_allow),
        onCloseBtnClick = onCloseSnackbarBtnClick,
        onActionBtnClick = onActionSnackbarBtnClick
    )
}