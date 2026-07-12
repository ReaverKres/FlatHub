package io.flatzen.widgets

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import flatzen.composeapp.generated.resources.Res
import flatzen.composeapp.generated.resources.domovita32
import flatzen.composeapp.generated.resources.eye
import flatzen.composeapp.generated.resources.filters_title
import flatzen.composeapp.generated.resources.kufar32
import flatzen.composeapp.generated.resources.onliner32
import flatzen.composeapp.generated.resources.realt32
import flatzen.composeapp.generated.resources.tab_favorites
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import io.flatzen.themes.FlatHubTheme
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalResourceApi::class)
@Composable
fun SwipeCardsIcon(
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    tint: Color = LocalContentColor.current,
) {
    AsyncImage(
        model = Res.getUri("drawable/swipe_cards.svg"),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = ContentScale.Fit,
        colorFilter = ColorFilter.tint(tint),
    )
}


@Composable
fun FlatImagePager(
    modifier: Modifier = Modifier,
    flatPlatform: FlatPlatform,
    imageUrls: List<String>,
    contentScale: ContentScale = ContentScale.Crop,
    savedInFavorite: Boolean = false,
    saveInFavoriteInProgress: Boolean = false,
    isViewed: Boolean = false,
    disliked: Boolean = false,
    clickOnFavorite: () -> Unit = {},
    clickOnClearDislike: () -> Unit = {},
) {
    if (imageUrls.isNotEmpty()) {
        ImagePager(
            modifier = modifier.fillMaxWidth(),
            flatPlatform = flatPlatform,
            imageUrls = imageUrls,
            contentScale = contentScale,
            isViewed = isViewed,
            savedInFavorite = savedInFavorite,
            saveInFavoriteInProgress = saveInFavoriteInProgress,
            disliked = disliked,
            clickOnFavorite = clickOnFavorite,
            clickOnClearDislike = clickOnClearDislike,
        )
    } else {
        FlatEmptyImage(
            flatPlatform = flatPlatform,
            isViewed = isViewed,
            savedInFavorite = savedInFavorite,
            saveInFavoriteInProgress = saveInFavoriteInProgress,
            disliked = disliked,
            clickOnFavorite = clickOnFavorite,
            clickOnClearDislike = clickOnClearDislike,
        )
    }
}

@Composable
private fun FlatEmptyImage(
    flatPlatform: FlatPlatform,
    isViewed: Boolean,
    savedInFavorite: Boolean,
    saveInFavoriteInProgress: Boolean,
    disliked: Boolean,
    clickOnFavorite: () -> Unit,
    clickOnClearDislike: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Face,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        AddToFavoriteIcon(
            savedInFavorite = savedInFavorite,
            saveInFavoriteInProgress = saveInFavoriteInProgress,
            clickOnFavorite = clickOnFavorite,
        )
        PlatformIcon(flatPlatform)
        TopLeftStatusIcon(
            isViewed = isViewed,
            disliked = disliked,
            clickOnClearDislike = clickOnClearDislike,
        )
    }
}

@Composable
private fun ImagePager(
    modifier: Modifier = Modifier,
    flatPlatform: FlatPlatform,
    imageUrls: List<String>,
    contentScale: ContentScale = ContentScale.Crop,
    isViewed: Boolean,
    savedInFavorite: Boolean = false,
    clickOnFavorite: () -> Unit = {},
    clickOnClearDislike: () -> Unit = {},
    saveInFavoriteInProgress: Boolean,
    disliked: Boolean,
) {
    val pagerState = rememberPagerState { imageUrls.size }

    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            AppAsyncImage(
                imageUrl = imageUrls[page],
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale
            )
        }

        AddToFavoriteIcon(
            savedInFavorite = savedInFavorite,
            saveInFavoriteInProgress = saveInFavoriteInProgress,
            clickOnFavorite = clickOnFavorite,
        )
        PlatformIcon(flatPlatform)
        TopLeftStatusIcon(
            isViewed = isViewed,
            disliked = disliked,
            clickOnClearDislike = clickOnClearDislike,
        )

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            repeat(imageUrls.size) { index ->
                val color = if (index == pagerState.currentPage) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                }
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(color)
                )
            }
        }
    }
}

@Composable
fun BoxScope.AddToFavoriteIcon(
    savedInFavorite: Boolean = false,
    saveInFavoriteInProgress: Boolean = false,
    clickOnFavorite: () -> Unit = {},
) {
    val rotation by animateFloatAsState(
        targetValue = if (saveInFavoriteInProgress) 360f else 0f,
        animationSpec = if (saveInFavoriteInProgress) {
            infiniteRepeatable(
                animation = tween(durationMillis = 1000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            )
        } else {
            tween(durationMillis = 300)
        }
    )
    Icon(
        imageVector = if (savedInFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
        contentDescription = stringResource(Res.string.tab_favorites),
        tint = if (savedInFavorite) Color.Red else Color.Red.copy(alpha = 0.5f),
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .clickable { clickOnFavorite() }
            .padding(8.dp)
            .size(24.dp)
            .rotate(rotation)
    )
}

@Composable
fun BoxScope.TopLeftStatusIcon(
    isViewed: Boolean,
    disliked: Boolean,
    clickOnClearDislike: () -> Unit,
) {
    when {
        isViewed -> EyeIcon()
        disliked -> SwipeCardsIcon(
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.85f),
            modifier = Modifier
                .align(Alignment.TopStart)
                .clickable { clickOnClearDislike() }
                .padding(8.dp)
                .size(24.dp),
        )
    }
}

@Composable
fun FlatPlatform.platformImage(): Painter {
    return when (this) {
        FlatPlatform.ONLINER -> painterResource(Res.drawable.onliner32)
        FlatPlatform.KUFAR -> painterResource(Res.drawable.kufar32)
        FlatPlatform.REALT -> painterResource(Res.drawable.realt32)
        FlatPlatform.DOMOVITA -> painterResource(Res.drawable.domovita32)
    }
}

@Composable
fun BoxScope.PlatformIcon(flatPlatform: FlatPlatform) {
    Image(
        modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(8.dp)
            .size(24.dp),
        painter = flatPlatform.platformImage(),
        contentDescription = null,
    )
}

@Composable
fun BoxScope.EyeIcon() {
    Image(
        modifier = Modifier
            .align(Alignment.TopStart)
            .padding(8.dp)
            .size(24.dp),
        painter = painterResource(Res.drawable.eye),
        contentDescription = null,
    )
}

@Composable
fun FlatZenFloatingActionButton(
    onClick: () -> Unit,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.secondary,
        contentColor = MaterialTheme.colorScheme.onSecondary,
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = FlatHubTheme.dimens.elevationFab,
            pressedElevation = FlatHubTheme.dimens.elevationFab,
            focusedElevation = FlatHubTheme.dimens.elevationFab,
            hoveredElevation = FlatHubTheme.dimens.elevationFab,
        ),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSecondary,
        )
    }
}

@Composable
fun FilterActionButton(onClick: () -> Unit, isAnyFilterApplied: Boolean) {
    Box {
        FlatZenFloatingActionButton(
            onClick = onClick,
            icon = Icons.Default.Build,
            contentDescription = stringResource(Res.string.filters_title),
        )

        if (isAnyFilterApplied) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(Color.Red)
            )
        }
    }
}