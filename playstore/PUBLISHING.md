# Publishing TapShare to Google Play Store

## Prerequisites

1. **Google Play Developer Account** ($25 one-time fee)
   - Sign up at https://play.google.com/console

2. **Signed AAB file** (already built)
   - Location: `release/TapShare-release.aab`

3. **Privacy Policy** (required because the app uses Location permission)
   - Host it on any public URL (GitHub Pages, your website, etc.)
   - Must disclose: Location permission is used only for WiFi Direct peer discovery, no data is collected

## Step-by-Step Publishing

### 1. Create the App

1. Go to [Google Play Console](https://play.google.com/console)
2. Click **"Create app"**
3. Fill in:
   - App name: **TapShare — Tap to Share**
   - Default language: English
   - App or Game: **App**
   - Free or Paid: **Free**
4. Accept the declarations and click **Create app**

### 2. Set Up Your Store Listing

Go to **Grow > Store presence > Main store listing**:

- **Short description**: Copy from `playstore/listing.md`
- **Full description**: Copy from `playstore/listing.md`
- **App icon**: 512x512 PNG (use the launcher icon or design a high-res version)
- **Feature graphic**: 1024x500 PNG (promotional banner)
- **Screenshots**: At least 2 phone screenshots (take from your device after installing)
  - Recommended: Home screen, Send screen, Receive screen, Transfer complete screen

### 3. Content Rating

Go to **Policy > App content > Content rating**:
1. Start the questionnaire
2. Category: **Utility** → answer all questions
3. The app should receive an **Everyone** rating

### 4. Target Audience

Go to **Policy > App content > Target audience**:
- Select **18+** (simplest option to avoid children's policy requirements)

### 5. Privacy Policy

Go to **Policy > App content > Privacy policy**:
- Enter the URL of your hosted privacy policy

### 6. Data Safety

Go to **Policy > App content > Data safety**:
- The app does NOT collect or share any user data
- Location is used only locally for WiFi Direct, not collected
- Fill in the form accordingly

### 7. Upload the AAB

Go to **Release > Production** (or start with **Testing > Internal testing** first):

1. Click **"Create new release"**
2. If prompted about Play App Signing, **opt in** (recommended by Google)
3. Upload `release/TapShare-release.aab`
4. Release name: `1.0.0`
5. Release notes:
   ```
   Initial release of TapShare!
   • Share text, images, files, and more by touching phones together
   • Automatic NFC detection and WiFi Direct transfer
   • Share from any app via Android's share menu
   • Real-time transfer progress with notifications
   ```
6. Click **Review release** → **Start rollout**

### 8. Wait for Review

Google typically reviews new apps within **1-3 days**. You'll receive an email when approved.

## Recommended: Start with Internal Testing

Instead of going straight to Production, do a **closed internal test** first:

1. Go to **Testing > Internal testing**
2. Create a release with the AAB
3. Add testers by email
4. Testers get a link to install via Play Store
5. Once verified, promote to Production

## Keystore Management

⚠️ **CRITICAL**: Keep your keystore safe!

- Keystore: `keystore/tapshare-release.jks`
- Config: `keystore.properties`
- **Back up both files** — if you lose them, you can never update the app on Play Store
- Never commit the keystore or properties file to git (they're in `.gitignore`)
- If using Play App Signing, Google holds the upload key, but you still need the keystore for updates

## Updating the App

For future updates:

1. Increment `versionCode` and `versionName` in `app/build.gradle.kts`
2. Build: `./gradlew bundleRelease`
3. Upload the new AAB to Play Console
4. Add release notes
5. Roll out

## App Requirements Checklist

- [x] Signed release AAB
- [x] App targets API 34 (Android 14) — meets Play Store requirements
- [x] NFC feature declared as required
- [x] All permissions declared in manifest
- [ ] Privacy policy URL (you must create and host this)
- [ ] App icon (512x512) for store listing
- [ ] Feature graphic (1024x500)
- [ ] At least 2 screenshots
- [ ] Store listing text (provided in listing.md)
