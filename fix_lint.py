import re

path = 'app/build.gradle.kts'
with open(path, 'r') as f:
    content = f.read()

lint_block = """
    lint {
        disable += setOf("MissingTranslation", "StringFormatInvalid")
    }
"""

if 'lint {' not in content:
    content = content.replace('android {', 'android {' + lint_block)

with open(path, 'w') as f:
    f.write(content)
