import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Keyed;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.minecart.ExplosiveMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.SmithingTransformRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.event.entity.ProjectileHitEvent;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.entity.Arrow;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public final class PotatoSMP extends JavaPlugin {

    private DataStore data;
    private Items items;
    private BladeManager blades;
    private PotatoRecipes recipes;
    private PotatoMenu menu;
    private StreakManager streaks;
    private TuffSnowballManager snowball;
    private A5Manager a5;
    private PassiveManager passives;
    private ItemAbilities abilities;
    private SecondAbilities secondAbilities;
    private WeaponPassives weaponPassives;
    private RecipeMenu recipeMenu;
    private PotatoRerollerListener reroller;
    private OverlordListener overlord;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        data = new DataStore(this);
        data.load();
        items = new Items(this);
        blades = new BladeManager(this, data, items);
        recipes = new PotatoRecipes(this, items);
        recipes.register();
        menu = new PotatoMenu(this);
        streaks = new StreakManager(this);
        snowball = new TuffSnowballManager(this);
        a5 = new A5Manager(this);
        passives = new PassiveManager(this);
        abilities = new ItemAbilities(this);
        secondAbilities = new SecondAbilities(this);
        weaponPassives = new WeaponPassives(this);
        recipeMenu = new RecipeMenu(this);
        reroller = new PotatoRerollerListener(this);
        overlord = new OverlordListener(this);

        getServer().getPluginManager().registerEvents(new CombatListener(this), this);
        getServer().getPluginManager().registerEvents(new GuardListener(this), this);
        getServer().getPluginManager().registerEvents(new RecipeGuard(this), this);
        getServer().getPluginManager().registerEvents(new FarmListener(this), this);
        getServer().getPluginManager().registerEvents(new FoodListener(this), this);
        getServer().getPluginManager().registerEvents(snowball, this);
        getServer().getPluginManager().registerEvents(a5, this);
        getServer().getPluginManager().registerEvents(new OwnerGuard(this), this);
        getServer().getPluginManager().registerEvents(menu, this);
        getServer().getPluginManager().registerEvents(passives, this);
        getServer().getPluginManager().registerEvents(abilities, this);
        getServer().getPluginManager().registerEvents(secondAbilities, this);
        getServer().getPluginManager().registerEvents(weaponPassives, this);
        getServer().getPluginManager().registerEvents(recipeMenu, this);
        getServer().getPluginManager().registerEvents(reroller, this);
        getServer().getPluginManager().registerEvents(overlord, this);
        passives.start();

        PotatoCommand command = new PotatoCommand(this);
        PluginCommand pluginCommand = getCommand("potato");
        if (pluginCommand != null) {
            pluginCommand.setExecutor(command);
            pluginCommand.setTabCompleter(command);
        }
        getLogger().info("PotatoSMP enabled. Warrior's Blade: " + (blades.exists() ? "forged" : "not forged") + ".");
    }

    @Override
    public void onDisable() {
        if (recipes != null) {
            recipes.unregister();
        }
        if (data != null) {
            data.save();
        }
    }

    /** Server-wide broadcast, skipped when broadcasts are turned off in config.yml. */
    public void broadcast(String miniMessage) {
        if (getConfig().getBoolean("broadcasts", true)) {
            Bukkit.broadcast(Text.msg(miniMessage));
        }
    }

    /** Potato SMP does nothing until an admin runs /potatosmp enable. The setting is saved. */
    public boolean isPotatoFeatureEnabled() {
        return data != null && data.pluginEnabled();
    }

    /**
     * Emergency stop: turns the plugin off, cancels every running Potato SMP task, clears
     * slams in progress, and removes Tuff Snowball minecarts and falling blocks already in
     * the world.
     */
    public void emergencyStop() {
        setPotatoFeatureEnabled(false);
        getServer().getScheduler().cancelTasks(this);
        a5.stopAll();
        NamespacedKey throwerKey = new NamespacedKey(this, "tuff_thrower");
        NamespacedKey cartKey = new NamespacedKey(this, "tuff_cart");
        for (World world : getServer().getWorlds()) {
            for (Entity entity : world.getEntities()) {
                PersistentDataContainer pdc = entity.getPersistentDataContainer();
                if (pdc.has(throwerKey, PersistentDataType.STRING) || pdc.has(cartKey, PersistentDataType.BYTE)) {
                    entity.remove();
                }
            }
        }
    }

    public void setPotatoFeatureEnabled(boolean value) {
        data.setPluginEnabled(value);
        if (value) {
            passives.start();
        }
    }

    /** True when the plugin is switched on, or when the actor is an operator (potato.admin). */
    public boolean activeFor(Object actor) {
        if (isPotatoFeatureEnabled()) {
            return true;
        }
        return actor instanceof org.bukkit.permissions.Permissible permissible
                && permissible.hasPermission("potato.admin");
    }

    /** When true, item cooldowns are skipped and their timers are hidden. Set by /potato nocooldown. */
    public boolean noCooldowns() {
        return data != null && data.noCooldowns();
    }

    public void setNoCooldowns(boolean value) {
        data.setNoCooldowns(value);
    }

    public DataStore data() {
        return data;
    }

    public Items items() {
        return items;
    }

    public BladeManager blades() {
        return blades;
    }

    public PotatoRecipes recipes() {
        return recipes;
    }

    public A5Manager a5() {
        return a5;
    }

    public StreakManager streaks() {
        return streaks;
    }

    public RecipeMenu recipeMenu() {
        return recipeMenu;
    }

    public PotatoMenu menu() {
        return menu;
    }
}

/**
 * Potato A5 Strike, normal and enchanted.
 *
 * Right-click to target the nearest player within range. If there are blocks above that
 * player, or above the user up to the launch height, the strike is refused and no cooldown
 * starts. Otherwise the user launches straight up above the target and slams down. The
 * landing deals area damage, falling off with distance. The user takes no fall damage from
 * the slam, which OwnerGuard enforces through isSlamming().
 */
final class A5Manager implements Listener {

    private static final double GRAVITY = 0.08;

    private final PotatoSMP plugin;
    private final Map<UUID, Long> readyAt = new HashMap<>();
    private final Set<UUID> slamming = new HashSet<>();

    public A5Manager(PotatoSMP plugin) {
        this.plugin = plugin;
    }

    /** Forgets every slam in progress. Used by the emergency stop. */
    public void stopAll() {
        slamming.clear();
    }

    public boolean isSlamming(UUID id) {
        return slamming.contains(id);
    }

    /** Milliseconds left on this player's A5 cooldown. */
    public long remainingMillis(UUID id) {
        return Math.max(0L, readyAt.getOrDefault(id, 0L) - System.currentTimeMillis());
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (!plugin.activeFor(event.getPlayer())) {
            return;
        }
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Items.Kind kind = plugin.items().kindOf(event.getItem());
        if (kind != Items.Kind.POTATO_A5_STRIKE && kind != Items.Kind.ENCHANTED_POTATO_A5_STRIKE) {
            return;
        }
        event.setCancelled(true);

        Player caster = event.getPlayer();
        boolean enchanted = kind == Items.Kind.ENCHANTED_POTATO_A5_STRIKE;
        FileConfiguration cfg = plugin.getConfig();
        UUID casterId = caster.getUniqueId();
        long now = System.currentTimeMillis();

        if (slamming.contains(casterId)) {
            caster.sendActionBar(Text.gradient("A5 Strike already in progress"));
            return;
        }
        long ready = readyAt.getOrDefault(casterId, 0L);
        if (!plugin.noCooldowns() && now < ready) {
            caster.sendActionBar(Text.itemCooldown(kind, ((ready - now) + 999L) / 1000L));
            return;
        }

        LivingEntity target = pickTarget(caster, cfg.getDouble("a5.range", 20));
        if (target == null) {
            caster.sendMessage(Text.msg("<red>No target in range."));
            return;
        }

        double launchHeight = Math.max(6.0, cfg.getDouble("a5.launch_height", 16));
        World world = caster.getWorld();
        if (blockedAbove(target.getLocation().add(0, 1, 0), world.getMaxHeight() - target.getY())
                || blockedAbove(caster.getLocation().add(0, 1, 0), launchHeight + 1.0)) {
            caster.sendActionBar(Text.gradient("Blocked by shelter"));
            caster.sendMessage(Text.msg("<red>Blocked by shelter. A5 Strike refused."));
            return;
        }

        long cooldownSeconds = enchanted
                ? cfg.getLong("a5.cooldown_enchanted", 40)
                : cfg.getLong("a5.cooldown_normal", 25);
        readyAt.put(casterId, now + cooldownSeconds * 1000L);

        double damage = enchanted
                ? cfg.getDouble("a5.enchanted_damage", 16)
                : cfg.getDouble("a5.normal_damage", 10);
        double radius = Math.max(2.0, cfg.getDouble("a5.radius", 5.5));
        startSlam(caster, target.getUniqueId(), launchHeight, damage, radius);
        world.playSound(caster.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 1.4f);
        world.playSound(caster.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_3, 1.0f, 0.8f);
    }

    /** Players are always preferred. Mobs are only picked when no player is in range. */
    private static LivingEntity pickTarget(Player caster, double range) {
        Player bestPlayer = null;
        double bestPlayerDistance = Double.MAX_VALUE;
        LivingEntity bestMob = null;
        double bestMobDistance = Double.MAX_VALUE;
        for (Entity entity : caster.getNearbyEntities(range, range, range)) {
            if (!(entity instanceof LivingEntity living)) {
                continue;
            }
            if (living.equals(caster) || living.isDead() || living instanceof ArmorStand) {
                continue;
            }
            double distance = living.getLocation().distance(caster.getLocation());
            if (living instanceof Player player) {
                if (distance < bestPlayerDistance) {
                    bestPlayerDistance = distance;
                    bestPlayer = player;
                }
            } else if (distance < bestMobDistance) {
                bestMobDistance = distance;
                bestMob = living;
            }
        }
        return bestPlayer != null ? bestPlayer : bestMob;
    }

    private void startSlam(Player caster, UUID targetId, double launchHeight, double damage, double radius) {
        UUID casterId = caster.getUniqueId();
        slamming.add(casterId);
        FileConfiguration cfg = plugin.getConfig();
        int maxAirTicks = Math.max(20, cfg.getInt("a5.max_air_ticks", 120));
        caster.setVelocity(new Vector(0, Math.sqrt(2.0 * GRAVITY * launchHeight), 0));

        new BukkitRunnable() {
            private int age = 0;
            private boolean descending = false;

            @Override
            public void run() {
                age++;
                Player self = Bukkit.getPlayer(casterId);
                if (self == null || self.isDead()) {
                    slamming.remove(casterId);
                    cancel();
                    return;
                }
                World world = self.getWorld();
                Location at = self.getLocation();
                Entity targetNow = Bukkit.getEntity(targetId);

                if (!descending) {
                    // Rising: drift toward the target, leave a cloud trail, and rising whistle.
                    Vector drift = horizontalTowards(at, targetNow, 0.3);
                    self.setVelocity(new Vector(drift.getX(), self.getVelocity().getY(), drift.getZ()));
                    world.spawnParticle(Particle.CLOUD, at, 10, 0.4, 0.2, 0.4, 0.03);
                    world.spawnParticle(Particle.FLAME, at, 5, 0.3, 0.1, 0.3, 0.01);
                    if (age % 4 == 0) {
                        world.playSound(at, Sound.ENTITY_PHANTOM_FLAP, 1.0f, 0.6f + age * 0.02f);
                    }
                    if (self.getVelocity().getY() <= 0 || age >= maxAirTicks) {
                        descending = true;
                        Vector slam = horizontalTowards(at, targetNow, 0.6);
                        slam.setY(-3.2);
                        self.setVelocity(slam);
                        world.playSound(at, Sound.ENTITY_PHANTOM_SWOOP, 1.2f, 0.6f);
                    }
                    return;
                }

                // Falling: steer at the target and leave a fiery trail.
                world.spawnParticle(Particle.FLAME, at, 8, 0.25, 0.25, 0.25, 0.02);
                world.spawnParticle(Particle.CRIT, at, 10, 0.3, 0.3, 0.3, 0.1);
                world.spawnParticle(Particle.SMOKE, at, 6, 0.3, 0.3, 0.3, 0.02);
                if (age > 5 && self.isOnGround()) {
                    impact(self, damage, radius);
                    slamming.remove(casterId);
                    cancel();
                    return;
                }
                if (age > maxAirTicks + 200) {
                    slamming.remove(casterId);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private void impact(Player caster, double damage, double radius) {
        World world = caster.getWorld();
        Location center = caster.getLocation();

        world.spawnParticle(Particle.EXPLOSION, center, 4, 0.6, 0.2, 0.6, 0.0);
        world.spawnParticle(Particle.CLOUD, center, 90, radius * 0.5, 0.4, radius * 0.5, 0.15);
        world.spawnParticle(Particle.CRIT, center, 140, radius * 0.4, 0.5, radius * 0.4, 0.4);
        world.spawnParticle(Particle.FLAME, center, 70, radius * 0.3, 0.3, radius * 0.3, 0.1);
        world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.6f, 0.6f);
        world.playSound(center, Sound.ITEM_TRIDENT_THUNDER, 1.2f, 0.8f);
        world.playSound(center, Sound.ENTITY_PLAYER_ATTACK_STRONG, 1.0f, 0.5f);

        // Expanding shockwave ring over a few ticks.
        new BukkitRunnable() {
            private int step = 0;

            @Override
            public void run() {
                if (step >= 5) {
                    cancel();
                    return;
                }
                double ring = radius * (step + 1) / 5.0;
                ringParticles(world, center.clone().add(0, 0.3, 0), ring, Particle.CLOUD, 36);
                step++;
            }
        }.runTaskTimer(plugin, 0L, 2L);

        for (LivingEntity living : world.getNearbyLivingEntities(center, radius)) {
            if (living.equals(caster) || living.isDead() || living instanceof ArmorStand) {
                continue;
            }
            double distance = living.getLocation().distance(center);
            double closeness = 1.0 - (distance / radius);
            // No source: a blast is never credited to the user, so it can't soul-ban or build streaks.
            living.damage(damage * (0.5 + 0.5 * closeness));
            Vector out = living.getLocation().toVector().subtract(center.toVector()).setY(0);
            if (out.lengthSquared() < 0.0001) {
                out = new Vector(1, 0, 0);
            }
            out.normalize().multiply(0.6 + 1.4 * closeness).setY(0.5 + 0.6 * closeness);
            living.setVelocity(out);
        }

        // Passive: a short Resistance I after every slam.
        caster.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 60, 0, false, true, true));
    }

    private static Vector horizontalTowards(Location from, Entity target, double speed) {
        if (target == null || !target.getWorld().equals(from.getWorld())) {
            return new Vector(0, 0, 0);
        }
        Vector toward = target.getLocation().toVector().subtract(from.toVector()).setY(0);
        if (toward.lengthSquared() < 0.01) {
            return new Vector(0, 0, 0);
        }
        return toward.normalize().multiply(speed);
    }

    private static void ringParticles(World world, Location center, double radius, Particle particle, int points) {
        for (int i = 0; i < points; i++) {
            double angle = 2.0 * Math.PI * i / points;
            world.spawnParticle(particle, center.getX() + Math.cos(angle) * radius, center.getY(),
                    center.getZ() + Math.sin(angle) * radius, 1, 0, 0, 0, 0.0);
        }
    }

    /** True if a block is hit going straight up from 'from' for up to 'distance' blocks. */
    private static boolean blockedAbove(Location from, double distance) {
        World world = from.getWorld();
        if (world == null || distance <= 0) {
            return false;
        }
        RayTraceResult hit = world.rayTraceBlocks(from, new Vector(0, 1, 0), distance,
                FluidCollisionMode.NEVER, true);
        return hit != null;
    }
}

/**
 * The single Warrior's Blade and its soul list. Only the UUID stored in data.yml is valid.
 * Any other blade item is a copy and gets removed when it is validated.
 */
final class BladeManager {

    private final PotatoSMP plugin;
    private final DataStore data;
    private final Items items;

    public BladeManager(PotatoSMP plugin, DataStore data, Items items) {
        this.plugin = plugin;
        this.data = data;
        this.items = items;
    }

    public boolean exists() {
        return data.bladeUid() != null;
    }

    public boolean isCurrent(ItemStack stack) {
        if (items.kindOf(stack) != Items.Kind.WARRIORS_BLADE) {
            return false;
        }
        UUID current = data.bladeUid();
        return current != null && current.equals(items.uidOf(stack));
    }

    /** Creates the one blade and records its UUID. Call only when exists() is false. */
    public ItemStack forge() {
        UUID uid = UUID.randomUUID();
        data.setBladeUid(uid);
        return items.makeBlade(uid);
    }

    public boolean isSoulBanned(UUID id) {
        return data.souls().containsKey(id);
    }

    public void soulBan(UUID id, String name) {
        data.addSoul(id, name);
    }

    public Map<UUID, String> souls() {
        return data.souls();
    }

    /** Destroys the blade and frees every soul it held. */
    public void destroy(String reason) {
        if (!exists()) {
            return;
        }
        int freed = data.souls().size();
        data.clearSouls();
        data.setBladeUid(null);
        plugin.broadcast("<gold>The Warrior's Blade was destroyed (" + reason + "). <white>" + freed
                + "</white> soul(s) freed.");
    }

