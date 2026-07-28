with open('app/src/main/java/com/easeaudio/service/RadioPlayerManager.kt', 'r') as f:
    content = f.read()

import re
content = re.sub(r'    private val attributionContext: Context by lazy \{.*?\n    \}\n', '', content, flags=re.DOTALL)
content = content.replace('ExoPlayer.Builder(attributionContext)', 'ExoPlayer.Builder(context)')
content = content.replace('MediaSession.Builder(attributionContext, player)', 'MediaSession.Builder(context, player)')

with open('app/src/main/java/com/easeaudio/service/RadioPlayerManager.kt', 'w') as f:
    f.write(content)

with open('app/src/main/java/com/easeaudio/service/RadioPlaybackService.kt', 'r') as f:
    content = f.read()

content = re.sub(r'    override fun attachBaseContext\(base: Context\?\) \{.*?\n    \}\n\n', '', content, flags=re.DOTALL)

with open('app/src/main/java/com/easeaudio/service/RadioPlaybackService.kt', 'w') as f:
    f.write(content)
