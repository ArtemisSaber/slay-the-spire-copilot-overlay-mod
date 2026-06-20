# Copilot Overlay Mod

In-game overlay for Slay the Spire that displays AI copilot advice. Polls `output/advice.txt` and renders it as a semi-transparent overlay in the top-right corner. Companion to [slay-the-spire-copilot](https://github.com/ArtemisSaber/slay-the-spire-copilot).

《杀戮尖塔》游戏内悬浮窗模组。读取 `output/advice.txt` 中的 AI 建议，以半透明悬浮窗形式展示在屏幕右上角。配合 [slay-the-spire-copilot](https://github.com/ArtemisSaber/slay-the-spire-copilot) 使用。

## Requirements

- Slay the Spire
- ModTheSpire 3.30+
- BaseMod

## Install

1. Download `CopilotOverlay.jar` from [Releases](https://github.com/ArtemisSaber/slay-the-spire-copilot-overlay-mod/releases).
2. Place it in Slay the Spire's `mods/` directory.
3. Launch the game with ModTheSpire and enable **Copilot Overlay** (requires BaseMod).

## Usage

The overlay reads `output/advice.txt` every 500ms and updates automatically. The file path resolves as:

- `COPILOT_ADVICE_PATH` environment variable, or
- `output/advice.txt` relative to the game's working directory

When used with the copilot tool via CommunicationMod, the working directory is the Slay the Spire install directory — no extra config needed. The overlay shows Chinese labels when the game language is set to Chinese.

悬浮窗每 500ms 读取一次 `output/advice.txt` 并自动更新。文件路径查找顺序：

- `COPILOT_ADVICE_PATH` 环境变量
- 相对于游戏工作目录的 `output/advice.txt`

通过 CommunicationMod 使用 copilot 时，工作目录就是 Slay the Spire 安装目录，无需额外配置。游戏语言为中文时，自动显示中文标签。

## Build

Dependencies live in `lib/` (gitignored). Copy jars from your Steam install:

```bash
STS=/home/howard/.local/share/Steam/steamapps/common/SlayTheSpire
WS=/home/howard/.local/share/Steam/steamapps/workshop/content/646570
cp "$STS/desktop-1.0.jar" "$WS/1605060445/ModTheSpire.jar" \
   "$WS/1605833019/BaseMod.jar" "$WS/1609158507/StSLib.jar" lib/
```

Build with Maven:

```bash
mvn package
cp target/CopilotOverlay.jar "$STS/mods/"
```

## License

MIT
