with open('app/src/main/java/com/easeaudio/ui/components/MiniPlayer.kt', 'r') as f:
    content = f.read()

import_statement = "import androidx.compose.ui.platform.testTag"
new_imports = """import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.border
import androidx.compose.ui.platform.testTag"""

content = content.replace(import_statement, new_imports)

surface_old = """        if (station != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onOpenFullPlayer() }
                    .testTag("mini_player_bar"),"""

surface_new = """        if (station != null) {
            var isFocused by remember { mutableStateOf(false) }
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .onFocusChanged { isFocused = it.isFocused }
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onOpenFullPlayer() }
                    .border(
                        width = if (isFocused) 2.5.dp else 0.dp,
                        color = if (isFocused) NeonCyan else Color.Transparent,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .testTag("mini_player_bar"),"""

content = content.replace(surface_old, surface_new)

fav_old = """                    // Favorite Button
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.testTag("mini_player_favorite")
                    ) {"""

fav_new = """                    // Favorite Button
                    var isFavFocused by remember { mutableStateOf(false) }
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .onFocusChanged { isFavFocused = it.isFocused }
                            .clip(CircleShape)
                            .background(if (isFavFocused) NeonPink else Color.Transparent)
                            .testTag("mini_player_favorite")
                    ) {"""

content = content.replace(fav_old, fav_new)

play_old = """                    // Play/Pause Button
                    IconButton(
                        onClick = onTogglePlay,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .testTag("mini_player_play_pause")
                    ) {"""

play_new = """                    // Play/Pause Button
                    var isPlayFocused by remember { mutableStateOf(false) }
                    IconButton(
                        onClick = onTogglePlay,
                        modifier = Modifier
                            .size(40.dp)
                            .onFocusChanged { isPlayFocused = it.isFocused }
                            .clip(CircleShape)
                            .background(if (isPlayFocused) NeonCyan else Color.White)
                            .testTag("mini_player_play_pause")
                    ) {"""

content = content.replace(play_old, play_new)


with open('app/src/main/java/com/easeaudio/ui/components/MiniPlayer.kt', 'w') as f:
    f.write(content)
