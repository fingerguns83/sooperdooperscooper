package net.fg83.sooperdooperscooper.task;

import net.fg83.sooperdooperscooper.SDS;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.MerchantRecipe;

import java.util.List;

import static net.fg83.sooperdooperscooper.SDS.config;
import static org.bukkit.Bukkit.getServer;

public class VillagerRestockTask implements Runnable {

    SDS plugin;

    public VillagerRestockTask(SDS plugin){
        this.plugin = plugin;
    }

    @Override
    public void run() {
        long currentWorldTime = getServer().getWorlds().getFirst().getTime();

        if (!isWithinTimeRestrictions(currentWorldTime)) {
            return;
        }

        if (config.getBoolean("console-shows-restocking")) {
            getServer().getLogger().info(">> Villagers restocking...");
        }

        List<World> worlds = getServer().getWorlds();
        for (World world : worlds) {
            processWorldVillagers(world);
        }
    }

    private boolean isWithinTimeRestrictions(long currentWorldTime) {
        boolean workingHoursOnly = config.getBoolean("working-hours-only");
        boolean daytimeHoursOnly = config.getBoolean("daytime-hours-only");

        if (!workingHoursOnly && !daytimeHoursOnly) {
            return true;
        }

        if (daytimeHoursOnly && currentWorldTime <= 12000){
            return true;
        }

        return workingHoursOnly && (currentWorldTime > 2000 && currentWorldTime < 9000);
    }

    private void processWorldVillagers(World world) {
        for (Villager villager : world.getEntitiesByClass(Villager.class)) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> restockVillagerIfEligible(villager));
        }
    }

    private void restockVillagerIfEligible(Villager villager) {
        if (!isVillagerEligibleForRestocking(villager)) {
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (MerchantRecipe recipe : villager.getRecipes()) {
                recipe.setUses(0);
            }
        });
    }

    private boolean isVillagerEligibleForRestocking(Villager villager) {
        if (!villager.isAdult()) {
            return false;
        }

        boolean noAIOnly = config.getBoolean("noai-only");

        if (noAIOnly && villager.hasAI()) {
            return false;
        }

        Villager.Profession profession = villager.getProfession();
        return profession != Villager.Profession.NONE && profession != Villager.Profession.NITWIT;
    }
}
