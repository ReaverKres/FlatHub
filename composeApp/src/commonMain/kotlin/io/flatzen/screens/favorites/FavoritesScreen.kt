package io.flatzen.screens.favorites

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.flatzen.commoncomponents.commonentities.FlatPlatform

import io.flatzen.viewmodel.FilterViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun FavoritesScreen(
    navigateToDetails: (flatPlatform: FlatPlatform, objectId: Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FilterViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

//    if (state.favorites.isEmpty()) {
//        EmptyScreenContent(modifier = modifier, message = "Вы еще ничего не добавили в избранное")
//    } else {
//        FlatGrid(
//            flats = state.favorites.toList(),
//            onFlatClick = { navigateToDetails(it.flatPlatform, it.adId) },
//            modifier = modifier
//        )
//    }
}