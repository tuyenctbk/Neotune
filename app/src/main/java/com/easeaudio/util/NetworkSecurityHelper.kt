package com.easeaudio.util

import android.annotation.SuppressLint
import android.util.Log
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

@SuppressLint("CustomX509TrustManager", "TrustAllX509TrustManager", "BadHostnameVerifier")
object NetworkSecurityHelper {
    private const val TAG = "NetworkSecurityHelper"

    private val defaultTrustManager: X509TrustManager? by lazy {
        try {
            val factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
            factory.init(null as KeyStore?)
            factory.trustManagers.firstOrNull { it is X509TrustManager } as? X509TrustManager
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load system TrustManager: ${e.message}", e)
            null
        }
    }

    val trustAllCerts = arrayOf<TrustManager>(
        object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                defaultTrustManager?.checkClientTrusted(chain, authType)
            }

            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                try {
                    defaultTrustManager?.checkServerTrusted(chain, authType)
                } catch (e: CertificateException) {
                    // Gracefully allow legacy/community radio streaming servers that may have
                    // expired or self-signed certificates without halting playback.
                    if (chain.isNullOrEmpty()) {
                        throw e
                    }
                    Log.w(TAG, "Server certificate validation bypassed for radio stream: ${e.message}")
                }
            }

            override fun getAcceptedIssuers(): Array<X509Certificate> =
                defaultTrustManager?.acceptedIssuers ?: emptyArray()
        }
    )

    val sslSocketFactory: SSLSocketFactory by lazy {
        try {
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, trustAllCerts, SecureRandom())
            sslContext.socketFactory
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize custom SSLSocketFactory: ${e.message}", e)
            HttpsURLConnection.getDefaultSSLSocketFactory()
        }
    }

    val hostnameVerifier = HostnameVerifier { _, _ -> true }

    fun install() {
        try {
            // Disable OCSP revocation checks that fail under date skew
            System.setProperty("com.sun.security.enableCRLDP", "false")
            System.setProperty("com.sun.net.ssl.checkRevocation", "false")
            System.setProperty("ocsp.enable", "false")
            Log.i(TAG, "Network security properties configured")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to configure network properties: ${e.message}", e)
        }
    }
}
