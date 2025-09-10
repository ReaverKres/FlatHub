package io.flatzen.widgets

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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import flatzen.composeapp.generated.resources.Res
import flatzen.composeapp.generated.resources.domovita32
import flatzen.composeapp.generated.resources.eye
import flatzen.composeapp.generated.resources.kufar32
import flatzen.composeapp.generated.resources.onliner32
import flatzen.composeapp.generated.resources.realt32
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import org.jetbrains.compose.resources.painterResource


@Composable
fun FlatImagePager(
    modifier: Modifier = Modifier,
    flatPlatform: FlatPlatform,
    imageUrls: List<String>,
    contentScale: ContentScale = ContentScale.Crop,
    savedInFavorite: Boolean = false,
    isViewed: Boolean = false,
    clickOnFavorite: () -> Unit = {},
) {
    if (imageUrls.isNotEmpty()) {
        ImagePager(
            modifier = modifier.fillMaxWidth(),
            flatPlatform = flatPlatform,
            imageUrls = imageUrls,
            contentScale = contentScale,
            isViewed = isViewed,
            savedInFavorite = savedInFavorite,
            clickOnFavorite = clickOnFavorite
        )
    } else {
        FlatEmptyImage(flatPlatform, isViewed, savedInFavorite, clickOnFavorite)
    }
}

@Composable
private fun FlatEmptyImage(
    flatPlatform: FlatPlatform,
    isViewed: Boolean,
    savedInFavorite: Boolean,
    clickOnFavorite: () -> Unit
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
        AddToFavoriteIcon(savedInFavorite, clickOnFavorite)
        PlatformIcon(flatPlatform)
        if (isViewed) {
            EyeIcon()
        }
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

        AddToFavoriteIcon(savedInFavorite, clickOnFavorite)
        PlatformIcon(flatPlatform)
        if (isViewed) {
            EyeIcon()
        }

        // Индикатор страниц
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
    clickOnFavorite: () -> Unit = {}
) {
    Icon(
        imageVector = if (savedInFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
        contentDescription = "Добавить в избранное",
        tint = if (savedInFavorite) Color.Red else Color.Red.copy(alpha = 0.5f),
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .clickable { clickOnFavorite() }
            .padding(8.dp)
            .size(24.dp)
    )
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
fun FilterActionButton(onClick: () -> Unit, isAnyFilterApplied: Boolean) {
    Box {
        FloatingActionButton(onClick = onClick) {
            Icon(Icons.Default.Build, contentDescription = "Фильтры")
        }

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