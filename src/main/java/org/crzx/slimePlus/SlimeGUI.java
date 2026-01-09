package org.crzx.slimePlus;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class SlimeGUI implements Listener {

    private final SlimePlus plugin;
    private final Inventory inventory;
    
    // Temporary storage for unsaved changes
    private static final HashMap<UUID, TempConfig> tempConfigs = new HashMap<>();
    private static final HashMap<UUID, String> waitingForInput = new HashMap<>();

    public SlimeGUI(SlimePlus plugin) {
        this.plugin = plugin;
        this.inventory = Bukkit.createInventory(null, 27, "§8SlimePlus Admin Control");
    }

    private static class TempConfig {
        double multiplier;
        boolean boost;
        double maxHeight;
        boolean preventFallDamage;

        TempConfig(double multiplier, boolean boost, double maxHeight, boolean preventFallDamage) {
            this.multiplier = multiplier;
            this.boost = boost;
            this.maxHeight = maxHeight;
            this.preventFallDamage = preventFallDamage;
        }
    }

    public void open(Player player) {
        TempConfig config = tempConfigs.getOrDefault(player.getUniqueId(), new TempConfig(
                plugin.getConfig().getDouble("bounce-multiplier", 1.0),
                plugin.getConfig().getBoolean("boost-enabled", false),
                plugin.getConfig().getDouble("max-bounce-height", 256.0),
                plugin.getConfig().getBoolean("prevent-fall-damage", true)
        ));
        tempConfigs.put(player.getUniqueId(), config);
        
        refresh(player);
        player.openInventory(inventory);
    }

    private void refresh(Player player) {
        TempConfig config = tempConfigs.get(player.getUniqueId());
        
        // Background - Use a nice border pattern
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        ItemStack border = createItem(Material.LIME_STAINED_GLASS_PANE, "§a§lSlime§f§lPlus");
        
        for (int i = 0; i < 27; i++) {
            if (i < 9 || i > 17 || i % 9 == 0 || i % 9 == 8) {
                inventory.setItem(i, border);
            } else {
                inventory.setItem(i, filler);
            }
        }

        // Slider (Multiplier) - Slot 10
        if (config.maxHeight <= 0) {
            inventory.setItem(10, createItem(Material.BARRIER, "§e§lBounce Multiplier", 
                    "§7Status: §c§lDISABLED", "§7Set Max Height > 0 to enable."));
        } else {
            inventory.setItem(10, createGlowItem(Material.SLIME_BALL, "§e§lBounce Multiplier", 
                    "§7Current: §b" + config.multiplier + "x", "", "§eLeft-Click: §a+0.5", "§eRight-Click: §c-0.5"));
        }

        // Toggle (Boost) - Slot 12
        if (config.maxHeight <= 0) {
            inventory.setItem(12, createItem(Material.BARRIER, "§b§lBoost Mechanic", 
                    "§7Status: §c§lDISABLED", "§7Set Max Height > 0 to enable."));
        } else {
            inventory.setItem(12, createToggleItem(config.boost, "§b§lBoost Mechanic", 
                    "§7Status: " + (config.boost ? "§aEnabled" : "§cDisabled"), "", "§eClick to toggle"));
        }

        // Input (Max Height) - Slot 14
        inventory.setItem(14, createItem(Material.PAPER, "§d§lMax Bounce Height", 
                "§7Current: §f" + config.maxHeight + " blocks", "", "§eClick to set custom value"));

        // Toggle (Fall Damage) - Slot 16
        inventory.setItem(16, createToggleItem(config.preventFallDamage, "§f§lFall Damage Protection", 
                "§7Status: " + (config.preventFallDamage ? "§aEnabled" : "§cDisabled"), "", "§eClick to toggle"));

        // Save - Slot 21
        inventory.setItem(21, createGlowItem(Material.EMERALD_BLOCK, "§a§lSAVE CHANGES", "§7Apply changes to config.yml"));

        // Cancel - Slot 23
        inventory.setItem(23, createItem(Material.REDSTONE_BLOCK, "§c§lDISCARD", "§7Discard all changes"));
    }

    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            List<String> loreList = new ArrayList<>();
            for (String line : lore) loreList.add(line);
            meta.setLore(loreList);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createGlowItem(Material material, String name, String... lore) {
        ItemStack item = createItem(material, name, lore);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.addEnchant(Enchantment.LURE, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createToggleItem(boolean enabled, String name, String... lore) {
        Material mat = enabled ? Material.LIME_DYE : Material.GRAY_DYE;
        if (enabled) return createGlowItem(mat, name, lore);
        return createItem(mat, name, lore);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals("§8SlimePlus Admin Control")) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        UUID uuid = player.getUniqueId();
        TempConfig config = tempConfigs.get(uuid);
        if (config == null) return;

        int slot = event.getRawSlot();
        
        if (slot == 10) { // Multiplier
            if (config.maxHeight <= 0) {
                player.sendMessage("§cYou must set a Max Height greater than 0 to use the multiplier.");
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f);
                return;
            }
            if (event.isLeftClick()) {
                if (config.multiplier >= 10.0) config.multiplier = 0.5;
                else config.multiplier += 0.5;
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1.2f);
            } else if (event.isRightClick()) {
                if (config.multiplier <= 0.5) config.multiplier = 10.0;
                else config.multiplier -= 0.5;
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 0.8f);
            }
            refresh(player);
        } else if (slot == 12) { // Boost
            if (config.maxHeight <= 0) {
                player.sendMessage("§cYou must set a Max Height greater than 0 to use the boost.");
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f);
                return;
            }
            config.boost = !config.boost;
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, config.boost ? 1.5f : 0.5f);
            refresh(player);
        } else if (slot == 14) { // Max Height
            player.closeInventory();
            player.sendMessage("§aPlease type the new maximum height in chat:");
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1f);
            waitingForInput.put(uuid, "maxHeight");
        } else if (slot == 16) { // Fall Damage
            config.preventFallDamage = !config.preventFallDamage;
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, config.preventFallDamage ? 1.5f : 0.5f);
            refresh(player);
        } else if (slot == 21) { // Save
            plugin.getConfig().set("bounce-multiplier", config.multiplier);
            plugin.getConfig().set("boost-enabled", config.boost);
            plugin.getConfig().set("max-bounce-height", config.maxHeight);
            plugin.getConfig().set("prevent-fall-damage", config.preventFallDamage);
            plugin.saveConfig();
            player.closeInventory();
            player.sendMessage("§aConfiguration saved successfully!");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
            tempConfigs.remove(uuid);
        } else if (slot == 23) { // Cancel
            player.closeInventory();
            player.sendMessage("§cChanges discarded.");
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.5f, 1f);
            tempConfigs.remove(uuid);
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        if (waitingForInput.containsKey(uuid)) {
            event.setCancelled(true);
            String type = waitingForInput.remove(uuid);
            String message = event.getMessage();
            
            try {
                double val = Double.parseDouble(message);
                if (val < 0) {
                    player.sendMessage("§cValue cannot be negative.");
                } else {
                    TempConfig config = tempConfigs.get(uuid);
                    if (config != null) {
                        if (type.equals("maxHeight")) {
                            config.maxHeight = val;
                            if (val == 0) {
                                player.sendMessage("§aMax height set to 0. Slime mechanics are now disabled.");
                            } else {
                                player.sendMessage("§aMax height set to " + val);
                            }
                        }
                    }
                }
            } catch (NumberFormatException e) {
                player.sendMessage("§cInvalid number format.");
            }
            
            // Re-open GUI on main thread
            Bukkit.getScheduler().runTask(plugin, () -> open(player));
        }
    }
}
