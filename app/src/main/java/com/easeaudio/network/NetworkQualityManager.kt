package com.easeaudio.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class QualityLevel {
    BEST_QUALITY_HD,      // Wi-Fi or fast 5G (Max audio bitrate, low latency buffer)
    BALANCED_ADAPTIVE,    // Standard Cellular (Moderate buffer, 128kbps)
    SAVER_SMOOTH          // Slow Network/Metered (High buffer capacity to prevent stalling)
}

data class NetworkStatus(
    val isConnected: Boolean = true,
    val isWifi: Boolean = true,
    val isMetered: Boolean = false,
    val qualityLevel: QualityLevel = QualityLevel.BEST_QUALITY_HD,
    val label: String = "Wi-Fi (HD Quality)",
    val minBufferMs: Int = 10000,
    val maxBufferMs: Int = 30000
)

class NetworkQualityManager(private val context: Context) {

    private val TAG = "NetworkQualityManager"
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _networkStatus = MutableStateFlow(evaluateCurrentNetwork())
    val networkStatus: StateFlow<NetworkStatus> = _networkStatus.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main)

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            updateNetworkStatus()
        }

        override fun onLost(network: Network) {
            updateNetworkStatus()
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            updateNetworkStatus()
        }
    }

    init {
        registerNetworkCallback()
    }

    private fun registerNetworkCallback() {
        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager.registerNetworkCallback(request, networkCallback)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register network callback: ${e.message}")
        }
    }

    fun unregisterNetworkCallback() {
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to unregister network callback: ${e.message}")
        }
    }

    private fun updateNetworkStatus() {
        scope.launch {
            _networkStatus.value = evaluateCurrentNetwork()
        }
    }

    private fun evaluateCurrentNetwork(): NetworkStatus {
        return try {
            val activeNetwork = connectivityManager.activeNetwork
            val caps = connectivityManager.getNetworkCapabilities(activeNetwork)

            if (activeNetwork == null || caps == null ||
                !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            ) {
                return NetworkStatus(
                    isConnected = false,
                    isWifi = false,
                    isMetered = true,
                    qualityLevel = QualityLevel.SAVER_SMOOTH,
                    label = "Offline / Low Signal",
                    minBufferMs = 30000,
                    maxBufferMs = 90000
                )
            }

            val isWifi = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            val isMetered = !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
            val linkDownstreamKbps = caps.linkDownstreamBandwidthKbps

            val (quality, label, minBuf, maxBuf) = when {
                isWifi -> {
                    Quadruple(
                        QualityLevel.BEST_QUALITY_HD,
                        "Wi-Fi (HD Audio)",
                        8000,
                        25000
                    )
                }
                // Bug #9: Check bandwidth BEFORE isMetered so that fast 5G on a metered
                // plan is not incorrectly downgraded. On most phones all cellular is metered,
                // so the old isMetered branch would always fire first, overriding the fast-path.
                linkDownstreamKbps > 5000 -> {
                    Quadruple(
                        QualityLevel.BEST_QUALITY_HD,
                        "Fast 5G/4G (HD)",
                        12000,
                        35000
                    )
                }
                linkDownstreamKbps in 1000..5000 -> {
                    Quadruple(
                        QualityLevel.BALANCED_ADAPTIVE,
                        "Cellular (Smooth Buffer)",
                        20000,
                        50000
                    )
                }
                isMetered -> {
                    // Slow metered connection
                    Quadruple(
                        QualityLevel.BALANCED_ADAPTIVE,
                        "Cellular (Smooth Buffer)",
                        20000,
                        50000
                    )
                }
                else -> {
                    Quadruple(
                        QualityLevel.SAVER_SMOOTH,
                        "Weak Connection (Max Buffer)",
                        30000,
                        90000
                    )
                }
            }

            NetworkStatus(
                isConnected = true,
                isWifi = isWifi,
                isMetered = isMetered,
                qualityLevel = quality,
                label = label,
                minBufferMs = minBuf,
                maxBufferMs = maxBuf
            )
        } catch (e: Exception) {
            NetworkStatus()
        }
    }

    fun unregister() {
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (e: Exception) {
            // Ignored
        }
    }

    private data class Quadruple<A, B, C, D>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D
    )
}
