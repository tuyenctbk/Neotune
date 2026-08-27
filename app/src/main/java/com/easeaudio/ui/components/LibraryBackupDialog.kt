package com.easeaudio.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.easeaudio.R
import com.easeaudio.data.RadioStation
import org.json.JSONArray
import org.json.JSONObject

@Composable
fun LibraryBackupDialog(
    favorites: List<RadioStation>,
    listenLater: List<com.easeaudio.data.ListenLaterItem> = emptyList(),
    onImportStations: (List<RadioStation>) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Export, 1: Import
    var importJsonText by remember { mutableStateOf("") }
    var importError by remember { mutableStateOf<String?>(null) }
    var importedCount by remember { mutableStateOf<Int?>(null) }

    // Generate formatted JSON string for export
    val exportJson = remember(favorites, listenLater) {
        try {
            val jsonArray = JSONArray()
            favorites.forEach { station ->
                val obj = JSONObject().apply {
                    put("id", station.id)
                    put("name", station.name)
                    put("genre", station.genre)
                    put("country", station.country)
                    put("streamUrl", station.streamUrl)
                    put("imageUrl", station.imageUrl)
                    put("bitrate", station.bitrate)
                    put("codec", station.codec)
                    put("isCustom", station.isCustom)
                    put("isFavorite", true)
                }
                jsonArray.put(obj)
            }
            val laterArray = JSONArray()
            listenLater.forEach { item ->
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("name", item.name)
                    put("genre", item.genre)
                    put("country", item.country)
                    put("streamUrl", item.streamUrl)
                    put("imageUrl", item.imageUrl)
                    put("bitrate", item.bitrate)
                    put("codec", item.codec)
                    put("isCustom", item.isCustom)
                }
                laterArray.put(obj)
            }
            val root = JSONObject().apply {
                put("app", "NeoTune")
                put("version", "1.0")
                put("exportedAt", System.currentTimeMillis())
                put("stations", jsonArray)
                put("listenLater", laterArray)
            }
            root.toString(2)
        } catch (e: Exception) {
            "[]"
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            modifier = Modifier
                .widthIn(max = 500.dp)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Backup,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.backup_restore_title),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${favorites.size} ${stringResource(R.string.favorites)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Segmented Tabs: Export vs Import
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    contentColor = MaterialTheme.colorScheme.primary,
                    indicator = {},
                    divider = {},
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        modifier = Modifier.clip(RoundedCornerShape(10.dp)),
                        text = {
                            Text(
                                text = stringResource(R.string.backup_tab_export),
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        modifier = Modifier.clip(RoundedCornerShape(10.dp)),
                        text = {
                            Text(
                                text = stringResource(R.string.backup_tab_import),
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (selectedTab == 0) {
                    // EXPORT VIEW
                    Text(
                        text = stringResource(R.string.backup_export_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 180.dp)
                    ) {
                        Text(
                            text = exportJson,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp),
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("NeoTune Library Backup", exportJson))
                                Toast.makeText(context, context.getString(R.string.backup_copied_success), Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("backup_copy_json_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.backup_copy_json))
                        }
                    }
                } else {
                    // IMPORT VIEW
                    Text(
                        text = stringResource(R.string.backup_import_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = importJsonText,
                        onValueChange = {
                            importJsonText = it
                            importError = null
                            importedCount = null
                        },
                        placeholder = {
                            Text(
                                text = "{\n  \"stations\": [\n    {\n      \"name\": \"Station Name\",\n      \"streamUrl\": \"...\"\n    }\n  ]\n}",
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .testTag("backup_import_input"),
                        shape = RoundedCornerShape(12.dp),
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    )

                    if (importError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = importError!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    if (importedCount != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.backup_import_success_format, importedCount!!),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = clipboard.primaryClip
                                if (clip != null && clip.itemCount > 0) {
                                    importJsonText = clip.getItemAt(0).text?.toString() ?: ""
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("backup_paste_btn")
                        ) {
                            Icon(imageVector = Icons.Filled.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.paste))
                        }

                        Button(
                            onClick = {
                                try {
                                    val parsedList = mutableListOf<RadioStation>()
                                    val trimmed = importJsonText.trim()
                                    if (trimmed.startsWith("[")) {
                                        val array = JSONArray(trimmed)
                                        for (i in 0 until array.length()) {
                                            val obj = array.getJSONObject(i)
                                            parsedList.add(parseStationJson(obj))
                                        }
                                    } else {
                                        val root = JSONObject(trimmed)
                                        val array = root.optJSONArray("stations") ?: JSONArray()
                                        for (i in 0 until array.length()) {
                                            val obj = array.getJSONObject(i)
                                            parsedList.add(parseStationJson(obj))
                                        }
                                    }

                                    if (parsedList.isNotEmpty()) {
                                        onImportStations(parsedList)
                                        importedCount = parsedList.size
                                        Toast.makeText(context, context.getString(R.string.backup_import_success_format, parsedList.size), Toast.LENGTH_SHORT).show()
                                    } else {
                                        importError = context.getString(R.string.backup_import_no_stations)
                                    }
                                } catch (e: Exception) {
                                    importError = context.getString(R.string.backup_import_invalid_json)
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            enabled = importJsonText.isNotBlank(),
                            modifier = Modifier.weight(1f).testTag("backup_restore_btn")
                        ) {
                            Text(stringResource(R.string.backup_restore_btn))
                        }
                    }
                }
            }
        }
    }
}

private fun parseStationJson(obj: JSONObject): RadioStation {
    return RadioStation(
        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
        name = obj.optString("name", "Restored Station"),
        genre = obj.optString("genre", "Custom"),
        country = obj.optString("country", "Global"),
        streamUrl = obj.optString("streamUrl", ""),
        imageUrl = obj.optString("imageUrl", ""),
        bitrate = obj.optString("bitrate", "128 kbps"),
        codec = obj.optString("codec", "AAC/MP3"),
        isFavorite = true,
        isCustom = obj.optBoolean("isCustom", false)
    )
}
