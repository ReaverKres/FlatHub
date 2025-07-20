package io.flatzen.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.LocalPlatformContext
import coil3.compose.SubcomposeAsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import io.flatzen.uiExtensions.thenIf

@Composable
fun AppAsyncImage(
    modifier: Modifier = Modifier,
    imageUrl: String? = null,
    contentScale: ContentScale = ContentScale.FillBounds,
    shape: Shape = RoundedCornerShape(10.dp),
    onPlaceholderClick: (() -> Unit)? = null,
    placeholder: @Composable () -> Unit = {
//        AsyncImagePlaceholder(modifier, onPlaceholderClick, shape)
    },
    cacheEnabled: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    if (imageUrl.isNullOrEmpty()) {
        placeholder()
        return
    }
    val context = LocalPlatformContext.current
    val imageRequest = if (cacheEnabled) {
        ImageRequest.Builder(context)
            .data(imageUrl)
            .memoryCacheKey(imageUrl)
            .diskCacheKey(imageUrl)
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .build()
    } else {
        ImageRequest.Builder(context)
            .data(data = imageUrl)
            .build()
    }

    SubcomposeAsyncImage(
        model = imageRequest,
        contentDescription = null,
        contentScale = contentScale,
        modifier = modifier
            .clip(shape)
            .thenIf( onClick != null ) { clickable(onClick = onClick!!) },
        loading = { placeholder() },
        onError = { error -> },
        error = { placeholder() }
    )
}

//@Composable
//private fun AsyncImagePlaceholder(modifier: Modifier, onPlaceholderClick: (() -> Unit)?, shape: Shape) {
//    Box(modifier = modifier
//        .background(AppTheme.appColors.cardPlaceholder, shape)
//        .thenIf( onPlaceholderClick != null ) { clickable(onClick = onPlaceholderClick!!) }
//    )