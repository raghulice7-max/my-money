package com.example.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.RecurringReminderEntity
import com.example.data.ReminderCategory
import com.example.data.ReminderType
import com.example.util.daysUntilDue
import com.example.util.formatInrCurrency
import com.example.util.getCurrentDueDate
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

enum class ReminderStatusPriority(
    val priority: Int,
    val badgeContainerColor: @Composable () -> Color,
    val badgeContentColor: @Composable () -> Color,
    val borderColor: @Composable () -> Color
) {
    OVERDUE(
        priority = 0,
        badgeContainerColor = { MaterialTheme.colorScheme.errorContainer },
        badgeContentColor = { MaterialTheme.colorScheme.onErrorContainer },
        borderColor = { MaterialTheme.colorScheme.error }
    ),
    DUE_SOON(
        priority = 1,
        badgeContainerColor = { Color(0xFFFFF3E0) }, // Warm Amber
        badgeContentColor = { Color(0xFFE65100) },
        borderColor = { Color(0xFFF57C00) }
    ),
    UPCOMING(
        priority = 2,
        badgeContainerColor = { MaterialTheme.colorScheme.secondaryContainer },
        badgeContentColor = { MaterialTheme.colorScheme.onSecondaryContainer },
        borderColor = { MaterialTheme.colorScheme.outlineVariant }
    ),
    PAID(
        priority = 3,
        badgeContainerColor = { MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) },
        badgeContentColor = { MaterialTheme.colorScheme.onPrimaryContainer },
        borderColor = { MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) }
    )
}

