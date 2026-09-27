# Fogwawe


## Installation

<img src="assets/icon-install.png" width="36" align="left">

1. Download the APK for the version you want from [`releases/`](releases/).
2. On your Android device, allow installs from unknown sources for the app you use to open the file (Settings → Apps → Special access → Install unknown apps).
3. Open the downloaded `.apk` file and confirm the install.

Or via `adb`:

```sh
adb install releases/Fogwawe-v1.0.8.apk
```

## Verifying a download

<img src="assets/icon-verify.png" width="36" align="left">

Compare the SHA-256 checksum against the value listed in [CHANGELOG.md](CHANGELOG.md):

```sh
sha256sum releases/Fogwawe-v1.0.8.apk
```

## Repository structure

<img src="assets/icon-structure.png" width="36" align="left">

```
.
├── releases/           # Versioned APK builds (Fogwawe-vX.Y.Z.apk)
├── assets/              # Icon images
├── CHANGELOG.md         # Per-version sizes and checksums
├── LICENSE              # GNU GPLv3
└── README.md
```

## License

This project is licensed under the **GNU General Public License v3.0** — see the [LICENSE](LICENSE) file for details.
