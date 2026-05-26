# rustyTaskClock Requirements

> Stand-alone, cross-platform Rust successor to TaskClock.

## 1. Purpose

`rustyTaskClock` is a new implementation of TaskClock in Rust.
It must preserve the proven TaskClock workflow while removing the need for a preinstalled desktop runtime such as Qt.
The result must be distributable as a standalone application for Windows, Linux, and macOS.

## 2. Product Goals

- Provide a small, fast desktop app for task-based time stamping.
- Keep the interaction model simple and direct.
- Support a single active task at any time.
- Allow users to create, start, stop, switch, and export task time data.
- Ship as a standalone release artifact per platform.

## 3. Non-Negotiable Constraints

- **Standalone distribution:** end users must not need to install Rust, Qt, .NET, Python, or any other app runtime.
- **Cross-platform:** first-class support for Windows, Linux, and macOS.
- **One active task only:** the app must enforce exactly one running task at a time.
- **Persistent local data:** task data must survive restarts.
- **Local storage only:** the app stores data on the local machine; no cloud dependency.
- **Versioned releases:** app title, binary name, tags, and release assets must all carry the version.
- **User-data directory:** runtime data must live in the platform-specific app-data directory, not next to the binary.

## 4. Functional Requirements

### 4.1 Task Management

- Users can create tasks/todos.
- Users can rename tasks inline from the task table by clicking the task name.
- Users can delete tasks.
- Users can view all tasks in a single list.
- Each task has a stable identifier.

### 4.2 Stamping Workflow

- A task can be started/stamped.
- If another task is started, the currently active task must be stopped first.
- Starting an already active task must stop it.
- Only one task may be active at any moment.
- Each start/stop cycle must be recorded as time spent on that task.

### 4.3 Timer Behavior

- The UI must show a live timer for the active task.
- The displayed time must refresh automatically while the app is open.
- The total workday time must also refresh automatically.
- Timer state must survive app restarts through persisted timestamps.

### 4.4 Workday Handling

- The app must support a new workday/reset action.
- Reset must not silently destroy tracked history.
- The UX must make it clear when a new workday begins.
- The workday timer logic must be testable without the UI.

### 4.5 Export

- Export task data as CSV.
- Export filenames must be deterministic and version-safe.
- Export must work without external spreadsheet software.
- The export flow must be simple enough for daily use.

### 4.6 Pinning / Always-on-top

- The window must support an always-on-top toggle.
- Toggling pinning must not visibly flash or recreate the window if avoidable.
- The pin state should be obvious in the UI.

## 5. UX Requirements

- The UI must be compact and readable.
- Large timer values must use the normal UI font, not a decorative or code-style font.
- Typography must be consistent across platforms.
- If a bundled font is needed to avoid ugly fallback rendering, bundle it explicitly.
- The app title must include the version.
- UI labels and asset names should stay human-readable and not feel technical.
- The app should feel like a small tool, not an enterprise dashboard.
- The App should be scalable, not to big in it's appearance
- should be possible to minimize the window-size and still should display valuable content, like displaying the main timer

## 6. Data and Persistence Requirements

- Persist all user data locally.
- Store data in the OS-appropriate application data directory.
- Do not write user data next to the executable.
- Use a single well-defined storage format.
- The storage format must be stable enough for future migration.
- Loading must tolerate missing or partially broken data gracefully.
- Corrupt data must produce a useful error message, not a silent crash.

## 7. Reliability Requirements

- The app must start cleanly on supported platforms.
- Packaging failures must be visible during CI, not only on user machines.
- The release artifact should be self-contained enough to run on a clean target machine.
- Crash handling should write a useful error log where applicable.
- Common runtime failures must be diagnosable from a log or error file.

## 8. Build and Packaging Requirements

- Provide platform-specific release artifacts.
- Prefer one artifact per platform that is easy to download and run.
- The release process must not depend on the end user having any development tools.
- Build output names must include the version and target platform.
- Release notes must mention that the build is standalone.
- CI should produce checksummed release artifacts.
- Packaging should avoid unnecessary extra dependencies.

## 9. Architecture Requirements

- Separate core logic from UI code.
- Keep timing, persistence, and export logic testable without a window.
- The UI layer should call into a small, well-defined core API.
- Platform-specific packaging concerns must stay isolated from core logic.
- Prefer a structure that makes later GUI replacement possible without rewriting the domain logic.

## 10. Testing Requirements

- Core logic must have automated tests.
- Timer math must be tested.
- Single-active-task rules must be tested.
- CSV export format must be tested.
- Persistence load/save round-trips must be tested.
- Workday reset behavior must be tested.
- UI behavior should have focused tests where practical, but core logic is the priority.
- Packaging/build behavior should have at least smoke-level verification in CI.

## 11. Release Requirements

- Release tag format must remain versioned and app-specific.
- App title, executable name, and release asset names must all match the same version.
- Existing public releases for the same version must not be republished.
- The private repo remains the source of truth.
- The public mirror/release path must stay one-way.

## 12. Lessons Learned from the Current TaskClock Iterations

These requirements are based on the current TaskClock work and user feedback:

- A source checkout is not enough for users; the shipped build must be truly standalone.
- A bundled runtime is necessary if users should not install Qt or similar desktop dependencies.
- Qt can be packaged standalone, but the user-facing build must be verified as such.
- Font rendering can look wrong if the build relies on system fallback fonts.
- Release artifacts should make the version obvious.
- User data should live in app-data, not beside the binary.
- Cross-platform packaging must be planned from the beginning, not bolted on later.
- Core time-stamping rules are more important than UI polish.
- The app must always enforce exactly one active task.
- The UI should remain small and practical, not feature-bloated.

## 13. Open Preparation Decisions

Before implementation, choose and document:

- UI stack: egui, iced, native toolkit, or other Rust GUI approach.
- Packaging strategy per platform.
- Font strategy for consistent large timer rendering.
- Data format and migration strategy.
- Release artifact naming convention.

## 14. Acceptance Criteria

`rustyTaskClock` is ready for implementation when:

- the requirements above are approved,
- the architecture decision is recorded,
- the first Rust project skeleton exists,
- and the planned feature set can be implemented without changing the product direction.
