package app.clearspace.network.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class KeyboardState {
    LOWERCASE, UPPERCASE, SYMBOLS
}

@Composable
fun SecureKeyboard(
    modifier: Modifier = Modifier,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    currentPassword: String = ""
) {
    var keyboardState by remember { mutableStateOf(KeyboardState.LOWERCASE) }

    val lowercaseRows = listOf(
        listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
        listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
        listOf("z", "x", "c", "v", "b", "n", "m")
    )

    val uppercaseRows = listOf(
        listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P"),
        listOf("A", "S", "D", "F", "G", "H", "J", "K", "L"),
        listOf("Z", "X", "C", "V", "B", "N", "M")
    )

    val symbolRows = listOf(
        listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
        listOf("@", "#", "$", "%", "&", "-", "+", "(", ")"),
        listOf("*", "\"", "'", ":", ";", "!", "?")
    )

    val activeRows = when (keyboardState) {
        KeyboardState.LOWERCASE -> lowercaseRows
        KeyboardState.UPPERCASE -> uppercaseRows
        KeyboardState.SYMBOLS -> symbolRows
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(4.dp)
            .background(MaterialTheme.colorScheme.surface),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Password Display Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "•".repeat(currentPassword.length),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Keys
        activeRows.forEachIndexed { rowIndex, row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                if (rowIndex == 2) {
                    // Add Shift key on 3rd row
                    KeyButton(
                        icon = Icons.Default.ArrowUpward,
                        isActive = keyboardState == KeyboardState.UPPERCASE,
                        onClick = {
                            keyboardState = if (keyboardState == KeyboardState.UPPERCASE) KeyboardState.LOWERCASE else KeyboardState.UPPERCASE
                        },
                        modifier = Modifier.weight(1.5f).padding(horizontal = 2.dp)
                    )
                }

                row.forEach { key ->
                    KeyButton(
                        text = key,
                        onClick = { onPasswordChange(currentPassword + key) },
                        modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
                    )
                }

                if (rowIndex == 2) {
                    // Add Backspace on 3rd row
                    KeyButton(
                        icon = Icons.AutoMirrored.Filled.Backspace,
                        onClick = {
                            if (currentPassword.isNotEmpty()) {
                                onPasswordChange(currentPassword.dropLast(1))
                            }
                        },
                        modifier = Modifier.weight(1.5f).padding(horizontal = 2.dp)
                    )
                }
            }
        }

        // Bottom row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            KeyButton(
                text = if (keyboardState == KeyboardState.SYMBOLS) "ABC" else "?123",
                onClick = {
                    keyboardState = if (keyboardState == KeyboardState.SYMBOLS) KeyboardState.LOWERCASE else KeyboardState.SYMBOLS
                },
                modifier = Modifier.weight(2f).padding(horizontal = 2.dp)
            )
            
            KeyButton(
                text = "SPACE",
                onClick = { onPasswordChange(currentPassword + " ") },
                modifier = Modifier.weight(5f).padding(horizontal = 2.dp)
            )

            KeyButton(
                icon = Icons.Default.Check,
                onClick = onSubmit,
                modifier = Modifier.weight(2f).padding(horizontal = 2.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
private fun KeyButton(
    modifier: Modifier = Modifier,
    text: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    isActive: Boolean = false,
    containerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surfaceVariant,
    contentColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: () -> Unit
) {
    val bg = if (isActive) MaterialTheme.colorScheme.primaryContainer else containerColor
    val fg = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else contentColor

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(54.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .clickable(onClick = onClick)
    ) {
        if (text != null) {
            Text(text = text, fontSize = 20.sp, color = fg)
        } else if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, tint = fg)
        }
    }
}
