package io.github.hello09x.fakeplayer.v1_19_R3.spi;

import io.github.hello09x.fakeplayer.api.spi.NMSNetwork;
import io.github.hello09x.fakeplayer.api.spi.NMSServerGamePacketListener;
import io.github.hello09x.fakeplayer.v1_19_R3.network.EmptyConnection;
import io.github.hello09x.fakeplayer.v1_19_R3.network.EmptyServerGamePacketListenerImpl;
import org.bukkit.Server;
import org.bukkit.craftbukkit.v1_19_R3.CraftServer;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.net.InetAddress;

public class NMSNetworkImpl implements NMSNetwork {

    private final @NotNull EmptyConnection connection;

    private NMSServerGamePacketListener serverGamePacketListener;

    public NMSNetworkImpl(
            @NotNull InetAddress address
    ) {
        this.connection = new EmptyConnection(address);
    }

    @Override
    public @NotNull NMSServerGamePacketListener placeNewPlayer(@NotNull Server server, @NotNull Player player) {
        var handle = ((CraftPlayer) player).getHandle();
        var listener = new EmptyServerGamePacketListenerImpl(
                ((CraftServer) server).getServer(),
                this.connection,
                handle
        );
        handle.connection = listener;
        ((CraftServer) server).getHandle().placeNewPlayer(
                this.connection,
                handle
        );
        this.serverGamePacketListener = listener;
        return listener;
    }

    @NotNull
    @Override
    public NMSServerGamePacketListener getServerGamePacketListener() throws IllegalStateException {
        if (this.serverGamePacketListener == null) {
            throw new IllegalStateException("not initialized");
        }
        return this.serverGamePacketListener;
    }

}
