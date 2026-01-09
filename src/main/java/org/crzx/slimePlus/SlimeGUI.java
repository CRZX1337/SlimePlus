package org.crzx.slimePlus;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
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

        TempConfig(double multiplier, boolean boost, double maxHeight) {
            this.multiplier = multiplier;
            this.boost = boost;
            this.maxHeight = maxHeight;
        }
    }

    public void open(Player player) {
        TempConfig config = tempConfigs.getOrDefault(player.getUniqueId(), new TempConfig(
                plugin.getConfig().getDouble("bounce-multiplier", 1.0),
                plugin.getConfig().getBoolean("boost-enabled", false),
                plugin.getConfig().getDouble("max-bounce-height", 256.0)
        ));
        tempConfigs.put(player.getUniqueId(), config);
        
        refresh(player);
        player.openInventory(inventory);
    }

    private void refresh(Player player) {
        TempConfig config = tempConfigs.get(player.getUniqueId());
        
        // Background
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) inventory.setItem(i, filler);

        // Slider (Multiplier) - Slot 10
        inventory.setItem(10, createItem(Material.SLIME_BALL, "§aBounce Multiplier", 
                "§7Current: §f" + config.multiplier, "§eLeft Click: §7+0.5", "§eRight Click: §7-0.5"));

        // Toggle (Boost) - Slot 12
        inventory.setItem(12, createItem(config.boost ? Material.LIME_DYE : Material.GRAY_DYE, "§aBoost Mechanic", 
                "§7Status: " + (config.boost ? "§aEnabled" : "§cDisabled"), "§eClick to toggle"));

        // Input (Max Height) - Slot 14
        inventory.setItem(14, createItem(Material.PAPER, "§aMax Bounce Height", 
                "§7Current: §f" + config.maxHeight, "§eClick to set custom value"));

        // Save - Slot 21
        inventory.setItem(21, createItem(Material.EMERALD_BLOCK, "§a§lSAVE", "§7Apply changes to config.yml"));

        // Cancel - Slot 23
        inventory.setItem(23, createItem(Material.REDSTONE_BLOCK, "§c§lCANCEL", "§7Discard all changes"));
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
            if (event.isLeftClick()) {
                if (config.multiplier >= 5.0) config.multiplier = 0.5;
                else config.multiplier += 0.5;
            } else if (event.isRightClick()) {
                if (config.multiplier <= 0.5) config.multiplier = 5.0;
                else config.multiplier -= 0.5;
            }
            refresh(player);
        } else if (slot == 12) { // Boost
            config.boost = !config.boost;
            refresh(player);
        } else if (slot == 14) { // Max Height
            player.closeInventory();
            player.sendMessage("§aPlease type the new maximum height in chat:");
            waitingForInput.put(uuid, "maxHeight");
        } else if (slot == 21) { // Save
            plugin.getConfig().set("bounce-multiplier", config.multiplier);
            plugin.getConfig().set("boost-enabled", config.boost);
            plugin.getConfig().set("max-bounce-height", config.maxHeight);
            plugin.saveConfig();
            player.closeInventory();
            player.sendMessage("§aConfiguration saved successfully!");
            tempConfigs.remove(uuid);
        } else if (slot == 23) { // Cancel
            player.closeInventory();
            player.sendMessage("§cChanges discarded.");
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
