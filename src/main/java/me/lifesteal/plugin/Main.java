package me.lifesteal.plugin;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Main extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("lifesteal").setExecutor(new LifestealCommand());
        getCommand("wyplacserce").setExecutor(new WyplacCommand());
    }

    public ItemStack getHeartItem(int amount) {
        String matName = getConfig().getString("heart-item.material", "NETHER_STAR");
        ItemStack heart = new ItemStack(Material.valueOf(matName), amount);
        ItemMeta meta = heart.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(getConfig().getString("heart-item.name", "§c§lSerce Lifesteal").replace("&", "§"));
            List<String> lore = getConfig().getStringList("heart-item.lore").stream()
                    .map(s -> s.replace("&", "§"))
                    .collect(Collectors.toList());
            meta.setLore(lore);
            meta.setCustomModelData(getConfig().getInt("heart-item.model-data", 1001));
            heart.setItemMeta(meta);
        }
        return heart;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        double currentMax = getPlayerMaxHealth(victim);
        double minHp = getConfig().getDouble("settings.min-hp", 2.0);

        if (currentMax <= minHp) {
            String banReason = getConfig().getString("messages.ban-reason", "§cStraciles wszystkie serca!").replace("&", "§");
            Bukkit.getScheduler().runTask(this, () -> {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "ban " + victim.getName() + " " + banReason);
            });
        } else {
            modifyMaxHealth(victim, -2.0);
            victim.getWorld().dropItemNaturally(victim.getLocation(), getHeartItem(1));
        }

        Player killer = victim.getKiller();
        if (killer != null) {
            modifyMaxHealth(killer, 2.0);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getItem() != null && isHeart(event.getItem()) && (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) {
            Player player = event.getPlayer();
            double limitHp = getConfig().getDouble("settings.max-hp", 60.0);
            if (getPlayerMaxHealth(player) < limitHp) {
                event.getItem().setAmount(event.getItem().getAmount() - 1);
                modifyMaxHealth(player, 2.0);
                player.playEffect(org.bukkit.EntityEffect.TOTEM_RESURRECT);
                player.playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 1.0f, 1.0f);
            } else {
                player.sendMessage(getConfig().getString("messages.limit-reached", "§cOsiagnales juz limit serc!").replace("&", "§"));
            }
            event.setCancelled(true);
        }
    }

    private boolean isHeart(ItemStack item) {
        return item != null && item.hasItemMeta() && item.getItemMeta().hasCustomModelData() && 
               item.getItemMeta().getCustomModelData() == getConfig().getInt("heart-item.model-data", 1001);
    }

    private double getPlayerMaxHealth(Player p) {
        AttributeInstance attr = p.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        return attr != null ? attr.getBaseValue() : 20.0;
    }

    private void modifyMaxHealth(Player p, double amount) {
        AttributeInstance attr = p.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (attr != null) {
            double limitHp = getConfig().getDouble("settings.max-hp", 60.0);
            double minHp = getConfig().getDouble("settings.min-hp", 2.0);
            double next = Math.min(limitHp, Math.max(minHp, attr.getBaseValue() + amount));
            attr.setBaseValue(next);
        }
    }

    public class LifestealCommand implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
            if (!sender.hasPermission("lifesteal.admin")) return true;
            if (args.length >= 3 && args[0].equalsIgnoreCase("give") && args[1].equalsIgnoreCase("heart")) {
                Player target = Bukkit.getPlayer(args[2]);
                int amount = (args.length == 4) ? Integer.parseInt(args[3]) : 1;
                if (target != null) {
                    target.getInventory().addItem(getHeartItem(amount));
                    sender.sendMessage("§aDano " + amount + " serc graczowi " + target.getName());
                }
                return true;
            }
            if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                reloadConfig();
                sender.sendMessage("§aKonfiguracja przeladowana!");
                return true;
            }
            sender.sendMessage("§cUzycie: /lifesteal give heart (gracz) [ilosc] LUB /lifesteal reload");
            return true;
        }
    }

    public class WyplacCommand implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
            if (!(sender instanceof Player)) return true;
            Player p = (Player) sender;
            int toWyplac = (args.length > 0) ? Integer.parseInt(args[0]) : 1;
            double currentMax = getPlayerMaxHealth(p);
            if (currentMax > (toWyplac * 2.0)) {
                modifyMaxHealth(p, -(toWyplac * 2.0));
                p.getInventory().addItem(getHeartItem(toWyplac));
                p.sendMessage("§aWyplacono " + toWyplac + " serc!");
            } else {
                p.sendMessage("§cMasz za malo serc!");
            }
            return true;
        }
    }
}
