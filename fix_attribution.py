import re

path = 'app/src/main/java/com/easeaudio/service/RadioPlayerManager.kt'
with open(path, 'r') as f:
    content = f.read()

# Make attributionContext just context
content = re.sub(r'    private val attributionContext: Context by lazy \{.*?\n    \}', '    private val attributionContext: Context get() = context', content, flags=re.DOTALL)

with open(path, 'w') as f:
    f.write(content)
