# Claude Watch

Voice-to-Claude pipeline: Galaxy Watch/Phone -> Server -> Deepgram -> Claude Code -> Response back.

## Key Files

- `server.py` - HTTP server (port 5566) + WebSocket (port 5567)
- `claude_wrapper.py` - Persistent Claude process with JSON streaming
- `permission_hook.py` - Tool approval hook for sensitive operations
- `logger.py` - Logging configuration
- `dashboard.html` - Vue.js 3 web dashboard
- `watch-app/` - Kotlin Wear OS app
- `phone-app/` - Kotlin Android companion app

## Commands

**Before committing, always run `make check` to lint and test everything (mirrors CI).**

```bash
make check          # Run all lints + tests (same as CI)
make lint           # Just lints
make test           # Just tests
make coverage       # Generate coverage reports (Python + watch + phone)
./server.py /path   # Start server (folder arg required)
```

Build & install apps: `cd watch-app && ./gradlew assembleDebug` (same for phone-app), then `adb install -r <apk>`.

After `adb install`, always force-stop — the old process keeps running with stale singletons:
```bash
adb -s <device> shell am force-stop com.claudewatch.app
```

## Git Workflow

- **Never push directly to master.** Always create a feature branch and open a PR.
- **Always use git worktrees** for feature branches:
  ```bash
  git worktree add ./worktrees/<branch-name> -b <branch-name>
  ```
  Leave worktrees alive after pushing. Only remove if the user explicitly asks.
- Branch naming: `feature/<short-description>` or `fix/<short-description>`

## Subagents

When spawning subagents (via Task tool) that write code:
- **Each subagent gets its own worktree** — create a worktree + feature branch before doing any work.
- **Never push to master** — subagents push only to their feature branch.
- **All changes go through a PR** — subagent must create a PR for review before anything is merged.
- **PR description must include test summary** — report: number of tests added, test types (Robolectric, JUnit, pytest), coverage percentage on changed files, and whether the 75% minimum is met.

## Testing

Always add unit tests for new features and bug fixes. Write tests that verify **real behavior**, not just data class field access.

- **Phone app**: Use Robolectric (`@RunWith(RobolectricTestRunner::class)`) for tests that touch Android APIs (clipboard, views, handlers). Use `ShadowLooper` for timer/delay assertions.
- **Watch/phone pure logic**: Extract algorithms into testable helper classes (see `RecordingTimerLogic` pattern) and test with plain JUnit.
- **Python server**: Use pytest with mocks for `claude_wrapper.py` and `server.py`.
- **Don't write trivial tests** like checking data class field access — test actual behavior: state changes, visibility toggling, clipboard writes, timer callbacks, flow emissions.
- **Always check test coverage** after adding tests — run `make coverage` and review the reports. Python: `htmlcov/index.html`, Watch: `watch-app/app/build/reports/jacoco/index.html`, Phone: `phone-app/app/build/reports/jacoco/index.html`.
- **Minimum 75% coverage on JaCoCo-measurable code** — pure logic, data classes, network clients, services. Run `make coverage` before pushing. If below 75%, extract logic into testable helpers (see `PermissionPromptHelper` pattern).
- **Robolectric-tested code is exempt from the 75% metric** — JaCoCo can't measure Robolectric coverage due to classloader conflicts. UI code (Activities, Views, Adapters) must still have Robolectric tests but won't show in JaCoCo reports. Report Robolectric test count separately in PR descriptions.

## Don't

- Don't push directly to master — always use a feature branch + PR
- Don't add HTTPS/TLS support — use reverse proxy if needed
- Don't modify tmux session name (`claude-watch`) — apps depend on it
- Don't change WebSocket port (5567) without updating phone app

## Architecture

- Claude runs as persistent process via `claude_wrapper.py` with `--output-format stream-json`
- Output displayed in tmux session `claude-watch` via tail
- Watch connects through phone relay (2-layer): Watch → Phone (Wearable DataLayer) → Server (WebSocket)
- For endpoints, WebSocket messages, and permissions, see [docs/server-api.md](docs/server-api.md)

## Logs

- `/tmp/claude-watch.log` - Main server log (DEBUG level)
- `/tmp/claude-watch-tts.log` - TTS debug log
- `/tmp/claude-watch-output.log` - Claude output stream

## Dependencies

Server: `pip install deepgram-sdk aiohttp` + `alacritty`, `tmux`
Apps: Android SDK, Kotlin, Gradle

## Mockups

Create SVG files in `docs/watch-mockups/` (300x300 round) or `docs/phone-mockups/` (standard mobile).
Use app colors: blue #0099FF, orange #F59E0B.

## Feature Docs

- [docs/features/hardware-button-record.md](docs/features/hardware-button-record.md) - Watch hardware button double-press to start recording
- [docs/features/public-viewer-tunnel.md](docs/features/public-viewer-tunnel.md) - Cloudflare Tunnel setup for read-only public viewer access
- [docs/features/security.md](docs/features/security.md) - Defense-in-depth security layers overview
- [docs/features/tailscale-auth.md](docs/features/tailscale-auth.md) - Tailscale peer verification for server access control
- [docs/screenshots.md](docs/screenshots.md) - ADB instructions for capturing phone/watch screenshots

## Diagrams

Clean style: white background (#ffffff), 1px black stroke rectangles, yellow (#fffde7) containers, mint green (#a5d6a7) highlights. Save to `docs/`.
