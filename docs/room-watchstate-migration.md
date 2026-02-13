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

## Schema Alignment Status
Legacy installs may have `episodes` declared with:
- `name VARCHAR(200)`
- `first_aired DATE`
- `watched BOOLEAN`

Room expects declarations compatible with:
- `name TEXT`
- `first_aired INTEGER`
- `watched INTEGER`

`AppDatabase` now runs a one-time legacy normalization before opening Room.
This aligns `episodes` table declarations so Room validation succeeds.

## Current Behavior Guarantees
- Main screen, season screen, next-episode screen watched toggles should not crash on legacy schema.
- Existing UI observers should continue to refresh because provider URI notifications are kept.
- Season "mark watched" semantics remain unchanged:
  - `watched=true` marks only aired episodes.
  - `watched=false` clears watched for the whole season.

## Cleanup Plan (Return to Normal Room)
When full DB migration is ready, continue cleanup in this order:

1. Keep schema declarations aligned via explicit migrations for future versions.

2. Verify Room opens successfully on upgrade paths from existing app versions.
   - Test old real-world DBs, not just clean installs.

3. Decide whether provider notifications are still needed.
   - If read paths still rely on `ContentObserver`, keep notifications.
   - If all reads are Room/Flow, retire provider-dependent notifications.

4. Remove transitional notes/TODO markers in code and this document section.

## Regression Checklist
- Toggle single episode watched from:
  - show detail episode list
  - episode detail screen
  - main screen quick actions
- Mark season watched and not watched.
- Mark show watched and not watched.
- Verify no crash and UI reflects changes immediately.
