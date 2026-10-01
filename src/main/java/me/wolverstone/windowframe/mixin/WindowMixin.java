package me.wolverstone.windowframe.mixin;

import com.mojang.blaze3d.platform.Window;
import me.wolverstone.windowframe.FrameFixer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Window.class)
public abstract class WindowMixin {
	@Shadow
	@Final
	private long handle;
	@Shadow
	private boolean fullscreen;
	@Shadow
	private boolean fullscreenRequested;
	@Shadow
	private boolean borderlessFullscreen;

	@Unique
	private boolean windowframe$started;

	@Unique
	private boolean windowframe$isWindowed() {
		return !this.fullscreen && !this.fullscreenRequested && !this.borderlessFullscreen;
	}

	/**
	 * setMode runs from the constructor, on F11 / video settings changes, setWindowed and exclusive fullscreen toggles.
	 */
	@Inject(method = "setMode", at = @At("TAIL"))
	private void windowframe$afterSetMode(CallbackInfo ci) {
		String reason = this.windowframe$started ? "mode change" : "startup";
		this.windowframe$started = true;
		if (this.windowframe$isWindowed()) {
			FrameFixer.apply(this.handle, reason, true);
		}
	}

	@Inject(method = "onIconified", at = @At("TAIL"))
	private void windowframe$afterIconified(boolean iconified, CallbackInfo ci) {
		if (!iconified && this.windowframe$isWindowed()) {
			FrameFixer.apply(this.handle, "restore", false);
		}
	}

	/**
	 * Covers leaving fullscreen through SDL events that did not go through setMode.
	 */
	@Inject(method = "updateFullscreenState", at = @At("TAIL"))
	private void windowframe$afterFullscreenState(CallbackInfo ci) {
		if (this.windowframe$started && this.windowframe$isWindowed()) {
			FrameFixer.apply(this.handle, "fullscreen state", false);
		}
	}
}
