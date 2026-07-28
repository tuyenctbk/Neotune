import urllib.request, json, urllib.parse
res=urllib.request.urlopen('https://html.duckduckgo.com/html/?q=' + urllib.parse.quote('\"attributionTag not declared in manifest\" \"AppOps\"')).read().decode('utf-8')
print(res[:2000])
