package io.flatzen.screens.favorites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import io.flatzen.kmpapp.screens.EmptyScreenContent
import io.flatzen.screens.list.FlatGrid
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
            state.flatList.isEmpty() -> EmptyScreenContent()
            else -> FlatGrid(
                isLoadingMore = false,
                noFlatsToLoadMore = false,
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