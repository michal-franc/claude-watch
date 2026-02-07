# Security: Defense in Depth

Multiple overlapping layers so no single failure compromises the system.

```
Internet ──X──  (not exposed)
           │
   Tailscale WireGuard       ← Encryption
           │
   Peer verification          ← Network access control
           │
   Server (HTTP + WS)        ← Localhost bypass for internal services
           │
   Permission hook            ← Tool-level approval
           │
   Claude process             ← Working directory isolation
```

## Layer 1: Network Encryption (Tailscale)

All traffic travels through WireGuard tunnels. The server binds `0.0.0.0` but is only reachable via Tailscale IPs (`100.x.x.x`). No TLS needed — Tailscale encrypts before packets hit the wire.

**Stops:** Eavesdropping, packet sniffing, MITM on local network.

## Layer 2: Peer Verification (`tailscale_auth.py`)

Connections restricted to explicitly named Tailscale nodes via `TAILSCALE_ALLOWED_NODES` env var. Queries `tailscaled` Unix socket to resolve peer IP → node name, checks against allowlist. Results cached 5 min.

- Env var not set → disabled (local dev)
- Env var set, Tailscale down → **fail-closed**
- Unknown IP → denied
- Localhost → always allowed (dashboard, permission hook)

Applied to `do_GET()`, `do_POST()`, and `websocket_handler()`.

**Stops:** Unauthorized devices on the tailnet.

## Layer 3: Permission Hook (`permission_hook.py`)

Intercepts Claude tool calls before execution.

**Auto-approved (read-only):** `Read`, `Glob`, `Grep`, safe Bash (`ls`, `cat`, `grep`, `find`, etc.)

**Require phone approval:** `Bash` (destructive), `Write`, `Edit`, `NotebookEdit`

In server-spawned sessions: hook sends request to server → server broadcasts to phone → user taps Allow/Deny → hook polls for decision. 120-second timeout → **denied**. Server unreachable → **denied**.

**Stops:** Claude executing destructive commands without human approval. Even with server API access, tool execution is still gated by the phone.

## Layer 4: Process Isolation (`claude_wrapper.py`)

Claude runs as a persistent subprocess in a specific working directory (`cwd=self.workdir`). Controlled stdin/stdout via JSON streaming. 5-second termination timeout, forced kill if unresponsive, auto-restart on broken pipe.

`CLAUDE_WATCH_SESSION=1` env var marks server-spawned sessions so the permission hook contacts the server instead of prompting locally.

**Stops:** Limits blast radius to the target project directory.

## Failure Modes

| What fails | Result |
|---|---|
| Tailscale down | Peer verification denies all non-localhost |
| Phone disconnected | Permission requests timeout → denied |
| Server crashes | Claude process dies with it |
| Unknown device on tailnet | Denied by peer verification |
| Claude tries destructive command | Blocked until phone approves |
| Someone hits `/api/message` | Can submit text, but tools still gated by permission hook |

## Conscious Tradeoffs

- **No TLS** — Tailscale provides encryption; TLS would duplicate it
- **No per-request auth tokens** — identity verified at network level
- **No certificate pinning** — apps connect via Tailscale IPs, not public DNS
- **CORS is `*`** — server only reachable via Tailscale
- **Request history in memory** — lost on restart, no persistent sensitive data
