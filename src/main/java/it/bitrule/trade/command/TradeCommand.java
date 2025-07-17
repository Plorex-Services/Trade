package it.bitrule.trade.command;

import it.bitrule.trade.MessageAssets;
import it.bitrule.trade.manager.TradeManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;

public final class TradeCommand extends Command {

    public TradeCommand() {
        super("trade", "Trade with another player", "/trade <player> | /trade accept <player>", new LinkedList<>());
    }

    /**
     * Executes the command, returning its success
     *
     * @param sender       Source object which is executing this command
     * @param commandLabel The alias of the command used
     * @param args         All arguments passed to the command, split via ' '
     * @return true if the command was successful, otherwise false
     */
    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(Component.join(
                    JoinConfiguration.newlines(),
                    MessageAssets.TRADE_COMMAND_USAGE.buildMany()
            ));
            return true;
        }

        if (args[0].equals("accept")) {
            TradeManager.getInstance().accept((Player) sender, args.length > 1 ? args[1] : "");
        } else {
            TradeManager.getInstance().request((Player) sender, args[0]);
        }

        return false;
    }
}