# WindowFrame

A small client-side Fabric mod for Minecraft 26.3 that makes sure the game window has a normal title bar
(minimize / maximize / close) and a resizable border whenever the game runs in windowed mode.
Fullscreen (exclusive and the borderless "non-exclusive" fullscreen) is never touched.

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/) 0.19.5+ and [Fabric API](https://modrinth.com/mod/fabric-api) for Minecraft 26.3.
2. Put `windowframe-<version>.jar` into the `mods` folder (client only, not needed on servers).
3. Start the game. Nothing else has to be run, the fix applies automatically.

If the game starts in fullscreen (it was last closed in fullscreen, or `fullscreen:true` in `options.txt`), it stays
in fullscreen. Press **F11** once; the window gets its frame and Minecraft remembers windowed mode for the next launches.
Modpacks can ship `options.txt` with `fullscreen:false` to start framed right away.

## Configuration

`config/windowframe.properties` is created on first start:

| Key | Default | Meaning |
| --- | --- | --- |
| `enabled` | `true` | Master switch |
| `win32Fallback` | `true` | On Windows, also repair the native window style (`WS_OVERLAPPEDWINDOW`) if SDL alone is not enough |
| `fixOversizedWindow` | `true` | After switching to windowed mode, shrink and center a window that covers the whole monitor or has its title bar off screen |
| `defaultWidth` / `defaultHeight` | `1280` / `720` | Size used by `fixOversizedWindow` (capped to the monitor's usable area) |
| `verbose` | `false` | Log the window state on every check, not only when something is changed |

## How it works

Since 26.3 the game uses **SDL3** (through LWJGL 3.4) instead of GLFW. The window is created by
`com.mojang.blaze3d.platform.Window` with `SDL_WINDOW_RESIZABLE`. The only place vanilla removes the
decorations is the Windows borderless fullscreen path (`applyBorderlessFullscreenWindow`, `SDL_SetWindowBordered(false)`),
which is undone by `leaveBorderlessFullscreenWindow` when going back to windowed mode.

A single mixin into `Window` re-checks the window after:

- `setMode` – startup, F11, the fullscreen option in Video Settings, exclusive fullscreen toggle,
- `onIconified(false)` – restoring from the taskbar,
- `updateFullscreenState` – fullscreen left through an SDL event.

Only when the game itself considers the window windowed (`fullscreen`, `fullscreenRequested` and
`borderlessFullscreen` all false, no `SDL_WINDOW_FULLSCREEN` flag) the mod:

1. calls `SDL_SetWindowBordered(true)` / `SDL_SetWindowResizable(true)` if the SDL flags say the frame is missing,
2. on Windows reads the HWND (`SDL_PROP_WINDOW_WIN32_HWND_POINTER`) and, if the style still has `WS_POPUP` or misses parts
   of `WS_OVERLAPPEDWINDOW`, fixes it with `SetWindowLongPtr` + `SetWindowPos(SWP_FRAMECHANGED | SWP_NOMOVE | SWP_NOSIZE | SWP_NOZORDER | SWP_NOACTIVATE)`
   via `org.lwjgl.system.windows.User32` (no JNA); skipped on other systems,
3. after a mode change, resizes a window that covers the whole monitor to the configured size and centers it.

Every change is logged with the `WindowFrame` logger, together with the detected backend at startup.
Resizing goes through normal SDL events, so the framebuffer, GUI scale and the Vulkan/OpenGL surface are updated by vanilla code.
All sizes are SDL window coordinates, so it works with Windows display scaling (tested at 125 %).

## Known limitations

- Built for Minecraft **26.3** (SDL3). 26.1 and 26.2 use GLFW and are not supported by this jar.
- A maximized window is never resized; a window you deliberately size to the whole monitor gets shrunk after the next
  switch from fullscreen to windowed (disable `fixOversizedWindow` if you do not want that).
- The Win32 fallback changes the style behind SDL's back; SDL recomputes the style on its own window operations, which is fine
  because the mod re-checks after them.
