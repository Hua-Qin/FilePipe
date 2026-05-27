# FilePipe Flavor Differences

FilePipe is built using two product flavors under the `distribution` dimension: **`github`** and **`playstore`**. 

| Feature / Characteristic | GitHub Flavor (`github`) | Play Store Flavor (`playstore`) |
| :--- | :--- | :--- |
| **Application ID** | `dev.bikram.filepipe.gh` | `dev.bikram.filepipe` |
| **Side-by-Side Installation** | Yes (due to different package ID suffixes) | Yes (due to different package ID suffixes) |
| **Update Mechanism** | Checks GitHub Release API (`/releases/latest`), downloads APK to cache, and launches system installer intent. | Leverages Google Play Core In-App Updates (flexible or immediate options). |
| **Manifest Permissions** | Requests `android.permission.REQUEST_INSTALL_PACKAGES` to allow APK installation. | No installation permissions requested. |
| **Save Update to Downloads** | Yes, settings toggle (`saveUpdateApkToDownloads`) to save a copy of the downloaded APK to the public Downloads folder. | No. |
| **Startup Cache Cleanup** | Yes, running `UpdateApkCacheMaintenance` deletes the cached APK once the installed package matches the cached version. | N/A. |
| **In-App Rating / Review** | Stubbed out (`GithubPlayUpdateNoOp`); does not prompt for ratings. | Integrates Google Play In-App Review API with automated prompt scheduling (`InAppRatingAutoPromptHost`). |
| **"Star on GitHub" Button** | Primary visual treatment (solid color background). | Secondary visual treatment (outlined border). |
| **"Rate on Play Store" Button** | Secondary visual treatment (outlined border, launches store listing URL in browser). | Primary visual treatment (solid color background, triggers native In-App Review dialog). |
| **Play Service Dependencies** | None. | Imports `com.google.android.play:app-update` and `com.google.android.play:review` libraries. |

---

## Backup Portability FAQ

### Are backups portable between the GitHub and Play Store flavors?
**Yes, the backup files (`filepipe_backup_*.json`) are fully portable between both flavors.**

Both flavors share the same data domain representation, Room database schema, and JSON serialization DTOs (using `ignoreUnknownKeys = true` to safely handle fields such as `saveUpdateApkToDownloads` if imported into the Play Store version).

> [!WARNING]
> **Storage Access Permissions (SAF/Document Trees) Do Not Transfer:**
> Android manages Storage Access Framework (SAF) folder permissions (granted via `takePersistableUriPermission`) at the package name level. Because the GitHub flavor (`dev.bikram.filepipe.gh`) and Play Store flavor (`dev.bikram.filepipe`) have different Application IDs, any folder permissions stored in settings (e.g., local backup destination or customized rule source/destination directories) will **not** be accessible by the other flavor upon restore.
> 
> **How to resolve:** After restoring a backup on a different flavor, you will need to re-pick those folders using the system folder picker to grant permission to the new application package.
