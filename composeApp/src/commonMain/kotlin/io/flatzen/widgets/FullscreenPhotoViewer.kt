package io.flatzen.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import flatzen.composeapp.generated.resources.Res
import flatzen.composeapp.generated.resources.close
import io.flatzen.themes.FlatHubTheme
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun FullscreenPhotoViewer(
    imageUrls: List<String>,
    initialPage: Int,
    onDismiss: (lastPage: Int) -> Unit,
) {
    if (imageUrls.isEmpty()) return

    val safeInitialPage = initialPage.coerceIn(0, imageUrls.lastIndex)
    val pagerState = rememberPagerState(initialPage = safeInitialPage) { imageUrls.size }
    val scope = rememberCoroutineScope()
    val dismiss: () -> Unit = { onDismiss(pagerState.currentPage) }

    Dialog(
        onDismissRequest = dismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        BackHandler(onBack = dismiss)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .photoTapZones(
                        onTapLeft = {
                            if (pagerState.currentPage > 0) {
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                }
                            }
                        },
                        onTapCenter = dismiss,
                        onTapRight = {
                            if (pagerState.currentPage < imageUrls.lastIndex) {
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            }
                        },
                    ),
            ) { page ->
                AppAsyncImage(
                    imageUrl = imageUrls[page],
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                )
            }

            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = FlatHubTheme.dimens.screenHorizontalCompact),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = dismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(Res.string.close),
                        tint = Color.White,
                    )
                }
                PhotoStepBar(
                    modifier = Modifier.weight(1f),
                    count = imageUrls.size,
                    activeIndex = pagerState.currentPage,
                )
            }
        }
    }
}
