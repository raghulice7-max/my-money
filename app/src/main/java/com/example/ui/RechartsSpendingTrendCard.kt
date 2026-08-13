package com.example.ui

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.R
import com.example.data.TransactionEntity
import com.example.util.formatInrCurrency
import java.text.SimpleDateFormat
import java.util.*

data class MonthlySpendTrend(
    val monthLabel: String,
    val yearMonthKey: String,
    val expense: Double,
    val income: Double,
    val net: Double
)

/**
 * Computes last [monthsCount] months of transactions aggregated by month.
 */
fun computeMonthlySpendingTrends(
    transactions: List<TransactionEntity>,
    monthsCount: Int = 6
): List<MonthlySpendTrend> {
    val sdfMonthLabel = SimpleDateFormat("MMM", Locale.getDefault())
    val sdfYearMonthKey = SimpleDateFormat("yyyy-MM", Locale.getDefault())

    val months = mutableListOf<Calendar>()
    for (i in (monthsCount - 1) downTo 0) {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -i)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        months.add(cal)
    }

    return months.map { monthCal ->
        val startMs = monthCal.timeInMillis
        val endCal = (monthCal.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        val endMs = endCal.timeInMillis

        val monthTxs = transactions.filter { it.dateMillis in startMs..endMs }
        val exp = monthTxs.filter { it.type == "EXPENSE" }.sumOf { it.amount }
        val inc = monthTxs.filter { it.type == "INCOME" }.sumOf { it.amount }

        MonthlySpendTrend(
            monthLabel = sdfMonthLabel.format(monthCal.time),
            yearMonthKey = sdfYearMonthKey.format(monthCal.time),
            expense = exp,
            income = inc,
            net = inc - exp
        )
    }
}

/**
 * Card Component that visualizes monthly spending trends using a custom Recharts Compose Wrapper
 * rendering an interactive Recharts area/line chart inside a Compose WebView wrapper,
 * paired with a native Compose Canvas chart mode and metric insights.
 */
