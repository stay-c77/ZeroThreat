package com.zerothreat.app.ui.manual

import androidx.compose.foundation.background
import androidx.compose.foundation. layout.*
import androidx.compose. foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text. KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons. filled.*
import androidx.compose. material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose. ui.text.style.TextAlign
import androidx.compose.ui. unit.dp
import androidx.compose.ui.unit.sp
import com.zerothreat.app. ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualCheckScreen(
    onCheckUrl: (String) -> Unit
) {
    var urlInput by remember { mutableStateOf("") }
    var isChecking by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier. height(32.dp))

        Icon(
            imageVector = Icons. Default.Search,
            contentDescription = "Check URL",
            tint = ElectricPurple,
            modifier = Modifier.size(80.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Check a Link",
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Paste any URL below to check if it's safe",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // URL Input Field
        OutlinedTextField(
            value = urlInput,
            onValueChange = { urlInput = it },
            modifier = Modifier. fillMaxWidth(),
            placeholder = {
                Text("https://example.com", color = TextMuted)
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons. Default.Link,
                    contentDescription = "URL",
                    tint = ElectricPurple
                )
            },
            trailingIcon = {
                if (urlInput.isNotEmpty()) {
                    IconButton(onClick = { urlInput = "" }) {
                        Icon(
                            imageVector = Icons.Default. Clear,
                            contentDescription = "Clear",
                            tint = TextSecondary
                        )
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ElectricPurple,
                unfocusedBorderColor = TextMuted,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = ElectricPurple
            ),
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (urlInput.isNotEmpty()) {
                        onCheckUrl(urlInput)
                    }
                }
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Check Button
        Button(
            onClick = {
                if (urlInput.isNotEmpty()) {
                    isChecking = true
                    onCheckUrl(urlInput)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = urlInput.isNotEmpty() && !isChecking,
            colors = ButtonDefaults.buttonColors(
                containerColor = ElectricPurple,
                disabledContainerColor = TextMuted
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isChecking) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = TextPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "Check",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Check Link",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Info Card
        Card(
            modifier = Modifier. fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = CardBackground
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Info",
                        tint = ElectricPurple,
                        modifier = Modifier. size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "How it works",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                InfoItem("✓ Checks against known phishing databases")
                InfoItem("✓ Analyzes URL patterns and keywords")
                InfoItem("✓ Provides instant threat assessment")
                InfoItem("✓ 100% private - all checks are local")
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Privacy Note
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Privacy",
                tint = ElectricPurple,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "All analysis is performed locally on your device",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun InfoItem(text: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            lineHeight = 20.sp
        )
    }
}