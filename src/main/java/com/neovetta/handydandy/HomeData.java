package com.neovetta.handydandy;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.*;

public class HomeData extends SavedData {

    public record SavedHome(
        ResourceKey<Level> dimension,
        double x, double y, double z,
        float yaw, float pitch
    ) {}

    public record HomeEntry(UUID owner, String name, SavedHome home) {}

    private static final Codec<SavedHome> SAVED_HOME_CODEC = RecordCodecBuilder.create(inst -> inst.group(
        ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension").forGetter(SavedHome::dimension),
        Codec.DOUBLE.fieldOf("x").forGetter(SavedHome::x),
        Codec.DOUBLE.fieldOf("y").forGetter(SavedHome::y),
        Codec.DOUBLE.fieldOf("z").forGetter(SavedHome::z),
        Codec.FLOAT.fieldOf("yaw").forGetter(SavedHome::yaw),
        Codec.FLOAT.fieldOf("pitch").forGetter(SavedHome::pitch)
    ).apply(inst, SavedHome::new));

    private static final Codec<HomeEntry> HOME_ENTRY_CODEC = RecordCodecBuilder.create(inst -> inst.group(
        UUIDUtil.STRING_CODEC.fieldOf("owner").forGetter(HomeEntry::owner),
        Codec.STRING.fieldOf("name").forGetter(HomeEntry::name),
        SAVED_HOME_CODEC.fieldOf("home").forGetter(HomeEntry::home)
    ).apply(inst, HomeEntry::new));

    private static final Codec<HomeData> CODEC =
        HOME_ENTRY_CODEC.listOf().xmap(HomeData::fromList, HomeData::toList);

    public static final SavedDataType<HomeData> TYPE = new SavedDataType<>(
        Identifier.fromNamespaceAndPath(HandyDandy.MOD_ID, "home_data"),
        HomeData::new,
        CODEC,
        null
    );

    private final Map<UUID, Map<String, SavedHome>> homes;

    public HomeData() {
        this.homes = new HashMap<>();
    }

    private static HomeData fromList(List<HomeEntry> entries) {
        HomeData hd = new HomeData();
        for (HomeEntry entry : entries) {
            hd.homes.computeIfAbsent(entry.owner(), k -> new HashMap<>()).put(entry.name(), entry.home());
        }
        return hd;
    }

    private List<HomeEntry> toList() {
        List<HomeEntry> entries = new ArrayList<>();
        homes.forEach((uuid, nameMap) ->
            nameMap.forEach((name, home) -> entries.add(new HomeEntry(uuid, name, home))));
        return entries;
    }

    public static HomeData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public Optional<SavedHome> getHome(UUID player, String name) {
        Map<String, SavedHome> playerHomes = homes.get(player);
        return playerHomes == null ? Optional.empty() : Optional.ofNullable(playerHomes.get(name));
    }

    public List<String> getHomeNames(UUID player) {
        Map<String, SavedHome> playerHomes = homes.get(player);
        return playerHomes == null ? List.of() : new ArrayList<>(playerHomes.keySet());
    }

    public int getHomeCount(UUID player) {
        Map<String, SavedHome> playerHomes = homes.get(player);
        return playerHomes == null ? 0 : playerHomes.size();
    }

    public void setHome(UUID player, String name, SavedHome home) {
        homes.computeIfAbsent(player, k -> new HashMap<>()).put(name, home);
        setDirty();
    }

    public boolean deleteHome(UUID player, String name) {
        Map<String, SavedHome> playerHomes = homes.get(player);
        if (playerHomes == null || !playerHomes.containsKey(name)) return false;
        playerHomes.remove(name);
        if (playerHomes.isEmpty()) homes.remove(player);
        setDirty();
        return true;
    }
}
