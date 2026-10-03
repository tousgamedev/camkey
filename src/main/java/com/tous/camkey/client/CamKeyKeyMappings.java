package com.tous.camkey.client;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;

import com.tous.camkey.CamKey;

@EventBusSubscriber(modid = CamKey.MODID, value = Dist.CLIENT)
public final class CamKeyKeyMappings {

    private static final String CATEGORY = "key.categories.camkey";

    public static final KeyMapping ADD_KEYFRAME =
            new KeyMapping("key.camkey.add", KeyConflictContext.IN_GAME, InputConstants.UNKNOWN, CATEGORY);
    public static final KeyMapping DELETE_LAST_KEYFRAME =
            new KeyMapping("key.camkey.delete", KeyConflictContext.IN_GAME, InputConstants.UNKNOWN, CATEGORY);
    public static final KeyMapping TOGGLE_PLAYBACK_PREVIEW =
            new KeyMapping("key.camkey.play_preview", KeyConflictContext.IN_GAME, InputConstants.UNKNOWN, CATEGORY);
    public static final KeyMapping TOGGLE_PLAYBACK_RECORD =
            new KeyMapping("key.camkey.play_record", KeyConflictContext.IN_GAME, InputConstants.UNKNOWN, CATEGORY);

    private CamKeyKeyMappings() {
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(ADD_KEYFRAME);
        event.register(DELETE_LAST_KEYFRAME);
        event.register(TOGGLE_PLAYBACK_PREVIEW);
        event.register(TOGGLE_PLAYBACK_RECORD);
    }
}
