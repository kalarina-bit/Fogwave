# Fogwawe

Android APK builds for **Fogwawe**, organized as sequential, incrementally versioned releases (`v1.0.0` → `v1.0.8`).

> **Note:** none of these builds have a recoverable build timestamp (reproducible Android builds normalize internal file dates), so versions are numbered in the order the APKs were provided. `v1.0.8` is the latest.

## Releases

| Version | APK |
|---|---|
| **v1.0.8** (latest) | [`releases/Fogwawe-v1.0.8.apk`](releases/Fogwawe-v1.0.8.apk) |
| v1.0.7 | [`releases/Fogwawe-v1.0.7.apk`](releases/Fogwawe-v1.0.7.apk) |
| v1.0.6 | [`releases/Fogwawe-v1.0.6.apk`](releases/Fogwawe-v1.0.6.apk) |
| v1.0.5 | [`releases/Fogwawe-v1.0.5.apk`](releases/Fogwawe-v1.0.5.apk) |
| v1.0.4 | [`releases/Fogwawe-v1.0.4.apk`](releases/Fogwawe-v1.0.4.apk) |
| v1.0.3 | [`releases/Fogwawe-v1.0.3.apk`](releases/Fogwawe-v1.0.3.apk) |
| v1.0.2 | [`releases/Fogwawe-v1.0.2.apk`](releases/Fogwawe-v1.0.2.apk) |
| v1.0.1 | [`releases/Fogwawe-v1.0.1.apk`](releases/Fogwawe-v1.0.1.apk) |
| v1.0.0 | [`releases/Fogwawe-v1.0.0.apk`](releases/Fogwawe-v1.0.0.apk) |

Full history and checksums: see [CHANGELOG.md](CHANGELOG.md).

## Installation

1. Download the APK for the version you want from [`releases/`](releases/).
2. On your Android device, allow installs from unknown sources for the app you use to open the file (Settings → Apps → Special access → Install unknown apps).
3. Open the downloaded `.apk` file and confirm the install.

Or via `adb`:

```sh
adb install releases/Fogwawe-v1.0.8.apk
```

## Verifying a download

Compare the SHA-256 checksum against the value listed in [CHANGELOG.md](CHANGELOG.md):

```sh
sha256sum releases/Fogwawe-v1.0.8.apk
```

## Repository structure

```
.
├── releases/           # Versioned APK builds (Fogwawe-vX.Y.Z.apk)
├── CHANGELOG.md         # Per-version sizes and checksums
├── LICENSE              # GNU GPLv3
└── README.md
```

## License

This project is licensed under the **GNU General Public License v3.0** — see the [LICENSE](LICENSE) file for details.
