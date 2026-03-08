package com.zerothreat.core.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zerothreat.core.data.db.AppDatabase
import com.zerothreat.core.data.db.AllScannedUrl
import com.zerothreat.core.data.db.BlockedUrl
import com.zerothreat.core.data.repository.UrlRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class UrlViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: UrlRepository

    val recentScans: StateFlow<List<AllScannedUrl>>
    val blockedUrls: StateFlow<List<BlockedUrl>>
    val threatCount: StateFlow<Int>
    val totalScans: StateFlow<Int>

    init {
        val urlDao = AppDatabase.getDatabase(application).urlDao()
        repository = UrlRepository(urlDao)

        recentScans = repository.allUrls
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

        blockedUrls = repository.allBlockedUrls
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
            
        threatCount = repository.threatCount
            .stateIn(viewModelScope, SharingStarted.Lazily, 0)
            
        totalScans = repository.totalScans
            .stateIn(viewModelScope, SharingStarted.Lazily, 0)

        // Normalize existing ID gaps (e.g., 1,3,4,8 -> 1,2,3,4) in scan order.
        viewModelScope.launch {
            repository.normalizeAllUrlIds()
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun deleteUrl(id: Int) {
        viewModelScope.launch {
            repository.deleteUrl(id)
        }
    }
}
