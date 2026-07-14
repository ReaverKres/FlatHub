package io.flatzen.screens.swipe

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import flatzen.composeapp.generated.resources.Res
import flatzen.composeapp.generated.resources.swipe_onboarding_detail
import flatzen.composeapp.generated.resources.swipe_onboarding_got_it
import flatzen.composeapp.generated.resources.swipe_onboarding_photo
import flatzen.composeapp.generated.resources.swipe_onboarding_swipe
import flatzen.composeapp.generated.resources.swipe_onboarding_title
import io.flatzen.themes.FlatHubTheme
import io.flatzen.widgets.FlatZenPrimaryButton
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

@Composable
fun SwipeOnboardingOverlay(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Scrim is always dark; onPrimary is dark in dark theme and would be unreadable.
    val overlayContentColor = Color.White

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = {},
            )
            .navigationBarsPadding()
            .padding(horizontal = FlatHubTheme.dimens.screenHorizontal),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(Res.string.swipe_onboarding_title),
                style = MaterialTheme.typography.headlineSmall,
                color = overlayContentColor,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                SwipeOnboardingDemoCard()
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OnboardingTip(
                    text = stringResource(Res.string.swipe_onboarding_swipe),
                    color = overlayContentColor,
                )
                OnboardingTip(
                    text = stringResource(Res.string.swipe_onboarding_photo),
                    color = overlayContentColor,
                )
                OnboardingTip(
                    text = stringResource(Res.string.swipe_onboarding_detail),
                    color = overlayContentColor,
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            FlatZenPrimaryButton(
                text = stringResource(Res.string.swipe_onboarding_got_it),
                onClick = onComplete,
            )
        }
    }
}

@Composable
private fun OnboardingTip(
    text: String,
    color: Color,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = color,
        textAlign = TextAlign.Start,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun SwipeOnboardingDemoCard() {
    val offsetX = remember { Animatable(0f) }
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth(0.78f),
        contentAlignment = Alignment.Center,
    ) {
        val maxOffset = with(density) { (maxWidth * 0.42f).toPx() }

        LaunchedEffect(maxOffset) {
            while (true) {
                delay(350)
                offsetX.animateTo(
                    targetValue = -maxOffset,
                    animationSpec = tween(520, easing = FastOutSlowInEasing),
                )
                delay(450)
                offsetX.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(320, easing = FastOutSlowInEasing),
                )
                delay(280)
                offsetX.animateTo(
                    targetValue = maxOffset,
                    animationSpec = tween(520, easing = FastOutSlowInEasing),
                )
                delay(450)
                offsetX.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(320, easing = FastOutSlowInEasing),
                )
                delay(500)
            }
        }

        val progress = (offsetX.value / maxOffset.coerceAtLeast(1f)).coerceIn(-1.5f, 1.5f)
        val likeAlpha = progress.coerceIn(0f, 1f)
        val dislikeAlpha = (-progress).coerceIn(0f, 1f)

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .graphicsLayer {
                    translationX = offsetX.value
                    rotationZ = offsetX.value / 48f
                },
            shape = FlatHubTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            shadowElevation = 8.dp,
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = FlatHubTheme.shapes.small,
                            ),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.55f)
                            .height(14.dp)
                            .background(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f),
                                shape = FlatHubTheme.shapes.extraSmall,
                            ),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(12.dp)
                            .background(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                shape = FlatHubTheme.shapes.extraSmall,
                            ),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.65f)
                            .height(12.dp)
                            .background(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                shape = FlatHubTheme.shapes.extraSmall,
                            ),
                    )
                }

                if (likeAlpha > 0.05f) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = FlatHubTheme.semantic.swipeLike.copy(alpha = likeAlpha),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(88.dp)
                            .graphicsLayer { rotationZ = -12f },
                    )
                }
                if (dislikeAlpha > 0.05f) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = dislikeAlpha),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(88.dp)
                            .graphicsLayer { rotationZ = 12f },
                    )
                }
            }
        }
    }
}
