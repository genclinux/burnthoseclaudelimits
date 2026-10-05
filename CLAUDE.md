# HBSnoor

Android app (Kotlin, Jetpack Compose, minSdk 29) for Hanife Betül: Islamic
wallpapers drawn in code, real mosque photos from Wikimedia Commons, Diyanet
prayer times, qibla, tesbih and a Hijri calendar. The UI text is Turkish.

## Rules for every change

- **Every user-visible new feature gets a "Yeni" popup.** Add an entry at the
  top of `WhatsNew.ALL` (`app/src/main/java/com/noor/wallpapers/WhatsNew.kt`)
  with the version it ships in, in Turkish, and an `Action` if the feature can
  be tried straight away. Never reuse or rename an id. `WhatsNewTest` fails if
  the newest entry's version differs from `versionName`.
- **Use the 2.0 design system** in `ui/Design.kt`: `ScreenHeader` at the top of
  every tab (with the settings gear), `NoorCard`/`InfoCard`, `SectionTitle`,
  `OptionRow`, `ChoiceChips`/`NoorChip`, `NoorSheet`, `Footnote`, and the `Noor`
  spacing and shape tokens. Don't add one-off paddings, radii or title styles.
- Bump `versionCode` and `versionName` in `app/build.gradle.kts` for each release.

## Building and releasing

- Google's Maven is not reachable from the Claude cloud sandbox, so Gradle
  can't build here. Pure-Kotlin logic (scheduling, Commons parsing, WhatsNew)
  can be compiled and unit-tested standalone with `kotlin-compiler-embeddable`
  from Maven Central; everything else is verified by CI.
- CI (`.github/workflows/android.yml`) runs unit tests and builds APKs on pushes
  to `main`/`claude/**` and on pull requests. A release is published by running
  the workflow by hand with `release_tag` (e.g. `v2.0.0`), or by pushing a `v*`
  tag; it only publishes if the tests and build pass.
- The `device-previews` job renders every wallpaper and screenshots the app's
  tabs on an emulator, and force-pushes contact sheets to the
  `device-previews` branch (`git fetch origin device-previews`).
