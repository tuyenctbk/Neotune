import re

path = 'app/src/main/java/com/easeaudio/service/RadioPlayerManager.kt'
with open(path, 'r') as f:
    content = f.read()

# Remove manual wakelock logic
content = re.sub(r'    private var wakeLock: PowerManager\.WakeLock\? = null\n', '', content)
content = re.sub(r'    private var wifiLock: WifiManager\.WifiLock\? = null\n', '', content)

content = re.sub(r'    private fun acquireLocks\(\) \{.*?\n    \}', '    private fun acquireLocks() {\n        // Handled by ExoPlayer C.WAKE_MODE_NETWORK\n    }', content, flags=re.DOTALL)
content = re.sub(r'    private fun releaseLocks\(\) \{.*?\n    \}', '    private fun releaseLocks() {\n        // Handled by ExoPlayer\n    }', content, flags=re.DOTALL)

# Re-add attributionContext
attribution_block = """
    private val attributionContext: Context by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.createAttributionContext("default_attribution")
        } else {
            context
        }
    }
"""

if 'attributionContext: Context' not in content:
    content = content.replace('class RadioPlayerManager(private val context: Context) {', 'class RadioPlayerManager(private val context: Context) {' + attribution_block)

content = content.replace('ExoPlayer.Builder(context)', 'ExoPlayer.Builder(attributionContext)')
content = content.replace('MediaSession.Builder(context, player)', 'MediaSession.Builder(attributionContext, player)')

with open(path, 'w') as f:
    f.write(content)
