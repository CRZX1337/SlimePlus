package org.crzx.slimePlus;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SlimeCommand implements CommandExecutor {

    private final SlimePlus plugin;

    public SlimeCommand(SlimePlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.isOp()) {
            player.sendMessage("§cYou do not have permission to use this command.");
            return true;
        }

        // Open Admin GUI
        plugin.getSlimeGUI().open(player);
        return true;
    }
}
