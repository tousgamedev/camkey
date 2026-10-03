package com.tous.camkey.playback;

import java.util.UUID;
import java.util.function.Consumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import com.tous.camkey.CamKey;

/**
 * Puts the player in Spectator for the duration of a playback (no collision, no fall damage while
 * the entity is being flown along the path) and puts them back afterwards.
 *
 * <p>The original game mode is stored in the server player's persistent data rather than a static
 * field, so it is saved with the player: quitting mid-playback restores it on logout, and a crash
 * mid-playback restores it on the next login. Single-player only — on a remote server there is no
 * {@link IntegratedServer} to reach, and multiplayer is out of scope.
 */
@EventBusSubscriber(modid = CamKey.MODID, value = Dist.CLIENT)
public final class SpectatorGuard {

    private static final String SAVED_GAME_MODE_TAG = CamKey.MODID + ":game_mode_before_playback";

    private SpectatorGuard() {
    }

    public static void engage(Minecraft minecraft, UUID playerId) {
        runOnServerPlayer(minecraft, playerId, SpectatorGuard::switchToSpectator);
    }

    public static void release(Minecraft minecraft, UUID playerId) {
        runOnServerPlayer(minecraft, playerId, SpectatorGuard::restoreGameMode);
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        // Fires before the player is saved, so the restored game mode is what gets written to disk.
        if (event.getEntity() instanceof ServerPlayer player) {
            restoreGameMode(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        // Only does anything if the game died mid-playback, leaving the saved tag behind.
        if (event.getEntity() instanceof ServerPlayer player) {
            restoreGameMode(player);
        }
    }

    private static void runOnServerPlayer(Minecraft minecraft, UUID playerId, Consumer<ServerPlayer> action) {
        IntegratedServer server = minecraft.getSingleplayerServer();
        if (server == null) {
            return;
        }
        // The integrated server runs on its own thread; hand the work to it rather than touching
        // server state from the render thread.
        server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player != null) {
                action.accept(player);
            }
        });
    }

    private static void switchToSpectator(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(SAVED_GAME_MODE_TAG)) {
            data.putString(SAVED_GAME_MODE_TAG, player.gameMode.getGameModeForPlayer().getName());
        }
        player.setGameMode(GameType.SPECTATOR);
    }

    private static void restoreGameMode(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(SAVED_GAME_MODE_TAG)) {
            return;
        }
        GameType previous = GameType.byName(data.getString(SAVED_GAME_MODE_TAG), GameType.SURVIVAL);
        data.remove(SAVED_GAME_MODE_TAG);
        player.setGameMode(previous);
    }
}