    /**
     * Validates one stack. Returns false if it must be deleted. Otherwise it re-enforces the
     * item's enchantments and returns true. Mutates the stack in place.
     */
    public boolean sanitize(ItemStack stack) {
        Items.Kind kind = items.kindOf(stack);
        if (kind == null) {
            return true;
        }
        if (stack.getType() != kind.material() || items.uidOf(stack) == null) {
            return false;
        }
        if (kind == Items.Kind.WARRIORS_BLADE) {
            if (!isCurrent(stack)) {
                return false;
            }
            items.enforce(stack);
        } else if (kind == Items.Kind.POTATO_SWORD) {
            items.enforce(stack);
        }
        return true;
    }

    /** Validates every slot of an inventory. Invalid items are removed. */
    public void validateInventory(Inventory inventory) {
        ItemStack[] contents = inventory.getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack stack = contents[slot];
            if (stack == null || stack.getType().isAir()) {
                continue;
            }
            inventory.setItem(slot, sanitize(stack) ? stack : null);
        }
    }
}

final class PotatoCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("help", "menu", "status", "blade", "give", "reload", "enable", "disable", "stop", "recipes", "recipe", "nocooldown");
    private static final List<String> GIVE_KINDS = java.util.Arrays.stream(Items.Kind.values())
            .filter(kind -> kind != Items.Kind.WARRIORS_BLADE)
            .map(kind -> kind.name().toLowerCase(Locale.ROOT))
            .toList();

    private final PotatoSMP plugin;

    public PotatoCommand(PotatoSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender);
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "menu" -> {
                if (!sender.hasPermission("potato.admin")) {
                    deny(sender);
                } else if (sender instanceof Player player) {
                    plugin.menu().open(player);
                } else {
                    sender.sendMessage(Text.msg("<red>Only players can open the menu."));
                }
            }
            case "status" -> status(sender, args);
            case "enable" -> setEnabled(sender, true);
            case "disable" -> setEnabled(sender, false);
            case "stop" -> stop(sender);
            case "nocooldown" -> noCooldown(sender, args);
            case "recipes", "recipe" -> {
                if (sender instanceof Player player) {
                    plugin.recipeMenu().open(player);
                } else {
                    sender.sendMessage(Text.msg("<red>Only players can open the recipe book."));
                }
            }
            case "blade" -> blade(sender, args);
            case "give" -> give(sender, args);
            case "reload" -> {
                if (!sender.hasPermission("potato.admin")) {
                    deny(sender);
                } else {
                    plugin.reloadConfig();
                    sender.sendMessage(Text.msg("<green>Config reloaded."));
                }
            }
            default -> sendHelp(sender);
        }
        return true;
    }

    private void status(CommandSender sender, String[] args) {
        OfflinePlayer target;
        if (args.length > 1) {
            target = Bukkit.getOfflinePlayerIfCached(args[1]);
            if (target == null) {
                sender.sendMessage(Text.msg("<red>Unknown player: " + Text.esc(args[1])));
                return;
            }
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            sender.sendMessage(Text.msg("<red>Usage: /potato status <player>"));
            return;
        }
        String name = target.getName() == null ? "unknown" : target.getName();
        int deaths = plugin.data().deaths(target.getUniqueId());
        boolean marked = plugin.data().isMarked(target.getUniqueId());
        boolean banned = plugin.blades().isSoulBanned(target.getUniqueId());
        sender.sendMessage(Text.msg("<gray>Status for <white>" + Text.esc(name) + "</white>"));
        sender.sendMessage(Text.msg("<gray>Deaths: <white>" + deaths));
        sender.sendMessage(Text.msg("<gray>Marked: " + (marked ? "<dark_red>Yes" : "<green>No")));
        sender.sendMessage(Text.msg("<gray>Soul-banned: " + (banned ? "<dark_red>Yes" : "<green>No")));
        sender.sendMessage(Text.msg("<gray>Kill streak: <white>" + plugin.data().streak(target.getUniqueId())));
    }

    private void blade(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Text.msg("<red>Usage: /potato blade <status|reset>"));
            return;
        }
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "status" -> {
                if (plugin.blades().exists()) {
                    sender.sendMessage(Text.msg("<gold>The Warrior's Blade is forged. <white>"
                            + plugin.blades().souls().size() + "</white> soul(s) held."));
                } else {
                    sender.sendMessage(Text.msg("<gray>The Warrior's Blade has not been forged."));
                }
            }
            case "reset" -> {
                if (!sender.hasPermission("potato.admin")) {
                    deny(sender);
                } else {
                    plugin.blades().destroy("reset by an admin");
                    sender.sendMessage(Text.msg("<green>Blade reset."));
                }
            }
            default -> sender.sendMessage(Text.msg("<red>Usage: /potato blade <status|reset>"));
        }
    }

    private void give(CommandSender sender, String[] args) {
        if (!sender.hasPermission("potato.admin")) {
            deny(sender);
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(Text.msg("<red>Usage: /potato give <player> <potato_of_life|potato_sword> [amount]"));
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(Text.msg("<red>That player is not online."));
            return;
        }
        Items.Kind kind = parseGiveKind(args[2]);
        if (kind == null) {
            sender.sendMessage(Text.msg("<red>The Warrior's Blade can only be created by forging it."));
            return;
        }
        int amount = 1;
        if (args.length >= 4) {
            try {
                amount = Integer.parseInt(args[3]);
            } catch (NumberFormatException ex) {
                sender.sendMessage(Text.msg("<red>Amount must be a whole number."));
                return;
            }
        }
        if (amount < 1 || amount > 16) {
            sender.sendMessage(Text.msg("<red>Amount must be between 1 and 16."));
            return;
        }
        for (int i = 0; i < amount; i++) {
            ItemStack stack = plugin.items().create(kind);
            target.getInventory().addItem(stack).values()
                    .forEach(overflow -> target.getWorld().dropItemNaturally(target.getLocation(), overflow));
        }
        sender.sendMessage(Text.msg("<green>Gave " + amount + " item(s) to " + Text.esc(target.getName()) + "."));
    }

    private static Items.Kind parseGiveKind(String raw) {
        for (Items.Kind kind : Items.Kind.values()) {
            if (kind != Items.Kind.WARRIORS_BLADE && kind.name().equalsIgnoreCase(raw)) {
                return kind;
            }
        }
        return null;
    }

    private void stop(CommandSender sender) {
        if (!sender.hasPermission("potato.admin")) {
            deny(sender);
            return;
        }
        plugin.emergencyStop();
        sender.sendMessage(Text.msg("<red>Potato SMP stopped. Running effects cancelled, and it's disabled until /potatosmp enable."));
    }

    private void setEnabled(CommandSender sender, boolean value) {
        if (!sender.hasPermission("potato.admin")) {
            deny(sender);
            return;
        }
        plugin.setPotatoFeatureEnabled(value);
        sender.sendMessage(Text.msg(value ? "<green>Potato SMP enabled." : "<yellow>Potato SMP disabled."));
    }

    private void noCooldown(CommandSender sender, String[] args) {
        if (!sender.hasPermission("potato.admin")) {
            deny(sender);
            return;
        }
        if (args.length < 2 || !(args[1].equalsIgnoreCase("true") || args[1].equalsIgnoreCase("false"))) {
            sender.sendMessage(Text.msg("<red>Usage: /potato nocooldown <true|false>"));
            return;
        }
        boolean value = Boolean.parseBoolean(args[1]);
        plugin.setNoCooldowns(value);
        sender.sendMessage(Text.msg(value ? "<green>Item cooldowns are off for everyone."
                : "<yellow>Item cooldowns are back on."));
    }

    private static void deny(CommandSender sender) {
        sender.sendMessage(Text.msg("<red>You don't have permission to do that."));
    }

    private static void sendHelp(CommandSender sender) {
        sender.sendMessage(Text.gradient("Potato SMP commands"));
        sender.sendMessage(Text.parse("<gray>/potato menu <dark_gray>- open the Potato SMP menu"));
        sender.sendMessage(Text.parse("<gray>/potato status [player] <dark_gray>- deaths, Marked, and ban status"));
        sender.sendMessage(Text.parse("<gray>/potato blade status <dark_gray>- whether the blade is forged"));
        sender.sendMessage(Text.parse("<gray>/potato give <player> <kind> [amount] <dark_gray>- admin"));
        sender.sendMessage(Text.parse("<gray>/potato blade reset <dark_gray>- admin: destroy the blade, free all souls"));
        sender.sendMessage(Text.parse("<gray>/potato reload <dark_gray>- admin: reload config.yml"));
        sender.sendMessage(Text.parse("<gray>/potato enable | disable <dark_gray>- admin: turn Potato SMP on or off"));
        sender.sendMessage(Text.parse("<gray>/potato stop <dark_gray>- admin: emergency stop, cancels running effects"));
        sender.sendMessage(Text.parse("<gray>/potato recipes <dark_gray>- open the recipe book"));
        sender.sendMessage(Text.parse("<gray>/potato nocooldown <true|false> <dark_gray>- admin: turn all item cooldowns off or on"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> options = new ArrayList<>();
        if (args.length == 1) {
            options.addAll(SUBCOMMANDS);
        } else if (args.length == 2 && args[0].equalsIgnoreCase("blade")) {
            options.add("status");
            options.add("reset");
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("status") || args[0].equalsIgnoreCase("give"))) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                options.add(player.getName());
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("nocooldown")) {
            options.add("true");
            options.add("false");
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            options.addAll(GIVE_KINDS);
        }
        String typed = args[args.length - 1].toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(typed)) {
                matches.add(option);
            }
        }
        return matches;
    }
}

/**
 * Persistent state in data.yml. Writes go to a temp file that is then moved over the
 * real file, so a crash mid-save can't leave a half-written file. The previous version is
 * kept as data.yml.bak and restored automatically if data.yml is unreadable.
 * All access happens on the main server thread.
 */
final class DataStore {

    private final PotatoSMP plugin;
    private final File file;
    private final File backup;
    private final File temp;
    private YamlConfiguration yml = new YamlConfiguration();

    public DataStore(PotatoSMP plugin) {
        this.plugin = plugin;
        File dir = plugin.getDataFolder();
        dir.mkdirs();
        this.file = new File(dir, "data.yml");
        this.backup = new File(dir, "data.yml.bak");
        this.temp = new File(dir, "data.yml.tmp");
    }

    public void load() {
        YamlConfiguration primary = read(file);
        if (primary == null || primary.getKeys(true).isEmpty()) {
            YamlConfiguration fallback = read(backup);
            if (fallback != null) {
                plugin.getLogger().warning("data.yml was unreadable or empty; restored from data.yml.bak.");
                primary = fallback;
            }
        }
        yml = primary == null ? new YamlConfiguration() : primary;
    }

    private YamlConfiguration read(File source) {
        if (!source.exists()) {
            return null;
        }
        YamlConfiguration config = new YamlConfiguration();
        try {
            config.load(source);
            return config;
        } catch (IOException | InvalidConfigurationException ex) {
            plugin.getLogger().severe("Could not read " + source.getName() + ": " + ex.getMessage());
            return null;
        }
    }

    public void save() {
        try {
            yml.save(temp);
            if (file.exists()) {
                Files.copy(file.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            try {
                Files.move(temp.toPath(), file.toPath(),
                        StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            plugin.getLogger().severe("Failed to save data.yml: " + ex.getMessage());
        }
    }

    public int deaths(UUID id) {
        return yml.getInt("deaths." + id, 0);
    }

    public int addDeath(UUID id) {
        int value = deaths(id) + 1;
        yml.set("deaths." + id, value);
        save();
        return value;
    }

    public boolean isMarked(UUID id) {
        return yml.contains("marked." + id);
    }

    public void setMarked(UUID id, String name) {
        yml.set("marked." + id, name);
        save();
    }

    public UUID bladeUid() {
        return parseUuid(yml.getString("blade.uid"));
    }

    public void setBladeUid(UUID uid) {
        yml.set("blade.uid", uid == null ? null : uid.toString());
        save();
    }

    /** Soul-banned players, keyed by UUID with their last known name. */
    public Map<UUID, String> souls() {
        Map<UUID, String> result = new HashMap<>();
        ConfigurationSection section = yml.getConfigurationSection("blade.souls");
        if (section == null) {
            return result;
        }
        for (String key : section.getKeys(false)) {
            UUID id = parseUuid(key);
            if (id != null) {
                result.put(id, section.getString(key, "unknown"));
            }
        }
        return result;
    }

    public void addSoul(UUID id, String name) {
        yml.set("blade.souls." + id, name);
        save();
    }

    public void clearSouls() {
        yml.set("blade.souls", null);
        save();
    }

    public boolean noCooldowns() {
        return yml.getBoolean("no_cooldowns", false);
    }

    public void setNoCooldowns(boolean value) {
        yml.set("no_cooldowns", value);
        save();
    }

    public boolean pluginEnabled() {
        return yml.getBoolean("enabled", false);
    }

    public void setPluginEnabled(boolean value) {
        yml.set("enabled", value);
        save();
    }

    public int streak(UUID id) {
        return yml.getInt("streaks." + id, 0);
    }

    /** Zero removes the entry. */
    public void setStreak(UUID id, int value) {
        yml.set("streaks." + id, value <= 0 ? null : value);
        save();
    }

    private static UUID parseUuid(String value) {
        if (value == null) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}

/**
 * Potato Hoe: harvesting a fully grown crop with it has a chance to drop a Golden Potato,
 * and the crop replants on the same farmland. Replanting only happens on farmland, so a
 * crop can't be replanted anywhere else.
 */
final class FarmListener implements Listener {

    private final PotatoSMP plugin;

    public FarmListener(PotatoSMP plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onHarvest(BlockBreakEvent event) {
        if (!plugin.isPotatoFeatureEnabled()) {
            return;
        }
        Player player = event.getPlayer();
        if (plugin.items().kindOf(player.getInventory().getItemInMainHand()) != Items.Kind.POTATO_HOE) {
            return;
        }
        Block block = event.getBlock();
        if (!(block.getBlockData() instanceof Ageable crop) || crop.getAge() < crop.getMaximumAge()) {
            return;
        }
        if (block.getRelative(BlockFace.DOWN).getType() != Material.FARMLAND) {
            return;
        }

        double chance = Math.max(0.0, Math.min(1.0, plugin.getConfig().getDouble("hoe.golden_potato_chance", 0.20)));
        if (ThreadLocalRandom.current().nextDouble() < chance) {
            block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5),
                    plugin.items().create(Items.Kind.GOLDEN_POTATO));
        }

        Material cropType = block.getType();
        Location location = block.getLocation();
        plugin.getServer().getScheduler().runTask(plugin, () -> replant(location, cropType));
    }

    private void replant(Location location, Material cropType) {
        Block block = location.getBlock();
        if (block.getType() != Material.AIR) {
            return;
        }
        if (block.getRelative(BlockFace.DOWN).getType() != Material.FARMLAND) {
            return;
        }
        BlockData data = cropType.createBlockData();
        if (data instanceof Ageable seedling) {
            seedling.setAge(0);
        }
        block.setBlockData(data);
    }
}

/** Effects for eating Potato of Life and Golden Potato. The food itself still feeds you. */
final class FoodListener implements Listener {

    private final PotatoSMP plugin;

    public FoodListener(PotatoSMP plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onConsume(PlayerItemConsumeEvent event) {
        if (!plugin.activeFor(event.getPlayer())) {
            return;
        }
        Items.Kind kind = plugin.items().kindOf(event.getItem());
        if (kind == Items.Kind.POTATO_OF_LIFE) {
            apply(event.getPlayer(), "effects.potato_of_life");
        } else if (kind == Items.Kind.GOLDEN_POTATO) {
            apply(event.getPlayer(), "effects.golden_potato");
        } else if (kind == Items.Kind.POTATO_SOUP) {
            event.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 0, false, true, true));
        }
    }

    private void apply(Player player, String path) {
        FileConfiguration cfg = plugin.getConfig();
        int regenLevel = Math.max(1, cfg.getInt(path + ".regeneration_level", 1));
        int regenSeconds = Math.max(0, cfg.getInt(path + ".regeneration_seconds", 0));
        int absorptionLevel = Math.max(1, cfg.getInt(path + ".absorption_level", 1));
        int absorptionSeconds = Math.max(0, cfg.getInt(path + ".absorption_seconds", 0));
        if (regenSeconds > 0) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, regenSeconds * 20,
                    regenLevel - 1, false, true, true));
        }
        if (absorptionSeconds > 0) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, absorptionSeconds * 20,
                    absorptionLevel - 1, false, true, true));
        }
    }
}

/**
 * The /potato menu. It uses a 54-slot chest in the same style as the BlissGems menus. The
 * holder is a dedicated class rather than a title match, so a renamed chest can't spoof it.
 */
final class PotatoMenu implements Listener {

    private static final int SIZE = 54;
    private static final int CLOSE_SLOT = 49;

    private final PotatoSMP plugin;

