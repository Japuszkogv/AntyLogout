package me.japuszkogv.antyLogout;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.UUID;

public final class AntyLogout extends JavaPlugin implements Listener {

    private final HashMap<UUID, Long> combatLog = new HashMap<>();
    private final HashMap<UUID, BossBar> bossBars = new HashMap<>();
    private final int combatTime = 20;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("AntyLogout has been enabled successfully!");
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player victim && event.getDamager() instanceof Player attacker) {
            startCombat(victim);
            startCombat(attacker);
        }
    }

    private void startCombat(Player player) {
        UUID uuid = player.getUniqueId();
        combatLog.put(uuid, System.currentTimeMillis() + (combatTime * 1000L));

        if (!bossBars.containsKey(uuid)) {
            BossBar bar = Bukkit.createBossBar("§cCombat Tag: " + combatTime + "s left", BarColor.RED, BarStyle.SOLID);
            bar.addPlayer(player);
            bossBars.put(uuid, bar);

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!player.isOnline() || !combatLog.containsKey(uuid)) {
                        removeBar(uuid);
                        this.cancel();
                        return;
                    }

                    long timeLeft = (combatLog.get(uuid) - System.currentTimeMillis()) / 1000;

                    if (timeLeft <= 0) {
                        player.sendMessage("§aYou are now safe to log out.");
                        removeBar(uuid);
                        combatLog.remove(uuid);
                        this.cancel();
                    } else {
                        bar.setTitle("§cCombat Tag: " + timeLeft + "s left");
                        bar.setProgress(Math.max(0.0, Math.min(1.0, (double) timeLeft / combatTime)));
                    }
                }
            }.runTaskTimer(this, 0L, 20L);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (combatLog.containsKey(uuid) && combatLog.get(uuid) > System.currentTimeMillis()) {
            player.setHealth(0);
            Bukkit.broadcastMessage("§6Player " + player.getName() + " was killed for logging out during combat!");
        }
        removeBar(uuid);
        combatLog.remove(uuid);
    }

    private void removeBar(UUID uuid) {
        if (bossBars.containsKey(uuid)) {
            bossBars.get(uuid).removeAll();
            bossBars.remove(uuid);
        }
    }
}
