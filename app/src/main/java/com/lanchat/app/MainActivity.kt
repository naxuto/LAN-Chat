package com.lanchat.app

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lanchat.app.service.LanChatService
import com.lanchat.app.ui.chat.ChatScreen
import com.lanchat.app.ui.chat.ChatViewModel
import com.lanchat.app.ui.diagnostics.DiagnosticsScreen
import com.lanchat.app.ui.diagnostics.DiagnosticsViewModel
import com.lanchat.app.ui.home.HomeScreen
import com.lanchat.app.ui.home.HomeViewModel
import com.lanchat.app.ui.pairing.PairingScreen
import com.lanchat.app.ui.pairing.PairingViewModel
import com.lanchat.app.ui.theme.LanChatTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
        }

        val serviceIntent = Intent(this, LanChatService::class.java)
        startForegroundService(serviceIntent)

        setContent {
            LanChatTheme {
                MainNavigation()
            }
        }
    }
}

@Composable
fun MainNavigation() {
    val navController = rememberNavController()
    val app = LocalContext.current.applicationContext as LanChatApplication

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            val vm: HomeViewModel = viewModel(
                factory = HomeViewModel.Factory(app.peerRepository, app.networkMonitor)
            )
            HomeScreen(
                viewModel = vm,
                onPeerClick = { peerId -> navController.navigate("chat/$peerId") },
                onDiagnosticsClick = { navController.navigate("diagnostics") },
                onPairingClick = { navController.navigate("pairing") },
                onSettingsClick = { navController.navigate("diagnostics") }
            )
        }
        composable("chat/{peerId}") { backStackEntry ->
            val peerId = backStackEntry.arguments?.getString("peerId") ?: ""
            val vm: ChatViewModel = viewModel(
                factory = ChatViewModel.Factory(
                    peerId,
                    app.peerRepository,
                    app.messageRepository,
                    app.connectionManager
                )
            )
            ChatScreen(viewModel = vm, onBackClick = { navController.popBackStack() })
        }
        composable("diagnostics") {
            val vm: DiagnosticsViewModel = viewModel(
                factory = DiagnosticsViewModel.Factory(app.networkMonitor, app.peerRepository)
            )
            DiagnosticsScreen(viewModel = vm, onBackClick = { navController.popBackStack() })
        }
        composable("pairing") {
            val vm: PairingViewModel = viewModel(
                factory = PairingViewModel.Factory(app.userRepository, app.peerRepository)
            )
            PairingScreen(viewModel = vm, onBackClick = { navController.popBackStack() })
        }
    }
}
