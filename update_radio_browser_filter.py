with open('app/src/main/java/com/easeaudio/data/RadioBrowserService.kt', 'r') as f:
    content = f.read()

filter_logic = """                    val codec = item.optString("codec", "MP3").uppercase()

                    // Filter out adult/nsfw content to keep the app safe
                    val isAdult = tags.contains("adult", ignoreCase = true) || 
                                  tags.contains("nsfw", ignoreCase = true) || 
                                  tags.contains("explicit", ignoreCase = true)
                    
                    if (!isAdult && name.isNotBlank() && streamUrl.isNotBlank() && (streamUrl.startsWith("http://") || streamUrl.startsWith("https://"))) {"""

content = content.replace(
    'val codec = item.optString("codec", "MP3").uppercase()\n\n                    if (name.isNotBlank() && streamUrl.isNotBlank() && (streamUrl.startsWith("http://") || streamUrl.startsWith("https://"))) {',
    filter_logic
)

with open('app/src/main/java/com/easeaudio/data/RadioBrowserService.kt', 'w') as f:
    f.write(content)
