with open('app/src/main/java/com/easeaudio/ui/components/MiniPlayer.kt', 'r') as f:
    content = f.read()

icon_old = """                        Icon(
                            imageVector = if (station.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (station.isFavorite) NeonPink else TextMuted
                        )"""

icon_new = """                        Icon(
                            imageVector = if (station.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavFocused) DarkBackground else if (station.isFavorite) NeonPink else TextMuted
                        )"""

content = content.replace(icon_old, icon_new)

with open('app/src/main/java/com/easeaudio/ui/components/MiniPlayer.kt', 'w') as f:
    f.write(content)
