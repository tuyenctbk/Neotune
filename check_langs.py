import os
import xml.etree.ElementTree as ET

langs = [
    "af", "sq", "am", "ar", "hy", "az", "eu", "be", "bn", "bs", 
    "bg", "ca", "ceb", "ny", "zh-CN", "co", "hr", "cs", "da", "nl", 
    "eo", "et", "tl", "fi", "fr", "fy", "gl", "ka", "de", "el", 
    "gu", "ht", "ha", "haw", "iw", "hi", "hmn", "hu", "is", "ig", 
    "id", "ga", "it", "ja", "jv", "kn", "kk", "km", "rw", "ko", 
    "ku", "ky", "lo", "la", "lv", "lt", "lb", "mk", "mg", "ms"
]

en_tree = ET.parse('app/src/main/res/values/strings.xml')
en_root = en_tree.getroot()
en_text = ""
for child in en_root:
    if child.tag == 'string' and child.attrib.get('name') == 'app_description':
        en_text = child.text
        break

needs_translation = []
for lang in langs:
    path = f'app/src/main/res/values-{lang}/strings.xml'
    if os.path.exists(path):
        tree = ET.parse(path)
        root = tree.getroot()
        val = ""
        for child in root:
            if child.tag == 'string' and child.attrib.get('name') == 'app_description':
                val = child.text
                break
        if val == en_text:
            needs_translation.append(lang)
    else:
        needs_translation.append(lang)

print(f"Needs translation: {len(needs_translation)}")
print(" ".join(needs_translation))
