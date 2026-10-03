package com.tous.camkey.client;

import java.util.Optional;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

import com.tous.camkey.CamKey;
import com.tous.camkey.model.Keyframe;
import com.tous.camkey.playback.PlaybackCamera;
import com.tous.camkey.session.CamKeySession;

/**
 * Connects the session's playback state to the game loop: advances it each client tick
 * (≈ FixedUpdate), applies it to the camera each render frame (≈ LateUpdate), and blocks player
 * input while it owns the camera.
 */
@EventBusSubscriber(modid = CamKey.MODID, value = Dist.CLIENT)
public final class PlaybackDriver {

    private static final double TICK_SECONDS = 1.0 / 20.0;

    private static boolean wasPlaying;

    private PlaybackDriver() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return;
        }

        CamKeySession session = CamKeySessionHolder.session();
        boolean isPlaying = session.isPlaying();
        if (isPlaying && !wasPlaying) {
            PlaybackCamera.begin(minecraft, player);
        } else if (!isPlaying && wasPlaying) {
            PlaybackCamera.end(minecraft, player);
        }
        wasPlaying = isPlaying;

        if (!isPlaying) {
            return;
        }

        session.tickPlayback(TICK_SECONDS);
        Optional<Keyframe> from = session.playbackKeyframeAt(0.0);
        Optional<Keyframe> to = session.playbackKeyframeAt(1.0);
        if (from.isPresent() && to.isPresent()) {
            PlaybackCamera.applyTick(minecraft, player, from.get(), to.get());
        }
    }

    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        CamKeySessionHolder.session().playbackKeyframeAt(event.getPartialTick())
                .ifPresent(keyframe -> PlaybackCamera.applyRotation(event, keyframe));
    }

    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        if (!CamKeySessionHolder.session().isPlaying()) {
            return;
        }
        var input = event.getInput();
        input.leftImpulse = 0.0F;
        input.forwardImpulse = 0.0F;
        input.up = false;
        input.down = false;
        input.left = false;
        input.right = false;
        input.jumping = false;
        input.shiftKeyDown = false;
    }

    @SubscribeEvent
    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        if (CamKeySessionHolder.session().isPlaying()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        // The session is about to be replaced for the next world (or discarded entirely).
        if (wasPlaying) {
            PlaybackCamera.abandon(Minecraft.getInstance());
        }
        wasPlaying = false;
    }
}
