package com.tous.camkey.playback;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ViewportEvent;

import com.tous.camkey.model.Keyframe;

/**
 * Per-frame camera control: puts the player's view where the playback path says it should be.
 *
 * <p>Minecraft's {@code Camera} has no public position setter and NeoForge has no event that exposes
 * camera position, so position is driven by moving the player entity the camera follows. Rotation
 * is overridden every render frame via {@link ViewportEvent.ComputeCameraAngles}.
 */
public final class PlaybackCamera {

    private static CameraType cameraTypeBeforePlayback;

    private PlaybackCamera() {
    }

    public static void begin(Minecraft minecraft, LocalPlayer player) {
        SpectatorGuard.engage(minecraft, player.getUUID());
        cameraTypeBeforePlayback = minecraft.options.getCameraType();
    }

    public static void end(Minecraft minecraft, LocalPlayer player) {
        SpectatorGuard.release(minecraft, player.getUUID());
        restoreCameraType(minecraft);
    }

    /**
     * For a playback cut short by leaving the world. The integrated server is already unreachable
     * by then, so only the client-side view is restored here — SpectatorGuard restores the game mode
     * from its own server-side logout hook.
     */
    public static void abandon(Minecraft minecraft) {
        restoreCameraType(minecraft);
    }

    /**
     * Places the player at the end of this tick's stretch of path, with its "old" position at the
     * start of it. Camera.setup() lerps old -> current by partial tick every render frame, so the
     * camera glides between ticks instead of stepping 20 times a second. (Entity.moveTo is not used:
     * it also resets the old position, which disables that lerp.)
     */
    public static void applyTick(Minecraft minecraft, LocalPlayer player, Keyframe from, Keyframe to) {
        // Keyframes are where the camera itself was, so playback must view from the eyes. Re-applied
        // every tick so pressing F5 mid-playback can't pull the camera back behind the player.
        minecraft.options.setCameraType(CameraType.FIRST_PERSON);

        // Keyframes store the camera (eye) position, but entity position is at the feet.
        double eyeHeight = player.getEyeHeight();
        player.setPos(to.x(), to.y() - eyeHeight, to.z());
        player.xo = from.x();
        player.yo = from.y() - eyeHeight;
        player.zo = from.z();
        player.setYRot(to.yaw());
        player.setXRot(to.pitch());
        player.setDeltaMovement(Vec3.ZERO);
    }

    /**
     * The player's own rotation isn't lerped by the renderer (LocalPlayer.getViewYRot returns the raw
     * value), so the caller samples the path at the frame's partial tick and it's applied directly.
     */
    public static void applyRotation(ViewportEvent.ComputeCameraAngles event, Keyframe keyframe) {
        event.setYaw(keyframe.yaw());
        event.setPitch(keyframe.pitch());
    }

    private static void restoreCameraType(Minecraft minecraft) {
        if (cameraTypeBeforePlayback != null) {
            minecraft.options.setCameraType(cameraTypeBeforePlayback);
            cameraTypeBeforePlayback = null;
        }
    }
}
