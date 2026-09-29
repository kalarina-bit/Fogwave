<p align="center">
  <img src="assets/banner.jpg" width="100%" alt="Fogwawe — Tune into Lithuania. Choose a station and take it with you.">
</p>

<h1 align="center">Fogwawe</h1>

<p align="center">
  <a href="releases/Fogwawe-v1.0.9.apk"><img src="assets/get-it-on-gitea.png" height="60" alt="Get it on Gitea"></a>
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
  <img src="assets/screenshots/1-find-play-love.png" width="200" alt="Find it. Play it. Love it.">
  <img src="assets/screenshots/2-stay-awhile.png" width="200" alt="Stay awhile">
  <img src="assets/screenshots/3-favorites.png" width="200" alt="Your favorites">
  <img src="assets/screenshots/4-stations.png" width="200" alt="Browse all stations">
  <img src="assets/screenshots/5-feel-closer.png" width="200" alt="Feel closer to Lithuania">
</p>

## Installation

<img src="assets/icon-install.png" width="36" align="left">

1. Download the APK for the version you want from [`releases/`](releases/).
2. On your Android device, allow installs from unknown sources for the app you use to open the file (Settings → Apps → Special access → Install unknown apps).
3. Open the downloaded `.apk` file and confirm the install.

Or via `adb`:

```sh
adb install releases/Fogwawe-v1.0.9.apk
```

## Verifying a download

<img src="assets/icon-verify.png" width="36" align="left">

Compare the SHA-256 checksum against the value listed in [CHANGELOG.md](CHANGELOG.md):

```sh
sha256sum releases/Fogwawe-v1.0.9.apk
```

## Repository structure

<img src="assets/icon-structure.png" width="36" align="left">

```
.
├── releases/           # Versioned APK builds (Fogwawe-vX.Y.Z.apk)
├── assets/              # Icon images
│   └── screenshots/     # App screenshots
├── CHANGELOG.md         # Per-version sizes and checksums
├── LICENSE              # GNU GPLv3
└── README.md
```

## License

This project is licensed under the **GNU General Public License v3.0** — see the [LICENSE](LICENSE) file for details.