    public PotatoMenu(PotatoSMP plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        if (!plugin.activeFor(player)) {
            player.sendMessage(Text.msg("<red>Potato SMP is disabled. An admin can run /potatosmp enable."));
            return;
        }
        MenuHolder holder = new MenuHolder();
        Inventory inventory = Bukkit.createInventory(holder, SIZE, Text.gradient("Potato SMP Menu"));
        holder.inventory = inventory;

        UUID id = player.getUniqueId();
        int deaths = plugin.data().deaths(id);
        boolean marked = plugin.data().isMarked(id);
        boolean banned = plugin.blades().isSoulBanned(id);
        boolean forged = plugin.blades().exists();
        Map<UUID, String> souls = plugin.blades().souls();

        inventory.setItem(11, item(Material.PAPER, "Your Status", List.of(
                "<gray>Deaths: <white>" + deaths,
                "<gray>Marked: " + (marked ? "<dark_red>Yes" : "<green>No"),
                "<gray>Soul-banned: " + (banned ? "<dark_red>Yes" : "<green>No"))));

        inventory.setItem(13, item(Material.NETHERITE_SWORD, "Warrior's Blade", List.of(
                "<gray>Status: " + (forged ? "<gold>Forged" : "<green>Not forged"),
                "<gray>Souls held: <white>" + souls.size())));

        List<String> soulLines = new ArrayList<>();
        int shown = 0;
        for (String name : souls.values()) {
            if (shown == 10) {
                break;
            }
            soulLines.add("<gray>- <white>" + Text.esc(name));
            shown++;
        }
        if (soulLines.isEmpty()) {
            soulLines.add("<gray>No souls are held.");
        } else if (souls.size() > shown) {
            soulLines.add("<gray>...and " + (souls.size() - shown) + " more.");
        }
        inventory.setItem(15, item(Material.BOOK, "Soul List", soulLines));

        inventory.setItem(22, item(Material.POTATO, "Potato of Life", List.of(
                "<gray>Dropped when a player kills another player.")));

        inventory.setItem(CLOSE_SLOT, item(Material.BARRIER, "Close", List.of("<gray>Click to close.")));

        // Every Potato SMP item, in every free slot. The status and close slots are skipped.
        java.util.Set<Integer> taken = java.util.Set.of(11, 13, 15, 22, CLOSE_SLOT);
        List<Integer> free = new ArrayList<>();
        for (int slot = 27; slot < SIZE; slot++) {
            if (!taken.contains(slot)) {
                free.add(slot);
            }
        }
        for (int slot = 0; slot < 27; slot++) {
            if (!taken.contains(slot)) {
                free.add(slot);
            }
        }
        int index = 0;
        for (Items.Kind kind : Items.Kind.values()) {
            if (index >= free.size()) {
                break;
            }
            inventory.setItem(free.get(index++), itemComponent(kind.material(), kind.displayComponent(), List.of(kind.fullLore())));
        }

        player.openInventory(inventory);
    }

    private static ItemStack itemComponent(Material material, Component name, List<String> lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(name);
        meta.lore(lore.stream().map(Text::parse).toList());
        stack.setItemMeta(meta);
        return stack;
    }

    private static ItemStack item(Material material, String name, List<String> lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(Text.gradient(name));
        meta.lore(lore.stream().map(Text::parse).toList());
        stack.setItemMeta(meta);
        return stack;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!plugin.activeFor(event.getWhoClicked())) {
            return;
        }
        if (!(event.getView().getTopInventory().getHolder() instanceof MenuHolder)) {
            return;
        }
        event.setCancelled(true);
        if (event.getRawSlot() == CLOSE_SLOT && event.getWhoClicked() instanceof Player player) {
            player.closeInventory();
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!plugin.activeFor(event.getWhoClicked())) {
            return;
        }
        if (event.getView().getTopInventory().getHolder() instanceof MenuHolder) {
            event.setCancelled(true);
        }
    }

    private static final class MenuHolder implements InventoryHolder {
        private Inventory inventory;

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}

/**
 * Custom Potato SMP items. Each item carries a kind tag and a unique UUID in its
 * PersistentDataContainer, so display names never decide identity.
 *
 * Enum constant names are written into item data. Renaming one later breaks existing items.
 */
final class Items {

    public enum Kind {
        POTATO_WAND(Material.BLAZE_ROD, "Potato Wand",
                "Right-click: fire a potato bolt that hits hard.",
                "Each bolt deals up to 9 damage and never kills in one hit.",
                "Shots have a short cooldown."),
        POTATO_CHARM(Material.EMERALD, "Potato Charm",
                "A lucky charm that brings good fortune when carried.",
                "Right-click: Lucky Surge, giving Luck II and Absorption for 20 s.",
                "Passive: Luck I while held in either hand."),
        POTATO_WARD(Material.NAUTILUS_SHELL, "Potato Ward",
                "A ward against flame.",
                "Right-click: Ward Bubble for 8 s. Enemies near you are pushed out.",
                "Passive: Fire Resistance while held in either hand."),
        POTATO_LANTERN(Material.SOUL_LANTERN, "Blue Lantern",
                "A blue lantern that lights up the dark.",
                "Passive: Night Vision while held in either hand."),
        POTATO_SLIME(Material.SLIME_BALL, "Potato Slime",
                "Squishy and bouncy.",
                "Sneak + right-click: Bounce, launching enemies within 4 blocks upward. 15 s cooldown.",
                "Passive: Jump Boost I while held in either hand."),
        POTATO_GHAST(Material.GHAST_TEAR, "Potato Ghast",
                "A tear that carries a wail.",
                "Sneak + right-click: Fear, pushing enemies within 8 blocks away. 20 s cooldown.",
                "Passive: Regeneration I while held in either hand."),
        POTATO_BLAZE(Material.BLAZE_POWDER, "Potato Blaze",
                "Hot powder with a kick.",
                "Sneak + right-click: Ignite, setting enemies within 5 blocks alight and hurting them. 20 s cooldown.",
                "Passive: Strength I while held in either hand."),
        POTATO_QUARTZ(Material.QUARTZ, "Potato Quartz",
                "Clear crystal, bright as day.",
                "Sneak + right-click: Prism Flash, glowing enemies within 12 blocks for 8 s. 25 s cooldown.",
                "Passive: Water Breathing while held in either hand."),
        POTATO_ECHO(Material.ECHO_SHARD, "Potato Echo",
                "It remembers every sound.",
                "Sneak + right-click: Echo Pulse, pushing and hitting enemies within 6 blocks. 15 s cooldown.",
                "Passive: Hero of the Village while held in either hand."),
        POTATO_SOUP(Material.MUSHROOM_STEW, "Potato Soup",
                "A hearty bowl of potato soup.",
                "Eat it for Regeneration I for 5 s."),
        POTATO_OVERLORD(Material.HEART_OF_THE_SEA, "Potato Overlord",
                "Admins only. It can never be crafted or rerolled.",
                "Right-click: Overlord Storm. Five seconds of lightning across 20 blocks, then a shockwave. 60 s cooldown."),
        POTATO_COMPASS(Material.COMPASS, "Potato Compass",
                "A compass that shows you who is around.",
                "Passive: Dolphin's Grace while held in either hand."),
        POTATO_FEATHER(Material.FEATHER, "Potato Feather",
                "Light as air.",
                "Passive: Slow Falling while held in either hand."),
        POTATO_SCROLL(Material.PAPER, "Potato Scroll",
                "A scroll full of old potato knowledge.",
                "Passive: Conduit Power while held in either hand."),
        POTATO_REROLLER(Material.AMETHYST_SHARD, "Potato Reroller",
                "Right-click to use it up and turn it into a random Potato item.",
                "It is never crafted, and it is gone once used."),
        POTATO_OF_LIFE(Material.POTATO, "Potato of Life", "Eat it for Regeneration II (5 s) and Absorption I (2 min).", "Dropped when a player kills another player.", "Passive: Health Boost I while held in either hand."),
        GOLDEN_POTATO(Material.POTATO, "Golden Potato", "A cheaper golden apple.", "Eat it for Regeneration I (3 s) and Absorption I (30 s).", "Passive: Absorption I while held in either hand."),
        POTATO_ORE(Material.RAW_GOLD, "Potato Ore", "Crafting and smithing ingredient.", "Used in potato tools, the mace, spear, bow, shield, and A5 Strike."),
        POTATO_SWORD(Material.NETHERITE_SWORD, "Potato Sword", "Kill a Marked player with it to forge the Warrior's Blade.", "Sharpness VI.", "Passive: Strength I while held in either hand."),
        WARRIORS_BLADE(Material.NETHERITE_SWORD, "Warrior's Blade", "Players killed directly by it are soul-banned.", "Destroying it frees every soul.", "Sharpness I, unbreakable.", "Passive: Strength II while held in either hand."),
        POTATO_HOE(Material.NETHERITE_HOE, "Potato Hoe", "1 in 10 chance of a Golden Potato on each harvest.", "Replants crops after harvest.", "Passive: Haste I while held in either hand."),
        POTATO_PICKAXE(Material.NETHERITE_PICKAXE, "Potato Pickaxe", "A potato-forged netherite pickaxe.", "Passive: Haste I while held in either hand."),
        POTATO_AXE(Material.NETHERITE_AXE, "Potato Axe", "A potato-forged netherite axe.", "Passive: Speed I while held in either hand."),
        POTATO_SHOVEL(Material.NETHERITE_SHOVEL, "Potato Shovel", "A potato-forged netherite shovel.", "Passive: Haste I while held in either hand."),
        POTATO_MACE(Material.MACE, "Potato Mace", "A simple potato-forged mace.", "Passive: Resistance I while held in either hand."),
        POTATO_SPEAR(Material.NETHERITE_SPEAR, "Potato Spear", "A potato-forged netherite spear.", "Passive: Speed I while held in either hand."),
        POTATO_HELMET(Material.NETHERITE_HELMET, "Potato Helmet", "Netherite armor made by smithing a diamond helmet.", "Passive: Resistance I while worn."),
        POTATO_CHESTPLATE(Material.NETHERITE_CHESTPLATE, "Potato Chestplate", "Netherite armor made by smithing a diamond chestplate.", "Passive: Resistance I while worn."),
        POTATO_LEGGINGS(Material.NETHERITE_LEGGINGS, "Potato Leggings", "Netherite armor made by smithing diamond leggings.", "Passive: Resistance I while worn."),
        POTATO_BOOTS(Material.NETHERITE_BOOTS, "Potato Boots", "Netherite armor made by smithing diamond boots.", "Passive: Resistance I while worn."),
        TUFF_SNOWBALL(Material.SNOWBALL, "Tuff Snowball", "Throw it: a rain of TNT minecarts, anvils, and pointed dripstone falls.", "TNT minecarts explode but never break blocks.", "It can't hurt the one who threw it."),
        POTATO_A5_STRIKE(Material.BREEZE_ROD, "Potato A5 Strike", "Right-click: launch above the nearest player or mob, then slam down.", "Refused if blocks are above the target or you.", "Passive: Jump Boost I while held in either hand."),
        ENCHANTED_POTATO_A5_STRIKE(Material.BREEZE_ROD, "Enchanted Potato A5 Strike", "A stronger A5 Strike with a shorter cooldown.", "Passive: Jump Boost II while held in either hand."),
        POTATO_BOW(Material.BOW, "Potato Bow", "Arrows from this bow deal bonus damage.", "Passive: Speed I while held in either hand."),
        POTATO_SHIELD(Material.SHIELD, "Potato Shield", "Hold it and a ring of potatoes surrounds you.", "Pushes enemies and mobs back, and halves melee damage from them.", "Passive: Resistance I while held in either hand.");

        private final Material material;
        private final String displayName;
        private final String[] lore;

        Kind(Material material, String displayName, String... lore) {
            this.material = material;
            this.displayName = displayName;
            this.lore = lore;
        }

        public Material material() {
            return material;
        }

        public String displayName() {
            return displayName;
        }

        public String[] lore() {
            return lore;
        }

        /** One line describing this item's second ability. Empty if the item has none. */
        public String abilityLine() {
            return switch (this) {
                case POTATO_OF_LIFE -> "Sneak + right-click: Heal 4 hearts. 60 s cooldown.";
                case GOLDEN_POTATO -> "Sneak + right-click: Golden Burst, healing nearby players and pushing enemies back. 30 s cooldown.";
                case POTATO_SWORD -> "Sneak + right-click: Slash enemies in front of you. 8 s cooldown.";
                case WARRIORS_BLADE -> "Sneak + right-click: Soul Reap, hitting enemies within 9 blocks and healing you. 25 s cooldown.";
                case POTATO_HOE -> "Sneak + right-click: Grow crops within 8 blocks. 30 s cooldown.";
                case POTATO_PICKAXE -> "Sneak + right-click: Vein Vision, showing ores within 12 blocks for 10 s. 30 s cooldown.";
                case POTATO_AXE -> "Sneak + right-click: Whirl, hitting enemies within 5 blocks. 12 s cooldown.";
                case POTATO_SHOVEL -> "Sneak + right-click: Earth Shatter, hitting enemies within 6 blocks. 20 s cooldown.";
                case POTATO_MACE -> "Sneak + right-click: Ground Slam, hitting enemies within 7 blocks hard. 15 s cooldown.";
                case POTATO_SPEAR -> "Sneak + right-click: Lunge forward and hit what you pass. 10 s cooldown.";
                case POTATO_HELMET, POTATO_CHESTPLATE, POTATO_LEGGINGS, POTATO_BOOTS ->
                        "Sneak + F: Thorns Burst, hitting and pushing enemies within 5 blocks. 45 s cooldown.";
                case POTATO_BOW -> "Sneak + right-click: Volley of three arrows. 8 s cooldown.";
                case POTATO_SHIELD -> "Sneak + right-click: Bash enemies within 5 blocks, hitting and pushing them. 12 s cooldown.";
                case POTATO_LANTERN -> "Sneak + right-click: Flare, setting enemies within 16 blocks alight. 30 s cooldown.";
                case POTATO_WAND -> "Sneak + right-click: Potato Nova, a burst of damage around you. 6 s cooldown.";
                case POTATO_CHARM -> "Sneak + right-click: Fortune Drop, dropping a Potato of Life at your feet. 40 s cooldown.";
                case POTATO_WARD -> "Sneak + right-click: Ward Ring, pushing enemies out within 6 blocks. 30 s cooldown.";
                case POTATO_SLIME, POTATO_GHAST, POTATO_BLAZE, POTATO_QUARTZ, POTATO_ECHO -> "";
                case POTATO_COMPASS -> "Sneak + right-click: Lodestone, glowing players within 30 blocks. 25 s cooldown.";
                case POTATO_FEATHER -> "Sneak + right-click: Gust, launching you up and pushing enemies within 4 blocks. 15 s cooldown.";
                case POTATO_SCROLL -> "Sneak + right-click: Reveal, glowing players within 40 blocks and counting mobs. 30 s cooldown.";
                case TUFF_SNOWBALL, POTATO_ORE, POTATO_A5_STRIKE, ENCHANTED_POTATO_A5_STRIKE, POTATO_REROLLER,
                        POTATO_SOUP, POTATO_OVERLORD -> "";
            };
        }

        /** Lore lines, plus the ability line when the item has one. */
        public String[] fullLore() {
            String line = abilityLine();
            if (line.isEmpty()) {
                return lore;
            }
            String[] all = java.util.Arrays.copyOf(lore, lore.length + 1);
            all[lore.length] = line;
            return all;
        }

        /** The two gradient colours for this item's name. */
        public String[] colors() {
            return switch (this) {
                case POTATO_SLIME -> new String[] {"#A5D6A7", "#2E7D32"};
                case POTATO_GHAST -> new String[] {"#F5F5F5", "#9E9E9E"};
                case POTATO_BLAZE -> new String[] {"#FFB74D", "#E65100"};
                case POTATO_QUARTZ -> new String[] {"#FFFFFF", "#BDBDBD"};
                case POTATO_ECHO -> new String[] {"#4DD0E1", "#006064"};
                case POTATO_SOUP -> new String[] {"#FFE0B2", "#D84315"};
                case POTATO_OVERLORD -> new String[] {"#FFD700", "#1A1A1A"};
                case POTATO_COMPASS -> new String[] {"#80DEEA", "#00838F"};
                case POTATO_FEATHER -> new String[] {"#F5F5F5", "#B0BEC5"};
                case POTATO_SCROLL -> new String[] {"#FFE0B2", "#A1887F"};
                case POTATO_REROLLER -> new String[] {"#FF9CF0", "#7D3C98"};
                case POTATO_WAND -> new String[] {"#E0B0FF", "#7B1FA2"};
                case POTATO_CHARM -> new String[] {"#7CFC00", "#00C853"};
                case POTATO_WARD -> new String[] {"#FF7F50", "#FF4500"};
                case POTATO_LANTERN -> new String[] {"#4FC3F7", "#0277BD"};
                case POTATO_OF_LIFE -> new String[] {"#FFE066", "#FFB347"};
                case GOLDEN_POTATO -> new String[] {"#FFD700", "#FFA500"};
                case POTATO_ORE -> new String[] {"#F4C430", "#B8860B"};
                case POTATO_SWORD -> new String[] {"#7FFF00", "#32CD32"};
                case WARRIORS_BLADE -> new String[] {"#FF3B3B", "#8B0000"};
                case POTATO_HOE -> new String[] {"#A0E040", "#228B22"};
                case POTATO_PICKAXE -> new String[] {"#40E0D0", "#1E90FF"};
                case POTATO_AXE -> new String[] {"#87CEFA", "#4169E1"};
                case POTATO_SHOVEL -> new String[] {"#DDA0DD", "#8A2BE2"};
                case POTATO_MACE -> new String[] {"#D0D0D0", "#5F6B7A"};
                case POTATO_SPEAR -> new String[] {"#00FFFF", "#008B8B"};
                case POTATO_HELMET, POTATO_CHESTPLATE, POTATO_LEGGINGS, POTATO_BOOTS ->
                        new String[] {"#B388FF", "#6A1B9A"};
                case TUFF_SNOWBALL -> new String[] {"#B0B0B0", "#707070"};
                case POTATO_A5_STRIKE -> new String[] {"#FF69B4", "#FF1493"};
                case ENCHANTED_POTATO_A5_STRIKE -> new String[] {"#FF00FF", "#8A2BE2"};
                case POTATO_BOW -> new String[] {"#FFD27F", "#FF8C69"};
                case POTATO_SHIELD -> new String[] {"#FFA07A", "#CD5C5C"};
                default -> new String[] {"#FFD86B", "#E07B39"};
            };
        }

