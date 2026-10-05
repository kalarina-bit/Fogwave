<p align="center">
  <img src="assets/icon-rounded.png" width="128" alt="Fogwawe icon">
</p>

<h1 align="center">Fogwave>

<p align="center">
  <a href="https://git.skysparkle.cc/kalarina/Fogwave/releases"><img src="assets/get-it-on-gitea.png" height="60" alt="Get it on Gitea"></a>
</p>

<p align="center">
  <img src="https://img.shields.io/github/downloads/kalarina-bit/Fogwave/total?label=downloads&color=2e7d32&labelColor=1b1b1b" alt="downloads counter">
</p>

**A calm, open-source internet radio app for Android.**

Listen to Lithuania's favorite stations in one tap — with background playback, a built-in equalizer, and a clean, distraction-free design.

## Features

- 📻 **10 Lithuanian stations** — ZIP FM, Power Hit Radio, Rock FM, Relax FM, M-1, Gold FM and more
- 🎧 **Background playback** with lock-screen, notification, headphone and Bluetooth controls
- 🚗 **Android Auto** support
- 🔄 **Auto-reconnect** when the stream drops or the network comes back
- 🎚️ **Equalizer** with presets, loudness boost and dynamics processing — settings are saved
- ❤️ **Favorites, recently played and similar stations** matched by genre
- 🗂️ **Four layouts** — list, table, tiles or icons, with adjustable size
- 🌙 **Clean, elegant UI** in a dark theme
- 🌍 Available in English, Russian, German, Japanese, Lithuanian, and Chinese
- 🔓 **100% open source** — no ads, no tracking, no accounts

## Screenshots

<p align="center">
  <img src="assets/screenshots/5-feel-closer.png" width="200" alt="Feel closer to Lithuania">
  <img src="assets/screenshots/4-stations.png" width="200" alt="Browse all stations">
  <img src="assets/screenshots/1-find-station.png" width="200" alt="Find a station, then settle in">
  <img src="assets/screenshots/2-your-style.png" width="200" alt="Just your style">
  <img src="assets/screenshots/3-soundtrack.png" width="200" alt="Your soundtrack">
</p>

## Installation

<img src="assets/icon-install.png" width="36" align="left">

1. Download the APK for the version you want from the [Releases page](https://github.com/kalarina-bit/Fogwave/releases).
2. On your Android device, allow installs from unknown sources for the app you use to open the file (Settings → Apps → Special access → Install unknown apps).
3. Open the downloaded `.apk` file and confirm the install.

Or via `adb` (after downloading):

```sh
adb install Fogwawe-v1.0.10.apk
```

## Verifying a download

<img src="assets/icon-verify.png" width="36" align="left">

Compare the SHA-256 checksum against the value listed in [CHANGELOG.md](CHANGELOG.md):

```sh
sha256sum Fogwawe-v1.0.10.apk
```

## Repository structure

<img src="assets/icon-structure.png" width="36" align="left">

```
.
├── assets/              # Icon images
│   └── screenshots/     # App screenshots
├── CHANGELOG.md         # Per-version sizes and checksums (links to GitHub Releases)
├── LICENSE              # GNU GPLv3
└── README.md
```

APK builds themselves are published as assets on the [Releases page](https://github.com/kalarina-bit/Fogwawe/releases), not stored in this repository.

## License

This project is licensed under the **GNU General Public License v3.0** — see the [LICENSE](LICENSE) file for details.
