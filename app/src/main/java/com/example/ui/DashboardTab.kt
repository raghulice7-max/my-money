package com.example.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import com.example.ui.theme.bounceClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BudgetEntity
import com.example.data.TransactionEntity
import com.example.data.PendingSmsTransactionEntity
import com.example.data.InvestmentEntity
import com.example.data.GoalEntity
import com.example.data.RecurringReminderEntity
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardTab(
    transactions: List<TransactionEntity>,
    budgets: List<BudgetEntity>,
    onNavigateToTransactions: () -> Unit,
    onNavigateToFuel: () -> Unit,
    onSetBudget: (String, Double) -> Unit,
    modifier: Modifier = Modifier,
    pendingTransactions: List<PendingSmsTransactionEntity> = emptyList(),
    onApprovePending: (PendingSmsTransactionEntity, String, String, Double, String) -> Unit = { _, _, _, _, _ -> },
    onDiscardPending: (PendingSmsTransactionEntity) -> Unit = {},
    userName: String = "Explorer",
    userEmail: String = "raghulice7@gmail.com",
    userWantsSummary: Boolean = true,
    onTriggerEmailSummary: () -> Unit = {},
    onSyncSmsInbox: () -> Unit = {},
    investments: List<InvestmentEntity> = emptyList(),
    goals: List<GoalEntity> = emptyList(),
    onAddInvestment: (String, String, Double, Double, Long, String) -> Unit = { _, _, _, _, _, _ -> },
    onDeleteInvestment: (InvestmentEntity) -> Unit = {},
    onUpdateInvestment: (InvestmentEntity) -> Unit = {},
    onAddGoal: (String, Double, Double, Long, String, String) -> Unit = { _, _, _, _, _, _ -> },
    onDeleteGoal: (GoalEntity) -> Unit = {},
    onUpdateGoal: (GoalEntity) -> Unit = {},
    onContributeToGoal: (GoalEntity, Double) -> Unit = { _, _ -> },
    recurringReminders: List<RecurringReminderEntity> = emptyList(),
    onAddReminder: (String, Double, Int, String, String, String) -> Unit = { _, _, _, _, _, _ -> },
    onDeleteReminder: (RecurringReminderEntity) -> Unit = {},
    onUpdateReminder: (RecurringReminderEntity) -> Unit = {},
    onPayReminder: (RecurringReminderEntity, (paymentId: Long, previousLastPaidDate: java.time.LocalDate?) -> Unit) -> Unit = { _, _ -> },
    onUndoPayReminder: (RecurringReminderEntity, Long, java.time.LocalDate?) -> Unit = { _, _, _ -> }
) {
    var showBudgetDialog by remember { mutableStateOf(false) }
    var selectedCategoryForDetail by remember { mutableStateOf<String?>(null) }
    var activeSubTab by remember { mutableStateOf(0) }

    // SMS permission states
    val context = LocalContext.current
    var hasSmsPermission by remember {
        mutableStateOf(
            context.checkSelfPermission(android.Manifest.permission.RECEIVE_SMS) == android.content.pm.PackageManager.PERMISSION_GRANTED &&
            context.checkSelfPermission(android.Manifest.permission.READ_SMS) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    val smsPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasSmsPermission = permissions.values.all { it }
    }

    var hasNotificationPermission by remember { mutableStateOf(false) }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                hasNotificationPermission = isNotificationServiceEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Computations
    var showAllTimeData by remember { mutableStateOf(false) } // Default to current month only as requested!

    val currentCalendar = remember { Calendar.getInstance() }
    val currentMonth = currentCalendar.get(Calendar.MONTH)
    val currentYear = currentCalendar.get(Calendar.YEAR)

    val currentMonthName = remember {
        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        monthFormat.format(Date())
    }

    val filteredTransactions = remember(transactions, showAllTimeData) {
        if (showAllTimeData) {
            transactions
        } else {
            transactions.filter { tx ->
                val txCalendar = Calendar.getInstance().apply { timeInMillis = tx.dateMillis }
                txCalendar.get(Calendar.MONTH) == currentMonth && txCalendar.get(Calendar.YEAR) == currentYear
            }
        }
    }

    val totalIncome = filteredTransactions.filter { it.type == "INCOME" }.sumOf { it.amount }
    val totalExpense = filteredTransactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    val netBalance = totalIncome - totalExpense

    val totalBudgetLimit = budgets.find { it.category == "TOTAL" }?.limitAmount ?: 1000.0
    // Current month expense specifically for budget tracking
    val currentMonthExpense = remember(transactions) {
        transactions.filter { tx ->
            val txCalendar = Calendar.getInstance().apply { timeInMillis = tx.dateMillis }
            txCalendar.get(Calendar.MONTH) == currentMonth && txCalendar.get(Calendar.YEAR) == currentYear && tx.type == "EXPENSE"
        }.sumOf { it.amount }
    }
    val budgetProgress = if (totalBudgetLimit > 0) (currentMonthExpense / totalBudgetLimit).coerceIn(0.0, 1.0) else 0.0
    val animatedBudgetProgress by animateFloatAsState(
        targetValue = budgetProgress.toFloat(),
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "budgetProgressAnim"
    )

    val expensesFiltered = remember(filteredTransactions) {
        filteredTransactions.filter { it.type == "EXPENSE" }
    }
    val categoryExpenseTotals = remember(expensesFiltered) {
        expensesFiltered
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }
    }
    val totalExpenseSpent = remember(categoryExpenseTotals) {
        categoryExpenseTotals.sumOf { it.second }
    }

    val recentTransactions = filteredTransactions.take(5)

    // Portfolio and wealth building metrics
    val periodSavingsTxSum = remember(filteredTransactions) {
        filteredTransactions.filter { it.category == "Savings" }.sumOf { it.amount }
    }
    val periodInvestmentTxSum = remember(filteredTransactions) {
        filteredTransactions.filter { it.category == "Investment" }.sumOf { it.amount }
    }
    val wealthBuildingSum = periodSavingsTxSum + periodInvestmentTxSum
    val wealthRate = if (totalIncome > 0) (wealthBuildingSum / totalIncome) * 100 else 0.0

    val activeGoalsSavingsSum = remember(goals) {
        goals.sumOf { it.currentAmount }
    }
    val activeInvestmentsValueSum = remember(investments) {
        investments.sumOf { it.currentValue }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_tab_root")
    ) {
        TabRow(
            selectedTabIndex = activeSubTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth().testTag("dashboard_sub_tabs")
        ) {
            Tab(
                selected = activeSubTab == 0,
                onClick = { activeSubTab = 0 },
                text = { Text("Overview", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                icon = { Icon(Icons.Default.Dashboard, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("dashboard_sub_tab_overview")
            )
            Tab(
                selected = activeSubTab == 1,
                onClick = { activeSubTab = 1 },
                text = { Text("Investments", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("dashboard_sub_tab_investments")
            )
            Tab(
                selected = activeSubTab == 2,
                onClick = { activeSubTab = 2 },
                text = { Text("Goals", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                icon = { Icon(Icons.Default.Savings, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("dashboard_sub_tab_goals")
            )
            Tab(
                selected = activeSubTab == 3,
                onClick = { activeSubTab = 3 },
                text = { Text("SIP & RD", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                icon = { Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("dashboard_sub_tab_reminders")
            )
        }

        AnimatedContent(
            targetState = activeSubTab,
            modifier = Modifier.fillMaxSize().weight(1f),
            transitionSpec = {
                (fadeIn(animationSpec = tween(150))).togetherWith(fadeOut(animationSpec = tween(150)))
            },
            label = "tabContent"
        ) { targetSubTab ->
            when (targetSubTab) {
                0 -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
        // Welcome and Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Hello $userName!",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "My Finances",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(
                    onClick = { showBudgetDialog = true },
                    modifier = Modifier.background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Set Budget",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        // Toggle selector: Current Month vs All Time
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = !showAllTimeData,
                    onClick = { showAllTimeData = false },
                    label = { Text(currentMonthName) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Current Month Only",
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.testTag("filter_current_month_chip")
                )

                FilterChip(
                    selected = showAllTimeData,
                    onClick = { showAllTimeData = true },
                    label = { Text("All-Time") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "All Time History",
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.testTag("filter_all_time_chip")
                )
            }
        }

        // Net Balance Card with Gradient
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("net_balance_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                                )
                            )
                        )
                        .padding(24.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = if (showAllTimeData) "Net Balance (Overall)" else "$currentMonthName Net Balance",
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatCurrency(netBalance),
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Income summary
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color.White.copy(alpha = 0.15f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.TrendingUp,
                                        contentDescription = "Income",
                                        tint = Color(0xFF81C784)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Income",
                                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                    Text(
                                        text = formatCurrency(totalIncome),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }

                            // Expenses summary
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color.White.copy(alpha = 0.15f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.TrendingDown,
                                        contentDescription = "Expenses",
                                        tint = Color(0xFFE57373)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Expenses",
                                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                    Text(
                                        text = formatCurrency(totalExpense),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Wealth Building & Insights Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("wealth_building_insights_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Analytics,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Wealth Building Insights",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        // Custom rate pill
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            Text(
                                text = "${String.format(Locale.getDefault(), "%.1f", wealthRate)}% Rate",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Your allocated Savings & Investments as a percentage of overall incoming revenue.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Savings Column
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .background(MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Savings,
                                    contentDescription = "Savings",
                                    tint = Color(0xFF3F51B5),
                                    modifier = Modifier.size(18.dp)
                               )
                                Text(
                                    text = "Savings Logs",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = formatCurrency(periodSavingsTxSum),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (activeGoalsSavingsSum > 0) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Total Goals: ${formatCurrency(activeGoalsSavingsSum)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        // Investments Column
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .background(MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TrendingUp,
                                    contentDescription = "Investment",
                                    tint = Color(0xFF009688),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Investment Logs",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = formatCurrency(periodInvestmentTxSum),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (activeInvestmentsValueSum > 0) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Portfolio: ${formatCurrency(activeInvestmentsValueSum)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Recharts Interactive Spending Trend Card
        item {
            RechartsSpendingTrendCard(
                transactions = transactions,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        // Month-End A.I. Summary Card (Interactive layout)
        if (userWantsSummary) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .bounceClickable { onTriggerEmailSummary() }
                        .testTag("month_end_summary_banner"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(MaterialTheme.colorScheme.secondary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Summary Logo",
                                tint = MaterialTheme.colorScheme.onSecondary
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "A.I. Month-End Report",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Draft, analyze & email report to $userEmail",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Proceed link",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }

        // Pending Transaction Reviews Section
        if (pendingTransactions.isNotEmpty() || !hasSmsPermission || !hasNotificationPermission) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SMS Auto-Track Staging",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    if (hasSmsPermission) {
                        TextButton(
                            onClick = onSyncSmsInbox,
                            modifier = Modifier.testTag("sms_sync_inbox_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Sync SMS Inbox",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sync Inbox", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            if (!hasSmsPermission) {
                item {
                    SmsPermissionRequestCard(
                        onGrantRequest = {
                            smsPermissionLauncher.launch(
                                arrayOf(
                                    android.Manifest.permission.RECEIVE_SMS,
                                    android.Manifest.permission.READ_SMS
                                )
                            )
                        }
                    )
                }
            }

            if (hasSmsPermission && !hasNotificationPermission) {
                item {
                    RcsNotificationTrackerCard(
                        onEnableRequest = {
                            try {
                                val intent = android.content.Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                try {
                                    val intent = android.content.Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                    context.startActivity(intent)
                                } catch (ex: Exception) {
                                    context.startActivity(android.content.Intent(android.provider.Settings.ACTION_SETTINGS))
                                }
                            }
                        }
                    )
                }
            }

            if (pendingTransactions.isNotEmpty()) {
                items(pendingTransactions, key = { pending -> "${pending.id}_${pending.dateMillis}" }) { pending ->
                    PendingSmsTransactionReviewCard(
                        pending = pending,
                        onApprove = { category, payee, amount, note ->
                            onApprovePending(pending, category, payee, amount, note)
                        },
                        onDiscard = {
                            onDiscardPending(pending)
                        }
                    )
                }
            }
        }

        // Quick Monthly Category Expenses Breakdown
        if (categoryExpenseTotals.isNotEmpty()) {
            item {
                Text(
                    text = "Monthly Expense Distribution",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                )
            }

            item {
                // Donut Chart Card
                var triggerChartAnim by remember { mutableStateOf(false) }
                LaunchedEffect(categoryExpenseTotals) {
                    triggerChartAnim = true
                }
                val chartAnimProgress by animateFloatAsState(
                    targetValue = if (triggerChartAnim) 1f else 0f,
                    animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
                    label = "overviewDonutChartAnim"
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Donut Graphic
                        Box(
                            modifier = Modifier.size(130.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.size(110.dp)) {
                                var accumulatedAngle = -90f

                                categoryExpenseTotals.forEach { catTotal ->
                                    val catName = catTotal.first
                                    val catAmt = catTotal.second
                                    val catStyle = FinanceCategory.getStyleFor(catName)

                                    val sweepAngle = 360f * (catAmt / totalExpenseSpent).toFloat() * chartAnimProgress

                                    drawArc(
                                        color = catStyle.color,
                                        startAngle = accumulatedAngle,
                                        sweepAngle = sweepAngle,
                                        useCenter = false,
                                        style = Stroke(width = 15.dp.toPx(), cap = StrokeCap.Round),
                                        size = Size(size.width, size.height)
                                    )

                                    accumulatedAngle += sweepAngle
                                }
                            }

                            // Center Label
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "Expenses",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = formatCurrency(totalExpenseSpent),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.widthIn(max = 90.dp)
                                )
                            }
                        }

                        // Donut Legend List
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            categoryExpenseTotals.take(5).forEach { entry ->
                                val pct = if (totalExpenseSpent > 0) (entry.second / totalExpenseSpent) * 100 else 0.0
                                val style = FinanceCategory.getStyleFor(entry.first)

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(9.dp)
                                            .background(style.color, RoundedCornerShape(2.dp))
                                    )
                                    Text(
                                        text = "${entry.first} ${String.format(Locale.getDefault(), "%.1f%%", pct)}",
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
                            if (categoryExpenseTotals.size > 5) {
                                Text(
                                    text = "+ ${categoryExpenseTotals.size - 5} more categories",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(start = 17.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Divider or title for rows
            item {
                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            // Items breakdown
            items(categoryExpenseTotals, key = { it.first }) { entry ->
                val categoryName = entry.first
                val spentAmount = entry.second
                val style = FinanceCategory.getStyleFor(categoryName)
                val percentage = if (totalExpenseSpent > 0) (spentAmount / totalExpenseSpent) * 100 else 0.0

                var rowTriggered by remember { mutableStateOf(false) }
                LaunchedEffect(entry) {
                    rowTriggered = true
                }
                val rowAnimProgress by animateFloatAsState(
                    targetValue = if (rowTriggered) (spentAmount / totalExpenseSpent).toFloat() else 0f,
                    animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
                    label = "rowProgressAnim_${categoryName}"
                )

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .bounceClickable { selectedCategoryForDetail = categoryName }
                        .testTag("overview_category_card_${categoryName.lowercase()}"),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Category colored solid circle
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(style.color, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = style.icon,
                                contentDescription = categoryName,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            // Category name and value
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = categoryName,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                )
                                Text(
                                    text = "-${formatCurrency(spentAmount)}",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFE53935)
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Custom outlined progress bar and percentage value
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                LinearProgressIndicator(
                                    progress = { rowAnimProgress },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .border(0.5.dp, Color(0xFF0F3A30).copy(alpha = 0.2f), RoundedCornerShape(4.dp)),
                                    color = Color(0xFF0F3A30), // PinePrimary-style solid premium organic dark-green
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )

                                Text(
                                    text = String.format(Locale.getDefault(), "%.1f%%", percentage),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.widthIn(min = 40.dp)
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Empty state if no expenses are recorded
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "No Expenses",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Expenses Tracked Yet",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Log transactions in the Ledger tab to view your monthly breakdown.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }

        // Recent Transactions Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Spending & Activity",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                TextButton(onClick = onNavigateToTransactions) {
                    Text("See All")
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "See All Transactions",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Recent Transaction list
        if (recentTransactions.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No transactions logged yet",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap logs or Refuel to add some!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(recentTransactions) { tx ->
                val style = FinanceCategory.getStyleFor(tx.category)
                TransactionItem(
                    transaction = tx,
                    categoryStyle = style,
                    onClick = onNavigateToTransactions
                )
            }
        }

        // Savings Goals Summary Section
        item {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "My Savings Goals",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    TextButton(onClick = { activeSubTab = 2 }) {
                        Text("Manage")
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Manage Goals",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                if (goals.isEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { activeSubTab = 2 },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Savings,
                                contentDescription = "Add Goal",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = "No savings goals configured",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Create custom piggybanks to save up on time.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        items(goals) { goal ->
                            val progress = if (goal.targetAmount > 0) (goal.currentAmount / goal.targetAmount).coerceIn(0.0, 1.0) else 0.0
                            val progressPercent = (progress * 100).toInt()
                            val themeColor = when (goal.category) {
                                "Emergency" -> Color(0xFFC62828)
                                "Savings" -> Color(0xFF2E7D32)
                                "Travel" -> Color(0xFF1565C0)
                                "Vehicle" -> Color(0xFFEF6C00)
                                "Home" -> Color(0xFF651FFF)
                                "Education" -> Color(0xFF00838F)
                                else -> MaterialTheme.colorScheme.primary
                            }

                            Card(
                                modifier = Modifier
                                    .width(200.dp)
                                    .clickable { activeSubTab = 2 },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = goal.name,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )

                                        val cIcon = when (goal.category) {
                                            "Emergency" -> Icons.Default.HealthAndSafety
                                            "Travel" -> Icons.Default.FlightTakeoff
                                            "Vehicle" -> Icons.Default.DirectionsCar
                                            "Home" -> Icons.Default.Home
                                            "Education" -> Icons.Default.School
                                            else -> Icons.Default.Savings
                                        }
                                        Icon(
                                            imageVector = cIcon,
                                            contentDescription = goal.category,
                                            tint = themeColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    val formattedTarget = remember(goal.targetAmount) {
                                        NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
                                            maximumFractionDigits = 0
                                        }.format(goal.targetAmount)
                                    }
                                    val formattedSaved = remember(goal.currentAmount) {
                                        NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
                                            maximumFractionDigits = 0
                                        }.format(goal.currentAmount)
                                    }

                                    Text(
                                        text = "$formattedSaved / $formattedTarget",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    GoalProgressBar(
                                        progress = progress.toFloat(),
                                        themeColor = themeColor,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = "$progressPercent% saved",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = themeColor
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Quick Refuel Redirect Callout card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .bounceClickable { onNavigateToFuel() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.secondary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalGasStation,
                            contentDescription = "Fuel Logs",
                            tint = MaterialTheme.colorScheme.onSecondary
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1.0f)) {
                        Text(
                            text = "Track Fuel Expenses",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "Log odometer, fill volume, calculate fuel economy trends",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Open Fuel Logs",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    } // Closes LazyColumn
                } // Closes branch 0
                1 -> {
                    InvestmentsSection(
                        investments = investments,
                        onAddInvestment = onAddInvestment,
                        onDeleteInvestment = onDeleteInvestment,
                        onUpdateInvestment = onUpdateInvestment,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                2 -> {
                    GoalsSection(
                        goals = goals,
                        onAddGoal = onAddGoal,
                        onDeleteGoal = onDeleteGoal,
                        onUpdateGoal = onUpdateGoal,
                        onContributeToGoal = onContributeToGoal,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                3 -> {
                    RemindersSection(
                        reminders = recurringReminders,
                        onAddReminder = onAddReminder,
                        onDeleteReminder = onDeleteReminder,
                        onUpdateReminder = onUpdateReminder,
                        onPayReminder = onPayReminder,
                        onUndoPayReminder = onUndoPayReminder,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    } // Closes Column

    // Set Budget Dialog
    if (showBudgetDialog) {
        var budgetInput by remember { mutableStateOf(totalBudgetLimit.toString()) }
        var selectedCategoryInput by remember { mutableStateOf("TOTAL") }
        val categoryOptions = listOf("TOTAL") + FinanceCategory.categories.filter { it.isExpense }.map { it.name }

        AlertDialog(
            onDismissRequest = { showBudgetDialog = false },
            title = { Text("Configure Budget Goals") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Set target limits to prevent overspending and analyze progress live.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    // Pick Category
                    Column {
                        Text("Category", style = MaterialTheme.typography.labelSmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        var dropdownExpanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { dropdownExpanded = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (selectedCategoryInput == "TOTAL") "Total Monthly Limit" else selectedCategoryInput
                                    )
                                    Icon(
                                        imageVector = Icons.Default.TrendingDown,
                                        contentDescription = "Select"
                                    )
                                }
                            }
                            DropdownMenu(
                                expanded = dropdownExpanded,
                                onDismissRequest = { dropdownExpanded = false }
                            ) {
                                categoryOptions.forEach { opt ->
                                    DropdownMenuItem(
                                        text = { Text(if (opt == "TOTAL") "Total Monthly Limit" else opt) },
                                        onClick = {
                                            selectedCategoryInput = opt
                                            dropdownExpanded = false

                                            // Default to existing if exists
                                            val currentLimit = budgets.find { it.category == opt }?.limitAmount
                                            budgetInput = currentLimit?.toString() ?: ""
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Limit input
                    OutlinedTextField(
                        value = budgetInput,
                        onValueChange = { budgetInput = it },
                        label = { Text("Budget Limit Amount (₹)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = budgetInput.toDoubleOrNull()
                        if (amt != null && amt >= 0) {
                            onSetBudget(selectedCategoryInput, amt)
                        }
                        showBudgetDialog = false
                    }
                ) {
                    Text("Save Limit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBudgetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Category Detail Transactions Dialog
    if (selectedCategoryForDetail != null) {
        val categoryName = selectedCategoryForDetail!!
        val filteredTxList = transactions.filter { it.category == categoryName }.sortedByDescending { it.dateMillis }
        val budgetEntity = budgets.find { it.category == categoryName }
        val limit = budgetEntity?.limitAmount ?: 0.0
        val spent = filteredTxList.filter { it.type == "EXPENSE" }.sumOf { it.amount }
        val style = FinanceCategory.getStyleFor(categoryName)

        AlertDialog(
            onDismissRequest = { selectedCategoryForDetail = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(style.color.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = style.icon,
                            contentDescription = categoryName,
                            tint = style.color,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "$categoryName Transactions",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (limit > 0.0) {
                        val prog = (spent / limit).coerceIn(0.0, 1.0)
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Spent vs Budget",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${formatCurrency(spent)} / ${formatCurrency(limit)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { prog.toFloat() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = style.color,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Total spent this month",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatCurrency(spent),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    if (filteredTxList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.ReceiptLong,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No transactions found",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 350.dp)
                        ) {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(filteredTxList) { tx ->
                                    TransactionItem(
                                        transaction = tx,
                                        categoryStyle = style,
                                        onClick = {
                                            selectedCategoryForDetail = null
                                            onNavigateToTransactions()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { selectedCategoryForDetail = null },
                    modifier = Modifier.testTag("category_detail_close_button")
                ) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun TransactionItem(
    transaction: TransactionEntity,
    categoryStyle: CategoryStyle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formatter = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }
    val dateString = formatter.format(Date(transaction.dateMillis))

    val (paymentMethod, cleanNote) = remember(transaction.note) {
        when {
            transaction.note.startsWith("[Card] ") -> "Card" to transaction.note.removePrefix("[Card] ")
            transaction.note.startsWith("[Wallet] ") -> "Wallet" to transaction.note.removePrefix("[Wallet] ")
            transaction.note == "[Card]" -> "Card" to ""
            transaction.note == "[Wallet]" -> "Wallet" to ""
            transaction.category == "Salary" || transaction.category == "Investment" || transaction.category == "Savings" || transaction.category == "Business & Freelance" -> "Card" to transaction.note
            transaction.category == "Snacks" || transaction.category == "Groceries" || transaction.category == "Food & Dining" || transaction.category == "Other" -> "Wallet" to transaction.note
            else -> "Card" to transaction.note
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .bounceClickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(categoryStyle.color, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = categoryStyle.icon,
                    contentDescription = transaction.category,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Text
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.payeeOrSource,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Payment Method Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Icon(
                            imageVector = if (paymentMethod == "Card") Icons.Default.CreditCard else Icons.Default.AccountBalanceWallet,
                            contentDescription = paymentMethod,
                            tint = if (paymentMethod == "Card") Color(0xFFEF5350) else Color(0xFFFFA726),
                            modifier = Modifier.size(10.dp)
                        )
                        Text(
                            text = paymentMethod,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    val detailsNoteText = if (cleanNote.isNotBlank()) "• $cleanNote" else ""
                    Text(
                        text = "$dateString $detailsNoteText",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Amount
            val prefix = if (transaction.type == "INCOME") "+" else "-"
            val amtColor = if (transaction.type == "INCOME") Color(0xFF4CAF50) else Color(0xFFE53935)
            Text(
                text = "$prefix${formatCurrency(transaction.amount)}",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = amtColor
            )
        }
    }
}

fun formatCurrency(amount: Double): String {
    if (amount.isNaN() || amount.isInfinite()) {
        return "₹0.00"
    }
    return try {
        val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        format.format(amount)
    } catch (e: Exception) {
        "₹${String.format(Locale.US, "%.2f", amount)}"
    }
}

// Custom SMS Review Components
@Composable
fun SmsPermissionRequestCard(
    onGrantRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Sms,
                    contentDescription = "SMS Track icon",
                    tint = MaterialTheme.colorScheme.tertiary
                )
                Text(
                    text = "Track Expenses from SMS Automatically",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
            Text(
                text = "Activate real-time parsing to automatically extract transaction details fields from standard Indian banking SMS. You review and approve every log before it is logged permanently.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
            )
            Button(
                onClick = onGrantRequest,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.align(Alignment.End)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Grant",
                        modifier = Modifier.size(16.dp)
                    )
                    Text("Grant SMS Permission", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PendingSmsTransactionReviewCard(
    pending: PendingSmsTransactionEntity,
    onApprove: (String, String, Double, String) -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var isExpanded by remember(pending.id) { mutableStateOf(false) }

    // Forms fields state
    var editPayee by remember(pending.id) { mutableStateOf(pending.payeeOrMerchant) }
    var editAmountStr by remember(pending.id) { mutableStateOf(pending.amount.toString()) }
    var selectedCategory by remember(pending.id) { mutableStateOf(pending.initialCategory) }
    var editNote by remember(pending.id) { mutableStateOf("") }

    val dateFormater = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
    val dateString = dateFormater.format(Date(pending.dateMillis))

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("pending_sms_${pending.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Bank Sender tag & Date trigger
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = pending.senderAddress,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Text(
                        text = "Received SMS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = dateString,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Body: Compact vs Expanded Toggle click row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (isExpanded) "Review Details" else editPayee,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!isExpanded) {
                            val catStyle = FinanceCategory.getStyleFor(selectedCategory)
                            Box(
                                modifier = Modifier
                                    .background(catStyle.color.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = catStyle.icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(10.dp),
                                        tint = catStyle.color
                                    )
                                    Text(
                                        text = selectedCategory,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = catStyle.color
                                    )
                                }
                            }
                        }
                    }
                    if (!isExpanded) {
                        Text(
                            text = pending.messageBody,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!isExpanded) {
                        Text(
                            text = formatCurrency(editAmountStr.toDoubleOrNull() ?: pending.amount),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (pending.type == "INCOME") Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = { isExpanded = !isExpanded }) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Expand SMS options"
                        )
                    }
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Full SMS Quote block
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "ORIGINAL SMS PAYLOAD:",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "\"${pending.messageBody}\"",
                            style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Fields form editors
                    OutlinedTextField(
                        value = editPayee,
                        onValueChange = { editPayee = it },
                        label = { Text("Vendor / Source") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = editAmountStr,
                            onValueChange = { editAmountStr = it },
                            label = { Text("Amount Paid") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                        OutlinedTextField(
                            value = editNote,
                            onValueChange = { editNote = it },
                            label = { Text("Custom Note") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    }

                    // Category selection list (represented cleanly with horizontal chips)
                    Text(
                        text = "Classify Category",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(FinanceCategory.categories.filter { if (pending.type == "INCOME") !it.isExpense else it.isExpense }) { categoryStyle ->
                            val isSelected = selectedCategory == categoryStyle.name
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCategory = categoryStyle.name },
                                label = { Text(categoryStyle.name) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = categoryStyle.icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = categoryStyle.color.copy(alpha = 0.2f),
                                    selectedLabelColor = categoryStyle.color,
                                    selectedLeadingIconColor = categoryStyle.color
                                )
                            )
                        }
                    }

                    // Staging Actions trigger
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                    ) {
                        OutlinedButton(
                            onClick = onDiscard,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Discard")
                        }
                        Button(
                            onClick = {
                                val finalAmt = editAmountStr.toDoubleOrNull() ?: pending.amount
                                val finalNote = editNote
                                com.example.SmsParserUtility.learnRule(context, editPayee, selectedCategory)
                                android.widget.Toast.makeText(context, "🤖 Parser trained for '$editPayee'!", android.widget.Toast.LENGTH_SHORT).show()
                                onApprove(selectedCategory, editPayee, finalAmt, finalNote)
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Log Transaction")
                        }
                    }
                }
            }

            // Compact mode buttons trigger if not expanded, for fast approvals
            if (!isExpanded) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    FilledTonalButton(
                        onClick = onDiscard,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("Discard", style = MaterialTheme.typography.labelSmall)
                    }
                    Button(
                        onClick = {
                            val finalNote = ""
                            com.example.SmsParserUtility.learnRule(context, editPayee, selectedCategory)
                            android.widget.Toast.makeText(context, "🤖 Parser trained for '$editPayee'!", android.widget.Toast.LENGTH_SHORT).show()
                            onApprove(selectedCategory, editPayee, pending.amount, finalNote)
                        },
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("Approve", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

fun isNotificationServiceEnabled(context: android.content.Context): Boolean {
    val pkgName = context.packageName
    val flat = android.provider.Settings.Secure.getString(
        context.contentResolver,
        "enabled_notification_listeners"
    )
    if (!flat.isNullOrEmpty()) {
        val names = flat.split(":")
        for (name in names) {
            val cn = android.content.ComponentName.unflattenFromString(name)
            if (cn != null && cn.packageName == pkgName) {
                return true
            }
        }
    }
    return false
}

@Composable
fun RcsNotificationTrackerCard(
    onEnableRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("rcs_interceptor_request_card"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(36.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Enable RCS & Google Messages Tracker",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "RCS messages (like SBI Card alerts) do not trigger standard SMS. Enabling Notification access intercepts and auto-detects them in real-time!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onEnableRequest,
                modifier = Modifier.align(Alignment.End),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Enable RCS Tracking", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
