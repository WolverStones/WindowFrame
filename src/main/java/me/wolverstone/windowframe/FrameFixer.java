package me.wolverstone.windowframe;

import static me.wolverstone.windowframe.WindowFrameClient.LOGGER;

import java.nio.IntBuffer;
import net.minecraft.util.Util;
import org.lwjgl.sdl.SDLError;
import org.lwjgl.sdl.SDLProperties;
import org.lwjgl.sdl.SDLVideo;
import org.lwjgl.sdl.SDL_Rect;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.windows.User32;

/**
 * Makes sure the SDL3 game window has a title bar and a resizable border while it is in windowed mode.
 * Callers must only invoke {@link #apply} when the game considers the window windowed (not exclusive
 * fullscreen and not the Windows borderless fullscreen window).
 */
public final class FrameFixer {
	// SDL_WindowFlags (SDL_video.h)
	private static final long SDL_WINDOW_FULLSCREEN = 0x1L;
	private static final long SDL_WINDOW_BORDERLESS = 0x10L;
	private static final long SDL_WINDOW_RESIZABLE = 0x20L;
	private static final long SDL_WINDOW_MINIMIZED = 0x40L;
	private static final long SDL_WINDOW_MAXIMIZED = 0x80L;

	private static final boolean WINDOWS = Util.getPlatform() == Util.OS.WINDOWS;
	private static boolean backendLogged;

	private FrameFixer() {
	}

	/**
	 * @param reason        what triggered the check, for the log
	 * @param allowResize   whether an oversized window may be shrunk and centered (only right after a mode change)
	 */
	public static void apply(long handle, String reason, boolean allowResize) {
		WindowFrameConfig config = WindowFrameClient.config();
		if (!config.enabled || handle == 0L) {
			return;
		}

		logBackendOnce(config);

		long flags = SDLVideo.SDL_GetWindowFlags(handle);
		if ((flags & SDL_WINDOW_FULLSCREEN) != 0L) {
			// Fullscreen is never touched.
			return;
		}
		if ((flags & SDL_WINDOW_MINIMIZED) != 0L) {
			// Checked again when the window is restored.
			return;
		}

		if (config.verbose) {
			LOGGER.info("[{}] window flags=0x{}{}", reason, Long.toHexString(flags), WINDOWS ? ", style=0x" + Long.toHexString(win32Style(handle)) : "");
		}

		boolean changed = applySdl(handle, flags, reason);
		if (WINDOWS && config.win32Fallback) {
			changed |= applyWin32(handle, reason);
		}
		if (allowResize && config.fixOversizedWindow && (SDLVideo.SDL_GetWindowFlags(handle) & SDL_WINDOW_MAXIMIZED) == 0L) {
			changed |= fitOnScreen(handle, config, reason);
		}
		if (changed) {
			SDLVideo.SDL_SyncWindow(handle);
		}
	}

	private static void logBackendOnce(WindowFrameConfig config) {
		if (!backendLogged) {
			backendLogged = true;
			LOGGER.info("Detected window backend: SDL3 (video driver '{}', platform {}); Win32 fallback {}",
					SDLVideo.SDL_GetCurrentVideoDriver(), Util.getPlatform(),
					!WINDOWS ? "not applicable" : config.win32Fallback ? "enabled" : "disabled");
		}
	}

	private static boolean applySdl(long handle, long flags, String reason) {
		boolean changed = false;
		if ((flags & SDL_WINDOW_BORDERLESS) != 0L) {
			if (SDLVideo.SDL_SetWindowBordered(handle, true)) {
				LOGGER.info("[{}] Restored window decorations via SDL_SetWindowBordered", reason);
				changed = true;
			} else {
				LOGGER.warn("[{}] SDL_SetWindowBordered failed: {}", reason, SDLError.SDL_GetError());
			}
		}
		if ((flags & SDL_WINDOW_RESIZABLE) == 0L) {
			if (SDLVideo.SDL_SetWindowResizable(handle, true)) {
				LOGGER.info("[{}] Made window resizable via SDL_SetWindowResizable", reason);
				changed = true;
			} else {
				LOGGER.warn("[{}] SDL_SetWindowResizable failed: {}", reason, SDLError.SDL_GetError());
			}
		}
		return changed;
	}

