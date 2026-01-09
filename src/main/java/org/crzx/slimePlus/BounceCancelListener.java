package org.crzx.slimePlus;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public class BounceCancelListener implements Listener {

    private final SlimePlus plugin;

    public BounceCancelListener(SlimePlus plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSlimeBounce(EntityDamageEvent event) {
        // We handle fall damage ourselves or let vanilla handle it
        // But specifically for the "jump" effect:
        // In Spigot, there isn't a direct "EntityBounceEvent" for players on slimes.
        // It's handled by the client/server physics.
        // To prevent the bounce entirely and make it like stone, we need to handle the physics.
    }
}
