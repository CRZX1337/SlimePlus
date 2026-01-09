package org.crzx.slimePlus;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
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
        // Use delta Y for more reliable velocity detection in PlayerMoveEvent
        double dy = event.getTo().getY() - event.getFrom().getY();
        
        // Trigger if moving downwards OR if they are standing on the block
        // This makes the "boost" much more reliable
        if (dy < 0 || player.isOnGround()) {
            // Check the block exactly below the player's feet
            // We check a slightly larger range (0.5 blocks) to ensure we catch the landing
            Block blockBelow = null;
            for (double d = 0.0; d <= 0.5; d += 0.1) {
                Block b = event.getTo().clone().subtract(0, d, 0).getBlock();
                if (b.getType() == Material.SLIME_BLOCK) {
                    blockBelow = b;
                    break;
                }
            }
            
            if (blockBelow != null) {
                // To prevent "infinite bouncing" or jitter, we only trigger if they aren't already moving upwards fast
                if (player.getVelocity().getY() > 0.5) return;

                double multiplier = plugin.getConfig().getDouble("bounce-multiplier", 1.0);
                boolean boostEnabled = plugin.getConfig().getBoolean("boost-enabled", false);
                double boostPower = plugin.getConfig().getDouble("boost-power", 0.5);
                boolean removeLimit = plugin.getConfig().getBoolean("remove-vanilla-limit", true);

                float fallDistance = player.getFallDistance();
                // Ensure a minimum bounce even for small drops
                if (fallDistance < 0.5f) fallDistance = 0.5f; 
                
                if (!removeLimit && fallDistance > 44.0f) {
                    fallDistance = 44.0f;
                }

                // Calculate base velocity using the same formula as before
                double newY = Math.sqrt(fallDistance * 0.2) * multiplier;
                
                // Automatic boost logic - MAKE IT POWERFUL
                if (boostEnabled) {
                    newY += (boostPower * 1.5) + 0.2; 
                } else {
                    newY += 0.1;
                }

                double maxVelocity = Math.sqrt(maxHeight * 0.2);
                if (newY > maxVelocity) {
                    newY = maxVelocity;
                }

                // Apply velocity
                Vector velocity = player.getVelocity();
                velocity.setY(newY);
                player.setVelocity(velocity);
                
                // CRITICAL: Reset fall distance
                player.setFallDistance(0);
                
                // --- COOL EFFECTS ---
                Location loc = player.getLocation();
                player.getWorld().spawnParticle(Particle.BLOCK, loc, 30, 0.5, 0.1, 0.5, 0.1, Material.SLIME_BLOCK.createBlockData());
                player.getWorld().spawnParticle(Particle.CLOUD, loc, 15, 0.3, 0.3, 0.3, 0.1);
                
                player.getWorld().playSound(loc, Sound.ENTITY_SLIME_JUMP, 1f, 1f);
                if (newY > 1.0) {
                    player.getWorld().playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.8f, 1.2f);
                }

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
