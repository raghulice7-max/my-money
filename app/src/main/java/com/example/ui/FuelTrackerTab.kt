package com.example.ui

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
import com.example.data.FuelEntryEntity
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun FuelTrackerTab(
    fuelEntries: List<FuelEntryEntity>,
    onAddFuelEntry: (Double, Double, Double, String, String) -> Unit, // amount, volume, odometer, note, vehicleType
    onDeleteFuelEntry: (FuelEntryEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    // Current local time date formatting
    val sdf = remember { SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()) }
    val currentDateStr = remember { sdf.format(Date()) }

    // Selected vehicle state
    var selectedVehicle by remember { mutableStateOf("CAR") } // "CAR" or "BIKE"

    // Form states
    var dateText by remember { mutableStateOf(currentDateStr) }
    var odoText by remember { mutableStateOf("") }
    var volumeText by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var notesText by remember { mutableStateOf("") }

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

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("fuel_tracker_tab_root"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App header logo line (Gas Station Icon with green backdrop)
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(Color(0xFF0F9D58), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalGasStation,
                        contentDescription = "Fuel Tracker Icon",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Fuel Tracker",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 22.sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Mileage & monthly fuel expense",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 1. Direct Add Log card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Vehicle",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Rounded Selector Row [ Car | Bike ]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .background(Color(0xFF1B3D2B).copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                            .padding(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Car tab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selectedVehicle == "CAR") Color.White else Color.Transparent)
                                .clickable { selectedVehicle = "CAR" },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.DirectionsCar,
                                    contentDescription = "Car",
                                    tint = if (selectedVehicle == "CAR") Color(0xFF1B3D2B) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Car",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = if (selectedVehicle == "CAR") Color(0xFF1B3D2B) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Bike tab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selectedVehicle == "BIKE") Color.White else Color.Transparent)
                                .clickable { selectedVehicle = "BIKE" },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.TwoWheeler,
                                    contentDescription = "Bike",
                                    tint = if (selectedVehicle == "BIKE") Color(0xFF1B3D2B) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Bike",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = if (selectedVehicle == "BIKE") Color(0xFF1B3D2B) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Input Form grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Date input
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CalendarToday, contentDescription = "Date", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Date",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = dateText,
                                onValueChange = { dateText = it },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White
                                )
                            )
                        }

                        // Odometer input
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Speed, contentDescription = "Odometer", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Odometer (km)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = odoText,
                                onValueChange = { odoText = it },
                                placeholder = { Text("e.g. 12450", fontSize = 13.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White
                                )
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Liters input
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocalGasStation, contentDescription = "Liters", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Liters",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = volumeText,
                                onValueChange = { volumeText = it },
                                placeholder = { Text("e.g. 5.2", fontSize = 13.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White
                                )
                            )
                        }

                        // Price input
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("₹", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Price (₹/L)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = priceText,
                                onValueChange = { priceText = it },
                                placeholder = { Text("e.g. 105", fontSize = 13.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White
                                )
                            )
                        }
                    }

                    // Optional Station details note
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Notes, contentDescription = "Notes", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Gas Station / Place (Optional)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedTextField(
                        value = notesText,
                        onValueChange = { notesText = it },
                        placeholder = { Text("e.g. HP Petrol Pump, NH44 Hub", fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )

                    // Live calculation cost highlight
                    if (computedCost > 0) {
                        Text(
                            text = "Estimated total refuel cost: " + formatCurrency(computedCost),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF0F9D58),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }

                    // Submit button
                    Button(
                        onClick = {
                            val odo = odoText.toDoubleOrNull()
                            val vol = volumeText.toDoubleOrNull()
                            val prc = priceText.toDoubleOrNull()
                            val amt = if (vol != null && prc != null) vol * prc else 0.0

                            if (vol != null && vol > 0 && odo != null && odo >= previousOdo && amt > 0) {
                                val stationName = notesText.ifBlank { "Gas Station Refuel" }
                                onAddFuelEntry(amt, vol, odo, stationName, selectedVehicle)

                                // Reset inputs
                                odoText = ""
                                volumeText = ""
                                priceText = ""
                                notesText = ""
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0F9D58)
                        )
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Refuel Button")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add fill-up", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }

        // 2. Metrics Block (Exactly styled matching screen visual layout)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Left Green Big Card: Current Month Spent Summary
                Card(
                    modifier = Modifier
                        .weight(1.1f)
                        .height(130.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF0F9D58)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "May 2026",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            fontWeight = FontWeight.Bold
                        )
                        Column {
                            Text(
                                text = formatCurrency(totalSpent),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${dec.format(totalVolume)} L · $fillUpsCount fill-up${if (fillUpsCount != 1) "s" else ""}",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                }

                // Right Column split in three small metrics cards matching standard layouts
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(130.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Avg Mileage Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("Avg mileage", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = if (averageFuelConsumption > 0) "${dec2.format(averageFuelConsumption)} km/L" else "—",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Total Spent Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("Total spent", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = formatCurrency(totalSpent),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Distance - Fuel Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("Distance · Fuel", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "${dec.format(totalDistanceDriven)} km",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Section Title: Historical Refuel logs for current vehicle choice
        item {
            Text(
                text = "${if (selectedVehicle == "BIKE") "Bike" else "Car"} Refueling History",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // Histoy Logs list items
        if (sortedDesc.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = "Empty",
                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No refuel records logged",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Use the form above to add a cycle!",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        } else {
            items(sortedDesc, key = { it.id }) { log ->
                val indexInAsc = sortedAsc.indexOf(log)
                val cycleEconomy = if (indexInAsc > 0) {
                    val prevLog = sortedAsc[indexInAsc - 1]
                    val dist = log.odometerReading - prevLog.odometerReading
                    if (dist > 0) {
                        val kmPerL = dist / log.volumeLiters
                        val l100k = (log.volumeLiters / dist) * 100.0
                        Pair(kmPerL, l100k)
                    } else null
                } else null

                FuelHistoryItem(
                    log = log,
                    cycleEconomy = cycleEconomy,
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
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formatter = remember { SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault()) }
    val dateString = formatter.format(Date(log.dateMillis))
    val dec = remember { DecimalFormat("#,##0.0") }
    val dec2 = remember { DecimalFormat("0.00") }

    var showDeleteConfirm by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            if (log.vehicleType == "BIKE") Color(0xFF26A69A).copy(alpha = 0.15f) else Color(0xFF2196F3).copy(alpha = 0.15f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (log.vehicleType == "BIKE") Icons.Default.TwoWheeler else Icons.Default.DirectionsCar,
                        contentDescription = log.vehicleType,
                        tint = if (log.vehicleType == "BIKE") Color(0xFF00796B) else Color(0xFF1976D2),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (log.note.isNotBlank()) log.note else "Gas Station Refuel",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
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
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
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

            // Mileage stats detail footer line
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = "Odometer",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Odometer: ",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${dec.format(log.odometerReading)} km",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (cycleEconomy != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.WaterDrop,
                            contentDescription = "Fuel economy",
                            tint = Color(0xFF0F9D58),
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${dec2.format(cycleEconomy.first)} km/L",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F9D58)
                        )
                    }
                } else {
                    Text(
                        text = "Calculating mileage...",
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                IconButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Refuel Log",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(12.dp)
                    )
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
