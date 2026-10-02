package com.tous.camkey.command;

import java.nio.file.Path;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

import com.tous.camkey.CamKey;
import com.tous.camkey.storage.JsonSequenceStorage;

@EventBusSubscriber(modid = CamKey.MODID, value = Dist.CLIENT)
public final class CamKeySessionHolder {

    private static final String STORAGE_DIR_NAME = "camkey";

    private static CamKeySession session = newFallbackSession();

    private CamKeySessionHolder() {
    }

    public static CamKeySession session() {
        return session;
    }

    @SubscribeEvent
    public static void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.hasSingleplayerServer()) {
            // Multiplayer is out of scope; leave whatever storage is already active rather than
            // guess at a path on a remote server.
            return;
        }
        Path worldPath = minecraft.getSingleplayerServer().getWorldPath(LevelResource.ROOT).resolve(STORAGE_DIR_NAME);
        session = new CamKeySession(new JsonSequenceStorage(worldPath));
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        session = newFallbackSession();
    }

    private static CamKeySession newFallbackSession() {
        return new CamKeySession(
                new JsonSequenceStorage(Minecraft.getInstance().gameDirectory.toPath().resolve("camkey-data")));
    }
}
