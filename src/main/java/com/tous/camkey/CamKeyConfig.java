package com.tous.camkey;

import net.neoforged.neoforge.common.ModConfigSpec;

import com.tous.camkey.session.CamKeySession;

/**
 * Client-side settings, saved to {@code config/camkey-client.toml} in the game directory.
 */
public final class CamKeyConfig {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.DoubleValue DEFAULT_PLAYBACK_SECONDS = BUILDER
            .comment("How long, in seconds, a sequence takes to play when started with a playback keybind.",
                    "/camkey play <name> <seconds> always uses the duration you type instead.")
            .defineInRange("defaultPlaybackSeconds", 10.0, 0.5, CamKeySession.MAX_PLAYBACK_SECONDS);

    static final ModConfigSpec SPEC = BUILDER.build();

    private CamKeyConfig() {
    }
}
