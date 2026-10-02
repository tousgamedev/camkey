package com.tous.camkey.command;

import java.util.Optional;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

import com.tous.camkey.CamKey;
import com.tous.camkey.capture.CameraCapture;
import com.tous.camkey.model.Keyframe;

@EventBusSubscriber(modid = CamKey.MODID, value = Dist.CLIENT)
public final class CamKeyPlaybackHandler {

    private static final double TICK_SECONDS = 1.0 / 20.0;

    private static boolean wasPlaying;
    private static GameType previousGameMode;
    private static boolean showHudForCurrentPlayback = true;

    private CamKeyPlaybackHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        CamKeySession session = CamKeySessionHolder.session();
        handleKeybinds(session);

        boolean isPlaying = session.isPlaying();
        if (isPlaying && !wasPlaying) {
            onPlaybackStarted(player);
        } else if (!isPlaying && wasPlaying) {
            onPlaybackStopped(player);
        }
        wasPlaying = isPlaying;

        if (!isPlaying) {
            return;
        }

        session.tickPlayback(TICK_SECONDS);
        session.currentPlaybackKeyframe().ifPresent(keyframe ->
                player.moveTo(keyframe.x(), keyframe.y(), keyframe.z(), keyframe.yaw(), keyframe.pitch()));
    }

    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        CamKeySession session = CamKeySessionHolder.session();
        if (!session.isPlaying()) {
            return;
        }
        Optional<Keyframe> keyframe = session.currentPlaybackKeyframe();
        if (keyframe.isEmpty()) {
            return;
        }
        event.setYaw(keyframe.get().yaw());
        event.setPitch(keyframe.get().pitch());
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
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (!CamKeySessionHolder.session().isPlaying() || !showHudForCurrentPlayback) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Component keyName = CamKeyKeyMappings.TOGGLE_PLAYBACK_PREVIEW.getTranslatedKeyMessage();
        Component message = Component.translatable("camkey.playback.hud", keyName);
        int x = minecraft.getWindow().getGuiScaledWidth() / 2;
        event.getGuiGraphics().drawCenteredString(minecraft.font, message, x, 10, 0xFFFFFF);
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        // The session is about to be replaced for the next world (or discarded entirely) — drop
        // our tracking so a stale previousGameMode from this world can't leak into the next.
        wasPlaying = false;
        previousGameMode = null;
    }

    private static void handleKeybinds(CamKeySession session) {
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
        boolean suppressFeedback = startingNow ? !previewMode : !showHudForCurrentPlayback;

        CommandResult result = session.toggleActivePlayback();
        if (startingNow && result.success()) {
            showHudForCurrentPlayback = previewMode;
        }

        // Failures always get reported — a producer needs to know a take didn't start, even in
        // Record mode — only successful start/cancel confirmations are ever suppressed.
        if (!suppressFeedback || !result.success()) {
            report(result);
        }
    }

    private static void report(CommandResult result) {
        Component message = Component.translatable(result.translationKey(), result.args());
        if (!result.success()) {
            message = message.copy().withStyle(ChatFormatting.RED);
        }
        Minecraft.getInstance().gui.getChat().addMessage(message);
    }

    private static void onPlaybackStarted(LocalPlayer player) {
        findServerPlayer(player).ifPresent(serverPlayer -> {
            previousGameMode = serverPlayer.gameMode.getGameModeForPlayer();
            serverPlayer.setGameMode(GameType.SPECTATOR);
        });
    }

    private static void onPlaybackStopped(LocalPlayer player) {
        if (previousGameMode == null) {
            return;
        }
        GameType restoreTo = previousGameMode;
        previousGameMode = null;
        findServerPlayer(player).ifPresent(serverPlayer -> serverPlayer.setGameMode(restoreTo));
    }

    private static Optional<ServerPlayer> findServerPlayer(LocalPlayer player) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.hasSingleplayerServer()) {
            return Optional.empty();
        }
        return Optional.ofNullable(minecraft.getSingleplayerServer().getPlayerList().getPlayer(player.getUUID()));
    }
}
