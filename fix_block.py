import re

path = 'app/src/main/java/com/easeaudio/service/RadioPlayerManager.kt'
with open(path, 'r') as f:
    content = f.read()

# Replace private val context: Context by lazy { ... }
# Because I replaced attributionContext with context, it's now private val context: Context by lazy { ... }
content = re.sub(r'    private val context: Context by lazy \{.*?\n    \}', '', content, flags=re.DOTALL)

# Since we replaced attributionContext with context, the body will have `context.createcontext("EaseAudio")`, etc., but we're removing the whole block!

with open(path, 'w') as f:
    f.write(content)
