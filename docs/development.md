# Development

[Español](es/development.md)

A guide to setting up the environment, building, testing and contributing.

## Versions

| Component | Version |
|---|---|
| Gradle (wrapper) | 8.9 |
| Android Gradle Plugin | 8.7.3 |
| Kotlin | 2.0.21 (with the Compose plugin) |
| KSP | 2.0.21-1.0.28 |
| Compose BOM | 2024.12.01 (Material 3) |
| Room | 2.6.1 |
| Navigation Compose | 2.8.5 |
| Lifecycle | 2.8.7 |
| JUnit | 4.13.2 |
| JDK | 17 |
| minSdk / compileSdk / targetSdk | 26 / 35 / 35 |

All library versions live in `gradle/libs.versions.toml` (version catalog). They date from
late 2024 and can be updated; when you do, bump Kotlin, KSP and the Compose plugin together,
since they are paired.

## Setting up the environment

### With Android Studio (easiest)

1. Install Android Studio and open it once so it downloads the SDK.
2. *File → Open* the project folder. Android Studio creates `local.properties` and uses its
   bundled JDK 17.
3. Run the `app` configuration on an emulator or a phone.

### Command line only (Windows)

This is how the project's current environment was set up:

1. **JDK 17**:
   ```bash
   winget install --id EclipseAdoptium.Temurin.17.JDK -e
   ```
   It is installed under `C:\Program Files\Eclipse Adoptium\jdk-17…`. If `java` is not on
   the PATH, set `JAVA_HOME` before running Gradle:
   ```bash
   export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-17.0.20.101-hotspot"
   ```
   (In PowerShell: `$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot"`.)

