with open('app/src/main/java/com/easeaudio/service/RadioPlayerManager.kt', 'r') as f:
    content = f.read()

content = content.replace('''    private val attributionContext: Context by lazy {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            context.createAttributionContext("default_attribution")
        } else {
            context
        }
    }''', '')

content = content.replace('ExoPlayer.Builder(attributionContext)', 'ExoPlayer.Builder(context)')
content = content.replace('MediaSession.Builder(attributionContext, player)', 'MediaSession.Builder(context, player)')

with open('app/src/main/java/com/easeaudio/service/RadioPlayerManager.kt', 'w') as f:
    f.write(content)

with open('app/src/main/java/com/easeaudio/service/RadioPlaybackService.kt', 'r') as f:
    content = f.read()

content = content.replace('''    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && base != null) {
                base.createAttributionContext("default_attribution")
            } else {
                base
            }
        )
    }''', '')

with open('app/src/main/java/com/easeaudio/service/RadioPlaybackService.kt', 'w') as f:
    f.write(content)

with open('app/src/main/AndroidManifest.xml', 'r') as f:
    content = f.read()

content = content.replace('    <attribution android:tag="default_attribution" android:label="@string/app_name" />\n', '')
content = content.replace('        android:attributionTags="default_attribution"\n', '')

with open('app/src/main/AndroidManifest.xml', 'w') as f:
    f.write(content)

