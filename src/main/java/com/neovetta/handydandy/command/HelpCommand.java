package com.neovetta.handydandy.command;

import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class HelpCommand {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            dispatcher.register(Commands.literal("handydandy")
                .executes(HelpCommand::execute)
                .then(Commands.literal("help").executes(HelpCommand::execute)))
        );
    }

    private static int execute(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();

        src.sendSuccess(() -> Component.literal("━━━━━━━━━━ HandyDandy ━━━━━━━━━━")
            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);

        header(src, "Commands");
        line(src, "/heal", "Restore health at XP cost");
        line(src, "/setHome [name]", "Save current location (default: home)");
        line(src, "/tpHome [name]", "Teleport to saved location (default: home)");
        line(src, "/delHome <name>", "Delete a saved location");
        line(src, "/tpList", "List all saved locations");

        header(src, "Item Abilities");
        line(src, "Iron/Diamond Sword  (right-click)", "Night vision while held");
        line(src, "Iron/Diamond Axe + sneak (right-click)", "Launch shulker bullet");
        line(src, "Swim Fins (crafted)", "Depth strider + respiration");
        line(src, "Jump Boots (crafted)", "Feather falling + protection");

        header(src, "Passive Features");
        line(src, "Axe + log", "Fell entire tree");
        line(src, "Death", "Auto-saves a 'death' waypoint");
        line(src, "Join", "Welcome message + arrival announcement");

        header(src, "Custom Recipes");
        line(src, "Diamond", "gunpowder / gravel / sand / iron → diamond");
        line(src, "Saddle", "leather / string / iron → saddle");
        line(src, "Name Tag", "string / paper → name tag");
        line(src, "Copper Pickaxe", "copper / diamond / iron bars → golden pickaxe");
        line(src, "Swim Fins", "lily pad / leather boots / emerald → enchanted boots");
        line(src, "Jump Boots", "leather / leather boots / emerald → enchanted boots");
        line(src, "Spawn Eggs", "diamond / meat / egg → cow / pig / spider egg");
        line(src, "Furnace Swords", "smelt iron/diamond sword → sharpened sword");

        return 1;
    }

    private static void header(CommandSourceStack src, String title) {
        src.sendSuccess(() -> Component.literal(title)
            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
    }

    private static void line(CommandSourceStack src, String cmd, String desc) {
        MutableComponent text = Component.literal("  " + cmd).withStyle(ChatFormatting.YELLOW)
            .append(Component.literal("  »  ").withStyle(ChatFormatting.DARK_GRAY))
            .append(Component.literal(desc).withStyle(ChatFormatting.GRAY));
        src.sendSuccess(() -> text, false);
    }
}
