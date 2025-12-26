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
        // Tworzy folder i domyslny config.yml jesli nie istnieja
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("lifesteal").setExecutor(new LifestealCommand());
        getCommand("wyplacserce").setExecutor(new WyplacCommand());
    }

    public ItemStack getHeartItem(int amount) {
        ItemStack heart = new ItemStack(Material.valueOf(getConfig().getString("heart-item.material", "NETHER_STAR")), amount);
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
