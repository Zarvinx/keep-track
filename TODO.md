# Episodes Tracker - Migration TODOs

## Architecture Improvements

### State Management
- [ ] **Replace AddShowSearchResults singleton** with proper state management
  - Consider Compose Navigation with parcelable arguments
  - Or use Hilt with shared ViewModel scopes
  - Current issue: Singleton data can be destroyed by Android when process is killed

### Navigation
- [ ] **Migrate to Jetpack Compose Navigation**
  - Currently using Activity-based navigation with Intents
  - Would enable proper argument passing and back stack management
  - Would eliminate need for singletons like AddShowSearchResults

### Dependency Injection
- [ ] **Add Hilt for proper DI**
  - Would simplify ViewModel factories
  - Better testability
  - Proper scoping for shared state

## Remaining Java → Kotlin/Compose Conversions

### Activities
- [x] `AboutActivity.java` - Simple activity
- [x] `EpisodeActivity.java` - Episode details viewer
- [x] `SettingsActivity.java` - Settings screen

### Fragments
- [x] `EpisodeDetailsFragment.java` - Episode details UI
- [x] `SettingsFragment.java` - Settings UI
- [x] `SelectBackupDialog.java` - Backup selection dialog

### Utilities (Lower Priority)
These can stay in Java for now, but should eventually be converted:
- [x] `EpisodesApplication.java` - Application class
- [x] `AutoRefreshHelper.java` - Background refresh logic
- [x] `EpisodesCounter.java` - Episode counting utilities
- [x] `FileUtilities.java` - File operations
- [x] `Preferences.java` - Shared preferences wrapper
- [x] `RefreshShowUtil.java` - Show refresh logic

## Performance Optimizations

### Database
- [ ] **Consider Room instead of raw ContentProvider**
  - Type safety
  - Better coroutines support
  - Compile-time query verification

### Image Loading
- [x] **Audit Coil image loading configuration**
  - Ensure proper caching
  - Consider placeholder/error states
  - Memory optimization for large lists

### Async Operations
- [x] **Replace remaining AsyncTask usage**
  - Converted deprecated android.os.AsyncTask to Kotlin coroutines
  - Converted AsyncTask wrapper and all task classes to Kotlin
  - All background tasks now use CoroutineScope with Dispatchers.IO


## Code Quality

### Component Organization
- [ ] **Refactor show status text into separate component**
  - Extract status message display (ended/caught up) from ShowListItem
  - Create reusable StatusText composable
  - Improves maintainability and testability

### Testing
- [ ] Add unit tests for ViewModels
- [ ] Add UI tests for main flows
- [ ] Test database migrations

### Documentation
- [ ] Add KDoc comments to public APIs
- [ ] Document migration decisions
- [ ] Create architecture documentation

## Feature Improvements

### UI/UX
- [x] **Add status text above show cards**
  - Display next episode air date ("Next episode airs in X days")
  - When next episode is not released, don't allow it to be selected as watched.
  - Show status for ended shows ("This show has ended")
  - Handle missing data ("No information about next episode")
  - Position above the progress bar in show list items
- [ ] Add Material You dynamic colors support
- [ ] Improve error messages and user feedback
- [ ] Add loading skeletons instead of just spinners
- [ ] Add pull-to-refresh in lists

### Functionality
- [ ] Add search within show list
- [ ] Add sorting options (by name, date added, watch progress)
- [ ] Export/Import improvements (JSON format option)

## Completed ✅
- [x] Convert MainActivity to Compose
- [x] Convert ShowActivity to Compose
- [x] Convert SeasonActivity to Compose + Kotlin
- [x] Convert AddShowSearchActivity to Compose + Kotlin
- [x] Convert AddShowPreviewActivity to Compose + Kotlin
- [x] Create ShowsViewModel with optimistic updates
- [x] Create EpisodesViewModel with optimistic updates
- [x] Fix scroll performance in shows list
- [x] Add lifecycle-aware data reloading
- [x] Centralize shadow styles in theme
- [x] Convert AddShowSearchResults to Kotlin object
- [x] Add status text above show cards with air date countdown
- [x] Implement show status tracking from TMDB (Ended/Canceled)
- [x] Implement "Refresh All Shows" feature
- [x] Disable watch buttons for unreleased/TBA episodes
