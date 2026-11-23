package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.LocalNoorGlassColors
import kotlin.math.cos
import kotlin.math.sin

fun toBanglaDigits(input: String): String {
    val map = mapOf(
        '0' to '০', '1' to '১', '2' to '২', '3' to '৩', '4' to '৪',
        '5' to '৫', '6' to '৬', '7' to '৭', '8' to '৮', '9' to '৯'
    )
    return buildString(input.length) {
        for (ch in input) {
            append(map[ch] ?: ch)
        }
    }
}

fun Number.toBn(): String = toBanglaDigits(this.toString())

@Composable
fun IslamicBackdrop(
    modifier: Modifier = Modifier,
    showMosqueHeaderArt: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val noor = LocalNoorGlassColors.current
    val gradientTop = if (noor.isDark) Color(0xFF0B1C16) else Color(0xFFF5F1E8)
    val gradientMid = if (noor.isDark) Color(0xFF10271F) else Color(0xFFEEE8DA)
    val gradientBottom = if (noor.isDark) Color(0xFF14352A) else Color(0xFFE7EFEA)
    val starColor = noor.accentGold.copy(alpha = if (noor.isDark) 0.08f else 0.11f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(gradientTop, gradientMid, gradientBottom))
            )
            .drawBehind {
                // Subtle Islamic 8-pointed geometric stars lattice
                val step = 180f
                val radius = 48f
                var y = 80f
                while (y < size.height) {
                    var x = 60f
                    while (x < size.width) {
                        drawIslamicEightStar(Offset(x, y), radius, starColor)
                        x += step
                    }
                    y += step
                }
            }
    ) {
        if (showMosqueHeaderArt) {
            Image(
                painter = painterResource(
                    id = if (noor.isDark) R.drawable.img_mosque_night else R.drawable.img_mosque_day
                ),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .alpha(if (noor.isDark) 0.24f else 0.18f)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(225.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                gradientMid.copy(alpha = 0.85f),
                                gradientMid
                            )
                        )
                    )
            )
        }
        content()
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawIslamicEightStar(
    center: Offset,
    radius: Float,
    color: Color
) {
    val path1 = Path()
    val path2 = Path()
    for (i in 0 until 4) {
        val angle1 = Math.toRadians((i * 90.0))
        val angle2 = Math.toRadians((i * 90.0 + 45.0))
        val p1 = Offset(center.x + (radius * cos(angle1)).toFloat(), center.y + (radius * sin(angle1)).toFloat())
        val p2 = Offset(center.x + (radius * cos(angle2)).toFloat(), center.y + (radius * sin(angle2)).toFloat())
        if (i == 0) {
            path1.moveTo(p1.x, p1.y)
            path2.moveTo(p2.x, p2.y)
        } else {
            path1.lineTo(p1.x, p1.y)
            path2.lineTo(p2.x, p2.y)
        }
    }
    path1.close()
    path2.close()
    drawPath(path1, color = color, style = Stroke(width = 1.4f))
    drawPath(path2, color = color, style = Stroke(width = 1.4f))
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    elevated: Boolean = false,
    cornerRadius: Dp = 22.dp,
    onClick: (() -> Unit)? = null,
    contentPadding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val noor = LocalNoorGlassColors.current
    val shape = RoundedCornerShape(cornerRadius)
    val bgColor by animateColorAsState(
        targetValue = if (elevated) noor.glassCardElevated else noor.glassCard,
        label = "glassBg"
    )
    Surface(
        modifier = modifier
            .clip(shape)
            .then(
                if (onClick != null) {
                    Modifier
                        .minimumInteractiveComponentSize()
                        .clickable(onClick = onClick)
                } else Modifier
            ),
        shape = shape,
        color = bgColor,
        border = BorderStroke(1.dp, noor.glassBorder),
        tonalElevation = if (noor.isDark) 2.dp else 4.dp
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content
        )
    }
}

@Composable
fun NoorSectionHeader(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val noor = LocalNoorGlassColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            if (icon != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = noor.accentGold.copy(alpha = 0.18f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (noor.isDark) noor.accentGold else noor.emeraldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = noor.textPrimary,
                    fontWeight = FontWeight.Bold
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = noor.textSecondary
                    )
                }
            }
        }
        if (trailing != null) {
            trailing()
        }
    }
}

@Composable
fun NoorProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp
) {
    val noor = LocalNoorGlassColors.current
    LinearProgressIndicator(
        progress = { progress.coerceIn(0f, 1f) },
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height)),
        color = if (noor.isDark) noor.accentGold else noor.emeraldPrimary,
        trackColor = noor.glassBorder.copy(alpha = 0.35f)
    )
}

@Composable
fun NoorBadgeChip(
    text: String,
    color: Color? = null,
    icon: ImageVector? = null
) {
    val noor = LocalNoorGlassColors.current
    val chipTint = color ?: if (noor.isDark) noor.accentGold else noor.emeraldPrimary
    Surface(
        shape = RoundedCornerShape(50),
        color = chipTint.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, chipTint.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = chipTint,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = chipTint,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
