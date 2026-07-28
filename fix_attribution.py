with open('app/src/main/AndroidManifest.xml', 'r') as f:
    content = f.read()

if 'audio_playback' not in content:
    content = content.replace('<application', '<attribution android:tag="audio_playback" android:label="@string/app_name" />\n    <application\n        android:attributionTags="audio_playback"')
    
with open('app/src/main/AndroidManifest.xml', 'w') as f:
    f.write(content)

with open('app/src/main/java/com/easeaudio/service/RadioPlayerManager.kt', 'r') as f:
    content = f.read()

import re
if 'attributionContext' not in content:
    content = content.replace('class RadioPlayerManager(private val context: Context) {',
'''class RadioPlayerManager(private val context: Context) {
    private val attributionContext: Context by lazy {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            context.createAttributionContext("audio_playback")
        } else {
            context
        }
    }
''')
    
    content = content.replace('ExoPlayer.Builder(context)', 'ExoPlayer.Builder(attributionContext)')
    content = content.replace('MediaSession.Builder(context, player)', 'MediaSession.Builder(attributionContext, player)')

with open('app/src/main/java/com/easeaudio/service/RadioPlayerManager.kt', 'w') as f:
    f.write(content)

with open('app/src/main/java/com/easeaudio/service/RadioPlaybackService.kt', 'r') as f:
    content = f.read()

if 'attachBaseContext' not in content:
    content = content.replace('class RadioPlaybackService : MediaSessionService() {',
'''class RadioPlaybackService : MediaSessionService() {
    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S && base != null) {
                base.createAttributionContext("audio_playback")
            } else {
                base
            }
        )
    }
''')

with open('app/src/main/java/com/easeaudio/service/RadioPlaybackService.kt', 'w') as f:
    f.write(content)

