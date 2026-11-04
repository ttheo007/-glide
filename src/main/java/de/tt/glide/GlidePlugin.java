package de.tt.glide;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class GlidePlugin extends JavaPlugin implements Listener {

    /** Spieler, die /glide eingeschaltet haben */
    private final Set<UUID> enabled = new HashSet<>();
    /** Spieler, die per Schleichen das Gleiten beendet haben (bis zum naechsten Schleichen oder Landung) */
    private final Set<UUID> paused = new HashSet<>();
    /** Spieler, die per Schleichen wieder gestartet haben: duerfen auch unter der Mindesthoehe gleiten */
    private final Set<UUID> manual = new HashSet<>();

    private double minHeight;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        minHeight = getConfig().getDouble("min-height", 18.0);
        getServer().getPluginManager().registerEvents(this, this);

        getServer().getScheduler().runTaskTimer(this, () -> {
            for (Player p : getServer().getOnlinePlayers()) {
                UUID id = p.getUniqueId();
                if (!enabled.contains(id)) continue;

                // Boden beruehrt (oder Wasser/Fliegen): Gleiten endet, Pause wird zurueckgesetzt
                if (p.isOnGround() || p.isInWater() || p.isFlying()) {
                    paused.remove(id);
                    manual.remove(id);
                    if (p.isGliding()) p.setGliding(false);
                    continue;
                }

                boolean high = manual.contains(id) || heightAboveGround(p) >= minHeight;
                if (paused.contains(id)) {
                    if (p.isGliding()) p.setGliding(false);
                } else if (high && !p.isGliding()) {
                    p.setGliding(true);
                }
                // Wer schon gleitet, gleitet weiter, bis geschlichen wird oder der Boden beruehrt wird
            }
        }, 1L, 1L);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Nur Spieler koennen /glide benutzen.");
            return true;
        }
        if (!p.hasPermission("glide.use")) {
            p.sendMessage("§cDafuer hast du keine Rechte.");
            return true;
        }
        UUID id = p.getUniqueId();
        if (enabled.remove(id)) {
            paused.remove(id);
            manual.remove(id);
            p.setGliding(false);
            p.sendMessage("§eGleiten §causgeschaltet§e.");
        } else {
            enabled.add(id);
            p.sendMessage("§eGleiten §aeingeschaltet§e. Start ab " + (int) minHeight
                    + " Bloecken Hoehe. §7Schleichen beendet das Gleiten, nochmal Schleichen startet es wieder.");
        }
        return true;
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent e) {
        Player p = e.getPlayer();
        UUID id = p.getUniqueId();
        if (!enabled.contains(id) || !e.isSneaking()) return;
        if (p.isOnGround() || p.isInWater() || p.isFlying()) return;

        if (p.isGliding()) {
            // Schleichen waehrend des Gleitens: beenden
            paused.add(id);
            manual.remove(id);
            p.setGliding(false);
            p.sendMessage("§eGleiten §caus§e. Nochmal Schleichen = wieder an.");
        } else if (paused.remove(id)) {
            // Naechstes Schleichen in der Luft: wieder starten
            manual.add(id);
            p.sendMessage("§eGleiten §aan§e.");
        }
    }

    // Der Client bricht das Gleiten sonst oft selbst ab. Das erlauben wir nur, wenn es gewollt ist.
    @EventHandler
    public void onToggleGlide(EntityToggleGlideEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        UUID id = p.getUniqueId();
        if (!enabled.contains(id) || e.isGliding()) return;
        if (paused.contains(id) || p.isOnGround() || p.isInWater()) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        UUID id = e.getPlayer().getUniqueId();
        enabled.remove(id);
        paused.remove(id);
        manual.remove(id);
    }

    private double heightAboveGround(Player p) {
        Location loc = p.getLocation();
        RayTraceResult hit = p.getWorld().rayTraceBlocks(
                loc, new Vector(0, -1, 0), 320, FluidCollisionMode.ALWAYS, true);
        if (hit == null || hit.getHitBlock() == null) return Double.MAX_VALUE; // kein Boden (Void)
        return loc.getY() - hit.getHitPosition().getY();
    }
}
