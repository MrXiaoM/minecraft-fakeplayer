package io.github.hello09x.fakeplayer.core.listener.packet;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import io.github.hello09x.fakeplayer.core.Main;
import io.github.hello09x.fakeplayer.core.manager.FakeplayerManager;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

public class PlayerChatPacketListener extends PacketAdapter {

    public final static PlayerChatPacketListener instance = new PlayerChatPacketListener();
    private final FakeplayerManager manager = FakeplayerManager.instance;

    private PlayerChatPacketListener() {
        super(PacketAdapter.params().plugin(Main.getInstance()).listenerPriority(ListenerPriority.LOWEST).types(
                PacketType.Play.Client.CHAT
        ));
    }

    @Override
    public void onPacketReceiving(@NotNull PacketEvent event) {
        var sender = event.getPlayer();
        if (!manager.isFake(sender)) {
            return;
        }
        event.setCancelled(true);

        var message = event.getPacket().getStrings().read(0);
        Bukkit.getScheduler().runTask(Main.getInstance(), () -> sender.chat(message));
    }
}
