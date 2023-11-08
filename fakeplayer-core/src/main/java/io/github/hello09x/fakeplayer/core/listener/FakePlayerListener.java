package io.github.hello09x.fakeplayer.core.listener;

import io.github.hello09x.fakeplayer.core.manager.FakeplayerManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.jetbrains.annotations.NotNull;

public class FakePlayerListener implements Listener {

    public final static FakePlayerListener instance = new FakePlayerListener();

    private final FakeplayerManager manager = FakeplayerManager.instance;

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onCommand(@NotNull PlayerCommandPreprocessEvent event) {
        var player = event.getPlayer();

        var command = event.getMessage();
        if (manager.isFake(player)) {
            if (command.equals("fp") || command.equals("fakeplayer")) {
                event.setCancelled(true);
            }
        }
    }

}
