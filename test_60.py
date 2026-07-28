import urllib.request
import urllib.parse
import json

langs = ["af", "sq", "am", "ar", "hy", "az", "eu", "be", "bn", "bs", 
    "bg", "ca", "ceb", "ny", "zh-CN", "co", "hr", "cs", "da", "nl", 
    "eo", "et", "tl", "fi", "fr", "fy", "gl", "ka", "de", "el", 
    "gu", "ht", "ha", "haw", "iw", "hi", "hmn", "hu", "is", "ig", 
    "id", "ga", "it", "ja", "jv", "kn", "kk", "km", "rw", "ko", 
    "ku", "ky", "lo", "la", "lv", "lt", "lb", "mk", "mg", "ms"]

q = "Hello"
for lang in langs[:10]:
    url = f'https://translate.googleapis.com/translate_a/t?client=gtx&sl=en&tl={lang}&dt=t'
    data = urllib.parse.urlencode({'q': q}).encode('utf-8')
    req = urllib.request.Request(url, data=data, headers={'User-Agent': 'Mozilla/5.0'})
    try:
        response = urllib.request.urlopen(req)
        print(f"{lang} Success:", response.read().decode('utf-8'))
    except Exception as e:
        print(f"{lang} Error:", e)
