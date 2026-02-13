# Room Add-Show Migration Notes

## Scope
This migration slice targets the write path for adding a new show and its episodes.

Changed flow:
- `AddShowTask` still performs the lightweight duplicate pre-check before full network fetch.
- Final write now goes through `ShowLibraryWriter`.
- `ShowLibraryWriter` uses Room transactional insert as the primary write path.

Relevant files:
- `app/src/main/java/com/redcoracle/episodes/services/AddShowTask.kt`
- `app/src/main/java/com/redcoracle/episodes/db/room/ShowLibraryWriter.kt`
- `app/src/main/java/com/redcoracle/episodes/db/room/AddShowRoomDao.kt`

## Transaction Behavior
Room path is wrapped in `withTransaction`:
- Insert show row.
- Insert all episode rows.
- Commit all-or-nothing on success.

Fallback path preserves previous non-transactional resolver behavior.

## Cleanup Plan
When full DB migration is ready:
1. Keep schema declarations aligned for future upgrades.
2. Validate app upgrade from existing user DBs.
3. Remove `@SkipQueryVerification` where possible and use full entity-backed DAO inserts.
