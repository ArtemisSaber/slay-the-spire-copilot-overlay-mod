# AGENTS.md

## Overview
Java mod for Slay the Spire using BaseMod. Reads `output/overlay.json` from the companion copilot tool and renders it as an in-game overlay. Falls back to `output/advice.txt` for backward compatibility. See `mod-overlay-plan.md` for full design doc.

## Project structure
```
├── pom.xml
├── src/main/java/copilot/overlay/
│   ├── CopilotOverlayMod.java     // @SpireInitializer entry point
│   ├── AdviceReader.java          // polls output/overlay.json every 500ms, JSON + text fallback
│   ├── AdviceOverlay.java         // BaseMod RenderSubscriber UI rendering, fade transitions
│   └── OverlayConfig.java         // position, hotkey, visibility, maxDataAgeMs
├── src/main/resources/
│   └── ModTheSpire.json
├── workspace/                     // Steam Workshop publishing workspace (gitignored)
│   ├── config.json                // Workshop title, description, tags, changeNote
│   ├── content/CopilotOverlay.jar // built artifact to upload
│   └── image.jpg                  // Workshop preview image
└── sendToDevs/                    // Uploader logs (gitignored)
```

## Build
Dependencies live in `lib/` (gitignored). Compile and package with Maven:

```bash
mvn package
cp target/CopilotOverlay.jar /home/howard/.local/share/Steam/steamapps/common/SlayTheSpire/mods/
```

On first clone, copy jars from STS install/workshop into `lib/`:
```bash
STS=/home/howard/.local/share/Steam/steamapps/common/SlayTheSpire
WS=/home/howard/.local/share/Steam/steamapps/workshop/content/646570
cp "$STS/desktop-1.0.jar" "$WS/1605060445/ModTheSpire.jar" "$WS/1605833019/BaseMod.jar" "$WS/1609158507/StSLib.jar" lib/
```

## Steam Workshop upload

Workshop item ID: `3748304500`. The upload tool is `mod-uploader.jar` shipped with Slay the Spire.

### Prerequisites
- Steam must be running and logged in
- `workspace/config.json` must have correct metadata
- `workspace/content/CopilotOverlay.jar` must be the latest build

### Upload steps
```bash
# 1. Build
mvn clean package

# 2. Copy JAR to workspace
cp target/CopilotOverlay.jar workspace/content/CopilotOverlay.jar

# 3. Update change note in workspace/config.json (edit the "changeNote" field)

# 4. Upload (Steam must be running)
java -jar "$STS/mod-uploader.jar" upload -w workspace
```

### workspace/config.json reference
```jsonc
{
  "steamPublishedID": "3748304500",
  "title": "Slay the spire copilot overlay",
  "description": "...",          // Full description shown on Workshop page
  "visibility": "public",        // "public" | "private" | "friends"
  "changeNote": "...",           // Brief note about what changed in this update
  "tags": ["Utility"]
}
```

## Key design decisions
- Implements `RenderSubscriber` (not raw `ISubscriber`) — BaseMod checks `instanceof` for subscriber interfaces
- Polls overlay file every ~500ms via daemon `ScheduledExecutorService`
- Reads `output/overlay.json` first; falls back to `output/advice.txt` if JSON absent
- File path: env var `COPILOT_ADVICE_PATH`, or resolved relative to cwd
- Top-right overlay with semi-transparent dark background rendered via ShapeRenderer + SpriteBatch
- Calls `sb.end()` / `sb.begin()` around ShapeRenderer draws (batch must be flushed first)
- JSON parsing via `com.badlogic.gdx.utils.JsonReader` (already on LibGDX classpath)
- Status-aware rendering: `loading` → animated text, `ok` → advice fields, `error` → red text
- Visibility controlled by copilot's `overlay_visibility` field: 400ms fade-in, 2s fade-out, 200ms content dip
- Staleness detection: data with `timestamp_ms` > `maxDataAgeMs` (60s) is hidden immediately
- Hotkey toggle: `F2` or `Ctrl+A` (not yet wired — pending keyboard hook)
- `hideDuringCombat` config flag present but not yet implemented
- Supports Chinese/English labels based on game language setting
