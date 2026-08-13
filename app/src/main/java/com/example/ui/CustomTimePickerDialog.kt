package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.util.Calendar
import java.util.Locale

@Composable
fun CustomTimePickerDialog(
    initialTimeMillis: Long,
    onTimeSelected: (hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val calendar = remember(initialTimeMillis) {
        Calendar.getInstance().apply { timeInMillis = initialTimeMillis }
    }
    
    val initialHour24 = calendar.get(Calendar.HOUR_OF_DAY)
    val initialMinute = calendar.get(Calendar.MINUTE)
    
    var hour12 by remember {
        mutableStateOf(
            when {
                initialHour24 == 0 -> 12
                initialHour24 > 12 -> initialHour24 - 12
                else -> initialHour24
            }
        )
    }
    var minute by remember { mutableStateOf(initialMinute) }
    var isAm by remember { mutableStateOf(initialHour24 < 12) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 6.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .width(320.dp)
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title
                Text(
                    text = "Select Time",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.align(Alignment.Start)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Active Display / Time Adjuster Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Hour Column
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = {
                                hour12 = if (hour12 == 12) 1 else hour12 + 1
                            },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Increase Hour", modifier = Modifier.size(20.dp))
                        }
                        
                        Text(
                            text = String.format(Locale.getDefault(), "%02d", hour12),
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(60.dp)
                        )
                        
                        IconButton(
                            onClick = {
                                hour12 = if (hour12 == 1) 12 else hour12 - 1
                            },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease Hour", modifier = Modifier.size(20.dp))
                        }
                    }
                    
                    // Separator
                    Text(
                        text = ":",
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    // Minute Column
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = {
                                minute = (minute + 1) % 60
                            },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Increase Minute", modifier = Modifier.size(20.dp))
                        }
                        
                        Text(
                            text = String.format(Locale.getDefault(), "%02d", minute),
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(60.dp)
                        )
                        
                        IconButton(
                            onClick = {
                                minute = if (minute == 0) 59 else minute - 1
                            },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease Minute", modifier = Modifier.size(20.dp))
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(4.dp))
                    
                    // AM / PM Column Selector
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Button(
                            onClick = { isAm = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isAm) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isAm) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier
                                .width(54.dp)
                                .height(36.dp)
                        ) {
                            Text("AM", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        
                        Button(
                            onClick = { isAm = false },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!isAm) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (!isAm) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier
                                .width(54.dp)
                                .height(36.dp)
                        ) {
                            Text("PM", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(28.dp))
                
                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            var finalHour24 = if (hour12 == 12) 0 else hour12
                            if (!isAm) {
                                finalHour24 += 12
                            }
                            onTimeSelected(finalHour24, minute)
                        }
                    ) {
                        Text("OK")
                    }
                }
            }
        }
    }
}
