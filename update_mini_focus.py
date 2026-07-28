with open('app/src/main/java/com/easeaudio/ui/components/MiniPlayer.kt', 'r') as f:
    content = f.read()

content = content.replace(
    'import androidx.compose.ui.focus.onFocusChanged',
    'import androidx.compose.ui.focus.onFocusChanged\nimport androidx.compose.ui.focus.focusProperties'
)

content = content.replace(
    '''                    // Favorite Button
                    var isFavFocused by remember { mutableStateOf(false) }
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .onFocusChanged { isFavFocused = it.isFocused }
                            .clip(CircleShape)
                            .background(if (isFavFocused) NeonPink else Color.Transparent)
                            .testTag("mini_player_favorite")
                    ) {''',
    '''                    // Favorite Button
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .focusProperties { canFocus = false }
                            .clip(CircleShape)
                            .background(Color.Transparent)
                            .testTag("mini_player_favorite")
                    ) {'''
)

with open('app/src/main/java/com/easeaudio/ui/components/MiniPlayer.kt', 'w') as f:
    f.write(content)
