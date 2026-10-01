package me.wolverstone.windowframe;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Simple properties config stored in {@code config/windowframe.properties}.
 */
public final class WindowFrameConfig {
	private static final String FILE_NAME = "windowframe.properties";

	public boolean enabled = true;
	public boolean win32Fallback = true;
	public boolean fixOversizedWindow = true;
	public int defaultWidth = 1280;
	public int defaultHeight = 720;
	public boolean verbose = false;

	public static WindowFrameConfig load() {
		WindowFrameConfig config = new WindowFrameConfig();
		Path path = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);

		if (Files.exists(path)) {
			Properties props = new Properties();
			try (Reader reader = Files.newBufferedReader(path)) {
				props.load(reader);
				config.enabled = bool(props, "enabled", config.enabled);
				config.win32Fallback = bool(props, "win32Fallback", config.win32Fallback);
				config.fixOversizedWindow = bool(props, "fixOversizedWindow", config.fixOversizedWindow);
				config.defaultWidth = Math.max(320, integer(props, "defaultWidth", config.defaultWidth));
				config.defaultHeight = Math.max(240, integer(props, "defaultHeight", config.defaultHeight));
				config.verbose = bool(props, "verbose", config.verbose);
			} catch (IOException | IllegalArgumentException e) {
				WindowFrameClient.LOGGER.warn("Failed to read {}, using defaults", path, e);
			}
		}

		config.save(path);
		return config;
	}

	private void save(Path path) {
		Properties props = new Properties();
		props.setProperty("enabled", Boolean.toString(enabled));
		props.setProperty("win32Fallback", Boolean.toString(win32Fallback));
		props.setProperty("fixOversizedWindow", Boolean.toString(fixOversizedWindow));
		props.setProperty("defaultWidth", Integer.toString(defaultWidth));
		props.setProperty("defaultHeight", Integer.toString(defaultHeight));
		props.setProperty("verbose", Boolean.toString(verbose));

		try {
			Files.createDirectories(path.getParent());
			try (Writer writer = Files.newBufferedWriter(path)) {
				props.store(writer, """
						WindowFrame config
						 enabled             - restore title bar and resizable border in windowed mode
						 win32Fallback       - on Windows, also fix the native window style directly if SDL is not enough
						 fixOversizedWindow  - shrink and center the window if it covers the whole monitor after leaving fullscreen
						 defaultWidth/Height - window size used by fixOversizedWindow
						 verbose             - log window state on every check, not only when something changes""");
			}
		} catch (IOException e) {
			WindowFrameClient.LOGGER.warn("Failed to write {}", path, e);
		}
	}

	private static boolean bool(Properties props, String key, boolean fallback) {
		String value = props.getProperty(key);
		return value == null ? fallback : Boolean.parseBoolean(value.trim());
	}

	private static int integer(Properties props, String key, int fallback) {
		String value = props.getProperty(key);
		return value == null ? fallback : Integer.parseInt(value.trim());
	}
}
