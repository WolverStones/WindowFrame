# Changelog

## 1.0.0
- Initial release for Minecraft 26.3.
- Restores the title bar (minimize / maximize / close) and the resizable border whenever the game is in windowed mode.
- The window is re-checked at startup, on F11, on fullscreen changes in Video Settings and when restored from the taskbar.
- A window that covers the whole monitor after leaving fullscreen is resized (1280×720 by default) and centered.
- Fullscreen (exclusive and borderless) is never touched.
- Windows-only fallback that repairs the native window style directly if SDL alone is not enough.
- Simple config in `config/windowframe.properties`: on/off switch, fallback, window size and verbose logging.
- Requires Minecraft 26.3.
