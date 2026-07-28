import os

filepath = "app/src/main/res/values-rw/strings.xml"
with open(filepath, 'r') as f:
    content = f.read()

content = content.replace('name="auto_adaptive_buffer"', 'name="auto_adaptive_buffer" formatted="false"')

with open(filepath, 'w') as f:
    f.write(content)
