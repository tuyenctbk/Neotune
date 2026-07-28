with open('app/src/main/AndroidManifest.xml', 'r') as f:
    content = f.read()

content = content.replace('<attribution android:tag="my_radio_tag" android:label="@string/app_name" />', '<attribution android:tag="" android:label="@string/app_name" />')
content = content.replace('android:attributionTags="my_radio_tag"', 'android:attributionTags=""')

with open('app/src/main/AndroidManifest.xml', 'w') as f:
    f.write(content)
