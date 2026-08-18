package com.easeaudio.util

import android.util.Log
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object NetworkSecurityHelper {
    private const val TAG = "NetworkSecurityHelper"

    val trustAllCerts = arrayOf<TrustManager>(
        object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                // Allow all client certificates
            }

            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                // Allow all server certificates to prevent clock-skew / OCSP chain validation failures
            }

            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
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

            HttpsURLConnection.setDefaultSSLSocketFactory(sslSocketFactory)
            HttpsURLConnection.setDefaultHostnameVerifier(hostnameVerifier)
            Log.i(TAG, "Custom SSL and TrustManager successfully installed")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to install custom network security: ${e.message}", e)
        }
    }
}
