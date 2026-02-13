# Room Add-Show Migration Notes

## Scope
This migration slice targets the write path for adding a new show and its episodes.

Changed flow:
- `AddShowTask` still performs the lightweight duplicate pre-check before full network fetch.
- Final write now goes through `ShowLibraryWriter`.
- `ShowLibraryWriter` attempts Room transactional insert first, then falls back to legacy
  `ContentResolver` inserts when Room cannot open due schema mismatch.

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

## Why Fallback Exists
Same reason as watch-state migration: Room schema validation can fail on legacy installs
where declared column types differ from Room expectations.

If Room throws while opening/validating:
- The writer falls back to legacy resolver inserts.
- Existing app behavior is preserved instead of crashing.

## Cleanup Plan
When full DB migration is ready:
1. Normalize schema declarations to match Room.
2. Validate app upgrade from existing user DBs.
3. Remove fallback resolver insert path in `ShowLibraryWriter`.
4. Remove `@SkipQueryVerification` if possible and use full entity-backed DAO inserts.
