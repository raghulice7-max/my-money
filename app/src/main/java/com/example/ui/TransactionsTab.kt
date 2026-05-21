package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TransactionEntity
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun TransactionsTab(
    transactions: List<TransactionEntity>,
    onAddTransaction: (Double, String, String, String, String, Long) -> Unit,
    onDeleteTransaction: (TransactionEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedTypeFilter by remember { mutableStateOf("ALL") } // "ALL", "INCOME", "EXPENSE", "FUEL"
    var selectedCategoryFilter by remember { mutableStateOf("ALL") }

    // Filter logic
    val filteredTransactions = transactions.filter { tx ->
        val matchesSearch = tx.payeeOrSource.contains(searchQuery, ignoreCase = true) ||
                tx.note.contains(searchQuery, ignoreCase = true) ||
                tx.category.contains(searchQuery, ignoreCase = true)

        val matchesType = when (selectedTypeFilter) {
            "ALL" -> true
            "INCOME" -> tx.type == "INCOME"
            "EXPENSE" -> tx.type == "EXPENSE" && tx.category != "Fuel / Gas"
            "FUEL" -> tx.category == "Fuel / Gas"
            else -> true
        }

        val matchesCategory = if (selectedCategoryFilter == "ALL") true else tx.category == selectedCategoryFilter

        matchesSearch && matchesType && matchesCategory
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("transactions_tab_root")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Search & Filter Header block
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 2.dp,
                shadowElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search description, category, shops...") },
                        leadingIcon = { Icon(Icons.Default.Search, "Search") },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )

                    // Type Chips filter
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("transaction_type_filter_row"),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("ALL", "INCOME", "EXPENSE", "FUEL").forEach { type ->
                            FilterChip(
                                selected = selectedTypeFilter == type,
                                onClick = {
                                    selectedTypeFilter = type
                                    selectedCategoryFilter = "ALL" // Reset category selection when changing main type
                                },
                                label = {
                                    Text(
                                        text = when(type) {
                                            "ALL" -> "All"
                                            "INCOME" -> "Incomes"
                                            "EXPENSE" -> "Expenses"
                                            "FUEL" -> "Fuel"
                                            else -> type
                                        }
                                    )
                                },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }

                    // Category Filter list if not ALL or is custom
                    val availableCategories = listOf("ALL") + FinanceCategory.categories
                        .filter { style ->
                            when (selectedTypeFilter) {
                                "INCOME" -> !style.isExpense
                                "EXPENSE" -> style.isExpense && style.name != "Fuel / Gas"
                                "FUEL" -> style.name == "Fuel / Gas"
                                else -> true
                            }
                        }
                        .map { it.name }

                    if (availableCategories.size > 2) {
                        ScrollableTabRow(
                            selectedTabIndex = availableCategories.indexOf(selectedCategoryFilter).coerceAtLeast(0),
                            edgePadding = 0.dp,
                            containerColor = Color.Transparent,
                            divider = {},
                            indicator = {}
                        ) {
                            availableCategories.forEach { cat ->
                                val isSelected = selectedCategoryFilter == cat
                                Tab(
                                    selected = isSelected,
                                    onClick = { selectedCategoryFilter = cat },
                                    modifier = Modifier.padding(bottom = 8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                            )
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = cat,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Results Log
            if (filteredTransactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Category,
                            contentDescription = "Empty",
                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No matches found",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Try clearing filters or search terms",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .testTag("transaction_list"),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredTransactions, key = { it.id }) { tx ->
                        val style = FinanceCategory.getStyleFor(tx.category)
                        DeletableTransactionItem(
                            transaction = tx,
                            categoryStyle = style,
                            onDelete = { onDeleteTransaction(tx) }
                        )
                    }
                }
            }
        }

        // Add Floating Action Button (FAB)
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("add_transaction_fab"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(Icons.Default.Add, contentDescription = "Log Transaction")
        }
    }

    // Add Transaction Dialog Form
    if (showAddDialog) {
        var txType by remember { mutableStateOf("EXPENSE") } // "EXPENSE", "INCOME"
        var amountText by remember { mutableStateOf("") }
        var payeeText by remember { mutableStateOf("") }
        var selectedCategory by remember { mutableStateOf("") }
        var notesText by remember { mutableStateOf("") }

        // Filter Categories based on Selected Type
        val categoriesForType = FinanceCategory.categories.filter {
            if (txType == "INCOME") !it.isExpense else it.isExpense && it.name != "Fuel / Gas" // Refuel happens in "Fuel Logs" tab
        }

        // Auto selection on switch
        LaunchedEffect(txType) {
            selectedCategory = categoriesForType.firstOrNull()?.name ?: "Other"
        }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                Text(
                    text = "New Transaction",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Transparent),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Type Row Selection
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { txType = "EXPENSE" },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (txType == "EXPENSE") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (txType == "EXPENSE") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Expense")
                        }

                        Button(
                            onClick = { txType = "INCOME" },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (txType == "INCOME") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (txType == "INCOME") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Income")
                        }
                    }

                    // Amount Text Box
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Amount (₹)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )

                    // Payee / Source Text Box
                    OutlinedTextField(
                        value = payeeText,
                        onValueChange = { payeeText = it },
                        label = { Text(if (txType == "INCOME") "Source (From)" else "Payee (To)") },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(if (txType == "INCOME") "Employer, Refund, Sale" else "Store name, Rent provider") },
                        singleLine = true
                    )

                    // Category selection dropdown
                    Column {
                        Text("Category", style = MaterialTheme.typography.labelSmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        var isCategoryMenuExpanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { isCategoryMenuExpanded = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(selectedCategory)
                                    Icon(Icons.Default.KeyboardArrowDown, "Expand")
                                }
                            }
                            DropdownMenu(
                                expanded = isCategoryMenuExpanded,
                                onDismissRequest = { isCategoryMenuExpanded = false }
                            ) {
                                categoriesForType.forEach { style ->
                                    DropdownMenuItem(
                                        text = { Text(style.name) },
                                        onClick = {
                                            selectedCategory = style.name
                                            isCategoryMenuExpanded = false
                                        },
                                        leadingIcon = {
                                            Icon(style.icon, style.name, tint = style.color, modifier = Modifier.size(18.dp))
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Optional Notes text box
                    OutlinedTextField(
                        value = notesText,
                        onValueChange = { notesText = it },
                        label = { Text("Notes / Tags") },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. coffee, project x, online") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = amountText.toDoubleOrNull()
                        val payee = payeeText.ifBlank { if (txType == "INCOME") "Received Funds" else "General Expense" }
                        if (amount != null && amount > 0) {
                            onAddTransaction(
                                amount,
                                txType,
                                selectedCategory,
                                payee,
                                notesText,
                                System.currentTimeMillis()
                            )
                            showAddDialog = false
                        }
                    }
                ) {
                    Text("Add Item")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun DeletableTransactionItem(
    transaction: TransactionEntity,
    categoryStyle: CategoryStyle,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formatter = remember { SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault()) }
    val dateString = formatter.format(Date(transaction.dateMillis))

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(categoryStyle.color.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = categoryStyle.icon,
                    contentDescription = transaction.category,
                    tint = categoryStyle.color,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Text info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.payeeOrSource,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = dateString,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (transaction.note.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = transaction.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Amount and Delete combo
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                val prefix = if (transaction.type == "INCOME") "+" else "-"
                val amtColor = if (transaction.type == "INCOME") Color(0xFF43A047) else MaterialTheme.colorScheme.onSurface
                Text(
                    text = "$prefix${formatCurrency(transaction.amount)}",
                    fontWeight = FontWeight.ExtraBold,
                    color = amtColor,
                    style = MaterialTheme.typography.bodyMedium
                )

                IconButton(
                    onClick = { showDeleteConfirmDialog = true },
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("delete_transaction_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Transaction") },
            text = { Text("Are you sure you want to permanently delete this transaction? This action is irreversible.") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        onDelete()
                        showDeleteConfirmDialog = false
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
