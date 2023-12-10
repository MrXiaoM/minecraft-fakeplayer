package io.github.hello09x.fakeplayer.v1_19_R3.spi;

import io.github.hello09x.fakeplayer.api.spi.NMSServerLevel;
import lombok.Getter;
import net.minecraft.server.level.ServerLevel;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_19_R3.CraftWorld;
import org.jetbrains.annotations.NotNull;

public class NMSServerLevelImpl implements NMSServerLevel {

    @Getter
    private final ServerLevel handle;

    public NMSServerLevelImpl(@NotNull World world) {
        this.handle = ((CraftWorld) world).getHandle();
    }

}
