package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
    // Computations: Past 6 months transaction chart (Monthly Outflow)
    val monthlyBars = remember(transactions) {
        val formatter = SimpleDateFormat("MMM", Locale.getDefault())
        // Generate 6 months ending with the current month
        (0..5).map { i ->
            val monthCal = Calendar.getInstance()
            monthCal.add(Calendar.MONTH, -i)
            
            monthCal.set(Calendar.DAY_OF_MONTH, 1)
            monthCal.set(Calendar.HOUR_OF_DAY, 0)
            monthCal.set(Calendar.MINUTE, 0)
            monthCal.set(Calendar.SECOND, 0)
            monthCal.set(Calendar.MILLISECOND, 0)
            val startMillis = monthCal.timeInMillis

            val endCal = Calendar.getInstance()
            endCal.timeInMillis = startMillis
            endCal.add(Calendar.MONTH, 1)
            val endMillis = endCal.timeInMillis

            val sumAmt = transactions
                .filter { it.type == "EXPENSE" && it.dateMillis in startMillis until endMillis }
                .sumOf { it.amount }

            val label = formatter.format(monthCal.time)
            Pair(label, sumAmt)
        }.reversed()
    }

    val maxBarValue = monthlyBars.maxOfOrNull { it.second } ?: 100.0
    val finalMaxBarValue = if (maxBarValue == 0.0) 100.0 else maxBarValue

    // Computations: Past 6 months details for Month-wise Summary
    val monthSummaries = remember(transactions) {
        val formatter = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        (0..5).map { i ->
            val monthCal = Calendar.getInstance()
            monthCal.add(Calendar.MONTH, -i)
            
            monthCal.set(Calendar.DAY_OF_MONTH, 1)
            monthCal.set(Calendar.HOUR_OF_DAY, 0)
            monthCal.set(Calendar.MINUTE, 0)
            monthCal.set(Calendar.SECOND, 0)
            monthCal.set(Calendar.MILLISECOND, 0)
            val startMillis = monthCal.timeInMillis

            val endCal = Calendar.getInstance()
            endCal.timeInMillis = startMillis
            endCal.add(Calendar.MONTH, 1)
            val endMillis = endCal.timeInMillis

            val income = transactions
                .filter { it.type == "INCOME" && it.dateMillis in startMillis until endMillis }
                .sumOf { it.amount }

            val expense = transactions
                .filter { it.type == "EXPENSE" && it.dateMillis in startMillis until endMillis }
                .sumOf { it.amount }

            val label = formatter.format(monthCal.time)
            Triple(label, income, expense)
        }
    }

    // State for interactive month view selection for Outflow Breakdown Donut Chart
    var selectedMonthForBreakdown by remember { mutableStateOf("ALL") }

    // Group by category filtered by selected month
    val expensesFiltered = remember(transactions, selectedMonthForBreakdown) {
        if (selectedMonthForBreakdown == "ALL") {
            transactions.filter { it.type == "EXPENSE" }
        } else {
            val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            transactions.filter { tx ->
                if (tx.type != "EXPENSE") return@filter false
                val txMonthStr = sdf.format(Date(tx.dateMillis))
                txMonthStr == selectedMonthForBreakdown
            }
        }
    }

    val categoryTotals = expensesFiltered
        .groupBy { it.category }
        .mapValues { entry -> entry.value.sumOf { it.amount } }
        .toList()
        .sortedByDescending { it.second }

    val totalSpent = categoryTotals.sumOf { it.second }

    val selectedDailySpends = remember(transactions, selectedMonthForBreakdown) {
        val cal = Calendar.getInstance()
        if (selectedMonthForBreakdown != "ALL") {
            try {
                val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                val parsedDate = sdf.parse(selectedMonthForBreakdown)
                if (parsedDate != null) {
                    cal.time = parsedDate
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        val year = cal.get(Calendar.YEAR)
        val monthIdx = cal.get(Calendar.MONTH)
        
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val dailyMap = (1..daysInMonth).associateWith { 0.0 }.toMutableMap()
        
        val tempCal = Calendar.getInstance()
        transactions.filter { it.type == "EXPENSE" }.forEach { tx ->
            tempCal.timeInMillis = tx.dateMillis
            if (tempCal.get(Calendar.YEAR) == year && tempCal.get(Calendar.MONTH) == monthIdx) {
                val day = tempCal.get(Calendar.DAY_OF_MONTH)
                dailyMap[day] = (dailyMap[day] ?: 0.0) + tx.amount
            }
        }
        
        dailyMap.toList().sortedBy { it.first }
    }

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

        // Monthly Expenditures Timeline (Bar Chart)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Monthly Outflow (Past 6 Months)",
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
                            val barCount = monthlyBars.size
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
                            monthlyBars.forEachIndexed { index, pair ->
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
                            monthlyBars.forEach { pair ->
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
                            text = "Peak monthly spending reached ${formatCurrency(maxBarValue)}.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Daily Outflow Chart (for the selected month)
        item {
            val selectedMonthName = if (selectedMonthForBreakdown == "ALL") {
                val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                sdf.format(Date())
            } else {
                selectedMonthForBreakdown
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Daily Outflow Timeline",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Daily expenses for $selectedMonthName",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val maxDailySpend = selectedDailySpends.maxOfOrNull { it.second } ?: 100.0
                    val finalMaxDailySpend = if (maxDailySpend == 0.0) 100.0 else maxDailySpend

                    // Custom Line/Bar Chart representing the daily spends
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .testTag("daily_spend_line_chart"),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val totalDays = selectedDailySpends.size
                            val chartWidth = size.width
                            val chartHeight = (size.height - 20f).coerceAtLeast(0f)

                            if (chartWidth > 0f && chartHeight > 0f) {
                                val pointSpacing = chartWidth / (totalDays - 1).coerceAtLeast(1)

                                // 1. Draw horizontal guidelines
                                val lines = 3
                                for (i in 0..lines) {
                                    val y = chartHeight * (i.toFloat() / lines)
                                    drawLine(
                                        color = Color.LightGray.copy(alpha = 0.2f),
                                        start = Offset(0f, y),
                                        end = Offset(chartWidth, y),
                                        strokeWidth = 1f
                                    )
                                }

                                // 2. Plot keys and map coordinates
                                val points = selectedDailySpends.mapIndexed { idx, pair ->
                                    val x = idx * pointSpacing
                                    val relativeSpend = (pair.second / finalMaxDailySpend).toFloat()
                                    val y = chartHeight - (chartHeight * relativeSpend)
                                    Offset(x, y)
                                }

                                // 3. Draw gradient area under the line
                                if (maxDailySpend > 0.0 && points.isNotEmpty()) {
                                    val fillPath = androidx.compose.ui.graphics.Path().apply {
                                        moveTo(0f, chartHeight)
                                        points.forEach { point ->
                                            lineTo(point.x, point.y)
                                        }
                                        lineTo(chartWidth, chartHeight)
                                        close()
                                    }
                                    drawPath(
                                        path = fillPath,
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                Color(0x7F2196F3),
                                                Color(0x002196F3)
                                            )
                                        )
                                    )

                                    // 4. Draw the actual line
                                    val strokePath = androidx.compose.ui.graphics.Path().apply {
                                        moveTo(points.first().x, points.first().y)
                                        for (i in 1 until points.size) {
                                            lineTo(points[i].x, points[i].y)
                                        }
                                    }
                                    drawPath(
                                        path = strokePath,
                                        color = Color(0xFF2196F3),
                                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                                    )

                                    // 5. Draw dots/indicators for days with spends
                                    points.forEachIndexed { idx, pt ->
                                        val dailyAmount = selectedDailySpends[idx].second
                                        if (dailyAmount > 0.0) {
                                            drawCircle(
                                                color = Color(0xFF00E5FF),
                                                radius = 4.dp.toPx(),
                                                center = pt
                                            )
                                            drawCircle(
                                                color = Color(0xFF2196F3),
                                                radius = 2.dp.toPx(),
                                                center = pt
                                            )
                                        }
                                    }
                                } else {
                                    // Draw horizontal baseline if empty
                                    drawLine(
                                        color = Color.LightGray.copy(alpha = 0.5f),
                                        start = Offset(0f, chartHeight),
                                        end = Offset(chartWidth, chartHeight),
                                        strokeWidth = 2f
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Sparkline day marks
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Day 1", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Day 10", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Day 20", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Day ${selectedDailySpends.size}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    // Highlight peak day of selected month
                    val peakDayPair = selectedDailySpends.maxByOrNull { it.second }
                    if (peakDayPair != null && peakDayPair.second > 0) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Highest single-day spend was on Day ${peakDayPair.first}: ${formatCurrency(peakDayPair.second)}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary
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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Outflow Breakdown",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.weight(1f)
                        )
                        
                        // Select Month Dropdown
                        var isMonthMenuExpanded by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButton(
                                onClick = { isMonthMenuExpanded = true },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(32.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (selectedMonthForBreakdown == "ALL") "All Months" else selectedMonthForBreakdown,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.KeyboardArrowDown, "Expand", modifier = Modifier.size(14.dp))
                            }
                            
                            DropdownMenu(
                                expanded = isMonthMenuExpanded,
                                onDismissRequest = { isMonthMenuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("All Months") },
                                    onClick = {
                                        selectedMonthForBreakdown = "ALL"
                                        isMonthMenuExpanded = false
                                    }
                                )
                                monthSummaries.forEach { summary ->
                                    val monthName = summary.first
                                    DropdownMenuItem(
                                        text = { Text(monthName) },
                                        onClick = {
                                            selectedMonthForBreakdown = monthName
                                            isMonthMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

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

        // Month-wise Summary cards Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Month-wise Summary",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Month Summary Details",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        monthSummaries.forEach { summary ->
                            val monthLabel = summary.first
                            val income = summary.second
                            val expense = summary.third
                            val saved = income - expense

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = monthLabel,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )

                                        // Savings Badge/Status Indicator
                                        val badgeColor = if (saved >= 0) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                                        val badgeTextColor = if (saved >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                                        Surface(
                                            color = badgeColor,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = if (saved >= 0) "Saved: ${formatCurrency(saved)}" else "Deficit: ${formatCurrency(-saved)}",
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = badgeTextColor
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        // Income details
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Income",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = if (income > 0) "+${formatCurrency(income)}" else "₹0.00",
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = Color(0xFF43A047)
                                            )
                                        }

                                        // Expense details
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Outflow",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = if (expense > 0) "-${formatCurrency(expense)}" else "₹0.00",
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.error
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
}