        /** The item name in this item's colours, gradient bold italic. */
        public Component displayComponent() {
            String[] c = colors();
            return Text.gradient(displayName, c[0], c[1]);
        }
    }

    private final NamespacedKey kindKey;
    private final NamespacedKey uidKey;
    private final NamespacedKey damageKey;
    private final NamespacedKey speedKey;

    public Items(Plugin plugin) {
        this.kindKey = new NamespacedKey(plugin, "kind");
        this.uidKey = new NamespacedKey(plugin, "uid");
        this.damageKey = new NamespacedKey(plugin, "netherite_damage");
        this.speedKey = new NamespacedKey(plugin, "netherite_speed");
    }

    /** Creates a new item of this kind with a fresh UUID. */
    public ItemStack create(Kind kind) {
        // The Tuff Snowball uses one fixed UUID so that stacks of it still stack together.
        return build(kind, kind == Kind.TUFF_SNOWBALL ? new UUID(0L, 0L) : UUID.randomUUID());
    }

    /** Builds the blade with a known UUID. Only BladeManager.forge should call this. */
    public ItemStack makeBlade(UUID uid) {
        return build(Kind.WARRIORS_BLADE, uid);
    }

    private ItemStack build(Kind kind, UUID uid) {
        ItemStack stack = new ItemStack(kind.material());
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(kind.displayComponent());
        meta.lore(Arrays.stream(kind.fullLore()).map(Text::parse).toList());
        meta.setEnchantmentGlintOverride(true);
        if (kind == Kind.WARRIORS_BLADE) {
            meta.setUnbreakable(true);
        }
        meta.getPersistentDataContainer().set(kindKey, PersistentDataType.STRING, kind.name());
        meta.getPersistentDataContainer().set(uidKey, PersistentDataType.STRING, uid.toString());
        stack.setItemMeta(meta);
        enforce(stack);
        return stack;
    }

