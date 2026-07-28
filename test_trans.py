import urllib.request
import urllib.parse
import json

url = 'https://translate.googleapis.com/translate_a/single?client=gtx&sl=en&tl=es&dt=t'
q = "Hello"
data = urllib.parse.urlencode({'q': q}).encode('utf-8')
req = urllib.request.Request(url, data=data, headers={'User-Agent': 'Mozilla/5.0'})
try:
    response = urllib.request.urlopen(req)
    res_json = json.loads(response.read().decode('utf-8'))
    print("Success:", res_json)
except Exception as e:
    print("Error:", e)
