package net.fg83.sooperdooperscooper;

import de.tr7zw.changeme.nbtapi.NBT;
import net.fg83.sooperdooperscooper.listener.NametagListener;
import net.fg83.sooperdooperscooper.task.PiglinCountdownTask;
import net.fg83.sooperdooperscooper.task.VillagerRestockTask;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class SDS extends JavaPlugin {

    public static FileConfiguration config;

    int restockInterval;

    private static final int FULL_DAY_TICKS = 24000;
    private static final int WORKING_HOURS_TICKS = 7000;
    private static final int DAYTIME_HOURS_TICKS = 12000;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = getConfig();
        this.getServer().getPluginManager().registerEvents(new NametagListener(), this);

        if (config.getBoolean("noai-villagers-trade")) {
            setupVillagerRestockTask();
        }

        if (config.getBoolean("noai-piglins-barter")) {
            initializePiglinBarterTask();
        }
    }

    private int calculateRestockInterval(int dailyRestockCount) {
        if (config.getBoolean("working-hours-only")) {
            return Math.round((float) WORKING_HOURS_TICKS / dailyRestockCount);
        }
        else if (config.getBoolean("daytime-hours-only")) {
            return Math.round((float) DAYTIME_HOURS_TICKS / dailyRestockCount);
        }
        else {
            return Math.round((float) FULL_DAY_TICKS / dailyRestockCount);
        }
    }

    private void setupVillagerRestockTask() {
        restockInterval = calculateRestockInterval(config.getInt("daily-restock-count"));

        getServer().getLogger().info("Restocking interval set to " + restockInterval + " game ticks");
        getServer().getScheduler().runTaskTimer(this, new VillagerRestockTask(this), 0, restockInterval);
    }

    private void initializePiglinBarterTask() {
        if (!NBT.preloadApi()) {
            getLogger().warning("NBT-API wasn't initialized properly, disabling the plugin");
            getPluginLoader().disablePlugin(this);
            return;
        }

        getServer().getScheduler().runTaskTimer(this, new PiglinCountdownTask(), 0, 1);
    }
}