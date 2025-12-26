package me.lifesteal.plugin;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;

public class Main extends JavaPlugin implements Listener {

    // Limit 30 serc (1 serce = 2 punkty zdrowia, więc 60.0)
    private final double MAX_HEALTH_LIMIT = 60.0;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("LifeSteal Plugin aktywowany!");
    }

    // --- TWORZENIE PRZEDMIOTU SERCA ---
    public ItemStack getHeartItem() {
        ItemStack heart = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = heart.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName("§c§lSerce Lifesteal");
            meta.setLore(Arrays.asList(
                "§7Kliknij PPM, aby zyskac dodatkowe serce!",
                "§8Przedmiot niezniszczalny (Nigdy nie znika)"
            ));
            
            // Tekstura (CustomModelData dla resourcepacka)
            meta.setCustomModelData(1001); 
            
            // Odporność na ogień i lawę (wbudowana w 1.21.x)
            meta.setFireResistant(true); 
            
            heart.setItemMeta(meta);
        }
        return heart;
    }

    // --- LOGIKA ŚMIERCI: SERCE ZAWSZE WYPADA ---
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        Player victim =
