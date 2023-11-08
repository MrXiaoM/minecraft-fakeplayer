package io.github.hello09x.fakeplayer.v1_20_R1.spi;

import com.google.common.collect.Iterables;
import io.github.hello09x.fakeplayer.api.Reflections;
import io.github.hello09x.fakeplayer.api.constant.ConstantPool;
import io.github.hello09x.fakeplayer.api.spi.NMSServerPlayer;
import io.github.hello09x.fakeplayer.core.Main;
import io.github.hello09x.fakeplayer.core.util.Worlds;
import io.github.hello09x.fakeplayer.v1_20_R1.network.EmptyAdvancements;
import lombok.Getter;
import lombok.SneakyThrows;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ServerboundClientInformationPacket;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.biome.BiomeManager;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.Optional;

public class NMSServerPlayerImpl implements NMSServerPlayer {

    private final static Field ServerPlayer$advancements = Reflections.getFirstFieldByType(
            ServerPlayer.class,
            PlayerAdvancements.class,
            false
    );

    @Getter
    private final ServerPlayer handle;

    @Getter
    private final CraftPlayer player;

    public NMSServerPlayerImpl(@NotNull Player player) {
        this.player = ((CraftPlayer) player);
        this.handle = ((CraftPlayer) player).getHandle();
    }

    @Override
    public double getX() {
        return handle.getX();
    }

    @Override
    public double getY() {
        return handle.getY();
    }

    @Override
    public double getZ() {
        return handle.getZ();
    }

    @Override
    public void setXo(double xo) {
        handle.xo = xo;
    }

    @Override
    public void setYo(double yo) {
        handle.yo = yo;
    }

    @Override
    public void setZo(double zo) {
        handle.zo = zo;
    }

    @Override
    public void doTick() {
        handle.doTick();
        ;
    }

    @Override
    public void absMoveTo(double x, double y, double z, float yRot, float xRot) {
        handle.absMoveTo(x, y, z, yRot, xRot);
    }

    @Override
    public float getYRot() {
        return handle.getYRot();
    }

    @Override
    public void setYRot(float yRot) {
        handle.setYRot(yRot);
    }

    @Override
    public float getXRot() {
        return handle.getXRot();
    }

    @Override
    public void setXRot(float xRot) {
        handle.setXRot(xRot);
    }

    @Override
    public void setZza(float zza) {
        handle.zza = zza;
    }

    @Override
    public void setXxa(float xxa) {
        handle.xxa = xxa;
    }

    @Override
    public boolean startRiding(@NotNull Entity entity, boolean force) {
        return handle.startRiding(new NMSEntityImpl(entity).getHandle(), force);
    }

    @Override
    public void stopRiding() {
        handle.stopRiding();
    }


    @Override
    public int getTickCount() {
        return handle.tickCount;
    }

    @Override
    public void drop(boolean allStack) {
        handle.drop(allStack);
    }

    @Override
    public void resetLastActionTime() {
        handle.resetLastActionTime();
    }

    @Override
    public boolean onGround() {
        return handle.onGround();
    }

    @Override
    public void jumpFromGround() {
        handle.jumpFromGround();
    }

    @Override
    public void setJumping(boolean jumping) {
        handle.setJumping(jumping);
    }

    @Override
    public boolean isUsingItem() {
        return handle.isUsingItem();
    }

    @Override
    public void unpersistAdvancements(@NotNull Plugin plugin) {
        if (ServerPlayer$advancements == null) {
            return;
        }

        var server = new NMSServerImpl(Bukkit.getServer()).getHandle();
        try {
            ServerPlayer$advancements.set(
                    handle,
                    new EmptyAdvancements(
                            server.getFixerUpper(),
                            server.getPlayerList(),
                            server.getAdvancements(),
                            plugin.getDataFolder().getParentFile().toPath(),
                            handle
                    )
            );
        } catch (IllegalAccessException ignored) {
        }
    }

    @Override
    public void copyTexture(@NotNull Player from) {
        var source = new NMSServerPlayerImpl(from).getHandle();
        Optional.of(source)
                .map(h -> h.getGameProfile().getProperties().get("textures"))
                .map(textures -> Iterables.getFirst(textures, null))
                .ifPresent(texture -> handle.getGameProfile().getProperties().put("textures", texture));
    }


    @Override
    public void drop(int slot, boolean flag, boolean flag1) {
        var inventory = handle.getInventory();
        handle.drop(inventory.removeItem(slot, inventory.getItem(slot).getCount()), flag, flag1);
    }

    @Override
    public void setPlayBefore() {
        player.readExtraData(new CompoundTag());
    }

    @Override
    public void configClientOptions() {
        var option = new ServerboundClientInformationPacket(
                "en_us",
                Bukkit.getViewDistance(),
                ChatVisiblity.FULL,
                false,
                ConstantPool.PLAYER_DISPLAYED_SKIN_PARTS_ALL_ENABLED,
                HumanoidArm.RIGHT,
                false,
                true
        );
        handle.updateOptions(option);
    }

    @SneakyThrows
    @Override
    public void bewatch(@NotNull Player player) {
        var a = ((CraftPlayer) player).getHandle();
        var b = getHandle();

        var aPlayer = a.getBukkitEntity();
        var bPlayer = b.getBukkitEntity();

        var aConnection = a.connection;
        var bConnection = b.connection;

        a.connection = bConnection;
        b.connection = aConnection;

        aConnection.player = b;
        bConnection.player = a;

        aPlayer.setHandle(b);
        bPlayer.setHandle(a);

        Reflections.getFirstFieldByType(net.minecraft.world.entity.Entity.class, CraftEntity.class, false).set(a, bPlayer);
        Reflections.getFirstFieldByType(net.minecraft.world.entity.Entity.class, CraftEntity.class, false).set(b, aPlayer);

        var level = (ServerLevel) b.level();
        var server = level.getServer();
        var gamerule = level.getGameRules();
        var playerList = server.getPlayerList();
        aConnection.send(new ClientboundLoginPacket(
                b.getId(),
                level.getLevelData().isHardcore(),
                b.gameMode.getGameModeForPlayer(),
                b.gameMode.getPreviousGameModeForPlayer(),
                level.getServer().levelKeys(),
                (RegistryAccess.Frozen) Reflections.getFirstFieldByType(PlayerList.class, RegistryAccess.Frozen.class, false).get(playerList),
                level.dimensionTypeId(),
                level.dimension(),
                BiomeManager.obfuscateSeed(level.getSeed()),
                playerList.maxPlayers,
                level.spigotConfig.viewDistance,
                level.spigotConfig.simulationDistance,
                gamerule.getBoolean(GameRules.RULE_DO_IMMEDIATE_RESPAWN),
                !gamerule.getBoolean(GameRules.RULE_REDUCEDDEBUGINFO),
                level.isDebug(),
                level.isFlat(),
                b.getLastDeathLocation(),
                b.getPortalCooldown()
        ));

        var pos = handle.getBukkitEntity().getLocation().clone();
        Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
            var world = Worlds.getOtherWorld(pos.getWorld());
            if (world == null) {
                return;
            }
            handle.getBukkitEntity().teleport(world.getSpawnLocation());

            Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                handle.getBukkitEntity().teleport(pos);
            });
        });

        // TODO 重新放置实体

    }

}
