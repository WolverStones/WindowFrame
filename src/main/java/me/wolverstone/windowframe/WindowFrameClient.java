package me.wolverstone.windowframe;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class WindowFrameClient implements ClientModInitializer {
	public static final String MOD_ID = "windowframe";
	public static final Logger LOGGER = LoggerFactory.getLogger("WindowFrame");

	private static WindowFrameConfig config;

	@Override
	public void onInitializeClient() {
		config();
	}

	/**
	 * Loaded lazily, because the window may be created before or after client entrypoints run.
	 */
	public static synchronized WindowFrameConfig config() {
		if (config == null) {
			config = WindowFrameConfig.load();
			LOGGER.info("Config loaded: enabled={}, win32Fallback={}, fixOversizedWindow={}, defaultSize={}x{}",
					config.enabled, config.win32Fallback, config.fixOversizedWindow, config.defaultWidth, config.defaultHeight);
		}
		return config;
	}
}
