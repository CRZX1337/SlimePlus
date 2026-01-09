package org.crzx.slimePlus;

import org.bukkit.plugin.java.JavaPlugin;

public final class SlimePlus extends JavaPlugin {

    private static SlimePlus instance;

    private SlimeGUI slimeGUI;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        slimeGUI = new SlimeGUI(this);
        getServer().getPluginManager().registerEvents(new SlimeListener(this), this);
        getServer().getPluginManager().registerEvents(slimeGUI, this);
        getCommand("slimeplus").setExecutor(new SlimeCommand(this));
    }

    public SlimeGUI getSlimeGUI() {
        return slimeGUI;
    }

    public static SlimePlus getInstance() {
        return instance;
    }

    public void reloadPluginConfig() {
        reloadConfig();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