data class EvaluatedReminder(
    val reminder: RecurringReminderEntity,
    val isPaidThisMonth: Boolean,
    val dueDate: LocalDate,
    val daysRemaining: Long,
    val statusPriority: ReminderStatusPriority
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersSection(
    reminders: List<RecurringReminderEntity>,
    onAddReminder: (String, Double, Int, String, String, String) -> Unit,
    onDeleteReminder: (RecurringReminderEntity) -> Unit,
    onUpdateReminder: (RecurringReminderEntity) -> Unit,
    onPayReminder: (RecurringReminderEntity, (paymentId: Long, previousLastPaidDate: LocalDate?) -> Unit) -> Unit,
    onUndoPayReminder: (RecurringReminderEntity, Long, LocalDate?) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedReminderForEdit by remember { mutableStateOf<RecurringReminderEntity?>(null) }
    var reminderToDelete by remember { mutableStateOf<RecurringReminderEntity?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val today = remember { LocalDate.now() }

    // Evaluated and sorted reminders
    val evaluatedReminders = remember(reminders, today) {
        reminders.map { reminder ->
            val isPaidThisMonth = reminder.lastPaidDate?.let {
                it.year == today.year && it.month == today.month
            } ?: false

            val dueDate = getCurrentDueDate(reminder.dayOfMonth, today)
            val daysRem = daysUntilDue(dueDate, today)

            val status = when {
                isPaidThisMonth -> ReminderStatusPriority.PAID
                daysRem < 0 -> ReminderStatusPriority.OVERDUE
                daysRem in 0..5 -> ReminderStatusPriority.DUE_SOON
                else -> ReminderStatusPriority.UPCOMING
            }

            EvaluatedReminder(
                reminder = reminder,
                isPaidThisMonth = isPaidThisMonth,
                dueDate = dueDate,
                daysRemaining = daysRem,
                statusPriority = status
            )
        }.sortedWith(
            compareBy<EvaluatedReminder> { it.statusPriority.priority }
                .thenBy { it.dueDate }
                .thenBy { it.reminder.name }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = Color.Transparent,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("reminders_section_root")
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header item
                item(key = "header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.reminders_title),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.reminders_subtitle),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = { showAddDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("add_reminder_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.add_new_reminder), fontSize = 14.sp)
                        }
                    }
                }

                // Summary Stats Card
                if (reminders.isNotEmpty()) {
                    item(key = "summary_card") {
                        val unpaidCount = evaluatedReminders.count { !it.isPaidThisMonth }
                        val totalMonthlyOutflow = reminders.sumOf { it.amount }

                        ReminderSummaryCard(
                            totalMonthlyOutflow = totalMonthlyOutflow,
                            unpaidCount = unpaidCount,
                            modifier = Modifier.animateItem()
                        )
                    }
                }

                // Reminders List or Empty State
                if (reminders.isEmpty()) {
                    item(key = "empty_state") {
                        ReminderEmptyState(modifier = Modifier.animateItem())
                    }
                } else {
                    items(
                        items = evaluatedReminders,
                        key = { it.reminder.id }
                    ) { evaluated ->
                        val reminder = evaluated.reminder

                        SwipeToDismissBox(
                            state = rememberSwipeToDismissBoxState(
                                confirmValueChange = { dismissValue ->
                                    if (dismissValue == SwipeToDismissBoxValue.EndToStart || dismissValue == SwipeToDismissBoxValue.StartToEnd) {
                                        reminderToDelete = reminder
                                        false
                                    } else false
                                }
                            ),
                            backgroundContent = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(MaterialTheme.colorScheme.errorContainer)
                                        .padding(horizontal = 20.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = stringResource(R.string.delete),
                                        tint = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            },
                            modifier = Modifier.animateItem()
                        ) {
                            ReminderCard(
                                evaluated = evaluated,
                                onEditClick = { selectedReminderForEdit = reminder },
                                onDeleteClick = { reminderToDelete = reminder },
                                onPayClick = {
                                    onPayReminder(reminder) { paymentId, previousDate ->
                                        coroutineScope.launch {
                                            val result = snackbarHostState.showSnackbar(
                                                message = "Logged ${formatInrCurrency(reminder.amount)} for ${reminder.name}",
                                                actionLabel = "Undo",
                                                duration = SnackbarDuration.Short
                                            )
                                            if (result == SnackbarResult.ActionPerformed) {
                                                onUndoPayReminder(reminder, paymentId, previousDate)
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    reminderToDelete?.let { reminder ->
        AlertDialog(
            onDismissRequest = { reminderToDelete = null },
            title = { Text(stringResource(R.string.delete_reminder_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.delete_reminder_message, reminder.name)) },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteReminder(reminder)
                        reminderToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { reminderToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Add Reminder Dialog
    if (showAddDialog) {
        ReminderFormDialog(
            title = stringResource(R.string.add_recurring_reminder),
            onDismiss = { showAddDialog = false },
            onSubmit = { name, amt, day, type, cat, notes ->
                onAddReminder(name, amt, day, type, cat, notes)
                showAddDialog = false
            }
        )
    }

    // Edit Reminder Dialog
    selectedReminderForEdit?.let { reminder ->
        ReminderFormDialog(
            title = stringResource(R.string.edit_reminder_details),
            initialReminder = reminder,
            onDismiss = { selectedReminderForEdit = null },
            onSubmit = { name, amt, day, type, cat, notes ->
                onUpdateReminder(
                    reminder.copy(
                        name = name,
                        amount = amt,
                        dayOfMonth = day,
                        type = type,
                        category = cat,
                        notes = notes
                    )
                )
                selectedReminderForEdit = null
            }
        )
    }
}

@Composable
fun ReminderSummaryCard(
    totalMonthlyOutflow: Double,
    unpaidCount: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.monthly_commitment),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = formatInrCurrency(totalMonthlyOutflow),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Box(
                modifier = Modifier
                    .background(
                        if (unpaidCount > 0) MaterialTheme.colorScheme.errorContainer
                        else MaterialTheme.colorScheme.primaryContainer,
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = if (unpaidCount > 0)
                        stringResource(R.string.pending_reminders, unpaidCount)
                    else stringResource(R.string.all_cleared),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (unpaidCount > 0) MaterialTheme.colorScheme.onErrorContainer
                    else MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
fun ReminderEmptyState(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.NotificationsActive,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.empty_reminders_title),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.empty_reminders_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = 16.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ReminderCard(
    evaluated: EvaluatedReminder,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onPayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val reminder = evaluated.reminder
    val typeEnum = reminder.typeEnum
    val themeColor = typeEnum.themeColor
    val indicatorBg = themeColor.copy(alpha = 0.08f)
    val statusPriority = evaluated.statusPriority

    val borderColor = statusPriority.borderColor()
    val badgeContainerColor = statusPriority.badgeContainerColor()
    val badgeContentColor = statusPriority.badgeContentColor()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("reminder_card_${reminder.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        border = BorderStroke(1.5.dp, borderColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top Badge Bar for Status
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(badgeContainerColor)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    val statusText = when (statusPriority) {
                        ReminderStatusPriority.OVERDUE -> stringResource(R.string.overdue_by_days, kotlin.math.abs(evaluated.daysRemaining))
                        ReminderStatusPriority.DUE_SOON -> if (evaluated.daysRemaining == 0L) stringResource(R.string.due_today) else stringResource(R.string.due_in_days, evaluated.daysRemaining)
                        ReminderStatusPriority.UPCOMING -> stringResource(R.string.due_in_days, evaluated.daysRemaining)
                        ReminderStatusPriority.PAID -> {
                            val formattedDate = reminder.lastPaidDate?.format(DateTimeFormatter.ofPattern("MMM d")) ?: ""
                            stringResource(R.string.paid_for_month, formattedDate)
                        }
                    }
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = badgeContentColor
                    )
                }

                // Type Tag (SIP / RD)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(themeColor.copy(alpha = 0.12f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = typeEnum.displayName,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = themeColor
                    )
                }
            }

            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(indicatorBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = typeEnum.icon,
                            contentDescription = typeEnum.displayName,
                            tint = themeColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = reminder.name,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 170.dp)
                        )
                        Text(
                            text = stringResource(R.string.due_on_day, reminder.dayOfMonth),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Edit/Delete buttons
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onEditClick, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.edit_reminder_details),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(onClick = onDeleteClick, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.delete),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Amount and Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.amount_due),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatInrCurrency(reminder.amount),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (evaluated.isPaidThisMonth) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = stringResource(R.string.all_cleared),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = onPayClick,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.pay_and_log), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (reminder.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "📝 ${reminder.notes}",
                    style = MaterialTheme.typography.labelSmall.copy(fontStyle = FontStyle.Italic),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderFormDialog(
    title: String,
    initialReminder: RecurringReminderEntity? = null,
    onDismiss: () -> Unit,
    onSubmit: (String, Double, Int, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(initialReminder?.name ?: "") }
    var amountStr by remember { mutableStateOf(initialReminder?.amount?.toString() ?: "") }
    var dayStr by remember { mutableStateOf(initialReminder?.dayOfMonth?.toString() ?: "5") }
    var typeCode by remember { mutableStateOf(initialReminder?.type ?: "SIP") }
    var categoryCode by remember { mutableStateOf(initialReminder?.category ?: "Investment") }
    var notes by remember { mutableStateOf(initialReminder?.notes ?: "") }

    var nameError by remember { mutableStateOf(false) }
    var amountError by remember { mutableStateOf(false) }
    var dayError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; nameError = false },
                    label = { Text(stringResource(R.string.reminder_name)) },
                    placeholder = { Text(stringResource(R.string.reminder_name_placeholder)) },
                    isError = nameError,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it; amountError = false },
                        label = { Text(stringResource(R.string.amount)) },
                        isError = amountError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = dayStr,
                        onValueChange = { dayStr = it; dayError = false },
                        label = { Text(stringResource(R.string.day_of_month)) },
                        placeholder = { Text("1-31") },
                        isError = dayError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                // Type Selector (SIP, RD, BILL, OTHER)
                Text(stringResource(R.string.type), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(ReminderType.values()) { item ->
                        val isSel = typeCode == item.code
                        FilterChip(
                            selected = isSel,
                            onClick = {
                                typeCode = item.code
                                if (item == ReminderType.SIP) categoryCode = "Investment"
                                else if (item == ReminderType.RD) categoryCode = "Savings"
                            },
                            label = { Text(item.displayName) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = item.themeColor.copy(alpha = 0.15f),
                                selectedLabelColor = item.themeColor
                            )
                        )
                    }
                }

                // Category selector
                Text(stringResource(R.string.ledger_category), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(ReminderCategory.values()) { catItem ->
                        val isSel = categoryCode == catItem.code
                        FilterChip(
                            selected = isSel,
                            onClick = { categoryCode = catItem.code },
                            label = { Text(catItem.displayName) },
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.notes_optional)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountStr.toDoubleOrNull()
                    val day = dayStr.toIntOrNull()

                    if (name.isBlank()) nameError = true
                    if (amt == null || amt <= 0) amountError = true
                    if (day == null || day !in 1..31) dayError = true

                    if (name.isNotBlank() && amt != null && amt > 0 && day != null && day in 1..31) {
                        onSubmit(name, amt, day, typeCode, categoryCode, notes)
                    }
                }
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun ReminderSummaryCardPreview() {
    MaterialTheme {
        ReminderSummaryCard(
            totalMonthlyOutflow = 8000.0,
            unpaidCount = 1,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ReminderEmptyStatePreview() {
    MaterialTheme {
        ReminderEmptyState(modifier = Modifier.padding(16.dp))
    }
}
