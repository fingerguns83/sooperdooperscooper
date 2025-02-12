package net.fg83.sooperdooperscooper;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Piglin;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import javax.annotation.Nullable;

import static net.fg83.sooperdooperscooper.SDS.config;

public class Util {

    private static final String NO_PERMISSION_MSG = ">> You don't have permission to do that, \"Doctor\".";
    private static final String PLAYER_REMOVE_ERROR = ">> That is a human being, you absolute monster.";
    private static final String PLAYER_GIVE_ERROR = ">> Are you trying to say they don't have a brain? Wow. Rude.";
    private static final String REVERSED_LOBOTOMY_MSG = ">> Lobotomy reversed! Hopefully there are no lasting effects.";
    private static final String LOBOTOMY_SUCCESS_MSG = ">> Lobotomy successful! Head empty, no thoughts.";
    private static final String ALREADY_NO_THOUGHTS_MSG = ">> There's nothing but air in here already!";
    private static final String BRAIN_ALREADY_PRESENT_MSG = ">> Slow down there, sport! There's already a brain in here!";
    private static final String ACTION_NOT_ALLOWED_MSG = ">> You cannot save this piglin's life. I'm sorry for your loss, doctor.";
    private static final String PIGLIN_DIVINE_INTERVENTION_MSG = ">> This Piglin was saved by divine intervention. You cannot do this.";

    public static void handleBrains(PlayerInteractEntityEvent event, @Nullable String permissionNode, boolean giveBrains) {
        Player player = event.getPlayer();
        LivingEntity entity = (LivingEntity) event.getRightClicked();
        World.Environment worldEnv = player.getWorld().getEnvironment();

        if (entity instanceof Player) {
            sendResponse(player, giveBrains ? PLAYER_GIVE_ERROR : PLAYER_REMOVE_ERROR, true);
            return;
        }

        String action = giveBrains ? "giveBrains" : "noBrains";
        boolean hasPermission = checkPerm(player, action, permissionNode);

        if (!hasPermission) {
            sendResponse(player, NO_PERMISSION_MSG, true);
            event.setCancelled(true);
            return;
        }

        boolean currentAIState = entity.hasAI();
        if (currentAIState == giveBrains) {
            sendResponse(player, giveBrains ? BRAIN_ALREADY_PRESENT_MSG : ALREADY_NO_THOUGHTS_MSG, true);
            event.setCancelled(true);
            return;
        }

        if (entity instanceof Piglin && !isPiglinActionAllowed(worldEnv, player, giveBrains)) {
            sendResponse(player, giveBrains ? PIGLIN_DIVINE_INTERVENTION_MSG : ACTION_NOT_ALLOWED_MSG, true);
            event.setCancelled(true);
            return;
        }

        entity.setAI(giveBrains);
        sendResponse(player, giveBrains ? REVERSED_LOBOTOMY_MSG : LOBOTOMY_SUCCESS_MSG, false);
    }

    private static boolean isPiglinActionAllowed(World.Environment worldEnv, Player player, boolean giveBrains) {
        return worldEnv.equals(World.Environment.NETHER) || config.getBoolean("allow-non-nether") || player.hasPermission("sds." + (giveBrains ? "giveBrains" : "noBrains"));
    }

    public static void sendResponse(Player player, String message, boolean isError){
        TextComponent content = new TextComponent(message);
        if (isError){
            content.setColor(ChatColor.RED);
        }
        else {
            content.setColor(ChatColor.GREEN);
        }
        player.spigot().sendMessage(content);
    }

    private static Boolean checkPerm(Player player, String action, String permissionNode){
        String permNode = "sds" + permissionNode + "." + action;

        if (player.hasPermission("sds." + action)){
            return true;
        }
        else {
            return player.hasPermission(permNode);
        }
    }

}
