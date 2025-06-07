package com.jouriroosjen.hardcoreSMPPlugin;

import com.jouriroosjen.hardcoreSMPPlugin.listeners.PlayerDeathListener;
import org.bukkit.plugin.java.JavaPlugin;

public final class HardcoreSMPPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(this), this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
