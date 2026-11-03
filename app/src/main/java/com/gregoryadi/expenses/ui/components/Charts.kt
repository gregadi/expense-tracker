package com.gregoryadi.expenses.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gregoryadi.expenses.data.db.GroupTotal
import com.gregoryadi.expenses.domain.Bucket
import com.gregoryadi.expenses.domain.Money
import kotlin.math.roundToInt

data class DonutSlice(val color: Color, val value: Long)

/** Ring chart. The ring sweeps round from the top whenever its data changes. [content] sits in the middle. */
@Composable
fun DonutChart(
    slices: List<DonutSlice>,
    modifier: Modifier = Modifier,
    thickness: Dp = 16.dp,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val track = MaterialTheme.colorScheme.surfaceContainerHigh
    val total = slices.sumOf { it.value }
    val sweep = remember { Animatable(0f) }
    LaunchedEffect(slices) {
        sweep.snapTo(0f)
        sweep.animateTo(1f, tween(Motion.GROW_MS + 200, easing = FastOutSlowInEasing))
    }
    Box(modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().padding(thickness / 2)) {
            val strokePx = thickness.toPx()
            drawArc(color = track, startAngle = 0f, sweepAngle = 360f, useCenter = false, style = Stroke(strokePx))
            if (total > 0L) {
                // Slices sit edge to edge with flat ends, so the ring reads as one continuous circle.
                // The ring sweeps round from the top; each slice stops at the sweep's leading edge.
                val visible = 360f * sweep.value
                val flat = Stroke(strokePx, cap = StrokeCap.Butt)
                var cumulative = 0f
                for (slice in slices) {
                    if (slice.value <= 0L) continue
                    val full = 360f * slice.value / total
                    val drawn = minOf(full, visible - cumulative)
                    if (drawn > 0f) {
                        // A hair of overlap hides the anti-aliasing seam between neighbouring slices.
                        val overlap = if (drawn < full) 0f else 0.6f
                        drawArc(
                            color = slice.color,
                            startAngle = cumulative - 90f,
                            sweepAngle = drawn + overlap,
                            useCenter = false,
                            style = flat,
                        )
                    }
                    cumulative += full
                }
            }
        }
        content()
    }
}

/** Bars that grow up from the baseline one after another when the chart appears or its data changes. */
@Composable
fun BarChart(
    buckets: List<Bucket>,
    modifier: Modifier = Modifier,
    height: Dp = 150.dp,
) {
    val max = (buckets.maxOfOrNull { it.total } ?: 0L).coerceAtLeast(1L)
    val barColor = MaterialTheme.colorScheme.primary
    val emptyColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val gap = if (buckets.size > 16) 1.dp else 3.dp
    val stagger = if (buckets.size > 16) 12 else 35
    // Restart the grow-in when the period changes (different number or labels of bars).
    val grow = remember { Animatable(0f) }
    LaunchedEffect(buckets.size, buckets.firstOrNull()?.label, buckets.sumOf { it.total }) {
        grow.snapTo(0f)
        grow.animateTo(1f, tween(Motion.GROW_MS + buckets.size * stagger, easing = FastOutSlowInEasing))
    }
    val span = (Motion.GROW_MS + buckets.size * stagger).toFloat()

    Column(modifier) {
        Row(Modifier.fillMaxWidth().height(height), verticalAlignment = Alignment.Bottom) {
            buckets.forEachIndexed { index, bucket ->
                // Each bar starts a little after the one before it.
                val start = index * stagger / span
                val local = ((grow.value - start) / (Motion.GROW_MS / span)).coerceIn(0f, 1f)
                Box(
                    Modifier.weight(1f).fillMaxHeight().padding(horizontal = gap),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    val target = if (bucket.total <= 0L) 0.03f else (bucket.total.toFloat() / max).coerceIn(0.05f, 1f)
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .fillMaxHeight((target * local).coerceAtLeast(0.001f))
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 2.dp, bottomEnd = 2.dp))
                            .background(if (bucket.total > 0L) barColor else emptyColor)
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            buckets.forEach { bucket ->
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    if (bucket.showLabel) {
                        Text(
                            text = bucket.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.wrapContentWidth(unbounded = true),
                        )
                    }
                }
            }
        }
    }
}

/**
 * One thin bar split into coloured segments, one per group (the "Paid with" strip on Home).
 * The segments grow in together from the left.
 */
@Composable
fun SegmentBar(groups: List<GroupTotal>, modifier: Modifier = Modifier, height: Dp = 10.dp) {
    val total = groups.sumOf { it.total }.coerceAtLeast(1L)
    val shown = animatedFraction(1f)
    Row(modifier.fillMaxWidth().height(height), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        if (groups.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxHeight().clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh))
        } else {
            groups.forEach { g ->
                Box(
                    Modifier
                        .weight((g.total.toFloat() / total).coerceAtLeast(0.01f))
                        .fillMaxHeight(),
                ) {
                    Box(
                        Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(shown.coerceAtLeast(0.001f))
                            .clip(CircleShape)
                            .background(Color(g.color))
                    )
                }
            }
        }
    }
}

/** Ranked list with a thin share bar per row that grows in, staggered; used for methods, categories, sources. */
@Composable
fun BreakdownList(
    items: List<GroupTotal>,
    total: Long,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        items.forEachIndexed { index, group ->
            val color = Color(group.color)
            val share = if (total > 0) group.total.toFloat() / total else 0f
            val grown = animatedFraction(share, delayMs = index * 70)
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ColorDot(color, size = 10.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = group.name,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(Money.format(group.total), style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Box(
                        Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(grown.coerceAtLeast(0.001f))
                            .clip(CircleShape)
                            .background(color)
                    )
                }
                Spacer(Modifier.height(4.dp))
                val uses = if (group.cnt == 1) "1 entry" else "${group.cnt} entries"
                Text(
                    text = "${(share * 100).roundToInt()}% · $uses",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
