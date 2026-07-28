with open('app/src/main/java/com/easeaudio/ui/components/MiniPlayer.kt', 'r') as f:
    content = f.read()

content = content.replace(
    'tint = if (isFavFocused) DarkBackground else if (station.isFavorite) NeonPink else TextMuted',
    'tint = if (station.isFavorite) NeonPink else TextMuted'
)

with open('app/src/main/java/com/easeaudio/ui/components/MiniPlayer.kt', 'w') as f:
    f.write(content)
