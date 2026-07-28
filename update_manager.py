import re

path = 'app/src/main/java/com/easeaudio/service/RadioPlayerManager.kt'
with open(path, 'r') as f:
    content = f.read()

# Add Build import if not present
if 'import android.os.Build' not in content:
    content = content.replace('import android.os.PowerManager', 'import android.os.PowerManager\nimport android.os.Build')

# Create attribution context
attribution_context_code = """
        val attributionContext = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.createAttributionContext("EaseAudio")
        } else {
            context
        }

        val audioAttributes = AudioAttributes.Builder()
"""
content = content.replace('        val audioAttributes = AudioAttributes.Builder()', attribution_context_code)

# Update ExoPlayer Builder to use attributionContext
content = content.replace('exoPlayer = ExoPlayer.Builder(context)', 'exoPlayer = ExoPlayer.Builder(attributionContext)')

# Update PowerManager and WifiManager contexts to use attributionContext
content = content.replace('val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager', 'val pm = attributionContext.getSystemService(Context.POWER_SERVICE) as? PowerManager')
content = content.replace('val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager', 'val wm = attributionContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager')

with open(path, 'w') as f:
    f.write(content)
