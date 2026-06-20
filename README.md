# Copilot Overlay Mod

In-game overlay for Slay the Spire that displays AI copilot advice. Reads `output/overlay.json` from the copilot tool and renders it as a semi-transparent overlay in the top-right corner. Falls back to `output/advice.txt` for backward compatibility. Companion to [slay-the-spire-copilot](https://github.com/ArtemisSaber/slay-the-spire-copilot).

《杀戮尖塔》游戏内悬浮窗模组。读取 copilot 工具输出的 `output/overlay.json`，以半透明悬浮窗形式展示在屏幕右上角。支持回退到 `output/advice.txt` 以保证向后兼容。配合 [slay-the-spire-copilot](https://github.com/ArtemisSaber/slay-the-spire-copilot) 使用。

## Requirements

- Slay the Spire
- ModTheSpire 3.30+
- BaseMod

## Install

1. Download `CopilotOverlay.jar` from [Releases](https://github.com/ArtemisSaber/slay-the-spire-copilot-overlay-mod/releases).
2. Place it in Slay the Spire's `mods/` directory.
3. Launch the game with ModTheSpire and enable **Copilot Overlay** (requires BaseMod).

## Usage

The overlay polls `output/overlay.json` every 500ms and updates automatically. Falls back to `output/advice.txt` if the JSON file is absent. The file path resolves as:

- `COPILOT_ADVICE_PATH` environment variable, or
- `output/overlay.json` relative to the game's working directory

When used with the copilot tool via CommunicationMod, the working directory is the Slay the Spire install directory — no extra config needed.

### Rendering states

The JSON `status` field drives three rendering modes:

| Status | Display |
|--------|---------|
| `loading` | "少女祈祷中..." / "A few moments later..." with animated ellipsis and pulsing alpha |
| `ok` | Structured advice fields (推荐/理由/风险/吐槽) with cyan labels |
| `error` | Error message in red-tinted text |

### Visibility lifecycle

The overlay uses the copilot's `overlay_visibility` field to control show/hide:

- **Appear** — fades in over 400ms
- **Content change** — brief 200ms alpha dip (1.0 → 0.7 → 1.0) for smooth transitions
- **Hide** — fades out over 2 seconds (configurable via `fadeDurationMs`)

### Staleness detection

Data with a `timestamp_ms` older than 60 seconds is treated as a previous run and hidden immediately — no stale advice is ever displayed. This threshold is configurable via `OverlayConfig.maxDataAgeMs`.

### Labels

The overlay uses Chinese field labels (`推荐`/`理由`/`风险`/`吐槽`) when the game language is set to Chinese, and English labels otherwise.

---

悬浮窗每 500ms 读取 `output/overlay.json` 并自动更新。如果 JSON 文件不存在，会回退到 `output/advice.txt`。文件路径查找顺序：

- `COPILOT_ADVICE_PATH` 环境变量
- 相对于游戏工作目录的 `output/overlay.json`

通过 CommunicationMod 使用 copilot 时，工作目录即为 Slay the Spire 安装目录，无需额外配置。

### 渲染状态

JSON 中的 `status` 字段驱动三种渲染模式：

| 状态 | 显示 |
|------|------|
| `loading` | 「少女祈祷中...」带动态省略号和呼吸灯效果 |
| `ok` | 结构化建议字段（推荐/理由/风险/吐槽），青色标签 |
| `error` | 红色调错误信息文本 |

### 可见性生命周期

悬浮窗通过 copilot 的 `overlay_visibility` 字段控制显示/隐藏：

- **出现** — 400ms 淡入动画
- **内容变化** — 200ms 轻微闪烁（透明度 1.0 → 0.7 → 1.0）
- **隐藏** — 2 秒淡出（可通过 `fadeDurationMs` 配置）

### 过期检测

`timestamp_ms` 超过 60 秒的数据被视为上次运行的残留，立即隐藏不显示。该阈值可通过 `OverlayConfig.maxDataAgeMs` 配置。

### 标签

游戏语言为中文时，自动使用中文标签（`推荐`/`理由`/`风险`/`吐槽`），否则使用英文标签。

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
