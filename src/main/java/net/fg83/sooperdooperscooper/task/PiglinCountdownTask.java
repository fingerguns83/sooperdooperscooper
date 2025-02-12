package net.fg83.sooperdooperscooper.task;

import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
import org.bukkit.World;
import org.bukkit.entity.Piglin;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTables;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import static net.fg83.sooperdooperscooper.SDS.config;
import static org.bukkit.Bukkit.getServer;

public class PiglinCountdownTask implements Runnable {
    /**
     * Runs this operation.
     */
    @Override
    public void run() {
        try {
            List<World> worlds = getServer().getWorlds();
            processWorlds(worlds);
        } catch (Exception e) {
            getServer().getLogger().warning(e.getMessage());
        }
    }

    private void processWorlds(List<World> worlds) {
        for (World world : worlds) {
            boolean allowBartering = config.getBoolean("allow-non-nether-barter");
            if (!world.getEnvironment().equals(World.Environment.NETHER) && !allowBartering) {
                continue;
            }
            processPiglins(world);
        }
    }

    private void processPiglins(World world) {
        for (Piglin piglin : world.getEntitiesByClass(Piglin.class)) {
            if (piglin.hasAI() || !piglin.isAdult()) {
                continue;
            }
            handlePiglinAdmiration(piglin);
        }
    }

    private void handlePiglinAdmiration(Piglin piglin) {
        AtomicBoolean finishedAdmiring = new AtomicBoolean(false);

        NBT.modify(piglin, nbt -> {
            ReadWriteNBT brain = nbt.getCompound("Brain");
            if (brain == null) return;

            ReadWriteNBT memories = brain.getCompound("memories");
            if (memories == null || !memories.hasTag("minecraft:admiring_item")) return;

            ReadWriteNBT admiring = memories.getCompound("minecraft:admiring_item");
            if (admiring == null) return;

            long ttl = admiring.getLong("ttl");
            if (ttl > 1) {
                admiring.setLong("ttl", ttl - 1);
            }
            else {
                finishedAdmiring.set(true);
            }
        });

        if (finishedAdmiring.get()) {
            clearPiglinAfterAdmiration(piglin);
            doLootDrop(piglin);
        }
    }

    private void clearPiglinAfterAdmiration(Piglin piglin) {
        piglin.getInventory().clear();
        Objects.requireNonNull(piglin.getEquipment()).setItemInOffHand(null);

        NBT.modify(piglin, nbt -> {
            ReadWriteNBT brain = nbt.getCompound("Brain");
            if (brain != null) {
                ReadWriteNBT memories = brain.getCompound("memories");
                if (memories != null) {
                    memories.clearNBT();
                }
            }
        });
    }

    private void doLootDrop(Piglin piglin){
        LootContext.Builder lootContextBuilder = new LootContext.Builder(piglin.getLocation())
                .lootedEntity(getServer().getOnlinePlayers().stream().findFirst().orElseThrow());

        List<ItemStack> outputItems = new ArrayList<>(LootTables.PIGLIN_BARTERING.getLootTable().populateLoot(null, lootContextBuilder.build()));

        for (ItemStack item : outputItems) {
            piglin.getWorld().dropItem(piglin.getLocation(), item);
        }
    }
}
