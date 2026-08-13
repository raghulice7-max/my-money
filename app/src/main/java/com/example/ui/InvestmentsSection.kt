package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.InvestmentEntity
import com.example.util.formatInrCurrency
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestmentsSection(
    investments: List<InvestmentEntity>,
    onAddInvestment: (String, String, Double, Double, Long, String) -> Unit,
    onDeleteInvestment: (InvestmentEntity) -> Unit,
    onUpdateInvestment: (InvestmentEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedInvestmentForEdit by remember { mutableStateOf<InvestmentEntity?>(null) }

    // Computations
    val totalInvested = remember(investments) { investments.sumOf { it.investedAmount } }
    val totalCurrentValue = remember(investments) { investments.sumOf { it.currentValue } }
    val absoluteReturn = totalCurrentValue - totalInvested
    val returnPercentage = if (totalInvested > 0) (absoluteReturn / totalInvested) * 100 else 0.0

    // Filter/Type state
    var selectedTypeFilter by remember { mutableStateOf("ALL") }
    var selectedDonutType by remember { mutableStateOf<String?>(null) }
    val investmentTypes = listOf("ALL", "Stocks", "Mutual Funds", "Savings/FD", "Gold", "Crypto", "Investment", "Savings", "Other")

    val filteredInvestments = remember(investments, selectedTypeFilter, selectedDonutType) {
        val baseList = if (selectedTypeFilter == "ALL") {
            investments
        } else {
            investments.filter { it.type == selectedTypeFilter }
        }
        if (selectedDonutType != null) {
            baseList.filter { it.type == selectedDonutType }
        } else {
            baseList
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("investments_section_root"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Portfolio summary card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "Total Portfolio Valuation",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatInrCurrency(totalCurrentValue),
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f))

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Invested Capital",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                            Text(
                                text = formatInrCurrency(totalInvested),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Absolute Returns",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (absoluteReturn >= 0) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = if (absoluteReturn >= 0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = "${if (absoluteReturn >= 0) "+" else ""}${formatInrCurrency(absoluteReturn)} (${String.format(Locale.getDefault(), "%.2f", returnPercentage)}%)",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (absoluteReturn >= 0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }

        // Investment Distribution Donut Chart Card
        if (investments.isNotEmpty() && totalCurrentValue > 0) {
            item {
                val typeValuationTotals = remember(investments) {
                    investments.groupBy { it.type }
                        .mapValues { (_, list) -> list.sumOf { it.currentValue } }
                        .toList()
                        .filter { it.second > 0 }
                        .sortedByDescending { it.second }
                }

                var triggerChartAnim by remember { mutableStateOf(false) }
                LaunchedEffect(investments) {
                    triggerChartAnim = true
                }
                val chartAnimProgress by animateFloatAsState(
                    targetValue = if (triggerChartAnim) 1f else 0f,
                    animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
                    label = "investmentDonutChartAnim"
                )

                Text(
                    text = "Investment Distribution",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
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

                                    typeValuationTotals.forEach { (type, valAmt) ->
                                        val color = getInvestmentTypeColor(type)
                                        val sweepAngle = 360f * (valAmt / totalCurrentValue).toFloat() * chartAnimProgress

                                        val isSelected = selectedDonutType == type
                                        val strokeWidth = if (isSelected) 20.dp.toPx() else 13.dp.toPx()
                                        val drawColor = if (selectedDonutType == null || isSelected) color else color.copy(alpha = 0.25f)

                                        drawArc(
                                            color = drawColor,
                                            startAngle = accumulatedAngle,
                                            sweepAngle = sweepAngle,
                                            useCenter = false,
                                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                                            size = Size(size.width, size.height)
                                        )

                                        accumulatedAngle += sweepAngle
                                    }
                                }

                                // Interactive Center Label
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .clickable { selectedDonutType = null }
                                        .padding(4.dp)
                                ) {
                                    Text(
                                        text = if (selectedDonutType != null) selectedDonutType!! else "Assets",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.widthIn(max = 85.dp)
                                    )
                                    val displayAmount = if (selectedDonutType != null) {
                                        typeValuationTotals.find { it.first == selectedDonutType }?.second ?: 0.0
                                    } else {
                                        totalCurrentValue
                                    }
                                    Text(
                                        text = formatInrCurrency(displayAmount),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.widthIn(max = 85.dp)
                                    )
                                    if (selectedDonutType != null) {
                                        Text(
                                            text = "Reset ✕",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                }
                            }

                            // Interactive Donut Legend List
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                typeValuationTotals.forEach { (type, valAmt) ->
                                    val pct = (valAmt / totalCurrentValue) * 100
                                    val color = getInvestmentTypeColor(type)
                                    val isSelected = selectedDonutType == type

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent)
                                            .clickable {
                                                selectedDonutType = if (isSelected) null else type
                                            }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(9.dp)
                                                .background(color, RoundedCornerShape(2.dp))
                                        )
                                        Text(
                                            text = "$type ${String.format(Locale.getDefault(), "%.1f%%", pct)}",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                                fontSize = 11.sp
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        // Smart Portfolio Mix Advisory Card
                        val mostConcentrated = typeValuationTotals.maxByOrNull { it.second }
                        if (mostConcentrated != null) {
                            val maxPct = (mostConcentrated.second / totalCurrentValue) * 100
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Allocation Tip",
                                    tint = if (maxPct > 70.0) Color(0xFFE65100) else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = when {
                                        maxPct > 70.0 -> "Concentration Risk: ${mostConcentrated.first} accounts for ${String.format(Locale.getDefault(), "%.1f", maxPct)}% of your portfolio. Consider rebalancing to diversify."
                                        typeValuationTotals.size >= 3 -> "Healthy Mix: Your portfolio is well diversified across ${typeValuationTotals.size} different asset classes!"
                                        else -> "Asset Allocation Tip: Try allocating funds to other assets (e.g. Mutual Funds or Gold) to manage risk."
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Action controls
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Holdings",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Button(
                    onClick = { showAddDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("add_investment_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add", fontSize = 14.sp)
                }
            }
        }

        // Type filter chips
        item {
            Box(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                ScrollableRow(
                    items = investmentTypes,
                    selectedItem = selectedTypeFilter,
                    onSelected = { selectedTypeFilter = it }
                )
            }
        }

        // Investments list
        if (filteredInvestments.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 180.dp)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = "No investments",
                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (selectedTypeFilter == "ALL") "No investments recorded yet." else "No investments found in '$selectedTypeFilter'.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (selectedTypeFilter == "ALL") {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap 'Add' to log stocks, mutual funds, gold, etc.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        } else {
            items(filteredInvestments) { investment ->
                InvestmentCard(
                    investment = investment,
                    onEditClick = { selectedInvestmentForEdit = investment },
                    onDeleteClick = { onDeleteInvestment(investment) }
                )
            }
        }
    }

    // Add dialog
    if (showAddDialog) {
        InvestmentFormDialog(
            title = "Log Investment",
            onDismiss = { showAddDialog = false },
            onSubmit = { name, type, invested, current, date, note ->
                onAddInvestment(name, type, invested, current, date, note)
                showAddDialog = false
            }
        )
    }

    // Edit dialog
    selectedInvestmentForEdit?.let { investment ->
        InvestmentFormDialog(
            title = "Edit Investment Details",
            initialInvestment = investment,
            onDismiss = { selectedInvestmentForEdit = null },
            onSubmit = { name, type, invested, current, date, note ->
                onUpdateInvestment(investment.copy(
                    name = name,
                    type = type,
                    investedAmount = invested,
                    currentValue = current,
                    dateMillis = date,
                    note = note
                ))
                selectedInvestmentForEdit = null
            }
        )
    }
}

@Composable
fun InvestmentCard(
    investment: InvestmentEntity,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gainLoss = investment.currentValue - investment.investedAmount
    val gainPercent = if (investment.investedAmount > 0) (gainLoss / investment.investedAmount) * 100 else 0.0

    val badgeColor = when (investment.type) {
        "Stocks" -> Color(0xFFE3F2FD) to Color(0xFF0D47A1) // Blue
        "Mutual Funds" -> Color(0xFFEDE7F6) to Color(0xFF4A148C) // Purple
        "Savings/FD" -> Color(0xFFE8F5E9) to Color(0xFF1B5E20) // Green
        "Gold" -> Color(0xFFFFFDE7) to Color(0xFFF57F17) // Gold/Yellow
        "Crypto" -> Color(0xFFFBE9E7) to Color(0xFFE64A19) // Orange/Red
        else -> Color(0xFFECEFF1) to Color(0xFF37474F) // Gray
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("investment_item_${investment.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = investment.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Surface(
                            shape = CircleShape,
                            color = badgeColor.first,
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Text(
                                text = investment.type,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = badgeColor.second,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (investment.note.isNotBlank()) {
                        Text(
                            text = investment.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onEditClick, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Holding",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(onClick = onDeleteClick, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Holding",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Current Value", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            formatInrCurrency(investment.currentValue),
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Column {
                        Text("Invested Cost", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            formatInrCurrency(investment.investedAmount),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("Gain / Loss", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "${if (gainLoss >= 0) "+" else ""}${formatInrCurrency(gainLoss)} (${String.format(Locale.getDefault(), "%.1f", gainPercent)}%)",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (gainLoss >= 0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScrollableRow(
    items: List<String>,
    selectedItem: String,
    onSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { item ->
            val isSelected = item == selectedItem
            FilterChip(
                selected = isSelected,
                onClick = { onSelected(item) },
                label = { Text(item, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestmentFormDialog(
    title: String,
    initialInvestment: InvestmentEntity? = null,
    onDismiss: () -> Unit,
    onSubmit: (String, String, Double, Double, Long, String) -> Unit
) {
    var name by remember { mutableStateOf(initialInvestment?.name ?: "") }
    var selectedType by remember { mutableStateOf(initialInvestment?.type ?: "Stocks") }
    var investedStr by remember { mutableStateOf(initialInvestment?.investedAmount?.toString() ?: "") }
    var currentStr by remember { mutableStateOf(initialInvestment?.currentValue?.toString() ?: "") }
    var note by remember { mutableStateOf(initialInvestment?.note ?: "") }
    var dateMillis by remember { mutableStateOf(initialInvestment?.dateMillis ?: System.currentTimeMillis()) }

    var expandedTypeDropdown by remember { mutableStateOf(false) }
    val types = listOf("Stocks", "Mutual Funds", "Savings/FD", "Gold", "Crypto", "Investment", "Savings", "Other")

    var nameError by remember { mutableStateOf(false) }
    var investedError by remember { mutableStateOf(false) }
    var currentError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Name
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = it.isBlank()
                    },
                    label = { Text("Holding/Asset Name") },
                    isError = nameError,
                    placeholder = { Text("e.g. Reliance, HDFC, Bitcoin") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("investment_name_input")
                )

                // Type Dropdown Selector
                Box(modifier = Modifier.fillMaxWidth()) {
                    ExposedDropdownMenuBox(
                        expanded = expandedTypeDropdown,
                        onExpandedChange = { expandedTypeDropdown = !expandedTypeDropdown }
                    ) {
                        OutlinedTextField(
                            value = selectedType,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Asset Category") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTypeDropdown) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedTypeDropdown,
                            onDismissRequest = { expandedTypeDropdown = false }
                        ) {
                            types.forEach { t ->
                                DropdownMenuItem(
                                    text = { Text(t) },
                                    onClick = {
                                        selectedType = t
                                        expandedTypeDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Cost and Value Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = investedStr,
                        onValueChange = {
                            investedStr = it
                            investedError = it.toDoubleOrNull() == null
                        },
                        label = { Text("Invested Capital") },
                        isError = investedError,
                        placeholder = { Text("INR") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("investment_cost_input")
                    )

                    OutlinedTextField(
                        value = currentStr,
                        onValueChange = {
                            currentStr = it
                            currentError = it.toDoubleOrNull() == null
                        },
                        label = { Text("Current Value") },
                        isError = currentError,
                        placeholder = { Text("INR") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("investment_val_input")
                    )
                }

                // Remark
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Notes (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val isNameValid = name.isNotBlank()
                    val investedAmt = investedStr.toDoubleOrNull()
                    val currentAmt = currentStr.toDoubleOrNull() ?: investedAmt

                    nameError = !isNameValid
                    investedError = investedAmt == null
                    currentError = currentStr.isNotBlank() && currentAmt == null

                    if (isNameValid && investedAmt != null && currentAmt != null) {
                        onSubmit(name, selectedType, investedAmt, currentAmt, dateMillis, note)
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

fun getInvestmentTypeColor(type: String): Color {
    return when (type) {
        "Stocks" -> Color(0xFF2196F3)        // Bright Blue
        "Mutual Funds" -> Color(0xFF9C27B0)  // Purple
        "Savings/FD" -> Color(0xFF4CAF50)    // Emerald Green
        "Gold" -> Color(0xFFFFEB3B)          // Golden Yellow
        "Crypto" -> Color(0xFFFF5722)        // Sunset Orange
        "Investment" -> Color(0xFF009688)    // Teal
        "Savings" -> Color(0xFFE91E63) // Rose Pink
        else -> Color(0xFF78909C)            // Modern Slate Gray
    }
}
