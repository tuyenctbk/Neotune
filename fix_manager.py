import re

path = 'app/src/main/java/com/easeaudio/service/RadioPlayerManager.kt'
with open(path, 'r') as f:
    content = f.read()

# Make attributionContext a class property
prop_code = """
    private val attributionContext: Context by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.createAttributionContext("EaseAudio")
        } else {
            context
        }
    }
"""

if 'private val attributionContext: Context' not in content:
    content = content.replace('private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())', 
                              'private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())\n' + prop_code)

# Remove local attributionContext in setupPlayer
content = re.sub(r'        val attributionContext = if \(Build\.VERSION\.SDK_INT >= Build\.VERSION_CODES\.S\) \{.*?\}.*?\} else \{.*?\}', '', content, flags=re.DOTALL)

with open(path, 'w') as f:
    f.write(content)
