package io.flatzen.widgets

import androidx.compose.foundation.background
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp


@Composable
fun FlatImagePager(
    modifier: Modifier = Modifier,
    imageUrls: List<String>,
    contentScale: ContentScale = ContentScale.Crop,
    savedInFavorite: Boolean = false,
    clickOnFavorite: () -> Unit = {},
) {
    if (imageUrls.isNotEmpty()) {
        ImagePager(
            modifier = modifier.fillMaxWidth(),
            imageUrls = imageUrls,
            contentScale = contentScale,
            savedInFavorite = savedInFavorite,
            clickOnFavorite = clickOnFavorite
        )
    } else {
        FlatEmptyImage(savedInFavorite, clickOnFavorite)
    }
}

@Composable
private fun FlatEmptyImage(savedInFavorite: Boolean, clickOnFavorite: () -> Unit) {
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
    }
}

@Composable
private fun ImagePager(
    modifier: Modifier = Modifier,
    imageUrls: List<String>,
    contentScale: ContentScale = ContentScale.Crop,
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
    IconButton(
        onClick = { clickOnFavorite() },
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(8.dp)
            .size(24.dp)
    ) {
        Icon(
            imageVector = if (savedInFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            contentDescription = "Добавить в избранное",
            tint = if (savedInFavorite) Color.Red else Color.Red.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxSize()
        )
    }
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