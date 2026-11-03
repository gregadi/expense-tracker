package com.gregoryadi.expenses.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.gregoryadi.expenses.domain.Money
import kotlin.math.roundToLong

/**
 * Shared motion. Every animation here is short (under ~700 ms) and eases out, so the app feels quick.
 * Compose follows the phone's "Remove animations" setting, so these switch off with it.
 */
object Motion {
    const val COUNT_MS = 700
    const val GROW_MS = 650
    const val ENTER_MS = 260

    /** Springy, slightly bouncy: presses, selection pills, sliding indicators. */
    fun <T> bouncy() = spring<T>(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow)
}

/** False on the first frame and true right after, so things can animate in from zero when a screen opens. */
@Composable
fun rememberAppeared(): Boolean {
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    return appeared
}

/**
 * Animates a growing fraction (0..1): starts at 0 when the screen opens, then follows [target].
 * [delayMs] staggers neighbouring bars.
 */
@Composable
fun animatedFraction(target: Float, delayMs: Int = 0): Float {
    val appeared = rememberAppeared()
    val value by animateFloatAsState(
        targetValue = if (appeared) target.coerceIn(0f, 1f) else 0f,
        animationSpec = tween(Motion.GROW_MS, delayMillis = delayMs, easing = FastOutSlowInEasing),
        label = "fraction",
    )
    return value
}

/**
 * A rupiah amount that counts up when it first shows and rolls to the new value whenever it changes.
 * [format] turns the number into text (default "Rp 25.000").
 */
@Composable
fun AnimatedAmount(
    amount: Long,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
    format: (Long) -> String = Money::format,
) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(amount) {
        anim.animateTo(amount.toFloat(), tween(Motion.COUNT_MS, easing = FastOutSlowInEasing))
    }
    // Floats lose precision above ~16 million, so show the exact amount once the roll has finished.
    val shown = if (anim.isRunning) anim.value.roundToLong() else amount
    Text(
        text = format(shown),
        style = style,
        color = color,
        textAlign = textAlign,
        maxLines = 1,
        overflow = TextOverflow.Clip,
        modifier = modifier,
    )
}

/**
 * Clickable that shrinks slightly while pressed and springs back: the app's standard tap feedback.
 * Apply it after size/shape modifiers and before background, so the whole shape scales.
 */
@Composable
fun Modifier.bouncyClick(
    enabled: Boolean = true,
    pressedScale: Float = 0.94f,
    onClick: () -> Unit,
): Modifier {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) pressedScale else 1f,
        animationSpec = Motion.bouncy(),
        label = "press",
    )
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = source,
            indication = LocalIndication.current,
            enabled = enabled,
            onClick = onClick,
        )
}
