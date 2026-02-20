# Server API Reference

## HTTP Endpoints (port 5566)

- `GET /health` - Health check
- `POST /transcribe` - Receive audio, transcribe, launch Claude
- `GET/POST /api/config` - Settings (model, language, response_mode)
- `GET /api/chat` - Chat history, state, current prompt
- `GET /api/history` - Request history
- `GET /api/response/<id>` - Poll for Claude response
- `POST /api/response/<id>/ack` - Acknowledge response
- `GET /api/audio/<id>` - TTS audio file
- `POST /api/message` - Text message from phone app
- `POST /api/claude/restart` - Restart Claude process
- `POST /api/prompt/respond` - Respond to Claude prompt
- `POST /api/permission/request` - Hook submits permission
- `GET /api/permission/status/<id>` - Hook polls for decision
- `POST /api/permission/respond` - App approves/denies
- `POST /api/image` - Upload image for display in clients (base64 JSON body)
- `GET /api/image/<id>` - Serve uploaded image binary

## WebSocket Messages (port 5567)

- `state` - Claude status (idle, listening, thinking, speaking)
- `chat` - New message
- `history` - Chat history on connect
- `prompt` - Permission prompt update
- `permission` - Permission request
- `permission_resolved` - Permission decision
- `usage` - Context/cost stats
- `text_chunk` - Streaming text chunk from Claude
- `tool` - Tool use notification
- `clients` - Connected clients list
- `image` - New image available (id, url, caption, mime, timestamp). The `mime` field contains the MIME type (e.g. `image/png`, `text/html`); clients should render `text/html` in an iframe/WebView instead of an `<img>` tag.

## Permission System

**Sensitive tools (require approval):** Bash, Write, Edit, NotebookEdit

**Auto-approved:** Read, Glob, Grep, safe Bash commands (ls, cat, grep, etc.)

## Response Modes

Set via dashboard: `disabled` (default), `text`, `audio`

## Known Issues

1. 5-second cooldown between Claude launches (duplicate guard)
2. No HTTPS - use `http://`
3. TTS truncated to 1500 chars
