// File: app/src/main/java/com/zerothreat/app/ui/info/InfoScreens.kt
package com.zerothreat.app.ui.info

import androidx.compose.foundation.background
import androidx.compose.foundation. layout.*
import androidx.compose. foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose. material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime. Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui. text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui. unit.sp
import com.zerothreat.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Privacy Policy",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground
                )
            )
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                InfoSection(
                    title = "Data Collection",
                    content = "ZeroThreat does NOT collect, store, or transmit any personal data. All threat analysis is performed locally on your device."
                )
            }

            item { Spacer(modifier = Modifier. height(16.dp)) }

            item {
                InfoSection(
                    title = "Local Processing",
                    content = "All URL scanning, threat detection, and security analysis happens entirely on your device. No data leaves your phone."
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            item {
                InfoSection(
                    title = "Permissions",
                    content = "• Notification Access: Only used to scan text for phishing links.  We do not read, store, or transmit notification content.\n\n• VPN Service: Used to monitor outgoing network requests. No data is logged or sent to external servers."
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            item {
                InfoSection(
                    title = "Third-Party Services",
                    content = "ZeroThreat does not use any third-party analytics, advertising, or tracking services."
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            item {
                InfoSection(
                    title = "Updates",
                    content = "Threat databases are updated locally via WiFi.  Only blacklist data is downloaded—no personal information is uploaded."
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            item {
                Text(
                    text = "Last updated: January 2026",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "About ZeroThreat",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground
                )
            )
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = CardBackground
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = "ZeroThreat",
                            style = MaterialTheme.typography. headlineMedium,
                            color = ElectricPurple,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier. height(8.dp))
                        Text(
                            text = "Version 1.0.0",
                            style = MaterialTheme.typography. bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }

            item {
                InfoSection(
                    title = "What is ZeroThreat?",
                    content = "ZeroThreat is an AI-powered Android application designed to protect you from phishing attacks, malicious links, and online scams in real-time."
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            item {
                InfoSection(
                    title = "Team",
                    content = "Developed by:\n• Hanna - Frontend Development\n• Keerthana - UI/UX Design\n• Sharon - Database & Testing\n• Stacey - Backend Development"
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            item {
                InfoSection(
                    title = "Technology",
                    content = "Built with:\n• Kotlin\n• Jetpack Compose\n• Material Design 3\n• Local ML Models\n• PhishTank & URLHaus Databases"
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            item {
                InfoSection(
                    title = "Mission",
                    content = "Our mission is to make mobile users safer by providing real-time, privacy-first protection against phishing and cyber threats."
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            item {
                Text(
                    text = "© 2026 ZeroThreat. All rights reserved.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api:: class)
@Composable
fun HelpScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Help & Support",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default. ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground
                )
            )
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                InfoSection(
                    title = "How to Use ZeroThreat",
                    content = "1. Choose between Manual or Smart Mode\n2. In Manual Mode, paste URLs to check them\n3. In Smart Mode, enable notifications and link monitoring\n4. ZeroThreat will alert you when threats are detected"
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            item {
                InfoSection(
                    title = "FAQs",
                    content = "Q: Does ZeroThreat use internet?\nA: Only for updating threat databases.  All scanning is local.\n\nQ: Why does it need VPN permission?\nA: To monitor network traffic for malicious links. No data is sent externally.\n\nQ: Is my data safe?\nA: Yes!  Everything stays on your device."
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            item {
                InfoSection(
                    title = "Troubleshooting",
                    content = "• App not detecting threats:  Make sure Smart Mode is enabled\n• Notifications not working: Check notification permissions in Settings\n• VPN issues:  Disable other VPN apps while using ZeroThreat"
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            item {
                InfoSection(
                    title = "Contact Support",
                    content = "For help or bug reports, contact:\nzerothreat. support@example.com"
                )
            }
        }
    }
}

@Composable
fun InfoSection(title: String, content: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = CardBackground
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = ElectricPurple,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                lineHeight = 22.sp
            )
        }
    }
}