# Unhook

An app that helps you stop doom scrolling. Unhook nudges you when you open your social apps, suggests something else to do, locks the apps when you have used them for too long, and shows your progress week by week.

## Features

- A notification with a suggestion from your list of alternatives (“How about going for a walk?”) when you open one of the selected apps, and again after 5, 10 and 15 minutes
- Own time per app: every app can get its own limit for the timeout. Apps without their own time follow the main time
- Code lock: the app can be locked with a four-digit code and, in the Android app, a fingerprint
- Daily limit: an upper limit for how long you can use the selected apps per day. When it is reached, the apps are locked until midnight
- A timer showing the time since you last scrolled for more than 5 minutes in a row
- A guided breathing exercise to ride out the urge
- Your own list of alternatives to scrolling
- Logging of triggers such as boredom, stress and procrastination
- Blocking of selected apps at set times
- Timeout: after 2 minutes in total, all selected apps are locked, and they open again after 10 (both numbers can be changed, and the feature can be turned off)
- An overview of when during the day you scroll
- This week compared with last week, with charts
- Your own “why”, shown when you need it
- Light and dark mode
- All data is stored only on the device. The app fetches nothing from the internet, and the fonts are bundled in the app itself

## Structure

```
docs/index.html        The web app (HTML, CSS and JS in one file). Also used as the interface of the Android app.
docs/privacy.html      Privacy policy
docs/manifest.json     PWA manifest, so the app can be installed
docs/sw.js             Service worker for installing and offline use
docs/icons/            App icons
docs/fonts-licenses/   Licences for the bundled fonts
android/               The Android app (Kotlin). The build file for the app is app/app.gradle.kts, not build.gradle.kts
  app/src/main/java/com/unhook/app/
    MainActivity.kt          Shows the web app in a WebView
    Bridge.kt                Connects the web app to Android (window.Android)
    UsageMonitorService.kt   Watches which app is open with UsageStatsManager
    Apps.kt                  Maps app names to package names
    TimeoutTracker.kt        Counts usage for the timeout, including own time per app
    Biometric.kt             Fingerprint prompt for the code lock
    Schedule.kt              Fixed blocking times
    BlockActivity.kt         The screen shown over locked apps
    Notifier.kt              Notifications
    BootReceiver.kt          Restarts the monitoring after a reboot
.github/workflows/     Builds the APK and AAB automatically on GitHub
```

## The web version

Open `docs/index.html` in a browser. In the web version you start sessions yourself, because a web page cannot see other apps.

**Publish with GitHub Pages:** Go to *Settings → Pages*, choose *Deploy from a branch*, branch `main` and folder `/docs`.

**Install as an app (PWA):** The first time you open the GitHub Pages link, Unhook asks whether you want to install it. On Android in Chrome it installs with one tap. On iPhone the app shows how to add it via *Share → Add to Home Screen*. The option is also under *More*.

`index.html` is always fetched from the network first, so updates arrive without you having to change the cache name in `sw.js`. Only bump `CACHE` when you change icons or the manifest.

Note: the PWA cannot see other apps. Automatic sessions and blocking require the Android app in `android/`.

## The Android app

The Android app shows the same web app, but records sessions automatically with `UsageStatsManager`.

### Build

**Android Studio:** Open the `android/` folder and press *Run*. Android Studio creates the Gradle wrapper itself.

**Command line:** Requires JDK 17, the Android SDK and Gradle 8.13.

```bash
cd android
gradle wrapper --gradle-version 8.13   # first time
./gradlew assembleDebug
```

The APK ends up in `android/app/build/outputs/apk/debug/`.

**GitHub Actions:** Every push to `main` builds a debug APK. Download it from the *Actions* tab under *Artifacts*.

### Permissions

The first time the app is opened, it shows a setup guide that walks through all the permissions one by one. It can be opened again under *More → Setup*.

| Permission | Why |
|---|---|
| Usage access | See which app is open. Turned on under *Settings → Usage access*. |
| Notifications | Suggestions when you open an app and after 5, 10 and 15 minutes. |
| Display over other apps | Show the lock screen. Without it you get a notification instead. |
| Foreground service | Keep the monitoring running in the background. |
| Biometrics | Unlock the app with a fingerprint, if you turn that on. |

### How the monitoring works

`UsageMonitorService` reads app events every 3 seconds. A session starts when a social app or browser comes to the foreground, and continues if you switch between such apps. It ends when you have been away for 20 seconds, or when the screen turns off. Sessions shorter than 30 seconds are not saved.

Known apps are listed in `Apps.kt`. Other apps you choose from the list of installed apps under *More → Apps*.

For browsers, the app only knows that the browser is open, not which page you are on.

## Publishing on Google Play

The app is built as a signed AAB file by GitHub Actions. PWABuilder is not needed.

- **Package name:** `io.github.gangstagggg.unhook` (set in `android/app/app.gradle.kts`). It cannot be changed after publishing.
- **Target API 36** (Android 16), with Android Gradle Plugin 8.13.2, Gradle 8.13 and Kotlin 2.2.21.
- **Version number:** `versionCode` is set automatically from the run number in GitHub Actions, so every upload has a higher number.
- **Privacy policy:** `docs/privacy.html`, published on GitHub Pages. Fill in your email address in the file before you submit.

Google Play requires a justification for usage access, display over other apps and a foreground service of the type `specialUse`. Describe that the app helps the user reduce their own screen time.

### Create the upload key and secrets

1. Install JDK 17 and run in a terminal:
   ```
   keytool -genkeypair -v -keystore upload.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000
   ```
   Choose a password and keep `upload.jks` and the password safe, outside the repo.
2. Turn the file into text (PowerShell):
   ```
   [Convert]::ToBase64String([IO.File]::ReadAllBytes("upload.jks")) | Set-Clipboard
   ```
3. In the repo: *Settings → Secrets and variables → Actions → New repository secret*. Create these four:

   | Name | Content |
   |---|---|
   | `UPLOAD_KEYSTORE_BASE64` | The text from step 2 |
   | `UPLOAD_KEYSTORE_PASSWORD` | The password of the key file |
   | `UPLOAD_KEY_ALIAS` | `upload` |
   | `UPLOAD_KEY_PASSWORD` | The password of the key (the same as above if you did not choose a separate one) |

4. Run *Actions → Android → Run workflow*. The finished file is under *Artifacts* as `unhook-release-aab`. Upload it in Play Console.

In Play Console, use *Play App Signing*. Then the key you created is only the upload key, and Google holds the actual signing key.

## Future work

- A real home screen widget (today only a preview is shown)
- An iOS version with the Screen Time API (FamilyControls and DeviceActivity)

## Licence

MIT. See `LICENSE`. The fonts (Bricolage Grotesque, Unbounded and Instrument Sans) are used under the SIL Open Font License 1.1. The licences are in `docs/fonts-licenses/`.
