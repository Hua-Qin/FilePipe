# FilePipe Flavor Differences

FilePipe is built using three product flavors under the `distribution` dimension: **`github`**, **`fdroid`**, and **`playstore`**.

| Feature / Characteristic | GitHub Flavor (`github`) | F-Droid Flavor (`fdroid`) | Play Store Flavor (`playstore`) |
| :--- | :--- | :--- | :--- |
| **Application ID** | `dev.bikram.filepipe.gh` | `dev.bikram.filepipe.gh` | `dev.bikram.filepipe` |
| **Side-by-Side Installation** | Can be installed beside Play Store. Cannot be installed beside F-Droid because both use the `.gh` package ID. | Can be installed beside Play Store. Cannot be installed beside GitHub because both use the `.gh` package ID. | Can be installed beside GitHub or F-Droid because those use the `.gh` package ID. |
| **Update Check Source** | GitHub Release API for `bikram-agarwal/filepipe`. | F-Droid package API for the installed package ID. | Google Play Core In-App Updates. |
| **Update Action** | Downloads the release APK to app cache and launches the system package installer. | Opens `fdroid.app:dev.bikram.filepipe.gh` so the user's installed FOSS package client handles the update. If no FOSS client handles that deep link, the app falls back to the app web page. | Starts the Play Core in-app update flow. |
| **Manifest Permissions** | Requests `REQUEST_INSTALL_PACKAGES` and `USE_EXACT_ALARM`. | Requests `USE_EXACT_ALARM`. Does not request install-package permission. | Requests `SCHEDULE_EXACT_ALARM`. Does not request install-package permission. |
| **Save Update APK to Downloads** | Yes, via the `saveUpdateApkToDownloads` setting. | No. Updates are delegated to the user's F-Droid-compatible client. | No. Updates are delegated to Play Core. |
| **In-App Rating / Review** | Stubbed out. Does not prompt for Play ratings. | Stubbed out. Does not prompt for Play ratings. | Uses the Google Play In-App Review API with automated prompt scheduling. |
| **Cross-Promo Cards** | Shows Remember and ObtainX cards. Tapping opens each app's webpage. | Shows Remember and ObtainX cards. Tapping first tries `fdroid.app:<target_package_id>`, then falls back to the target app's webpage. | Shows Remember only. Tapping opens the Remember Play Store listing. |

---

## Backup Portability FAQ

### Are backups portable between the GitHub, F-Droid, and Play Store flavors?

**Yes, the backup files (`filepipe_backup_*.json`) are fully portable between all three flavors.**

All flavors share the same data domain representation, Room database schema, and JSON serialization DTOs. Backup import uses `ignoreUnknownKeys = true`, so flavor-specific preference fields can be safely ignored by a flavor that does not use them.

> [!WARNING]
> **Storage Access Permissions (SAF/Document Trees) Do Not Transfer:**
> Android manages Storage Access Framework (SAF) folder permissions granted through `takePersistableUriPermission` at the package-name level.
>
> The GitHub and F-Droid flavors share `dev.bikram.filepipe.gh`, so their SAF grants are tied to the same package ID. The Play Store flavor uses `dev.bikram.filepipe`, so SAF grants from GitHub or F-Droid are not available to the Play Store build, and Play Store grants are not available to GitHub or F-Droid.
>
> **How to resolve:** After restoring a backup on a flavor with a different package ID, re-pick affected folders with the system folder picker so Android grants access to the active package.
