with open('app/src/main/java/com/easeaudio/data/RadioBrowserService.kt', 'r') as f:
    content = f.read()

# Make search require https to avoid playback issues (cleartext traffic issues) and ensure better quality streams
# Or actually Android allows cleartext, but https is better
content = content.replace(
    'val urlBuilder = StringBuilder("$BASE_URL/search?offset=$offset&limit=$limit&order=clickcount&reverse=true&hidebroken=true")',
    'val urlBuilder = StringBuilder("$BASE_URL/search?offset=$offset&limit=$limit&order=clickcount&reverse=true&hidebroken=true&is_https=true")'
)

with open('app/src/main/java/com/easeaudio/data/RadioBrowserService.kt', 'w') as f:
    f.write(content)
