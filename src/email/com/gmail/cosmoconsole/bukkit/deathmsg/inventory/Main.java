package email.com.gmail.cosmoconsole.bukkit.deathmsg.inventory;

import email.com.gmail.cosmoconsole.bukkit.deathmsg.ConfigTooOldException;
import email.com.gmail.cosmoconsole.bukkit.deathmsg.DMPReloadEvent;
import email.com.gmail.cosmoconsole.bukkit.deathmsg.DeathMessageTagListener;
import email.com.gmail.cosmoconsole.bukkit.deathmsg.DeathMessagesPrime;
import email.com.gmail.cosmoconsole.bukkit.deathmsg.DeathPreDMPEvent;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;
import org.json.simple.JSONObject;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ClickEvent.Action;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

public class Main extends JavaPlugin implements Listener, DeathMessageTagListener {
    final static String HEXINDEX = "0123456789abcdef";

    private static final int CONFIG_VERSION = 9;
    
    DeathMessagesPrime dmp;
    long expiry = 600 * 1000L;
    HashMap<String, Long> invdat;
    HashMap<String, Inventory> invobj;
    HashMap<UUID, String> lasttok;
    HashMap<UUID, String> lasttokkiller;
    Random rand;
    FileConfiguration config;
    String invtag;
    int mc_ver = 0;
    
    private boolean mcVer(int ver) {
        return mc_ver >= ver;
    }

