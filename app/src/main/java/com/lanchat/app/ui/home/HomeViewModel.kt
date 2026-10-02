package com.lanchat.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lanchat.app.data.entities.PeerEntity
import com.lanchat.app.network.NetworkMonitor
import com.lanchat.app.network.NetworkStatus
import com.lanchat.app.repository.PeerRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(
    peerRepository: PeerRepository,
    networkMonitor: NetworkMonitor
) : ViewModel() {

    val peers: StateFlow<List<PeerEntity>> = peerRepository.allPeers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val networkStatus: StateFlow<NetworkStatus> = networkMonitor.networkStatus
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NetworkStatus())

    class Factory(
        private val peerRepository: PeerRepository,
        private val networkMonitor: NetworkMonitor
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(peerRepository, networkMonitor) as T
        }
    }
}
