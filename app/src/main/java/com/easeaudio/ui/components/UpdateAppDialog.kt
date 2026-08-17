package com.easeaudio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.easeaudio.R
import com.easeaudio.engagement.UpdateInfo
import com.easeaudio.ui.theme.*

@Composable
fun UpdateAppDialog(
    updateInfo: UpdateInfo,
    onUpdateConfirmed: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = {
            if (!updateInfo.isForceUpdate) onDismiss()
        },
        properties = DialogProperties(
            dismissOnBackPress = !updateInfo.isForceUpdate,
            dismissOnClickOutside = !updateInfo.isForceUpdate,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                .testTag("dialog_update_app"),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (updateInfo.isForceUpdate) stringResource(R.string.critical_update_header) else stringResource(R.string.new_version_header),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        ),
                        color = if (updateInfo.isForceUpdate) Color(0xFFFF5252) else MaterialTheme.colorScheme.primary
                    )
                    if (!updateInfo.isForceUpdate) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Big System Update Badge Icon
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(if (updateInfo.isForceUpdate) Color(0xFFFF5252).copy(alpha = 0.15f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.SystemUpdate,
                        contentDescription = null,
                        tint = if (updateInfo.isForceUpdate) Color(0xFFFF5252) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(38.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (updateInfo.isForceUpdate) stringResource(R.string.update_required_title) else stringResource(R.string.update_available_title, updateInfo.latestVersionName),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                val descriptionText = when {
                    updateInfo.updateNotes.isNotBlank() -> updateInfo.updateNotes
                    updateInfo.isForceUpdate -> stringResource(R.string.update_critical_desc)
                    else -> stringResource(R.string.update_optional_desc)
                }

                Text(
                    text = descriptionText,
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onUpdateConfirmed,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (updateInfo.isForceUpdate) Color(0xFFFF5252) else MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.background
                    ),
                    shape = RoundedCornerShape(14.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_update_confirm")
                ) {
                    Icon(
                        imageVector = Icons.Filled.SystemUpdate,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.update_now_btn),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    )
                }

                if (!updateInfo.isForceUpdate) {
                    Spacer(modifier = Modifier.height(10.dp))
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = stringResource(R.string.remind_later_btn),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}
