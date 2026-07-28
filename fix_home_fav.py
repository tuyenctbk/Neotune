with open('app/src/main/java/com/easeaudio/ui/screens/HomeScreen.kt', 'r') as f:
    content = f.read()

content = content.replace(
    '''            // Favorite Button
            var isFavFocused by remember { mutableStateOf(false) }
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier
                    .focusProperties { canFocus = false }
                    .testTag("favorite_button_${station.id}")
            ) {''',
    '''            // Favorite Button
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier
                    .focusProperties { canFocus = false }
                    .testTag("favorite_button_${station.id}")
            ) {'''
)

with open('app/src/main/java/com/easeaudio/ui/screens/HomeScreen.kt', 'w') as f:
    f.write(content)
