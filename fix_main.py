with open('app/src/main/java/com/easeaudio/MainActivity.kt', 'r') as f:
    content = f.read()

target = '''    @android.annotation.SuppressLint("InvalidFragmentVersionForActivityResult")
    // Triggering rebuild again
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        AppThemeState.loadTheme(applicationContext)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                Log.d("MainActivity", "Notification permission granted: $isGranted")
            }.launch(Manifest.permission.POST_NOTIFICATIONS)
        }'''

replacement = '''    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        Log.d("MainActivity", "Notification permission granted: $isGranted")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        AppThemeState.loadTheme(applicationContext)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }'''

content = content.replace(target, replacement)

with open('app/src/main/java/com/easeaudio/MainActivity.kt', 'w') as f:
    f.write(content)
