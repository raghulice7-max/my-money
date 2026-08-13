package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import com.example.ui.theme.bounceClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TransactionsTab(
    transactions: List<TransactionEntity>,
    onAddTransaction: (Double, String, String, String, String, Long) -> Unit,
    onDeleteTransaction: (TransactionEntity) -> Unit,
    onUpdateTransaction: (TransactionEntity) -> Unit,
    onClearAllTransactions: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var showClearAllDialog by remember { mutableStateOf(false) }
    var transactionToEdit by remember { mutableStateOf<TransactionEntity?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedTypeFilter by remember { mutableStateOf("ALL") } // "ALL", "INCOME", "EXPENSE", "FUEL"
    var selectedCategoryFilter by remember { mutableStateOf("ALL") }
    var selectedMonthFilter by remember { mutableStateOf("ALL") }

    val monthsFormatter = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
    val monthFilterKeyFormatter = remember { SimpleDateFormat("yyyy-MM", Locale.getDefault()) }

    val availableMonths = remember(transactions) {
        val uniqueMonthKeys = transactions.map { tx ->
            val date = Date(tx.dateMillis)
            monthFilterKeyFormatter.format(date) to monthsFormatter.format(date)
        }
        val distinctOrdered = uniqueMonthKeys.distinctBy { it.first }.sortedByDescending { it.first }
        listOf("ALL" to "All Months") + distinctOrdered
    }

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

        val matchesMonth = if (selectedMonthFilter == "ALL") {
            true
        } else {
            monthFilterKeyFormatter.format(Date(tx.dateMillis)) == selectedMonthFilter
        }

        matchesSearch && matchesType && matchesCategory && matchesMonth
    }

    val groupedTransactions = remember(filteredTransactions) {
        filteredTransactions.groupBy { tx ->
            val cal = Calendar.getInstance().apply { timeInMillis = tx.dateMillis }
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis
        }.toList().sortedByDescending { it.first }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("transactions_tab_root")
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("transaction_list"),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            // Search & Filter Header block
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    tonalElevation = 2.dp,
                    shadowElevation = 1.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Logged History (${transactions.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (transactions.isNotEmpty()) {
                                TextButton(
                                    onClick = { showClearAllDialog = true },
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    ),
                                    modifier = Modifier.testTag("delete_all_logs_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteSweep,
                                        contentDescription = "Delete All Logs",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Delete All", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }

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

                        // Month Filter Chips Row
                        if (availableMonths.size > 2) {
                            ScrollableTabRow(
                                selectedTabIndex = availableMonths.indexOfFirst { it.first == selectedMonthFilter }.coerceAtLeast(0),
                                edgePadding = 0.dp,
                                containerColor = Color.Transparent,
                                divider = {},
                                indicator = {}
                            ) {
                                availableMonths.forEach { (key, display) ->
                                    val isSelected = selectedMonthFilter == key
                                    Tab(
                                        selected = isSelected,
                                        onClick = { selectedMonthFilter = key },
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(
                                                    if (isSelected) MaterialTheme.colorScheme.secondaryContainer
                                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                                )
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = display,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer
                                                else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Results Log
            if (filteredTransactions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 280.dp),
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
                }
            } else {
                groupedTransactions.forEach { (dateMillis, txList) ->
                    item(key = "header_$dateMillis") {
                        DateGroupHeader(dateMillis = dateMillis)
                    }

                    itemsIndexed(txList, key = { _, tx -> "${tx.id}_${tx.dateMillis}_${tx.amount}" }) { index, tx ->
                        val style = FinanceCategory.getStyleFor(tx.category)
                        DeletableTransactionItem(
                            transaction = tx,
                            categoryStyle = style,
                            onEdit = { transactionToEdit = tx },
                            onDelete = { onDeleteTransaction(tx) }
                        )

                        if (index < txList.size - 1) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 76.dp, end = 16.dp),
                                thickness = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )
                        }
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

    // Edit Transaction Dialog Form
    if (transactionToEdit != null) {
        val editingTx = transactionToEdit!!
        var txType by remember(editingTx) { mutableStateOf(editingTx.type) }
        var amountText by remember(editingTx) { mutableStateOf(editingTx.amount.toString()) }
        var payeeText by remember(editingTx) { mutableStateOf(editingTx.payeeOrSource) }
        var selectedCategory by remember(editingTx) { mutableStateOf(editingTx.category) }
        
        var paymentMethod by remember(editingTx) {
            mutableStateOf(
                when {
                    editingTx.note.startsWith("[Card] ") || editingTx.note == "[Card]" -> "Card"
                    editingTx.note.startsWith("[Wallet] ") || editingTx.note == "[Wallet]" -> "Wallet"
                    editingTx.category == "Salary" || editingTx.category == "Investment" || editingTx.category == "Savings" || editingTx.category == "Business & Freelance" -> "Card"
                    editingTx.category == "Snacks" || editingTx.category == "Groceries" || editingTx.category == "Food & Dining" || editingTx.category == "Other" -> "Wallet"
                    else -> "Card"
                }
            )
        }

        var notesText by remember(editingTx) {
            mutableStateOf(
                when {
                    editingTx.note.startsWith("[Card] ") -> editingTx.note.removePrefix("[Card] ")
                    editingTx.note.startsWith("[Wallet] ") -> editingTx.note.removePrefix("[Wallet] ")
                    editingTx.note == "[Card]" || editingTx.note == "[Wallet]" -> ""
                    else -> editingTx.note
                }
            )
        }

        var selectedDateMillis by remember(editingTx) { mutableStateOf(editingTx.dateMillis) }
        var showDatePicker by remember { mutableStateOf(false) }
        var showTimePicker by remember { mutableStateOf(false) }

        val sdf = remember { SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()) }
        val dateText = sdf.format(Date(selectedDateMillis))

        // Filter Categories based on Selected Type
        val categoriesForType = FinanceCategory.categories.filter {
            if (txType == "INCOME") !it.isExpense else it.isExpense && it.name != "Fuel / Gas"
        }

        // Auto selection on type switch
        LaunchedEffect(txType) {
            val isValid = categoriesForType.any { it.name == selectedCategory }
            if (!isValid) {
                selectedCategory = categoriesForType.firstOrNull()?.name ?: "Other"
            }
        }

        AlertDialog(
            onDismissRequest = { transactionToEdit = null },
            title = {
                Text(
                    text = "Edit Transaction",
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

                    // Clickable calendar date picker matching Fuel Tracker style
                    if (showDatePicker) {
                        CustomCalendarDatePicker(
                            selectedDateMillis = selectedDateMillis,
                            onDateSelected = { selected ->
                                val prevCal = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
                                val newCal = Calendar.getInstance().apply {
                                    timeInMillis = selected
                                    set(Calendar.HOUR_OF_DAY, prevCal.get(Calendar.HOUR_OF_DAY))
                                    set(Calendar.MINUTE, prevCal.get(Calendar.MINUTE))
                                    set(Calendar.SECOND, prevCal.get(Calendar.SECOND))
                                    set(Calendar.MILLISECOND, prevCal.get(Calendar.MILLISECOND))
                                }
                                selectedDateMillis = newCal.timeInMillis
                                showDatePicker = false
                            },
                            onDismiss = { showDatePicker = false }
                        )
                    }

                    if (showTimePicker) {
                        CustomTimePickerDialog(
                            initialTimeMillis = selectedDateMillis,
                            onTimeSelected = { h, m ->
                                val updatedCal = Calendar.getInstance().apply {
                                    timeInMillis = selectedDateMillis
                                    set(Calendar.HOUR_OF_DAY, h)
                                    set(Calendar.MINUTE, m)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }
                                selectedDateMillis = updatedCal.timeInMillis
                                showTimePicker = false
                            },
                            onDismiss = { showTimePicker = false }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Date Column
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = "Date",
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Date",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .clickable { showDatePicker = true }
                                    .clip(RoundedCornerShape(8.dp)),
                                color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                                border = CardDefaults.outlinedCardBorder()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = dateText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = "Select Date",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        // Time Column
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = "Time",
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Time",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            val timeFormatter = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
                            val timeText = timeFormatter.format(Date(selectedDateMillis))
                            
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .clickable { showTimePicker = true }
                                    .clip(RoundedCornerShape(8.dp)),
                                color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                                border = CardDefaults.outlinedCardBorder()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = timeText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = "Select Time",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Category selection dropdown
                    var isCategoryMenuExpanded by remember { mutableStateOf(false) }
                    Column {
                        Text(
                            text = "Category",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box {
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

                    // Payment Method Row Selection
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Payment Method",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            listOf("Card", "Wallet").forEach { method ->
                                val isSelected = paymentMethod == method
                                val icon = if (method == "Card") Icons.Default.CreditCard else Icons.Default.AccountBalanceWallet
                                val activeColor = if (method == "Card") Color(0xFFEF5350) else Color(0xFFFFA726)
                                
                                OutlinedCard(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .clickable { paymentMethod = method },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.outlinedCardColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                    ),
                                    border = CardDefaults.outlinedCardBorder().copy(
                                        brush = androidx.compose.ui.graphics.SolidColor(
                                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                        )
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = method,
                                            tint = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = method,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Notes text box
                    OutlinedTextField(
                        value = notesText,
                        onValueChange = { notesText = it },
                        label = { Text("Notes / Tags") },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. coffee, online") },
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
                            val finalNote = if (notesText.isBlank()) "[$paymentMethod]" else "[$paymentMethod] $notesText"
                            onUpdateTransaction(
                                editingTx.copy(
                                    amount = amount,
                                    type = txType,
                                    category = selectedCategory,
                                    payeeOrSource = payee,
                                    note = finalNote,
                                    dateMillis = selectedDateMillis
                                )
                            )
                            transactionToEdit = null
                        }
                    }
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { transactionToEdit = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add Transaction Dialog Form
    if (showAddDialog) {
        var txType by remember { mutableStateOf("EXPENSE") } // "EXPENSE", "INCOME"
        var amountText by remember { mutableStateOf("") }
        var payeeText by remember { mutableStateOf("") }
        var selectedCategory by remember { mutableStateOf("") }
        var paymentMethod by remember { mutableStateOf("Card") }
        var notesText by remember { mutableStateOf("") }
        var selectedDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
        var showDatePicker by remember { mutableStateOf(false) }
        var showTimePicker by remember { mutableStateOf(false) }

        val sdf = remember { SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()) }
        val dateText = sdf.format(Date(selectedDateMillis))

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
                    // Auto-Fill pasting SMS helper
                    var showPasteBox by remember { mutableStateOf(false) }
                    var pasteText by remember { mutableStateOf("") }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showPasteBox = !showPasteBox }
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sms,
                                    contentDescription = "SMS icon",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Have SMS / Bank message?",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                text = if (showPasteBox) "Hide" else "Auto-Fill with SMS",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }

                        if (showPasteBox) {
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = pasteText,
                                onValueChange = { pasteText = it },
                                label = { Text("Paste Bank Message Body") },
                                placeholder = { Text("e.g. Rs.55.00 spent on your SBI Credit Card...") },
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 3,
                                trailingIcon = {
                                    if (pasteText.isNotBlank()) {
                                        IconButton(
                                            onClick = {
                                                val parsed = com.example.SmsReceiver().parseSms(pasteText)
                                                if (parsed != null) {
                                                    txType = parsed.type
                                                    amountText = parsed.amount.toString()
                                                    payeeText = parsed.payeeOrMerchant
                                                    selectedCategory = parsed.guessedCategory
                                                    pasteText = ""
                                                    showPasteBox = false
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AutoAwesome,
                                                contentDescription = "Apply Parse",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

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

                    // Clickable calendar date picker matching Fuel Tracker style
                    if (showDatePicker) {
                        CustomCalendarDatePicker(
                            selectedDateMillis = selectedDateMillis,
                            onDateSelected = { selected ->
                                val prevCal = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
                                val newCal = Calendar.getInstance().apply {
                                    timeInMillis = selected
                                    set(Calendar.HOUR_OF_DAY, prevCal.get(Calendar.HOUR_OF_DAY))
                                    set(Calendar.MINUTE, prevCal.get(Calendar.MINUTE))
                                    set(Calendar.SECOND, prevCal.get(Calendar.SECOND))
                                    set(Calendar.MILLISECOND, prevCal.get(Calendar.MILLISECOND))
                                }
                                selectedDateMillis = newCal.timeInMillis
                                showDatePicker = false
                            },
                            onDismiss = { showDatePicker = false }
                        )
                    }

                    if (showTimePicker) {
                        CustomTimePickerDialog(
                            initialTimeMillis = selectedDateMillis,
                            onTimeSelected = { h, m ->
                                val updatedCal = Calendar.getInstance().apply {
                                    timeInMillis = selectedDateMillis
                                    set(Calendar.HOUR_OF_DAY, h)
                                    set(Calendar.MINUTE, m)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }
                                selectedDateMillis = updatedCal.timeInMillis
                                showTimePicker = false
                            },
                            onDismiss = { showTimePicker = false }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Date Column
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = "Date",
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Date",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .clickable { showDatePicker = true }
                                    .clip(RoundedCornerShape(8.dp)),
                                color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                                border = CardDefaults.outlinedCardBorder()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = dateText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = "Select Date",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        // Time Column
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = "Time",
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Time",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            val timeFormatter = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
                            val timeText = timeFormatter.format(Date(selectedDateMillis))
                            
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .clickable { showTimePicker = true }
                                    .clip(RoundedCornerShape(8.dp)),
                                color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                                border = CardDefaults.outlinedCardBorder()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = timeText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = "Select Time",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

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

                    // Payment Method Row Selection
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Payment Method",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            listOf("Card", "Wallet").forEach { method ->
                                val isSelected = paymentMethod == method
                                val icon = if (method == "Card") Icons.Default.CreditCard else Icons.Default.AccountBalanceWallet
                                val activeColor = if (method == "Card") Color(0xFFEF5350) else Color(0xFFFFA726)
                                
                                OutlinedCard(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .clickable { paymentMethod = method },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.outlinedCardColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                    ),
                                    border = CardDefaults.outlinedCardBorder().copy(
                                        brush = androidx.compose.ui.graphics.SolidColor(
                                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                        )
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = method,
                                            tint = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = method,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
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
                            val finalNote = if (notesText.isBlank()) "[$paymentMethod]" else "[$paymentMethod] $notesText"
                            onAddTransaction(
                                amount,
                                txType,
                                selectedCategory,
                                payee,
                                finalNote,
                                selectedDateMillis
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

    // Delete All Confirmation Dialog
    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = { Text("Delete All Logs", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to permanently delete ALL logged transactions? This will erase all history and is completely irreversible.") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        onClearAllTransactions()
                        showClearAllDialog = false
                    }
                ) {
                    Text("Delete All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) {
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
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formatter = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val timeString = formatter.format(Date(transaction.dateMillis))

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // Parse the paymentMethod and cleanNote from note
    val (paymentMethod, cleanNote) = remember(transaction.note) {
        when {
            transaction.note.startsWith("[Card] ") -> "Card" to transaction.note.removePrefix("[Card] ")
            transaction.note.startsWith("[Wallet] ") -> "Wallet" to transaction.note.removePrefix("[Wallet] ")
            transaction.note == "[Card]" -> "Card" to ""
            transaction.note == "[Wallet]" -> "Wallet" to ""
            // Default heuristics:
            transaction.category == "Salary" || transaction.category == "Investment" || transaction.category == "Savings" || transaction.category == "Business & Freelance" -> "Card" to transaction.note
            transaction.category == "Snacks" || transaction.category == "Groceries" || transaction.category == "Food & Dining" || transaction.category == "Other" -> "Wallet" to transaction.note
            else -> "Card" to transaction.note
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .bounceClickable { onEdit() }
            .padding(vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon - solid primary category color background with clean white icon inside
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(categoryStyle.color, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = categoryStyle.icon,
                    contentDescription = transaction.category,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Text info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.payeeOrSource,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Payment Method Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = if (paymentMethod == "Card") Icons.Default.CreditCard else Icons.Default.AccountBalanceWallet,
                            contentDescription = paymentMethod,
                            tint = if (paymentMethod == "Card") Color(0xFFEF5350) else Color(0xFFFFA726),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = paymentMethod,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Details text with bullet spacing separator
                    val detailsText = prepDetailsString(timeString, cleanNote)
                    Text(
                        text = detailsText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Right column: Amount & Action options
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                val prefix = if (transaction.type == "INCOME") "+" else "-"
                val amtColor = if (transaction.type == "INCOME") Color(0xFF43A047) else Color(0xFFE53935)
                
                Text(
                    text = "$prefix${formatCurrency(transaction.amount)}",
                    fontWeight = FontWeight.ExtraBold,
                    color = amtColor,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp)
                )

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = { onEdit() },
                    modifier = Modifier.size(32.dp).testTag("edit_transaction_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = { showDeleteConfirmDialog = true },
                    modifier = Modifier.size(32.dp).testTag("delete_transaction_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
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

@Composable
fun DateGroupHeader(dateMillis: Long, modifier: Modifier = Modifier) {
    val headerFormatter = remember { SimpleDateFormat("MMM dd, EEEE", Locale.getDefault()) }
    val displayText = headerFormatter.format(Date(dateMillis))

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 18.dp, start = 16.dp, end = 16.dp, bottom = 4.dp)
    ) {
        Text(
            text = displayText,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.padding(bottom = 6.dp)
        )
        HorizontalDivider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
        )
    }
}

fun prepDetailsString(time: String, note: String): String {
    return if (note.isNotBlank()) {
        "•  $time  •  $note"
    } else {
        "•  $time"
    }
}


