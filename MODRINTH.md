# WindowFrame

**Get your normal game window back.** WindowFrame is a tiny client-side Fabric mod that makes sure Minecraft has a proper title bar (minimize / maximize / close) and a resizable border every time it runs in windowed mode.

Fullscreen is never touched, and the mod works on its own as soon as the game starts. There are no buttons and no setup.

---

## ✨ What it does

- **🪟 Title bar and border in windowed mode.** Minimize, maximize and close buttons, drag-to-resize edges, moving the window, and Win + arrow snapping all work.
- **🔁 Re-checked when it matters.** The window is checked at startup, when you toggle F11, when you change fullscreen in Video Settings, and when you restore the game from the taskbar.
- **📐 Fits the window on screen.** If the window covers the whole monitor (title bar off screen) after leaving fullscreen, it is resized to 1280×720 (configurable) and centered.
- **🖥️ Leaves fullscreen alone.** Exclusive fullscreen and borderless fullscreen are never touched.
- **📏 DPI-aware.** It works with Windows display scaling, and the game resizes its framebuffer and GUI normally.
- **🧩 Modpack-friendly.** It is client-only, has one small mixin, and needs nothing besides Fabric API.

---

## ⚙️ Configuration

On first launch WindowFrame creates `config/windowframe.properties`:

```properties
enabled=true
win32Fallback=true
fixOversizedWindow=true
defaultWidth=1280
defaultHeight=720
verbose=false
```

| Option | What it does |
| --- | --- |
| `enabled` | Turns the whole mod on or off |
| `win32Fallback` | On Windows, also repairs the native window style directly if SDL alone is not enough |
| `fixOversizedWindow` | Shrinks and centers a window that covers the whole monitor after switching to windowed mode |
| `defaultWidth` / `defaultHeight` | Size used by `fixOversizedWindow` (never larger than your screen) |
| `verbose` | Logs the window state on every check, useful when reporting issues |

---

## 🔧 How it works

Minecraft 26.3 uses **SDL3** instead of GLFW for its window. WindowFrame hooks into the vanilla `Window` class. Whenever the game considers itself in windowed mode, it:

1. Restores the border and resizing through SDL (`SDL_SetWindowBordered`, `SDL_SetWindowResizable`).
2. On Windows only, checks the native window style and fixes it if needed (via LWJGL, no extra libraries).
3. Resizes and centers the window if it does not fit on the screen.

Everything it changes is written to the log under `WindowFrame`.

---

## 📋 Requirements

- Minecraft **26.3**
- Fabric Loader **0.19.5+**
- [Fabric API](https://modrinth.com/mod/fabric-api)

Client-side only. Not needed on servers.

---

## ⚠️ Known limitations

- Only supports **26.3** (SDL3). Minecraft 26.1 and 26.2 use GLFW and are not supported.
- A maximized window is never resized. If you size the window to fill the whole monitor yourself, it will be shrunk the next time you leave fullscreen. Set `fixOversizedWindow=false` if you don't want that.
- The title bar color follows your Windows light/dark app mode.
