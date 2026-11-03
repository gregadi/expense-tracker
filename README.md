# Expense Tracker

A personal, offline Android expense tracker for a Galaxy Z Fold. No server, no account: everything lives in an
on-device Room database. Kotlin + Jetpack Compose, minimalist UI, separate layouts for the cover screen and the
inner screen.

## What it does

- **Quick add**: Expense / Income switch, amount keypad, payment method chips (last used is preselected),
  category or income source, date, note. The amount sits right above the keypad; in landscape the sheet
  splits into two columns so it never scrolls out of view.
- **Income and savings**: record money coming in (Salary, Freelance, Business, Gift, Interest, Other income).
  Home and Insights show Received, Spent and Saved for any day, week, month or year.
- **Payment methods**: Cash, BCA Debit, QRIS, GoPay, OVO, DANA, ShopeePay, Credit card to start; add, rename,
  recolour or archive your own in Settings > Payment methods & categories.
- **Insights**: Day / Week / Month / Year, total, trend chart, and spending per payment method and per category.
- **History**: grouped by day, All / Expenses / Income filter, search, filter by method or category,
  swipe left to delete (with undo), tap to edit.
- **Backup**: save and restore a JSON file, export CSV for Sheets/Excel. Android Auto Backup is switched on too.

Amounts are whole rupiah stored as `Long`. Weeks start on Monday.

**Look and motion ("Ink")**: dark-first, near-black ground with one lime accent (`ui/theme/Theme.kt`).
Numbers count up and roll when they change, charts and bars grow in, the keypad amount rolls digit by digit,
tabs and Settings slide, list entries animate in and out, and taps spring (`ui/components/Motion.kt`).
Animations follow the phone's "Remove animations" setting.

## Run it

1. Open this folder in **Android Studio** (File > Open). Let Gradle sync; it downloads Gradle 8.10.2 and the
   Android SDK pieces it needs. Android Studio may offer to upgrade the Android Gradle Plugin; you can accept.
2. On the Fold: Settings > About phone > tap *Build number* 7 times, then Settings > Developer options >
   turn on **USB debugging** (or **Wireless debugging**).
3. Plug the phone in, accept the prompt on the phone, pick it in the device dropdown, press **Run**.

No Gradle wrapper jar is included (it could not be downloaded when this project was generated). Android Studio
doesn't need it. To use `./gradlew` from a terminal, run `gradle wrapper` once (needs a local Gradle install).

### Install without a cable

`Build > Build APK(s)` (or `./gradlew assembleRelease`) produces `app/build/outputs/apk/release/app-release.apk`.
Send it to the phone (Drive, AirDrop-style share, USB file transfer), open it, and allow "install unknown apps"
for the app you opened it from. The release build is signed with your local debug key so it installs as is;
create your own keystore before sharing the app with anyone, and keep it safe, because every update must be
signed with the same key.

## Layout

```
app/src/main/java/com/gregoryadi/expenses/
  data/db/          Room entities, DAOs, database
  data/repository/  ExpenseRepository, LookupRepository
  data/port/        JSON/CSV export and JSON import
  data/prefs/       SettingsStore (last-used method, theme)
  domain/           Period (day/week/month/year), bucketing, Money, defaults
  ui/               ExpensesRoot (app shell), home/ add/ history/ insights/ manage/ settings/
  ui/components/    charts, cards, expense row
  ui/theme/         colours, type, shapes
```

Screens > ViewModels > repositories > DAOs. Dependencies are wired by hand in `AppContainer`.

## Folding

`WindowSizeClass` decides the layout. Compact (cover screen): bottom bar, single column. Anything wider
(the inner screen): navigation rail and two panes (History list + detail, Insights trend + breakdowns).
The activity handles size changes itself, and the add-expense draft lives in a ViewModel, so unfolding
mid-entry keeps what you typed.

## Tests

`./gradlew testDebugUnitTest` runs unit tests for period ranges, trend buckets, rupiah formatting and savings.

## Not built yet (roadmap milestones 4 and 5)

Favorites, budgets, recurring expenses, home screen widget, app lock, top-ups/transfers, receipt photos,
Flex-mode layout, date-range filter in History, reordering payment methods.

## Notes

- Room exports its schema to `app/schemas/`. Commit it; every schema change needs a migration so updates
  never wipe your data.
- Database v2 (income) adds a `kind` column to `expense` and `category` (`MIGRATION_1_2` in `AppDatabase`).
  Existing entries become expenses; income sources are seeded on the next launch. Old backups import as expenses.
- Versions are pinned in `gradle/libs.versions.toml`.
