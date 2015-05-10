package com.gmail.gogobebe2.bountyhead;

import com.gmail.gogobebe2.bountyhead.Listeners.HeadType;
import com.gmail.gogobebe2.bountyhead.Listeners.onBountyHeadSignCreateListener;
import com.gmail.gogobebe2.bountyhead.Listeners.onBountyHeadSignUseListener;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.text.NumberFormat;
import java.util.Arrays;

public class BountyHead extends JavaPlugin {
    public static Economy economy = null;

    @Override
    public void onEnable() {
        getLogger().info("Starting up BountyHead. If you have any bugs or problems, email me at: gogobebe2@gmail.com");
        if (!setupEconomy()) {
            getLogger().severe("Error!!! No economy plugin found!!!");
        }
        getConfig().options().copyDefaults(true);
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(new onBountyHeadSignCreateListener(), this);
        getServer().getPluginManager().registerEvents(new onBountyHeadSignUseListener(this), this);
    }

    @Override
    public void onDisable() {
        reloadConfig();
        saveConfig();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("bountyhead") || (label.equalsIgnoreCase("bounty")) || label.equalsIgnoreCase("bh")) {
            if (args.length > 0) {
                String subCommand = args[0];
                String[] arguments = {};
                if (args.length >= 1) {
                    arguments = Arrays.copyOfRange(args, 1, args.length);
                }
                if ((subCommand.equalsIgnoreCase("sellhead") || args[0].equalsIgnoreCase("sh")) && checkPermission(sender, "bountyhead.sellhead")) {
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(ChatColor.RED + "Error! You have to be a player to use this command!");
                        return true;
                    }
                    Player player = (Player) sender;
                    sellSkull(player);
                    return true;
                }
                else if (subCommand.equalsIgnoreCase("reload") && checkPermission(sender, "bountyhead.reload")) {
                    reloadConfig();
                    saveConfig();
                    sender.sendMessage(ChatColor.GREEN + "Config files reloaded!");
                    return true;
                }
                else if ((subCommand.equalsIgnoreCase("placebounty") || subCommand.equalsIgnoreCase("p")) && checkPermission(sender, "bountyhead.placebounty")) {
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(ChatColor.RED + "Error! You have to be a player to use this command!");
                        return true;
                    }
                    Player player = (Player) sender;
                    if (arguments.length < 2) {
                        player.sendMessage(ChatColor.RED + "Error! Wrong usage! Type " + ChatColor.GOLD
                                + "/bh p <player> <money>" + ChatColor.RED + " to place a bounty on a player head.");
                        return true;
                    }
                    String target = arguments[0];
                    double amount;
                    try {
                        amount = Double.parseDouble(arguments[1]);
                    }
                    catch (NumberFormatException exc) {
                        player.sendMessage(ChatColor.RED + "Error! " + ChatColor.GOLD + arguments[1] + ChatColor.RED + " is not a number!");
                        return true;
                    }

                    if (amount <= 0) {
                        player.sendMessage(ChatColor.RED + "Error! You cannot place a bounty of " + ChatColor.GOLD
                                + amount + ChatColor.RED + " on a player's head!");
                        return true;
                    }

                    economy.withdrawPlayer(player, amount);

                    //TODO: add target and the amount to a list in a txt file.
                        for (Player p : Bukkit.getOnlinePlayers()) {
                            p.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + player.getName() + ChatColor.GOLD
                                    + " placed bounty of " + ChatColor.DARK_PURPLE + ChatColor.BOLD + formatMoney(amount) + ChatColor.GOLD + " on "
                                    + ChatColor.BOLD + Bukkit.getOfflinePlayer(target).getName() + ChatColor.GOLD + "'s head.");
                            p.sendMessage(ChatColor.YELLOW + "" + ChatColor.ITALIC + "Someone kill him!");
                        }
                        return true;
                }
                else if ((subCommand.equalsIgnoreCase("removebounty") || subCommand.equalsIgnoreCase("r")) && checkPermission(sender, "bountyhead.placebounty")) {
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(ChatColor.RED + "Error! You have to be a player to use this command!");
                        return true;
                    }
                    Player player = (Player) sender;
                    if (arguments.length != 1) {
                        player.sendMessage(ChatColor.RED + "Error! Wrong usage! Type " + ChatColor.GOLD
                                + "/bh r <player>" + ChatColor.RED + " to remove a bounty from a player's head.");
                        return true;
                    }
                    String target = arguments[0];
                    //TODO: find the amount matching the target, give the player back his money, then remove the data of the target.
                }
            }
            displayHelp(sender);
        }
        return false;
    }

    private void showCommandUsage(CommandSender sender, String permission, String subCommand, String description) {
        if (sender.hasPermission(permission)) {
            sender.sendMessage(ChatColor.BLUE + " - " + ChatColor.BOLD + "/bountyhead " + ChatColor.BLUE + subCommand);
            sender.sendMessage(ChatColor.DARK_PURPLE + "" + ChatColor.ITALIC + description);
        }
    }

    private boolean checkPermission(CommandSender sender, String permission) {
        if (sender.hasPermission(permission)) {
            return true;
        } else {
            sender.sendMessage(ChatColor.RED + "Error! You don't have permission to use this command!");
            return false;
        }
    }

    private void displayHelp(CommandSender sender) {
        if (checkPermission(sender, "bountyhead.help")) {
            sender.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "[BountyHead Help]");
            showCommandUsage(sender, "bountyhead.sell", "sellhead|sh", "sell a player's head in your inventory");
            showCommandUsage(sender, "bountyhead.placebounty", "placebounty|p <player> <money>", "place a bounty on someone's head");
            showCommandUsage(sender, "bountyhead.removebounty", "removebounty|r <player>", "remove a bounty from someone's head");
            showCommandUsage(sender, "bountyhead.reload", "reload", "reload the BountyHead config files.");
        }
    }

    private String getSkullOwner(ItemStack skull) {
        SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
        String head;
        if (skullMeta.hasOwner()) {
            head = skullMeta.getOwner();
        } else if (skullMeta.hasDisplayName()) {
            head = skullMeta.getDisplayName();
        } else if (skull.getDurability() == 0) {
            head = "Skeleton";
        } else if (skull.getDurability() == 1) {
            head = "WSkeleton";
        } else if (skull.getDurability() == 2) {
            head = "Zombie";
        } else if (skull.getDurability() == 3) {
            head = "Steve";
        } else if (skull.getDurability() == 4) {
            head = "Creeper";
        } else {
            head = "Unknown";
        }
        return head;
    }


    private HeadType getHeadType(String owner) {
        if (owner.contains("MHF_")) {
            owner = owner.replaceFirst("MHF_", "");
        }
        for (String headName : getConfig().getConfigurationSection("prices.mobs").getKeys(false)) {
            if (headName.equalsIgnoreCase(owner)) {
                return HeadType.MOB;
            }
        }
        for (String headName : getConfig().getConfigurationSection("prices.blocks").getKeys(false)) {
            if (headName.equalsIgnoreCase(owner)) {
                return HeadType.BLOCK;
            }
        }
        for (String headName : getConfig().getConfigurationSection("prices.bonus").getKeys(false)) {
            if (headName.equalsIgnoreCase(owner)) {
                return HeadType.BONUS;
            }
        }

        return HeadType.PLAYER;
    }

    private double getSkullPrice(ItemStack skull) {
        String head = getSkullOwner(skull);
        HeadType headType = getHeadType(head);
        if (getConfig().isSet("prices.all")) {
            return getConfig().getDouble("prices.all");
        } else if (getConfig().isSet("prices.allMobs") && headType.equals(HeadType.MOB)) {
            return getConfig().getDouble("prices.allMobs");
        } else if (getConfig().isSet("prices.allBlocks") && headType.equals(HeadType.BLOCK)) {
            return getConfig().getDouble("prices.allBlocks");
        } else if (getConfig().isSet("prices.allBonus") && headType.equals(HeadType.BONUS)) {
            return getConfig().getDouble("prices.allBonus");
        } else if (headType.equals(HeadType.PLAYER)) {
            if (getConfig().isSet("prices.players.specificPlayers." + head)) {
                return getConfig().getDouble("prices.players.specificPlayers." + head);
            } else {
                double balance;
                try {
                    //noinspection deprecation
                    balance = BountyHead.economy.getBalance(Bukkit.getOfflinePlayer(head));
                } catch (NullPointerException exc) {
                    balance = 1;
                }
                return (getConfig().getDouble("prices.players.percentage") / 100) * balance;
            }
        } else {
            if (head.contains("MHF_")) {
                head = head.replaceFirst("MHF_", "");
            }
            if (headType.equals(HeadType.MOB)) {
                return getConfig().getDouble("prices.mobs." + head);
            } else if (headType.equals(HeadType.BLOCK)) {
                return getConfig().getDouble("prices.blocks." + head);
            } else if (headType.equals(HeadType.BONUS)) {
                return getConfig().getDouble("prices.bonus." + head);
            } else {
                throw new NumberFormatException();
            }
        }
    }

    private String formatMoney(double money) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance();
        return formatter.format(money);
    }

    public void sellSkull(Player player) {
        if (player.hasPermission("bountyhead.usesign")) {
            if (!player.getInventory().contains(Material.SKULL_ITEM, 1)) {
                player.sendMessage(ChatColor.RED + "Oops! You don't have any heads in your inventory!");
            } else {
                PlayerInventory inventory = player.getInventory();
                final boolean IS_SNEAKING = player.isSneaking();
                final int AMOUNT;
                int slot;
                if (inventory.getItemInHand() != null && inventory.getItemInHand().getType().equals(Material.SKULL_ITEM)) {
                    slot = inventory.getHeldItemSlot();
                } else {
                    slot = inventory.first(Material.SKULL_ITEM);
                }
                if (slot == -1) {
                    throw new NullPointerException("Null pointer exception! There are no items in the player's inventory that have the Material of Material.SKULL_ITEM");
                }
                ItemStack item = inventory.getItem(slot);
                final String SKULL_OWNER = getSkullOwner(item);
                double price = getSkullPrice(item);
                if (IS_SNEAKING) {
                    AMOUNT = item.getAmount();
                } else {
                    AMOUNT = 1;
                }
                price *= AMOUNT;

                BountyHead.economy.depositPlayer(player, price);
                item.setAmount(item.getAmount() - AMOUNT);
                inventory.setItem(slot, item);
                player.updateInventory();
                player.sendMessage(ChatColor.BLUE + "Sold "
                        + (IS_SNEAKING ? ChatColor.DARK_GREEN + "" + ChatColor.BOLD + AMOUNT : "a") + " " + ChatColor.DARK_GREEN
                        + ChatColor.ITALIC + SKULL_OWNER + ChatColor.BLUE + (IS_SNEAKING ? " heads " : " head") + " for "
                        + ChatColor.DARK_GREEN + formatMoney(price) + ChatColor.BLUE + ".");
            }
        } else {
            player.sendMessage(ChatColor.RED + "Error! You do not have permission to use head signs!");
        }

    }

    public static boolean isHeadSign(Block block) {
        if (!(block.getState() instanceof Sign)) {
            return false;
        }
        Sign sign = (Sign) block.getState();
        String[] signLines = sign.getLines();
        return signLines[0].equalsIgnoreCase(ChatColor.DARK_BLUE + " [Sell] ") && signLines[1].equalsIgnoreCase(" Head ");
    }


    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }
}
