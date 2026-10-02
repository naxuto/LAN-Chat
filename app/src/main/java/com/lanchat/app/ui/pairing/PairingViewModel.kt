package com.lanchat.app.ui.pairing

import android.graphics.Bitmap
import android.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.lanchat.app.repository.PeerRepository
import com.lanchat.app.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class PairingViewModel(
    private val userRepository: UserRepository,
    private val peerRepository: PeerRepository
) : ViewModel() {

    private val _qrBitmap = MutableStateFlow<Bitmap?>(null)
    val qrBitmap: StateFlow<Bitmap?> = _qrBitmap.asStateFlow()

    init {
        generateUserQr()
    }

    private fun generateUserQr() {
        viewModelScope.launch {
            val user = userRepository.getOrCreateUser()
            val json = JSONObject().apply {
                put("deviceId", user.deviceId)
                put("displayName", user.displayName)
                put("publicKey", user.publicKey)
            }.toString()

            _qrBitmap.value = createQrBitmap(json)
        }
    }

    private suspend fun createQrBitmap(content: String): Bitmap = withContext(Dispatchers.Default) {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 512, 512)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bmp.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
            }
        }
        bmp
    }

    fun processScannedQr(rawJson: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val obj = JSONObject(rawJson)
                val deviceId = obj.getString("deviceId")
                peerRepository.setTrusted(deviceId, true)
                onResult(true)
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }

    class Factory(
        private val userRepository: UserRepository,
        private val peerRepository: PeerRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PairingViewModel(userRepository, peerRepository) as T
        }
    }
}
