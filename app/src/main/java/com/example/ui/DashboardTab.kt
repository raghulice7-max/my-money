package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BudgetEntity
import com.example.data.TransactionEntity
import com.example.data.PendingSmsTransactionEntity
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
    onDiscardPending: (PendingSmsTransactionEntity) -> Unit = {}
) {
    var showBudgetDialog by remember { mutableStateOf(false) }

    // SMS permission states
    val context = LocalContext.current
    var hasSmsPermission by remember {
        mutableStateOf(
            context.checkSelfPermission(android.Manifest.permission.RECEIVE_SMS) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    val smsPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasSmsPermission = isGranted
    }

    // Computations
    val totalIncome = transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
    val totalExpense = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    val netBalance = totalIncome - totalExpense

    val totalBudgetLimit = budgets.find { it.category == "TOTAL" }?.limitAmount ?: 1000.0
    val budgetProgress = if (totalBudgetLimit > 0) (totalExpense / totalBudgetLimit).coerceIn(0.0, 1.0) else 0.0

    val recentTransactions = transactions.take(5)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_tab_root"),
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
                        text = "Hello Explorer!",
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
                            text = "Net Balance",
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

        // Pending Transaction Reviews Section
        if (pendingTransactions.isNotEmpty() || !hasSmsPermission) {
            item {
                Text(
                    text = "SMS Auto-Track Staging",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            if (!hasSmsPermission) {
                item {
                    SmsPermissionRequestCard(
                        onGrantRequest = {
                            smsPermissionLauncher.launch(android.Manifest.permission.RECEIVE_SMS)
                        }
                    )
                }
            }

            if (pendingTransactions.isNotEmpty()) {
                items(pendingTransactions, key = { it.id }) { pending ->
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

        // Budget Progress Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Monthly Expenses Budget",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Limit: ${formatCurrency(totalBudgetLimit)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { showBudgetDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Budget Goal",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Progress indicators
                    LinearProgressIndicator(
                        progress = { budgetProgress.toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .testTag("budget_progress_bar"),
                        color = if (totalExpense > totalBudgetLimit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${(budgetProgress * 100).toInt()}% spent",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (totalExpense > totalBudgetLimit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = "${formatCurrency(totalExpense)} of ${formatCurrency(totalBudgetLimit)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Overspent Warning
                    if (totalExpense > totalBudgetLimit) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = "Warning",
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Alert: You've exceeded your spending budget!",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            }
        }

        // Quick Category Budgets Breakdown
        val categoryBudgets = budgets.filter { it.category != "TOTAL" }
        if (categoryBudgets.isNotEmpty()) {
            item {
                Text(
                    text = "Category Budgets",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                )
            }

            items(categoryBudgets) { budget ->
                val spent = transactions
                    .filter { it.type == "EXPENSE" && it.category == budget.category }
                    .sumOf { it.amount }
                val prog = if (budget.limitAmount > 0) (spent / budget.limitAmount).coerceIn(0.0, 1.0) else 0.0
                val style = FinanceCategory.getStyleFor(budget.category)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(style.color.copy(alpha = 0.15f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = style.icon,
                                        contentDescription = budget.category,
                                        tint = style.color,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = budget.category,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }

                            Text(
                                text = "${formatCurrency(spent)} / ${formatCurrency(budget.limitAmount)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = { prog.toFloat() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = style.color,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
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

        // Quick Refuel Redirect Callout card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToFuel() },
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
    }

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
}

@Composable
fun TransactionItem(
    transaction: TransactionEntity,
    categoryStyle: CategoryStyle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formatter = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    val dateString = formatter.format(Date(transaction.dateMillis))

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(categoryStyle.color.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = categoryStyle.icon,
                    contentDescription = transaction.category,
                    tint = categoryStyle.color,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Text
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.payeeOrSource,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = transaction.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = " • $dateString",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Amount
            val prefix = if (transaction.type == "INCOME") "+" else "-"
            val amtColor = if (transaction.type == "INCOME") Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface
            Text(
                text = "$prefix${formatCurrency(transaction.amount)}",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = amtColor
            )
        }
    }
}

fun formatCurrency(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    return format.format(amount)
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
    var isExpanded by remember { mutableStateOf(false) }

    // Forms fields state
    var editPayee by remember { mutableStateOf(pending.payeeOrMerchant) }
    var editAmountStr by remember { mutableStateOf(pending.amount.toString()) }
    var selectedCategory by remember { mutableStateOf(pending.initialCategory) }
    var editNote by remember { mutableStateOf("") }

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
                                val finalNote = editNote.ifBlank { "SMS Approved: ${pending.messageBody}" }
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
                            val finalNote = "SMS Approved: ${pending.messageBody}"
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