@Composable
fun RechartsSpendingTrendCard(
    transactions: List<TransactionEntity>,
    modifier: Modifier = Modifier
) {
    var monthsWindow by remember { mutableStateOf(6) } // 6 or 12 months
    var viewMetric by remember { mutableStateOf("EXPENSE") } // "EXPENSE", "INCOME", "BOTH"
    var useNativeCanvasChart by remember { mutableStateOf(false) }
    var selectedPointIndex by remember { mutableStateOf(-1) }

    val trends = remember(transactions, monthsWindow) {
        computeMonthlySpendingTrends(transactions, monthsWindow)
    }

    val totalExpenseWindow = remember(trends) { trends.sumOf { it.expense } }
    val avgMonthlyExpense = remember(trends) { if (trends.isNotEmpty()) totalExpenseWindow / trends.size else 0.0 }
    val maxExpenseMonth = remember(trends) { trends.maxByOrNull { it.expense } }

    val latestMonth = trends.lastOrNull()
    val prevMonth = if (trends.size >= 2) trends[trends.size - 2] else null
    val momChangePercent = if (prevMonth != null && prevMonth.expense > 0) {
        ((latestMonth?.expense ?: 0.0) - prevMonth.expense) / prevMonth.expense * 100
    } else 0.0

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("recharts_spending_trend_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoGraph,
                            contentDescription = "Recharts Trends",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Monthly Spending Trends",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Recharts Interactive Compose Visualization",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Window selector (6M vs 12M)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = monthsWindow == 6,
                        onClick = { monthsWindow = 6 },
                        label = { Text("6M", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.height(28.dp)
                    )
                    FilterChip(
                        selected = monthsWindow == 12,
                        onClick = { monthsWindow = 12 },
                        label = { Text("12M", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.height(28.dp)
                    )
                }
            }

            // High-Level Stat Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Monthly Avg Expense",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatInrCurrency(avgMonthlyExpense),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "MoM Trend",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val isUp = momChangePercent > 0
                        Icon(
                            imageVector = if (isUp) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                            contentDescription = null,
                            tint = if (isUp) MaterialTheme.colorScheme.error else Color(0xFF2E7D32),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = String.format(Locale.US, "%.1f%%", Math.abs(momChangePercent)),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isUp) MaterialTheme.colorScheme.error else Color(0xFF2E7D32)
                        )
                    }
                }
            }

            // Toggle metric: Expenses / Income / Both
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = viewMetric == "EXPENSE",
                    onClick = { viewMetric = "EXPENSE" },
                    label = { Text("Expenses", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = viewMetric == "INCOME",
                    onClick = { viewMetric = "INCOME" },
                    label = { Text("Income", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFE8F5E9),
                        selectedLabelColor = Color(0xFF2E7D32)
                    ),
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = viewMetric == "BOTH",
                    onClick = { viewMetric = "BOTH" },
                    label = { Text("Comparison", fontSize = 11.sp) },
                    modifier = Modifier.weight(1f)
                )
            }

            // Recharts Custom Wrapper Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(8.dp)
                    .testTag("recharts_wrapper_container"),
                contentAlignment = Alignment.Center
            ) {
                if (!useNativeCanvasChart) {
                    RechartsWebViewWrapper(
                        trends = trends,
                        viewMetric = viewMetric,
                        onErrorFallback = { useNativeCanvasChart = true }
                    )
                } else {
                    RechartsNativeCanvasWrapper(
                        trends = trends,
                        viewMetric = viewMetric,
                        selectedIndex = selectedPointIndex,
                        onSelectIndex = { selectedPointIndex = it }
                    )
                }
            }

            // Selected detail highlight if tapped in Native Canvas or info note
            if (selectedPointIndex in trends.indices) {
                val point = trends[selectedPointIndex]
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Month: ${point.monthLabel}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "Expense: ${formatInrCurrency(point.expense)}  |  Income: ${formatInrCurrency(point.income)}",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Maximum Expense Month Insight
            maxExpenseMonth?.let { maxItem ->
                if (maxItem.expense > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Highest monthly spending was in ${maxItem.monthLabel} (${formatInrCurrency(maxItem.expense)}).",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Recharts Web Wrapper utilizing a Jetpack Compose [AndroidView] with an embedded HTML/JS template
 * that renders responsive Recharts Area & Bar charts based on modern Web standards.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun RechartsWebViewWrapper(
    trends: List<MonthlySpendTrend>,
    viewMetric: String,
    onErrorFallback: () -> Unit
) {
    val context = LocalContext.current

    val jsonTrendData = remember(trends) {
        val sb = StringBuilder("[")
        trends.forEachIndexed { idx, item ->
            sb.append("""{"month":"${item.monthLabel}","expense":${item.expense},"income":${item.income},"net":${item.net}}""")
            if (idx < trends.size - 1) sb.append(",")
        }
        sb.append("]")
        sb.toString()
    }

    val htmlContent = remember(jsonTrendData, viewMetric) {
        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
            <script src="https://unpkg.com/react@18/umd/react.production.min.js" crossorigin></script>
            <script src="https://unpkg.com/react-dom@18/umd/react-dom.production.min.js" crossorigin></script>
            <script src="https://unpkg.com/recharts@2.12.7/dist/Recharts.js"></script>
            <style>
                body {
                    margin: 0;
                    padding: 0;
                    background-color: transparent;
                    font-family: system-ui, -apple-system, Roboto, sans-serif;
                    overflow: hidden;
                    user-select: none;
                }
                #root {
                    width: 100vw;
                    height: 100vh;
                    display: flex;
                    justify-content: center;
                    align-items: center;
                }
                .recharts-tooltip-wrapper {
                    font-size: 11px !important;
                    border-radius: 8px !important;
                }
            </style>
        </head>
        <body>
            <div id="root"></div>
            <script>
                const data = $jsonTrendData;
                const viewMetric = "$viewMetric";
                const { ResponsiveContainer, AreaChart, Area, BarChart, Bar, XAxis, YAxis, Tooltip, CartesianGrid, Legend } = Recharts;

                function App() {
                    const formatYAxis = (tickItem) => {
                        if (tickItem >= 1000) return '₹' + (tickItem / 1000).toFixed(0) + 'k';
                        return '₹' + tickItem;
                    };

                    const formatCurrency = (val) => '₹' + Number(val).toLocaleString('en-IN');

                    if (viewMetric === 'BOTH') {
                        return React.createElement(ResponsiveContainer, { width: "100%", height: "100%" },
                            React.createElement(BarChart, { data: data, margin: { top: 10, right: 10, left: -20, bottom: 0 } },
                                React.createElement(CartesianGrid, { strokeDasharray: "3 3", stroke: "#e0e0e0" }),
                                React.createElement(XAxis, { dataKey: "month", tick: { fontSize: 10, fill: '#666' } }),
                                React.createElement(YAxis, { tickFormatter: formatYAxis, tick: { fontSize: 10, fill: '#666' } }),
                                React.createElement(Tooltip, { formatter: (val) => formatCurrency(val) }),
                                React.createElement(Bar, { dataKey: "expense", name: "Expense", fill: "#E53935", radius: [4, 4, 0, 0] }),
                                React.createElement(Bar, { dataKey: "income", name: "Income", fill: "#2E7D32", radius: [4, 4, 0, 0] })
                            )
                        );
                    }

                    const isExpense = viewMetric === 'EXPENSE';
                    val strokeColor = isExpense ? "#E53935" : "#2E7D32";
                    val fillColor = isExpense ? "url(#colorExpense)" : "url(#colorIncome)";

                    return React.createElement(ResponsiveContainer, { width: "100%", height: "100%" },
                        React.createElement(AreaChart, { data: data, margin: { top: 10, right: 10, left: -20, bottom: 0 } },
                            React.createElement('defs', null,
                                React.createElement('linearGradient', { id: 'colorExpense', x1: '0', y1: '0', x2: '0', y2: '1' },
                                    React.createElement('stop', { offset: '5%', stopColor: '#E53935', stopOpacity: 0.6 }),
                                    React.createElement('stop', { offset: '95%', stopColor: '#E53935', stopOpacity: 0.05 })
                                ),
                                React.createElement('linearGradient', { id: 'colorIncome', x1: '0', y1: '0', x2: '0', y2: '1' },
                                    React.createElement('stop', { offset: '5%', stopColor: '#2E7D32', stopOpacity: 0.6 }),
                                    React.createElement('stop', { offset: '95%', stopColor: '#2E7D32', stopOpacity: 0.05 })
                                )
                            ),
                            React.createElement(CartesianGrid, { strokeDasharray: "3 3", stroke: "#eceff1" }),
                            React.createElement(XAxis, { dataKey: "month", tick: { fontSize: 11, fill: '#546e7a' } }),
                            React.createElement(YAxis, { tickFormatter: formatYAxis, tick: { fontSize: 10, fill: '#546e7a' } }),
                            React.createElement(Tooltip, { formatter: (val) => formatCurrency(val) }),
                            React.createElement(Area, {
                                type: "monotone",
                                dataKey: isExpense ? "expense" : "income",
                                stroke: strokeColor,
                                strokeWidth: 2.5,
                                fillOpacity: 1,
                                fill: fillColor
                            })
                        )
                    );
                }

                ReactDOM.render(React.createElement(App), document.getElementById('root'));
            </script>
        </body>
        </html>
        """.trimIndent()
    }

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = true
                webViewClient = object : WebViewClient() {
                    override fun onReceivedError(
                        view: WebView?,
                        errorCode: Int,
                        description: String?,
                        failingUrl: String?
                    ) {
                        onErrorFallback()
                    }
                }
                loadDataWithBaseURL("https://recharts.org", htmlContent, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL("https://recharts.org", htmlContent, "text/html", "UTF-8", null)
        },
        modifier = Modifier.fillMaxSize()
    )
}

/**
 * Native Jetpack Compose Canvas fallback that replicates the Recharts smooth curve area chart style.
 */
@Composable
private fun RechartsNativeCanvasWrapper(
    trends: List<MonthlySpendTrend>,
    viewMetric: String,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit
) {
    val primaryColor = if (viewMetric == "INCOME") Color(0xFF2E7D32) else MaterialTheme.colorScheme.error

    if (trends.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No data available", style = MaterialTheme.typography.bodySmall)
        }
        return
    }

    val maxVal = remember(trends, viewMetric) {
        val max = when (viewMetric) {
            "INCOME" -> trends.maxOfOrNull { it.income } ?: 100.0
            "BOTH" -> trends.maxOfOrNull { maxOf(it.expense, it.income) } ?: 100.0
            else -> trends.maxOfOrNull { it.expense } ?: 100.0
        }
        if (max <= 0) 100.0 else max
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .pointerInput(trends) {
                    detectTapGestures { offset ->
                        val stepX = size.width / (trends.size - 1).coerceAtLeast(1)
                        val tappedIdx = (offset.x / stepX).toInt().coerceIn(0, trends.size - 1)
                        onSelectIndex(tappedIdx)
                    }
                }
        ) {
            val width = size.width
            val height = size.height
            val stepX = width / (trends.size - 1).coerceAtLeast(1)

            val strokePath = Path()
            val fillPath = Path()

            trends.forEachIndexed { i, item ->
                val value = if (viewMetric == "INCOME") item.income else item.expense
                val ratio = (value / maxVal).toFloat()
                val x = i * stepX
                val y = height - (ratio * (height - 30.dp.toPx())) - 10.dp.toPx()

                if (i == 0) {
                    strokePath.moveTo(x, y)
                    fillPath.moveTo(x, height)
                    fillPath.lineTo(x, y)
                } else {
                    val prevX = (i - 1) * stepX
                    val prevVal = if (viewMetric == "INCOME") trends[i - 1].income else trends[i - 1].expense
                    val prevRatio = (prevVal / maxVal).toFloat()
                    val prevY = height - (prevRatio * (height - 30.dp.toPx())) - 10.dp.toPx()

                    val controlX1 = prevX + (stepX / 2)
                    val controlX2 = x - (stepX / 2)

                    strokePath.cubicTo(controlX1, prevY, controlX2, y, x, y)
                    fillPath.cubicTo(controlX1, prevY, controlX2, y, x, y)
                }

                if (i == trends.size - 1) {
                    fillPath.lineTo(x, height)
                    fillPath.close()
                }
            }

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(primaryColor.copy(alpha = 0.35f), primaryColor.copy(alpha = 0.02f))
                )
            )

            drawPath(
                path = strokePath,
                color = primaryColor,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            trends.forEachIndexed { i, _ ->
                val value = if (viewMetric == "INCOME") trends[i].income else trends[i].expense
                val ratio = (value / maxVal).toFloat()
                val x = i * stepX
                val y = height - (ratio * (height - 30.dp.toPx())) - 10.dp.toPx()

                val isSel = i == selectedIndex
                drawCircle(
                    color = if (isSel) Color.White else primaryColor,
                    radius = if (isSel) 6.dp.toPx() else 4.dp.toPx(),
                    center = Offset(x, y)
                )
                drawCircle(
                    color = primaryColor,
                    radius = if (isSel) 4.dp.toPx() else 2.dp.toPx(),
                    center = Offset(x, y),
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }
        }

        // X-Axis Labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            trends.forEach { item ->
                Text(
                    text = item.monthLabel,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
