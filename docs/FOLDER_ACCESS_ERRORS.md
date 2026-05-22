# Folder Access Warnings and Errors

FilePipe shows folder warnings and errors when a rule points to a folder that is missing, blocked, or no longer accessible.

## Folder Access Modes

FilePipe can access folders in two ways.

| Mode | How it works | Best for |
|---|---|---|
| Selective access | You pick folders one by one with Android's folder picker. FilePipe only gets access to the folders you choose. | Most rules, especially when you only need a few specific folders. |
| All files access | You grant FilePipe broad storage access in Android settings. FilePipe can use normal storage paths such as Pictures, Music, or custom folders. | Rules that need broad access, or folders Android will not allow through the folder picker. |

Most rules use one mode consistently, but existing rules can sometimes contain a mix. For example, a rule may have been created in All files access mode and later opened after switching to Selective access. When that happens, FilePipe highlights any folder that no longer works in the current mode.

## What the Severity Means

| Severity | What it means | Can you save other rule changes? |
|---|---|---|
| Amber warning | A source folder is missing or unavailable, but the rule may still work with other source folders. | Yes |
| Red error | The rule cannot run correctly until this is fixed. | Fix the highlighted issue first |

## Folder Access Matrix

| App mode | Folder state | What FilePipe shows | How to resolve |
|---|---|---|---|
| Any mode | ✅ Source and destination are available | No warning or error | Nothing to fix. |
| Any mode | ⚠️ Source folder is missing or deleted | Amber warning on the source folder | Recreate the source folder in the same place, pick the source folder again, or remove it from the rule. |
| Any mode | ❌ Destination folder is missing or deleted | Red error on the destination folder | Recreate the destination folder in the same place, or pick a new destination folder. |
| Any mode | ⚠️ Source is missing and ❌ destination is also missing | Red error overall. Source may be amber, destination is red | Fix the destination first, then recreate, re-pick, or remove missing source folders. |
| Selective access | ❌ Source or destination folder-picker permission was lost | Red error on the affected folder | Tap the highlighted folder path and grant access again. |
| Selective access | 🚫 Source or destination is internal storage root or the top-level Download folder | Red error on the affected folder | Pick a subfolder instead, such as `Download/Receipts`, or switch to All files access. |
| Selective access | ❌ Source or destination is an All files path from an older/different mode | Red error on the affected folder | Re-pick the folder through Android's folder picker, or switch back to All files access. |
| All files access | ❌ All files access permission was turned off | Red error on filesystem folders | Turn All files access back on in Android settings, or re-pick folders using Selective access. |
| All files access | ❌ Rule still uses a folder-picker folder from an older/different mode, but that folder-picker permission was lost | Red error on the affected folder | Tap the highlighted folder path and grant access again, or pick a filesystem folder. |
| Any mode | ❌ Rule has no source folder selected | Red error: source folder is required | Add at least one source folder. |
| Any mode | ❌ Rule has no destination folder selected | Red error: destination folder is required | Pick a destination folder. |
| Any mode | ❌ Source and destination are the same folder | Red error | Choose a different destination or source folder. |

## Hide Missing Source Folder Warnings

`Hide missing source folder warnings` only hides amber warnings for missing source folders on the rule card.

It does not hide red errors, including:

- Missing destination folder
- Lost folder permission
- Internal storage root or top-level Download folder blocked in Selective access
- Required rule fields left empty
- Source and destination being the same folder

## After Backup Restore or New Phone Setup

Rules can be restored from backup, but Android folder permissions are not restored with them.

After restoring:

- Tap highlighted folders and grant access again.
- If you use All files access, turn it back on in Android settings.
- Check rules with red errors before relying on scheduled runs.

## If a Recreated Folder Still Shows a Warning

If you delete a selected source folder and recreate it in the same place, the amber warning should normally clear.
