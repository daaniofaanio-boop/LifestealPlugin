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
    private final double LIMIT_HP = 60.0;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    public ItemStack getHeartItem() {
        ItemStack heart = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = heart.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§c§lSerce Lifesteal");
            meta.setLore(Arrays.asList("§7Kliknij PPM, aby zyskac serce!", "§8Przedmiot niezniszczalny"));
            meta.setCustomModelData(1001);
            meta.setFireResistant(true);
            heart.setItemMeta(meta);
        }
        return heart;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        modifyMaxHealth(victim, -2.0);
        victim.getWorld().dropItemNaturally(victim.getLocation(), getHeartItem());
        Player killer = victim.getKiller();
        if (killer != null) {
            modifyMaxHealth(killer, 2.0);
        }
    }

    @EventHandler
    public void onHeartSpawn(ItemSpawnEvent event) {
        if (isHeart(event.getEntity().getItemStack())) {
            event.getEntity().setTicksLived(-32768);
            event.getEntity().setInvulnerable(true);
        }
    }

    @EventHandler
    public void onHeartDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Item) {
            Item itemEntity = (Item) event.getEntity();
            if (isHeart(itemEntity.getItemStack())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getItem() != null && isHeart(event.getItem()) && (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) {
            Player player = event.getPlayer();
            // Poprawione na GENERIC_MAX_HEALTH dla 1.21.1
            AttributeInstance attr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (attr != null && attr.getBaseValue() < LIMIT_HP) {
                event.getItem().setAmount(event.getItem().getAmount() - 1);
                modifyMaxHealth(player, 2.0);
                player.playEffect(org.bukkit.EntityEffect.TOTEM_RESURRECT);
                player.playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 1.0f, 1.0f);
            }
            event.setCancelled(true);
        }
    }

    private boolean isHeart(ItemStack item) {
        return item != null && item.getType() == Material.NETHER_STAR && item.hasItemMeta() && item.getItemMeta().hasCustomModelData();
    }

    private void modifyMaxHealth(Player p, double amount) {
        // Poprawione na GENERIC_MAX_HEALTH dla 1.21.1
        AttributeInstance attr = p.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (attr != null) {
            double current = attr.getBaseValue();
            double next = Math.min(LIMIT_HP, Math.max(2.0, current + amount));
            attr.setBaseValue(next);
        }
    }
}
