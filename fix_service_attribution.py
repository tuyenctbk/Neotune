with open('app/src/main/java/com/easeaudio/service/RadioPlaybackService.kt', 'r') as f:
    content = f.read()

import_statement = "import androidx.media3.session.MediaSessionService"
new_imports = "import androidx.media3.session.MediaSessionService\nimport android.content.Context\nimport android.os.Build"
content = content.replace(import_statement, new_imports)

class_start = "class RadioPlaybackService : MediaSessionService() {"
attach_base = """class RadioPlaybackService : MediaSessionService() {

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && base != null) {
                base.createAttributionContext("neotune_radio_playback")
            } else {
                base
            }
        )
    }
"""
content = content.replace(class_start, attach_base)

with open('app/src/main/java/com/easeaudio/service/RadioPlaybackService.kt', 'w') as f:
    f.write(content)
