package org.mineUGC.core.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.mineUGC.core.message.Messages;

import java.util.List;

public abstract class UgcCommand implements CommandExecutor, TabCompleter {

    protected static final String PERMISSION_PREFIX = "mineugc.";
    protected final Messages messages;

    protected UgcCommand(Messages messages) {
        this.messages = messages;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission(PERMISSION_PREFIX + command.getName())) {
            sender.sendMessage(messages.get("command.no-permission"));
            return true;
        }
        return execute(sender, command, label, args);
    }

    protected abstract boolean execute(CommandSender sender, Command command, String label, String[] args);

    @Override
    public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                               @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission(PERMISSION_PREFIX + command.getName())) {
            return List.of();
        }
        return tabComplete(sender, command, alias, args);
    }

    protected List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return List.of();
    }

    protected boolean requirePlayer(CommandSender sender) {
        if (!(sender instanceof org.bukkit.entity.Player)) {
            sender.sendMessage(messages.get("command.player-only"));
            return false;
        }
        return true;
    }
}
