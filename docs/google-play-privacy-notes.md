# Google Play Privacy Notes

Last reviewed: June 8, 2026

These notes are based on the current Glance Dictionary codebase and should be rechecked before each Google Play release.

## Current App Behavior

- App name: Glance Dictionary
- Package: `com.example.glancedict`
- Declared `uses-permission` entries: none
- Runtime permissions requested: none
- Internet permission: not declared
- Ads SDKs: none found
- Analytics SDKs: none found
- Crash-reporting SDKs: none found
- Account creation: not implemented
- User-entered data: words, translations, and categories
- App settings stored locally: font size, column count, active categories, collapsed categories, and display cache
- Storage: local Android app storage through SQLite and SharedPreferences
- External link: user-initiated link to `https://joinrestartabroad.com`, opened by the user's browser or chosen external app

## Suggested Google Play Data Safety Declarations

For the current app version, the Data safety form should generally indicate:

- Data collection: No user data is collected by this app.
- Data sharing: No user data is shared by this app.
- Data processed locally only: User-entered vocabulary and settings are stored on the user's device and are not transmitted off device by the app.
- Security practices: No user data is transmitted by the app, so encryption in transit is not applicable for app-collected data.
- Data deletion: Users can delete words/categories in the app, clear app storage, or uninstall the app. There is no developer-operated server-side user data to delete.
- Ads: No, this app does not contain ads.
- App access: No login or restricted access.
- Permissions declaration: No sensitive or high-risk permissions are requested.

## Publishing Requirements to Complete

- Host `docs/privacy-policy.md` as a public, active, non-geofenced, non-editable URL. Google Play does not accept a PDF as the privacy policy URL.
- Replace the privacy policy placeholders with the exact developer or company name shown in the Google Play listing and a working privacy contact method.
- Add the privacy policy URL in Play Console under Policy and programs > App content > Privacy policy.
- Include the privacy policy link or policy text inside the app itself before publishing.
- Keep the Play Console Data safety form consistent with the privacy policy and with any future SDK, permission, networking, or data-storage changes.
