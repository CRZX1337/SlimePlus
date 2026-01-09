package org.crzx.slimePlus;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class SlimeListener implements Listener {

    private final SlimePlus plugin;
    private final Set<UUID> boostedPlayers = new HashSet<>();

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
            // Check the block the player is STANDING ON (more precise than DOWN)
            Block standingOn = player.getLocation().clone().subtract(0, 0.1, 0).getBlock();
            
            if (standingOn.getType() == Material.SLIME_BLOCK) {
                // 1. Fix Walking Speed without changing FOV
                // Instead of PotionEffects or setVelocity (which cause jitter/FOV zoom),
                // we slightly increase the "To" location to compensate for slime friction.
                Location from = event.getFrom();
                Location to = event.getTo();
                
                double dx = to.getX() - from.getX();
                double dz = to.getZ() - from.getZ();
                
                // Only apply if they are moving horizontally and on the ground
                if ((dx != 0 || dz != 0) && player.isOnGround()) {
                    // Slime friction is roughly 1.5x stronger than stone. 
                    // We push the destination a bit further.
                    event.setTo(new Location(
                        to.getWorld(), 
                        from.getX() + (dx * 1.6), 
                        to.getY(), 
                        from.getZ() + (dz * 1.6), 
                        to.getYaw(), 
                        to.getPitch()
                    ));
                }

                // 2. Fix Jumping (Slime usually reduces jump height)
                if (to.getY() > from.getY() && !player.isOnGround()) {
                    if (player.getVelocity().getY() > 0 && player.getVelocity().getY() < 0.42) {
                        Vector vel = player.getVelocity();
                        vel.setY(0.42);
                        player.setVelocity(vel);
                    }
                }

                // 3. Prevent Bounce (Cancel fall distance when hitting)
                if (player.getVelocity().getY() < 0) {
                    player.setFallDistance(0);
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
                
                // Mark player as boosted to prevent fall damage on any block
                boostedPlayers.add(player.getUniqueId());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        
        Player player = (Player) event.getEntity();
        
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            if (boostedPlayers.contains(player.getUniqueId())) {
                boolean preventDamage = plugin.getConfig().getBoolean("prevent-fall-damage", true);
                if (preventDamage) {
                    event.setCancelled(true);
                }
                boostedPlayers.remove(player.getUniqueId());
            }
        }
    }
}
