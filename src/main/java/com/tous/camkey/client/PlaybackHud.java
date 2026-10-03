package com.tous.camkey.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import com.tous.camkey.CamKey;

/**
 * Draws the "Playing... (Press X to Cancel)" hint during playback, unless the playback was started
 * in Record mode.
 */
@EventBusSubscriber(modid = CamKey.MODID, value = Dist.CLIENT)
public final class PlaybackHud {

    private PlaybackHud() {
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (!CamKeySessionHolder.session().isHintVisible()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Component keyName = CamKeyKeyMappings.TOGGLE_PLAYBACK_PREVIEW.getTranslatedKeyMessage();
        Component message = Component.translatable("camkey.playback.hud", keyName);
        int x = minecraft.getWindow().getGuiScaledWidth() / 2;
        event.getGuiGraphics().drawCenteredString(minecraft.font, message, x, 10, 0xFFFFFF);
    }
}
