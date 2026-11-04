package de.tt.glide;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class GlidePlugin extends JavaPlugin implements Listener {

    private final Set<UUID> enabled = new HashSet<>();
    private double minHeight;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        minHeight = getConfig().getDouble("min-height", 18.0);
        getServer().getPluginManager().registerEvents(this, this);

        // Jede Tick pruefen, ob ein Spieler gleiten darf
        getServer().getScheduler().runTaskTimer(this, () -> {
            for (Player p : getServer().getOnlinePlayers()) {
                if (!enabled.contains(p.getUniqueId())) continue;
                boolean canGlide = canGlide(p);
                if (canGlide && !p.isGliding()) {
                    p.setGliding(true);
                } else if (!canGlide && p.isGliding()) {
                    p.setGliding(false);
                }
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
            p.setGliding(false);
            p.sendMessage("§eGleiten §causgeschaltet§e.");
        } else {
            enabled.add(id);
            p.sendMessage("§eGleiten §aeingeschaltet§e. Es startet ab " + (int) minHeight + " Bloecken Hoehe.");
        }
        return true;
    }

    // Verhindert, dass der Client das Gleiten abbricht, solange die Hoehe stimmt
    @EventHandler
    public void onToggleGlide(EntityToggleGlideEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        if (!enabled.contains(p.getUniqueId())) return;
        if (!e.isGliding() && canGlide(p)) {
            e.setCancelled(true);
        }
    }

    private boolean canGlide(Player p) {
        if (p.isOnGround() || p.isInWater() || p.isFlying()) return false;
        return heightAboveGround(p) >= minHeight;
    }

    private double heightAboveGround(Player p) {
        Location loc = p.getLocation();
        RayTraceResult hit = p.getWorld().rayTraceBlocks(
                loc, new Vector(0, -1, 0), 320, FluidCollisionMode.ALWAYS, true);
        if (hit == null) return Double.MAX_VALUE; // kein Boden (Void)
        Block b = hit.getHitBlock();
        return b == null ? Double.MAX_VALUE : loc.getY() - hit.getHitPosition().getY();
    }
}
