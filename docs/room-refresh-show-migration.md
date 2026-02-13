# Room Refresh-Show Migration Notes

## Scope
This migration slice moves refresh-show writes to a Room transaction while preserving a legacy fallback.

Updated path:
- `RefreshShowUtil.refreshShow(...)` now uses `RefreshShowWriter`.
- Writer attempts Room transactional sync first:
  - update show metadata
  - reconcile existing episodes (update/delete/insert)
- On Room open/validation failure, it falls back to existing provider-based write logic.

Relevant files:
- `app/src/main/java/com/redcoracle/episodes/RefreshShowUtil.kt`
- `app/src/main/java/com/redcoracle/episodes/db/room/RefreshShowWriter.kt`
- `app/src/main/java/com/redcoracle/episodes/db/room/RefreshShowRoomDao.kt`

## Compatibility
- Room path manually calls `notifyChange(...)` for `shows` and `episodes` URIs.
- Fallback path keeps existing resolver operations and notifications unchanged.

## Cleanup Plan
After full schema alignment with Room:
1. Remove fallback lambda path in `RefreshShowWriter`.
2. Keep only Room transactional refresh.
3. Revisit `@SkipQueryVerification` and replace with entity-backed verified queries where feasible.
