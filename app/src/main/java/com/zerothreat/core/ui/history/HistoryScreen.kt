package com.zerothreat.core.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zerothreat.core.ui.theme.*
import com.zerothreat.core.ui.viewmodel.UrlViewModel
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: UrlViewModel = viewModel()
) {
    val hazeState = LocalUiSurfaceState.current
    val history by viewModel.recentScans.collectAsState()
    val sortedHistory = remember(history) { history.sortedBy { it.id } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .appBackground(hazeState)
    ) {
        // Top Bar
        CenterAlignedTopAppBar(
            modifier = Modifier.appContainer(
                hazeState = hazeState,
                cornerRadius = 24.dp,
                thin = true
            ),
            title = {
                Text(
                    "Scan History",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = Color.Transparent
            ),
            actions = {
                IconButton(onClick = { viewModel.clearHistory() }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Clear History",
                        tint = TextSecondary
                    )
                }
            }
        )

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(sortedHistory) { scan ->
                // Reuse DatabaseItem logic (simplified view)
                com.zerothreat.core.ui.database.DatabaseItem(
                    item = scan,
                    onDelete = { viewModel.deleteUrl(scan.id) }
                )
            }
            
            if (sortedHistory.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No scans yet",
                            color = TextWhite
                        )
                    }
                }
            }
        }
    }
}
