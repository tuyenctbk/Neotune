import os
import xml.etree.ElementTree as ET
import urllib.request
import urllib.parse
import json
import time

def translate_texts(texts, target_lang):
    if target_lang == 'zh-rCN': target_lang = 'zh-CN'
    elif target_lang == 'iw': target_lang = 'he'
    
    url = f'https://translate.googleapis.com/translate_a/t?client=gtx&sl=en&tl={target_lang}&dt=t'
    sep = "\n"
    q = sep.join(texts)
    data = urllib.parse.urlencode({'q': q}).encode('utf-8')
    req = urllib.request.Request(url, data=data, headers={'User-Agent': 'Mozilla/5.0'})
    try:
        response = urllib.request.urlopen(req)
        res_json = json.loads(response.read().decode('utf-8'))
        
        translated = res_json[0]
        parts = translated.strip('\n').split(sep)
        
        # If it doesn't match perfectly, let's just translate one by one
        if len(parts) != len(texts):
            print(f"Fallback to 1 by 1 for {target_lang}")
            res = []
            for t in texts:
                data = urllib.parse.urlencode({'q': t}).encode('utf-8')
                req = urllib.request.Request(url, data=data, headers={'User-Agent': 'Mozilla/5.0'})
                resp = urllib.request.urlopen(req)
                r_j = json.loads(resp.read().decode('utf-8'))
                res.append(r_j[0].strip('\n'))
                time.sleep(0.1)
            return res
            
        return [p.strip('\n') for p in parts]
    except Exception as e:
        print(f"Error translating to {target_lang}: {e}")
        return texts

def escape_xml(text):
    text = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    text = text.replace("'", "\\'").replace('"', '\\"')
    return text

tree = ET.parse('app/src/main/res/values/strings.xml')
root = tree.getroot()
en_strings = []
for child in root:
    if child.tag == 'string':
        name = child.attrib.get('name')
        text = "".join(child.itertext())
        if text:
            text = text.replace('\n', ' ')
            en_strings.append((name, text))

dirs = [d for d in os.listdir('app/src/main/res') if d.startswith('values-') and os.path.isdir(os.path.join('app/src/main/res', d))]

for d in dirs:
    lang = d.replace('values-', '')
    print(f"[{lang}] Starting...")
    
    out_file = os.path.join('app/src/main/res', d, 'strings.xml')
    existing_tree = None
    try:
        existing_tree = ET.parse(out_file)
    except:
        pass
    
    existing_strings = {}
    if existing_tree:
        for child in existing_tree.getroot():
            if child.tag == 'string':
                existing_strings[child.attrib.get('name')] = "".join(child.itertext())

    missing_names = []
    missing_texts = []
    for name, text in en_strings:
        if name not in existing_strings or name in ['data_provider_desc', 'data_provider_gratitude', 'data_provider_info', 'dismiss']:
            missing_names.append(name)
            missing_texts.append(text)
            
    if not missing_names:
        print(f"[{lang}] Up to date.")
        continue
        
    print(f"[{lang}] Translating {len(missing_texts)} missing strings...")
    translated_texts = translate_texts(missing_texts, lang)
    
    for name, tr_text in zip(missing_names, translated_texts):
        existing_strings[name] = tr_text
        
    with open(out_file, 'w', encoding='utf-8') as f:
        f.write("<?xml version='1.0' encoding='utf-8'?>\n")
        f.write("<resources>\n")
        for name, _ in en_strings: # preserve order
            text = existing_strings.get(name, dict(en_strings).get(name, ""))
            if type(text) == str:
                f.write(f'    <string name="{name}">{escape_xml(text.strip())}</string>\n')
            else:
                f.write(f'    <string name="{name}">{escape_xml(str(text).strip())}</string>\n')
        f.write("</resources>\n")
        
print("Done!")
