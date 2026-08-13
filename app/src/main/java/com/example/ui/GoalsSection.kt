package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GoalEntity
import com.example.util.formatInrCurrency
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsSection(
    goals: List<GoalEntity>,
    onAddGoal: (String, Double, Double, Long, String, String) -> Unit,
    onDeleteGoal: (GoalEntity) -> Unit,
    onUpdateGoal: (GoalEntity) -> Unit,
    onContributeToGoal: (GoalEntity, Double) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedGoalForEdit by remember { mutableStateOf<GoalEntity?>(null) }
    var selectedGoalForContribution by remember { mutableStateOf<GoalEntity?>(null) }
    var milestoneBoostGoalAndAmount by remember { mutableStateOf<Pair<GoalEntity, Double>?>(null) }

    // Computations
    val totalTarget = remember(goals) { goals.sumOf { it.targetAmount } }
    val totalSaved = remember(goals) { goals.sumOf { it.currentAmount } }
    val overallProgress = if (totalTarget > 0) (totalSaved / totalTarget).coerceIn(0.0, 1.0) else 0.0

    // Filter/Category state
    var selectedCategoryFilter by remember { mutableStateOf("ALL") }
    val goalCategories = listOf("ALL", "Emergency", "Savings", "Travel", "Vehicle", "Home", "Education", "Other")

    val filteredGoals = remember(goals, selectedCategoryFilter) {
        if (selectedCategoryFilter == "ALL") {
            goals
        } else {
            goals.filter { it.category == selectedCategoryFilter }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("goals_section_root"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Overall Goals Progress Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "Total Accumulated Goals Savings",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = formatInrCurrency(totalSaved),
                            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "Target: ${formatInrCurrency(totalTarget)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    GoalProgressBar(
                        progress = overallProgress.toFloat(),
                        themeColor = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${(overallProgress * 100).toInt()}% completed",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        val remaining = maxOf(0.0, totalTarget - totalSaved)
                        if (remaining > 0) {
                            Text(
                                text = "${formatInrCurrency(remaining)} to go",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                            )
                        } else {
                            Text(
                                text = "All Goals Achieved! 🎉",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }
                }
            }
        }

        // Section Title & Add Action Button
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "My Goals",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Button(
                    onClick = { showAddDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("add_goal_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Goal", fontSize = 14.sp)
                }
            }
        }

        // Category filter chips
        item {
            Box(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                ScrollableRow(
                    items = goalCategories,
                    selectedItem = selectedCategoryFilter,
                    onSelected = { selectedCategoryFilter = it }
                )
            }
        }

        // Goals List
        if (filteredGoals.isEmpty()) {
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
                            imageVector = Icons.Default.Savings,
                            contentDescription = "No goals",
                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (selectedCategoryFilter == "ALL") "No savings goals set yet." else "No goals found in '$selectedCategoryFilter'.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (selectedCategoryFilter == "ALL") {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap 'New Goal' to begin tracking savings for a car, emergency vault, or vacation.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        } else {
            items(filteredGoals) { goal ->
                GoalCard(
                    goal = goal,
                    onEditClick = { selectedGoalForEdit = goal },
                    onDeleteClick = { onDeleteGoal(goal) },
                    onContributeClick = { selectedGoalForContribution = goal },
                    onMilestoneClick = { amtNeeded ->
                        milestoneBoostGoalAndAmount = Pair(goal, amtNeeded)
                    }
                )
            }
        }
    }

    // Add Goal Dialog
    if (showAddDialog) {
        GoalFormDialog(
            title = "Set Financial Goal",
            onDismiss = { showAddDialog = false },
            onSubmit = { name, target, current, targetDate, category, note ->
                onAddGoal(name, target, current, targetDate, category, note)
                showAddDialog = false
            }
        )
    }

    // Edit Goal Dialog
    selectedGoalForEdit?.let { goal ->
        GoalFormDialog(
            title = "Edit Goal Details",
            initialGoal = goal,
            onDismiss = { selectedGoalForEdit = null },
            onSubmit = { name, target, current, targetDate, category, note ->
                onUpdateGoal(goal.copy(
                    name = name,
                    targetAmount = target,
                    currentAmount = current,
                    targetDateMillis = targetDate,
                    category = category,
                    note = note
                ))
                selectedGoalForEdit = null
            }
        )
    }

    // Goal Contribution Action Dialog
    selectedGoalForContribution?.let { goal ->
        ContributionDialog(
            goal = goal,
            onDismiss = { selectedGoalForContribution = null },
            onConfirm = { contribution ->
                onContributeToGoal(goal, contribution)
                selectedGoalForContribution = null
            }
        )
    }

    // Milestone Boost Dialog
    milestoneBoostGoalAndAmount?.let { (goal, amount) ->
        AlertDialog(
            onDismissRequest = { milestoneBoostGoalAndAmount = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Savings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Milestone Boost! 🚀", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(
                        text = "Boost your savings for \"${goal.name}\" to clear the next milestone checkpoint!",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                            Text(
                                text = "Amount Needed to Clear Checkpoint:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatInrCurrency(amount),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "This contribution will be instantly added to your goal balance and logged in your ledger.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onContributeToGoal(goal, amount)
                        milestoneBoostGoalAndAmount = null
                    }
                ) {
                    Text("Contribute Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { milestoneBoostGoalAndAmount = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun GoalCard(
    goal: GoalEntity,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onContributeClick: () -> Unit,
    onMilestoneClick: (Double) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val progress = if (goal.targetAmount > 0) (goal.currentAmount / goal.targetAmount).coerceIn(0.0, 1.0) else 0.0
    val progressPercent = (progress * 100).toInt()

    val categoryIcon = when (goal.category) {
        "Emergency" -> Icons.Default.HealthAndSafety
        "Savings" -> Icons.Default.Savings
        "Travel" -> Icons.Default.FlightTakeoff
        "Vehicle" -> Icons.Default.DirectionsCar
        "Home" -> Icons.Default.Home
        "Education" -> Icons.Default.School
        else -> Icons.Default.PlaylistAddCheck
    }

    val themeColor = when (goal.category) {
        "Emergency" -> Color(0xFFC62828) // Deep Red
        "Savings" -> Color(0xFF2E7D32) // Emerald Green
        "Travel" -> Color(0xFF1565C0) // Active Blue
        "Vehicle" -> Color(0xFFEF6C00) // Dark Orange
        "Home" -> Color(0xFF651FFF) // Royal Indigo
        "Education" -> Color(0xFF00838F) // Teal
        else -> MaterialTheme.colorScheme.primary
    }

    val isCompleted = goal.currentAmount >= goal.targetAmount

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("goal_item_${goal.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Goal header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(themeColor.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = categoryIcon,
                            contentDescription = goal.category,
                            tint = themeColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = goal.name,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (isCompleted) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Completed",
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        val dateFormatted = remember(goal.targetDateMillis) {
                            val formatter = SimpleDateFormat("MMM yyyy", Locale.getDefault())
                            formatter.format(Date(goal.targetDateMillis))
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Target: $dateFormatted",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            val daysRemaining = remember(goal.targetDateMillis) {
                                val diff = goal.targetDateMillis - System.currentTimeMillis()
                                (diff / (1000 * 60 * 60 * 24)).toInt()
                            }

                            val statusText = when {
                                isCompleted -> "Completed"
                                daysRemaining > 30 -> "${daysRemaining / 30} months left"
                                daysRemaining in 1..30 -> "$daysRemaining days left"
                                daysRemaining == 0 -> "Ends today"
                                else -> "Overdue by ${Math.abs(daysRemaining)} days"
                            }
                            val statusBgColor = when {
                                isCompleted -> Color(0xFFE8F5E9)
                                daysRemaining > 30 -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                                daysRemaining in 1..30 -> Color(0xFFFFF3E0)
                                else -> Color(0xFFFFEBEE)
                            }
                            val statusTextColor = when {
                                isCompleted -> Color(0xFF2E7D32)
                                daysRemaining > 30 -> MaterialTheme.colorScheme.onSecondaryContainer
                                daysRemaining in 1..30 -> Color(0xFFE65100)
                                else -> Color(0xFFC62828)
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(statusBgColor)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = statusText,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = statusTextColor
                                )
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onEditClick, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Goal",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(onClick = onDeleteClick, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Goal",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Progress status text
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${formatInrCurrency(goal.currentAmount)} of ${formatInrCurrency(goal.targetAmount)}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$progressPercent%",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = themeColor
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Custom styled animated progress bar with gradient and segment marks
            GoalProgressBar(
                progress = progress.toFloat(),
                themeColor = themeColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            )

            // Milestone subtext and recommended monthly savings rate if not completed
            val daysRemainingForSugg = remember(goal.targetDateMillis) {
                val diff = goal.targetDateMillis - System.currentTimeMillis()
                (diff / (1000 * 60 * 60 * 24)).toInt()
            }

            val milestoneText = when {
                isCompleted -> "🎉 Goal achieved!"
                progressPercent in 1..24 -> "🌱 Getting started!"
                progressPercent in 25..49 -> "📈 Quarter way there!"
                progressPercent in 50..74 -> "🎯 Halfway mark cleared!"
                progressPercent in 75..99 -> "🔥 Almost accomplished!"
                else -> "✨ Active planning"
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = milestoneText,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = themeColor
                )

                if (!isCompleted) {
                    val remainingAmount = maxOf(0.0, goal.targetAmount - goal.currentAmount)
                    val monthsRemaining = maxOf(0.1, daysRemainingForSugg / 30.437)
                    val recommendedMonthly = remainingAmount / monthsRemaining
                    if (daysRemainingForSugg > 0 && recommendedMonthly > 0) {
                        Text(
                            text = "💡 Rec. Save: ${formatInrCurrency(recommendedMonthly)}/mo",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Interactive Savings Goal Milestones Row
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Milestone Checkpoints (Tap to Boost)",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val milestones = listOf(
                        Triple(25, 0.25, "Quarterly"),
                        Triple(50, 0.50, "Halfway"),
                        Triple(75, 0.75, "Three-Quarters")
                    )

                    milestones.forEach { (percent, fraction, label) ->
                        val targetAmount = goal.targetAmount * fraction
                        val isReached = goal.currentAmount >= targetAmount
                        val amountNeeded = targetAmount - goal.currentAmount

                        val milestoneColor = if (isReached) themeColor else MaterialTheme.colorScheme.outlineVariant

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isReached) themeColor.copy(alpha = 0.08f) 
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                                )
                                .clickable(!isReached) { onMilestoneClick(amountNeeded) }
                                .padding(vertical = 8.dp, horizontal = 6.dp)
                                .weight(1f)
                        ) {
                            Icon(
                                imageVector = if (isReached) Icons.Default.CheckCircle else Icons.Default.Lock,
                                contentDescription = null,
                                tint = milestoneColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$percent%",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isReached) themeColor else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = label,
                                fontSize = 8.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (percent != 75) {
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bottom controls: notes & Piggybank contribute action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    if (goal.note.isNotBlank()) {
                        Text(
                            text = goal.note,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                ElevatedButton(
                    onClick = onContributeClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = themeColor.copy(alpha = 0.08f),
                        contentColor = themeColor
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("contribute_goal_btn_${goal.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Savings,
                        contentDescription = "Contribute",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isCompleted) "Add More" else "Add Fund",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalFormDialog(
    title: String,
    initialGoal: GoalEntity? = null,
    onDismiss: () -> Unit,
    onSubmit: (String, Double, Double, Long, String, String) -> Unit
) {
    var name by remember { mutableStateOf(initialGoal?.name ?: "") }
    var targetStr by remember { mutableStateOf(initialGoal?.targetAmount?.toString() ?: "") }
    var currentStr by remember { mutableStateOf(initialGoal?.currentAmount?.toString() ?: "") }
    var selectedCategory by remember { mutableStateOf(initialGoal?.category ?: "Savings") }
    var note by remember { mutableStateOf(initialGoal?.note ?: "") }
    var targetDateMillis by remember { mutableStateOf(initialGoal?.targetDateMillis ?: (System.currentTimeMillis() + 31536000000L)) } // 1 year default

    var expandedCatDropdown by remember { mutableStateOf(false) }
    val categories = listOf("Emergency", "Savings", "Travel", "Vehicle", "Home", "Education", "Other")

    var nameError by remember { mutableStateOf(false) }
    var targetError by remember { mutableStateOf(false) }
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
                    label = { Text("Goal Name") },
                    isError = nameError,
                    placeholder = { Text("e.g. New Electric Bike, Trip to Japan") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("goal_name_input")
                )

                // Category Dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    ExposedDropdownMenuBox(
                        expanded = expandedCatDropdown,
                        onExpandedChange = { expandedCatDropdown = !expandedCatDropdown }
                    ) {
                        OutlinedTextField(
                            value = selectedCategory,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Goal Category") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCatDropdown) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedCatDropdown,
                            onDismissRequest = { expandedCatDropdown = false }
                        ) {
                            categories.forEach { c ->
                                DropdownMenuItem(
                                    text = { Text(c) },
                                    onClick = {
                                        selectedCategory = c
                                        expandedCatDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Cost and Saved Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = targetStr,
                        onValueChange = {
                            targetStr = it
                            targetError = it.toDoubleOrNull() == null
                        },
                        label = { Text("Target budget") },
                        isError = targetError,
                        placeholder = { Text("INR") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("goal_target_input")
                    )

                    OutlinedTextField(
                        value = currentStr,
                        onValueChange = {
                            currentStr = it
                            currentError = it.toDoubleOrNull() == null
                        },
                        label = { Text("Saved Already") },
                        isError = currentError,
                        placeholder = { Text("INR") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("goal_saved_input")
                    )
                }

                // Target Date Input Field
                var showDatePicker by remember { mutableStateOf(false) }
                val targetDateFormatted = remember(targetDateMillis) {
                    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                    sdf.format(Date(targetDateMillis))
                }

                OutlinedTextField(
                    value = targetDateFormatted,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Target Completion Date") },
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(
                                imageVector = Icons.Default.Event,
                                contentDescription = "Select Target Date"
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true }
                )

                if (showDatePicker) {
                    CustomCalendarDatePicker(
                        selectedDateMillis = targetDateMillis,
                        onDateSelected = {
                            targetDateMillis = it
                            showDatePicker = false
                        },
                        onDismiss = { showDatePicker = false }
                    )
                }

                // Remark
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Notes / Motivations") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val isNameValid = name.isNotBlank()
                    val targetAmt = targetStr.toDoubleOrNull()
                    val currentAmt = currentStr.toDoubleOrNull() ?: 0.0

                    nameError = !isNameValid
                    targetError = targetAmt == null
                    currentError = currentStr.isNotBlank() && currentStr.toDoubleOrNull() == null

                    if (isNameValid && targetAmt != null) {
                        onSubmit(name, targetAmt, currentAmt, targetDateMillis, selectedCategory, note)
                    }
                }
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ContributionDialog(
    goal: GoalEntity,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var amountStr by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Deposit to '${goal.name}'",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Add standard savings into your goals piggybank. Remaining to target: ${formatInrCurrency(maxOf(0.0, goal.targetAmount - goal.currentAmount))}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = {
                        amountStr = it
                        isError = it.toDoubleOrNull() == null
                    },
                    label = { Text("Deposit Amount") },
                    placeholder = { Text("INR (positive to deposit, negative to withdraw)") },
                    isError = isError,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("contribution_amount_input")
                )

                // Quick pre-filled buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val presets = listOf(500.0, 1000.0, 5000.0)
                    presets.forEach { amt ->
                        Button(
                            onClick = { amountStr = amt.toInt().toString() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("+${amt.toInt()}")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountStr.toDoubleOrNull()
                    if (amt != null) {
                        onConfirm(amt)
                    } else {
                        isError = true
                    }
                }
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
