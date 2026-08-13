package com.example.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.ui.theme.bounceClickable
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.delay

@Composable
fun TrendsTab(
    transactions: List<TransactionEntity>,
    modifier: Modifier = Modifier
) {
    // Period selection mode: "WEEK" or "MONTH"
    var currentPeriodMode by remember { mutableStateOf("WEEK") }
    // Current pagination offset in weeks
    var currentWeekOffset by remember { mutableStateOf(0) }
    // Current pagination offset in months
    var currentMonthOffset by remember { mutableStateOf(0) }
    // Selection mode: OVERVIEW (donut) or FLOW (bezier line chart)
    var currentViewMode by remember { mutableStateOf("FLOW") }
    // Daily view detail selection
    var selectedDayIndex by remember { mutableStateOf(4) }
    // Selected week index of the month (0 to 3)
    var selectedWeekIndex by remember { mutableStateOf(0) }
    // Selected day of the month for forecasting (1-indexed, e.g. 1 to D_total)
    var selectedForecastDay by remember { mutableStateOf(-1) }

    // Find the latest transaction date to anchor our week window, or default to now
    val latestTxDate = remember(transactions) {
        transactions.maxOfOrNull { it.dateMillis } ?: System.currentTimeMillis()
    }

    val baseCalendar = remember(latestTxDate) {
        Calendar.getInstance().apply {
            timeInMillis = latestTxDate
            // Set to Sunday of that week
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    // Determine Sundays & Saturdays of the navigated week unit
    val weekRange = remember(baseCalendar, currentWeekOffset) {
        val startCal = Calendar.getInstance().apply {
            timeInMillis = baseCalendar.timeInMillis
            add(Calendar.WEEK_OF_YEAR, currentWeekOffset)
        }
        val endCal = Calendar.getInstance().apply {
            timeInMillis = startCal.timeInMillis
            add(Calendar.DAY_OF_YEAR, 6)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        Pair(startCal, endCal)
    }

    val dateRangeStr = remember(weekRange) {
        val start = weekRange.first.time
        val end = weekRange.second.time
        val startSdf = SimpleDateFormat("MMM dd", Locale.getDefault())
        val endSdf = SimpleDateFormat("MMM dd", Locale.getDefault())
        "${startSdf.format(start)} - ${endSdf.format(end)}"
    }

    val baseMonthCalendar = remember(latestTxDate) {
        Calendar.getInstance().apply {
            timeInMillis = latestTxDate
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    val monthRange = remember(baseMonthCalendar, currentMonthOffset) {
        val startCal = Calendar.getInstance().apply {
            timeInMillis = baseMonthCalendar.timeInMillis
            add(Calendar.MONTH, currentMonthOffset)
        }
        val endCal = Calendar.getInstance().apply {
            timeInMillis = startCal.timeInMillis
            set(Calendar.DAY_OF_MONTH, startCal.getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        Pair(startCal, endCal)
    }

    val monthDateRangeStr = remember(monthRange) {
        val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        sdf.format(monthRange.first.time)
    }

    val activeDateRangeStr = if (currentPeriodMode == "WEEK") dateRangeStr else monthDateRangeStr

    // Capture week transaction scope
    val weeklyTransactions = remember(transactions, weekRange) {
        val startMs = weekRange.first.timeInMillis
        val endMs = weekRange.second.timeInMillis
        transactions.filter { it.dateMillis in startMs..endMs }
    }

    // Capture month transaction scope
    val monthlyTransactions = remember(transactions, monthRange) {
        val startMs = monthRange.first.timeInMillis
        val endMs = monthRange.second.timeInMillis
        transactions.filter { it.dateMillis in startMs..endMs }
    }

    // Compute Expense, Income, and Net Totals
    val weeklyExpense = remember(weeklyTransactions) {
        weeklyTransactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    }
    val weeklyIncome = remember(weeklyTransactions) {
        weeklyTransactions.filter { it.type == "INCOME" }.sumOf { it.amount }
    }
    val weeklyTotal = weeklyIncome - weeklyExpense

    val monthlyExpense = remember(monthlyTransactions) {
        monthlyTransactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    }
    val monthlyIncome = remember(monthlyTransactions) {
        monthlyTransactions.filter { it.type == "INCOME" }.sumOf { it.amount }
    }
    val monthlyTotal = monthlyIncome - monthlyExpense

    // --- SPENDING FORECAST COMPUTATION VARIABLES (HIGH PERFORMANCE & STATE-ACCESSIBLE) ---
    val totalDaysInMonth = remember(monthRange) { monthRange.first.getActualMaximum(Calendar.DAY_OF_MONTH) }
    
    val elapsedDays = remember(monthRange) {
        val todayCal = Calendar.getInstance()
        val startCal = monthRange.first
        val endCal = monthRange.second
        
        if (todayCal.before(startCal)) {
            0
        } else if (todayCal.after(endCal)) {
            startCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        } else {
            todayCal.get(Calendar.DAY_OF_MONTH)
        }
    }

    val monthlyExpensesList = remember(transactions, monthRange) {
        transactions.filter { 
            it.type == "EXPENSE" && it.dateMillis in monthRange.first.timeInMillis..monthRange.second.timeInMillis 
        }
    }
    
    val actualSpendsByDay = remember(monthlyExpensesList, totalDaysInMonth) {
        val result = DoubleArray(totalDaysInMonth + 1) { 0.0 }
        val cal = Calendar.getInstance()
        monthlyExpensesList.forEach { tx ->
            cal.timeInMillis = tx.dateMillis
            val day = cal.get(Calendar.DAY_OF_MONTH)
            if (day in 1..totalDaysInMonth) {
                result[day] = result[day] + tx.amount
            }
        }
        result
    }

    val actualCumulative = remember(actualSpendsByDay, totalDaysInMonth) {
        val result = DoubleArray(totalDaysInMonth + 1) { 0.0 }
        var runningSum = 0.0
        for (d in 1..totalDaysInMonth) {
            runningSum += actualSpendsByDay[d]
            result[d] = runningSum
        }
        result
    }

    val overallDailyAverage = remember(transactions) {
        val expenses = transactions.filter { it.type == "EXPENSE" }
        if (expenses.isEmpty()) {
            300.0
        } else {
            val minTime = expenses.minOf { it.dateMillis }
            val maxTime = expenses.maxOf { it.dateMillis }
            val timeDiff = maxTime - minTime
            val daysSpan = maxOf(1, (timeDiff / (1000 * 60 * 60 * 24)).toInt())
            val totalExp = expenses.sumOf { it.amount }
            val avg = totalExp / daysSpan
            if (avg > 0.0) avg else 300.0
        }
    }

    val projectionDailyRate = remember(elapsedDays, actualCumulative, overallDailyAverage) {
        if (elapsedDays > 0) {
            val rateSoFar = actualCumulative[elapsedDays] / elapsedDays
            if (rateSoFar < 10.0) overallDailyAverage else rateSoFar
        } else {
            overallDailyAverage
        }
    }

    val predictedCumulative = remember(elapsedDays, actualCumulative, projectionDailyRate, totalDaysInMonth) {
        val result = DoubleArray(totalDaysInMonth + 1) { 0.0 }
        for (d in 1..totalDaysInMonth) {
            if (d <= elapsedDays) {
                result[d] = actualCumulative[d]
            } else {
                val base = if (d - 1 <= elapsedDays) actualCumulative[elapsedDays] else result[d - 1]
                result[d] = base + projectionDailyRate
            }
        }
        result
    }

    val maxCumulativeAmount = remember(predictedCumulative) { predictedCumulative.maxOrNull() ?: 1000.0 }
    val finalMaxCumulative = if (maxCumulativeAmount <= 0.0) 1000.0 else maxCumulativeAmount

    val activeForecastDay = remember(selectedForecastDay, elapsedDays, totalDaysInMonth) {
        if (selectedForecastDay == -1 || selectedForecastDay > totalDaysInMonth) {
            if (elapsedDays > 0) elapsedDays else 1
        } else {
            selectedForecastDay
        }
    }

    // Previous month comparator properties
    val prevMonthRange = remember(monthRange) {
         val prevStart = Calendar.getInstance().apply {
             timeInMillis = monthRange.first.timeInMillis
             add(Calendar.MONTH, -1)
         }
         val prevEnd = Calendar.getInstance().apply {
             timeInMillis = prevStart.timeInMillis
             set(Calendar.DAY_OF_MONTH, prevStart.getActualMaximum(Calendar.DAY_OF_MONTH))
             set(Calendar.HOUR_OF_DAY, 23)
             set(Calendar.MINUTE, 59)
             set(Calendar.SECOND, 59)
             set(Calendar.MILLISECOND, 999)
         }
         Pair(prevStart, prevEnd)
    }

    val prevMonthExpenses = remember(transactions, prevMonthRange) {
        transactions.filter {
            it.type == "EXPENSE" && it.dateMillis in prevMonthRange.first.timeInMillis..prevMonthRange.second.timeInMillis
        }.sumOf { it.amount }
    }

    val diffPercent = if (prevMonthExpenses > 0) {
        ((predictedCumulative[totalDaysInMonth] - prevMonthExpenses) / prevMonthExpenses) * 100
    } else {
        0.0
    }

    val activeTransactions = remember(currentPeriodMode, weeklyTransactions, monthlyTransactions) {
        if (currentPeriodMode == "WEEK") weeklyTransactions else monthlyTransactions
    }
    val activeExpense = remember(currentPeriodMode, weeklyExpense, monthlyExpense) {
        if (currentPeriodMode == "WEEK") weeklyExpense else monthlyExpense
    }
    val activeIncome = remember(currentPeriodMode, weeklyIncome, monthlyIncome) {
        if (currentPeriodMode == "WEEK") weeklyIncome else monthlyIncome
    }
    val activeTotal = remember(currentPeriodMode, weeklyTotal, monthlyTotal) {
        if (currentPeriodMode == "WEEK") weeklyTotal else monthlyTotal
    }

    // Chart entry transitions logic
    var chartTarget by remember { mutableStateOf(0f) }
    LaunchedEffect(currentWeekOffset, currentMonthOffset, currentViewMode, currentPeriodMode) {
        chartTarget = 0f
        delay(20)
        chartTarget = 1f
    }
    val chartProgress by animateFloatAsState(
        targetValue = chartTarget,
        animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing),
        label = "chartProgress"
    )

    // Compute daily values across days (Sun=0 to Sat=6)
    val dailySpendsOfWeek = remember(weekRange, weeklyTransactions) {
        val result = MutableList(7) { 0.0 }
        
        val tempCal = Calendar.getInstance()
        weeklyTransactions.filter { it.type == "EXPENSE" }.forEach { tx ->
            tempCal.timeInMillis = tx.dateMillis
            val dayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK) // 1 (Sunday) to 7 (Saturday)
            val index = dayOfWeek - 1
            if (index in 0..6) {
                result[index] = result[index] + tx.amount
            }
        }
        result
    }

    // Day dates inside calendar table
    val dayDatesOfWeek = remember(weekRange) {
        val result = MutableList(7) { 0 }
        val tempCal = Calendar.getInstance().apply {
            timeInMillis = weekRange.first.timeInMillis
        }
        for (i in 0..6) {
            result[i] = tempCal.get(Calendar.DAY_OF_MONTH)
            tempCal.add(Calendar.DAY_OF_YEAR, 1)
        }
        result
    }

    val monthlyPeriods = remember(monthRange) {
        val startCal = Calendar.getInstance().apply { timeInMillis = monthRange.first.timeInMillis }
        val monthMax = startCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val monthSdf = SimpleDateFormat("MMM", Locale.getDefault())
        val monthName = monthSdf.format(startCal.time)
        
        List(4) { idx ->
            val (fromDay, toDay) = when (idx) {
                0 -> Pair(1, 7)
                1 -> Pair(8, 14)
                2 -> Pair(15, 21)
                else -> Pair(22, monthMax)
            }
            Triple(idx, "$monthName %02d-%02d".format(Locale.US, fromDay, toDay), Pair(fromDay, toDay))
        }
    }

    val monthlyPeriodsSpendsAndCounts = remember(monthlyPeriods, monthlyTransactions) {
        monthlyPeriods.map { (_, _, range) ->
            val (fromDay, toDay) = range
            val tempCal = Calendar.getInstance()
            val periodTx = monthlyTransactions.filter { tx ->
                tempCal.timeInMillis = tx.dateMillis
                val day = tempCal.get(Calendar.DAY_OF_MONTH)
                day in fromDay..toDay
            }
            val periodExpense = periodTx.filter { it.type == "EXPENSE" }.sumOf { it.amount }
            val periodTxCount = periodTx.size
            Pair(periodExpense, periodTxCount)
        }
    }

    val monthlyPeriodsSpends = remember(monthlyPeriodsSpendsAndCounts) {
        monthlyPeriodsSpendsAndCounts.map { it.first }
    }

    // Auto focus on day of maximal spend when navigation occurs
    LaunchedEffect(dailySpendsOfWeek) {
        val maxIndex = dailySpendsOfWeek.indexOf(dailySpendsOfWeek.maxOrNull() ?: 0.0)
        selectedDayIndex = if (maxIndex >= 0 && dailySpendsOfWeek[maxIndex] > 0.0) maxIndex else 4
    }

    LaunchedEffect(monthlyPeriodsSpends) {
        val maxIndex = monthlyPeriodsSpends.indexOf(monthlyPeriodsSpends.maxOrNull() ?: 0.0)
        selectedWeekIndex = if (maxIndex >= 0 && monthlyPeriodsSpends[maxIndex] > 0.0) maxIndex else 0
    }

    // Compute expenditures category grouping based on the active period
    val expensesFiltered = remember(activeTransactions) {
        activeTransactions.filter { it.type == "EXPENSE" }
    }
    val categoryTotals = remember(expensesFiltered) {
        expensesFiltered
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }
    }
    val totalSpent = remember(categoryTotals) {
        categoryTotals.sumOf { it.second }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("trends_tab_root"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- PERIOD SELECTION TAB/SELECTOR (WEEKLY vs MONTHLY) ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val periodsList = listOf("WEEK" to "Weekly Trends", "MONTH" to "Monthly Trends")
                    periodsList.forEach { (mode, label) ->
                        val isSelected = (currentPeriodMode == mode)
                        val backBg = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                        val textCol = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(backBg)
                                .clickable { currentPeriodMode = mode }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.3.sp
                                ),
                                color = textCol,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // --- PAGINATION NAVIGATION HEADER ---
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { 
                        if (currentPeriodMode == "WEEK") currentWeekOffset-- else currentMonthOffset--
                    },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = "Previous",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Text(
                    text = activeDateRangeStr,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = { 
                        if (currentPeriodMode == "WEEK") currentWeekOffset++ else currentMonthOffset++
                    },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Next",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                IconButton(
                    onClick = { /* Optional Filter trigger */ },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Filter",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // --- EXPENDITURE / INCOME / NET BALANCES SUMMARY STATS BAR ---
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "EXPENSE",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
                        )
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = formatCurrency(activeExpense),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        ),
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(28.dp)
                        .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f))
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "INCOME",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
                        )
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = formatCurrency(activeIncome),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        ),
                        color = Color(0xFF2E7D32)
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(28.dp)
                        .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f))
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "TOTAL",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
                        )
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = (if (activeTotal >= 0) "" else "-") + formatCurrency(Math.abs(activeTotal)),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        ),
                        color = if (activeTotal >= 0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // --- DROPDOWN SECTOR SELECTOR (EXPENSE OVERVIEW / EXPENSE FLOW) ---
        item {
            var isViewModeMenuExpanded by remember { mutableStateOf(false) }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .border(
                            width = 1.5.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { isViewModeMenuExpanded = true }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Dropdown indicator",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (currentViewMode) {
                            "OVERVIEW" -> "EXPENSE OVERVIEW"
                            "FLOW" -> "EXPENSE FLOW"
                            "RECHARTS" -> "RECHARTS TRENDS"
                            else -> "SPENDING FORECAST"
                        },
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.5.sp
                        )
                    )
                }

                DropdownMenu(
                    expanded = isViewModeMenuExpanded,
                    onDismissRequest = { isViewModeMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("EXPENSE OVERVIEW") },
                        onClick = {
                            currentViewMode = "OVERVIEW"
                            isViewModeMenuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("RECHARTS TRENDS") },
                        onClick = {
                            currentViewMode = "RECHARTS"
                            isViewModeMenuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("EXPENSE FLOW") },
                        onClick = {
                            currentViewMode = "FLOW"
                            isViewModeMenuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("SPENDING FORECAST") },
                        onClick = {
                            currentViewMode = "FORECAST"
                            isViewModeMenuExpanded = false
                        }
                    )
                }
            }
        }

        // --- RENDERING VIEWS CORRESPONDING TO VIEW MODE SELECTION ---
        if (currentViewMode == "RECHARTS") {
            item {
                RechartsSpendingTrendCard(
                    transactions = transactions,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        if (currentViewMode == "OVERVIEW") {
            // VIEW 1: EXPENSE OVERVIEW (Donut Chart & Percent Progress list)
            if (totalSpent == 0.0) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "No records for this week",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Add transaction records under Logs to display donut insights.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .testTag("spend_donut_chart_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Donut Graphics layout
                            Box(
                                modifier = Modifier
                                    .size(160.dp)
                                    .testTag("spend_donut_chart"),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.size(130.dp)) {
                                    var accumulatedAngle = -90f

                                    categoryTotals.forEach { catTotal ->
                                        val catName = catTotal.first
                                        val catAmt = catTotal.second
                                        val catStyle = FinanceCategory.getStyleFor(catName)

                                        val sweepAngle = 360f * (catAmt / totalSpent).toFloat() * chartProgress

                                        drawArc(
                                            color = catStyle.color,
                                            startAngle = accumulatedAngle,
                                            sweepAngle = sweepAngle,
                                            useCenter = false,
                                            style = Stroke(width = 18.dp.toPx(), cap = StrokeCap.Round),
                                            size = Size(size.width, size.height)
                                        )

                                        accumulatedAngle += sweepAngle
                                    }
                                }

                                // Central content stack
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "Expenses",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    val topCategory = categoryTotals.firstOrNull()
                                    if (topCategory != null) {
                                        val topPct = (topCategory.second / totalSpent) * 100
                                        Text(
                                            text = "${topPct.toInt()}%",
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = topCategory.first,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = FinanceCategory.getStyleFor(topCategory.first).color,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.widthIn(max = 84.dp)
                                        )
                                    } else {
                                        Text(
                                            text = formatCurrency(totalSpent),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }

                            // Donut Legend indicators on right
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                categoryTotals.take(4).forEach { entry ->
                                    val pct = (entry.second / totalSpent) * 100
                                    val style = FinanceCategory.getStyleFor(entry.first)

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .background(style.color, RoundedCornerShape(2.dp))
                                        )
                                        Text(
                                            text = "${entry.first} ${pct.toInt()}%",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                                if (categoryTotals.size > 4) {
                                    Text(
                                        text = "+ ${categoryTotals.size - 4} more",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(start = 16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // List header label
                item {
                    Text(
                        text = "Outflow Breakdown",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                // Breakdown elements loop
                items(categoryTotals) { entry ->
                    val pct = (entry.second / totalSpent) * 100
                    val style = FinanceCategory.getStyleFor(entry.first)

                    Surface(
                        modifier = Modifier.fillMaxWidth().testTag("overview_category_row_${entry.first}"),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(style.color.copy(alpha = 0.12f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = style.icon,
                                    contentDescription = entry.first,
                                    tint = style.color,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = entry.first,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "-${formatCurrency(entry.second)}",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Visual progress loading channel
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(8.dp)
                                            .background(
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f),
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth((pct / 100f).toFloat() * chartProgress)
                                                .fillMaxHeight()
                                                .background(
                                                    color = style.color,
                                                    shape = RoundedCornerShape(4.dp)
                                                )
                                        )
                                    }

                                    Text(
                                        text = String.format(Locale.US, "%.1f%%", pct),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.width(42.dp),
                                        textAlign = TextAlign.End
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else if (currentViewMode == "FLOW") {
            // VIEW 2: EXPENSE FLOW (Bezier Line Chart & Day/Week Table grid)
            item {
                if (currentPeriodMode == "WEEK") {
                    // WEEK MODE: Daily Expense Trend (7 points)
                    val maxDailySpendOfWeek = dailySpendsOfWeek.maxOrNull() ?: 0.0
                    val finalMaxDaily = if (maxDailySpendOfWeek == 0.0) 100.0 else maxDailySpendOfWeek

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .testTag("daily_spend_line_chart_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Daily Expense Trend",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(190.dp)
                                    .testTag("daily_spend_line_chart"),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val labelAreaWidth = 58.dp.toPx()
                                    val chartWidth = size.width - labelAreaWidth
                                    val chartHeight = size.height - 24.dp.toPx()

                                    if (chartWidth > 0f && chartHeight > 0f) {
                                        val pointSpacing = chartWidth / 6f

                                        // Guidelines
                                        val gridCount = 3
                                        for (gridIdx in 0..gridCount) {
                                            val percent = gridIdx.toFloat() / gridCount
                                            val yVal = chartHeight * percent

                                            drawLine(
                                                color = Color.LightGray.copy(alpha = 0.25f),
                                                start = Offset(labelAreaWidth, yVal),
                                                end = Offset(size.width, yVal),
                                                strokeWidth = 1.dp.toPx()
                                            )
                                        }

                                        // Define path and coordinates
                                        val points = dailySpendsOfWeek.mapIndexed { idx, value ->
                                            val x = labelAreaWidth + idx * pointSpacing
                                            val relativeSpend = (value / finalMaxDaily).toFloat()
                                            val y = chartHeight - (chartHeight * relativeSpend * chartProgress)
                                            Offset(x, y)
                                        }

                                        // Fill and Stroke curves
                                        if (maxDailySpendOfWeek > 0.0 && points.size == 7) {
                                            val fillPath = androidx.compose.ui.graphics.Path().apply {
                                                moveTo(points.first().x, chartHeight)
                                                lineTo(points.first().x, points.first().y)
                                                for (i in 0 until points.size - 1) {
                                                    val p1 = points[i]
                                                    val p2 = points[i+1]
                                                    val controlX1 = p1.x + pointSpacing / 2f
                                                    val controlY1 = p1.y
                                                    val controlX2 = p2.x - pointSpacing / 2f
                                                    val controlY2 = p2.y
                                                    cubicTo(controlX1, controlY1, controlX2, controlY2, p2.x, p2.y)
                                                }
                                                lineTo(points.last().x, chartHeight)
                                                close()
                                            }

                                            drawPath(
                                                path = fillPath,
                                                brush = Brush.verticalGradient(
                                                    colors = listOf(
                                                        Color(0xFFE57373).copy(alpha = 0.35f),
                                                        Color(0xFFE57373).copy(alpha = 0.005f)
                                                    )
                                                )
                                            )

                                            val strokePath = androidx.compose.ui.graphics.Path().apply {
                                                moveTo(points.first().x, points.first().y)
                                                for (i in 0 until points.size - 1) {
                                                    val p1 = points[i]
                                                    val p2 = points[i+1]
                                                    val controlX1 = p1.x + pointSpacing / 2f
                                                    val controlY1 = p1.y
                                                    val controlX2 = p2.x - pointSpacing / 2f
                                                    val controlY2 = p2.y
                                                    cubicTo(controlX1, controlY1, controlX2, controlY2, p2.x, p2.y)
                                                }
                                            }

                                            drawPath(
                                                path = strokePath,
                                                color = Color(0xFFE57373),
                                                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                                            )

                                            // Hover guide cursor line
                                            val activePt = points.getOrNull(selectedDayIndex)
                                            if (activePt != null) {
                                                drawLine(
                                                    color = Color.Gray.copy(alpha = 0.3f),
                                                    start = Offset(activePt.x, 0f),
                                                    end = Offset(activePt.x, chartHeight),
                                                    strokeWidth = 1.dp.toPx()
                                                )
                                            }

                                            // Point Nodes
                                            points.forEachIndexed { idx, pt ->
                                                val isHovered = (idx == selectedDayIndex)
                                                drawCircle(
                                                    color = Color(0xFFE57373),
                                                    radius = (if (isHovered) 5.5.dp else 4.5.dp).toPx(),
                                                    center = pt
                                                )
                                                drawCircle(
                                                    color = Color.White,
                                                    radius = (if (isHovered) 3.5.dp else 2.5.dp).toPx(),
                                                    center = pt
                                                )
                                            }
                                        } else {
                                            drawLine(
                                                color = Color.LightGray.copy(alpha = 0.5f),
                                                start = Offset(labelAreaWidth, chartHeight),
                                                end = Offset(size.width, chartHeight),
                                                strokeWidth = 2.dp.toPx()
                                            )
                                        }
                                    }
                                }

                                // Composable grid values on left y-axis
                                Row(modifier = Modifier.fillMaxSize()) {
                                    Column(
                                        modifier = Modifier
                                            .width(58.dp)
                                            .fillMaxHeight()
                                            .padding(bottom = 24.dp),
                                        verticalArrangement = Arrangement.SpaceBetween,
                                        horizontalAlignment = Alignment.End
                                    ) {
                                        val stepAmount = finalMaxDaily / 3f
                                        for (gridIdx in 3 downTo 0) {
                                            val spendAmt = stepAmount * gridIdx
                                            val labelText = if (spendAmt == 0.0) "-₹0.00" else "-₹${String.format(Locale.US, "%.0f", spendAmt)}"
                                            Text(
                                                text = labelText,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                modifier = Modifier.padding(end = 6.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.weight(1f))
                                }

                                // Date range markers on bottom x-axis
                                val sdfAxis = SimpleDateFormat("MMM dd", Locale.getDefault())
                                val dateSun = weekRange.first.time
                                val dateWed = Calendar.getInstance().apply {
                                    timeInMillis = weekRange.first.timeInMillis
                                    add(Calendar.DAY_OF_YEAR, 3)
                                }.time
                                val dateSat = weekRange.second.time

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(24.dp)
                                        .padding(start = 58.dp, top = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = sdfAxis.format(dateSun),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = sdfAxis.format(dateWed),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = sdfAxis.format(dateSat),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Centered hover tooltip
                                val activeDayAmount = dailySpendsOfWeek[selectedDayIndex]
                                val activeDayLabel = remember(selectedDayIndex, weekRange) {
                                    val temp = Calendar.getInstance().apply {
                                        timeInMillis = weekRange.first.timeInMillis
                                        add(Calendar.DAY_OF_YEAR, selectedDayIndex)
                                    }
                                    val sdfTool = SimpleDateFormat("MMM dd", Locale.getDefault())
                                    sdfTool.format(temp.time)
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(bottom = 24.dp),
                                    contentAlignment = Alignment.TopStart
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 50.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Spacer(modifier = Modifier.weight((selectedDayIndex + 0.5f) / 8f, fill = true))
                                        Card(
                                            shape = RoundedCornerShape(8.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = Color(0xFFFDFBF4)
                                            ),
                                            border = CardDefaults.outlinedCardBorder().copy(
                                                brush = Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary))
                                            ),
                                            modifier = Modifier.padding(bottom = 32.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(
                                                    text = activeDayLabel,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                                )
                                                Text(
                                                    text = formatCurrency(activeDayAmount),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.weight((7.5f - selectedDayIndex) / 8f, fill = true))
                                    }
                                }

                                // Touch tracking transparent columns layer overlay
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(start = 58.dp, bottom = 24.dp)
                                ) {
                                    for (i in 0..6) {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .clickable(
                                                    interactionSource = remember { MutableInteractionSource() },
                                                    indication = null
                                                ) {
                                                    selectedDayIndex = i
                                                }
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // MONTH MODE: Weekly Interval Expense Trend (4 points)
                    val maxWeeklySpendOfMonth = monthlyPeriodsSpends.maxOrNull() ?: 0.0
                    val finalMaxWeekly = if (maxWeeklySpendOfMonth == 0.0) 100.0 else maxWeeklySpendOfMonth

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .testTag("weekly_spend_line_chart_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Weekly Outflow Distribution",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(190.dp)
                                    .testTag("weekly_spend_line_chart"),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val labelAreaWidth = 58.dp.toPx()
                                    val chartWidth = size.width - labelAreaWidth
                                    val chartHeight = size.height - 24.dp.toPx()

                                    if (chartWidth > 0f && chartHeight > 0f) {
                                        val pointSpacing = chartWidth / 3f

                                        // Guidelines
                                        val gridCount = 3
                                        for (gridIdx in 0..gridCount) {
                                            val percent = gridIdx.toFloat() / gridCount
                                            val yVal = chartHeight * percent

                                            drawLine(
                                                color = Color.LightGray.copy(alpha = 0.25f),
                                                start = Offset(labelAreaWidth, yVal),
                                                end = Offset(size.width, yVal),
                                                strokeWidth = 1.dp.toPx()
                                            )
                                        }

                                        // Coordinates
                                        val points = monthlyPeriodsSpends.mapIndexed { idx, value ->
                                            val x = labelAreaWidth + idx * pointSpacing
                                            val relativeSpend = (value / finalMaxWeekly).toFloat()
                                            val y = chartHeight - (chartHeight * relativeSpend * chartProgress)
                                            Offset(x, y)
                                        }

                                        // Bezier curves
                                        if (maxWeeklySpendOfMonth > 0.0 && points.size == 4) {
                                            val fillPath = androidx.compose.ui.graphics.Path().apply {
                                                moveTo(points.first().x, chartHeight)
                                                lineTo(points.first().x, points.first().y)
                                                for (i in 0 until points.size - 1) {
                                                    val p1 = points[i]
                                                    val p2 = points[i+1]
                                                    val controlX1 = p1.x + pointSpacing / 2f
                                                    val controlY1 = p1.y
                                                    val controlX2 = p2.x - pointSpacing / 2f
                                                    val controlY2 = p2.y
                                                    cubicTo(controlX1, controlY1, controlX2, controlY2, p2.x, p2.y)
                                                }
                                                lineTo(points.last().x, chartHeight)
                                                close()
                                            }

                                            drawPath(
                                                path = fillPath,
                                                brush = Brush.verticalGradient(
                                                    colors = listOf(
                                                        Color(0xFFE57373).copy(alpha = 0.35f),
                                                        Color(0xFFE57373).copy(alpha = 0.005f)
                                                    )
                                                )
                                            )

                                            val strokePath = androidx.compose.ui.graphics.Path().apply {
                                                moveTo(points.first().x, points.first().y)
                                                for (i in 0 until points.size - 1) {
                                                    val p1 = points[i]
                                                    val p2 = points[i+1]
                                                    val controlX1 = p1.x + pointSpacing / 2f
                                                    val controlY1 = p1.y
                                                    val controlX2 = p2.x - pointSpacing / 2f
                                                    val controlY2 = p2.y
                                                    cubicTo(controlX1, controlY1, controlX2, controlY2, p2.x, p2.y)
                                                }
                                            }

                                            drawPath(
                                                path = strokePath,
                                                color = Color(0xFFE57373),
                                                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                                            )

                                            // Hover guide cursor line
                                            val activePt = points.getOrNull(selectedWeekIndex)
                                            if (activePt != null) {
                                                drawLine(
                                                    color = Color.Gray.copy(alpha = 0.3f),
                                                    start = Offset(activePt.x, 0f),
                                                    end = Offset(activePt.x, chartHeight),
                                                    strokeWidth = 1.dp.toPx()
                                                )
                                            }

                                            // Point Nodes
                                            points.forEachIndexed { idx, pt ->
                                                val isHovered = (idx == selectedWeekIndex)
                                                drawCircle(
                                                    color = Color(0xFFE57373),
                                                    radius = (if (isHovered) 5.5.dp else 4.5.dp).toPx(),
                                                    center = pt
                                                )
                                                drawCircle(
                                                    color = Color.White,
                                                    radius = (if (isHovered) 3.5.dp else 2.5.dp).toPx(),
                                                    center = pt
                                                )
                                            }
                                        } else {
                                            drawLine(
                                                color = Color.LightGray.copy(alpha = 0.5f),
                                                start = Offset(labelAreaWidth, chartHeight),
                                                end = Offset(size.width, chartHeight),
                                                strokeWidth = 2.dp.toPx()
                                            )
                                        }
                                    }
                                }

                                // Y-Axis
                                Row(modifier = Modifier.fillMaxSize()) {
                                    Column(
                                        modifier = Modifier
                                            .width(58.dp)
                                            .fillMaxHeight()
                                            .padding(bottom = 24.dp),
                                        verticalArrangement = Arrangement.SpaceBetween,
                                        horizontalAlignment = Alignment.End
                                    ) {
                                        val stepAmount = finalMaxWeekly / 3f
                                        for (gridIdx in 3 downTo 0) {
                                            val spendAmt = stepAmount * gridIdx
                                            val labelText = if (spendAmt == 0.0) "-₹0.00" else "-₹${String.format(Locale.US, "%.0f", spendAmt)}"
                                            Text(
                                                text = labelText,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                modifier = Modifier.padding(end = 6.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.weight(1f))
                                }

                                // X-Axis Week Markers
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(24.dp)
                                        .padding(start = 58.dp, top = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceAround,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "Week 1", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(text = "Week 2", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(text = "Week 3", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(text = "Week 4", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                // Tooltip Info Card
                                val activeWeekAmount = monthlyPeriodsSpends[selectedWeekIndex]
                                val activeWeekLabel = monthlyPeriods[selectedWeekIndex].second

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(bottom = 24.dp),
                                    contentAlignment = Alignment.TopStart
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 50.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Spacer(modifier = Modifier.weight((selectedWeekIndex + 0.5f) / 5f, fill = true))
                                        Card(
                                            shape = RoundedCornerShape(8.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFDFBF4)),
                                            border = CardDefaults.outlinedCardBorder().copy(
                                                brush = Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary))
                                            ),
                                            modifier = Modifier.padding(bottom = 32.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(
                                                    text = activeWeekLabel,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                                )
                                                Text(
                                                    text = formatCurrency(activeWeekAmount),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.weight((4.5f - selectedWeekIndex) / 5f, fill = true))
                                    }
                                }

                                // Click Zones Overlay (4 Zones)
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(start = 58.dp, bottom = 24.dp)
                                ) {
                                    for (i in 0..3) {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .clickable(
                                                    interactionSource = remember { MutableInteractionSource() },
                                                    indication = null
                                                ) {
                                                    selectedWeekIndex = i
                                                }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Timeline header text
            item {
                Text(
                    text = if (currentPeriodMode == "WEEK") "Weekly Activity Table" else "Monthly Activity Table",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                )
            }

            // Beautiful day-by-day table card matching Image 4
            item {
                if (currentPeriodMode == "WEEK") {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("day_grid_table_card"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                        ) {
                            // Days Names Header Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                val daysList = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                                daysList.forEachIndexed { idx, name ->
                                    val isSelected = (idx == selectedDayIndex)
                                    Text(
                                        text = name,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f)
                            )

                            // Dates Number Circles Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                dayDatesOfWeek.forEachIndexed { idx, dateNum ->
                                    val isSelected = (idx == selectedDayIndex)
                                    Box(
                                        modifier = Modifier.weight(1f),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .background(
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent,
                                                    shape = CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = dateNum.toString(),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Outflow numerical red amounts values
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                dailySpendsOfWeek.forEachIndexed { idx, value ->
                                    val isSelected = (idx == selectedDayIndex)
                                    val amountLabel = if (value == 0.0) "-0.0" else String.format(Locale.US, "-%.1f", value)
                                    Text(
                                        text = amountLabel,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                        color = if (value > 0.0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.error.copy(alpha = 0.45f),
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("month_grid_table_card"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp, horizontal = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Column Headers
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "INTERVAL / DATE RANGE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.weight(1.8f)
                                )
                                Text(
                                    text = "LOGS",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(0.8f)
                                )
                                Text(
                                    text = "OUTFLOW",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.weight(1.2f)
                                )
                            }

                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f)
                            )

                            // 4 Weekly Interval Rows
                            monthlyPeriods.forEachIndexed { idx, rangeTriple ->
                                val (weekIndex, labelStr, _) = rangeTriple
                                val (weekSpend, weekCount) = monthlyPeriodsSpendsAndCounts[weekIndex]
                                val isSelected = (weekIndex == selectedWeekIndex)

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) 
                                            else Color.Transparent
                                        )
                                        .clickable { selectedWeekIndex = weekIndex }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1.8f)) {
                                        Text(
                                            text = "Week ${idx + 1}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = labelStr,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                        )
                                    }

                                    // Counter badge
                                    Box(
                                        modifier = Modifier.weight(0.8f),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    color = if (weekCount > 0) MaterialTheme.colorScheme.secondaryContainer 
                                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                    shape = RoundedCornerShape(10.dp)
                                                )
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "$weekCount logs",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (weekCount > 0) MaterialTheme.colorScheme.onSecondaryContainer 
                                                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                            )
                                        }
                                    }

                                    // Expense outflows
                                    Text(
                                        text = if (weekSpend > 0.0) "-${formatCurrency(weekSpend)}" else "₹0.00",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (weekSpend > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.weight(1.2f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // VIEW 3: SPENDING FORECAST (Predicted Spending Forecast for the Rest of the Month using a Line Chart)
            // Forecast Overview Metric Blocks Item
            item {
                Card(
                     modifier = Modifier.fillMaxWidth().testTag("forecast_summary_card"),
                     shape = RoundedCornerShape(16.dp),
                     colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))
                ) {
                     Column(modifier = Modifier.padding(16.dp)) {
                         Text(
                             text = "Month End Prediction Summary",
                             style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                             color = MaterialTheme.colorScheme.primary
                         )
                         Spacer(modifier = Modifier.height(12.dp))

                         Row(
                             modifier = Modifier.fillMaxWidth(),
                             horizontalArrangement = Arrangement.SpaceBetween
                         ) {
                             Column(modifier = Modifier.weight(1f)) {
                                 Text(
                                     text = "Spent to Date",
                                     style = MaterialTheme.typography.labelSmall,
                                     color = MaterialTheme.colorScheme.onSurfaceVariant
                                 )
                                 Text(
                                     text = formatCurrency(actualCumulative[elapsedDays.coerceIn(0, totalDaysInMonth)]),
                                     style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                     color = MaterialTheme.colorScheme.onSurface
                                 )
                             }
                             Column(modifier = Modifier.weight(1f)) {
                                 Text(
                                     text = "Forecasted Remaining",
                                     style = MaterialTheme.typography.labelSmall,
                                     color = MaterialTheme.colorScheme.onSurfaceVariant
                                 )
                                 val rem = maxOf(0.0, predictedCumulative[totalDaysInMonth] - actualCumulative[elapsedDays.coerceIn(0, totalDaysInMonth)])
                                 Text(
                                     text = formatCurrency(rem),
                                     style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                     color = MaterialTheme.colorScheme.secondary
                                 )
                             }
                         }

                         Spacer(modifier = Modifier.height(12.dp))
                         HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                         Spacer(modifier = Modifier.height(12.dp))

                         Row(
                             modifier = Modifier.fillMaxWidth(),
                             horizontalArrangement = Arrangement.SpaceBetween,
                             verticalAlignment = Alignment.CenterVertically
                         ) {
                             Column {
                                 Text(
                                     text = "Projected End-of-Month Total",
                                     style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                     color = MaterialTheme.colorScheme.onSurfaceVariant
                                 )
                                 Text(
                                     text = formatCurrency(predictedCumulative[totalDaysInMonth]),
                                     style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                                     color = MaterialTheme.colorScheme.primary
                                 )
                             }

                             // Previous month comparator indicator
                             // Fetch previous month actual expenses
                             val prevMonthRange = remember(monthRange) {
                                  val prevStart = Calendar.getInstance().apply {
                                      timeInMillis = monthRange.first.timeInMillis
                                      add(Calendar.MONTH, -1)
                                  }
                                  val prevEnd = Calendar.getInstance().apply {
                                      timeInMillis = prevStart.timeInMillis
                                      set(Calendar.DAY_OF_MONTH, prevStart.getActualMaximum(Calendar.DAY_OF_MONTH))
                                      set(Calendar.HOUR_OF_DAY, 23)
                                      set(Calendar.MINUTE, 59)
                                      set(Calendar.SECOND, 59)
                                      set(Calendar.MILLISECOND, 999)
                                  }
                                  Pair(prevStart, prevEnd)
                             }

                             val prevMonthExpenses = remember(transactions, prevMonthRange) {
                                 transactions.filter {
                                     it.type == "EXPENSE" && it.dateMillis in prevMonthRange.first.timeInMillis..prevMonthRange.second.timeInMillis
                                 }.sumOf { it.amount }
                             }

                             val diffPercent = if (prevMonthExpenses > 0) {
                                 ((predictedCumulative[totalDaysInMonth] - prevMonthExpenses) / prevMonthExpenses) * 100
                             } else {
                                 0.0
                             }

                             if (prevMonthExpenses > 0.0) {
                                 val color = if (diffPercent > 0) MaterialTheme.colorScheme.error else Color(0xFF2E7D32)
                                 val icon = if (diffPercent > 0) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward
                                 val label = if (diffPercent > 0) "Higher" else "Lower"
                                 Column(horizontalAlignment = Alignment.End) {
                                     Text(
                                         text = "vs Last Month (${formatCurrency(prevMonthExpenses)})",
                                         style = MaterialTheme.typography.labelSmall,
                                         color = MaterialTheme.colorScheme.onSurfaceVariant
                                     )
                                     Spacer(modifier = Modifier.height(2.dp))
                                     Row(verticalAlignment = Alignment.CenterVertically) {
                                         Icon(
                                             imageVector = icon,
                                             contentDescription = label,
                                             tint = color,
                                             modifier = Modifier.size(14.dp)
                                         )
                                         Spacer(modifier = Modifier.width(4.dp))
                                         Text(
                                             text = String.format(Locale.US, "%.1f%% %s", Math.abs(diffPercent), label),
                                             style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                             color = color
                                          )
                                     }
                                 }
                             }
                         }
                     }
                }
            }

            // --- THE FORECASTING LINE CHART CARD ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("forecast_line_chart_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Cumulative Spend Path",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Box(modifier = Modifier.size(8.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                                    Text("Actual", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Box(modifier = Modifier.size(8.dp).border(1.dp, MaterialTheme.colorScheme.secondary, CircleShape))
                                    Text("Predicted", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(210.dp),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            // Y Axis Labels Column on the left
                            Column(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .align(Alignment.TopStart)
                                    .width(50.dp)
                                    .padding(bottom = 24.dp), // align with chart bottom spacing
                                verticalArrangement = Arrangement.SpaceBetween,
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(text = formatShortInr(finalMaxCumulative), fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                                Text(text = formatShortInr(finalMaxCumulative * 0.75), fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                                Text(text = formatShortInr(finalMaxCumulative * 0.50), fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                                Text(text = formatShortInr(finalMaxCumulative * 0.25), fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                                Text(text = "₹0", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                            }

                            // Chart Canvas Area
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(start = 54.dp) // Offset for Y-axis labels
                            ) {
                                val actualColor = MaterialTheme.colorScheme.primary
                                val forecastColor = MaterialTheme.colorScheme.secondary

                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val chartWidth = size.width
                                    val chartHeight = size.height - 24.dp.toPx() // Reserve space at bottom for X labels

                                    if (chartWidth > 0f && chartHeight > 0f) {
                                        val xStep = chartWidth / (totalDaysInMonth - 1)

                                        // Horizontal gridlines (draw across the canvas area)
                                        val gridCount = 4
                                        for (g in 0..gridCount) {
                                            val pct = g.toFloat() / gridCount
                                            val yVal = chartHeight * pct
                                            drawLine(
                                                color = Color.LightGray.copy(alpha = 0.22f),
                                                start = Offset(0f, yVal),
                                                end = Offset(chartWidth, yVal),
                                                strokeWidth = 1.dp.toPx()
                                            )
                                        }

                                        // 1. Draw Actual spent curve
                                        if (elapsedDays > 0) {
                                            val actualPoints = (1..elapsedDays).map { d ->
                                                val x = (d - 1) * xStep
                                                val relativeAmt = (actualCumulative[d] / finalMaxCumulative).toFloat()
                                                val y = chartHeight - (chartHeight * relativeAmt * chartProgress)
                                                Offset(x, y)
                                            }

                                            // Draw actual fill
                                            val actualFillPath = androidx.compose.ui.graphics.Path().apply {
                                                moveTo(0f, chartHeight)
                                                lineTo(actualPoints.first().x, actualPoints.first().y)
                                                for (i in 1 until actualPoints.size) {
                                                    lineTo(actualPoints[i].x, actualPoints[i].y)
                                                }
                                                lineTo(actualPoints.last().x, chartHeight)
                                                close()
                                            }
                                            drawPath(
                                                path = actualFillPath,
                                                brush = Brush.verticalGradient(
                                                    colors = listOf(
                                                        actualColor.copy(alpha = 0.2f),
                                                        actualColor.copy(alpha = 0.005f)
                                                    )
                                                )
                                            )

                                            // Draw actual stroke
                                            val actualStrokePath = androidx.compose.ui.graphics.Path().apply {
                                                moveTo(actualPoints.first().x, actualPoints.first().y)
                                                for (i in 1 until actualPoints.size) {
                                                    lineTo(actualPoints[i].x, actualPoints[i].y)
                                                }
                                            }
                                            drawPath(
                                                path = actualStrokePath,
                                                color = actualColor,
                                                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                                            )
                                        }

                                        // 2. Draw Predicted spent curve
                                        if (elapsedDays < totalDaysInMonth) {
                                            val predictionStartDay = maxOf(1, elapsedDays)
                                            val predictedPoints = (predictionStartDay..totalDaysInMonth).map { d ->
                                                val x = (d - 1) * xStep
                                                val relativeAmt = (predictedCumulative[d] / finalMaxCumulative).toFloat()
                                                val y = chartHeight - (chartHeight * relativeAmt * chartProgress)
                                                Offset(x, y)
                                            }

                                            // Draw prediction fill
                                            val predFillPath = androidx.compose.ui.graphics.Path().apply {
                                                moveTo(predictedPoints.first().x, chartHeight)
                                                lineTo(predictedPoints.first().x, predictedPoints.first().y)
                                                for (i in 1 until predictedPoints.size) {
                                                    lineTo(predictedPoints[i].x, predictedPoints[i].y)
                                                }
                                                lineTo(predictedPoints.last().x, chartHeight)
                                                close()
                                            }
                                            drawPath(
                                                path = predFillPath,
                                                brush = Brush.verticalGradient(
                                                    colors = listOf(
                                                        forecastColor.copy(alpha = 0.12f),
                                                        forecastColor.copy(alpha = 0.001f)
                                                    )
                                                )
                                            )

                                            // Draw prediction stroke (Dashed)
                                            val predStrokePath = androidx.compose.ui.graphics.Path().apply {
                                                moveTo(predictedPoints.first().x, predictedPoints.first().y)
                                                for (i in 1 until predictedPoints.size) {
                                                    lineTo(predictedPoints[i].x, predictedPoints[i].y)
                                                }
                                            }
                                            drawPath(
                                                path = predStrokePath,
                                                color = forecastColor,
                                                style = Stroke(
                                                    width = 3.dp.toPx(),
                                                    cap = StrokeCap.Round,
                                                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
                                                )
                                            )
                                        }

                                        // 3. Draw "Today" vertical cursor guidance
                                        if (elapsedDays in 1 until totalDaysInMonth) {
                                            val todayX = (elapsedDays - 1) * xStep
                                            drawLine(
                                                color = Color.Gray.copy(alpha = 0.35f),
                                                start = Offset(todayX, 0f),
                                                end = Offset(todayX, chartHeight),
                                                strokeWidth = 1.dp.toPx(),
                                                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                                            )
                                        }

                                        // 4. Draw Selected day marker/indicator nodes
                                        if (activeForecastDay in 1..totalDaysInMonth) {
                                            val isPredicted = (activeForecastDay > elapsedDays)
                                            val nodeX = (activeForecastDay - 1) * xStep
                                            val nodeY = chartHeight - (chartHeight * (predictedCumulative[activeForecastDay] / finalMaxCumulative).toFloat() * chartProgress)

                                            // Draw a subtle vertical hover line
                                            drawLine(
                                                color = (if (isPredicted) forecastColor else actualColor).copy(alpha = 0.3f),
                                                start = Offset(nodeX, 0f),
                                                end = Offset(nodeX, chartHeight),
                                                strokeWidth = 1.5.dp.toPx()
                                            )

                                            // Circle node
                                            drawCircle(
                                                color = if (isPredicted) forecastColor else actualColor,
                                                radius = 6.dp.toPx(),
                                                center = Offset(nodeX, nodeY)
                                            )
                                            drawCircle(
                                                color = Color.White,
                                                radius = 3.dp.toPx(),
                                                center = Offset(nodeX, nodeY)
                                            )
                                        }
                                    }
                                }

                                // Interactive Clickable box overlays
                                Row(modifier = Modifier.fillMaxSize().padding(end = 0.dp)) {
                                    for (d in 1..totalDaysInMonth) {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .clickable(
                                                    interactionSource = remember { MutableInteractionSource() },
                                                    indication = null
                                                ) {
                                                    selectedForecastDay = d
                                                }
                                        )
                                    }
                                }

                                // Absolute X Axis Days Labels bottom row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.BottomStart)
                                        .height(20.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Day 1", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("Day 10", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("Day 20", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("Day $totalDaysInMonth", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }

            // --- SELECTED DAY CORRESPONDING FORECAST HOVER BUBBLE DETAIL ---
            item {
                if (activeForecastDay in 1..totalDaysInMonth) {
                    val isFutureForecast = activeForecastDay > elapsedDays
                    val dailyAmt = predictedCumulative[activeForecastDay] - predictedCumulative[activeForecastDay - 1]
                    val cumulAmt = predictedCumulative[activeForecastDay]

                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("forecast_day_detail_card"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isFutureForecast) {
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.15f)
                            } else {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                            }
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = (if (isFutureForecast) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary).copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        color = (if (isFutureForecast) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary).copy(alpha = 0.12f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isFutureForecast) Icons.Default.Star else Icons.Default.Info,
                                    contentDescription = "Day status",
                                    tint = if (isFutureForecast) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Day $activeForecastDay of Month",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isFutureForecast) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = if (isFutureForecast) "Projected spending path" else "Logged spending record",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Accumulated: ${formatCurrency(cumulAmt)}",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (dailyAmt >= 0.0) "+${formatCurrency(dailyAmt)} (Daily)" else "₹0.00 (Daily)",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }

            // --- DATA DRIVEN OPTIMIZATION TIPS FOR THE USER ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("forecast_tips_card"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Optimization Tip",
                                tint = Color(0xFFFBC02D),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Spent Control Insight",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        val tipMessage = remember(predictedCumulative, elapsedDays, projectionDailyRate, totalDaysInMonth) {
                            val projectedTotal = predictedCumulative[totalDaysInMonth]
                            val dailySpend = projectionDailyRate
                            when {
                                projectedTotal > 20000.0 && dailySpend > 800.0 -> {
                                    "Your daily expense rate is a high ${formatCurrency(dailySpend)}. At this speed, you will accumulate around ${formatCurrency(projectedTotal)} by end-of-month. Try limiting luxury expenditures/subscriptions to save at least 15%."
                                }
                                projectedTotal in 5000.0..20000.0 -> {
                                    "Your spending velocity is moderate. Saving an extra ${formatCurrency(dailySpend * 2)} per week will bring down the end prediction value to ${formatCurrency(projectedTotal - dailySpend * 2 * 3)}!"
                                }
                                else -> {
                                    "Phenomenal! Your current daily outflow is very small (${formatCurrency(dailySpend)}). This sets your projected spend to a highly optimized ${formatCurrency(projectedTotal)}. Great job keeping your wallet healthy!"
                                }
                            }
                        }

                        Text(
                            text = tipMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

fun formatShortInr(amount: Double): String {
    return when {
        amount >= 10000000 -> "₹%.1fCr".format(amount / 10000000)
        amount >= 100000 -> "₹%.1fL".format(amount / 100000)
        amount >= 1000 -> "₹%.1fK".format(amount / 1000)
        else -> "₹%.0f".format(amount)
    }
}
