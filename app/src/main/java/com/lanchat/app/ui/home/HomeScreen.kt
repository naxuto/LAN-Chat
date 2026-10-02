package com.lanchat.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lanchat.app.data.entities.PeerEntity
import com.lanchat.app.data.entities.PeerState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onPeerClick: (String) -> Unit,
    onDiagnosticsClick: () -> Unit,
    onPairingClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val peers by viewModel.peers.collectAsState()
    val networkStatus by viewModel.networkStatus.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("LAN Chat") },
                actions = {
                    IconButton(onClick = onPairingClick) {
                        Icon(Icons.Default.QrCode, contentDescription = "Pairing")
                    }
                    IconButton(onClick = onDiagnosticsClick) {
                        Icon(Icons.Default.Build, contentDescription = "Diagnostics")
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            NetworkHeader(networkStatus.isLanAvailable)

            if (!networkStatus.isLanAvailable) {
                ApIsolationBanner()
            }

            Text(
                text = "Discovered Devices",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(peers) { peer ->
                    PeerItem(peer = peer, onClick = { onPeerClick(peer.deviceId) })
                }
            }
        }
    }
}

@Composable
fun NetworkHeader(isAvailable: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isAvailable) Color(0xFFE8F5E9) else Color(0xFFFFEBEE))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(
                    if (isAvailable) Color(0xFF4CAF50) else Color(0xFFF44336),
                    CircleShape
                )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (isAvailable) "Local Network Connected" else "Local Network Unavailable - Connect Wi-Fi",
            style = MaterialTheme.typography.bodyMedium,
            color = if (isAvailable) Color(0xFF2E7D32) else Color(0xFFC62828)
        )
    }
}

@Composable
fun ApIsolationBanner() {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Devices are connected to Wi-Fi, but device-to-device communication appears to be blocked by the router.\n\nDisable AP Isolation / Client Isolation or use a normal LAN SSID.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(12.dp)
        )
    }
}

@Composable
fun PeerItem(peer: PeerEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(
                        if (peer.state == PeerState.ONLINE) Color(0xFF4CAF50) else Color.Gray,
                        CircleShape
                    )
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = peer.displayName, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = peer.ipAddress, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
    }
}
