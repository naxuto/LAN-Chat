package com.lanchat.app.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lanchat.app.data.entities.MessageEntity
import com.lanchat.app.data.entities.PeerEntity
import com.lanchat.app.network.ConnectionManager
import com.lanchat.app.repository.MessageRepository
import com.lanchat.app.repository.PeerRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModel(
    private val peerDeviceId: String,
    private val peerRepository: PeerRepository,
    private val messageRepository: MessageRepository,
    private val connectionManager: ConnectionManager
) : ViewModel() {

    private val _peer = MutableStateFlow<PeerEntity?>(null)
    val peer: StateFlow<PeerEntity?> = _peer.asStateFlow()

    val messages: StateFlow<List<MessageEntity>> = _peer.flatMapLatest { p ->
        if (p != null) {
            val convId = messageRepository.ensureDirectConversation(p.deviceId, p.displayName)
            messageRepository.getMessages(convId)
        } else {
            MutableStateFlow(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            _peer.value = peerRepository.getPeer(peerDeviceId)
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            connectionManager.sendDirectMessage(peerDeviceId, text.trim())
        }
    }

    class Factory(
        private val peerDeviceId: String,
        private val peerRepository: PeerRepository,
        private val messageRepository: MessageRepository,
        private val connectionManager: ConnectionManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ChatViewModel(peerDeviceId, peerRepository, messageRepository, connectionManager) as T
        }
    }
}
