with open('app/src/main/AndroidManifest.xml', 'r') as f:
    content = f.read()

content = content.replace('neotune_radio_playback', 'default_attribution')

with open('app/src/main/AndroidManifest.xml', 'w') as f:
    f.write(content)

with open('app/src/main/java/com/easeaudio/service/RadioPlayerManager.kt', 'r') as f:
    content = f.read()

content = content.replace(
    '''class RadioPlayerManager(private val baseContext: Context) {
    private val context: Context = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
        baseContext.createAttributionContext("neotune_radio_playback")
    } else {
        baseContext
    }
''',
    '''class RadioPlayerManager(private val context: Context) {
    private val attributionContext: Context by lazy {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            context.createAttributionContext("default_attribution")
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

content = content.replace(
    '''    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && base != null) {
                base.createAttributionContext("neotune_radio_playback")
            } else {
                base
            }
        )
    }''',
    '''    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && base != null) {
                base.createAttributionContext("default_attribution")
            } else {
                base
            }
        )
    }'''
)

with open('app/src/main/java/com/easeaudio/service/RadioPlaybackService.kt', 'w') as f:
    f.write(content)
