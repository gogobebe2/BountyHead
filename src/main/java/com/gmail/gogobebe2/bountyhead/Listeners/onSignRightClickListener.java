package com.gmail.gogobebe2.bountyhead.Listeners;

import com.gmail.gogobebe2.bountyhead.BountyHead;
import net.ess3.api.MaxMoneyException;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.math.BigDecimal;

public class onSignRightClickListener implements Listener {
    BountyHead plugin;

    public onSignRightClickListener(BountyHead plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onSign(PlayerInteractEvent event) {
        if (event.isCancelled()) return;
        Player player = event.getPlayer();
        if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            if (BountyHead.isHeadSign(event.getClickedBlock())) {
                // TODO: check permission.
                if (!player.getInventory().contains(Material.SKULL_ITEM, 1)) {
                    player.sendMessage(ChatColor.RED + "Oops! You don't have any heads in your inventory!");
                } else {
                    sellSkull(player);
                }

            }
        }
    }

    private HeadType getHeadType(String owner) {
        if (owner.contains("MHF_")) {
            owner = owner.replaceFirst("MHF_", "");
        }
        for (String headName : plugin.getConfig().getConfigurationSection("prices.mobs").getKeys(false)) {
            plugin.getLogger().severe("DEBUG: headname: prices.mobs." + headName + " owner: " + owner);
            if (headName.equalsIgnoreCase(owner)) {
                return HeadType.MOB;
            }
        }
        for (String headName : plugin.getConfig().getConfigurationSection("prices.blocks").getKeys(false)) {
            plugin.getLogger().severe("DEBUG: headname: prices.blocks." + headName + " owner: " + owner);
            if (headName.equalsIgnoreCase(owner)) {
                return HeadType.BLOCK;
            }
        }
        for (String headName : plugin.getConfig().getConfigurationSection("prices.bonus").getKeys(false)) {
            plugin.getLogger().severe("DEBUG: headname: prices.bonus." + headName + " owner: " + owner);
            if (headName.equalsIgnoreCase(owner)) {
                return HeadType.BONUS;
            }
        }

        return HeadType.PLAYER;
    }

    private double getSkullPrice(SkullMeta skull) {
        String head;
        if (skull.hasOwner()) {
            head = skull.getOwner();
        } else {
            head = skull.getDisplayName();
        }
        HeadType headType = getHeadType(head);
        if (plugin.getConfig().isSet("prices.all")) {
            return plugin.getConfig().getDouble("prices.all");
        } else if (plugin.getConfig().isSet("prices.allMobs") && headType.equals(HeadType.MOB)) {
            return plugin.getConfig().getDouble("prices.allMobs");
        } else if (plugin.getConfig().isSet("prices.allBlocks") && headType.equals(HeadType.BLOCK)) {
            return plugin.getConfig().getDouble("prices.allBlocks");
        } else if (plugin.getConfig().isSet("prices.allBonus") && headType.equals(HeadType.BONUS)) {
            return plugin.getConfig().getDouble("prices.allBonus");
        } else if (headType.equals(HeadType.PLAYER)) {
            if (plugin.getConfig().isSet("prices.players.specificPlayer." + head)) {
                return plugin.getConfig().getDouble("prices.players.specificPlayer." + head);
            } else {
                double balance;
                try {
                    balance = BountyHead.getEss3().getUser(head).getMoney().doubleValue();
                } catch (NullPointerException exc) {
                    balance = 1;
                }
                return (plugin.getConfig().getDouble("prices.players.percentage") / 100) * balance;
            }
        } else {
            if (head.contains("MHF_")) {
                head = head.replaceFirst("MHF_", "");
            }
            if (headType.equals(HeadType.MOB)) {
                return plugin.getConfig().getDouble("prices.mobs." + head);
            } else if (headType.equals(HeadType.BLOCK)) {
                return plugin.getConfig().getDouble("prices.blocks." + head);
            } else if (headType.equals(HeadType.BONUS)) {
                return plugin.getConfig().getDouble("prices.bonus." + head);
            } else {
                throw new NumberFormatException();
            }
        }
    }


    private void sellSkull(Player player) {
        Inventory inventory = player.getInventory();
        final boolean IS_SNEAKING = player.isSneaking();
        final int AMOUNT;
        int slot = inventory.first(Material.SKULL_ITEM);
        if (slot == -1) {
            throw new NullPointerException("Null pointer exception! There are no items in the player's inventory that have the Material of Material.SKULL_ITEM");
        }
        ItemStack item = inventory.getItem(slot);
        SkullMeta skull = (SkullMeta) item.getItemMeta();
        double price = getSkullPrice(skull);
        if (IS_SNEAKING) {
            AMOUNT = item.getAmount();
        } else {
            AMOUNT = 1;
        }
        price *= AMOUNT;

        try {
            BountyHead.getEss3().getUser(player).giveMoney(BigDecimal.valueOf(price));
        } catch (MaxMoneyException e) {
            player.sendMessage(ChatColor.DARK_RED + "Error! Max money limit reached! Please report this error to the server administrator!");
        }
        item.setAmount(item.getAmount() - AMOUNT);
        inventory.setItem(slot, item);
        player.updateInventory();
        player.sendMessage(ChatColor.GREEN + "Sold " + ChatColor.BOLD + AMOUNT + ChatColor.GREEN + ChatColor.BOLD
                + ((AMOUNT == 1) ? " head " : " heads for ") + plugin.getConfig().getString("currencySymbol") + price + ChatColor.GREEN + ".");
    }
}
