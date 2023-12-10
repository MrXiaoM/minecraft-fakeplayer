package io.github.hello09x.fakeplayer.v1_19_R3.network;

import io.github.hello09x.fakeplayer.api.spi.NMSServerGamePacketListener;
import io.github.hello09x.fakeplayer.core.Main;
import io.github.hello09x.fakeplayer.core.manager.FakeplayerManager;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftPlayer;
import org.bukkit.plugin.messaging.StandardMessenger;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class EmptyServerGamePacketListenerImpl extends ServerGamePacketListenerImpl implements NMSServerGamePacketListener {

    private final static FakeplayerManager manager = FakeplayerManager.instance;

    public EmptyServerGamePacketListenerImpl(
            @NotNull MinecraftServer server,
            @NotNull Connection connection,
            @NotNull ServerPlayer player
    ) {
        super(server, connection, player);
        Optional.ofNullable(Bukkit.getPlayer(player.getUUID()))
                .map(CraftPlayer.class::cast)
                .ifPresent(p -> p.addChannel(StandardMessenger.validateAndCorrectChannel(BUNGEE_CORD_CHANNEL)));
    }

    @Override
    public void send(Packet<?> packet) {
        if (packet instanceof ClientboundCustomPayloadPacket p) {
            // 接收到自定义的数据包，由于假人没有连接导致 BungeeCord 的插件消息无法正确通过 Proxy 发送
            // 因此将该数据包通过真实的玩家重新发送一份
            this.handleCustomPayloadPacket(p);
        }
    }

    private void handleCustomPayloadPacket(@NotNull ClientboundCustomPayloadPacket packet) {
        var channel = StandardMessenger.validateAndCorrectChannel(packet.getIdentifier().getNamespace() + ":" + packet.getIdentifier().getPath());
        if (!channel.equals(BUNGEE_CORD_CHANNEL)) {
            return;
        }

        var recipient = Bukkit
                .getOnlinePlayers()
                .stream()
                .filter(manager::isNotFake)
                .findAny()
                .orElse(null);
        if (recipient == null) {
            return;
        }

        var message = packet.getData().array();
        recipient.sendPluginMessage(Main.getInstance(), channel, message);
    }

}