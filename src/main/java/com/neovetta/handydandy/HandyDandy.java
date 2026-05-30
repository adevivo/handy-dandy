package com.neovetta.handydandy;

import com.neovetta.handydandy.command.HealCommand;
import com.neovetta.handydandy.command.HelpCommand;
import com.neovetta.handydandy.command.HomeCommands;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HandyDandy implements ModInitializer {
    public static final String MOD_ID = "handydandy";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static ModConfig CONFIG;

    @Override
    public void onInitialize() {
        CONFIG = ModConfig.load();
        HelpCommand.register();
        HealCommand.register();
        HomeCommands.register();
        ModEvents.register();
        LOGGER.info("HandyDandy initialized");
    }
}
