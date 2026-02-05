# Toadie — Android Companion App

Android phone app with an animated creature (Toadie), real-time chat, wake word detection, and permission prompt handling. Connects to the server via WebSocket for live updates, and relays traffic to the watch via Wearable DataLayer.

<p align="center">
  <img src="../docs/phone-mockups/screenshots/creature-idle.png" width="250" alt="Idle">
  &nbsp;
  <img src="../docs/phone-mockups/screenshots/creature-thinking.png" width="250" alt="Thinking">
  &nbsp;
  <img src="../docs/phone-mockups/screenshots/wake-word-listening.png" width="250" alt="Wake Word">
</p>

## Features

- **Animated Creature** — Green troll (Toadie) with state-driven animations: breathing, blinking, thought bubbles, bouncing, particles, and glow effects
- **Real-time Chat** — Messages via WebSocket, blue user bubbles, orange Claude bubbles, auto-scroll
- **Voice & Text Input** — Tap mic to record, tap again to send; or type messages
- **Wake Word** — Say "hey toadie" hands-free (Picovoice Porcupine), works with screen off, fullscreen overlay with audio wave visualization
- **Permission Prompts** — Approve/deny dangerous tool calls (Bash, file writes) directly from the phone
- **Watch Relay** — Forwards all traffic between watch and server via Wearable DataLayer (Bluetooth)
- **Kiosk Mode** — Fullscreen immersive display, triple-tap top-left to exit

## Creature States

<p align="center">
  <img src="../docs/phone-mockups/screenshots/creature-idle.png" width="200" alt="Idle">
  &nbsp;
  <img src="../docs/phone-mockups/screenshots/creature-thinking.png" width="200" alt="Thinking">
  &nbsp;
  <img src="../docs/phone-mockups/screenshots/creature-speaking.png" width="200" alt="Speaking">
</p>

| State | Trigger | Visual |
|-------|---------|--------|
| Idle | Default | Breathing, occasional blinks, green glow |
| Listening | Audio received | Perked horns, dilated pupils, pulse rings |
| Thinking | Processing | Closed eyes, thought bubbles, blue glow |
| Speaking | Response ready | Bouncing, open mouth, gold sparkles |
| Sleeping | 2+ min idle | Droopy horns, Zzz particles, purple glow |
| Offline | Disconnected | Gray body, sad frown |

## Setup

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Configure server address in Settings (gear icon) — enter IP and WebSocket port (e.g. `192.168.1.100:5567`).

For wake word, add your [Picovoice](https://picovoice.ai/) access key to `local.properties`:
```
porcupine.access_key=your-key-here
```

## Testing

```bash
./gradlew test
```

## Requirements

- Android 8.0+ (API 26)
- Network access to server (or Tailscale)
- Picovoice access key (for wake word)
