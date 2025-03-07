package io.github.hello09x.fakeplayer.core.repository.model;

import io.github.hello09x.fakeplayer.core.command.Permission;
import io.github.hello09x.fakeplayer.core.manager.FakeplayerManager;
import net.kyori.adventure.translation.Translatable;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * @param key            配置项 key
 * @param translationKey 翻译 key
 * @param defaultValue   默认值
 * @param options        可选值
 * @param converter      转换器
 * @param accessor       访问器, 访问或者设置假人当前配置
 */
public record Config<T>(

        @NotNull
        String key,

        @NotNull
        String translationKey,

        @NotNull Class<T> type,

        @NotNull
        T defaultValue,

        @NotNull
        List<String> options,

        @Nullable
        String permission,

        @NotNull
        Function<String, T> converter,

        @UnknownNullability
        Accessor<T> accessor

) implements Translatable {

    private static final Map<String, Config<?>> values = new HashMap<>();

    public static Config<Boolean> collidable = build(
            "collidable",
            "fakeplayer.config.collidable",
            Boolean.class,
            true,
            List.of("true", "false"),
            null,
            Boolean::valueOf,
            new Accessor<>(LivingEntity::isCollidable, LivingEntity::setCollidable)
    );

    /**
     * 无敌
     */
    public static Config<Boolean> invulnerable = build(
            "invulnerable",
            "fakeplayer.config.invulnerable",
            Boolean.class,
            true,
            List.of("true", "false"),
            null,
            Boolean::valueOf,
            new Accessor<>(LivingEntity::isInvulnerable, LivingEntity::setInvulnerable)
    );

    /**
     * 看向实体
     */
    public static Config<Boolean> look_at_entity = build(
            "look_at_entity",
            "fakeplayer.config.look_at_entity",
            Boolean.class,
            true,
            List.of("true", "false"),
            null,
            Boolean::valueOf,
            null
    );

    /**
     * 拾取物品
     */
    public static Config<Boolean> pickup_items = build(
            "pickup_items",
            "fakeplayer.config.pickup_items",
            Boolean.class,
            true,
            List.of("true", "false"),
            null,
            Boolean::valueOf,
            new Accessor<>(LivingEntity::getCanPickupItems, LivingEntity::setCanPickupItems)
    );

    /**
     * 使用皮肤
     */
    public static Config<Boolean> skin = build(
            "skin",
            "fakeplayer.config.skin",
            Boolean.class,
            true,
            List.of("true", "false"),
            null,
            Boolean::valueOf,
            null
    );

    /**
     * 自动补货
     */
    public static Config<Boolean> replenish = build(
            "replenish",
            "fakeplayer.config.replenish",
            Boolean.class,
            false,
            List.of("true", "false"),
            Permission.replenish,
            Boolean::valueOf,
            new Accessor<>(FakeplayerManager.instance::isReplenish, FakeplayerManager.instance::setReplenish)
    );

    @SuppressWarnings("unchecked")
    public static @NotNull <T> Config<T> valueOf(@NotNull String name) {
        return (Config<T>) valueOfOpt(name).orElseThrow(() -> new IllegalArgumentException("No config named: " + name));
    }

    @SuppressWarnings("unchecked")
    public static @NotNull <T> Optional<Config<T>> valueOfOpt(@NotNull String name) {
        return Optional.ofNullable((Config<T>) values.get(name));
    }

    public static @NotNull Config<?>[] values() {
        return values.values().toArray(Config[]::new);
    }

    private static <T> Config<T> build(
            @NotNull String name,
            @NotNull String translationKey,
            @NotNull Class<T> type,
            @NotNull T defaultValue,
            @NotNull List<String> options,
            @Nullable String permission,
            @NotNull Function<String, T> converter,
            @Nullable Accessor<T> accessor
    ) {
        var config = new Config<>(name, translationKey, type, defaultValue, options, permission, converter, accessor);
        values.put(name, config);
        return config;
    }

    public boolean hasPermission(@NotNull CommandSender player) {
        return this.permission == null || player.hasPermission(this.permission);
    }

    @Override
    public @NotNull String translationKey() {
        return this.translationKey;
    }

    public boolean hasAccessor() {
        return this.accessor != null;
    }

    public record Accessor<T>(
            @NotNull Function<Player, T> getter,
            @NotNull BiConsumer<Player, T> setter
    ) {

    }


}
