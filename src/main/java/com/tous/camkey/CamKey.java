package com.tous.camkey;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(CamKey.MODID)
public class CamKey {

    public static final String MODID = "camkey";

    public CamKey(ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.CLIENT, CamKeyConfig.SPEC);
    }
}
