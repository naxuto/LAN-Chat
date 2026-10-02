package com.lanchat.app.ui.diagnostics

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lanchat.app.data.entities.PeerState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsScreen(
    viewModel: DiagnosticsViewModel,
    onBackClick: () -> Unit
) {
    val networkStatus by viewModel.networkStatus.collectAsState()
    val peers by viewModel.peers.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Network Diagnostics") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Wi-Fi: ${if (networkStatus.isWifiConnected) "Connected" else "Disconnected"}")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Local IP: ${networkStatus.localIp}")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Gateway: ${networkStatus.gatewayIp}")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Local Network: ${if (networkStatus.isLanAvailable) "Available" else "Unavailable"}")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Internet: Not Required")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("LAN Chat: Ready")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Online Peers: ${peers.count { it.state == PeerState.ONLINE }}")
                }
            }
        }
    }
}
