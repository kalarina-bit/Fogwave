# Contributing to Fogwave

Thanks for wanting to help! This guide explains where to contribute, what fits the project and how to send changes.

## Where to contribute

Issues and pull requests go to the repository on [Gitea](https://git.skysparkle.cc/kalarina/Fogwave).

## Ways to help

- **Report a bug** — use the *Bug report* template.
- **Suggest a station** — use the *Station request* template.
- **Suggest a feature** — use the *Feature request* template; check [Out of scope](#out-of-scope) first.
- **Fix a translation** — use the *Translation fix* template or send a pull request.
- **Write code** — see [Pull requests](#pull-requests).

## Before you start

- **Small changes** (typos, obvious bugs, translation fixes): send a pull request directly.
- **Anything bigger** (new features, UI changes, new dependencies): open an issue first and wait for a go-ahead, so your work isn't wasted.

## Ground rules

- No ads, analytics, trackers, crash reporters or accounts.
- No Google Play Services, Firebase or other proprietary libraries — the app must stay buildable for F-Droid.
- New dependencies must be open source and justified in the pull request.
- Be respectful in issues and reviews.

## Out of scope

These will not be accepted:

- Stations from outside Lithuania.
- A light theme or automatic light/dark switching — Fogwave is dark only.
- New UI languages (see [Translations](#translations)).

## Stations

A new station must be Lithuanian, have an official public stream (HTTP or HTTPS) and allow playback in third-party players. Include the official website and stream URL in the request.

## Translations

Fogwave is available in exactly six languages: English, Russian, German, Japanese, Lithuanian and Chinese. Fixes to these are welcome; new languages are not accepted.

- App strings: `app/src/main/res/values-<lang>/strings.xml` (English is `values/`).
- Store listing: `fastlane/metadata/android/<locale>/`.

If your code adds new strings, add them to `values/strings.xml`. Translations for the other languages are welcome but not required — mention missing ones in the pull request.

## Pull requests

1. Fork the repository and create a branch from `main`.
2. Keep one topic per pull request.
3. Follow the existing Kotlin and Jetpack Compose style; keep comments short and in English; don't reformat unrelated code.
4. Make sure `./gradlew assembleDebug` builds, and test on a device or emulator.
5. Write short, plain commit messages in English (for example `Fix crash when stream URL is empty`).
6. Add screenshots for UI changes.

## Security

Please don't post details of security issues publicly. Open a short issue titled "Security contact" without any details, and the maintainer will reach out privately.

## License

By contributing, you agree that your contribution is licensed under the [GNU General Public License v3.0 only](LICENSE), the same as the rest of the project.
