package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.FuelEntryEntity
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FuelTrackerTab(
    fuelEntries: List<FuelEntryEntity>,
    onAddFuelEntry: (Double, Double, Double, String, String, Long) -> Unit, // amount, volume, odometer, note, vehicleType, dateMillis
    onUpdateFuelEntry: (Int, Double, Double, Double, String, String, Long) -> Unit, // id, amount, volume, odometer, note, vehicleType, dateMillis
    onDeleteFuelEntry: (FuelEntryEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    // Current state selections
    var selectedVehicle by remember { mutableStateOf("CAR") } // "CAR" or "BIKE"

    // Creation Form states
    var selectedDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var odoText by remember { mutableStateOf("") }
    var volumeText by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var notesText by remember { mutableStateOf("") }

    // State for open date pickers
    var showDatePickerDialog by remember { mutableStateOf(false) }

    // State for editing fuel log
    var editingLog by remember { mutableStateOf<FuelEntryEntity?>(null) }

    // Date formatting helper
    val sdf = remember { SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()) }
    val dateText = sdf.format(Date(selectedDateMillis))

    // Filtered lists based on choice
    val filteredEntries = fuelEntries.filter { it.vehicleType.uppercase() == selectedVehicle }
    val sortedAsc = filteredEntries.sortedBy { it.odometerReading }
    val sortedDesc = filteredEntries.sortedByDescending { it.dateMillis }

    // Summary calculations (on filtered active vehicle logs)
    val totalSpent = filteredEntries.sumOf { it.amountPaid }
    val totalVolume = filteredEntries.sumOf { it.volumeLiters }
    val fillUpsCount = filteredEntries.size

    val firstLogOdometer = sortedAsc.firstOrNull()?.odometerReading ?: 0.0
    val latestLogOdometer = sortedAsc.lastOrNull()?.odometerReading ?: 0.0
    val totalDistanceDriven = if (sortedAsc.size >= 2) latestLogOdometer - firstLogOdometer else 0.0

    val fuelConsumedForDistance = if (sortedAsc.size >= 2) {
        sortedAsc.drop(1).sumOf { it.volumeLiters }
    } else {
        0.0
    }

    val averageFuelConsumption = if (totalDistanceDriven > 0 && fuelConsumedForDistance > 0) {
        totalDistanceDriven / fuelConsumedForDistance
    } else {
        0.0
    }

    val previousOdo = sortedAsc.lastOrNull()?.odometerReading ?: 0.0

    // Live price calculation helpers
    val volumeTextD = volumeText.toDoubleOrNull() ?: 0.0
    val priceTextD = priceText.toDoubleOrNull() ?: 0.0
    val computedCost = volumeTextD * priceTextD

    val dec = remember { DecimalFormat("#,##0.0") }
    val dec2 = remember { DecimalFormat("0.00") }

    // Date Picker Dialog Container
    if (showDatePickerDialog) {
        CustomCalendarDatePicker(
            selectedDateMillis = selectedDateMillis,
            onDateSelected = { selected ->
                selectedDateMillis = selected
                showDatePickerDialog = false
            },
            onDismiss = { showDatePickerDialog = false }
        )
    }

    // Modal Edit Form Dialog
    if (editingLog != null) {
        EditFuelLogDialog(
            log = editingLog!!,
            onDismiss = { editingLog = null },
            onSave = { updatedLog ->
                onUpdateFuelEntry(
                    updatedLog.id,
                    updatedLog.amountPaid,
                    updatedLog.volumeLiters,
                    updatedLog.odometerReading,
                    updatedLog.note,
                    updatedLog.vehicleType,
                    updatedLog.dateMillis
                )
                editingLog = null
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("fuel_tracker_tab_root"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Header Unit - Highly styled with Emerald slate modern gradient accent
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.secondary
                                    )
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalGasStation,
                            contentDescription = "Fuel Tracker",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Fuel Performance",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Real-time mileage, cost analysis and trends",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 1. Direct Add Log Card (Premium Refined Interface)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                ),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Log Refuel Check-in",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Compact segment tab row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .background(
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Car tab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selectedVehicle == "CAR") MaterialTheme.colorScheme.surface else Color.Transparent)
                                .clickable { selectedVehicle = "CAR" },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.DirectionsCar,
                                    contentDescription = "Car",
                                    tint = if (selectedVehicle == "CAR") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Car Logs",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (selectedVehicle == "CAR") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Bike tab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selectedVehicle == "BIKE") MaterialTheme.colorScheme.surface else Color.Transparent)
                                .clickable { selectedVehicle = "BIKE" },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.TwoWheeler,
                                    contentDescription = "Bike",
                                    tint = if (selectedVehicle == "BIKE") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Bike Logs",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (selectedVehicle == "BIKE") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Input Fields Row (Clickable Date Selection Layout)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Interactive Calendar Date Picker Field
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
                                    .clickable { showDatePickerDialog = true }
                                    .clip(RoundedCornerShape(8.dp)),
                                color = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
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

                        // Odometer numeric input
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = "Odometer",
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Odometer (km)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = odoText,
                                onValueChange = { odoText = it },
                                placeholder = { Text("e.g. 12450", fontSize = 13.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth().testTag("add_fuel_odo_input"),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Liters numeric input
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.WaterDrop,
                                    contentDescription = "Liters",
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Liters (L)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = volumeText,
                                onValueChange = { volumeText = it },
                                placeholder = { Text("e.g. 15.5", fontSize = 13.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth().testTag("add_fuel_liters_input"),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp)
                            )
                        }

                        // Price/Liter numeric input
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Payments,
                                    contentDescription = "Price per liter",
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Price (₹/L)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = priceText,
                                onValueChange = { priceText = it },
                                placeholder = { Text("e.g. 102.5", fontSize = 13.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth().testTag("add_fuel_price_input"),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }

                    // Optional location Station card input
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Notes,
                                contentDescription = "Notes",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Station Place / Note (Optional)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = notesText,
                            onValueChange = { notesText = it },
                            placeholder = { Text("e.g. HP Petrol Pump, NH44 Hub", fontSize = 13.sp) },
                            modifier = Modifier.fillMaxWidth().testTag("add_fuel_notes_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    // Automatic calculation helper highlight
                    if (computedCost > 0) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Total Payment Drafted:",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = formatCurrency(computedCost),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // Save Refuel Action Button
                    Button(
                        onClick = {
                            val odo = odoText.toDoubleOrNull()
                            val vol = volumeText.toDoubleOrNull()
                            val prc = priceText.toDoubleOrNull()
                            val amt = if (vol != null && prc != null) vol * prc else 0.0

                            if (vol != null && vol > 0 && odo != null && odo >= previousOdo && amt > 0) {
                                val stationName = notesText.ifBlank { "Gas Station Refuel" }
                                onAddFuelEntry(amt, vol, odo, stationName, selectedVehicle, selectedDateMillis)

                                // Clear forms
                                odoText = ""
                                volumeText = ""
                                priceText = ""
                                notesText = ""
                                selectedDateMillis = System.currentTimeMillis()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("add_fuel_log_submit_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Log")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Refuel Record", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                    }
                }
            }
        }

        // 2. Beautiful Statistics Cards (Clean Material Integration with Dynamic Month Headers)
        item {
            val dynamicMonthName = remember(sortedDesc) {
                if (sortedDesc.isNotEmpty()) {
                    val logDate = Date(sortedDesc.first().dateMillis)
                    val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                    monthFormat.format(logDate)
                } else {
                    val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                    monthFormat.format(Date())
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Left Prominent Highlight: Active Month Expenses
                Card(
                    modifier = Modifier
                        .weight(1.1f)
                        .height(140.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = dynamicMonthName,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Bold
                        )
                        Column {
                            Text(
                                text = formatCurrency(totalSpent),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${dec.format(totalVolume)} L · $fillUpsCount Refuels",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Right detailed quick view columns
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(140.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Average Mileage Details Card (Calculated clearly)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.WaterDrop,
                                    contentDescription = "Mileage",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Avg mileage", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (averageFuelConsumption > 0) "${dec2.format(averageFuelConsumption)} km/L" else "Needs 2+ logs",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (averageFuelConsumption > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Total Miles Driven
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Directions,
                                    contentDescription = "Distance",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Total distance", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${dec.format(totalDistanceDriven)} km",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Section Title
        item {
            Text(
                text = "${if (selectedVehicle == "BIKE") "Bike" else "Car"} Refueling Records",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // Historical Log List displaying items dynamically
        if (sortedDesc.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalGasStation,
                            contentDescription = "Empty Log",
                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No refuel records logged yet",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Record fill-ups above to automatically compute mileage!",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        } else {
            items(sortedDesc, key = { log -> "${log.id}_${log.dateMillis}" }) { log ->
                val indexInAsc = sortedAsc.indexOf(log)
                val cycleEconomy = if (indexInAsc > 0) {
                    val prevLog = sortedAsc[indexInAsc - 1]
                    val dist = log.odometerReading - prevLog.odometerReading
                    if (dist > 0 && log.volumeLiters > 0.0) {
                        val kmPerL = dist / log.volumeLiters
                        val l100k = (log.volumeLiters / dist) * 100.0
                        Pair(kmPerL, l100k)
                    } else null
                } else null

                FuelHistoryItem(
                    log = log,
                    cycleEconomy = cycleEconomy,
                    onEdit = { editingLog = log },
                    onDelete = { onDeleteFuelEntry(log) }
                )
            }
        }
    }
}

@Composable
fun FuelHistoryItem(
    log: FuelEntryEntity,
    cycleEconomy: Pair<Double, Double>?, // Pair (kmPerL, l100k)
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formatter = remember { SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault()) }
    val dateString = formatter.format(Date(log.dateMillis))
    val dec = remember { DecimalFormat("#,##0.0") }
    val dec2 = remember { DecimalFormat("0.00") }

    var showDeleteConfirm by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = if (log.vehicleType == "BIKE") {
                                MaterialTheme.colorScheme.tertiaryContainer
                            } else {
                                MaterialTheme.colorScheme.secondaryContainer
                            },
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (log.vehicleType == "BIKE") Icons.Default.TwoWheeler else Icons.Default.DirectionsCar,
                        contentDescription = log.vehicleType,
                        tint = if (log.vehicleType == "BIKE") {
                            MaterialTheme.colorScheme.onTertiaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (log.note.isNotBlank()) log.note else "Gas Station Refuel",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = dateString,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatCurrency(log.amountPaid),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${dec.format(log.volumeLiters)} Liters",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "@ ${formatCurrency(log.amountPaid / log.volumeLiters)}/L",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Mileage stats detail footer line (Comprises dynamic actions + inline stats)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = "Odometer",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${dec.format(log.odometerReading)} km",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (cycleEconomy != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.WaterDrop,
                            contentDescription = "Fuel economy",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${dec2.format(cycleEconomy.first)} km/L",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    Text(
                        text = "First entry",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                }

                // Interactive Edit & Delete Actions Side-by-Side
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Edit Button (Added to fulfill editable requirement!)
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(28.dp).testTag("edit_fuel_log_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Fuel Entry",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    // Delete Button
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.size(28.dp).testTag("delete_fuel_log_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Fuel Entry",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Fuel Log") },
            text = { Text("Are you sure you want to delete this fuel entry? This will also remove the corresponding transaction.") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditFuelLogDialog(
    log: FuelEntryEntity,
    onDismiss: () -> Unit,
    onSave: (FuelEntryEntity) -> Unit
) {
    var editVehicleType by remember { mutableStateOf(log.vehicleType) }
    var editOdometer by remember { mutableStateOf(log.odometerReading.toString()) }
    var editVolumeLiters by remember { mutableStateOf(log.volumeLiters.toString()) }
    var editAmountPaid by remember { mutableStateOf(log.amountPaid.toString()) }
    var editNote by remember { mutableStateOf(log.note) }
    var editDateMillis by remember { mutableStateOf(log.dateMillis) }

    var showDatePicker by remember { mutableStateOf(false) }

    val sdf = remember { SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()) }
    val dateText = sdf.format(Date(editDateMillis))

    if (showDatePicker) {
        CustomCalendarDatePicker(
            selectedDateMillis = editDateMillis,
            onDateSelected = { selected ->
                editDateMillis = selected
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Edit Fuel Record",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Vehicle selection row
                Text("Vehicle Type", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .background(
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (editVehicleType == "CAR") MaterialTheme.colorScheme.surface else Color.Transparent)
                            .clickable { editVehicleType = "CAR" },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Car", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (editVehicleType == "BIKE") MaterialTheme.colorScheme.surface else Color.Transparent)
                            .clickable { editVehicleType = "BIKE" },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Bike", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                // Date click selection field matching the calendar picker requirements
                Text("Refueling Date", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clickable { showDatePicker = true }
                        .clip(RoundedCornerShape(8.dp)),
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = dateText, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = "Edit Date", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                }

                // Inputs
                OutlinedTextField(
                    value = editOdometer,
                    onValueChange = { editOdometer = it },
                    label = { Text("Odometer Reading (km)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("edit_fuel_odo_input")
                )

                OutlinedTextField(
                    value = editVolumeLiters,
                    onValueChange = { editVolumeLiters = it },
                    label = { Text("Liters (L)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("edit_fuel_liters_input")
                )

                OutlinedTextField(
                    value = editAmountPaid,
                    onValueChange = { editAmountPaid = it },
                    label = { Text("Total Paid (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("edit_fuel_amount_input")
                )

                OutlinedTextField(
                    value = editNote,
                    onValueChange = { editNote = it },
                    label = { Text("Note / Station") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("edit_fuel_notes_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalOdo = editOdometer.toDoubleOrNull() ?: log.odometerReading
                    val finalVolume = editVolumeLiters.toDoubleOrNull() ?: log.volumeLiters
                    val finalAmt = editAmountPaid.toDoubleOrNull() ?: log.amountPaid

                    onSave(
                        log.copy(
                            vehicleType = editVehicleType,
                            odometerReading = finalOdo,
                            volumeLiters = finalVolume,
                            amountPaid = finalAmt,
                            note = editNote,
                            dateMillis = editDateMillis
                        )
                    )
                },
                modifier = Modifier.testTag("save_edit_fuel_btn")
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
