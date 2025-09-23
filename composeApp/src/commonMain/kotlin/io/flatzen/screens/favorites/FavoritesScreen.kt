package io.flatzen.screens.favorites

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import flatzen.composeapp.generated.resources.Res
import flatzen.composeapp.generated.resources.favorite_is_empty
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import io.flatzen.kmpapp.screens.EmptyScreenContent
import io.flatzen.screens.home.FlatList
import io.flatzen.viewmodel.FavoritesScreenAction
import io.flatzen.viewmodel.FavoritesViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun FavoritesScreen(
    navigateToDetails: (flatPlatform: FlatPlatform, objectId: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = koinViewModel<FavoritesViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(modifier = modifier) { paddingValues ->
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
                    viewModel.onIntent(FavoritesScreenAction.ClickOnFavorite(it.flatPlatform, it.adId))
                },
                onLoadMore = { }
            )
        }
    }
}