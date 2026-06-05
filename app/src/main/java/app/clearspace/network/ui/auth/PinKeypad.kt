package app.clearspace.network.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.draw.scale
import androidx.compose.material3.ripple

@Composable
fun PinKeypad(
    modifier: Modifier = Modifier,
    onPinChange: (String) -> Unit,
    onSubmit: () -> Unit,
    currentPin: String = ""
) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9")
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Pin display dots
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(vertical = 16.dp)
        ) {
            for (i in 0 until maxOf(currentPin.length, 4)) {
                val isFilled = i < currentPin.length
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(
                            if (isFilled) MaterialTheme.colorScheme.primary 
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                )
            }
        }

        // Keys
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(0.8f),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { digit ->
                    NumpadButton(
                        text = digit,
                        onClick = { onPinChange(currentPin + digit) }
                    )
                }
            }
        }

        // Bottom row
        Row(
            modifier = Modifier.fillMaxWidth(0.8f),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            NumpadButton(
                icon = Icons.AutoMirrored.Filled.Backspace,
                onClick = {
                    if (currentPin.isNotEmpty()) {
                        onPinChange(currentPin.dropLast(1))
                    }
                }
            )
            NumpadButton(
                text = "0",
                onClick = { onPinChange(currentPin + "0") }
            )
            NumpadButton(
                icon = Icons.Default.Check,
                onClick = onSubmit,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
private fun NumpadButton(
    modifier: Modifier = Modifier,
    text: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    containerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surfaceVariant,
    contentColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: () -> Unit
) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessLow
        ),
        label = "button_scale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(72.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(containerColor)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = contentColor),
                onClick = onClick
            )
    ) {
        if (text != null) {
            Text(text = text, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = contentColor)
        } else if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(32.dp))
        }
    }
}
