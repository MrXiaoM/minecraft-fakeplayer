package io.github.hello09x.fakeplayer.core.command;

import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import dev.jorel.commandapi.executors.CommandArguments;
import io.github.hello09x.fakeplayer.core.Main;
import lombok.NoArgsConstructor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@NoArgsConstructor
public class PossesCommand extends AbstractCommand {

    public final static PossesCommand instance = new PossesCommand();

    public void posses(@NotNull Player sender, @NotNull CommandArguments args) throws WrapperCommandSyntaxException {
        var target = super.getTarget(sender, args);
        Main.getVersionSupport().player(target).bewatch(sender);
    }


}