    /** Returns the kind of a Potato SMP item, or null for anything else. Null-safe. */
    public Kind kindOf(ItemStack stack) {
        if (stack == null || stack.getType().isAir() || !stack.hasItemMeta()) {
            return null;
        }
        String value = stack.getItemMeta().getPersistentDataContainer().get(kindKey, PersistentDataType.STRING);
        if (value == null) {
            return null;
        }
        try {
            return Kind.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public UUID uidOf(ItemStack stack) {
        if (stack == null || stack.getType().isAir() || !stack.hasItemMeta()) {
            return null;
        }
        String value = stack.getItemMeta().getPersistentDataContainer().get(uidKey, PersistentDataType.STRING);
        if (value == null) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    /** A Potato Sword is valid only with the right material, tag, UUID, and Sharpness VI. */
    public boolean isValidSword(ItemStack stack) {
        Enchantment sharpness = sharpness();
        return stack != null
                && kindOf(stack) == Kind.POTATO_SWORD
                && stack.getType() == Kind.POTATO_SWORD.material()
                && uidOf(stack) != null
                && sharpness != null
                && stack.getEnchantmentLevel(sharpness) == 6;
    }

    /** Forces Sharpness on swords and the blade. Removes every other enchantment from them. */
    public void enforce(ItemStack stack) {
        Kind kind = kindOf(stack);
        Enchantment sharpness = sharpness();
        if (sharpness == null || kind == null) {
            return;
        }
        int level;
        if (kind == Kind.WARRIORS_BLADE) {
            level = 1;
        } else if (kind == Kind.POTATO_SWORD) {
            level = 6;
        } else {
            return;
        }
        ItemMeta meta = stack.getItemMeta();
        for (Enchantment enchantment : new ArrayList<>(meta.getEnchants().keySet())) {
            meta.removeEnchant(enchantment);
        }
        meta.addEnchant(sharpness, level, true);
        if (kind == Kind.WARRIORS_BLADE) {
            meta.setUnbreakable(true);
        }
        stack.setItemMeta(meta);
    }

    public Enchantment sharpness() {
        return Registry.ENCHANTMENT.get(NamespacedKey.minecraft("sharpness"));
    }
}

/**
 * Death counting, Marked status, Potato of Life drops, and Warrior's Blade kills.
 *
 * Kill credit rules, which close the loopholes I can see:
 *  - Only the most recent player hit counts, and any non-entity damage clears it.
 *  - The victim's final damage cause must be a melee or projectile attack. So a player
 *    who was hit and then died from a fall or lava is not credited.
 *  - The hit must be within kill_weapon_window_seconds.
 *  - The weapon used is the attacker's main hand at the moment of the hit, not at death.
 *  - Projectiles credit a kill but never carry a Sword or Blade.
 */
final class CombatListener implements Listener {

    private record Hit(UUID attacker, ItemStack weapon, long at) {
    }

    private final PotatoSMP plugin;
    private final Map<UUID, Hit> lastHit = new HashMap<>();

    public CombatListener(PotatoSMP plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onDamage(EntityDamageEvent event) {
        if (!plugin.isPotatoFeatureEnabled()) {
            return;
        }
        if (!(event.getEntity() instanceof Player victim) || event.isCancelled()) {
            return;
        }
        if (event instanceof EntityDamageByEntityEvent byEntity && isEntityCause(event.getCause())) {
            Player attacker = attackerOf(byEntity.getDamager());
            if (attacker != null && !attacker.equals(victim)) {
                ItemStack weapon = byEntity.getDamager() instanceof Player p
                        ? p.getInventory().getItemInMainHand().clone()
                        : null;
                lastHit.put(victim.getUniqueId(), new Hit(attacker.getUniqueId(), weapon, System.currentTimeMillis()));
                return;
            }
        }
        lastHit.remove(victim.getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent event) {
        if (!plugin.isPotatoFeatureEnabled()) {
            return;
        }
        Player victim = event.getEntity();
        UUID victimId = victim.getUniqueId();
        Hit hit = lastHit.remove(victimId);
        boolean wasMarked = plugin.data().isMarked(victimId);

        int threshold = Math.max(1, plugin.getConfig().getInt("marked_deaths", 5));
        int deaths = plugin.data().addDeath(victimId);
        plugin.streaks().onDeath(victimId);
        if (!wasMarked && deaths >= threshold) {
            plugin.data().setMarked(victimId, victim.getName());
            plugin.broadcast("<dark_red><bold>" + Text.esc(victim.getName()) + "</bold></dark_red>"
                    + "<gray> has become </gray><dark_red>Marked</dark_red><gray> after " + deaths + " deaths.</gray>");
        }

        if (hit == null || !killedByEntity(victim)) {
            return;
        }
        long windowMs = Math.max(1, plugin.getConfig().getInt("kill_weapon_window_seconds", 15)) * 1000L;
        if (System.currentTimeMillis() - hit.at() > windowMs) {
            return;
        }
        Player killer = Bukkit.getPlayer(hit.attacker());
        if (killer == null || !killer.isOnline()) {
            return;
        }

        plugin.streaks().onPlayerKill(killer, victimId);

        if (plugin.getConfig().getBoolean("potato_of_life_on_player_kill", true)) {
            event.getDrops().add(plugin.items().create(Items.Kind.POTATO_OF_LIFE));
        }

        ItemStack weapon = hit.weapon();
        if (plugin.blades().isCurrent(weapon)) {
            plugin.blades().soulBan(victimId, victim.getName());
            plugin.broadcast("<dark_purple>" + Text.esc(victim.getName()) + "</dark_purple>"
                    + "<gray> was soul-banned by </gray><white>" + Text.esc(killer.getName()) + "</white>"
                    + "<gray>'s Warrior's Blade.</gray>");
            String banMessage = plugin.getConfig().getString("ban_message", "You were soul-banned.");
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (victim.isOnline()) {
                    victim.kick(Text.parse(banMessage));
                }
            });
        } else if (wasMarked && plugin.items().isValidSword(weapon)) {
            if (plugin.blades().exists()) {
                killer.sendMessage(Text.msg("<gray>A Warrior's Blade already exists, so nothing was forged."));
            } else {
                ItemStack blade = plugin.blades().forge();
                victim.getWorld().dropItemNaturally(victim.getLocation(), blade);
                plugin.broadcast("<gold><bold>" + Text.esc(killer.getName()) + "</bold></gold>"
                        + "<gray> forged the </gray><gold>Warrior's Blade</gold><gray> from a Marked soul.</gray>");
            }
        }
    }

    private static boolean killedByEntity(Player victim) {
        EntityDamageEvent last = victim.getLastDamageCause();
        return last != null && isEntityCause(last.getCause());
    }

    private static boolean isEntityCause(DamageCause cause) {
        return cause == DamageCause.ENTITY_ATTACK
                || cause == DamageCause.ENTITY_SWEEP_ATTACK
                || cause == DamageCause.PROJECTILE;
    }

    private static Player attackerOf(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player shooter) {
            return shooter;
        }
        return null;
    }
}

/**
 * Anti-dupe and item protection: validates inventories, pickups, and chunk loads, blocks
 * creative cloning, and stops the blade from being enchanted, repaired, renamed, ground, or
 * smithed. It also destroys the blade when its item is burned, lava'd, or despawned, which
 * frees every soul it held.
 */
final class GuardListener implements Listener {

    private static final String CUSTOM_BLOCKED = "<red>That Potato SMP item can't be used in this block.";

    private final PotatoSMP plugin;

    public GuardListener(PotatoSMP plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!plugin.isPotatoFeatureEnabled()) {
            return;
        }
        plugin.blades().validateInventory(event.getPlayer().getInventory());
    }

    /** Soul-banned players can't log back in until the blade is destroyed. */
    @EventHandler
    public void onLogin(PlayerLoginEvent event) {
        if (!plugin.isPotatoFeatureEnabled()) {
            return;
        }
        if (plugin.blades().isSoulBanned(event.getPlayer().getUniqueId())) {
            String message = plugin.getConfig().getString("ban_message", "You were soul-banned.");
            event.disallow(PlayerLoginEvent.Result.KICK_BANNED, Text.parse(message));
        }
    }

    @EventHandler
    public void onCreative(InventoryCreativeEvent event) {
        if (!plugin.isPotatoFeatureEnabled()) {
            return;
        }
        if (isCustom(event.getCursor()) || isCustom(event.getCurrentItem())) {
            event.setCancelled(true);
            event.getWhoClicked().sendMessage(Text.msg("<red>Potato SMP items can't be taken from creative."));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (!plugin.activeFor(event.getWhoClicked())) {
            return;
        }
        InventoryType top = event.getView().getTopInventory().getType();
        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();

        if (top == InventoryType.ANVIL || top == InventoryType.GRINDSTONE || top == InventoryType.SMITHING
                || top == InventoryType.ENCHANTING || top == InventoryType.CARTOGRAPHY || top == InventoryType.LOOM) {
            if (top == InventoryType.SMITHING
                    ? blockedInSmithing(current) || blockedInSmithing(cursor)
                    : isCustom(current) || isCustom(cursor)) {
                event.setCancelled(true);
                event.getWhoClicked().sendMessage(Text.msg(CUSTOM_BLOCKED));
                return;
            }
        }
        if (current != null && !current.getType().isAir() && !plugin.blades().sanitize(current)) {
            event.setCancelled(true);
            event.setCurrentItem(null);
        }
        if (cursor != null && !cursor.getType().isAir() && !plugin.blades().sanitize(cursor)) {
            event.setCancelled(true);
            event.setCursor(null);
        }
    }

    @EventHandler
    public void onPrepareEnchant(PrepareItemEnchantEvent event) {
        if (!plugin.isPotatoFeatureEnabled()) {
            return;
        }
        if (isCustom(event.getItem())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEnchant(EnchantItemEvent event) {
        if (!plugin.isPotatoFeatureEnabled()) {
            return;
        }
        if (isCustom(event.getItem())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onAnvil(PrepareAnvilEvent event) {
        if (!plugin.isPotatoFeatureEnabled()) {
            return;
        }
        if (anyCustom(event.getInventory().getContents()) || isCustom(event.getResult())) {
            event.setResult(null);
        }
    }

    /**
     * Potato armor is allowed in a smithing table only when the addition is Potato Ore and
     * the base is plain Diamond armor. Anything else custom makes the result null.
     */
    @EventHandler
    public void onSmithing(PrepareSmithingEvent event) {
        if (!plugin.isPotatoFeatureEnabled()) {
            return;
        }
        ItemStack base = event.getInventory().getInputEquipment();
        ItemStack addition = event.getInventory().getInputMineral();
        ItemStack result = event.getResult();
        boolean potatoArmor = addition != null
                && addition.getType() == org.bukkit.Material.NETHERITE_INGOT
                && plugin.items().kindOf(addition) == null
                && plugin.items().kindOf(base) == null
                && base != null
                && base.getType().name().startsWith("DIAMOND_")
                && isArmorKind(plugin.items().kindOf(result));
        if (potatoArmor) {
            return;
        }
        if (anyCustom(event.getInventory().getContents()) || isCustom(result)) {
            event.setResult(null);
        }
    }

    private boolean blockedInSmithing(ItemStack stack) {
        Items.Kind kind = plugin.items().kindOf(stack);
        return kind != null && kind != Items.Kind.POTATO_ORE && !isArmorKind(kind);
    }

    private static boolean isArmorKind(Items.Kind kind) {
        return kind == Items.Kind.POTATO_HELMET || kind == Items.Kind.POTATO_CHESTPLATE
                || kind == Items.Kind.POTATO_LEGGINGS || kind == Items.Kind.POTATO_BOOTS;
    }

    @EventHandler
    public void onHopperPickup(InventoryPickupItemEvent event) {
        if (!plugin.isPotatoFeatureEnabled()) {
            return;
        }
        if (!validItem(event.getItem())) {
            event.setCancelled(true);
            event.getItem().remove();
        }
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (!plugin.isPotatoFeatureEnabled()) {
            return;
        }
        if (!validItem(event.getItem())) {
            event.setCancelled(true);
            event.getItem().remove();
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        if (!plugin.isPotatoFeatureEnabled()) {
            return;
        }
        Chunk chunk = event.getChunk();
        for (Entity entity : chunk.getEntities()) {
            if (entity instanceof Item item && !validItem(item)) {
                item.remove();
            }
        }
    }

    @EventHandler
    public void onDespawn(ItemDespawnEvent event) {
        if (!plugin.isPotatoFeatureEnabled()) {
            return;
        }
        if (event.getEntity() instanceof Item item && plugin.blades().isCurrent(item.getItemStack())) {
            plugin.blades().destroy("its item despawned");
        }
    }

    /**
     * Fire, lava, explosions, and cactus can destroy a dropped item. The check runs two ticks
     * later, after the server has removed the entity, so the blade is only destroyed if it
     * really is gone.
     */
    @EventHandler
    public void onItemDamage(EntityDamageEvent event) {
        if (!plugin.isPotatoFeatureEnabled()) {
            return;
        }
        if (!(event.getEntity() instanceof Item item) || event.isCancelled()) {
            return;
        }
        if (!plugin.blades().isCurrent(item.getItemStack())) {
            return;
        }
        String cause = event.getCause().name().toLowerCase(Locale.ROOT).replace('_', ' ');
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!item.isValid()) {
                plugin.blades().destroy("burned or destroyed by " + cause);
            }
        }, 2L);
    }

    private boolean validItem(Item item) {
        ItemStack stack = item.getItemStack();
        if (!plugin.blades().sanitize(stack)) {
            return false;
        }
        item.setItemStack(stack);
        return true;
    }

    private boolean isCustom(ItemStack stack) {
        return plugin.items().kindOf(stack) != null;
    }

    private boolean anyCustom(ItemStack[] stacks) {
        for (ItemStack stack : stacks) {
            if (isCustom(stack)) {
                return true;
            }
        }
        return false;
    }
}

/**
 * Our items never hurt the player who owns them. A projectile or falling block tagged with its
 * owner's UUID can't damage that owner. A5 Strike slams don't cause fall damage to the user.
 */
final class OwnerGuard implements Listener {

    private final PotatoSMP plugin;
    private final NamespacedKey ownerKey;

    public OwnerGuard(PotatoSMP plugin) {
        this.plugin = plugin;
        this.ownerKey = new NamespacedKey(plugin, "tuff_thrower");
    }

    @EventHandler
    public void onOwnedDamage(EntityDamageByEntityEvent event) {
        if (!plugin.activeFor(event.getDamager())) {
            return;
        }
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        if (isOwnedBy(event.getDamager(), victim.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onSlamFall(EntityDamageEvent event) {
        if (!plugin.isPotatoFeatureEnabled()) {
            return;
        }
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL
                && event.getEntity() instanceof Player player
                && plugin.a5().isSlamming(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    private boolean isOwnedBy(Entity entity, UUID owner) {
        String tag = entity.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
        return tag != null && tag.equals(owner.toString());
    }
}

/**
 * Checks every crafting preview. For a Potato SMP recipe, each slot must match its rule.
 * For any vanilla recipe, no Potato SMP item is allowed in the grid. A failed check clears
 * the result, so nothing is consumed and nothing is produced.
 */
final class RecipeGuard implements Listener {

    private final PotatoSMP plugin;

    public RecipeGuard(PotatoSMP plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        if (!plugin.activeFor(event.getView().getPlayer())) {
            event.getInventory().setResult(null);
            return;
        }
        Recipe recipe = event.getRecipe();
        Map<Material, Items.Kind> rule = null;
        if (recipe instanceof Keyed keyed) {
            rule = plugin.recipes().ruleFor(keyed.getKey());
        }
        if (!matches(event.getInventory().getMatrix(), rule)) {
            event.getInventory().setResult(null);
        }
    }

    private boolean matches(ItemStack[] matrix, Map<Material, Items.Kind> rule) {
        for (ItemStack stack : matrix) {
            if (stack == null || stack.getType().isAir()) {
                continue;
            }
            Items.Kind kind = plugin.items().kindOf(stack);
            if (rule == null || !rule.containsKey(stack.getType())) {
                if (kind != null) {
                    return false;
                }
                continue;
            }
            if (kind != rule.get(stack.getType())) {
                return false;
            }
        }
        return true;
    }
}

/**
 * Potato SMP recipes. Each crafting recipe has a rule: the material each slot must use, and
 * the custom kind it must carry, with null meaning a plain untagged item. RecipeGuard checks
 * the rule on every craft preview, so a Potato of Life can't stand in for a Golden Potato.
 */
final class PotatoRecipes {

    private final PotatoSMP plugin;
    private final Items items;
    private final Map<NamespacedKey, Map<Material, Items.Kind>> rules = new HashMap<>();
    private final List<NamespacedKey> keys = new ArrayList<>();

    public PotatoRecipes(PotatoSMP plugin, Items items) {
        this.plugin = plugin;
        this.items = items;
    }

    public Map<Material, Items.Kind> ruleFor(NamespacedKey key) {
        return rules.get(key);
    }

    /** One recipe as it should appear in the recipe book: a 3x3 grid, the symbols, and the result. */
    record RecipeView(Items.Kind result, String[] rows, Map<Character, ItemStack> symbols) {
    }

    private final List<RecipeView> views = new ArrayList<>();

    public List<RecipeView> views() {
        return views;
    }

    private ItemStack displayItem(Material material, Map<Material, Items.Kind> rule) {
        Items.Kind kind = rule == null ? null : rule.get(material);
        return kind != null ? items.create(kind) : new ItemStack(material);
    }

    public void register() {
        views.clear();
        // Golden Potato: 4 gold nuggets + 1 plain potato. Cheaper than a golden apple (8 ingots).
        shapeless("golden_potato", Items.Kind.GOLDEN_POTATO,
                rule(Material.GOLD_NUGGET, null, Material.POTATO, null),
                Material.GOLD_NUGGET, Material.GOLD_NUGGET, Material.GOLD_NUGGET, Material.GOLD_NUGGET,
                Material.POTATO);

        // Potato Ore: 5 netherite scrap + 4 Golden Potatoes.
        shapeless("potato_ore", Items.Kind.POTATO_ORE,
                rule(Material.NETHERITE_SCRAP, null, Material.POTATO, Items.Kind.GOLDEN_POTATO),
                Material.NETHERITE_SCRAP, Material.NETHERITE_SCRAP, Material.NETHERITE_SCRAP,
                Material.NETHERITE_SCRAP, Material.NETHERITE_SCRAP,
                Material.POTATO, Material.POTATO, Material.POTATO, Material.POTATO);

        // Potato Mace: heavy core + potato ore.
        shapeless("potato_mace", Items.Kind.POTATO_MACE,
                rule(Material.HEAVY_CORE, null, Material.RAW_GOLD, Items.Kind.POTATO_ORE),
                Material.HEAVY_CORE, Material.RAW_GOLD);

        String[] spear = {"  O", " S ", "S  "};
        shaped("potato_spear", Items.Kind.POTATO_SPEAR,
                rule(Material.RAW_GOLD, Items.Kind.POTATO_ORE, Material.NETHERITE_INGOT, null), spear,
                Map.of('O', Material.RAW_GOLD, 'S', Material.NETHERITE_INGOT));

        String[] hoe = {"OO", " S", " S"};
        shaped("potato_hoe", Items.Kind.POTATO_HOE,
                rule(Material.RAW_GOLD, Items.Kind.POTATO_ORE, Material.NETHERITE_INGOT, null), hoe,
                Map.of('O', Material.RAW_GOLD, 'S', Material.NETHERITE_INGOT));

        String[] pickaxe = {"OOO", " S ", " S "};
        shaped("potato_pickaxe", Items.Kind.POTATO_PICKAXE,
                rule(Material.RAW_GOLD, Items.Kind.POTATO_ORE, Material.NETHERITE_INGOT, null), pickaxe,
                Map.of('O', Material.RAW_GOLD, 'S', Material.NETHERITE_INGOT));

        String[] axe = {"OO", "OS", " S"};
        shaped("potato_axe", Items.Kind.POTATO_AXE,
                rule(Material.RAW_GOLD, Items.Kind.POTATO_ORE, Material.NETHERITE_INGOT, null), axe,
                Map.of('O', Material.RAW_GOLD, 'S', Material.NETHERITE_INGOT));

        String[] shovel = {" O", " S", " S"};
        shaped("potato_shovel", Items.Kind.POTATO_SHOVEL,
                rule(Material.RAW_GOLD, Items.Kind.POTATO_ORE, Material.NETHERITE_INGOT, null), shovel,
                Map.of('O', Material.RAW_GOLD, 'S', Material.NETHERITE_INGOT));

        // Armor is made at a smithing table: diamond piece + potato ore + netherite template.
        // Potato Wand: blaze rod + potato ore.
        shapeless("potato_wand", Items.Kind.POTATO_WAND,
                rule(Material.BLAZE_ROD, null, Material.RAW_GOLD, Items.Kind.POTATO_ORE),
                Material.BLAZE_ROD, Material.RAW_GOLD);

        // Potato Charm: emerald + potato ore.
        shapeless("potato_charm", Items.Kind.POTATO_CHARM,
                rule(Material.EMERALD, null, Material.RAW_GOLD, Items.Kind.POTATO_ORE),
                Material.EMERALD, Material.RAW_GOLD);

        // Potato Ward: nautilus shell + potato ore + netherite ingot.
        shapeless("potato_ward", Items.Kind.POTATO_WARD,
                rule(Material.NAUTILUS_SHELL, null, Material.RAW_GOLD, Items.Kind.POTATO_ORE,
                        Material.NETHERITE_INGOT, null),
                Material.NAUTILUS_SHELL, Material.RAW_GOLD, Material.NETHERITE_INGOT);

        // Potato Lantern: glow ink sac + potato ore.
        shapeless("potato_lantern", Items.Kind.POTATO_LANTERN,
                rule(Material.GLOW_INK_SAC, null, Material.RAW_GOLD, Items.Kind.POTATO_ORE),
                Material.GLOW_INK_SAC, Material.RAW_GOLD);

        // Potato Slime, Ghast, Blaze, Quartz, and Echo: the base item + potato ore.
        shapeless("potato_slime", Items.Kind.POTATO_SLIME,
                rule(Material.SLIME_BALL, null, Material.RAW_GOLD, Items.Kind.POTATO_ORE),
                Material.SLIME_BALL, Material.RAW_GOLD);
        shapeless("potato_ghast", Items.Kind.POTATO_GHAST,
                rule(Material.GHAST_TEAR, null, Material.RAW_GOLD, Items.Kind.POTATO_ORE),
                Material.GHAST_TEAR, Material.RAW_GOLD);
        shapeless("potato_blaze", Items.Kind.POTATO_BLAZE,
                rule(Material.BLAZE_POWDER, null, Material.RAW_GOLD, Items.Kind.POTATO_ORE),
                Material.BLAZE_POWDER, Material.RAW_GOLD);
        shapeless("potato_quartz", Items.Kind.POTATO_QUARTZ,
                rule(Material.QUARTZ, null, Material.RAW_GOLD, Items.Kind.POTATO_ORE),
                Material.QUARTZ, Material.RAW_GOLD);
        shapeless("potato_echo", Items.Kind.POTATO_ECHO,
                rule(Material.ECHO_SHARD, null, Material.RAW_GOLD, Items.Kind.POTATO_ORE),
                Material.ECHO_SHARD, Material.RAW_GOLD);

        // Potato Soup: 3 plain potatoes + a bowl.
        shapeless("potato_soup", Items.Kind.POTATO_SOUP,
                rule(Material.POTATO, null, Material.BOWL, null),
                Material.POTATO, Material.POTATO, Material.POTATO, Material.BOWL);

        // Potato Compass, Feather, and Scroll: the base item + potato ore.
        shapeless("potato_compass", Items.Kind.POTATO_COMPASS,
                rule(Material.COMPASS, null, Material.RAW_GOLD, Items.Kind.POTATO_ORE),
                Material.COMPASS, Material.RAW_GOLD);
        shapeless("potato_feather", Items.Kind.POTATO_FEATHER,
                rule(Material.FEATHER, null, Material.RAW_GOLD, Items.Kind.POTATO_ORE),
                Material.FEATHER, Material.RAW_GOLD);
        shapeless("potato_scroll", Items.Kind.POTATO_SCROLL,
                rule(Material.PAPER, null, Material.RAW_GOLD, Items.Kind.POTATO_ORE),
                Material.PAPER, Material.RAW_GOLD);

        // Potato Bow: 3 potato ore + 3 string.
        String[] bow = {" OS", "O S", " OS"};
        shaped("potato_bow", Items.Kind.POTATO_BOW,
                rule(Material.RAW_GOLD, Items.Kind.POTATO_ORE, Material.STRING, null), bow,
                Map.of('O', Material.RAW_GOLD, 'S', Material.STRING));

        // Potato Shield: 3 potato ore + 3 netherite ingots + a plain shield.
        String[] shield = {"ONO", "NSN", " O "};
        shaped("potato_shield", Items.Kind.POTATO_SHIELD,
                rule(Material.RAW_GOLD, Items.Kind.POTATO_ORE, Material.NETHERITE_INGOT, null, Material.SHIELD, null),
                shield, Map.of('O', Material.RAW_GOLD, 'N', Material.NETHERITE_INGOT, 'S', Material.SHIELD));

        // Potato A5 Strike: breeze rod + potato ore.
        shapeless("potato_a5_strike", Items.Kind.POTATO_A5_STRIKE,
                rule(Material.BREEZE_ROD, null, Material.RAW_GOLD, Items.Kind.POTATO_ORE),
                Material.BREEZE_ROD, Material.RAW_GOLD);

        // Enchanted A5 Strike: a normal A5 Strike + netherite ingot + golden potato.
        shapeless("enchanted_potato_a5_strike", Items.Kind.ENCHANTED_POTATO_A5_STRIKE,
                rule(Material.BREEZE_ROD, Items.Kind.POTATO_A5_STRIKE, Material.NETHERITE_INGOT, null,
                        Material.POTATO, Items.Kind.GOLDEN_POTATO),
                Material.BREEZE_ROD, Material.NETHERITE_INGOT, Material.POTATO);

        // Tuff Snowball: 8 tuff + 1 snowball.
        shapeless("tuff_snowball", Items.Kind.TUFF_SNOWBALL,
                rule(Material.TUFF, null, Material.SNOWBALL, null),
                Material.TUFF, Material.TUFF, Material.TUFF, Material.TUFF,
                Material.TUFF, Material.TUFF, Material.TUFF, Material.TUFF, Material.SNOWBALL);

        smithingArmor("potato_helmet", Items.Kind.POTATO_HELMET, Material.DIAMOND_HELMET);
        smithingArmor("potato_chestplate", Items.Kind.POTATO_CHESTPLATE, Material.DIAMOND_CHESTPLATE);
        smithingArmor("potato_leggings", Items.Kind.POTATO_LEGGINGS, Material.DIAMOND_LEGGINGS);
        smithingArmor("potato_boots", Items.Kind.POTATO_BOOTS, Material.DIAMOND_BOOTS);
    }

    public void unregister() {
        for (NamespacedKey key : keys) {
            Bukkit.removeRecipe(key);
        }
        keys.clear();
        rules.clear();
    }

    private void shapeless(String id, Items.Kind result, Map<Material, Items.Kind> rule, Material... ingredients) {
        Map<Material, Character> symbolFor = new HashMap<>();
        Map<Character, ItemStack> symbols = new HashMap<>();
        char[][] grid = {{' ', ' ', ' '}, {' ', ' ', ' '}, {' ', ' ', ' '}};
        char next = 'a';
        int index = 0;
        for (Material material : ingredients) {
            Character symbol = symbolFor.get(material);
            if (symbol == null) {
                symbol = next++;
                symbolFor.put(material, symbol);
                symbols.put(symbol, displayItem(material, rule));
            }
            grid[index / 3][index % 3] = symbol;
            index++;
        }
        views.add(new RecipeView(result, new String[] {new String(grid[0]), new String(grid[1]), new String(grid[2])}, symbols));
        NamespacedKey key = new NamespacedKey(plugin, id);
        ShapelessRecipe recipe = new ShapelessRecipe(key, items.create(result));
        for (Material material : ingredients) {
            recipe.addIngredient(material);
        }
        add(key, recipe, rule);
    }

    private void shaped(String id, Items.Kind result, Map<Material, Items.Kind> rule, String[] rows,
                        Map<Character, Material> ingredients) {
        Map<Character, ItemStack> symbols = new HashMap<>();
        for (Map.Entry<Character, Material> entry : ingredients.entrySet()) {
            symbols.put(entry.getKey(), displayItem(entry.getValue(), rule));
        }
        views.add(new RecipeView(result, rows, symbols));
        NamespacedKey key = new NamespacedKey(plugin, id);
        ShapedRecipe recipe = new ShapedRecipe(key, items.create(result));
        recipe.shape(rows);
        for (Map.Entry<Character, Material> entry : ingredients.entrySet()) {
            recipe.setIngredient(entry.getKey(), entry.getValue());
        }
        add(key, recipe, rule);
    }

    private void smithingArmor(String id, Items.Kind result, Material diamondPiece) {
        NamespacedKey key = new NamespacedKey(plugin, id);
        SmithingTransformRecipe recipe = new SmithingTransformRecipe(key, items.create(result),
                new RecipeChoice.MaterialChoice(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
                new RecipeChoice.MaterialChoice(diamondPiece),
                new RecipeChoice.MaterialChoice(Material.NETHERITE_INGOT),
                false);
        if (Bukkit.addRecipe(recipe)) {
            keys.add(key);
        } else {
            plugin.getLogger().warning("Could not register recipe " + id);
        }
    }

    private void add(NamespacedKey key, org.bukkit.inventory.Recipe recipe, Map<Material, Items.Kind> rule) {
        if (Bukkit.addRecipe(recipe)) {
            keys.add(key);
            rules.put(key, rule);
        } else {
            plugin.getLogger().warning("Could not register recipe " + key.getKey());
        }
    }

    /** Builds a rule from alternating material/kind pairs. A null kind means plain, untagged. */
    private static Map<Material, Items.Kind> rule(Object... pairs) {
        Map<Material, Items.Kind> map = new HashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put((Material) pairs[i], (Items.Kind) pairs[i + 1]);
        }
        return map;
    }
}

/**
 * The Tuff Snowball. Thrown, it flies for a moment, then a rain of TNT minecarts, anvils,
 * and pointed dripstone falls from above where it stopped.
 *
 * Rules:
 *  - TNT minecarts never break blocks. Vanilla explosions on them are cancelled, and on
 *    landing they deal their own area damage, falling off with distance from the centre.
 *  - Anvils and dripstone are falling blocks that don't drop items. They hurt entities as
 *    vanilla falling blocks do.
 *  - The thrower takes no damage from any of it. Falling blocks are tagged with the thrower's
 *    UUID and their hits on that player are cancelled. Blasts skip the thrower.
 */
final class TuffSnowballManager implements Listener {

    private final PotatoSMP plugin;
    private final NamespacedKey throwerKey;
    private final NamespacedKey cartMarkerKey;

    public TuffSnowballManager(PotatoSMP plugin) {
        this.plugin = plugin;
        this.throwerKey = new NamespacedKey(plugin, "tuff_thrower");
        this.cartMarkerKey = new NamespacedKey(plugin, "tuff_cart");
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (!plugin.activeFor(event.getPlayer())) {
            return;
        }
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (plugin.items().kindOf(event.getItem()) != Items.Kind.TUFF_SNOWBALL) {
            return;
        }
        event.setCancelled(true);

        Player player = event.getPlayer();
        if (!player.hasPermission("potato.use")) {
            return;
        }
        ItemStack hand = player.getInventory().getItemInMainHand();
        ItemStack remaining = hand.clone();
        remaining.setAmount(hand.getAmount() - 1);
        player.getInventory().setItemInMainHand(remaining.getAmount() > 0 ? remaining : null);

        double speed = plugin.getConfig().getDouble("tuff_snowball.throw_speed", 2.2);
        Vector velocity = player.getEyeLocation().getDirection().multiply(speed);
        Snowball ball = player.launchProjectile(Snowball.class, velocity);
        ball.getPersistentDataContainer().set(throwerKey, PersistentDataType.STRING, player.getUniqueId().toString());
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_SNOWBALL_THROW, 1.0f, 0.6f);
        track(ball, player.getUniqueId());
    }

    /** Watches the snowball each tick. It releases the rain after its flight time or when it lands. */
    private void track(Snowball ball, UUID thrower) {
        int releaseTicks = Math.max(1, ticks(plugin.getConfig(), "tuff_snowball.release_seconds", 1.0));
        new BukkitRunnable() {
            private int age = 0;
            private Location last = ball.getLocation();

            @Override
            public void run() {
                if (!ball.isValid()) {
                    cancel();
                    release(last, thrower);
                    return;
                }
                last = ball.getLocation();
                ball.getWorld().spawnParticle(Particle.SNOWFLAKE, last, 4, 0.15, 0.15, 0.15, 0.01);
                age++;
                if (age >= releaseTicks) {
                    cancel();
                    ball.remove();
                    release(last, thrower);
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private void release(Location center, UUID thrower) {
        if (center == null || center.getWorld() == null) {
            return;
        }
        FileConfiguration cfg = plugin.getConfig();
        int rainTicks = ticks(cfg, "tuff_snowball.rain_seconds", 3.0);
        int interval = Math.max(1, cfg.getInt("tuff_snowball.rain_interval_ticks", 4));
        center.getWorld().playSound(center, Sound.ENTITY_TNT_PRIMED, 1.0f, 0.8f);
        new BukkitRunnable() {
            private int elapsed = 0;

            @Override
            public void run() {
                if (elapsed >= rainTicks) {
                    cancel();
                    return;
                }
                spawnPayload(center, thrower);
                elapsed += interval;
            }
        }.runTaskTimer(plugin, 0L, interval);
    }

    private void spawnPayload(Location center, UUID thrower) {
        FileConfiguration cfg = plugin.getConfig();
        double radius = cfg.getDouble("tuff_snowball.rain_radius", 6);
        double height = cfg.getDouble("tuff_snowball.rain_height", 20);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        Location spawn = center.clone().add(
                (random.nextDouble() * 2.0 - 1.0) * radius, height, (random.nextDouble() * 2.0 - 1.0) * radius);

        center.getWorld().spawnParticle(Particle.CLOUD, spawn, 8, 0.5, 0.5, 0.5, 0.02);
        int tnt = Math.max(0, cfg.getInt("tuff_snowball.weights.tnt_minecart", 50));
        int anvil = Math.max(0, cfg.getInt("tuff_snowball.weights.anvil", 25));
        int dripstone = Math.max(0, cfg.getInt("tuff_snowball.weights.pointed_dripstone", 25));
        int total = Math.max(1, tnt + anvil + dripstone);
        int roll = random.nextInt(total);

        if (roll < tnt) {
            spawnCart(spawn, thrower);
        } else if (roll < tnt + anvil) {
            spawnFalling(spawn, Material.ANVIL, thrower,
                    (float) cfg.getDouble("tuff_snowball.anvil_damage_per_block", 2.0),
                    cfg.getInt("tuff_snowball.anvil_max_damage", 40));
        } else {
            spawnFalling(spawn, Material.POINTED_DRIPSTONE, thrower,
                    (float) cfg.getDouble("tuff_snowball.dripstone_damage_per_block", 2.0),
                    cfg.getInt("tuff_snowball.dripstone_max_damage", 20));
        }
    }

    private void spawnCart(Location spawn, UUID thrower) {
        ExplosiveMinecart cart = spawn.getWorld().spawn(spawn, ExplosiveMinecart.class);
        cart.getPersistentDataContainer().set(throwerKey, PersistentDataType.STRING, thrower.toString());
        cart.getPersistentDataContainer().set(cartMarkerKey, PersistentDataType.BYTE, (byte) 1);
        int lifespan = ticks(plugin.getConfig(), "tuff_snowball.lifespan_seconds", 15.0);
        new BukkitRunnable() {
            private int age = 0;

            @Override
            public void run() {
                age++;
                if (!cart.isValid()) {
                    cancel();
                    return;
                }
                if (age >= lifespan) {
                    cart.remove();
                    cancel();
                    return;
                }
                if (age >= 2 && cart.isOnGround()) {
                    Location where = cart.getLocation();
                    float power = (float) plugin.getConfig().getDouble("tuff_snowball.blast_power", 3.0);
                    // Real explosion, but no block damage. The cart is the source, so the thrower
                    // is protected by OwnerGuard.
                    where.getWorld().createExplosion(cart, where, power, false, false);
                    cart.remove();
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private void spawnFalling(Location spawn, Material material, UUID thrower, float damagePerBlock, int maxDamage) {
        FallingBlock block = spawn.getWorld().spawnFallingBlock(spawn, material.createBlockData());
        block.setDropItem(false);
        block.setHurtEntities(true);
        block.setDamagePerBlock(damagePerBlock);
        block.setMaxDamage(maxDamage);
        block.getPersistentDataContainer().set(throwerKey, PersistentDataType.STRING, thrower.toString());
        World world = spawn.getWorld();
        new BukkitRunnable() {
            private int age = 0;

            @Override
            public void run() {
                age++;
                if (!block.isValid()) {
                    cancel();
                    return;
                }
                if ((age >= 2 && block.isOnGround()) || age > 200) {
                    Location at = block.getLocation();
                    world.spawnParticle(Particle.CLOUD, at, 14, 0.4, 0.2, 0.4, 0.02);
                    world.spawnParticle(Particle.CRIT, at, 10, 0.3, 0.2, 0.3, 0.05);
                    world.playSound(at, Sound.BLOCK_STONE_BREAK, 1.0f, 0.8f);
                    block.remove();
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    /** Area damage for a landed TNT minecart. Blocks are never changed. The thrower is skipped. */
    private void blast(Location center, UUID thrower) {
        FileConfiguration cfg = plugin.getConfig();
        double radius = Math.max(0.5, cfg.getDouble("tuff_snowball.blast_radius", 4));
        double maxDamage = cfg.getDouble("tuff_snowball.blast_max_damage", 20);
        World world = center.getWorld();
        if (world == null) {
            return;
        }
        world.spawnParticle(Particle.EXPLOSION, center, 1);
        world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);

        // No damage source: rain and blast kills are never credited to the thrower.
        for (LivingEntity target : world.getNearbyLivingEntities(center, radius)) {
            if (target.getUniqueId().equals(thrower)) {
                continue;
            }
            double distance = target.getLocation().distance(center);
            double damage = maxDamage * (1.0 - distance / radius);
            if (damage <= 0) {
                continue;
            }
            target.damage(damage);
        }
    }

    /** Falling blocks thrown by a player can't hurt that player. */
    @EventHandler
    public void onFallingHit(EntityDamageByEntityEvent event) {
        if (!plugin.activeFor(event.getDamager())) {
            return;
        }
        if (!(event.getDamager() instanceof FallingBlock block)) {
            return;
        }
        String owner = block.getPersistentDataContainer().get(throwerKey, PersistentDataType.STRING);
        if (owner != null && owner.equals(event.getEntity().getUniqueId().toString())) {
            event.setCancelled(true);
        }
    }

    /** Anvils and dripstone vanish when they land: no block is placed and no item is left. */
    @EventHandler
    public void onFallingLand(EntityChangeBlockEvent event) {
        if (!plugin.isPotatoFeatureEnabled()) {
            return;
        }
        if (event.getEntity() instanceof FallingBlock block
                && block.getPersistentDataContainer().has(throwerKey, PersistentDataType.STRING)) {
            event.setCancelled(true);
            block.remove();
        }
    }

    /** TNT minecarts from the rain never explode vanilla-style, so they never break blocks. */
    @EventHandler
    public void onCartExplode(EntityExplodeEvent event) {
        if (!plugin.isPotatoFeatureEnabled()) {
            return;
        }
        if (event.getEntity().getPersistentDataContainer().has(cartMarkerKey, PersistentDataType.BYTE)) {
            event.blockList().clear();
        }
    }

    private static int ticks(FileConfiguration cfg, String path, double defaultSeconds) {
        return (int) Math.round(cfg.getDouble(path, defaultSeconds) * 20.0);
    }

    private static int ticks(org.bukkit.plugin.Plugin plugin, String path, double defaultSeconds) {
        return ticks(plugin.getConfig(), path, defaultSeconds);
    }
}

/**
 * Kill streaks: consecutive player kills without dying. Reaching a tier grants a regeneration
 * buff and a glowing debuff, and the server is told about it. Death resets the streak.
 *
 * Farming guard: killing the same player again within victim_repeat_cooldown_seconds does
 * not count. The last-victim memory is in-memory only, so a restart resets it.
 */
final class StreakManager {

    private record Last(UUID victim, long at) {
    }

    private final PotatoSMP plugin;
    private final Map<UUID, Last> lastVictim = new HashMap<>();

    public StreakManager(PotatoSMP plugin) {
        this.plugin = plugin;
    }

    public void onPlayerKill(Player killer, UUID victim) {
        UUID killerId = killer.getUniqueId();
        long now = System.currentTimeMillis();
        long cooldownMs = Math.max(0, plugin.getConfig().getLong("streaks.victim_repeat_cooldown_seconds", 300)) * 1000L;
        Last last = lastVictim.get(killerId);
        if (last != null && last.victim().equals(victim) && now - last.at() < cooldownMs) {
            return;
        }
        lastVictim.put(killerId, new Last(victim, now));

        int streak = plugin.data().streak(killerId) + 1;
        plugin.data().setStreak(killerId, streak);

        ConfigurationSection tiers = plugin.getConfig().getConfigurationSection("streaks.tiers");
        if (tiers == null) {
            return;
        }
        ConfigurationSection tier = tiers.getConfigurationSection(String.valueOf(streak));
        if (tier != null) {
            applyTier(killer, streak, tier);
        }
    }

    public void onDeath(UUID victim) {
        lastVictim.remove(victim);
        if (plugin.data().streak(victim) > 0) {
            plugin.data().setStreak(victim, 0);
        }
    }

    private void applyTier(Player killer, int streak, ConfigurationSection tier) {
        int regenLevel = Math.max(1, tier.getInt("buff.regeneration_level", 1));
        int regenSeconds = Math.max(0, tier.getInt("buff.regeneration_seconds", 20));
        int glowSeconds = Math.max(0, tier.getInt("debuff.glowing_seconds", 60));
        if (regenSeconds > 0) {
            killer.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, regenSeconds * 20,
                    regenLevel - 1, false, true, true));
        }
        if (glowSeconds > 0) {
            killer.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, glowSeconds * 20,
                    0, false, true, true));
        }
        plugin.broadcast("<gold>" + Text.esc(killer.getName()) + "</gold><gray> is on a </gray><gold>"
                + streak + " kill streak</gold><gray>.</gray>");
    }
}

/** MiniMessage helpers. Headings and names use the gradient, bold, italic style. */
final class Text {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final String GRAD_OPEN = "<gradient:#FFD86B:#E07B39><bold><italic>";
    private static final String GRAD_CLOSE = "</italic></bold></gradient>";
    private static final String PREFIX = GRAD_OPEN + "[Potato SMP]" + GRAD_CLOSE;

    private Text() {
    }

    /** Escapes text so player names and other user input can't inject MiniMessage tags. */
    public static String esc(String raw) {
        return MM.escapeTags(raw);
    }

    /** Gradient bold italic in the two given colours. */
    public static Component gradient(String text, String from, String to) {
        return MM.deserialize("<gradient:" + from + ":" + to + "><bold><italic>" + esc(text)
                + "</italic></bold></gradient>");
    }

    /** Item name in its own colours, then the cooldown in plain text. */
    public static Component itemCooldown(Items.Kind kind, long seconds) {
        return kind.displayComponent().append(parse(" <gray>ready in <white>" + seconds + "s</white>"));
    }

    /** Lore and plain text. Never italic. */
    public static Component parse(String mini) {
        return MM.deserialize(mini).decoration(TextDecoration.ITALIC, false);
    }

    public static Component gradient(String text) {
        return MM.deserialize(GRAD_OPEN + esc(text) + GRAD_CLOSE);
    }

    /** A message with the Potato SMP prefix. */
    public static Component msg(String mini) {
        return MM.deserialize(PREFIX + " " + mini).decoration(TextDecoration.ITALIC, false);
    }
}

/**
 * Item passives, the potato bow, and the potato shield.
 *
 * Passives are refreshed every 5 ticks and last 3 seconds after you stop holding the item.
 * The shield draws a ring of potatoes and pushes enemies and mobs back. The bow's arrows
 * deal bonus damage. Every check happens only while the plugin is enabled.
 */
final class PassiveManager implements Listener {

    private final PotatoSMP plugin;
    private final NamespacedKey arrowKey;
    private final Set<UUID> hudShown = new HashSet<>();
    private BukkitTask task;

    PassiveManager(PotatoSMP plugin) {
        this.plugin = plugin;
        this.arrowKey = new NamespacedKey(plugin, "potato_bow_arrow");
    }

    void start() {
        if (task != null) {
            task.cancel();
        }
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 5L, 5L);
    }

    private void tick() {
        Items items = plugin.items();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!plugin.activeFor(player)) {
                continue;
            }
            Items.Kind main = items.kindOf(player.getInventory().getItemInMainHand());
            Items.Kind off = items.kindOf(player.getInventory().getItemInOffHand());
            addPassive(player, main);
            addPassive(player, off);
            if (wearsPotatoArmor(player)) {
                apply(player, PotionEffectType.RESISTANCE, 0);
            }
            updateHud(player, main);
            if (main == Items.Kind.POTATO_SHIELD || off == Items.Kind.POTATO_SHIELD) {
                shieldPulse(player);
            }
        }
    }

    /**
     * Shows the held item's cooldown above the hotbar. When nothing is shown any more, the
     * action bar is cleared, so an old cooldown never sticks around after switching items.
     */
    private void updateHud(Player player, Items.Kind main) {
        UUID id = player.getUniqueId();
        Component line = null;
        if (main == Items.Kind.POTATO_A5_STRIKE || main == Items.Kind.ENCHANTED_POTATO_A5_STRIKE) {
            long left = plugin.a5().remainingMillis(id);
            if (left > 0 && !plugin.noCooldowns()) {
                line = Text.itemCooldown(main, (left + 999L) / 1000L);
            }
        }
        if (line != null) {
            player.sendActionBar(line);
            hudShown.add(id);
        } else if (hudShown.remove(id)) {
            player.sendActionBar(Component.empty());
        }
    }

    private void addPassive(Player player, Items.Kind kind) {
        if (kind == null) {
            return;
        }
        switch (kind) {
            case POTATO_OF_LIFE -> apply(player, PotionEffectType.HEALTH_BOOST, 0);
            case GOLDEN_POTATO -> apply(player, PotionEffectType.ABSORPTION, 0);
            case POTATO_SWORD -> apply(player, PotionEffectType.STRENGTH, 0);
            case WARRIORS_BLADE -> apply(player, PotionEffectType.STRENGTH, 1);
            case POTATO_HOE, POTATO_PICKAXE, POTATO_SHOVEL -> apply(player, PotionEffectType.HASTE, 0);
            case POTATO_AXE, POTATO_SPEAR, POTATO_BOW -> apply(player, PotionEffectType.SPEED, 0);
            case POTATO_MACE, POTATO_SHIELD -> apply(player, PotionEffectType.RESISTANCE, 0);
            case POTATO_A5_STRIKE -> apply(player, PotionEffectType.JUMP_BOOST, 0);
            case ENCHANTED_POTATO_A5_STRIKE -> apply(player, PotionEffectType.JUMP_BOOST, 1);
            case POTATO_CHARM -> apply(player, PotionEffectType.LUCK, 0);
            case POTATO_WARD -> apply(player, PotionEffectType.FIRE_RESISTANCE, 0);
            case POTATO_LANTERN -> apply(player, PotionEffectType.NIGHT_VISION, 0);
            case POTATO_COMPASS -> apply(player, PotionEffectType.DOLPHINS_GRACE, 0);
            case POTATO_FEATHER -> apply(player, PotionEffectType.SLOW_FALLING, 0);
            case POTATO_SCROLL -> apply(player, PotionEffectType.CONDUIT_POWER, 0);
            case POTATO_SLIME -> apply(player, PotionEffectType.JUMP_BOOST, 0);
            case POTATO_GHAST -> apply(player, PotionEffectType.REGENERATION, 0);
            case POTATO_BLAZE -> apply(player, PotionEffectType.STRENGTH, 0);
            case POTATO_QUARTZ -> apply(player, PotionEffectType.WATER_BREATHING, 0);
            case POTATO_ECHO -> apply(player, PotionEffectType.HERO_OF_THE_VILLAGE, 0);
            default -> {
            }
        }
    }

    private static void apply(Player player, PotionEffectType type, int amplifier) {
        player.addPotionEffect(new PotionEffect(type, 60, amplifier, false, false, true));
    }

    private boolean wearsPotatoArmor(Player player) {
        Items items = plugin.items();
        for (ItemStack piece : player.getInventory().getArmorContents()) {
            Items.Kind kind = items.kindOf(piece);
            if (kind == Items.Kind.POTATO_HELMET || kind == Items.Kind.POTATO_CHESTPLATE
                    || kind == Items.Kind.POTATO_LEGGINGS || kind == Items.Kind.POTATO_BOOTS) {
                return true;
            }
        }
        return false;
    }

    private void shieldPulse(Player holder) {
        World world = holder.getWorld();
        Location center = holder.getLocation();
        for (int i = 0; i < 16; i++) {
            double angle = 2.0 * Math.PI * i / 16;
            world.spawnParticle(Particle.ITEM,
                    center.getX() + Math.cos(angle) * 2.2, center.getY() + 1.0,
                    center.getZ() + Math.sin(angle) * 2.2, 1, 0, 0, 0, 0.0, new ItemStack(Material.POTATO));
        }
        for (LivingEntity living : world.getNearbyLivingEntities(center, 2.5)) {
            if (living.equals(holder) || living instanceof ArmorStand) {
                continue;
            }
            Vector away = living.getLocation().toVector().subtract(center.toVector()).setY(0);
            if (away.lengthSquared() < 0.0001) {
                away = new Vector(1, 0, 0);
            }
            away.normalize().multiply(0.9).setY(0.35);
            living.setVelocity(away);
        }
    }

    private boolean shieldUp(Player player) {
        Items items = plugin.items();
        return items.kindOf(player.getInventory().getItemInMainHand()) == Items.Kind.POTATO_SHIELD
                || items.kindOf(player.getInventory().getItemInOffHand()) == Items.Kind.POTATO_SHIELD;
    }

    @EventHandler
    public void onShoot(EntityShootBowEvent event) {
        if (!plugin.activeFor(event.getEntity())) {
            return;
        }
        if (plugin.items().kindOf(event.getBow()) != Items.Kind.POTATO_BOW) {
            return;
        }
        if (event.getProjectile() instanceof Arrow arrow) {
            arrow.getPersistentDataContainer().set(arrowKey, PersistentDataType.BYTE, (byte) 1);
        }
    }

    @EventHandler
    public void onDamaged(EntityDamageByEntityEvent event) {
        if (!plugin.activeFor(event.getDamager())) {
            return;
        }
        if (event.getDamager() instanceof Arrow arrow
                && arrow.getPersistentDataContainer().has(arrowKey, PersistentDataType.BYTE)) {
            event.setDamage(event.getDamage() * 1.5 + 2.0);
        }
        if (event.getEntity() instanceof Player victim && shieldUp(victim)
                && event.getDamager() instanceof LivingEntity attacker
                && attacker.getLocation().distance(victim.getLocation()) <= 3.0) {
            event.setDamage(event.getDamage() * 0.5);
        }
    }
}

/**
 * Right-click abilities for the Potato Wand, Potato Charm, and Potato Ward. Each one has its
 * own cooldown, and the cooldown shows above the hotbar with the item's colours.
 */
final class ItemAbilities implements Listener {

    private final PotatoSMP plugin;
    private final NamespacedKey boltKey;
    private final Map<UUID, Map<Items.Kind, Long>> readyAt = new HashMap<>();

    ItemAbilities(PotatoSMP plugin) {
        this.plugin = plugin;
        this.boltKey = new NamespacedKey(plugin, "potato_wand_bolt");
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (!plugin.activeFor(event.getPlayer())) {
            return;
        }
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (event.getPlayer().isSneaking()) {
            return;
        }
        Items.Kind kind = plugin.items().kindOf(event.getItem());
        if (kind != Items.Kind.POTATO_WAND && kind != Items.Kind.POTATO_CHARM && kind != Items.Kind.POTATO_WARD) {
            return;
        }
        event.setCancelled(true);
        Player player = event.getPlayer();
        if (!ready(player, kind)) {
            return;
        }
        switch (kind) {
            case POTATO_WAND -> fireBolt(player, kind, 0.5);
            case POTATO_CHARM -> luckySurge(player, kind, 40.0);
            case POTATO_WARD -> wardBubble(player, kind, 30.0);
            default -> {
            }
        }
    }

    private boolean ready(Player player, Items.Kind kind) {
        long now = System.currentTimeMillis();
        long at = readyAt.getOrDefault(player.getUniqueId(), Map.of()).getOrDefault(kind, 0L);
        if (!plugin.noCooldowns() && now < at) {
            player.sendActionBar(Text.itemCooldown(kind, (at - now + 999L) / 1000L));
            return false;
        }
        return true;
    }

    private void startCooldown(Player player, Items.Kind kind, double seconds) {
        readyAt.computeIfAbsent(player.getUniqueId(), id -> new HashMap<>())
                .put(kind, System.currentTimeMillis() + (long) (seconds * 1000.0));
    }

    private void fireBolt(Player player, Items.Kind kind, double cooldownSeconds) {
        Vector velocity = player.getEyeLocation().getDirection().multiply(2.8);
        Snowball bolt = player.launchProjectile(Snowball.class, velocity);
        bolt.setItem(new ItemStack(Material.POTATO));
        bolt.getPersistentDataContainer().set(boltKey, PersistentDataType.BYTE, (byte) 1);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0f, 0.6f);
        player.getWorld().spawnParticle(Particle.CRIT, player.getEyeLocation(), 10, 0.2, 0.2, 0.2, 0.1);
        startCooldown(player, kind, cooldownSeconds);
    }

    /** A bolt hits for up to the configured damage, and never below 1 health. */
    @EventHandler
    public void onBoltHit(ProjectileHitEvent event) {
        if (!plugin.activeFor(event.getEntity().getShooter())) {
            return;
        }
        if (!(event.getEntity() instanceof Snowball bolt)
                || !bolt.getPersistentDataContainer().has(boltKey, PersistentDataType.BYTE)) {
            return;
        }
        if (event.getHitEntity() instanceof LivingEntity target && bolt.getShooter() instanceof Player shooter
                && !target.equals(shooter)) {
            double base = plugin.getConfig().getDouble("wand.damage", 9);
            double damage = Math.min(base, target.getHealth() - 1.0);
            if (damage > 0) {
                target.damage(damage, shooter);
            }
        }
        bolt.getWorld().spawnParticle(Particle.CRIT, bolt.getLocation(), 14, 0.3, 0.3, 0.3, 0.2);
        bolt.getWorld().playSound(bolt.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1.0f, 1.2f);
    }

    private void luckySurge(Player player, Items.Kind kind, double cooldownSeconds) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.LUCK, 20 * 20, 1, false, true, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 15 * 20, 1, false, true, true));
        World world = player.getWorld();
        Location at = player.getLocation();
        for (int i = 0; i < 24; i++) {
            double angle = 2.0 * Math.PI * i / 24;
            world.spawnParticle(Particle.HAPPY_VILLAGER, at.getX() + Math.cos(angle) * 1.5, at.getY() + 1.0,
                    at.getZ() + Math.sin(angle) * 1.5, 1, 0, 0, 0, 0.0);
        }
        world.playSound(at, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.4f);
        startCooldown(player, kind, cooldownSeconds);
    }

    /** An 8 second bubble: you get Resistance II, and enemies inside are pushed out. */
    private void wardBubble(Player player, Items.Kind kind, double cooldownSeconds) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 8 * 20, 1, false, true, true));
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.2f);
        startCooldown(player, kind, cooldownSeconds);
        new BukkitRunnable() {
            private int elapsed = 0;

            @Override
            public void run() {
                if (elapsed >= 8 * 20 || !player.isOnline()) {
                    cancel();
                    return;
                }
                elapsed += 10;
                World world = player.getWorld();
                Location center = player.getLocation();
                for (int i = 0; i < 24; i++) {
                    double angle = 2.0 * Math.PI * i / 24;
                    world.spawnParticle(Particle.ENCHANT, center.getX() + Math.cos(angle) * 8.0,
                            center.getY() + 0.3, center.getZ() + Math.sin(angle) * 8.0, 1, 0, 0, 0, 0.0);
                }
                for (LivingEntity living : world.getNearbyLivingEntities(center, 8.0)) {
                    if (living.equals(player) || living instanceof ArmorStand) {
                        continue;
                    }
                    Vector out = living.getLocation().toVector().subtract(center.toVector()).setY(0);
                    if (out.lengthSquared() < 0.0001) {
                        out = new Vector(1, 0, 0);
                    }
                    living.setVelocity(out.normalize().multiply(1.1).setY(0.35));
                }
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }
}

/**
 * The second ability for each item: sneak + right-click. Armor uses sneak + F instead, because
 * right-clicking armor equips it. Each ability has its own cooldown, shown above the hotbar.
 */
final class SecondAbilities implements Listener {

    private final PotatoSMP plugin;
    private final Map<UUID, Map<Items.Kind, Long>> readyAt = new HashMap<>();

    SecondAbilities(PotatoSMP plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (!plugin.activeFor(event.getPlayer())) {
            return;
        }
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Player player = event.getPlayer();
        if (!player.isSneaking()) {
            return;
        }
        Items.Kind kind = plugin.items().kindOf(event.getItem());
        if (kind == null || isArmor(kind) || kind.abilityLine().isEmpty()) {
            return;
        }
        event.setCancelled(true);
        event.setUseItemInHand(org.bukkit.event.Event.Result.DENY);
        event.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);
        run(player, kind);
    }

    @EventHandler
    public void onSwap(PlayerSwapHandItemsEvent event) {
        if (!plugin.activeFor(event.getPlayer())) {
            return;
        }
        Player player = event.getPlayer();
        if (!player.isSneaking()) {
            return;
        }
        Items.Kind kind = plugin.items().kindOf(event.getMainHandItem());
        if (kind == null || !isArmor(kind)) {
            return;
        }
        event.setCancelled(true);
        run(player, kind);
    }

    private static boolean isArmor(Items.Kind kind) {
        return kind == Items.Kind.POTATO_HELMET || kind == Items.Kind.POTATO_CHESTPLATE
                || kind == Items.Kind.POTATO_LEGGINGS || kind == Items.Kind.POTATO_BOOTS;
    }

    private void run(Player player, Items.Kind kind) {
        long now = System.currentTimeMillis();
        long at = readyAt.getOrDefault(player.getUniqueId(), Map.of()).getOrDefault(kind, 0L);
        if (!plugin.noCooldowns() && now < at) {
            player.sendActionBar(Text.itemCooldown(kind, (at - now + 999L) / 1000L));
            return;
        }
        double cooldown = perform(player, kind);
        if (cooldown > 0) {
            readyAt.computeIfAbsent(player.getUniqueId(), id -> new HashMap<>())
                    .put(kind, now + (long) (cooldown * 1000.0));
        }
    }

    /** Does the ability. Returns its cooldown in seconds, or 0 if nothing happened. */
    private double perform(Player player, Items.Kind kind) {
        switch (kind) {
            case POTATO_OF_LIFE -> {
                heal(player, 8.0);
                return 60;
            }
            case GOLDEN_POTATO -> {
                for (Player ally : Bukkit.getOnlinePlayers()) {
                    if (ally.getWorld().equals(player.getWorld())
                            && ally.getLocation().distance(player.getLocation()) <= 8.0) {
                        heal(ally, 6.0);
                    }
                }
                push(player, 7.0, 1.0);
                return 30;
            }
            case POTATO_SWORD -> {
                slash(player);
                return 8;
            }
            case WARRIORS_BLADE -> {
                int hits = 0;
                for (LivingEntity e : nearby(player, 9.0)) {
                    e.damage(8.0, player);
                    hits++;
                }
                heal(player, 2.0 * hits);
                player.getWorld().spawnParticle(Particle.SOUL, player.getLocation().add(0, 1, 0), 40, 2.0, 0.5, 2.0, 0.05);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SHOOT, 0.8f, 1.4f);
                return 25;
            }
            case POTATO_HOE -> {
                grow(player, 8);
                return 30;
            }
            case POTATO_PICKAXE -> {
                veinVision(player);
                return 30;
            }
            case POTATO_AXE -> {
                hit(player, 5.0, 4.0);
                return 12;
            }
            case POTATO_SHOVEL -> {
                hit(player, 6.0, 4.0);
                return 20;
            }
            case POTATO_MACE -> {
                hit(player, 7.0, 10.0);
                return 15;
            }
            case POTATO_SPEAR -> {
                lunge(player);
                return 10;
            }
            case POTATO_BOW -> {
                volley(player);
                return 8;
            }
            case POTATO_SHIELD -> {
                hit(player, 5.0, 3.0);
                return 12;
            }
            case POTATO_LANTERN -> {
                for (LivingEntity e : nearby(player, 16.0)) {
                    e.setFireTicks(80);
                }
                player.getWorld().spawnParticle(Particle.FLAME, player.getLocation().add(0, 1, 0), 60, 4.0, 1.0, 4.0, 0.05);
                player.getWorld().playSound(player.getLocation(), Sound.ITEM_FIRECHARGE_USE, 1.0f, 1.0f);
                return 30;
            }
            case POTATO_WAND -> {
                hit(player, 5.0, 3.0);
                return 6;
            }
            case POTATO_CHARM -> {
                player.getWorld().dropItemNaturally(player.getLocation(), plugin.items().create(Items.Kind.POTATO_OF_LIFE));
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
                return 40;
            }
            case POTATO_WARD -> {
                push(player, 6.0, 1.4);
                ringBurst(player, 6.0);
                return 30;
            }
            case POTATO_HELMET, POTATO_CHESTPLATE, POTATO_LEGGINGS, POTATO_BOOTS -> {
                hit(player, 5.0, 4.0);
                return 45;
            }
            case POTATO_SLIME -> {
                push(player, 4.0, 1.4);
                return 15;
            }
            case POTATO_GHAST -> {
                push(player, 8.0, 1.6);
                return 20;
            }
            case POTATO_BLAZE -> {
                for (LivingEntity e : nearby(player, 5.0)) {
                    e.setFireTicks(100);
                    e.damage(3.0, player);
                }
                return 20;
            }
            case POTATO_QUARTZ -> {
                for (LivingEntity e : nearby(player, 12.0)) {
                    e.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 160, 0, false, true, true));
                }
                return 25;
            }
            case POTATO_ECHO -> {
                hit(player, 6.0, 2.0);
                return 15;
            }
            case POTATO_COMPASS -> {
                int found = 0;
                for (Entity entity : player.getNearbyEntities(30, 30, 30)) {
                    if (entity instanceof Player other && !other.equals(player)) {
                        other.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 200, 0, false, true, true));
                        found++;
                    }
                }
                player.sendMessage(Text.msg("<gray>Lodestone found <white>" + found + "</white> player(s) within 30 blocks."));
                player.getWorld().playSound(player.getLocation(), Sound.ITEM_LODESTONE_COMPASS_LOCK, 1.0f, 1.0f);
                return 25;
            }
            case POTATO_FEATHER -> {
                push(player, 4.0, 1.0);
                player.setVelocity(new Vector(0, 1.2, 0));
                player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 30, 0.5, 0.2, 0.5, 0.05);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BREEZE_JUMP, 1.0f, 1.3f);
                return 15;
            }
            case POTATO_SCROLL -> {
                int players = 0;
                int mobs = 0;
                for (Entity entity : player.getNearbyEntities(40, 40, 40)) {
                    if (entity instanceof Player other && !other.equals(player)) {
                        other.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 200, 0, false, true, true));
                        players++;
                    } else if (entity instanceof LivingEntity && !(entity instanceof Player)) {
                        mobs++;
                    }
                }
                player.sendMessage(Text.msg("<gray>Reveal: <white>" + players + "</white> player(s) and <white>"
                        + mobs + "</white> mob(s) within 40 blocks."));
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.0f);
                return 30;
            }
            default -> {
                return 0;
            }
        }
    }

    private void veinVision(Player player) {
        World world = player.getWorld();
        Location centre = player.getLocation();
        List<Location> ores = new ArrayList<>();
        for (int dx = -12; dx <= 12; dx++) {
            for (int dy = -12; dy <= 12; dy++) {
                for (int dz = -12; dz <= 12; dz++) {
                    Block block = world.getBlockAt(centre.getBlockX() + dx, centre.getBlockY() + dy,
                            centre.getBlockZ() + dz);
                    if (block.getType().name().endsWith("_ORE")) {
                        ores.add(block.getLocation().add(0.5, 0.5, 0.5));
                    }
                }
            }
        }
        new BukkitRunnable() {
            private int elapsed = 0;

            @Override
            public void run() {
                if (elapsed >= 200 || !player.isOnline()) {
                    cancel();
                    return;
                }
                for (Location ore : ores) {
                    world.spawnParticle(Particle.END_ROD, ore, 3, 0.2, 0.2, 0.2, 0.0);
                }
                elapsed += 10;
            }
        }.runTaskTimer(plugin, 0L, 10L);
        world.playSound(centre, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.0f);
    }

    private static void ringBurst(Player player, double radius) {
        World world = player.getWorld();
        Location centre = player.getLocation().add(0, 0.3, 0);
        for (int i = 0; i < 36; i++) {
            double angle = 2.0 * Math.PI * i / 36;
            world.spawnParticle(Particle.CLOUD, centre.getX() + Math.cos(angle) * radius, centre.getY(),
                    centre.getZ() + Math.sin(angle) * radius, 1, 0, 0, 0, 0.0);
        }
    }

    private static List<LivingEntity> nearby(Player player, double radius) {
        List<LivingEntity> found = new ArrayList<>();
        for (LivingEntity living : player.getWorld().getNearbyLivingEntities(player.getLocation(), radius)) {
            if (living.equals(player) || living.isDead() || living instanceof ArmorStand) {
                continue;
            }
            found.add(living);
        }
        return found;
    }

    private static void heal(Player player, double amount) {
        AttributeInstance max = player.getAttribute(Attribute.MAX_HEALTH);
        double cap = max == null ? 20.0 : max.getValue();
        player.setHealth(Math.min(cap, player.getHealth() + amount));
        player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 2, 0), 8, 0.4, 0.3, 0.4, 0.0);
    }

    private static void push(Player player, double radius, double strength) {
        for (LivingEntity e : nearby(player, radius)) {
            Vector out = e.getLocation().toVector().subtract(player.getLocation().toVector()).setY(0);
            if (out.lengthSquared() < 0.0001) {
                out = new Vector(1, 0, 0);
            }
            e.setVelocity(out.normalize().multiply(strength).setY(0.4));
        }
        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 30, radius * 0.5, 0.3, radius * 0.5, 0.05);
    }

    /** Damage and push everything within the radius. */
    private static void hit(Player player, double radius, double damage) {
        for (LivingEntity e : nearby(player, radius)) {
            e.damage(damage, player);
            Vector out = e.getLocation().toVector().subtract(player.getLocation().toVector()).setY(0);
            if (out.lengthSquared() < 0.0001) {
                out = new Vector(1, 0, 0);
            }
            e.setVelocity(out.normalize().multiply(0.8).setY(0.45));
        }
        player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, player.getLocation().add(0, 1, 0), 6, 1.5, 0.3, 1.5, 0.0);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 0.9f);
    }

    /** Hits enemies in a cone in front of the player. */
    private static void slash(Player player) {
        Vector facing = player.getEyeLocation().getDirection().setY(0);
        if (facing.lengthSquared() < 0.0001) {
            return;
        }
        facing.normalize();
        for (LivingEntity e : nearby(player, 5.0)) {
            Vector toward = e.getLocation().toVector().subtract(player.getLocation().toVector()).setY(0);
            if (toward.lengthSquared() < 0.0001) {
                continue;
            }
            if (toward.normalize().dot(facing) > 0.5) {
                e.damage(5.0, player);
            }
        }
        player.getWorld().spawnParticle(Particle.SWEEP_ATTACK,
                player.getLocation().add(facing.clone().multiply(1.5)).add(0, 1, 0), 3, 0.3, 0.2, 0.3, 0.0);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.2f);
    }

    /** Grows every crop within the radius to full height. */
    private static void grow(Player player, int radius) {
        World world = player.getWorld();
        int px = player.getLocation().getBlockX();
        int py = player.getLocation().getBlockY();
        int pz = player.getLocation().getBlockZ();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    Block block = world.getBlockAt(px + dx, py + dy, pz + dz);
                    if (block.getBlockData() instanceof Ageable crop && crop.getAge() < crop.getMaximumAge()) {
                        crop.setAge(crop.getMaximumAge());
                        block.setBlockData(crop);
                        world.spawnParticle(Particle.HAPPY_VILLAGER, block.getLocation().add(0.5, 0.5, 0.5),
                                6, 0.3, 0.3, 0.3, 0.0);
                    }
                }
            }
        }
        world.playSound(player.getLocation(), Sound.ITEM_BONE_MEAL_USE, 1.0f, 1.0f);
    }

    private static void lunge(Player player) {
        Vector forward = player.getEyeLocation().getDirection().setY(0);
        if (forward.lengthSquared() < 0.0001) {
            return;
        }
        forward.normalize();
        for (LivingEntity e : nearby(player, 4.0)) {
            Vector toward = e.getLocation().toVector().subtract(player.getLocation().toVector()).setY(0);
            if (toward.lengthSquared() > 0.0001 && toward.normalize().dot(forward) > 0.3) {
                e.damage(6.0, player);
            }
        }
        player.setVelocity(forward.multiply(2.0).setY(0.2));
        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 20, 0.3, 0.2, 0.3, 0.05);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BREEZE_JUMP, 1.0f, 1.0f);
    }

    /** Three arrows in a fan. They are vanilla arrows, so the bow's own passive does not apply. */
    private static void volley(Player player) {
        Vector eye = player.getEyeLocation().getDirection();
        for (double degrees : new double[] {-6.0, 0.0, 6.0}) {
            Vector direction = eye.clone().rotateAroundY(Math.toRadians(degrees));
            Arrow arrow = player.launchProjectile(Arrow.class, direction.multiply(3.5));
            arrow.setShooter(player);
        }
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0f, 0.8f);
    }
}

