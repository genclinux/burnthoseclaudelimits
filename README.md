# Noor · نور — Islamic wallpapers, made for Hanife Betül

An Android app that draws Islamic art on the device at its native resolution, so
nothing is downloaded and nothing is upscaled. It is tuned for the
**OPPO Find X9 Pro** (6.78" LTPO OLED, **1272 × 2772**, ColorOS 16 / Android 16)
and works on any Android 10+ phone or **tablet**.

![Gallery](docs/noor-gallery.jpg)

## For Hanife Betül

The app was made for Hanife Betül, and it's full of not-so-secret easter eggs
for her (in Turkish):

- **Her own collection**, first in the gallery: her name in a calligraphy
  medallion, and the two verses her names come from. *Hanîf* is in Rûm 30:30.
  *Betül* shares its root with *tebettül* in Müzzemmil 73:8. The collection
  also has a dusk mosque, a "Nur" lantern scene, a mihrab with her name
  inscribed, and a rosette.
- **A "Hanife Betül" palette** (rose gold, deep teal, dusty rose and sage) that
  works on every design.
- **An "HB" constellation** hidden in every night sky and lantern scene, and a
  tiny **H·B star** at the foot of every wallpaper.
- **A shooting star** in the live wallpaper every 37 seconds. Make a wish.
- **Greetings** in the header: time-of-day greetings, Friday greetings (Hayırlı
  Cumalar), and Ramadan and Eid greetings worked out from the Hijri calendar.
- **A welcome note** on first launch. A **✦ button** (or five taps on the
  "Noor" title) opens a dedication page explaining what her names mean.
- **Personal messages** when she sets a wallpaper, a compliment on every
  seventh shuffle, and a personal note when her favourites list is empty.

## Phones and tablets

On a phone the wallpaper is rendered at the exact panel size. A tablet can be
held either way up, so the app renders a square wallpaper as wide as the
panel's long side. The main subject sits in the centred square that stays
visible in both orientations, and the background fills the rest. The live
wallpaper redraws itself for each orientation.

| Tablet, portrait | Tablet, landscape |
|---|---|
| ![](docs/noor-tablet-portrait.jpg) | ![](docs/noor-tablet-landscape.jpg) |

## What's inside

| Category | Designs |
|---|---|
| **Geometric** | Star patterns built with Hankin's *polygons-in-contact* method over five tilings: Khatam, Rub el Hizb, Shamsa, twelvefold Rosette and Hexagram. Each comes in three styles: gold strapwork, Fez/Marrakesh zellige, and OLED-friendly gold linework. |
| **Calligraphy** | Bismillah, Allah, Muhammad, SubhanAllah, Alhamdulillah, Allahu Akbar, the Tawhid, MashaAllah, and verses 94:6, 3:173, 2:152 and 57:4. Set in Naskh (Amiri), Ruqaa (Aref Ruqaa) and Kufi (Reem Kufi), each with a transliteration and its meaning. |
| **Night Mosques** | Procedural Ottoman, Persian and Mughal mosque silhouettes under night, dusk or dawn skies. Lit windows, a crescent moon, and optional reflections in water. |
| **Ramadan & Eid** | Hanging fanous lanterns, a crescent, and *Ramadan Kareem* / *Eid Mubarak* calligraphy. |
| **Mihrab** | A tiled wall with a gilded pointed arch, a Bismillah inscription and a glowing Mamluk glass lamp. |

There are 44 designs, and each can use any of the **9 palettes**: Hanife Betül, Emerald & Gold,
Midnight Lapis, Iznik Turquoise, Desert Sand, Isfahan Rose, Alhambra
Terracotta, Onyx & Pearl (true black for OLED) and Royal Amethyst. **Shuffle**
re-seeds the design (moon position, lantern layout, mosque details and so on).

On the detail screen you can:

- **Set wallpaper** on the home screen, the lock screen or both. The bitmap is
  rendered at the exact panel size, so ColorOS doesn't crop or scroll it.
- Pick **Live wallpaper** for the same design with slowly twinkling stars and a
  drifting band of light. It animates only while visible, at 20 fps.
- **Save** a PNG to `Pictures/Noor` (no storage permission needed).
- Add designs to **Favourites**. Your palette and seed choices are remembered for
  each design.

## Install

Every push builds an APK in GitHub Actions (**Actions → Android build →
`noor-apk` artifact**). `app-release.apk` is minified and signed with the
debug key, so it installs directly:

1. On the Find X9 Pro, open the APK and allow *Install unknown apps* for your
   browser or file manager when ColorOS asks.
2. Open **Noor**, choose a design, then tap **Set wallpaper**.

To build it yourself, use JDK 17 and the Android SDK (API 36):

```bash
./gradlew assembleRelease   # app/build/outputs/apk/release/app-release.apk
./gradlew testDebugUnitTest # geometry engine tests
```

## How it works

```
app/src/main/java/com/noor/wallpapers/
├── art/          Pure Kotlin scene engine (no Android imports)
│   ├── StarPatterns.kt   tilings + Hankin construction
│   ├── GeometricArt.kt   strapwork / zellige / linework styles
│   ├── CalligraphyArt.kt phrases, medallion and frame
│   ├── NightArt.kt       sky, moon, procedural mosque silhouettes
│   ├── LanternArt.kt     Ramadan & Eid lanterns
│   ├── MihrabArt.kt      niche, arch, lamp
│   ├── Ambient.kt        live-wallpaper animation layer
│   └── Catalog.kt        the gallery
├── wallpaper/    Android renderer, WallpaperManager / MediaStore, live wallpaper service
└── ui/           Jetpack Compose gallery and detail screens
```

Each design emits a `Scene`, which is a list of paths, gradients and text in a
platform-neutral form. On the phone, `AndroidRenderer` draws it onto a `Canvas`.
`tools/preview` draws the same scenes with Java2D, so you can work on the art
on any desktop JVM, no Android SDK needed:

```bash
gradle -p tools/preview run -Pscale=0.5   # phone + tablet PNGs and contact sheets in tools/preview/build/previews
gradle -p tools/preview run -Pdevice=tablet_landscape -Ponly=hb
gradle -p tools/preview test              # same engine tests, on plain JVM
```

## Credits

The fonts are bundled under the SIL Open Font License 1.1. Their licence texts
are in `app/src/main/assets/fonts/`:

- [Amiri](https://github.com/aliftype/amiri) by Khaled Hosny
- [Aref Ruqaa](https://github.com/aliftype/aref-ruqaa)
- [Reem Kufi](https://github.com/aliftype/reem-kufi)

The app generates all the artwork itself. It contains no third-party images.
