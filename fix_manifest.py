import re

path = 'app/src/main/AndroidManifest.xml'
with open(path, 'r') as f:
    content = f.read()

# Add attribution tag before application
attr_tag = '    <attribution android:tag="default_attribution" android:label="@string/app_name" />\n'
content = content.replace('    <application\n', attr_tag + '    <application\n')

# Add android:attributionTags to application
content = content.replace('    <application\n', '    <application\n        android:attributionTags="default_attribution"\n')

with open(path, 'w') as f:
    f.write(content)
