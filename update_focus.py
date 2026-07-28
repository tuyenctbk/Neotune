with open('app/src/main/java/com/easeaudio/ui/screens/HomeScreen.kt', 'r') as f:
    content = f.read()

content = content.replace(
    'import androidx.compose.ui.focus.onFocusChanged',
    'import androidx.compose.ui.focus.onFocusChanged\nimport androidx.compose.ui.focus.focusProperties'
)

content = content.replace(
    '''            // Favorite Button
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.testTag("favorite_button_${station.id}")
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
