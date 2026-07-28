import os
import glob
import re

for filepath in glob.glob("app/src/main/res/values-*/strings.xml"):
    with open(filepath, 'r') as f:
        content = f.read()
    
    # Fix broken backslashes
    content = content.replace("\\\\\\\\'", "\\'")
    content = content.replace("\\\\'", "\\'")
    
    with open(filepath, 'w') as f:
        f.write(content)
