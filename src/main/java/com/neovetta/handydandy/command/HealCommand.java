package com.neovetta.handydandy.command;

import com.mojang.brigadier.context.CommandContext;
import com.neovetta.handydandy.HandyDandy;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

public class HealCommand {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            dispatcher.register(Commands.literal("heal").executes(HealCommand::execute))
        );
    }

    private static int execute(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        ServerPlayer player;
        try {
            player = src.getPlayerOrException();
        } catch (Exception e) {
            src.sendFailure(Component.literal("Only players can use this command"));
            return 0;
        }

        if (!HandyDandy.CONFIG.enableHeal) {
            player.sendSystemMessage(Component.literal("Heal command has been disabled"));
            return 0;
        }

        if (HandyDandy.CONFIG.healXpType.equals("fixed")) {
            if (!healFixed(player, HandyDandy.CONFIG.healXpCost)) return 0;
        } else {
            healVariable(player);
        }

        player.level().playSound(null, player.blockPosition(), SoundEvents.VILLAGER_AMBIENT, SoundSource.PLAYERS, 1.0f, 0.25f);
        return 1;
    }

    private static boolean healFixed(ServerPlayer player, float cost) {
        if (player.experienceProgress >= cost) {
            player.experienceProgress -= cost;
            player.setHealth(player.getMaxHealth());
        } else if (player.experienceLevel > 0) {
            float remainingProgress = player.experienceProgress;
            player.giveExperienceLevels(-1);
            float finalExp = 1.0f - (cost - remainingProgress);
            player.experienceProgress = Math.max(0f, finalExp);
            player.setHealth(player.getMaxHealth());
        } else {
            player.sendSystemMessage(Component.literal("Not enough XP. Cost is: " + cost));
            return false;
        }
        player.sendSystemMessage(Component.literal("Your health has been restored!"));
        return true;
    }

    private static void healVariable(ServerPlayer player) {
        float maxHealth = player.getMaxHealth();
        float missing = maxHealth - player.getHealth();
        float healCost = missing / maxHealth;
        String message;

        if (player.experienceProgress >= healCost) {
            player.experienceProgress -= healCost;
            player.setHealth(maxHealth);
            message = String.format("Full heal for %.2f xp", healCost);
        } else if (player.experienceLevel > 0) {
            float remainingProgress = player.experienceProgress;
            player.giveExperienceLevels(-1);
            float finalExp = 1.0f - (healCost - remainingProgress);
            player.experienceProgress = Math.max(0f, finalExp);
            player.setHealth(maxHealth);
            message = String.format("Full heal for %.2f xp", healCost);
        } else {
            // partial heal: trade remaining XP bar for health
            float healAmtAvailable = maxHealth * player.experienceProgress;
            float healAmtRounded = Math.round(healAmtAvailable);
            float healAmtFinal = (healAmtRounded > healAmtAvailable) ? healAmtRounded - 0.5f : healAmtRounded;
            if (healAmtFinal <= 0) {
                message = "Not healed; you need some xp first";
            } else {
                player.setHealth(Math.min(maxHealth, player.getHealth() + healAmtFinal));
                player.experienceProgress = 0f;
                message = "Restored " + healAmtFinal + " hearts for remaining xp";
            }
        }

        player.sendSystemMessage(Component.literal(message));
    }
}
