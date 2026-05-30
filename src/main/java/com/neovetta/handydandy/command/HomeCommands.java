package com.neovetta.handydandy.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.neovetta.handydandy.HandyDandy;
import com.neovetta.handydandy.HomeData;
import com.neovetta.handydandy.HomeData.SavedHome;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class HomeCommands {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            for (String cmd : List.of("setHome", "sethome")) {
                dispatcher.register(Commands.literal(cmd)
                    .executes(ctx -> setHome(ctx, "home"))
                    .then(Commands.argument("name", StringArgumentType.word())
                        .executes(ctx -> setHome(ctx, StringArgumentType.getString(ctx, "name")))));
            }

            for (String cmd : List.of("tpHome", "tphome")) {
                dispatcher.register(Commands.literal(cmd)
                    .executes(ctx -> tpHome(ctx, "home"))
                    .then(Commands.argument("name", StringArgumentType.word())
                        .executes(ctx -> tpHome(ctx, StringArgumentType.getString(ctx, "name")))));
            }

            for (String cmd : List.of("delHome", "delhome")) {
                dispatcher.register(Commands.literal(cmd)
                    .then(Commands.argument("name", StringArgumentType.word())
                        .executes(ctx -> delHome(ctx, StringArgumentType.getString(ctx, "name")))));
            }

            for (String cmd : List.of("tpList", "tplist")) {
                dispatcher.register(Commands.literal(cmd)
                    .executes(HomeCommands::tpList));
            }
        });
    }

    private static int setHome(CommandContext<CommandSourceStack> ctx, String name) {
        name = name.toLowerCase();
        ServerPlayer player = getPlayer(ctx.getSource());
        if (player == null) return 0;
        if (!HandyDandy.CONFIG.enableTp) {
            player.sendSystemMessage(Component.literal("TP commands have been disabled"));
            return 0;
        }
        if (player.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
            player.sendSystemMessage(Component.literal("Teleporting is dangerous; you'll need to wear a helmet"));
            return 0;
        }
        HomeData data = HomeData.get(ctx.getSource().getServer());
        if (data.getHomeCount(player.getUUID()) >= HandyDandy.CONFIG.maxLocations) {
            player.sendSystemMessage(Component.literal(
                "You already have " + HandyDandy.CONFIG.maxLocations + " locations, please delete some first."));
            return 0;
        }
        ServerLevel level = (ServerLevel) player.level();
        data.setHome(player.getUUID(), name,
            new SavedHome(level.dimension(), player.getX(), player.getY(), player.getZ(),
                player.getYRot(), player.getXRot()));
        player.sendSystemMessage(Component.literal("Your " + name + " has been saved"));
        return 1;
    }

    private static int tpHome(CommandContext<CommandSourceStack> ctx, String name) {
        name = name.toLowerCase();
        ServerPlayer player = getPlayer(ctx.getSource());
        if (player == null) return 0;
        if (!HandyDandy.CONFIG.enableTp) {
            player.sendSystemMessage(Component.literal("TP commands have been disabled"));
            return 0;
        }
        if (player.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
            player.sendSystemMessage(Component.literal("Teleporting is dangerous; you'll need to wear a helmet"));
            return 0;
        }
        HomeData data = HomeData.get(ctx.getSource().getServer());
        var homeOpt = data.getHome(player.getUUID(), name);
        if (homeOpt.isEmpty()) {
            player.sendSystemMessage(Component.literal(
                player.getName().getString() + " has no '" + name + "' location saved yet"));
            return 0;
        }
        SavedHome home = homeOpt.get();
        MinecraftServer server = ctx.getSource().getServer();
        ServerLevel targetLevel = server.getLevel(home.dimension());
        if (targetLevel == null) {
            player.sendSystemMessage(Component.literal("Target dimension not found"));
            return 0;
        }

        player.level().playSound(null, player.blockPosition(),
            SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 0.5f);

        // Teleport mounted vehicle (carries player along)
        Entity vehicle = player.getVehicle();
        if (vehicle != null) {
            vehicle.teleportTo(targetLevel, home.x(), home.y(), home.z(),
                Collections.emptySet(), home.yaw(), home.pitch(), false);
        }

        // Teleport nearby owned tame animals
        AABB searchArea = new AABB(
            player.getX() - 10, player.getY() - 10, player.getZ() - 10,
            player.getX() + 10, player.getY() + 10, player.getZ() + 10);
        UUID playerUuid = player.getUUID();
        List<TamableAnimal> pets = ((ServerLevel) player.level()).getEntities(
            EntityTypeTest.forClass(TamableAnimal.class), searchArea, e -> {
                EntityReference<LivingEntity> ref = e.getOwnerReference();
                return ref != null && playerUuid.equals(ref.getUUID());
            });
        for (TamableAnimal pet : pets) {
            pet.teleportTo(targetLevel, home.x(), home.y(), home.z(),
                Collections.emptySet(), home.yaw(), home.pitch(), false);
        }

        player.teleportTo(targetLevel, home.x(), home.y(), home.z(),
            Collections.emptySet(), home.yaw(), home.pitch(), false);

        targetLevel.playSound(null, player.blockPosition(),
            SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 0.25f);
        return 1;
    }

    private static int delHome(CommandContext<CommandSourceStack> ctx, String name) {
        name = name.toLowerCase();
        ServerPlayer player = getPlayer(ctx.getSource());
        if (player == null) return 0;
        if (!HandyDandy.CONFIG.enableTp) {
            player.sendSystemMessage(Component.literal("TP commands have been disabled"));
            return 0;
        }
        HomeData data = HomeData.get(ctx.getSource().getServer());
        if (!data.deleteHome(player.getUUID(), name)) {
            player.sendSystemMessage(Component.literal("Location '" + name + "' does not exist"));
            return 0;
        }
        player.sendSystemMessage(Component.literal("Location " + name + " has been deleted"));
        return 1;
    }

    private static int tpList(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = getPlayer(ctx.getSource());
        if (player == null) return 0;
        if (!HandyDandy.CONFIG.enableTp) {
            player.sendSystemMessage(Component.literal("TP commands have been disabled"));
            return 0;
        }
        HomeData data = HomeData.get(ctx.getSource().getServer());
        List<String> names = data.getHomeNames(player.getUUID());
        if (names.isEmpty()) {
            player.sendSystemMessage(Component.literal("No locations saved yet"));
        } else {
            player.sendSystemMessage(Component.literal("[" + String.join(", ", names) + "]"));
        }
        return 1;
    }

    private static ServerPlayer getPlayer(CommandSourceStack src) {
        try {
            return src.getPlayerOrException();
        } catch (Exception e) {
            src.sendFailure(Component.literal("Only players can use this command"));
            return null;
        }
    }
}
