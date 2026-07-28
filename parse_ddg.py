import urllib.request, urllib.parse, re

def search(query):
    req = urllib.request.Request(
        'https://html.duckduckgo.com/html/?q=' + urllib.parse.quote(query),
        headers={'User-Agent': 'Mozilla/5.0'}
    )
    res = urllib.request.urlopen(req).read().decode('utf-8')
    for match in re.finditer(r'<a class="result__snippet[^>]*>(.*?)</a>', res, re.IGNORECASE | re.DOTALL):
        text = re.sub(r'<[^>]+>', '', match.group(1)).strip()
        print(text)
        print("---")
search('"attributionTag" "not declared in manifest"')
