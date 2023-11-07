package io.github.hello09x.fakeplayer.core.listener;

import com.google.common.base.Throwables;
import io.github.hello09x.bedrock.i18n.I18n;
import io.github.hello09x.fakeplayer.core.Main;
import io.github.hello09x.fakeplayer.core.config.FakeplayerConfig;
import io.github.hello09x.fakeplayer.core.manager.FakeplayerManager;
import io.github.hello09x.fakeplayer.core.repository.UsedIdRepository;
import io.github.hello09x.fakeplayer.core.util.InternalAddressGenerator;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

import static net.kyori.adventure.text.Component.*;
import static net.kyori.adventure.text.format.NamedTextColor.GRAY;

public class PlayerListeners implements Listener {

    public final static PlayerListeners instance = new PlayerListeners();
    private final static Logger log = Main.getInstance().getLogger();
    private final FakeplayerManager manager = FakeplayerManager.instance;

    private final UsedIdRepository usedIdRepository = UsedIdRepository.instance;
    private final FakeplayerConfig config = FakeplayerConfig.instance;
    private final I18n i18n = Main.i18n();

    /**
     * 拒绝假人用过的 ID 上线
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onPreLogin(@NotNull AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            return;
        }

        if (InternalAddressGenerator.canBeGenerate(event.getAddress())) {
            // 假人模拟登陆的 ip 地址
            return;
        }

        if (usedIdRepository.contains(event.getUniqueId())) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, textOfChildren(
                    i18n.translate("fakeplayer.listener.prelogin.deny-used-uuid"),
                    newline(),
                    newline(),
                    text("<<---- fakeplayer ---->>", GRAY)
            ));
            log.info("玩家 %s '%s' 被假人使用过, 拒绝连接服务器".formatted(event.getName(), event.getUniqueId()));
        }
    }

    /**
     * 由于查看假人背包时看不到盔甲栏, 且在将物品从假人背包移动到玩家背包时候有概率会将物品放置到假人的盔甲栏而找不到了, 因此禁止玩家操作假人背包
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onClickFakePlayerInventory(@NotNull InventoryClickEvent event) {
        var top = event.getView().getTopInventory();
        if (top.getType() == InventoryType.PLAYER && (top.getHolder() instanceof Player player && manager.isFake(player))) {
            if (event.getView().getTitle().startsWith("*")) {
                event.setCancelled(true);
            }
        }
    }

//    /**
//     * 死亡退出游戏
//     */
//    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
//    public void onDead(@NotNull PlayerDeathEvent event) {
//        var player = event.getPlayer();
//        if (!manager.isFake(player)) {
//            return;
//        }
//
//        // 有一些跨服同步插件会退出时同步生命值, 假人重新生成的时候同步为 0
//        // 因此在死亡时将生命值设置恢复满血先
//        Optional.ofNullable(player.getAttribute(Attribute.GENERIC_MAX_HEALTH))
//                .map(AttributeInstance::getValue)
//                .ifPresent(player::setHealth);
//        event.setCancelled(true);
//        manager.remove(event.getPlayer().getName(), event.deathMessage());
//    }

    /**
     * 退出游戏掉落背包
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onQuit(@NotNull PlayerQuitEvent event) {
        var player = event.getPlayer();
        if (!manager.isFake(player)) {
            return;
        }

        try {
            manager.dispatchCommands(player, config.getDestroyCommands());
        } catch (Throwable e) {
            log.warning("执行 destroy-commands 时发生错误: \n" + Throwables.getStackTraceAsString(e));
        } finally {
            manager.cleanup(player);
        }
    }

    /**
     * 右键假人打开其背包
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void invsee(@NotNull PlayerInteractAtEntityEvent event) {
        if (!(event.getRightClicked() instanceof Player target) || !manager.isFake(target)) {
            return;
        }

        manager.openInventory(event.getPlayer(), target);
    }

}
