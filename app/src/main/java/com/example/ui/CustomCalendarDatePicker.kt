package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CustomCalendarDatePicker(
    selectedDateMillis: Long,
    onDateSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val initialCal = remember(selectedDateMillis) {
        Calendar.getInstance().apply {
            timeInMillis = selectedDateMillis
        }
    }

    // Explicitly track the active view year and month
    var currentMonth by remember { mutableStateOf(initialCal.get(Calendar.MONTH)) }
    var currentYear by remember { mutableStateOf(initialCal.get(Calendar.YEAR)) }

    val monthName = remember(currentMonth) {
        val sdf = SimpleDateFormat("MMMM", Locale.getDefault())
        val tempCal = Calendar.getInstance()
        tempCal.set(Calendar.MONTH, currentMonth)
        sdf.format(tempCal.time)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 8.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .width(320.dp)
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Header Row (Visual Matches Screenshot)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$monthName $currentYear",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Month Dropdown",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Previous Month button (Up Arrow matching screenshot)
                        IconButton(
                            onClick = {
                                if (currentMonth == 0) {
                                    currentMonth = 11
                                    currentYear -= 1
                                } else {
                                    currentMonth -= 1
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = "Previous Month",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Next Month button (Down Arrow matching screenshot)
                        IconButton(
                            onClick = {
                                if (currentMonth == 11) {
                                    currentMonth = 0
                                    currentYear += 1
                                } else {
                                    currentMonth += 1
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = "Next Month",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Days of week header row (Su, Mo, Tu, We, Th, Fr, Sa)
                val dayHeaders = listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa")
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    dayHeaders.forEach { header ->
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = header,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 6.dp))

                // Build days grid (42 days representation)
                val daysList = remember(currentMonth, currentYear) {
                    getCalendarGridDays(currentMonth, currentYear)
                }

                // Selected Date Day/Month/Year logic
                val selCal = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
                val selDay = selCal.get(Calendar.DAY_OF_MONTH)
                val selMonth = selCal.get(Calendar.MONTH)
                val selYear = selCal.get(Calendar.YEAR)

                // 6 rows of 7 columns grid rendering
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (row in 0 until 6) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            for (col in 0 until 7) {
                                val index = row * 7 + col
                                if (index < daysList.size) {
                                    val gridDay = daysList[index]
                                    val isCurrentMonth = gridDay.month == currentMonth && gridDay.year == currentYear
                                    val isSelected = gridDay.day == selDay && gridDay.month == selMonth && gridDay.year == selYear

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .padding(1.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                                            )
                                            .clickable {
                                                val outCal = Calendar.getInstance().apply {
                                                    set(Calendar.YEAR, gridDay.year)
                                                    set(Calendar.MONTH, gridDay.month)
                                                    set(Calendar.DAY_OF_MONTH, gridDay.day)
                                                    set(Calendar.HOUR_OF_DAY, 12)
                                                    set(Calendar.MINUTE, 0)
                                                    set(Calendar.SECOND, 0)
                                                    set(Calendar.MILLISECOND, 0)
                                                }
                                                onDateSelected(outCal.timeInMillis)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = gridDay.day.toString(),
                                            fontWeight = if (isSelected) FontWeight.ExtraBold else if (isCurrentMonth) FontWeight.SemiBold else FontWeight.Normal,
                                            color = if (isSelected) {
                                                MaterialTheme.colorScheme.onPrimary
                                            } else if (isCurrentMonth) {
                                                MaterialTheme.colorScheme.onSurface
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                            },
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Spacer(modifier = Modifier.height(8.dp))

                // Bottom actions row ("Clear" on left, "Today" on right)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            onDateSelected(System.currentTimeMillis())
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Clear",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    TextButton(
                        onClick = {
                            onDateSelected(System.currentTimeMillis())
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Today",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

data class GridDay(val day: Int, val month: Int, val year: Int)

fun getCalendarGridDays(month: Int, year: Int): List<GridDay> {
    val list = mutableListOf<GridDay>()
    
    // First day of target Month
    val cal = Calendar.getInstance().apply {
        clear()
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month)
        set(Calendar.DAY_OF_MONTH, 1)
    }

    // 1 = Sunday, 2 = Monday ... 7 = Saturday
    val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
    
    // Fill prev month days
    val prevMonthCal = Calendar.getInstance().apply {
        clear()
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month)
        set(Calendar.DAY_OF_MONTH, 1)
        add(Calendar.MONTH, -1)
    }
    val maxPrevDays = prevMonthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val prefixDaysCount = firstDayOfWeek - 1

    for (i in (maxPrevDays - prefixDaysCount + 1)..maxPrevDays) {
        list.add(GridDay(i, prevMonthCal.get(Calendar.MONTH), prevMonthCal.get(Calendar.YEAR)))
    }

    // Fill current month days
    val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    for (i in 1..maxDays) {
        list.add(GridDay(i, month, year))
    }

    // Fill next month days
    val nextMonthCal = Calendar.getInstance().apply {
        clear()
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month)
        set(Calendar.DAY_OF_MONTH, 1)
        add(Calendar.MONTH, 1)
    }
    val suffixDaysCount = 42 - list.size
    for (i in 1..suffixDaysCount) {
        list.add(GridDay(i, nextMonthCal.get(Calendar.MONTH), nextMonthCal.get(Calendar.YEAR)))
    }

    return list
}
