package com.easeaudio.data

import android.content.Context
import android.content.Intent
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import com.easeaudio.R
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

data class RecordingState(
    val isRecording: Boolean = false,
    val station: RadioStation? = null,
    val trackTitle: String = "",
    val durationSeconds: Long = 0L,
    val bytesRecorded: Long = 0L,
    val currentFilePath: String = ""
)

data class RecordingItem(
    val id: String,
    val fileName: String,
    val file: File,
    val stationName: String,
    val trackTitle: String,
    val sizeBytes: Long,
    val timestamp: Long,
    val durationSeconds: Long = 0L
) {
    val formattedSize: String
        get() {
            val mb = sizeBytes.toDouble() / (1024 * 1024)
            return if (mb >= 1.0) String.format(Locale.US, "%.1f MB", mb) else String.format(Locale.US, "%d KB", sizeBytes / 1024)
        }

    val formattedDate: String
        get() = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(timestamp))
}

class RadioStreamRecorder private constructor(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _recordingState = MutableStateFlow(RecordingState())
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

    private var activeJob: Job? = null
    private var activeOutputStream: FileOutputStream? = null
    private var activeInputStream: InputStream? = null
    private var activeConnection: HttpURLConnection? = null
    private var activeFile: File? = null

    private fun getRecordingsDirectory(): File {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_RECORDINGS)
            ?: File(context.filesDir, "recordings")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun startRecording(station: RadioStation, currentTrackTitle: String?): Boolean {
        if (_recordingState.value.isRecording) {
            Log.w(TAG, "Already recording")
            return false
        }

        val track = if (!currentTrackTitle.isNullOrBlank() && currentTrackTitle != "Live Audio Stream") {
            currentTrackTitle.trim()
        } else {
            station.genre.ifBlank { "Live Broadcast" }
        }

        val sanitizedStation = station.name.replace(Regex("[^a-zA-Z0-9._-]"), "_").take(25)
        val sanitizedTrack = track.replace(Regex("[^a-zA-Z0-9._-]"), "_").take(30)
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())

        val extension = when {
            station.codec.contains("aac", ignoreCase = true) -> "aac"
            station.streamUrl.contains(".aac", ignoreCase = true) -> "aac"
            station.streamUrl.contains(".ogg", ignoreCase = true) -> "ogg"
            else -> "mp3"
        }

        val fileName = "${sanitizedStation}_${sanitizedTrack}_$timestamp.$extension"
        val targetFile = File(getRecordingsDirectory(), fileName)
        activeFile = targetFile

        _recordingState.value = RecordingState(
            isRecording = true,
            station = station,
            trackTitle = track,
            durationSeconds = 0L,
            bytesRecorded = 0L,
            currentFilePath = targetFile.absolutePath
        )

        activeJob = scope.launch {
            var bytesWritten = 0L
            var startTime = System.currentTimeMillis()

            try {
                val url = URL(station.streamUrl)
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    (this as? javax.net.ssl.HttpsURLConnection)?.apply {
                        sslSocketFactory = com.easeaudio.util.NetworkSecurityHelper.sslSocketFactory
                        hostnameVerifier = com.easeaudio.util.NetworkSecurityHelper.hostnameVerifier
                    }
                    requestMethod = "GET"
                    connectTimeout = 15000
                    readTimeout = 15000
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) EaseAudio/1.0")
                    setRequestProperty("Accept", "*/*")
                    setRequestProperty("Icy-MetaData", "0") // request raw stream without ICY text interleaving
                }
                activeConnection = connection

                val responseCode = connection.responseCode
                if (responseCode !in 200..299) {
                    Log.w(TAG, "Recording HTTP error: $responseCode")
                    try { connection.disconnect() } catch (_: Exception) {}
                    if (targetFile.exists()) targetFile.delete()
                    _recordingState.value = RecordingState(isRecording = false)
                    return@launch
                }

                val inStream = connection.inputStream
                activeInputStream = inStream
                val outStream = FileOutputStream(targetFile)
                activeOutputStream = outStream

                val buffer = ByteArray(8192)
                var lastReportedSec = -1L
                while (isActive) {
                    val bytesRead = inStream.read(buffer)
                    if (bytesRead == -1) break
                    outStream.write(buffer, 0, bytesRead)
                    bytesWritten += bytesRead
                    val elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000L

                    // Throttle state emissions to once per second to reduce CPU & UI recomposition overhead
                    if (elapsedSeconds != lastReportedSec) {
                        lastReportedSec = elapsedSeconds
                        _recordingState.value = _recordingState.value.copy(
                            bytesRecorded = bytesWritten,
                            durationSeconds = elapsedSeconds
                        )
                    }
                }

                outStream.flush()
                Log.i(TAG, "Recording stream finished normally, size: $bytesWritten bytes")
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    Log.e(TAG, "Error recording stream: ${e.message}")
                }
            } finally {
                try {
                    activeOutputStream?.flush()
                    activeOutputStream?.close()
                } catch (_: Exception) {}
                try {
                    activeInputStream?.close()
                } catch (_: Exception) {}
                try {
                    activeConnection?.disconnect()
                } catch (_: Exception) {}

                activeOutputStream = null
                activeInputStream = null
                activeConnection = null

                _recordingState.value = RecordingState(isRecording = false)
            }
        }

        return true
    }

    fun stopRecording(): File? {
        val file = activeFile
        activeJob?.cancel()
        activeJob = null
        try {
            activeOutputStream?.flush()
            activeOutputStream?.close()
        } catch (_: Exception) {}
        try {
            activeInputStream?.close()
        } catch (_: Exception) {}
        try {
            activeConnection?.disconnect()
        } catch (_: Exception) {}

        _recordingState.value = RecordingState(isRecording = false)
        if (file != null && file.exists() && file.length() == 0L) {
            try { file.delete() } catch (_: Exception) {}
        }
        return if (file != null && file.exists() && file.length() > 0L) file else null
    }

    fun getRecordings(): List<RecordingItem> {
        val dir = getRecordingsDirectory()
        val files = dir.listFiles { f ->
            if (f.isFile && (f.extension.equals("mp3", true) || f.extension.equals("aac", true) || f.extension.equals("ogg", true) || f.extension.equals("m4a", true))) {
                if (f.length() == 0L) {
                    try { f.delete() } catch (_: Exception) {}
                    false
                } else {
                    true
                }
            } else false
        } ?: return emptyList()

        return files.map { file ->
            val nameParts = file.nameWithoutExtension.split("_")
            val stationName = if (nameParts.isNotEmpty()) nameParts[0].replace("_", " ") else "Radio Station"
            val trackTitle = if (nameParts.size >= 2) nameParts[1].replace("_", " ") else "Live Recording"

            RecordingItem(
                id = file.absolutePath,
                fileName = file.name,
                file = file,
                stationName = stationName,
                trackTitle = trackTitle,
                sizeBytes = file.length(),
                timestamp = file.lastModified()
            )
        }.sortedByDescending { it.timestamp }
    }

    fun deleteRecording(item: RecordingItem): Boolean {
        return try {
            if (item.file.exists()) {
                item.file.delete()
            } else false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete recording: ${e.message}")
            false
        }
    }

    fun shareRecording(context: Context, item: RecordingItem) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                item.file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "audio/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "${item.stationName} - ${item.trackTitle}")
                putExtra(Intent.EXTRA_TEXT, context.getString(R.string.share_recording_text, item.stationName, item.trackTitle))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(shareIntent, context.getString(R.string.share_recording)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to share recording: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "RadioStreamRecorder"

        @Volatile
        private var instance: RadioStreamRecorder? = null

        fun getInstance(context: Context): RadioStreamRecorder {
            return instance ?: synchronized(this) {
                instance ?: RadioStreamRecorder(context.applicationContext).also { instance = it }
            }
        }
    }
}
