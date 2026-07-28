with open('app/src/main/java/com/easeaudio/service/RadioPlaybackService.kt', 'r') as f:
    content = f.read()

if 'attachBaseContext' not in content:
    content = content.replace('class RadioPlaybackService : MediaSessionService() {',
'''class RadioPlaybackService : MediaSessionService() {
    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && base != null) {
                base.createAttributionContext("my_radio_tag")
            } else {
                base
            }
        )
    }
''')

with open('app/src/main/java/com/easeaudio/service/RadioPlaybackService.kt', 'w') as f:
    f.write(content)
