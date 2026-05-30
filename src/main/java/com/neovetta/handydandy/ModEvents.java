package com.neovetta.handydandy;

import com.neovetta.handydandy.HomeData.SavedHome;
import net.minecraft.server.MinecraftServer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;

import java.util.List;
import java.util.Set;

public class ModEvents {

    private static final Set<Block> LOG_BLOCKS = Set.of(
        Blocks.OAK_LOG, Blocks.BIRCH_LOG, Blocks.SPRUCE_LOG, Blocks.JUNGLE_LOG,
        Blocks.ACACIA_LOG, Blocks.DARK_OAK_LOG, Blocks.CHERRY_LOG, Blocks.MANGROVE_LOG
    );

    private static final Set<Block> LEAF_BLOCKS = Set.of(
        Blocks.OAK_LEAVES, Blocks.BIRCH_LEAVES, Blocks.SPRUCE_LEAVES, Blocks.JUNGLE_LEAVES,
        Blocks.ACACIA_LEAVES, Blocks.DARK_OAK_LEAVES, Blocks.CHERRY_LEAVES, Blocks.MANGROVE_LEAVES
    );

    public static void register() {
        registerJoin();
        registerDeath();
        registerItemUse();
        registerBlockBreak();
        registerDamage();
    }

    private static final List<ResourceKey<Recipe<?>>> MOD_RECIPES = List.of(
        recipeKey("copper_pickaxe"),
        recipeKey("cow_spawn_egg"),
        recipeKey("custom_diamond"),
        recipeKey("custom_nametag"),
        recipeKey("custom_saddle"),
        recipeKey("diamond_sword_smelt"),
        recipeKey("iron_sword_smelt"),
        recipeKey("jump_boots"),
        recipeKey("pig_spawn_egg"),
        recipeKey("spider_spawn_egg"),
        recipeKey("swim_fins")
    );

    private static ResourceKey<Recipe<?>> recipeKey(String name) {
        return ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath(HandyDandy.MOD_ID, name));
    }

    private static void registerJoin() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.player;
            player.awardRecipesByKey(MOD_RECIPES);
            player.sendSystemMessage(
                Component.literal("Welcome " + player.getName().getString() + "! HandyDandy mod is running.")
                    .withStyle(ChatFormatting.AQUA));
            server.getPlayerList().getPlayers().forEach(other -> {
                if (!other.getUUID().equals(player.getUUID())) {
                    other.sendSystemMessage(
                        Component.literal(player.getName().getString() + " Just Joined!"));
                }
            });
        });
    }

    private static void registerDeath() {
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (entity instanceof ServerPlayer player) {
                ServerLevel serverLevel = (ServerLevel) player.level();
                MinecraftServer server = serverLevel.getServer();
                HomeData data = HomeData.get(server);
                data.setHome(player.getUUID(), "death", new SavedHome(
                    serverLevel.dimension(),
                    player.getX(), player.getY(), player.getZ(),
                    player.getYRot(), player.getXRot()));
            }
        });
    }

    private static void registerItemUse() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (world.isClientSide()) return InteractionResult.PASS;
            var stack = player.getItemInHand(hand);
            var item = stack.getItem();

            if (item == Items.IRON_SWORD || item == Items.DIAMOND_SWORD) {
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 6000, 0, false, false));
                return InteractionResult.SUCCESS;
            }

            if ((item == Items.IRON_AXE || item == Items.DIAMOND_AXE) && player.isShiftKeyDown()) {
                if (player instanceof ServerPlayer serverPlayer) {
                    var eyePos = player.getEyePosition();
                    var look = player.getLookAngle();
                    ShulkerBullet bullet = new ShulkerBullet(world, serverPlayer, null, null);
                    bullet.setPos(eyePos.x + look.x * 1.5, eyePos.y + look.y * 1.5, eyePos.z + look.z * 1.5);
                    bullet.setDeltaMovement(look.x * 1.5, look.y * 1.5, look.z * 1.5);
                    world.addFreshEntity(bullet);
                    stack.hurtAndBreak(5, serverPlayer, EquipmentSlot.MAINHAND);
                }
                return InteractionResult.SUCCESS;
            }

            return InteractionResult.PASS;
        });
    }

    private static void registerDamage() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, damageSource, amount) -> {
            if (!(entity instanceof ServerPlayer player)) return true;
            if (!damageSource.is(DamageTypeTags.IS_FALL)) return true;
            ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
            if (boots.isEmpty()) return true;
            ItemEnchantments enchantments = boots.get(DataComponents.ENCHANTMENTS);
            if (enchantments == null || enchantments.isEmpty()) return true;
            // If the boots have our custom jumping enchantment, they're Jump Boots — cancel fall damage
            return enchantments.keySet().stream()
                    .noneMatch(h -> h.is(net.minecraft.resources.Identifier.fromNamespaceAndPath("handydandy", "jumping")));
        });
    }

    private static void registerBlockBreak() {
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            var item = player.getMainHandItem().getItem();
            if (item != Items.IRON_AXE && item != Items.DIAMOND_AXE) return true;
            if (!LOG_BLOCKS.contains(state.getBlock())) return true;
            if (world instanceof ServerLevel serverLevel) {
                breakTree(serverLevel, pos, state.getBlock(), player);
            }
            return false;
        });
    }

    private static void breakTree(ServerLevel world, BlockPos startPos, Block trunkBlock,
                                   net.minecraft.world.entity.player.Player player) {
        world.destroyBlock(startPos, true, player);
        int height = 1;
        while (world.getBlockState(startPos.above(height)).getBlock() == trunkBlock) {
            world.destroyBlock(startPos.above(height), true, player);
            height++;
        }
        for (int x = -4; x <= 4; x++) {
            for (int y = 0; y <= height; y++) {
                for (int z = -4; z <= 4; z++) {
                    BlockPos sweepPos = startPos.offset(x, y, z);
                    Block block = world.getBlockState(sweepPos).getBlock();
                    if (LOG_BLOCKS.contains(block) || LEAF_BLOCKS.contains(block)) {
                        world.destroyBlock(sweepPos, true, player);
                    }
                }
            }
        }
    }
}
