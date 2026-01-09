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
        // Added a check to ensure we only trigger if they are actually falling significantly
        double maxHeight = plugin.getConfig().getDouble("max-bounce-height", 256.0);

        // --- STONE-LIKE BEHAVIOR WHEN MAX HEIGHT IS 0 ---
        if (maxHeight <= 0) {
            Block blockBelow = player.getLocation().getBlock().getRelative(BlockFace.DOWN);
            if (blockBelow.getType() == Material.SLIME_BLOCK) {
                // 1. Prevent Bounce (Cancel downward velocity when hitting the block)
                if (player.getVelocity().getY() < -0.1) {
                    Vector velocity = player.getVelocity();
                    velocity.setY(0);
                    player.setVelocity(velocity);
                    player.setFallDistance(0);
                }
                
                // 2. Prevent "Jump Suction" (Slimes usually limit jump height)
                // If the player just started moving up (jumped), we give them a boost to match stone jumps
                if (event.getTo().getY() > event.getFrom().getY() && player.getVelocity().getY() > 0) {
                    // Normal jump velocity is ~0.42. Slime reduces it. We push it back up.
                    if (player.getVelocity().getY() < 0.4) {
                        Vector velocity = player.getVelocity();
                        velocity.setY(0.42);
                        player.setVelocity(velocity);
                    }
                }
            }
            return;
        }

        // --- CUSTOM BOUNCE LOGIC ---
        if (player.getVelocity().getY() < -0.1) {
            Block blockBelow = player.getLocation().getBlock().getRelative(BlockFace.DOWN);
            
            if (blockBelow.getType() == Material.SLIME_BLOCK) {
                double multiplier = plugin.getConfig().getDouble("bounce-multiplier", 1.0);
                
                boolean boostEnabled = plugin.getConfig().getBoolean("boost-enabled", false);
                double boostPower = plugin.getConfig().getDouble("boost-power", 0.5);
                boolean removeLimit = plugin.getConfig().getBoolean("remove-vanilla-limit", true);

                float fallDistance = player.getFallDistance();
                if (fallDistance < 0.1f) fallDistance = 0.5f; 
                
                if (!removeLimit && fallDistance > 44.0f) {
                    fallDistance = 44.0f;
                }

                double newY = Math.sqrt(fallDistance * 0.2) * multiplier;
                
                if (boostEnabled) {
                    newY += boostPower;
                }

                double maxVelocity = Math.sqrt(maxHeight * 0.2);
                if (newY > maxVelocity) {
                    newY = maxVelocity;
                }

                Vector velocity = player.getVelocity();
                velocity.setY(newY);
                player.setVelocity(velocity);
                player.setFallDistance(0);
            }
        }
    }
}