/**
 * Passives that happen on every hit with the Potato Sword or the Warrior's Blade: lifesteal and
 * a chance to call down lightning. The blade does everything the sword does, plus more.
 */
final class WeaponPassives implements Listener {

    private final PotatoSMP plugin;

    WeaponPassives(PotatoSMP plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent event) {
        if (!plugin.activeFor(event.getDamager())) {
            return;
        }
        if (!(event.getDamager() instanceof Player attacker)) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity victim) || victim.equals(attacker)) {
            return;
        }
        Items.Kind kind = plugin.items().kindOf(attacker.getInventory().getItemInMainHand());
        if (kind == Items.Kind.POTATO_SWORD) {
            apply(event, attacker, victim, 0.03, 0.10, 0.0);
        } else if (kind == Items.Kind.WARRIORS_BLADE) {
            apply(event, attacker, victim, 0.06, 0.25, 2.0);
        }
    }

    private static void apply(EntityDamageByEntityEvent event, Player attacker, LivingEntity victim,
                              double lifesteal, double lightningChance, double bonusDamage) {
        double dealt = event.getDamage() + bonusDamage;
        event.setDamage(dealt);

        AttributeInstance max = attacker.getAttribute(Attribute.MAX_HEALTH);
        double cap = max == null ? 20.0 : max.getValue();
        attacker.setHealth(Math.min(cap, attacker.getHealth() + dealt * lifesteal));
        attacker.getWorld().spawnParticle(Particle.HEART, attacker.getLocation().add(0, 2, 0), 2, 0.3, 0.2, 0.3, 0.0);

        if (ThreadLocalRandom.current().nextDouble() < lightningChance) {
            victim.getWorld().strikeLightning(victim.getLocation());
        }
    }
}

