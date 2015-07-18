/**
 * TownyAutoUnlocker - Licensed under the MIT License (MIT)
 *
 * Copyright (c) Stealth2800 <http://stealthyone.com/>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package me.st28.townyautounlocker;

import com.griefcraft.lwc.LWC;
import com.griefcraft.model.Protection;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.event.PlotClearEvent;
import com.palmergames.bukkit.towny.event.TownUnclaimEvent;
import com.palmergames.bukkit.towny.object.WorldCoord;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class TownyAutoUnlocker extends JavaPlugin implements Listener {

    private int townBlockSize;

    private String msgPlotUnclaimed;
    private String msgTownUnclaimed;

    private final EnumSet<Material> unclaimBlocks = EnumSet.noneOf(Material.class);

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getConfig().options().copyDefaults(true);
        saveConfig();

        reload();

        townBlockSize = TownySettings.getTownBlockSize();

        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("townyautounlocker.reload")) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "You do not have permission to use this command."));
            return true;
        }

        reload();
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
            String.format("%s v%s by Stealth2800 reloaded.",
                getName(),
                getDescription().getVersion()
            )
        ));
        return true;
    }

    private void reload() {
        reloadConfig();

        FileConfiguration config = getConfig();

        msgPlotUnclaimed = ChatColor.translateAlternateColorCodes('&',
            config.getString(
                "messages.plot_unclaimed",
                "&cYour locks on a plot you previously owned have been unlocked."
            )
        );

        msgTownUnclaimed = ChatColor.translateAlternateColorCodes('&',
            config.getString(
                "messages.town_unclaimed",
                "&cYour locks in a town you previously lived in have been unlocked."
            )
        );

        unclaimBlocks.clear();
        for (String rawMat : config.getStringList("unclaim blocks")) {
            try {
                unclaimBlocks.add(Material.valueOf(rawMat.toUpperCase()));
            } catch (Exception ex) {
                getLogger().warning("Invalid material in unclaim blocks '" + rawMat + "'");
            }
        }
    }

    @EventHandler
    public void onTownUnclaim(TownUnclaimEvent e) {
        WorldCoord coord = e.getWorldCoord();

        removeTownProtections(coord.getBukkitWorld(), getPlotCoords(coord));
    }

    @EventHandler
    public void onPlotClear(PlotClearEvent e) {
        WorldCoord coord = e.getTownBlock().getWorldCoord();

        removePlotProtections(coord.getBukkitWorld(), getPlotCoords(coord));
    }

    private Set<BlockLocation> getPlotCoords(WorldCoord coord) {
        BlockLocation newCoord = new BlockLocation(coord, townBlockSize);

        Set<BlockLocation> locations = new HashSet<>();

        for (int x = newCoord.x; x < newCoord.x + townBlockSize; x++) {
            for (int z = newCoord.z; z < newCoord.z + townBlockSize; z++) {
                locations.add(new BlockLocation(x, z));
            }
        }

        return locations;
    }

    private void removeTownProtections(World world, Set<BlockLocation> locations) {
        LWC lwc = LWC.getInstance();

        Set<String> alertPlayers = new HashSet<>();

        for (BlockLocation location : locations) {
            for (int y = 0; y < 256; y++) {
                Protection protection = lwc.findProtection(world, location.x, y, location.z);

                if (protection != null) {
                    if (unclaimBlocks.contains(protection.getBlock().getType())) {
                        protection.remove();
                        alertPlayers.add(protection.getOwner());
                    }
                }
            }
        }

        alertPlayers(alertPlayers, msgTownUnclaimed);
    }

    private void removePlotProtections(World world, Set<BlockLocation> locations) {
        LWC lwc = LWC.getInstance();

        Set<String> alertPlayers = new HashSet<>();

        for (BlockLocation location : locations) {
            for (int y = 0; y < 256; y++) {
                Protection protection = lwc.findProtection(world, location.x, y, location.z);

                if (protection != null) {
                    if (protection.getBlock().getType() == Material.AIR
                            || unclaimBlocks.contains(protection.getBlock().getType())) {
                        protection.remove();
                        alertPlayers.add(protection.getOwner());
                    }
                }
            }
        }

        alertPlayers(alertPlayers, msgPlotUnclaimed);
    }

    private void alertPlayers(Set<String> raw, String message) {
        for (String rawUuid : raw) {
            Player owner = null;
            try {
                owner = Bukkit.getPlayer(UUID.fromString(rawUuid));
            } catch (Exception ex) {
                owner = Bukkit.getPlayer(rawUuid);
            }

            if (owner != null) {
                owner.sendMessage(message);
            }
        }
    }

}