	private static long hwnd(long handle) {
		int props = SDLVideo.SDL_GetWindowProperties(handle);
		return props == 0 ? 0L : SDLProperties.SDL_GetPointerProperty(props, SDLVideo.SDL_PROP_WINDOW_WIN32_HWND_POINTER, 0L);
	}

	private static long win32Style(long handle) {
		long hwnd = hwnd(handle);
		return hwnd == 0L ? 0L : User32.GetWindowLongPtr(hwnd, User32.GWL_STYLE);
	}

	private static boolean applyWin32(long handle, String reason) {
		long hwnd = hwnd(handle);
		if (hwnd == 0L) {
			return false;
		}

		long style = User32.GetWindowLongPtr(hwnd, User32.GWL_STYLE);
		boolean hasPopup = (style & User32.WS_POPUP) != 0L;
		boolean hasFrame = (style & User32.WS_OVERLAPPEDWINDOW) == User32.WS_OVERLAPPEDWINDOW;
		if (!hasPopup && hasFrame) {
			return false;
		}

		long newStyle = (style & ~(long)User32.WS_POPUP) | User32.WS_OVERLAPPEDWINDOW;
		User32.SetWindowLongPtr(null, hwnd, User32.GWL_STYLE, newStyle);
		User32.SetWindowPos(null, hwnd, 0L, 0, 0, 0, 0,
				User32.SWP_FRAMECHANGED | User32.SWP_NOMOVE | User32.SWP_NOSIZE | User32.SWP_NOZORDER | User32.SWP_NOACTIVATE);
		LOGGER.info("[{}] Restored window frame via Win32 (style 0x{} -> 0x{})", reason, Long.toHexString(style), Long.toHexString(newStyle));
		return true;
	}

	/**
	 * If the windowed window covers the whole usable area of its monitor (or its title bar is off screen),
	 * shrink it to the configured size and center it so the title bar is reachable.
	 */
	private static boolean fitOnScreen(long handle, WindowFrameConfig config, String reason) {
		int display = SDLVideo.SDL_GetDisplayForWindow(handle);
		if (display == 0) {
			return false;
		}

		try (MemoryStack stack = MemoryStack.stackPush()) {
			SDL_Rect usable = SDL_Rect.malloc(stack);
			if (!SDLVideo.SDL_GetDisplayUsableBounds(display, usable)) {
				return false;
			}

			IntBuffer x = stack.mallocInt(1);
			IntBuffer y = stack.mallocInt(1);
			IntBuffer w = stack.mallocInt(1);
			IntBuffer h = stack.mallocInt(1);
			if (!SDLVideo.SDL_GetWindowPosition(handle, x, y) || !SDLVideo.SDL_GetWindowSize(handle, w, h)) {
				return false;
			}

			IntBuffer top = stack.callocInt(1);
			IntBuffer left = stack.callocInt(1);
			IntBuffer bottom = stack.callocInt(1);
			IntBuffer right = stack.callocInt(1);
			// May fail before the window is shown; borders are then treated as zero.
			SDLVideo.SDL_GetWindowBordersSize(handle, top, left, bottom, right);
			int borderW = left.get(0) + right.get(0);
			int borderH = top.get(0) + bottom.get(0);

			boolean coversMonitor = w.get(0) + borderW >= usable.w() && h.get(0) + borderH >= usable.h();
			boolean titleBarHidden = y.get(0) - top.get(0) < usable.y();
			if (!coversMonitor && !titleBarHidden) {
				return false;
			}

			int newW = w.get(0);
			int newH = h.get(0);
			if (coversMonitor) {
				newW = Math.max(320, Math.min(config.defaultWidth, usable.w() - borderW));
				newH = Math.max(240, Math.min(config.defaultHeight, usable.h() - borderH));
				SDLVideo.SDL_SetWindowSize(handle, newW, newH);
			}

			int newX = usable.x() + (usable.w() - newW - borderW) / 2 + left.get(0);
			int newY = usable.y() + (usable.h() - newH - borderH) / 2 + top.get(0);
			SDLVideo.SDL_SetWindowPosition(handle, newX, newY);
			LOGGER.info("[{}] Window {}x{} at {},{} did not fit monitor {}x{}; resized to {}x{} at {},{}", reason,
					w.get(0), h.get(0), x.get(0), y.get(0), usable.w(), usable.h(), newW, newH, newX, newY);
			return true;
		}
	}
}
