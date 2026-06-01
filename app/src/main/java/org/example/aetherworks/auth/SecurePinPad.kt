package org.example.aetherworks.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * SecurePinPad
 * A custom, randomized, in-app keypad designed to bypass the Android system keyboard (e.g., Gboard).
 * Prevents third-party keyboards from tracking the user's PIN/Password.
 */
@Composable
fun SecurePinPad(
    modifier: Modifier = Modifier,
    onPinComplete: (CharArray) -> Unit,
    pinLength: Int = 6,
    randomizeLayout: Boolean = true
) {
    var pin by remember { mutableStateOf(CharArray(0)) }
    
    // Generate layout on first composition. If randomizeLayout is true, shuffle the numbers 0-9.
    val keypadLayout = remember(randomizeLayout) {
        val numbers = (0..9).toList().let {
            if (randomizeLayout) it.shuffled() else it
        }
        numbers
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // PIN Display
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(bottom = 48.dp)
        ) {
            for (i in 0 until pinLength) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(
                            if (i < pin.size) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                )
            }
        }

        // Keypad Grid
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Rows 1-3
            for (row in 0..2) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    for (col in 0..2) {
                        val number = keypadLayout[row * 3 + col]
                        PinButton(
                            text = number.toString(),
                            onClick = {
                                if (pin.size < pinLength) {
                                    val newPin = pin.copyOf(pin.size + 1)
                                    newPin[pin.size] = number.toString()[0]
                                    pin = newPin
                                    
                                    if (pin.size == pinLength) {
                                        onPinComplete(pin)
                                        // Reset immediately after passing it up
                                        pin = CharArray(0)
                                    }
                                }
                            }
                        )
                    }
                }
            }
            
            // Row 4 (Empty, Zero, Backspace)
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Spacer(modifier = Modifier.size(72.dp))
                
                PinButton(
                    text = keypadLayout[9].toString(),
                    onClick = {
                        if (pin.size < pinLength) {
                            val newPin = pin.copyOf(pin.size + 1)
                            newPin[pin.size] = keypadLayout[9].toString()[0]
                            pin = newPin
                            
                            if (pin.size == pinLength) {
                                onPinComplete(pin)
                                pin = CharArray(0)
                            }
                        }
                    }
                )
                
                IconButton(
                    onClick = {
                        if (pin.isNotEmpty()) {
                            val newPin = CharArray(pin.size - 1)
                            System.arraycopy(pin, 0, newPin, 0, pin.size - 1)
                            pin = newPin
                        }
                    },
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                        contentDescription = "Backspace",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun PinButton(
    text: String,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
    ) {
        Text(
            text = text,
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
