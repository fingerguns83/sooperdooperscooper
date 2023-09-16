package net.fg83.sooperdooperscooper;

import de.tr7zw.changeme.nbtapi.NBTCompound;
import de.tr7zw.changeme.nbtapi.NBTEntity;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTables;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class SDS extends JavaPlugin implements Listener {

    int restockInterval;
    FileConfiguration config;

    SDS plugin = this;

    public void sendResponse(Player player, String message, boolean isError){
        TextComponent content = new TextComponent(message);
        if (isError){
            content.setColor(ChatColor.RED);
        }
        else {
            content.setColor(ChatColor.GREEN);
        }
        player.spigot().sendMessage(content);
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.getServer().getPluginManager().registerEvents(this, this);

        config = getConfig();

        if (config.getBoolean("villager-tags")){
            if (config.getBoolean("working-hours-only")) {
                restockInterval = Math.round((float) (7000 / config.getInt("daily-restock-count")));
            } else if (config.getBoolean("daytime-hours-only")) {
                restockInterval = Math.round((float) (12000 / config.getInt("daily-restock-count")));
            } else {
                restockInterval = Math.round((float) (24000 / config.getInt("daily-restock-count")));
            }
            getServer().getLogger().info("Restocking interval set to " + restockInterval + " game ticks");
            new RestockTask().runTaskTimer(this, 0, restockInterval);
        }

        if (config.getBoolean("piglin-tags")){
            new DecrementPiglin().runTaskTimer(this, 0, 1);
        }
    }

    @EventHandler
    public void onNametag(PlayerInteractEntityEvent event){
        Player player = event.getPlayer();
        EquipmentSlot hand = event.getHand();
        ItemStack item = event.getPlayer().getInventory().getItem(hand);
        assert item != null;
        if (item.getType().equals(Material.NAME_TAG) && event.getRightClicked() instanceof LivingEntity) {
            if (Objects.requireNonNull(item.getItemMeta()).hasDisplayName() && item.getItemMeta().getDisplayName().equalsIgnoreCase("nobrains")) {
                if (event.getRightClicked() instanceof Villager) {
                    if (config.getBoolean("villager-tags") || config.getBoolean("global-tags")) {
                        noBrains(event, "villager");
                        event.setCancelled(true);
                    }
                }
                else if (event.getRightClicked() instanceof Piglin) {
                    if (config.getBoolean("piglin-tags") || config.getBoolean("global-tags")) {
                        noBrains(event, "piglin");
                        event.setCancelled(true);
                    }
                }
                else {
                    if (config.getBoolean("global-tags")){
                        noBrains(event, "global");
                        event.setCancelled(true);
                    }
                }
            } else if (item.getItemMeta().hasDisplayName() && item.getItemMeta().getDisplayName().equalsIgnoreCase("givebrains")) {
                if (event.getRightClicked() instanceof Villager) {
                    if (config.getBoolean("villager-tags") || config.getBoolean("global-tags")){
                        giveBrains(event, "villager");
                        event.setCancelled(true);
                    }
                }
                else if (event.getRightClicked() instanceof Piglin){
                    if (config.getBoolean("piglin-tags") || config.getBoolean("global-tags")){
                        giveBrains(event, "piglin");
                        event.setCancelled(true);
                    }
                }
                else {
                    if (config.getBoolean("global-tags")){
                        giveBrains(event, "global");
                        event.setCancelled(true);
                    }
                }
            }
        }
    }

    private void noBrains(PlayerInteractEntityEvent event, String permissionNode){
        Player player = event.getPlayer();
        EquipmentSlot hand = event.getHand();
        ItemStack item = event.getPlayer().getInventory().getItem(hand);

        if (event.getRightClicked() instanceof Player){
            sendResponse(player, ">> That is a human being, you absolute monster.", true);
            return;
        }

        boolean permValid = false;

        switch (permissionNode){
            case "villager":
                if (player.hasPermission("sds.villager.noBrains") || player.hasPermission("sds.globalNoBrains")){
                    permValid = true;
                }
                break;
            case "piglin":
                if (player.hasPermission("sds.piglin.noBrains") || player.hasPermission("sds.globalNoBrains")){
                    permValid = true;
                }
                break;
            case "global":
                if (player.hasPermission("sds.globalNoBrains")){
                    permValid = true;
                }
                break;
        }

        if (permValid) {
            LivingEntity targetEntity = (LivingEntity) event.getRightClicked();
            if (targetEntity.hasAI()) {
                targetEntity.setAI(false);
                Objects.requireNonNull(player.getInventory().getItem(hand)).setAmount(item.getAmount() - 1);
                sendResponse(player, ">> Lobotomy successful! Head empty, no thoughts.", false);
            } else {
                sendResponse(player, ">> There's nothing but air in here already!", true);
            }
        }
        else {
            sendResponse(player, ">> You don't have permission to do that, \"Doctor\".", true);
        }
    }
    private void giveBrains(PlayerInteractEntityEvent event, String permissionNode){
        Player player = event.getPlayer();
        EquipmentSlot hand = event.getHand();
        ItemStack item = event.getPlayer().getInventory().getItem(hand);

        if (event.getRightClicked() instanceof Player){
            sendResponse(player, ">> Are you trying to say they don't have a brain? Wow. Rude.", true);
            return;
        }

        boolean permValid = false;

        switch (permissionNode){
            case "villager":
                if (player.hasPermission("sds.villager.giveBrains") || player.hasPermission("sds.globalGiveBrains")){
                    permValid = true;
                }
                break;
            case "piglin":
                if (player.hasPermission("sds.piglin.giveBrains") || player.hasPermission("sds.globalGiveBrains")){
                    permValid = true;
                }
                break;
            case "global":
                if (player.hasPermission("sds.globalGiveBrains")){
                    permValid = true;
                }
                break;
        }

        if (permValid) {
            LivingEntity targetEntity = (LivingEntity) event.getRightClicked();
            if (targetEntity.hasAI()){
                sendResponse(player, ">> Slow down there, sport! There's already a brain in here!", true);
            }
            else {
                targetEntity.setAI(true);
                Objects.requireNonNull(player.getInventory().getItem(hand)).setAmount(item.getAmount() - 1);
                sendResponse(player, ">> Lobotomy reversed! Hopefully there are no lasting effects.", false);
            }
        }
        else {
            sendResponse(player, ">> You don't have permission to do that, \"Doctor\".", true);
        }
    }
    private class RestockTask extends BukkitRunnable {
        @Override
        public void run() {
            long time = getServer().getWorlds().get(0).getTime();

            if (config.getBoolean("working-hours-only") && (time < 2000 || time > 9000)) {
                return;
            }
            if (config.getBoolean("daytime-hours-only") && (time > 12000)) {
                return;
            }

            if (config.getBoolean("console-shows-restocking")) {
                getServer().getLogger().info(">> Villagers restocking...");
            }

            List<World> worlds = getServer().getWorlds();
            for (World world : worlds) {
                for (Villager villager : world.getEntitiesByClass(Villager.class)) {
                    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                        if (config.getBoolean("noai-only") && villager.hasAI()) {
                            return;
                        }
                        if (!villager.isAdult() || villager.getProfession().equals(Villager.Profession.NONE) || villager.getProfession().equals(Villager.Profession.NITWIT)) {
                            return;
                        }
                        for (MerchantRecipe recipe : villager.getRecipes()) {
                            recipe.setUses(0);
                        }
                    });
                }
            }
        }
    }

    private class DecrementPiglin extends BukkitRunnable {
        @Override
        public void run() {
            List<World> worlds = getServer().getWorlds();
            for (World world : worlds) {
                if (!world.getEnvironment().equals(World.Environment.NETHER)) {
                    continue;
                }
                for (Piglin piglin : world.getEntitiesByClass(Piglin.class)) {
                    if (piglin.hasAI() || !piglin.isAdult()){
                        continue;
                    }
                    NBTEntity nbtPig = new NBTEntity(piglin);
                    NBTCompound memories = nbtPig.getCompound("Brain").getCompound("memories");
                    if (memories.hasTag("minecraft:admiring_item")){
                        NBTCompound admiring = memories.getCompound("minecraft:admiring_item");
                        long ttl = admiring.getLong("ttl");
                        if (ttl - 1 > 0){
                            admiring.setLong("ttl", ttl - 1);
                        }
                        else {
                            piglin.getInventory().clear();
                            piglin.getEquipment().setItemInOffHand(null);
                            memories.clearNBT();

                            LootContext.Builder lootContextBuilder = new LootContext.Builder(piglin.getLocation()).lootedEntity(getServer().getOnlinePlayers().stream().findFirst().get());

                            List<ItemStack> outputItems = new ArrayList<>(LootTables.PIGLIN_BARTERING.getLootTable().populateLoot(null, lootContextBuilder.build()));

                            for (ItemStack item : outputItems){
                                piglin.getWorld().dropItem(piglin.getLocation(), item);
                            }
                        }
                    }
                }
            }
        }
    }

}