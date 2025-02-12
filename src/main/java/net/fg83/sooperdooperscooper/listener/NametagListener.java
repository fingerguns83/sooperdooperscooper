package net.fg83.sooperdooperscooper.listener;

import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

import static net.fg83.sooperdooperscooper.Util.*;

public class NametagListener implements Listener {
    private static final String NO_BRAINS_TAG = "nobrains";
    private static final String GIVE_BRAINS_TAG = "givebrains";
    private static final String INVALID_ENTITY_RESPONSE = ">> ...I--I genuinely don't know what you're going for here. But *fine*, waste your tag.";

    @EventHandler
    public void onNametagInteraction(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        EquipmentSlot hand = event.getHand();
        ItemStack itemInHand = player.getInventory().getItem(hand);
        Entity targetEntity = event.getRightClicked();

        if (itemInHand == null || itemInHand.getType() != Material.NAME_TAG || !(targetEntity instanceof LivingEntity)) {
            return;
        }
        if (!itemInHand.hasItemMeta() || !Objects.requireNonNull(itemInHand.getItemMeta()).hasDisplayName()) {
            return;
        }

        String displayName = itemInHand.getItemMeta().getDisplayName();

        switch (displayName.toLowerCase()) {
            case NO_BRAINS_TAG -> {
                if (handleInvalidEntityType(player, targetEntity)) return;
                String permNode = determinePermissionNode(targetEntity);
                handleBrains(event, permNode, false);
            }
            case GIVE_BRAINS_TAG -> {
                if (handleInvalidEntityType(player, targetEntity)) return;
                String permNode = determinePermissionNode(targetEntity);
                handleBrains(event, permNode, true);
            }
        }
    }

    private boolean handleInvalidEntityType(Player player, Entity targetEntity) {
        if (targetEntity instanceof ArmorStand || targetEntity instanceof Boat || targetEntity instanceof Minecart) {
            sendResponse(player, INVALID_ENTITY_RESPONSE, true);
            return true;
        }
        return false;
    }

    private String determinePermissionNode(Entity targetEntity) {
        if (targetEntity instanceof Villager) {
            return ".villager";
        } else if (targetEntity instanceof Piglin) {
            return ".piglin";
        }
        return "";
    }
}
