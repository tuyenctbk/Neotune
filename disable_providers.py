with open('app/src/main/AndroidManifest.xml', 'r') as f:
    content = f.read()

replacement = '''
        <provider
            android:name="com.google.android.gms.ads.MobileAdsInitProvider"
            android:authorities="${applicationId}.mobileadsinitprovider"
            tools:node="remove" />
            
        <activity
'''
content = content.replace('<activity', replacement, 1)

with open('app/src/main/AndroidManifest.xml', 'w') as f:
    f.write(content)
