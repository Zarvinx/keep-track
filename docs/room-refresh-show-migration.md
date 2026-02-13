# Room Refresh-Show Migration Notes

## Scope
This migration slice moves refresh-show writes to a Room transaction.

Updated path:
- `RefreshShowUtil.refreshShow(...)` now uses `RefreshShowWriter`.
- Writer performs Room transactional sync:
  - update show metadata
  - reconcile existing episodes (update/delete/insert)

Relevant files:
- `app/src/main/java/com/redcoracle/episodes/RefreshShowUtil.kt`
- `app/src/main/java/com/redcoracle/episodes/db/room/RefreshShowWriter.kt`
- `app/src/main/java/com/redcoracle/episodes/db/room/RefreshShowRoomDao.kt`

## Compatibility
- Writer manually calls `notifyChange(...)` for `shows` and `episodes` URIs.

## Cleanup Plan
After broader Room migration:
1. Revisit `@SkipQueryVerification` and replace with entity-backed verified queries where feasible.
2. Move refresh orchestration into a consolidated database gateway with other write flows.
