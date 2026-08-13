package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import com.example.ui.*
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: FinanceViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.example.SmsParserUtility.initRules(applicationContext)
        enableEdgeToEdge()

        setContent {
            val appTheme by viewModel.appThemeFlow.collectAsStateWithLifecycle()
            val appFontFamily by viewModel.appFontFamilyFlow.collectAsStateWithLifecycle()
            val appFontSize by viewModel.appFontSizeFlow.collectAsStateWithLifecycle()

            MyApplicationTheme(
                appTheme = appTheme,
                appFontFamily = appFontFamily,
                appFontSize = appFontSize
            ) {
                // Collect states from ViewModel
                val transactions by viewModel.transactions.collectAsStateWithLifecycle()
                val budgets by viewModel.budgets.collectAsStateWithLifecycle()
                val fuelEntries by viewModel.fuelEntries.collectAsStateWithLifecycle()
                val pendingTransactions by viewModel.pendingTransactions.collectAsStateWithLifecycle()
                val investments by viewModel.investments.collectAsStateWithLifecycle()
                val goals by viewModel.goals.collectAsStateWithLifecycle()
                val recurringReminders by viewModel.recurringReminders.collectAsStateWithLifecycle()

                // Collect Profile states with lifecycles
                val userName by viewModel.userNameFlow.collectAsStateWithLifecycle()
                val userEmail by viewModel.userEmailFlow.collectAsStateWithLifecycle()
                val userWantsSummary by viewModel.userWantsSummaryFlow.collectAsStateWithLifecycle()

                // Active Tab layout: 0 = Dashboard, 1 = Transactions, 2 = Fuel Tracker, 3 = Trends
                var activeTab by remember { mutableStateOf(0) }
                var showProfileDialog by remember { mutableStateOf(false) }

                val coroutineScope = rememberCoroutineScope()
                val context = LocalContext.current

                // Trigger email summary formulation function
                val triggerEmailSummary: () -> Unit = {
                    coroutineScope.launch {
                        Toast.makeText(context, "Drafting month-end financial summary with AI...", Toast.LENGTH_SHORT).show()
                        val transStr = transactions.joinToString("\n") { tx ->
                            "- ${if (tx.type == "INCOME") "+" else "-"}${tx.amount} INR, Category: ${tx.category}, Payee/Source: ${tx.payeeOrSource}"
                        }
                        val budgetsStr = budgets.joinToString("\n") { bd ->
                            "- ${bd.category}: Limit ${bd.limitAmount} INR"
                        }
                        val draft = GeminiService.generateMonthlySummary(userName, userEmail, transStr, budgetsStr)
                        
                        // Launch email draft Intent
                        try {
                            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:")
                                putExtra(Intent.EXTRA_EMAIL, arrayOf(userEmail))
                                putExtra(Intent.EXTRA_SUBJECT, "Finance Tracker: Monthly Report for $userName")
                                putExtra(Intent.EXTRA_TEXT, draft)
                            }
                            context.startActivity(Intent.createChooser(emailIntent, "Send report via Email"))
                        } catch (ex: Exception) {
                            Toast.makeText(context, "No email client found. Copying draft to clipboard instead.", Toast.LENGTH_LONG).show()
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("Financial Summary", draft)
                            clipboard.setPrimaryClip(clip)
                        }
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        Column {
                            TopAppBar(
                                title = {
                                    Text(
                                        text = when (activeTab) {
                                            0 -> "Dashboard"
                                            1 -> "Ledger"
                                            2 -> "Fuel Economy"
                                            3 -> "Insights"
                                            else -> "Finance Tracker"
                                        }
                                    )
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                actions = {
                                    IconButton(
                                        onClick = { showProfileDialog = true },
                                        modifier = Modifier
                                            .padding(end = 8.dp)
                                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                                            .testTag("user_profile_action_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = "User Profile",
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            )
                            // A light divider to separate topbar from main viewport
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        }
                    },
                    bottomBar = {
                        Column {
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            NavigationBar(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("app_bottom_nav")
                                    .windowInsetsPadding(WindowInsets.navigationBars)
                            ) {
                                NavigationBarItem(
                                    selected = activeTab == 0,
                                    onClick = { activeTab = 0 },
                                    icon = { Icon(Icons.Default.Dashboard, "Dashboard") },
                                    label = { Text("Overview") }
                                )
                                NavigationBarItem(
                                    selected = activeTab == 1,
                                    onClick = { activeTab = 1 },
                                    icon = { Icon(Icons.Default.ReceiptLong, "Transactions") },
                                    label = { Text("Logs") }
                                )
                                NavigationBarItem(
                                    selected = activeTab == 2,
                                    onClick = { activeTab = 2 },
                                    icon = { Icon(Icons.Default.LocalGasStation, "Fuel Tracker") },
                                    label = { Text("Refuel") }
                                )
                                NavigationBarItem(
                                    selected = activeTab == 3,
                                    onClick = { activeTab = 3 },
                                    icon = { Icon(Icons.Default.BarChart, "Trends") },
                                    label = { Text("Trends") }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        AnimatedContent(
                            targetState = activeTab,
                            transitionSpec = {
                                if (targetState > initialState) {
                                    (slideInHorizontally { width -> width / 4 } + fadeIn(animationSpec = tween(220)))
                                        .togetherWith(slideOutHorizontally { width -> -width / 4 } + fadeOut(animationSpec = tween(220)))
                                } else {
                                    (slideInHorizontally { width -> -width / 4 } + fadeIn(animationSpec = tween(220)))
                                        .togetherWith(slideOutHorizontally { width -> width / 4 } + fadeOut(animationSpec = tween(220)))
                                }
                            },
                            label = "tabTransition",
                            modifier = Modifier.fillMaxSize()
                        ) { targetTab ->
                            when (targetTab) {
                                0 -> DashboardTab(
                                    transactions = transactions,
                                    budgets = budgets,
                                    onNavigateToTransactions = { activeTab = 1 },
                                    onNavigateToFuel = { activeTab = 2 },
                                    onSetBudget = { category, limit -> viewModel.setBudget(category, limit) },
                                    pendingTransactions = pendingTransactions,
                                    onApprovePending = { pending, category, payee, amount, note ->
                                        viewModel.approvePendingTransaction(pending, category, payee, amount, note)
                                    },
                                    onDiscardPending = { pending ->
                                        viewModel.discardPendingTransaction(pending)
                                    },
                                    userName = userName,
                                    userEmail = userEmail,
                                    userWantsSummary = userWantsSummary,
                                    onTriggerEmailSummary = triggerEmailSummary,
                                    onSyncSmsInbox = { viewModel.syncSmsInbox(context) },
                                    investments = investments,
                                    goals = goals,
                                    onAddInvestment = { name, type, invested, current, date, note ->
                                        viewModel.addInvestment(name, type, invested, current, date, note)
                                    },
                                    onDeleteInvestment = { viewModel.deleteInvestment(it) },
                                    onUpdateInvestment = { viewModel.updateInvestment(it) },
                                    onAddGoal = { name, target, current, targetDate, category, note ->
                                        viewModel.addGoal(name, target, current, targetDate, category, note)
                                    },
                                    onDeleteGoal = { viewModel.deleteGoal(it) },
                                    onUpdateGoal = { viewModel.updateGoal(it) },
                                    onContributeToGoal = { goal, amt ->
                                        viewModel.contributeToGoal(goal, amt)
                                    },
                                    recurringReminders = recurringReminders,
                                    onAddReminder = { name, amt, day, type, cat, notes ->
                                        viewModel.addRecurringReminder(name, amt, day, type, cat, notes)
                                    },
                                    onDeleteReminder = { viewModel.deleteRecurringReminder(it) },
                                    onUpdateReminder = { viewModel.updateRecurringReminder(it) },
                                    onPayReminder = { reminder, callback ->
                                        viewModel.payRecurringReminder(reminder, callback)
                                    },
                                    onUndoPayReminder = { reminder, paymentId, previousDate ->
                                        viewModel.undoPayRecurringReminder(reminder, paymentId, previousDate)
                                    }
                                )
                                1 -> TransactionsTab(
                                    transactions = transactions,
                                    onAddTransaction = { amt, type, cat, payee, note, date ->
                                        viewModel.addTransaction(amt, type, cat, payee, note, date)
                                    },
                                    onDeleteTransaction = { tx -> viewModel.deleteTransaction(tx) },
                                    onUpdateTransaction = { tx -> viewModel.updateTransaction(tx) },
                                    onClearAllTransactions = { viewModel.clearAllTransactions() }
                                )
                                2 -> FuelTrackerTab(
                                    fuelEntries = fuelEntries,
                                    onAddFuelEntry = { spent, volume, odo, note, vehicleType, dateMs ->
                                        viewModel.addFuelEntry(spent, volume, odo, note, vehicleType, dateMs)
                                    },
                                    onUpdateFuelEntry = { id, spent, volume, odo, note, vehicleType, dateMs ->
                                        viewModel.updateFuelEntry(id, spent, volume, odo, note, vehicleType, dateMs)
                                    },
                                    onDeleteFuelEntry = { log -> viewModel.deleteFuelEntry(log) }
                                )
                                3 -> TrendsTab(
                                    transactions = transactions
                                )
                            }
                        }
                    }
                }

                // Profile Configuration Dialog
                if (showProfileDialog) {
                    var inputName by remember { mutableStateOf(userName) }
                    var inputEmail by remember { mutableStateOf(userEmail) }
                    var wantsSummaryToggle by remember { mutableStateOf(userWantsSummary) }

                    var summaryPreview by remember { mutableStateOf<String?>(null) }
                    var loadingSummary by remember { mutableStateOf(false) }

                    AlertDialog(
                        onDismissRequest = { showProfileDialog = false },
                        title = {
                            Text(
                                "Profile & Settings",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        },
                        text = {
                            var selectedSettingTab by remember { mutableStateOf(0) }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                TabRow(
                                    selectedTabIndex = selectedSettingTab,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp)),
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    indicator = {}
                                ) {
                                    Tab(
                                        selected = selectedSettingTab == 0,
                                        onClick = { selectedSettingTab = 0 },
                                        text = { Text("Profile & AI", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                    )
                                    Tab(
                                        selected = selectedSettingTab == 1,
                                        onClick = { selectedSettingTab = 1 },
                                        text = { Text("App Settings", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                    )
                                    Tab(
                                        selected = selectedSettingTab == 2,
                                        onClick = { selectedSettingTab = 2 },
                                        text = { Text("Backup & Cloud", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                if (selectedSettingTab == 0) {
                                    OutlinedTextField(
                                        value = inputName,
                                        onValueChange = { inputName = it },
                                        label = { Text("Explorer Name") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth().testTag("profile_name_input")
                                    )

                                    OutlinedTextField(
                                        value = inputEmail,
                                        onValueChange = { inputEmail = it },
                                        label = { Text("Notification Email ID") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth().testTag("profile_email_input")
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                "Month-End Summary",
                                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                                            )
                                            Text(
                                                "Auto-prepare formatted email summary draft",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Switch(
                                            checked = wantsSummaryToggle,
                                            onCheckedChange = { wantsSummaryToggle = it },
                                            modifier = Modifier.testTag("profile_summary_switch")
                                        )
                                    }

                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            "A.I. Month-End Report",
                                            style = MaterialTheme.typography.labelLarge.copy(color = MaterialTheme.colorScheme.primary)
                                        )
                                        Text(
                                            "Ask Gemini AI to draft, analyze, and structures your month-end transaction report now.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        if (loadingSummary) {
                                            Column(
                                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                CircularProgressIndicator()
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text("AI analyzing transaction history...", style = MaterialTheme.typography.bodySmall)
                                            }
                                        } else if (summaryPreview != null) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                shape = MaterialTheme.shapes.medium,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(modifier = Modifier.padding(12.dp)) {
                                                    Text(
                                                        summaryPreview ?: "",
                                                        style = MaterialTheme.typography.bodySmall
                                                    )
                                                    Spacer(modifier = Modifier.height(12.dp))
                                                    Button(
                                                        onClick = {
                                                            triggerEmailSummary()
                                                        },
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Text("Directly Send as Email")
                                                    }
                                                }
                                            }
                                        } else {
                                            Button(
                                                onClick = {
                                                    coroutineScope.launch {
                                                        loadingSummary = true
                                                        val transStr = transactions.joinToString("\n") { tx ->
                                                            "- ${if (tx.type == "INCOME") "INCOME" else "EXPENSE"}: ${tx.amount} INR, Category: ${tx.category}, Payee: ${tx.payeeOrSource}"
                                                        }
                                                        val budgetsStr = budgets.joinToString("\n") { bd ->
                                                            "- ${bd.category}: Limit ${bd.limitAmount} INR"
                                                        }
                                                        summaryPreview = GeminiService.generateMonthlySummary(
                                                            inputName,
                                                            inputEmail,
                                                            transStr,
                                                            budgetsStr
                                                        )
                                                        loadingSummary = false
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                                modifier = Modifier.fillMaxWidth().testTag("ai_generate_summary_button")
                                            ) {
                                                Text("Preview A.I. Report Draft")
                                            }
                                        }
                                    }
                                } else if (selectedSettingTab == 1) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Text(
                                            "Appearance Theme",
                                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            val themes = listOf(
                                                "SYSTEM" to "System",
                                                "LIGHT" to "Light",
                                                "DARK" to "Dark"
                                            )
                                            themes.forEach { (mode, label) ->
                                                val isSelected = appTheme == mode
                                                Surface(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clickable { viewModel.updateTheme(mode) }
                                                        .border(
                                                            width = if (isSelected) 2.dp else 1.dp,
                                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                                            shape = RoundedCornerShape(8.dp)
                                                        ),
                                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = label,
                                                            fontSize = 11.sp,
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(
                                            "Typography Font Style",
                                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            val fonts = listOf(
                                                "DEFAULT" to "Default",
                                                "SANS_SERIF" to "Sans",
                                                "SERIF" to "Serif",
                                                "MONOSPACE" to "Mono"
                                            )
                                            fonts.forEach { (key, label) ->
                                                val isSelected = appFontFamily == key
                                                Surface(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clickable { viewModel.updateFontFamily(key) }
                                                        .border(
                                                            width = if (isSelected) 2.dp else 1.dp,
                                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                                            shape = RoundedCornerShape(8.dp)
                                                        ),
                                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 2.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = label,
                                                            fontSize = 11.sp,
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(
                                            "Font Size density",
                                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            val sizes = listOf(
                                                "SMALL" to "Small",
                                                "NORMAL" to "Medium",
                                                "LARGE" to "Large"
                                            )
                                            sizes.forEach { (key, label) ->
                                                val isSelected = appFontSize == key
                                                Surface(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clickable { viewModel.updateFontSize(key) }
                                                        .border(
                                                            width = if (isSelected) 2.dp else 1.dp,
                                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                                            shape = RoundedCornerShape(8.dp)
                                                        ),
                                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = label,
                                                            fontSize = 11.sp,
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    // Backup & Cloud sync tab (selectedSettingTab == 2)
                                    var backupJsonText by remember { mutableStateOf("") }
                                    var importJsonInputValue by remember { mutableStateOf("") }
                                    var isImportError by remember { mutableStateOf(false) }

                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Text(
                                            "Cloud Sync & Backup Logs",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary
                                        )

                                        Card(
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                                            ),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = androidx.compose.material.icons.Icons.Default.Cloud,
                                                    contentDescription = "Cloud Info",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                                Text(
                                                    text = "Local application data is isolated and deleted by Android when you uninstall or reinstall. Use backups below to keep your data safe, copy, or share it anywhere.",
                                                    fontSize = 11.sp,
                                                    lineHeight = 15.sp,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }
                                        }

                                        // Section 1: Export
                                        Text(
                                            "Option A: Export & Save Local Backup",
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.secondary
                                        )

                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = {
                                                     coroutineScope.launch {
                                                         val json = viewModel.exportBackupJson()
                                                         backupJsonText = json
                                                         // Copy to Clipboard
                                                         val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                                         val clip = android.content.ClipData.newPlainText("Finance Tracker Backup", json)
                                                         clipboard.setPrimaryClip(clip)
                                                         Toast.makeText(context, "✅ Backup JSON copied to clipboard!", Toast.LENGTH_LONG).show()
                                                     }
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                            ) {
                                                Icon(
                                                    imageVector = androidx.compose.material.icons.Icons.Default.ContentCopy,
                                                    contentDescription = "Copy",
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Generate Backup & Copy")
                                            }

                                            if (backupJsonText.isNotEmpty()) {
                                                OutlinedTextField(
                                                    value = backupJsonText,
                                                    onValueChange = {},
                                                    readOnly = true,
                                                    label = { Text("Backup Payload Snapshot") },
                                                    modifier = Modifier.fillMaxWidth().height(100.dp),
                                                    maxLines = 4,
                                                    textStyle = androidx.compose.ui.text.TextStyle(
                                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                                        fontSize = 10.sp
                                                    )
                                                )
                                                Text(
                                                    text = "Saved snapshot! Paste this text in your email draft, cloud document, or notes app to backup safely.",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                        // Section 2: Import
                                        Text(
                                            "Option B: Import & Recover Data",
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.secondary
                                        )

                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = importJsonInputValue,
                                                onValueChange = {
                                                    importJsonInputValue = it
                                                    isImportError = false
                                                },
                                                label = { Text("Paste Backup JSON payload") },
                                                placeholder = { Text("Paste code starting with {\"budgets\":...") },
                                                modifier = Modifier.fillMaxWidth().height(100.dp),
                                                isError = isImportError,
                                                textStyle = androidx.compose.ui.text.TextStyle(
                                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                                    fontSize = 11.sp
                                                )
                                            )

                                            if (isImportError) {
                                                Text(
                                                    text = "Invalid format. Ensure you copy/pasted the complete payload exactly.",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.error
                                                )
                                            }

                                            Button(
                                                onClick = {
                                                    if (importJsonInputValue.trim().isEmpty()) {
                                                        isImportError = true
                                                        return@Button
                                                    }
                                                    coroutineScope.launch {
                                                        val success = viewModel.importBackupJson(importJsonInputValue.trim())
                                                        if (success) {
                                                            Toast.makeText(context, "🎉 Superb! All transactions, goals, and history recovered perfectly!", Toast.LENGTH_LONG).show()
                                                             importJsonInputValue = ""
                                                             showProfileDialog = false
                                                        } else {
                                                            isImportError = true
                                                            Toast.makeText(context, "❌ Recovery failed. Check string formats.", Toast.LENGTH_LONG).show()
                                                        }
                                                    }
                                                },
                                                modifier = Modifier.fillMaxWidth().testTag("restore_backup_button"),
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                             ) {
                                                 Icon(
                                                     imageVector = androidx.compose.material.icons.Icons.Default.CloudDownload,
                                                     contentDescription = "Restore",
                                                     modifier = Modifier.size(18.dp)
                                                 )
                                                 Spacer(modifier = Modifier.width(6.dp))
                                                 Text("Restore & Replace Records")
                                             }
                                         }
                                     }
                                 }
                             }
                         },
                         confirmButton = {
                            Button(
                                onClick = {
                                    viewModel.updateProfile(inputName, inputEmail, wantsSummaryToggle)
                                    showProfileDialog = false
                                },
                                modifier = Modifier.testTag("save_profile_button")
                            ) {
                                Text("Save Changes")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showProfileDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }
        }
    }

    private val smsReceiver = SmsReceiver()

    override fun onStart() {
        super.onStart()
        try {
            val filter = android.content.IntentFilter("android.provider.Telephony.SMS_RECEIVED").apply {
                priority = 999
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(smsReceiver, filter, android.content.Context.RECEIVER_EXPORTED)
            } else {
                registerReceiver(smsReceiver, filter)
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error registering SMS receiver dynamically", e)
        }
    }

    override fun onStop() {
        super.onStop()
        try {
            unregisterReceiver(smsReceiver)
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error unregistering SMS receiver", e)
        }
    }
}
