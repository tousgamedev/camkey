package com.tous.camkey.client;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import com.tous.camkey.CamKey;
import com.tous.camkey.capture.CameraCapture;
import com.tous.camkey.session.CamKeySession;
import com.tous.camkey.session.CommandResult;

/**
 * The keybind counterpart to {@code CamKeyCommands}: reads key presses, calls into the session, and
 * reports the result in chat.
 */
@EventBusSubscriber(modid = CamKey.MODID, value = Dist.CLIENT)
public final class KeybindHandler {

    private KeybindHandler() {
    }

    // HIGH priority so a playback started by a key press is picked up by PlaybackDriver on the same tick.
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onClientTick(ClientTickEvent.Post event) {
        if (Minecraft.getInstance().player == null) {
            return;
        }
        CamKeySession session = CamKeySessionHolder.session();

        if (CamKeyKeyMappings.TOGGLE_PLAYBACK_PREVIEW.consumeClick()) {
            togglePlayback(session, true);
        }
        if (CamKeyKeyMappings.TOGGLE_PLAYBACK_RECORD.consumeClick()) {
            togglePlayback(session, false);
        }

        if (session.isPlaying()) {
            // Drain (but ignore) add/delete while a scripted playback owns the camera, so a press
            // during playback doesn't queue up and fire the instant playback ends.
            CamKeyKeyMappings.ADD_KEYFRAME.consumeClick();
            CamKeyKeyMappings.DELETE_LAST_KEYFRAME.consumeClick();
            return;
        }

        if (CamKeyKeyMappings.ADD_KEYFRAME.consumeClick()) {
            Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
            report(session.addToActive(CameraCapture.capture(camera)));
        }
        if (CamKeyKeyMappings.DELETE_LAST_KEYFRAME.consumeClick()) {
            report(session.deleteLastFromActive());
        }
    }

    private static void togglePlayback(CamKeySession session, boolean previewMode) {
        boolean startingNow = !session.isPlaying();
        // Which mode's silence applies: the one being started now, or — when canceling — whichever
        // mode the already-running playback was started in, regardless of which key cancels it.
        boolean suppressFeedback = startingNow ? !previewMode : !session.isHintVisible();

        CommandResult result = session.toggleActivePlayback(previewMode);

        // Failures always get reported — a producer needs to know a take didn't start, even in
        // Record mode — only successful start/cancel confirmations are ever suppressed.
        if (!suppressFeedback || !result.success()) {
            report(result);
        }
    }

    private static void report(CommandResult result) {
        Minecraft.getInstance().gui.getChat().addMessage(ResultText.toComponent(result));
    }
}
