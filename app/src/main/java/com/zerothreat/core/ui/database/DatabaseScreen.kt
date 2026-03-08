package com.zerothreat.core.ui.database

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zerothreat.core.data.db.AllScannedUrl
import com.zerothreat.core.ui.theme.*
import com.zerothreat.core.ui.viewmodel.UrlViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatabaseScreen(
    viewModel: UrlViewModel = viewModel()
) {
    val hazeState = LocalUiSurfaceState.current
    val allUrls by viewModel.recentScans.collectAsState() // This is actually allUrls flow from repo
    val sortedUrls = remember(allUrls) { allUrls.sortedBy { it.id } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .appBackground(hazeState)
    ) {
        // Header
        CenterAlignedTopAppBar(
            modifier = Modifier.appContainer(
                hazeState = hazeState,
                cornerRadius = 24.dp,
                thin = true
            ),
            title = {
                Text(
                    "Database Records",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = Color.Transparent
            )
        )

        // Count Summary
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Total Records: ${sortedUrls.size}",
                color = TextWhite,
                style = MaterialTheme.typography.bodyMedium
            )
            TextButton(onClick = { viewModel.clearHistory() }) {
                Text("Clear All", color = DangerRed)
            }
        }

        // List
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(sortedUrls) { item ->
                DatabaseItem(
                    item = item,
                    onDelete = { viewModel.deleteUrl(item.id) }
                )
            }
            
            if (sortedUrls.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        Text("Database is empty", color = TextWhite)
                    }
                }
            }
        }
    }
}

@Composable
fun DatabaseItem(
    item: AllScannedUrl,
    onDelete: () -> Unit
) {
    val hazeState = LocalUiSurfaceState.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(112.dp)
            .appContainer(hazeState = hazeState, cornerRadius = 24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ID Badge
            Surface(
                color = ElectricPurple.copy(alpha = 0.16f),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = "#${item.id}",
                    color = ElectricPurple,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.domain,
                        modifier = Modifier.weight(1f),
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    // Status Badge
                    val (badgeColor, badgeText) = when (item.result) {
                        com.zerothreat.core.detector.PhishingResult.SAFE -> SafeGreen to "SAFE"
                        com.zerothreat.core.detector.PhishingResult.SUSPICIOUS -> WarningYellow to "SUSPICIOUS"
                        com.zerothreat.core.detector.PhishingResult.PHISHING -> DangerRed to "PHISHING"
                    }
                    
                    Surface(
                        color = badgeColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = badgeText,
                            color = badgeColor,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
                
                Text(
                    text = item.url,
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Timestamp + Source
                    val date = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(item.timestamp))
                    Text(
                        text = "$date • ${item.source}",
                        color = TextMuted,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
