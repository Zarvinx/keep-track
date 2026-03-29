# keep-track

## Version Bumping

When making changes that warrant a version bump, update `app/build.gradle`:
- `versionCode` always increments by 1
- `versionName` follows semver based on significance:
  - **Patch** `0.x.Y` — bug fixes, minor UI tweaks, small isolated changes
  - **Minor** `0.X.0` — new features, meaningful UX improvements, multiple new screens or data
  - **Major** `X.0.0` — large architectural changes, breaking DB changes without migration, full rewrites

Always bump both together. Do not bump the version unless explicitly asked or when preparing a release commit.
