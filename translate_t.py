import os
import xml.etree.ElementTree as ET
import urllib.request
import urllib.parse
import json
import time

langs = [
    "af", "sq", "am", "ar", "hy", "az", "eu", "be", "bn", "bs", 
    "bg", "ca", "ceb", "ny", "zh-CN", "co", "hr", "cs", "da", "nl", 
    "eo", "et", "tl", "fi", "fr", "fy", "gl", "ka", "de", "el", 
    "gu", "ht", "ha", "haw", "iw", "hi", "hmn", "hu", "is", "ig", 
    "id", "ga", "it", "ja", "jv", "kn", "kk", "km", "rw", "ko", 
    "ku", "ky", "lo", "la", "lv", "lt", "lb", "mk", "mg", "ms"
]

def translate_texts(texts, target_lang):
    url = f'https://translate.googleapis.com/translate_a/t?client=gtx&sl=en&tl={target_lang}&dt=t'
    sep = "\n"
    q = sep.join(texts)
    data = urllib.parse.urlencode({'q': q}).encode('utf-8')
    req = urllib.request.Request(url, data=data, headers={'User-Agent': 'Mozilla/5.0'})
    try:
        response = urllib.request.urlopen(req)
        res_json = json.loads(response.read().decode('utf-8'))
        # res_json is usually ["translated string"]
        translated = res_json[0]
        return translated.strip('\n').split(sep)
    except Exception as e:
        print(f"Error translating to {target_lang}: {e}")
        return texts

def escape_xml(text):
    text = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    text = text.replace("'", "\\'").replace('"', '\\"')
    return text

tree = ET.parse('app/src/main/res/values/strings.xml')
root = tree.getroot()

strings = []
for child in root:
    if child.tag == 'string':
        name = child.attrib.get('name')
        text = child.text
        if text:
            text = text.replace('\n', ' ')
            strings.append((name, text))

names = [s[0] for s in strings]
texts = [s[1] for s in strings]

print(f"Found {len(texts)} strings.")

for lang in langs:
    print(f"[{lang}] Starting...")
    translated_texts = translate_texts(texts, lang)
    if len(translated_texts) != len(texts):
        print(f"[{lang}] Fallback needed! {len(translated_texts)} vs {len(texts)}")
        translated_texts = texts
    
    out_dir = f'app/src/main/res/values-{lang}'
    os.makedirs(out_dir, exist_ok=True)
    out_file = os.path.join(out_dir, 'strings.xml')
    
    with open(out_file, 'w', encoding='utf-8') as f:
        f.write('<?xml version="1.0" encoding="utf-8"?>\n')
        f.write('<resources>\n')
        for name, tr_text in zip(names, translated_texts):
            f.write(f'    <string name="{name}">{escape_xml(tr_text.strip())}</string>\n')
        f.write('</resources>\n')
    time.sleep(1)

print("All done!")
