# Glance Dictionary Widget

Native Android home-screen widget plus lightweight companion dialogs for managing language word pairs.

## Implemented Scope

- Resizable `AppWidgetProvider` using a `GridView` collection.
- Dynamic density calculation from widget width, preferred font size, and measured active word text.
- `RemoteViewsService` / `RemoteViewsFactory` backed by local SQLite data.
- Transparent dialog-style activities for settings, adding words, editing/deleting words, and category management.
- Font size stepper from 10sp to 36sp, persisted in `SharedPreferences`.
- Active category filtering persisted in `SharedPreferences`.
- Single word add and bulk parsing by first `,`, `-`, or `:` delimiter per line.
- Category delete flow with delete-all or move-to-`Uncategorized` options.
- Immediate widget refresh after data or preference changes through `notifyAppWidgetViewDataChanged`.

## Data Model

The app uses `SQLiteOpenHelper` to keep the implementation dependency-light while matching the requested SQLite schema:

- `categories(id, name UNIQUE)`
- `words(id, category_id, native_word, translated_word, date_added)`

Default category: `Uncategorized`.

## Build Notes

This project is source-complete, but the current machine does not have the Android SDK, `gradle`, or a Gradle wrapper installed, so local compilation was not run here.

To build on a machine with Android tooling:

```bash
cd language-dictionary-widget
gradle assembleDebug
```

or add a Gradle wrapper with a compatible Gradle version and run:

```bash
./gradlew assembleDebug
```

## Platform Note

Android `RemoteViews` collection widgets do not support true full-row spanning category headers inside a multi-column `GridView`. The implementation injects category header cells with distinct styling. If strict spanning headers are required, the widget should use a custom rendered bitmap/list approach or accept a single-column `ListView` layout.
