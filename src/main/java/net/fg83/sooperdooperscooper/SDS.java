package net.fg83.sooperdooperscooper;

import de.tr7zw.changeme.nbtapi.NBT;

import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
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

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

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

        if (config.getBoolean("noai-villagers-trade")){
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

        if (config.getBoolean("noai-piglins-barter")){
            if (!NBT.preloadApi()) {
                getLogger().warning("NBT-API wasn't initialized properly, disabling the plugin");
                getPluginLoader().disablePlugin(this);
                return;
            }
            new DecrementPiglin().runTaskTimer(this, 0, 1);
        }
    }

    @EventHandler
    public void onNametag(PlayerInteractEntityEvent event){
        Player player = event.getPlayer();
        EquipmentSlot hand = event.getHand();
        ItemStack item = event.getPlayer().getInventory().getItem(hand);
        Entity target = event.getRightClicked();

        assert item != null;
        if (item.getType().equals(Material.NAME_TAG) && target instanceof LivingEntity) {
            assert item.getItemMeta() != null;

            if (item.getItemMeta().hasDisplayName()){
                String permNode;
                if (item.getItemMeta().getDisplayName().equalsIgnoreCase("nobrains")) {
                    if (event.getRightClicked() instanceof Villager) {
                        permNode = ".villager";
                    }
                    else if (event.getRightClicked() instanceof Piglin) {
                        permNode = ".piglin";
                    }
                    else if (target instanceof ArmorStand || target instanceof Boat || target instanceof Minecart){
                        sendResponse(player, ">> ...I--I genuinely don't know what you're going for here. But *fine*, waste your tag.", true);
                        return;
                    }
                    else {
                        permNode = "";
                    }
                    noBrains(event, permNode);
                    return;
                }
                if (item.getItemMeta().getDisplayName().equalsIgnoreCase("givebrains")) {
                    if (event.getRightClicked() instanceof Villager) {
                        permNode = ".villager";
                    }
                    else if (event.getRightClicked() instanceof Piglin) {
                        permNode = ".piglin";
                    }
                    else if (target instanceof ArmorStand || target instanceof Boat || target instanceof Minecart){
                        sendResponse(player, ">> ...I--I genuinely don't know what you're going for here. But *fine*, waste your tag.", true);
                        return;
                    }
                    else {
                        permNode = "";
                    }
                    giveBrains(event, permNode);
                }
            }
        }
    }

    private Boolean checkPerm(Player player, String action, String permissionNode){
        String permNode = "sds" + permissionNode + "." + action;

        if (player.hasPermission("sds." + action)){
            return true;
        }
        else {
            return player.hasPermission(permNode);
        }
    }
    private void noBrains(PlayerInteractEntityEvent event, @Nullable String permissionNode){
        Player player = event.getPlayer();
        LivingEntity targetEntity = (LivingEntity) event.getRightClicked();
        World.Environment worldEnv = event.getPlayer().getWorld().getEnvironment();

        if (targetEntity instanceof Player){
            sendResponse(player, ">> That is a human being, you absolute monster.", true);
            return;
        }



        if (checkPerm(player, "noBrains", permissionNode)) {
            if (targetEntity.hasAI()) {
                if (targetEntity instanceof Piglin){
                    if (!worldEnv.equals(World.Environment.NETHER) && !config.getBoolean("allow-non-nether") && !player.hasPermission("sds.noBrains")){
                        sendResponse(player, ">> You cannot save this piglin's life. I'm sorry for your loss, doctor.", true);
                        event.setCancelled(true);
                        return;
                    }
                }

                targetEntity.setAI(false);
                sendResponse(player, ">> Lobotomy successful! Head empty, no thoughts.", false);
            }
            else {
                sendResponse(player, ">> There's nothing but air in here already!", true);
                event.setCancelled(true);
            }
        }
        else {
            sendResponse(player, ">> You don't have permission to do that, \"Doctor\".", true);
            event.setCancelled(true);
        }
    }
    private void giveBrains(PlayerInteractEntityEvent event, String permissionNode){
        Player player = event.getPlayer();
        LivingEntity targetEntity = (LivingEntity) event.getRightClicked();
        World.Environment worldEnv = event.getPlayer().getWorld().getEnvironment();

        if (event.getRightClicked() instanceof Player){
            sendResponse(player, ">> Are you trying to say they don't have a brain? Wow. Rude.", true);
            return;
        }

        if (checkPerm(player, "giveBrains", permissionNode)) {
            if (!targetEntity.hasAI()){
                if (targetEntity instanceof Piglin){
                    if (!worldEnv.equals(World.Environment.NETHER) && !config.getBoolean("allow-non-nether") && !player.hasPermission("sds.giveBrains")){
                        sendResponse(player, ">> This Piglin was saved by divine intervention. You cannot do this.", true);
                        event.setCancelled(true);
                        return;
                    }
                }

                targetEntity.setAI(true);
                sendResponse(player, ">> Lobotomy reversed! Hopefully there are no lasting effects.", false);
            }
            else {
                sendResponse(player, ">> Slow down there, sport! There's already a brain in here!", true);
                event.setCancelled(true);
            }
        }
        else {
            sendResponse(player, ">> You don't have permission to do that, \"Doctor\".", true);
            event.setCancelled(true);
        }
    }
    private class RestockTask extends BukkitRunnable {
        @Override
        public void run() {
            long time = getServer().getWorlds().getFirst().getTime();

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
            try {

                List<World> worlds = getServer().getWorlds();
                for (World world : worlds) {
                    if (!world.getEnvironment().equals(World.Environment.NETHER) && !config.getBoolean("allow-non-nether-barter")) {
                        continue;
                    }
                    for (Piglin piglin : world.getEntitiesByClass(Piglin.class)) {
                        if (piglin.hasAI() || !piglin.isAdult()){
                            continue;
                        }

                        AtomicBoolean finishedAdmiring = new AtomicBoolean(false);

                        NBT.modify(piglin, nbt -> {
                            ReadWriteNBT brain = nbt.getCompound("Brain");
                            assert brain != null;
                            ReadWriteNBT memories = brain.getCompound("memories");
                            assert memories != null;

                            if (memories.hasTag("minecraft:admiring_item")){
                                ReadWriteNBT admiring = memories.getCompound("minecraft:admiring_item");
                                assert admiring != null;
                                long ttl = admiring.getLong("ttl");
                                if (ttl - 1 > 0){
                                    admiring.setLong("ttl", ttl - 1);
                                }
                                else {
                                    finishedAdmiring.set(true);
                                }
                            }
                        });
                        if (finishedAdmiring.get()){
                            piglin.getInventory().clear();
                            NBT.modify(piglin, nbt -> {
                                ReadWriteNBT brain = nbt.getCompound("Brain");
                                assert brain != null;
                                ReadWriteNBT memories = brain.getCompound("memories");
                                assert memories != null;
                                memories.clearNBT();
                            });
                            Objects.requireNonNull(piglin.getEquipment()).setItemInOffHand(null);

                            LootContext.Builder lootContextBuilder = new LootContext.Builder(piglin.getLocation()).lootedEntity(getServer().getOnlinePlayers().stream().findFirst().orElseThrow());

                            List<ItemStack> outputItems = new ArrayList<>(LootTables.PIGLIN_BARTERING.getLootTable().populateLoot(null, lootContextBuilder.build()));

                            for (ItemStack item : outputItems){
                                piglin.getWorld().dropItem(piglin.getLocation(), item);
                            }
                        }
                    }
                }
            }
            catch (Exception e){
                getServer().getLogger().warning(e.getMessage());
            }
        }
    }

}