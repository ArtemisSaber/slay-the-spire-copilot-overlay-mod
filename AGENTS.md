# AGENTS.md

## Overview
Java mod for Slay the Spire using BaseMod. Reads `output/advice.txt` from the companion copilot tool and renders it as an in-game overlay. See `mod-overlay-plan.md` for full design doc.

## Project structure
```
├── pom.xml
├── src/main/java/copilot/overlay/
│   ├── CopilotOverlayMod.java     // @SpireInitializer entry point
│   ├── AdviceReader.java          // polls output/advice.txt every 500ms
│   ├── AdviceOverlay.java         // BaseMod RenderSubscriber UI rendering
│   └── OverlayConfig.java         // position, hotkey, visibility
└── src/main/resources/
    └── ModTheSpire.json
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

## Key design decisions
- Implements `RenderSubscriber` (not raw `ISubscriber`) — BaseMod checks `instanceof` for subscriber interfaces
- Polls advice file every ~500ms via daemon `ScheduledExecutorService`
- Advice file path: env var `COPILOT_ADVICE_PATH`, fallback to `output/advice.txt` relative to cwd
- Top-right overlay with semi-transparent dark background rendered via ShapeRenderer + SpriteBatch
- Calls `sb.end()` / `sb.begin()` around ShapeRenderer draws (batch must be flushed first)
- Hotkey toggle: `F2` or `Ctrl+A` (not yet wired — pending keyboard hook)
- Visible during decision screens, hidden during combat (configurable but not yet auto-detected)
- Parses Chinese field labels (推荐/理由/风险/吐槽) with both `：` and `:` colons
- No protocol changes — this mod only reads the file the copilot writes
