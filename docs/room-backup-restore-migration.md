# Room Backup/Restore Migration Notes

## Scope
Backup and restore still use file-based DB copy, but are now coordinated with Room lifecycle.

Updated files:
- `app/src/main/java/com/redcoracle/episodes/db/room/AppDatabase.kt`
- `app/src/main/java/com/redcoracle/episodes/services/BackupTask.kt`
- `app/src/main/java/com/redcoracle/episodes/services/RestoreTask.kt`

## What Changed
1. Backup now calls `AppDatabase.checkpoint(context)` before copying `episodes.db`.
2. Restore now calls `AppDatabase.closeInstance()` before replacing the DB file.
3. Restore also calls `AppDatabase.closeInstance()` again in `finally` to clear stale handles,
   then reloads provider DB state with `ShowsProvider.reloadDatabase(context)`.

## Why This Matters
- Room may keep an open SQLite connection while backup/restore runs.
- Without coordination, backup can miss WAL state, and restore can race with active handles.
- Checkpoint + close reduces corruption/race risk during raw file copy.

## Current Design
- Still legacy file-copy backup format (`.db`) for compatibility.
- No schema export/import conversion yet.
- Provider notifications and reload behavior are preserved.

## Future Cleanup / Next Step
When full Room migration is complete:
1. Consider replacing raw file copy with explicit SQLite backup API flow.
2. Move provider reload and Room lifecycle handling behind one backup/restore abstraction.
3. Add restore validation and user-visible error reporting for invalid backup files.
