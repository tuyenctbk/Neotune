with open('app/src/main/java/com/easeaudio/service/RadioPlayerManager.kt', 'r') as f:
    content = f.read()

content = content.replace(
    'class RadioPlayerManager(private val context: Context) {',
    '''class RadioPlayerManager(private val baseContext: Context) {
    private val context: Context = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
        baseContext.createAttributionContext("neotune_radio_playback")
    } else {
        baseContext
    }
'''
)

with open('app/src/main/java/com/easeaudio/service/RadioPlayerManager.kt', 'w') as f:
    f.write(content)
