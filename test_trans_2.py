import urllib.request
import urllib.parse
import json

url = 'https://translate.googleapis.com/translate_a/single?client=dict-chrome-ex&sl=en&tl=es&dt=t'
q = "Hello"
data = urllib.parse.urlencode({'q': q}).encode('utf-8')
req = urllib.request.Request(url, data=data, headers={'User-Agent': 'Mozilla/5.0'})
try:
    response = urllib.request.urlopen(req)
    res_json = json.loads(response.read().decode('utf-8'))
    print("Success dict-chrome-ex:", res_json)
except Exception as e:
    print("Error dict-chrome-ex:", e)

url2 = 'https://translate.googleapis.com/translate_a/t?client=gtx&sl=en&tl=es&dt=t'
req2 = urllib.request.Request(url2, data=data, headers={'User-Agent': 'Mozilla/5.0'})
try:
    response2 = urllib.request.urlopen(req2)
    print("Success t:", response2.read().decode('utf-8'))
except Exception as e:
    print("Error t:", e)
