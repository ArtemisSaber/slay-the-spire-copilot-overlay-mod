# In-Game Advice Display Mod — Plan

## Overview

A companion Java mod (BaseMod-compatible) that reads `output/advice.txt` and renders it as an overlay in-game. No protocol changes needed — just reads the file the copilot already writes.

## Architecture

### 1. File Watcher/Reader
- Poll `output/advice.txt` every ~500ms using Java timer
- Read on game start and when file changes
- Handle file-not-found gracefully (show "等待建议..." placeholder)
- Determine path from env var `COPILOT_ADVICE_PATH` or fall back to CommunicationModCJK config directory

### 2. UI Rendering
- Register as a BaseMod subscriber (`ISubscriber`)
- Override `receiveRender` to draw text overlay
- Position: top-right corner (configurable)
- Semi-transparent dark background box, scrollable text
- Support Chinese text via game's existing font (loaded by TogetherInSpireChinesePatch)
- Parse structured fields (推荐/理由/风险/吐槽) for styled display

### 3. Hotkey Toggle
- Register keyboard shortcut (`F2` or `Ctrl+A`)
- Default: visible during decision screens, hidden during combat
- Configurable in mod settings

### 4. Mod Metadata
- `@SpireInitializer` entry point
- Dependencies: `basemod`
- Name: "Copilot Overlay"

## File Structure

```
mod-copilot-overlay/
├── pom.xml
├── src/main/java/copilot/overlay/
│   ├── CopilotOverlayMod.java     // entry point
│   ├── AdviceReader.java          // reads output/advice.txt
│   ├── AdviceOverlay.java         // UI rendering
│   └── OverlayConfig.java         // position, hotkey, visibility
└── src/main/resources/
    └── ModTheSpire.json
```

## Open Questions
1. Should the mod be standalone or integrated into CommunicationMod?
2. Should it also display postmortem after run ends?
3. UI layout: compact single box or expanded cards view?