    public void onEnable() {
        dmp = (DeathMessagesPrime) getServer().getPluginManager().getPlugin("DeathMessagesPrime");
        dmp.registerTag(this, "inventory", this);
        dmp.registerTag(this, "inventorykiller", this);
        dmp.registerTag(this, "plrtaginv", this);
        dmp.registerTag(this, "killerinv", this);
        dmp.registerTag(this, "victiminv", this);
        dmp.registerTag(this, "killer2inv", this);
        rand = new Random();
        invdat = new HashMap<String, Long>();
        invobj = new HashMap<String, Inventory>();
        lasttok = new HashMap<UUID, String>();
        lasttokkiller = new HashMap<UUID, String>();
        Bukkit.getPluginManager().registerEvents((Listener)this, (Plugin)this);
        loadConfig();

        String ver = Bukkit.getServer().getVersion().split("\\(MC:")[1].split("\\)")[0].trim().split(" ")[0].trim();
        this.getLogger().info("Minecraft version is " + ver);
        try {
            String[] tokens = ver.split("\\.");
            int mcMajor = Integer.parseInt(tokens[0]);
            int mcMinor = 0;
            @SuppressWarnings("unused")
            int mcRevision = 0;
            if (tokens.length > 1) {
                mcMinor = Integer.parseInt(tokens[1]);
            }
            if (tokens.length > 2) {
                mcRevision = Integer.parseInt(tokens[2]);
            }
            mc_ver = mcMajor * 1000 + mcMinor;
            // 1.8 = 1_008
            // 1.9 = 1_009
            // 1.10 = 1_010
            // ...
            // 1.14 = 1_014
            // 1.15 = 1_015
        } catch (Exception ex) {
            this.getLogger().warning("Cannot detect Minecraft version from string - " +
                                     "some features will not work properly. " + 
                                     "Please contact the plugin author if you are on " +
                                     "standard CraftBukkit or Spigot. This plugin " + 
                                     "expects getVersion() to return a string " + 
                                     "containing '(MC: 1.14)' or similar. The version " + 
                                     "DMP tried to parse was '" + ver + "'");
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                Set<String> setk = new HashSet<String>(invdat.keySet());
                for (String s: setk) {
                    if ((now-invdat.get(s)>=expiry)) {
                        if (invobj.containsKey(s))
                            clearInventory(invobj.remove(s));
                        invdat.remove(s);
                    }
                }
            }
        }.runTaskTimer(this, 1200L, 1200L);
    }
    
    protected void clearInventory(Inventory inventory) {
        for (HumanEntity he: inventory.getViewers()) {
            he.closeInventory();
        }
        inventory.clear();
    }

    private void loadConfig() {
        this.config = this.getConfig();
        this.invtag = "Inventory";
        try {
            this.config.load(new File(this.getDataFolder(), "config.yml"));
            if (!this.config.contains("config-version")) {
                throw new Exception();
            }
            if (this.config.getInt("config-version") < CONFIG_VERSION) {
                throw new ConfigTooOldException();
            }
        }
        catch (FileNotFoundException e6) {
            this.getLogger().info("Extracting default config.");
            this.saveResource("config.yml", true);
            try {
                this.config.load(new File(this.getDataFolder(), "config.yml"));
            }
            catch (IOException | InvalidConfigurationException ex3) {
                ex3.printStackTrace();
                this.getLogger().severe("The JAR config is broken, disabling");
                this.getServer().getPluginManager().disablePlugin((Plugin)this);
                this.setEnabled(false);
            }
        }
        catch (ConfigTooOldException e3) {
            this.getLogger().warning("!!! WARNING !!! Your configuration is old. There may be new features or some config behavior might have changed, so it is adviced to regenerate your config when possible!");
        }
        catch (Exception e4) {
            e4.printStackTrace();
            this.getLogger().severe("Configuration is invalid. Re-extracting it.");
            final boolean success = !new File(this.getDataFolder(), "config.yml").isFile() || new File(this.getDataFolder(), "config.yml").renameTo(new File(this.getDataFolder(), "config.yml.broken" + new Date().getTime()));
            if (!success) {
                this.getLogger().severe("Cannot rename the broken config, disabling");
                this.getServer().getPluginManager().disablePlugin((Plugin)this);
                this.setEnabled(false);
            }
            this.saveResource("config.yml", true);
            try {
                this.config.load(new File(this.getDataFolder(), "config.yml"));
            }
            catch (IOException | InvalidConfigurationException ex4) {
                ex4.printStackTrace();
                this.getLogger().severe("The JAR config is broken, disabling");
                this.getServer().getPluginManager().disablePlugin((Plugin)this);
                this.setEnabled(false);
            }
        }
        this.invtag = config.getString("inventory-text", "Inventory");
        this.expiry = 1000L * config.getInt("expiry-time-seconds", 600);
    }
    
    private String capitalize(String s) {
        if (s.isEmpty()) return s;
        StringBuilder sb = new StringBuilder();
        for (String word: s.split(" ")) {
            sb.append(" ");
            if (word.length() < 2) {
                sb.append(word.toUpperCase());
            } else {    
                sb.append(word.toUpperCase().charAt(0));
                sb.append(word.substring(1));
            }
        }
        return sb.toString().substring(1);
    }
    
    @SuppressWarnings("unchecked")
    String convertEntityToJson(Entity entity, String name) throws Exception {
        Map<String, String> data = new HashMap<String, String>();
        data.put( "name", name );
        data.put( "type", capitalize(entity.getType().toString().replace('_', ' ').toLowerCase()).replace(" ","") );
        data.put( "id", entity.getUniqueId().toString() );  
        JSONObject json = new JSONObject();
        json.putAll( data );
        return json.toString();
    }
    
    @Override
    public TextComponent formatTag(String arg0, Player arg1, DamageCause arg2, Entity arg3) {
        boolean useCustomTooltip = !this.config.getString("inventory-tooltip", "").isEmpty() && this.config.getBoolean("inventory-tooltip-instead-of-entity", false);
        if (arg0.equals("inventory")) {
            TextComponent tc2 = new TextComponent(this.invtag);
            tc2.setClickEvent(new ClickEvent(Action.RUN_COMMAND, "/^DMP_Inventory_Show!" + lasttok.get(arg1.getUniqueId())));
            if (!this.config.getString("inventory-tooltip", "").isEmpty())
                tc2.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(this.config.getString("inventory-tooltip", ""))));
            return tc2;
        }
        if (arg0.equals("inventorykiller")) {
            if (!lasttokkiller.containsKey(arg1.getUniqueId()))
                return new TextComponent();
            TextComponent tc2 = new TextComponent(this.config.getString("inventorykiller-text", this.invtag));
            tc2.setClickEvent(new ClickEvent(Action.RUN_COMMAND, "/^DMP_Inventory_Show!" + lasttokkiller.get(arg1.getUniqueId())));
            if (!this.config.getString("inventory-tooltip", "").isEmpty())
                tc2.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(this.config.getString("inventory-tooltip", ""))));
            return tc2;
        }
        if (arg0.equals("plrtaginv")) {
            TextComponent tc2 = new TextComponent(arg1.getName());
            try {
                tc2.setHoverEvent(useCustomTooltip ? new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(this.config.getString("inventory-tooltip", "")))
                : new HoverEvent(HoverEvent.Action.SHOW_ENTITY, new BaseComponent[] {
                        new TextComponent(convertEntityToJson(arg1, arg1.getName()).replace("\"id\"", "id").replace("\"name\"", "name").replace("\"type\"", "type"))
                }));
            } catch (Exception e) {
            }
            tc2.setClickEvent(new ClickEvent(Action.RUN_COMMAND, "/^DMP_Inventory_Show!" + lasttok.get(arg1.getUniqueId())));
            return tc2;
        }
        if (arg0.equals("victiminv")) {
            TextComponent tc2 = new TextComponent(arg1.getDisplayName());
            try {
                tc2.setHoverEvent(useCustomTooltip ? new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(this.config.getString("inventory-tooltip", "")))
                        : new HoverEvent(HoverEvent.Action.SHOW_ENTITY, new BaseComponent[] {
                        new TextComponent(convertEntityToJson(arg1, arg1.getName()).replace("\"id\"", "id").replace("\"name\"", "name").replace("\"type\"", "type"))
                }));
            } catch (Exception e) {
            }
            tc2.setClickEvent(new ClickEvent(Action.RUN_COMMAND, "/^DMP_Inventory_Show!" + lasttok.get(arg1.getUniqueId())));
            return tc2;
        }
        if (arg0.equals("killerinv")) {
            if (!lasttokkiller.containsKey(arg1.getUniqueId()))
                return new TextComponent();
            if (arg3 == null || !(arg3 instanceof Player))
                return new TextComponent();
            Player killer = (Player) arg3;
            TextComponent tc2 = new TextComponent(killer.getName());
            try {
                tc2.setHoverEvent(useCustomTooltip ? new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(this.config.getString("inventory-tooltip", "")))
                        : new HoverEvent(HoverEvent.Action.SHOW_ENTITY, new BaseComponent[] {
                        new TextComponent(convertEntityToJson(killer, killer.getName()).replace("\"id\"", "id").replace("\"name\"", "name").replace("\"type\"", "type"))
                }));
            } catch (Exception e) {
            }
            tc2.setClickEvent(new ClickEvent(Action.RUN_COMMAND, "/^DMP_Inventory_Show!" + lasttokkiller.get(arg1.getUniqueId())));
            return tc2;
        }
        if (arg0.equals("killer2inv")) {
            if (!lasttokkiller.containsKey(arg1.getUniqueId()))
                return new TextComponent();
            if (arg3 == null || !(arg3 instanceof Player))
                return new TextComponent();
            Player killer = (Player) arg3;
            TextComponent tc2 = new TextComponent(killer.getDisplayName());
            try {
                tc2.setHoverEvent(useCustomTooltip ? new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(this.config.getString("inventory-tooltip", "")))
                        : new HoverEvent(HoverEvent.Action.SHOW_ENTITY, new BaseComponent[] {
                        new TextComponent(convertEntityToJson(killer, killer.getName()).replace("\"id\"", "id").replace("\"name\"", "name").replace("\"type\"", "type"))
                }));
            } catch (Exception e) {
            }
            tc2.setClickEvent(new ClickEvent(Action.RUN_COMMAND, "/^DMP_Inventory_Show!" + lasttokkiller.get(arg1.getUniqueId())));
            return tc2;
        }
        return null;
    }
    
    public String randomStringRaw() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 16; i++) {
            sb.append(HEXINDEX.charAt(rand.nextInt(16)));
        }
        return sb.toString();
    }
    
    public String randomString() {
        String s = null;
        while (s == null || invdat.keySet().contains(s)) {
            s = randomStringRaw();
        }
        return s;
    }
    
    @EventHandler
    public void PlayerCommand(PlayerCommandPreprocessEvent event) {
        Player p = event.getPlayer();
        if (event.getMessage().startsWith("/^DMP_Inventory_Show!")) {
            if (!p.hasPermission("dmpinventoryplayerinfo.viewinv")) {
                String msg = org.bukkit.ChatColor.translateAlternateColorCodes('&', config.getString("no-permission-message", ""));
                if (msg.length() > 0) {
                    p.sendMessage(msg);
                }
                return;
            }
            String token = event.getMessage().substring(21); 
            event.setCancelled(true);
            if (invobj.containsKey(token)) {
                int timeleft = (int)Math.ceil(((invdat.get(token)+expiry)-System.currentTimeMillis())/1000.0d);
                if (timeleft < 0) {
                    if (config.getBoolean("time-messages", false)) {
                        String msg = org.bukkit.ChatColor.translateAlternateColorCodes('&', config.getString("time-expired-message", ""));
                        if (msg.length() > 0) {
                            p.sendMessage(msg);
                        }
                    }
                    return;
                }
                p.openInventory(invobj.get(token));
                if (config.getBoolean("time-messages", false)) {
                    String msg = org.bukkit.ChatColor.translateAlternateColorCodes('&', config.getString("time-remaining-message", ""));
                    msg = msg.replace("%n%", Integer.toString(timeleft));
                    if (msg.length() > 0) {
                        p.sendMessage(msg);
                    }
                }
            } else {
                if (config.getBoolean("time-messages", false)) {
                    String msg = org.bukkit.ChatColor.translateAlternateColorCodes('&', config.getString("time-expired-message", ""));
                    if (msg.length() > 0) {
                        p.sendMessage(msg);
                    }
                }
            }
        }
    }
    
    @EventHandler
    public void onDMPReload(DMPReloadEvent event) {
        this.loadConfig();
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory inventory = event.getInventory();
        if (isDMPVirtualInventory(inventory)) {
            if (config.getBoolean("allow-duplication", false) && event.getWhoClicked().hasPermission("dmpinventoryplayerinfo.duplicate")) {
            } else {
                event.setCancelled(true);
            }
        }
    }
    
    private boolean isDMPVirtualInventory(Inventory inventory) {
        return invobj.values().contains(inventory);
    }

    @EventHandler
    public void onPlayerDeath(DeathPreDMPEvent e) {
        String token = randomString();
        Player p = e.getPlayer();
        PlayerInventory pi = p.getInventory();
        String trunc = p.getDisplayName();
        if (trunc.length() > 32)
            trunc = trunc.substring(0, 32);
        Inventory inv;
        try {
            inv = createInventory(null, 54, trunc);
        } catch (Exception ex) {
            ex.printStackTrace();
            return;
        }
        for (int i = 0; i < 9; i++) {
            trySetItem(inv, i + 45, pi.getItem(i));
        }
        for (int i = 9; i < 36; i++) {
            trySetItem(inv, i + 9, pi.getItem(i));
        }
        trySetItem(inv, 0, pi.getHelmet());
        trySetItem(inv, 1, pi.getChestplate());
        trySetItem(inv, 2, pi.getLeggings());
        trySetItem(inv, 3, pi.getBoots());
        if (mcVer(1_009))
            trySetItem(inv, 4, pi.getItemInOffHand());
        int slot = 9;
        int used = 0; // xp = 8, hunger = 4, potion = 2, health = 1
        for (String s: getConfig().getStringList("status-items")) {
            if (s.equalsIgnoreCase("health")) {
                if ((used & 1) > 0) continue;
                used |= 1;
                try {
                    inv.setItem(--slot, createDamageItem(e, e.getPlayer()));
                } catch (Exception ex) {
                    getLogger().warning("Fallback items.health.type is invalid! Please report to plugin author with this message and stack trace and include your Minecraft version in the report.");
                    ex.printStackTrace();
                }
            }
            if (s.equalsIgnoreCase("potion")) {
                if ((used & 2) > 0) continue;
                used |= 2;
                try {
                    inv.setItem(--slot, createPotionItem(e, e.getPlayer()));
                } catch (Exception ex) {
                    getLogger().warning("Fallback items.potion.type is invalid! Please report to plugin author with this message and stack trace and include your Minecraft version in the report.");
                    ex.printStackTrace();
                }
            }
            if (s.equalsIgnoreCase("food")) {
                if ((used & 4) > 0) continue;
                used |= 4;
                try {
                    inv.setItem(--slot, createHungerItem(e, e.getPlayer()));
                } catch (Exception ex) {
                    getLogger().warning("Fallback items.food.type is invalid! Please report to plugin author with this message and stack trace and include your Minecraft version in the report.");
                    ex.printStackTrace();
                }
            }
            if (s.equalsIgnoreCase("experience")) {
                if ((used & 8) > 0) continue;
                used |= 8;
                try {
                    inv.setItem(--slot, createExperienceItem(e, e.getPlayer()));
                } catch (Exception ex) {
                    getLogger().warning("Fallback items.experience.type is invalid! Please report to plugin author with this message and stack trace and include your Minecraft version in the report.");
                    ex.printStackTrace();
                }
            }
        }
        lasttok.put(p.getUniqueId(), token);
        if (invobj.containsKey(token))
            clearInventory(invobj.get(token));
        invobj.put(token, inv);
        invdat.put(token, System.currentTimeMillis());
        if (e.getDamager() != null && e.getDamager() instanceof Player)
            alsoKillerInv((Player)e.getDamager(),p.getUniqueId(),e);
        else
            lasttokkiller.remove(p.getUniqueId());
    }

    private Inventory createInventory(InventoryHolder owner, int size, String title) {
        Inventory inv = Bukkit.createInventory(owner, size, title);
        
        return inv;
    }

    private void trySetItem(Inventory inv, int i, ItemStack s) {
        try {
            inv.setItem(i, s);
        } catch (Exception ex) {
        }
    }

    private ItemStack createPotionItem(DeathPreDMPEvent e, Player p) {
        ItemStack potion = new ItemStack(Material.POTION);
        PotionMeta potm = (PotionMeta) potion.getItemMeta();
        for (PotionEffect pe: p.getActivePotionEffects()) {
            potm.addCustomEffect(pe, false);
        }
        potm.setDisplayName(ChatColor.translateAlternateColorCodes('&', getConfig().getString("items.potion.name", p.getDisplayName())));
        potion.setItemMeta(potm);
        return potion;
    }
    
    private ItemStack createDamageItem(DeathPreDMPEvent e, Player p) {
        ItemStack dmgis = new ItemStack(tryMaterial(getConfig().getString("items.health.type","REDSTONE_BLOCK"),Material.REDSTONE_BLOCK), 1);
        ItemMeta dmgism = dmgis.getItemMeta();
        if (getConfig().getBoolean("items.health.classic", true)) {
            int dmg = (int)Math.ceil(e.getDamage());
            dmgis.setAmount(Math.min(64, dmg));
            if (dmg > 50) {
                dmgism.setDisplayName(ChatColor.RED + "â�¤ x 25+");
            } else if (dmg > 20) {
                dmgism.setDisplayName(ChatColor.RED + "â�¤ x " + (dmg >> 1) + ((dmg&1)>0 ? ".5" : ""));
            } else {
                StringBuilder dmgismn = new StringBuilder();
                while (dmg >= 2) {
                    dmgismn.append("â�¤");
                    dmg -= 2;
                }
                if (dmg > 0) {
                    dmgismn.append("â™¥");
                }
                dmgism.setDisplayName(ChatColor.RED + dmgismn.toString());
            }
        } else {
            String v = ChatColor.translateAlternateColorCodes('&', getConfig().getString("items.health.name", e.getPlayer().getDisplayName()));
            if (getConfig().getString("items.health.name") != null) {
                v = v.replace("%d1", String.valueOf((int)(p.getHealth())));
                try {
                    v = v.replace("%d2", String.valueOf((int)(p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue())));
                } catch (Exception ex) {
                    v = v.replace("%d2", String.valueOf((int)(40)));
                }
                v = v.replace("%d3", String.valueOf((int)Math.ceil(e.getDamage())));
            }
            dmgism.setDisplayName(v);
        }
        dmgis.setItemMeta(dmgism);
        return dmgis;
    }
    
    private ItemStack createHungerItem(DeathPreDMPEvent e, Player p) {
        ItemStack dmgis = new ItemStack(tryMaterial(getConfig().getString("items.food.type","COOKED_BEEF"),Material.COOKED_BEEF), 1);
        ItemMeta dmgism = dmgis.getItemMeta();
        if (getConfig().getBoolean("items.food.classic", true)) {
            int dmg = (int)Math.ceil(p.getFoodLevel());
            int leftover = (20 - dmg) >> 1;
            StringBuilder dmgismn = new StringBuilder();
            while (dmg >= 2) {
                dmgismn.append("â– ");
                dmg -= 2;
            }
            if (dmg > 0) {
                dmgismn.append("â—§");
            }
            for (int j = 0; j < leftover; ++j) {
                dmgismn.append("â–¡");
            }
            dmgism.setDisplayName(ChatColor.GOLD + dmgismn.toString());
        } else {
            String v = ChatColor.translateAlternateColorCodes('&', getConfig().getString("items.food.name", e.getPlayer().getDisplayName()));
            if (getConfig().getString("items.food.name") != null) {
                v = v.replace("%d1", String.valueOf((int)Math.ceil(p.getFoodLevel())));
                v = v.replace("%d2", "20");
            }
            dmgism.setDisplayName(v);
        }
        dmgis.setItemMeta(dmgism);
        return dmgis;
    }
    
    private ItemStack createExperienceItem(DeathPreDMPEvent e, Player p) {
        ItemStack dmgis = new ItemStack(tryMaterial(getConfig().getString("items.experience.type",getXPBottle()),Material.getMaterial(getXPBottle())), 1);
        ItemMeta dmgism = dmgis.getItemMeta();
        String v = ChatColor.translateAlternateColorCodes('&', getConfig().getString("items.experience.name", e.getPlayer().getDisplayName()));
        if (getConfig().getString("items.experience.name") != null) {
            v = v.replace("%d1", String.valueOf((int)p.getTotalExperience()));
            v = v.replace("%d2", String.valueOf((int)p.getLevel()));
        }
        dmgism.setDisplayName(v);
        dmgis.setItemMeta(dmgism);
        return dmgis;
    }

    private String getXPBottle() {
        if (mcVer(1_013))
            return "EXPERIENCE_BOTTLE";
        else
            return "EXP_BOTTLE";
    }

    private Material tryMaterial(String string, Material backup) {
        try {
            Material m = Material.getMaterial(string);
            if (m == null)
                return backup;
            return m;
        } catch (Exception e) {
            return backup;
        }
    }

    private void alsoKillerInv(Player p, UUID ou, DeathPreDMPEvent e) {
        String token = randomString();
        PlayerInventory pi = p.getInventory();
        String trunc = p.getDisplayName();
        if (trunc.length() > 32)
            trunc = trunc.substring(0, 32);
        Inventory inv;
        try {
            inv = createInventory(null, 45, trunc);
        } catch (Exception ex) {
            ex.printStackTrace();
            return;
        }
        for (int i = 0; i < 9; i++) {
            inv.setItem(i + 36, pi.getItem(i));
        }
        for (int i = 9; i < 36; i++) {
            inv.setItem(i, pi.getItem(i));
        }
        inv.setItem(0, pi.getHelmet());
        inv.setItem(1, pi.getChestplate());
        inv.setItem(2, pi.getLeggings());
        inv.setItem(3, pi.getBoots());
        if (mcVer(1_009))
            inv.setItem(4, pi.getItemInOffHand());
        int slot = 9;
        int used = 0; // xp = 8, hunger = 4, potion = 2, health = 1
        for (String s: getConfig().getStringList("status-items-killer")) {
            if (s.equalsIgnoreCase("health")) {
                if ((used & 1) > 0) continue;
                used |= 1;
                inv.setItem(--slot, createDamageItem(e, p));
            }
            if (s.equalsIgnoreCase("potion")) {
                if ((used & 2) > 0) continue;
                used |= 2;
                inv.setItem(--slot, createPotionItem(e, p));
            }
            if (s.equalsIgnoreCase("food")) {
                if ((used & 4) > 0) continue;
                used |= 4;
                inv.setItem(--slot, createHungerItem(e, p));
            }
            if (s.equalsIgnoreCase("experience")) {
                if ((used & 8) > 0) continue;
                used |= 8;
                inv.setItem(--slot, createExperienceItem(e, p));
            }
        }
        lasttokkiller.put(ou, token);
        if (invobj.containsKey(token))
            clearInventory(invobj.get(token));
        invobj.put(token, inv);
        invdat.put(token, System.currentTimeMillis());
    }
}
