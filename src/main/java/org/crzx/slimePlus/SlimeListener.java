package org.crzx.slimePlus;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

public class SlimeListener implements Listener {

    private final SlimePlus plugin;

    public SlimeListener(SlimePlus plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        
        // Check if player is falling and about to hit a slime block
        if (player.getVelocity().getY() < 0) {
            Block blockBelow = player.getLocation().getBlock().getRelative(BlockFace.DOWN);
            
            if (blockBelow.getType() == Material.SLIME_BLOCK) {
                // Check if we should override vanilla behavior
                double multiplier = plugin.getConfig().getDouble("bounce-multiplier", 1.0);
                double maxHeight = plugin.getConfig().getDouble("max-bounce-height", 256.0);
                
                // If max height is 0, disable plugin mechanics for slime blocks
                if (maxHeight <= 0) return;

                boolean boostEnabled = plugin.getConfig().getBoolean("boost-enabled", false);
                double boostPower = plugin.getConfig().getDouble("boost-power", 0.5);
                boolean removeLimit = plugin.getConfig().getBoolean("remove-vanilla-limit", true);

                // Use fall distance for calculation, but ensure it's at least something if they just jumped
                float fallDistance = player.getFallDistance();
                if (fallDistance < 0.1f) fallDistance = 0.5f; 
                
                // Vanilla limit is around 44 blocks. If removeLimit is false, we cap it.
                if (!removeLimit && fallDistance > 44.0f) {
                    fallDistance = 44.0f;
                }

                // IMPROVED PHYSICS CALCULATION
                // In Minecraft, velocity is applied every tick. 
                // To reach height H, the initial velocity V should be approximately sqrt(2 * gravity * H)
                // Minecraft gravity is ~0.08 per tick, but air resistance is 0.98.
                // A better approximation for "fun" physics:
                double newY = Math.sqrt(fallDistance * 0.2) * multiplier;
                
                // If boost is enabled, add a significant kick
                if (boostEnabled) {
                    newY += boostPower;
                }

                // Cap at max height velocity
                double maxVelocity = Math.sqrt(maxHeight * 0.2);
                if (newY > maxVelocity) {
                    newY = maxVelocity;
                }

                Vector velocity = player.getVelocity();
                velocity.setY(newY);
                player.setVelocity(velocity);
                
                // Reset fall distance and handle potential damage negation
                player.setFallDistance(0);
            }
        }
    }
}
