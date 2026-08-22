package com.easeaudio.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.easeaudio.R
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.easeaudio.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttributionDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = stringResource(id = R.string.data_provider_info),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(id = R.string.data_provider_desc),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(id = R.string.data_provider_gratitude),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)
                OutlinedButton(
                    onClick = {
                        try {
                            val intent = android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse("https://neotune.ai.studio/")
                            ).apply {
                                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            android.util.Log.e("AttributionDialog", "Failed to open PWA URL", e)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Open Web App (PWA): neotune.ai.studio",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.background
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_close_attribution_dialog")
            ) {
                Text(
                    text = stringResource(id = R.string.dismiss),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}
