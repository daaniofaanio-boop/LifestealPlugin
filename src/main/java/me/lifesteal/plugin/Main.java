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

public class Main extends JavaPlugin implements Listener {
    private final double LIMIT_HP = 60.0;
    private final double MIN_HP = 2.0;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("lifesteal").setExecutor(new LifestealCommand());
        getCommand("wyplacserce").setExecutor(new WyplacCommand());
    }

    public ItemStack getHeartItem(int amount) {
        ItemStack heart = new ItemStack(Material.NETHER_STAR, amount);
        ItemMeta meta = heart.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§c§lSerce Lifesteal");
            meta.setLore(Arrays.asList("§7Kliknij PPM, aby zyskac serce!", "§8Przedmiot niezniszczalny"));
            meta.setCustomModelData(1001);
            heart.setItemMeta(meta);
        }
        return heart;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        double currentMax = getPlayerMaxHealth(victim);

        if (currentMax <= MIN_HP) {
            // Gracz ma 1 serce (2HP) i ginie -> dostaje bana
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "ban " + victim.getName() + " §cStraciles wszystkie serca!");
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
            if (getPlayerMaxHealth(player) < LIMIT_HP) {
                event.getItem().setAmount(event.getItem().getAmount() - 1);
                modifyMaxHealth(player, 2.0);
                player.playEffect(org.bukkit.EntityEffect.TOTEM_RESURRECT);
                player.playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 1.0f, 1.0f);
            } else {
                player.sendMessage("§cOsiagnales juz limit serc!");
            }
            event.setCancelled(true);
        }
    }

    private boolean isHeart(ItemStack item) {
        return item != null && item.getType() == Material.NETHER_STAR && item.hasItemMeta() && item.getItemMeta().hasCustomModelData();
    }

    private double getPlayerMaxHealth(Player p) {
        AttributeInstance attr = p.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        return attr != null ? attr.getBaseValue() : 20.0;
    }

    private void modifyMaxHealth(Player p, double amount) {
        AttributeInstance attr = p.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (attr != null) {
            double next = Math.min(LIMIT_HP, Math.max(MIN_HP, attr.getBaseValue() + amount));
            attr.setBaseValue(next);
        }
    }

    // Komenda /lifesteal give heart (ilość)
    public class LifestealCommand implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
            if (!sender.hasPermission("lifesteal.admin")) return true;
            
            if (args.length >= 3 && args[0].equalsIgnoreCase("give") && args[1].equalsIgnoreCase("heart")) {
                Player target = Bukkit.getPlayer(args[2]);
                int amount = args.length == 4 ? Integer.parseInt(args[3]) : 1;
                
                if (target != null) {
                    target.getInventory().addItem(getHeartItem(amount));
