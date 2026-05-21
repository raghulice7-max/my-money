package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.*
import com.example.ui.theme.MyApplicationTheme

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

                // Active Tab layout: 0 = Dashboard, 1 = Transactions, 2 = Fuel Tracker, 3 = Trends
                var activeTab by remember { mutableStateOf(0) }

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
                                    IconButton(onClick = { activeTab = 0 }) {
                                        Icon(
                                            imageVector = Icons.Default.Dashboard,
                                            contentDescription = "Go Dashboard",
                                            tint = if (activeTab == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            )
                            // A light divider to separate topbar from main viewport
                            Divider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        }
                    },
                    bottomBar = {
                        Column {
                            Divider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
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
                                }
                            )
                            1 -> TransactionsTab(
                                transactions = transactions,
                                onAddTransaction = { amt, type, cat, payee, note, date ->
                                    viewModel.addTransaction(amt, type, cat, payee, note, date)
                                },
                                onDeleteTransaction = { tx -> viewModel.deleteTransaction(tx) }
                            )
                            2 -> FuelTrackerTab(
                                fuelEntries = fuelEntries,
                                onAddFuelEntry = { spent, volume, odo, note, vehicleType ->
                                    viewModel.addFuelEntry(spent, volume, odo, note, vehicleType)
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
        }
    }
}
