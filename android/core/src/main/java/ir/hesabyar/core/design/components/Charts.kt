package ir.hesabyar.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.hesabyar.core.design.theme.HesabyarTheme
import ir.hesabyar.domain.format.PersianText
import ir.hesabyar.domain.model.ChartPoint

@Composable
fun SimpleBarChart(
    points: List<ChartPoint>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
    chartHeight: androidx.compose.ui.unit.Dp = 130.dp,
    showValue: Boolean = false
) {
    if (points.isEmpty()) {
        Box(modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
            Text("داده‌ای برای نمایش نیست", style = MaterialTheme.typography.bodySmall, color = HesabyarTheme.colors.onSurfaceSecondary)
        }
        return
    }
    val maxValue = points.maxOf { it.value }.coerceAtLeast(1L)
    Row(
        modifier = modifier.fillMaxWidth().height(chartHeight + 40.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        points.forEach { point ->
            val fraction = (point.value.coerceAtLeast(0L).toFloat() / maxValue.toFloat()).coerceIn(0f, 1f)
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (showValue && fraction > 0.45f) {
                    Text(
                        text = PersianText.formatCompact(point.value),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 8.sp,
                        color = HesabyarTheme.colors.onSurfaceSecondary,
                        maxLines = 1
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((chartHeight * fraction).coerceAtLeast(3.dp))
                        .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                        .background(if (fraction > 0f) barColor else barColor.copy(alpha = 0.15f))
                )
                Text(
                    text = PersianText.toPersianDigits(point.label),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    color = HesabyarTheme.colors.onSurfaceSecondary,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
