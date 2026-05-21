package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TransactionEntity
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TrendsTab(
    transactions: List<TransactionEntity>,
    modifier: Modifier = Modifier
) {
    val expensesOnly = transactions.filter { it.type == "EXPENSE" }

    // Computations: Group by category
    val categoryTotals = expensesOnly
        .groupBy { it.category }
        .mapValues { entry -> entry.value.sumOf { it.amount } }
        .toList()
        .sortedByDescending { it.second }

    val totalSpent = categoryTotals.sumOf { it.second }

    // Computations: Past 7 days transaction chart
    val last7DayBars = remember(transactions) {
        val formatter = SimpleDateFormat("E", Locale.getDefault())
        val cal = Calendar.getInstance()

        // Generate 7 days ending today
        (0..6).map { i ->
            val dayCal = Calendar.getInstance()
            dayCal.add(Calendar.DAY_OF_YEAR, -i)
            dayCal.set(Calendar.HOUR_OF_DAY, 0)
            dayCal.set(Calendar.MINUTE, 0)
            dayCal.set(Calendar.SECOND, 0)
            dayCal.set(Calendar.MILLISECOND, 0)

            val startMillis = dayCal.timeInMillis
            val endMillis = startMillis + (24 * 60 * 60 * 1000)

            val sumStr = transactions
                .filter { it.type == "EXPENSE" && it.dateMillis in startMillis until endMillis }
                .sumOf { it.amount }

            val label = formatter.format(dayCal.time)
            Pair(label, sumStr)
        }.reversed()
    }

    val maxBarValue = last7DayBars.maxOfOrNull { it.second } ?: 100.0
    val finalMaxBarValue = if (maxBarValue == 0.0) 100.0 else maxBarValue

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("trends_tab_root"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title
        item {
            Column {
                Text(
                    text = "Financial Trends",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Spending & Insights",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Daily Expenditures Timeline (Bar Chart)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Daily Outflow (Past 7 Days)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Draw the custom Bar chart
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .testTag("expense_bar_chart"),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val barCount = last7DayBars.size
                            val spacing = size.width / (barCount * 3 + 1)
                            val barWidth = spacing * 2
                            val chartHeight = size.height - 40f // Margin for text

                            // Draw baseline grid rules
                            val gridCount = 4
                            for (gridIdx in 0..gridCount) {
                                val yVal = chartHeight * (gridIdx.toFloat() / gridCount)
                                drawLine(
                                    color = Color.LightGray.copy(alpha = 0.3f),
                                    start = Offset(0f, yVal),
                                    end = Offset(size.width, yVal),
                                    strokeWidth = 2f
                                )
                            }

                            // Draw bars
                            last7DayBars.forEachIndexed { index, pair ->
                                val label = pair.first
                                val value = pair.second
                                val barHeight = chartHeight * (value / finalMaxBarValue).toFloat()

                                val startX = spacing + index * (barWidth + spacing)
                                val startY = chartHeight // Start drawing bottom-up from here

                                // Draw bar if > 0
                                if (barHeight > 0) {
                                    val topY = chartHeight - barHeight
                                    drawRoundRect(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                Color(0xFF2196F3),
                                                Color(0xFF00E5FF)
                                            )
                                        ),
                                        topLeft = Offset(startX, topY),
                                        size = Size(barWidth, barHeight),
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f)
                                    )
                                } else {
                                    // Empty state placeholder indicator line
                                    val dotY = chartHeight - 4f
                                    drawLine(
                                        color = Color.LightGray.copy(alpha = 0.5f),
                                        start = Offset(startX, dotY),
                                        end = Offset(startX + barWidth, dotY),
                                        strokeWidth = 4f
                                    )
                                }
                            }
                        }

                        // Labels placed at absolute bottom positions using row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(24.dp)
                                .align(Alignment.BottomCenter),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            last7DayBars.forEach { pair ->
                                Text(
                                    text = pair.first,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.width(36.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Peak spend indicator text
                    if (maxBarValue > 0) {
                        Text(
                            text = "Peak spending day reached ${formatCurrency(maxBarValue)} in transactions logo.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Spending Categories Breakdown (Donut Chart)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Outflow Breakdown",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    if (totalSpent == 0.0) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "No spending records to visualize",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        // Pie / Donut canvas
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .testTag("spend_donut_chart"),
                            contentAlignment = Alignment.Center
                        ) {
                            val colorScheme = MaterialTheme.colorScheme
                            Canvas(modifier = Modifier.size(160.dp)) {
                                var accumulatedAngle = -90f // Start from the top noon position

                                categoryTotals.forEach { catTotal ->
                                    val catName = catTotal.first
                                    val catAmt = catTotal.second
                                    val catStyle = FinanceCategory.getStyleFor(catName)

                                    val sweepAngle = 360f * (catAmt / totalSpent).toFloat()

                                    // Draw arc slice
                                    drawArc(
                                        color = catStyle.color,
                                        startAngle = accumulatedAngle,
                                        sweepAngle = sweepAngle,
                                        useCenter = false,
                                        style = Stroke(width = 30.dp.toPx(), cap = StrokeCap.Round),
                                        size = Size(size.width, size.height)
                                    )

                                    accumulatedAngle += sweepAngle
                                }
                            }

                            // Center overlay texts
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "Total Spent",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = formatCurrency(totalSpent),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Category Labels list with bullet indicators
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            categoryTotals.forEach { entry ->
                                val pct = (entry.second / totalSpent) * 100
                                val style = FinanceCategory.getStyleFor(entry.first)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .background(style.color, CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = entry.first,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Row(
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${pct.toInt()}%",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(end = 12.dp)
                                        )
                                        Text(
                                            text = formatCurrency(entry.second),
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
