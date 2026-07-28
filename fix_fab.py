with open('app/src/main/java/com/easeaudio/ui/screens/HomeScreen.kt', 'r') as f:
    content = f.read()

fab_old = """                FloatingActionButton(
                    onClick = onOpenAddStation,
                    containerColor = NeonCyan,
                    contentColor = DarkBackground,
                    shape = CircleShape,
                    modifier = Modifier
                        .navigationBarsPadding()
                        .testTag("fab_add_station")
                ) {"""

fab_new = """                var isFabFocused by remember { mutableStateOf(false) }
                FloatingActionButton(
                    onClick = onOpenAddStation,
                    containerColor = if (isFabFocused) Color.White else NeonCyan,
                    contentColor = DarkBackground,
                    shape = CircleShape,
                    modifier = Modifier
                        .navigationBarsPadding()
                        .onFocusChanged { isFabFocused = it.isFocused }
                        .border(
                            width = if (isFabFocused) 3.dp else 0.dp,
                            color = if (isFabFocused) NeonCyan else Color.Transparent,
                            shape = CircleShape
                        )
                        .testTag("fab_add_station")
                ) {"""

content = content.replace(fab_old, fab_new)

with open('app/src/main/java/com/easeaudio/ui/screens/HomeScreen.kt', 'w') as f:
    f.write(content)
