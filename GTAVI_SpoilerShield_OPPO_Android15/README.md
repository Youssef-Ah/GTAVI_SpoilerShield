# GTA VI Spoiler Shield — OPPO / realme Android 15 build

This is an experimental personal-use Android Studio project. It watches visible accessibility text in the official Facebook and YouTube apps. When it finds a configured keyword in a visible node, it estimates the surrounding card bounds and performs a controlled upward swipe intended to move that card out of view.

## What changed in this build
- Only scans the official Facebook (`com.facebook.katana`) and YouTube (`com.google.android.youtube`) apps.
- Checks visible nodes rather than concatenating all text from the whole window.
- Uses phrase-boundary matching to reduce partial-word matches.
- Estimates a likely card from the matching node's ancestors.
- Uses a card-sized, bounded swipe rather than one fixed full-screen swipe.
- Adds cooldowns to reduce repeated gestures.
- No INTERNET permission and no network code.

## Important limitations
This is best-effort feed skipping, not true post deletion or a guaranteed per-card filter. Android AccessibilityService cannot directly remove another app's feed item. The swipe may move past the matching card, move too little, or also move nearby content. Some app content (especially text baked into thumbnails, inaccessible video surfaces, or off-screen lazy-loaded items) may not appear in the accessibility tree. Facebook and YouTube UI updates can change behavior. Test carefully before relying on it.

The app does not use OCR in this build. OCR would require additional on-device image processing and careful privacy/performance handling.

## Requirements
- Android Studio
- Android SDK 35
- JDK 17
- Android phone running Android 8.0/API 26 or newer
- Official Facebook and YouTube apps

## Build and install
1. Extract the ZIP.
2. Open the `GTAVI_SpoilerShield_OPPO_Android15` folder in Android Studio.
3. Let Gradle sync. Install SDK 35 if prompted.
4. Connect the phone with USB debugging enabled, or use Android Studio wireless debugging.
5. Press Run and select the phone.
6. Open GTA VI Spoiler Shield and save your Facebook/YouTube switches and keyword list.
7. Tap Enable filtering, review the disclosure, and open Android Accessibility settings.
8. Under Downloaded apps / Installed apps / Accessibility, enable GTA VI Spoiler Shield.

## OPPO / realme background settings
Menu names vary by model and ColorOS / realme UI version. Use Settings search if a path differs.

1. Settings > Apps > App management > GTA VI Spoiler Shield > Battery usage.
2. Enable **Allow foreground activity** and **Allow background activity**, if shown.
3. Enable **Allow auto launch** if shown. Some recent realme UI versions do not expose this option.
4. Open Recent apps and lock the app if your phone offers a lock option.
5. Keep Battery Saver / Super Power Saving from restricting this app.
6. Re-open Accessibility settings after a reboot or if filtering stops, and verify the service remains enabled.

Official realme help describes enabling foreground/background activity and auto-launch under App battery management on supported versions: https://www.realme.com/global/support/faq

## Test procedure
1. First test with a harmless temporary keyword such as `FILTERTEST123` in the app's keyword list.
2. Use a test page/video title that visibly contains that phrase.
3. Confirm that the service skips it, then remove the test keyword.
4. Test the GTA VI phrases on Facebook and YouTube.
5. If the swipe moves too much content, reduce `0.88f` in `swipeCardOut()` to `0.65f`. If it moves too little, increase it carefully, up to `1.0f`.
6. If the app repeatedly scrolls unexpectedly, disable the Accessibility service immediately and adjust cooldowns / keyword specificity.

## Privacy and permissions
The Accessibility permission is powerful: this service can inspect visible interface text and dispatch touch gestures in selected apps. This project processes text locally and contains no internet permission or upload code. Enable it only if you trust the source code you built. You can turn it off at any time in Android Accessibility settings.

## Distribution
This is a personal sideloading prototype, not a Play Store-ready release. Google Play has additional Accessibility API, disclosure, consent, and permitted-use requirements.