2. **Android SDK**: download the *command-line tools* from
   [developer.android.com/studio](https://developer.android.com/studio#command-line-tools-only)
   and unzip them into `%LOCALAPPDATA%\Android\Sdk\cmdline-tools\latest`. Then:
   ```bash
   sdkmanager --licenses
   sdkmanager "platform-tools" "platforms;android-35" "build-tools;35.0.0"
   ```
   Gradle downloads any other build-tools it needs by itself (e.g. 34).

3. **`local.properties`** at the project root (not committed):
   ```properties
   sdk.dir=C:/Users/<user>/AppData/Local/Android/Sdk
   ```
   Use forward slashes `/`; backslashes must be escaped (`C\:\\Users\\…`).

### macOS / Linux

Same steps, with JDK 17 from your package manager and the SDK in `~/Library/Android/sdk` or
`~/Android/Sdk`. The `.gitattributes` file keeps `gradlew` with Unix line endings so it
stays executable.

## Gradle tasks

| Command | What it does |
|---|---|
| `./gradlew assembleDebug` | Builds the debug APK (`app/build/outputs/apk/debug/app-debug.apk`) |
| `./gradlew installDebug` | Builds and installs on the connected device |
| `./gradlew assembleRelease` | Builds the signed, minified release APK (see [Releasing](#releasing-a-version)) |
| `./gradlew testDebugUnitTest` | Runs the analysis engine's JUnit tests |
| `./gradlew :app:compileDebugKotlin --rerun` | Recompiles Kotlin to show warnings |
| `./gradlew clean` | Deletes build output |

On Windows use `gradlew.bat` (or `./gradlew.bat` from Git Bash).

HTML test report: `app/build/reports/tests/testDebugUnitTest/index.html`.
XML results: `app/build/test-results/testDebugUnitTest/`.

The project builds **with no warnings**; keep it that way.

## Emulator

### Hardware acceleration (Windows)

The emulator needs the **Windows Hypervisor Platform** (WHPX). To enable it, from an
administrator terminal:

```bash
Dism /Online /Enable-Feature /FeatureName:HypervisorPlatform /All
```

and reboot. Virtualisation must also be enabled in the BIOS/UEFI (AMD-V or VT-x). To check:

```bash
emulator -accel-check
```

It should say *"WHPX … is installed and usable"*.

### Creating and starting the virtual device

```bash
sdkmanager "emulator" "system-images;android-35;google_apis;x86_64"
avdmanager create avd -n Verso_API35 -k "system-images;android-35;google_apis;x86_64" -d pixel_7
emulator -avd Verso_API35 -no-audio -no-boot-anim
```

Wait for boot and install:

```bash
adb wait-for-device
adb shell getprop sys.boot_completed      # 1 once it has finished
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.tuapp.verso/com.tuapp.MainActivity
```

The `applicationId` is `com.tuapp.verso`; the code's package is `com.tuapp`.

### Manual testing with adb

| Action | Command |
|---|---|
| Screenshot | `adb exec-out screencap -p > screenshot.png` |
| Tap | `adb shell input tap X Y` |
| Type (no accents or ñ) | `adb shell input text 'hola%smundo'` (`%s` = space) |
| Enter / Escape / end of line | `adb shell input keyevent 66` / `111` / `123` |
| Dark mode | `adb shell cmd uimode night yes` (and `no` to go back) |
| Wake the screen | `adb shell input keyevent 224` |
| Grant the microphone permission | `adb shell pm grant com.tuapp.verso android.permission.RECORD_AUDIO` |
| Put a test audio in Downloads | `adb push file.wav /sdcard/Download/` |
| UI tree | `adb shell uiautomator dump /sdcard/ui.xml` then `adb shell cat /sdcard/ui.xml` |
| App crashes | `adb logcat -b crash` |

Tips:
- `adb input text` doesn't accept non-ASCII characters: to test accents, type by hand in
  the emulator or use lines without them.
- Gboard may open a stylus tutorial when it receives text from adb and swallow the input.
  Turn it off with `adb shell settings put secure stylus_handwriting_enabled 0`.
- From Git Bash on Windows, prefix commands with `MSYS_NO_PATHCONV=1` so paths like
  `/sdcard/ui.xml` aren't converted into Windows paths.
- If screenshots come out black, the emulator's screen went to sleep: wake it with
  keyevent 224, or keep it on with `adb shell svc power stayon true`.

### Manual editor checklist

- [ ] The margin lines up with every line, including lines wrapped over several rows.
- [ ] Typing at the end of a long poem keeps the cursor visible above the keyboard.
- [ ] The panel collapses when the keyboard opens.
- [ ] Tapping a device highlights it and scrolls to it; tapping it again clears it.
- [ ] Clear alliteration has a solid underline; possible, a dotted one.
- [ ] Dark mode is readable (margin, highlight, panel).
- [ ] Leaving and coming back keeps the note; an emptied note disappears.
- [ ] Audios: record (with pause), attach a file, play and seek, rename, delete.
- [ ] Link audios and notes from both sides; card counters update; deleting an audio or a
      note removes only the links.
- [ ] Installing over the previous release keeps notes (and audios).

## Releasing a version

### Signing

Release builds are signed with a private key that is **not in the repository**:

- Key: `%USERPROFILE%\.verso-firma\verso-release.jks` (PKCS12, alias `verso`, RSA 4096,
  valid for ~27 years).
- Passwords and path: `keystore.properties` at the project root (in `.gitignore`), with a
  copy in `%USERPROFILE%\.verso-firma\`.

```properties
storeFile=C:/Users/<user>/.verso-firma/verso-release.jks
storePassword=…
keyAlias=verso
keyPassword=…
```

`app/build.gradle.kts` reads that file if it exists. Without it, `assembleRelease`
produces an unsigned APK, which Android refuses to install.

> **Back up the `.verso-firma` folder** (e.g. in a password manager or on an external
> drive). If the key is lost, new versions can't be installed over earlier ones, and users
> would have to uninstall the app (losing their notes).

### Release build

The release enables R8 (`isMinifyEnabled`) and resource shrinking (`isShrinkResources`):
the APK goes from ~17 MB (debug) to ~1.2 MB. Project-specific rules go in
`app/proguard-rules.pro`; Room, Compose and Navigation ship their own.
After changing dependencies, **always test the release build** on a device: R8 can strip
code that is only used through reflection.

### Steps

1. Bump `versionCode` (an integer, +1 each time) and `versionName` in `app/build.gradle.kts`.
2. Build and check:
   ```bash
   ./gradlew testDebugUnitTest assembleRelease
   apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk
   ```
3. Install it on the emulator or a phone and go through the checklist above. If a debug
   build is installed, uninstall it first (`adb uninstall com.tuapp.verso`): the
   signatures don't match. To check that updates keep existing notes, install the new
   version **over** the previous release with `adb install -r`.
4. Tag and publish (release notes in English, with a collapsible Spanish section):
   ```bash
   git tag -a v0.1.0 -m "Verso 0.1.0"
   git push origin v0.1.0
   cp app/build/outputs/apk/release/app-release.apk verso-0.1.0.apk
   gh release create v0.1.0 verso-0.1.0.apk --title "Verso 0.1.0" --notes-file notes.md
   ```

## Conventions

- **Language**:
  - App code (classes, functions, variables, comments, file and folder names) is in
    **English**. Callback parameters follow the `onSomething` pattern (`onValueChange`,
    `onSelect`).
  - The **analysis engine** (`analisis/` and its tests) stays in **Spanish**: its names
    are Spanish metrics terms (`silabas`, `sinalefa`, `rima asonante`) that have no
    exact English equivalent.
  - Everything the user sees (UI text, analysis results) is in **Spanish**.
  - Documentation is in English, with a Spanish copy in `docs/es/`. The README exists in
    English (`README.md`) and Spanish (`README.es.md`). Keep both versions in sync.
- **Engine without Android**: nothing in `analisis/` may import `android.*` or
  `androidx.*`. Anything that needs Android goes in `ui/` or `data/`.
- **Tests**: every engine change comes with tests, preferably using real lines of verse.
- **State**: screens read Compose state from the ViewModel; heavy work runs off the main
  thread.
- **UI text**: plain, friendly Spanish, without jargon (*"No hay recursos detectados
  todavía."*).
- **Design**: verses in a serif with generous line height; UI in the system sans serif; no
  shadows; theme colours, never hard-coded (so dark mode works).
- **Database compatibility**: table and column names are part of the stored data. Don't
  rename them; to rename a Kotlin property, keep the column with `@ColumnInfo(name = …)`.

## Adding a literary device

1. Add the value to the `Recursos.Tipo` enum with its display name and description (in
   Spanish).
2. Write the detector as a private function in `Recursos.kt` and call it from `detectar()`
   (inside the stanza loop if it depends on consecutive lines, or in the per-line loop if
   it concerns a single line). Return `Recurso(tipo, lineas, evidencia, palabras)`.
3. If the highlight should differ from the default (every word in `palabras`, or the whole
   line if it is empty), add a case to `AnalisisPoema.rangos`.
4. Tests in `RecursosTest` (that it is detected, and that it gives **no** false positives
   on well-known lines) and, if you touched `rangos`, in `AnalisisPoemaTest`.
5. Document the rule in [analysis-engine.md](analysis-engine.md) (and its Spanish copy).

## Changing the data model

`VersoDatabase` is at **version 2** (0.2.0: audios). Room exports each version's schema to
`app/schemas/com.tuapp.data.VersoDatabase/<version>.json` (commit these files). To change an
entity (e.g. add a `tags` column to `Note`):

1. Add the field with a default value. Keep existing table and column names.
2. Bump `version` to 3 and register a migration next to `MIGRATION_1_2`:
   ```kotlin
   val MIGRATION_2_3 = object : Migration(2, 3) {
       override fun migrate(db: SupportSQLiteDatabase) {
           db.execSQL("ALTER TABLE notas ADD COLUMN etiquetas TEXT NOT NULL DEFAULT ''")
       }
   }
   Room.databaseBuilder(…).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build()
   ```
3. Build, and copy the SQL of new tables from the generated `3.json` (`createSql`) into the
   migration, so it matches what Room expects exactly.
4. Test on a device: install the new build **over** the previous release with notes and
   audios, and check nothing is lost.

## Troubleshooting

| Symptom | Cause and fix |
|---|---|
| `java: command not found` / Gradle can't find Java | Set `JAVA_HOME` to the JDK 17 |
| `SDK location not found` | `local.properties` with `sdk.dir` is missing |
| `gradlew` won't run on Linux/macOS | `chmod +x gradlew`; check it has LF line endings |
| `Android Emulator hypervisor driver is not installed` / emulator won't start | Enable the Windows Hypervisor Platform and reboot |
| Experimental API error (`This foundation API is experimental…`) | Add `@OptIn(ExperimentalFoundationApi::class)` or `ExperimentalLayoutApi` to the function |
| `Platform declaration clash` for `setX(…)` | A `var x … private set` already generates `setX`: name the function differently (`updateX`) |
| Text typed with `adb input` doesn't appear | Gboard's stylus tutorial is open; see [the tips](#manual-testing-with-adb) |
| App crashes on launch after changing `Note` | The Room migration is missing (see above) |
