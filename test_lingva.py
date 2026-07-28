import urllib.request
import json
import urllib.parse

instances = [
    'https://lingva.pussthecat.org/api/v1/en/es/Hello',
    'https://translate.plausibility.cloud/api/v1/en/es/Hello',
    'https://translate.fedilab.app/api/v1/en/es/Hello'
]

for url in instances:
    try:
        req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
        response = urllib.request.urlopen(req, timeout=5)
        res_json = json.loads(response.read().decode('utf-8'))
        print(url, "Success:", res_json)
    except Exception as e:
        print(url, "Error:", e)

