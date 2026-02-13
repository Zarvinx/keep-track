# Room Watch-State Migration Notes

## Scope
This document covers the incremental migration for episode watch-state writes only.

Current branch intent:
- Keep existing read paths on `ContentResolver`/`ContentProvider`.
- Move watched-write operations to Room where possible.
- Preserve legacy observer updates via `notifyChange(...)`.

Relevant files:
- `app/src/main/java/com/redcoracle/episodes/db/room/EpisodeWatchStateWriter.kt`
- `app/src/main/java/com/redcoracle/episodes/db/room/EpisodesRoomDao.kt`
- `app/src/main/java/com/redcoracle/episodes/db/room/AppDatabase.kt`
- `app/src/main/java/com/redcoracle/episodes/db/room/EpisodeEntity.kt`

## Why There Is a Fallback Path
Existing devices may already have a legacy SQLite schema created by `SQLiteOpenHelper`.
Room validates schema strictly and can throw at runtime if declared types differ.

Observed mismatch in crash logs for `episodes`:
- Existing table declarations include:
  - `name VARCHAR(200)`
  - `first_aired DATE`
  - `watched BOOLEAN`
- Room entity inference expects:
  - `name TEXT`
  - `first_aired INTEGER` (from `Long?`)
  - `watched INTEGER` (from `Int?`)

Even though SQLite is loosely typed, Room schema validation checks declared types and can fail open.

To avoid user-facing crashes during incremental migration, `EpisodeWatchStateWriter`:
- Tries DAO-based Room updates first.
- Falls back to legacy `ContentResolver.update(...)` on failure.
- Always triggers `notifyChange(...)` for provider observers.

## Current Behavior Guarantees
- Main screen, season screen, next-episode screen watched toggles should not crash on legacy schema.
- Existing UI observers should continue to refresh because provider URI notifications are kept.
- Season "mark watched" semantics remain unchanged:
  - `watched=true` marks only aired episodes.
  - `watched=false` clears watched for the whole season.

## Cleanup Plan (Return to Normal Room)
When full DB migration is ready, remove fallback in this order:

1. Normalize schema so Room and on-device DB match exactly.
   - Use Room migrations or a controlled table rebuild.
   - Align `episodes` column declarations with Room expectations.

2. Verify Room opens successfully on upgrade paths from existing app versions.
   - Test old real-world DBs, not just clean installs.

3. Remove legacy write fallback in `EpisodeWatchStateWriter`.
   - Keep DAO-only writes.

4. Decide whether provider notifications are still needed.
   - If read paths still rely on `ContentObserver`, keep notifications.
   - If all reads are Room/Flow, retire provider-dependent notifications.

5. Remove transitional notes/TODO markers in code and this document section.

## Regression Checklist
- Toggle single episode watched from:
  - show detail episode list
  - episode detail screen
  - main screen quick actions
- Mark season watched and not watched.
- Mark show watched and not watched.
- Verify no crash and UI reflects changes immediately.
