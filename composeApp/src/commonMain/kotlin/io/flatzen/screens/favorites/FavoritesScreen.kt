package io.flatzen.screens.favorites

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import flatzen.composeapp.generated.resources.Res
import flatzen.composeapp.generated.resources.favorite_is_empty
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import io.flatzen.kmpapp.screens.EmptyScreenContent
import io.flatzen.screens.home.FlatList
import io.flatzen.viewmodel.FavoritesScreenAction
import io.flatzen.viewmodel.FavoritesViewModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    navigateToDetails: (flatPlatform: FlatPlatform, objectId: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = koinViewModel<FavoritesViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = { Text("Избранное", style = MaterialTheme.typography.headlineMedium) },
            )
        }) { paddingValues ->
        Box(modifier.fillMaxSize().padding(paddingValues)) {
            when {
                state.flatList.isEmpty() -> EmptyScreenContent(
                    modifier = Modifier.fillMaxSize(),
                    stringResource = Res.string.favorite_is_empty
                )

                else -> FlatList(
                    isLoadingMore = false,
                    flats = state.flatList,
                    onFlatClick = { navigateToDetails(it.flatPlatform, it.adId) },
                    clickOnFavorite = {
                        viewModel.onIntent(
                            FavoritesScreenAction.ClickOnFavorite(
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