/**
 * /potato recipes: one recipe per page. The item's name is across the top, its 3x3 ingredient
 * grid sits below, and the result is on the right. Arrows move between pages.
 */
final class RecipeMenu implements Listener {

    private static final int SIZE = 54;
    private static final int PREV = 48;
    private static final int PAGE = 49;
    private static final int NEXT = 50;
    private static final int CLOSE = 53;
    private static final int[] GRID = {11, 12, 13, 20, 21, 22, 29, 30, 31};

    private final PotatoSMP plugin;

    RecipeMenu(PotatoSMP plugin) {
        this.plugin = plugin;
    }

    static final class RecipeHolder implements InventoryHolder {
        private Inventory inventory;
        private int page;

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    void open(Player player) {
        if (!plugin.activeFor(player)) {
            player.sendMessage(Text.msg("<red>Potato SMP is disabled. An admin can run /potatosmp enable."));
            return;
        }
        RecipeHolder holder = new RecipeHolder();
        Inventory inventory = Bukkit.createInventory(holder, SIZE, Text.gradient("Potato Recipes"));
        holder.inventory = inventory;
        render(holder);
        player.openInventory(inventory);
    }

    private void render(RecipeHolder holder) {
        Inventory inventory = holder.inventory;
        inventory.clear();
        List<PotatoRecipes.RecipeView> views = plugin.recipes().views();
        ItemStack glass = named(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int slot = 0; slot < SIZE; slot++) {
            inventory.setItem(slot, glass);
        }
        if (views.isEmpty()) {
            inventory.setItem(22, named(Material.BARRIER, "No recipes", List.of()));
            return;
        }
        holder.page = Math.floorMod(holder.page, views.size());
        PotatoRecipes.RecipeView view = views.get(holder.page);

        ItemStack title = new ItemStack(Material.PAPER);
        ItemMeta titleMeta = title.getItemMeta();
        titleMeta.displayName(view.result().displayComponent());
        title.setItemMeta(titleMeta);
        inventory.setItem(4, title);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                char symbol = view.rows()[row].charAt(col);
                if (symbol == ' ') {
                    continue;
                }
                ItemStack ingredient = view.symbols().get(symbol);
                if (ingredient != null) {
                    inventory.setItem(GRID[row * 3 + col], ingredient.clone());
                }
            }
        }
        inventory.setItem(23, named(Material.ARROW, "->", List.of()));
        inventory.setItem(24, plugin.items().create(view.result()));

