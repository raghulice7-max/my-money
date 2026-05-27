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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.*
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: FinanceViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                // Collect states from ViewModel
                val transactions by viewModel.transactions.collectAsStateWithLifecycle()
                val budgets by viewModel.budgets.collectAsStateWithLifecycle()
                val fuelEntries by viewModel.fuelEntries.collectAsStateWithLifecycle()
                val pendingTransactions by viewModel.pendingTransactions.collectAsStateWithLifecycle()

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
                        when (activeTab) {
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
                                onTriggerEmailSummary = triggerEmailSummary
                            )
                            1 -> TransactionsTab(
                                transactions = transactions,
                                onAddTransaction = { amt, type, cat, payee, note, date ->
                                    viewModel.addTransaction(amt, type, cat, payee, note, date)
                                },
                                onDeleteTransaction = { tx -> viewModel.deleteTransaction(tx) },
                                onUpdateTransaction = { tx -> viewModel.updateTransaction(tx) }
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
                                "User Profile",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        },
                        text = {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
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
}
