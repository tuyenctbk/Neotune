import re

with open('app/src/main/java/com/easeaudio/viewmodel/RadioViewModel.kt', 'r') as f:
    content = f.read()

content = content.replace(
    'val availableGenres = listOf(\n        "All",\n        "News & Reports",\n        "Lo-Fi & Chill",\n        "Jazz",\n        "Rock",\n        "Classical",\n        "Ambient",\n        "EDM",\n        "Custom"\n    )',
    'val availableGenres = listOf(\n        "All",\n        "News & Reports",\n        "Lo-Fi & Chill",\n        "Pop",\n        "Jazz",\n        "Rock",\n        "Hip Hop",\n        "Classical",\n        "Ambient",\n        "EDM",\n        "House",\n        "Country",\n        "Custom"\n    )'
)

with open('app/src/main/java/com/easeaudio/viewmodel/RadioViewModel.kt', 'w') as f:
    f.write(content)

with open('app/src/main/java/com/easeaudio/data/RadioBrowserService.kt', 'r') as f:
    content2 = f.read()

content2 = content2.replace(
    '"EDM" -> "edm"',
    '"EDM" -> "edm"\n            "Pop" -> "pop"\n            "Hip Hop" -> "hip hop"\n            "House" -> "house"\n            "Country" -> "country"'
)

with open('app/src/main/java/com/easeaudio/data/RadioBrowserService.kt', 'w') as f:
    f.write(content2)