        inventory.setItem(PREV, named(Material.SPECTRAL_ARROW, "Previous", List.of("<gray>Go back one recipe.")));
        inventory.setItem(PAGE, named(Material.BOOK, "Page " + (holder.page + 1) + " / " + views.size(), List.of()));
        inventory.setItem(NEXT, named(Material.SPECTRAL_ARROW, "Next", List.of("<gray>Go forward one recipe.")));
        inventory.setItem(CLOSE, named(Material.BARRIER, "Close", List.of("<gray>Click to close.")));
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof RecipeHolder holder)) {
            return;
        }
        event.setCancelled(true);
        if (event.getRawSlot() >= SIZE || !(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        int slot = event.getRawSlot();
        if (slot == CLOSE) {
            player.closeInventory();
        } else if (slot == PREV) {
            holder.page--;
            render(holder);
        } else if (slot == NEXT) {
            holder.page++;
            render(holder);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof RecipeHolder) {
            event.setCancelled(true);
        }
    }

    private static ItemStack named(Material material, String name, List<String> lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(Text.parse(name));
        meta.lore(lore.stream().map(Text::parse).toList());
        stack.setItemMeta(meta);
        return stack;
    }
}

/**
 * Potato Reroller: right-click to use it up. It turns into a random Potato item. The result is
 * never another reroller or the Warrior's Blade, and the reroller is never crafted.
 */
final class PotatoRerollerListener implements Listener {

    private final PotatoSMP plugin;

    PotatoRerollerListener(PotatoSMP plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (!plugin.activeFor(event.getPlayer())) {
            return;
        }
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (plugin.items().kindOf(event.getItem()) != Items.Kind.POTATO_REROLLER) {
            return;
        }
        event.setCancelled(true);
        event.setUseItemInHand(org.bukkit.event.Event.Result.DENY);
        event.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);

        Player player = event.getPlayer();
        List<Items.Kind> pool = new ArrayList<>();
        for (Items.Kind kind : Items.Kind.values()) {
            if (kind != Items.Kind.POTATO_REROLLER && kind != Items.Kind.WARRIORS_BLADE
                    && kind != Items.Kind.POTATO_OVERLORD) {
                pool.add(kind);
            }
        }
        Items.Kind result = pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
        ItemStack created = plugin.items().create(result);

        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getAmount() <= 1) {
            player.getInventory().setItemInMainHand(created);
        } else {
            ItemStack rest = hand.clone();
            rest.setAmount(hand.getAmount() - 1);
            player.getInventory().setItemInMainHand(rest);
            player.getInventory().addItem(created).values()
                    .forEach(overflow -> player.getWorld().dropItemNaturally(player.getLocation(), overflow));
        }

        player.getWorld().spawnParticle(Particle.ENCHANT, player.getLocation().add(0, 1, 0), 40, 0.4, 0.5, 0.4, 0.5);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 0.8f);
        player.sendMessage(Text.msg("<gray>Your reroller became <white>" + Text.esc(result.displayName()) + "</white>."));
    }
}

/**
 * Potato Overlord: an admin-only item with a right-click storm. Operators only, and it's never
 * craftable or rerolled.
 */
final class OverlordListener implements Listener {

    private static final long COOLDOWN_MS = 60_000L;

    private final PotatoSMP plugin;
    private final Map<UUID, Long> readyAt = new HashMap<>();

    OverlordListener(PotatoSMP plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (!plugin.activeFor(event.getPlayer())) {
            return;
        }
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (plugin.items().kindOf(event.getItem()) != Items.Kind.POTATO_OVERLORD) {
            return;
        }
        event.setCancelled(true);
        event.setUseItemInHand(org.bukkit.event.Event.Result.DENY);
        event.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);

        Player player = event.getPlayer();
        if (!player.hasPermission("potato.admin")) {
            player.sendMessage(Text.msg("<red>Only admins can use the Potato Overlord."));
            return;
        }
        long now = System.currentTimeMillis();
        long ready = readyAt.getOrDefault(player.getUniqueId(), 0L);
        if (!plugin.noCooldowns() && now < ready) {
            player.sendActionBar(Text.itemCooldown(Items.Kind.POTATO_OVERLORD, (ready - now + 999L) / 1000L));
            return;
        }
        readyAt.put(player.getUniqueId(), now + COOLDOWN_MS);
        storm(player);
    }

    /** Five seconds of lightning across 20 blocks, with a spiral of particles, then a shockwave. */
    private void storm(Player player) {
        World world = player.getWorld();
        Location centre = player.getLocation();
        world.playSound(centre, Sound.ENTITY_WITHER_SPAWN, 1.0f, 0.7f);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        new BukkitRunnable() {
            private int step = 0;

            @Override
            public void run() {
                if (!player.isOnline() || !world.equals(player.getWorld())) {
                    cancel();
                    return;
                }
                if (step >= 50) {
                    cancel();
                    shockwave(player, world, centre);
                    return;
                }
                if (step % 3 == 0) {
                    for (int i = 0; i < 2; i++) {
                        double angle = random.nextDouble() * 2.0 * Math.PI;
                        double distance = random.nextDouble() * 20.0;
                        world.strikeLightning(centre.clone().add(
                                Math.cos(angle) * distance, 0, Math.sin(angle) * distance));
                    }
                }
                // A ring of particles that climbs and spirals outward as the storm goes on.
                double radius = 1.0 + 19.0 * (step % 25) / 25.0;
                double height = 0.5 + step * 0.06;
                for (int i = 0; i < 16; i++) {
                    double angle = 2.0 * Math.PI * i / 16 + step * 0.25;
                    world.spawnParticle(Particle.ENCHANT, centre.getX() + Math.cos(angle) * radius,
                            centre.getY() + height, centre.getZ() + Math.sin(angle) * radius, 1, 0, 0, 0, 0.0);
                }
                if (step % 5 == 0) {
                    world.playSound(centre, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 0.8f);
                }
                step++;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    /** The finale: everything in range takes damage, is slowed, and is thrown into the air. */
    private void shockwave(Player player, World world, Location centre) {
        for (LivingEntity living : world.getNearbyLivingEntities(centre, 20.0)) {
            if (living.equals(player) || living.isDead()) {
                continue;
            }
            // No source: the shockwave is never credited to the user.
            living.damage(6.0);
            living.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1, false, true, true));
            Vector out = living.getLocation().toVector().subtract(centre.toVector()).setY(0);
            if (out.lengthSquared() < 0.0001) {
                out = new Vector(1, 0, 0);
            }
            living.setVelocity(out.normalize().multiply(1.8).setY(1.4));
        }
        world.spawnParticle(Particle.EXPLOSION, centre, 8, 3.0, 1.0, 3.0, 0.0);
        for (int i = 0; i < 64; i++) {
            double angle = 2.0 * Math.PI * i / 64;
            world.spawnParticle(Particle.CLOUD, centre.getX() + Math.cos(angle) * 20.0, centre.getY() + 0.5,
                    centre.getZ() + Math.sin(angle) * 20.0, 1, 0, 0, 0, 0.0);
        }
        world.playSound(centre, Sound.ENTITY_GENERIC_EXPLODE, 1.6f, 0.5f);
    }
}
