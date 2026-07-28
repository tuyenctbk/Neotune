with open('app/src/main/java/com/easeaudio/MainActivity.kt', 'r') as f:
    content = f.read()

target = '''        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        }'''
replacement = '''        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }'''
content = content.replace(target, replacement)

with open('app/src/main/java/com/easeaudio/MainActivity.kt', 'w') as f:
    f.write(content)
