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

    @Override
    public void onEnable() {
        saveDefaultConfig();
        data = new DataStore(this);
        data.load();
        items = new Items(this);
        blades = new BladeManager(this, data, items);
        recipes = new PotatoRecipes(this, items);
        if (isEnabled()) {
            recipes.register();
        }
        menu = new PotatoMenu(this);
        streaks = new StreakManager(this);
        snowball = new TuffSnowballManager(this);
        a5 = new A5Manager(this);

        getServer().getPluginManager().registerEvents(new CombatListener(this), this);
        getServer().getPluginManager().registerEvents(new GuardListener(this), this);
        getServer().getPluginManager().registerEvents(new RecipeGuard(this), this);
        getServer().getPluginManager().registerEvents(new FarmListener(this), this);
        getServer().getPluginManager().registerEvents(new FoodListener(this), this);
        getServer().getPluginManager().registerEvents(snowball, this);
        getServer().getPluginManager().registerEvents(a5, this);
        getServer().getPluginManager().registerEvents(new OwnerGuard(this), this);
        getServer().getPluginManager().registerEvents(menu, this);

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
    public boolean isEnabled() {
        return data != null && data.pluginEnabled();
    }

    /**
     * Emergency stop: turns the plugin off, cancels every running Potato SMP task, clears
     * slams in progress, and removes Tuff Snowball minecarts and falling blocks already in
     * the world.
     */
    public void emergencyStop() {
        setEnabled(false);
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

    public void setEnabled(boolean value) {
        data.setPluginEnabled(value);
        recipes.unregister();
        if (value) {
            recipes.register();
        }
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

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (!plugin.isEnabled()) {
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
        if (now < ready) {
            caster.sendActionBar(Text.gradient("A5 Strike ready in " + ((ready - now) + 999L) / 1000L + "s"));
            return;
        }

        Player target = nearestPlayer(caster, cfg.getDouble("a5.range", 10));
        if (target == null) {
            caster.sendMessage(Text.msg("<red>No player in range."));
            return;
        }

        double launchHeight = Math.max(4.0, cfg.getDouble("a5.launch_height", 12));
        World world = caster.getWorld();
        if (blockedAbove(target.getLocation().add(0, 1, 0), world.getMaxHeight() - target.getY())) {
            caster.sendMessage(Text.msg("<red>Blocks are above that player. A5 Strike refused."));
            return;
        }
        if (blockedAbove(caster.getLocation().add(0, 1, 0), launchHeight + 1.0)) {
            caster.sendMessage(Text.msg("<red>Blocks are above you. A5 Strike refused."));
            return;
        }

        long cooldownSeconds = enchanted
                ? cfg.getLong("a5.cooldown_enchanted", 40)
                : cfg.getLong("a5.cooldown_normal", 25);
        readyAt.put(casterId, now + cooldownSeconds * 1000L);

        double damage = enchanted
                ? cfg.getDouble("a5.enchanted_damage", 16)
                : cfg.getDouble("a5.normal_damage", 10);
        double radius = Math.max(1.0, cfg.getDouble("a5.radius", 3));
        startSlam(caster, target.getUniqueId(), launchHeight, damage, radius);
        world.playSound(caster.getLocation(), Sound.ENTITY_BREEZE_JUMP, 1.0f, 0.7f);
    }

    private void startSlam(Player caster, UUID targetId, double launchHeight, double damage, double radius) {
        UUID casterId = caster.getUniqueId();
        slamming.add(casterId);
        int maxAirTicks = Math.max(20, plugin.getConfig().getInt("a5.max_air_ticks", 120));
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
                if (!descending) {
                    if (self.getVelocity().getY() <= 0 || age >= maxAirTicks) {
                        descending = true;
                        Vector slam = new Vector(0, -2.5, 0);
                        Player targetNow = Bukkit.getPlayer(targetId);
                        if (targetNow != null && targetNow.getWorld().equals(self.getWorld())) {
                            Vector horizontal = targetNow.getLocation().toVector()
                                    .subtract(self.getLocation().toVector()).setY(0);
                            if (horizontal.lengthSquared() > 0.01) {
                                slam.add(horizontal.normalize().multiply(0.4));
                            }
                        }
                        self.setVelocity(slam);
                    }
                    return;
                }
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
        world.spawnParticle(Particle.EXPLOSION, center, 1);
        world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.6f);
        for (LivingEntity living : world.getNearbyLivingEntities(center, radius)) {
            if (living.equals(caster) || living.isDead() || living instanceof ArmorStand) {
                continue;
            }
            double distance = living.getLocation().distance(center);
            double falloff = 1.0 - (distance / radius) * 0.5;
            // No source: a blast is never credited to the user, so it can't soul-ban or build streaks.
            living.damage(damage * falloff);
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

    private static Player nearestPlayer(Player caster, double range) {
        Player best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Entity entity : caster.getNearbyEntities(range, range, range)) {
            if (!(entity instanceof Player player)) {
                continue;
            }
            if (player.equals(caster) || player.isDead()) {
                continue;
            }
            double distance = player.getLocation().distance(caster.getLocation());
            if (distance < bestDistance) {
                bestDistance = distance;
                best = player;
            }
        }
        return best;
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

    private static final List<String> SUBCOMMANDS = List.of("help", "menu", "status", "blade", "give", "reload", "enable", "disable", "stop");
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
                if (sender instanceof Player player) {
                    plugin.menu().open(player);
                } else {
                    sender.sendMessage(Text.msg("<red>Only players can open the menu."));
                }
            }
            case "status" -> status(sender, args);
            case "enable" -> setEnabled(sender, true);
            case "disable" -> setEnabled(sender, false);
            case "stop" -> stop(sender);
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
        plugin.setEnabled(value);
        sender.sendMessage(Text.msg(value ? "<green>Potato SMP enabled." : "<yellow>Potato SMP disabled."));
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
        if (!plugin.isEnabled()) {
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

        double chance = Math.max(0.0, Math.min(1.0, plugin.getConfig().getDouble("hoe.golden_potato_chance", 0.10)));
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
        if (!plugin.isEnabled()) {
            return;
        }
        Items.Kind kind = plugin.items().kindOf(event.getItem());
        if (kind == Items.Kind.POTATO_OF_LIFE) {
            apply(event.getPlayer(), "effects.potato_of_life");
        } else if (kind == Items.Kind.GOLDEN_POTATO) {
            apply(event.getPlayer(), "effects.golden_potato");
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
        if (!plugin.isEnabled()) {
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

        // Every Potato SMP item, one per slot in the bottom rows.
        int slot = 27;
        for (Items.Kind kind : Items.Kind.values()) {
            if (slot > 44) {
                break;
            }
            inventory.setItem(slot++, item(kind.material(), kind.displayName(), List.of(kind.lore())));
        }

        player.openInventory(inventory);
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
        if (!plugin.isEnabled()) {
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
        if (!plugin.isEnabled()) {
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
        POTATO_OF_LIFE(Material.POTATO, "Potato of Life", "Eat for regeneration and absorption."),
        GOLDEN_POTATO(Material.POTATO, "Golden Potato", "A cheaper golden apple."),
        POTATO_ORE(Material.RAW_GOLD, "Potato Ore", "Smithing ingredient for Potato armor."),
        POTATO_SWORD(Material.NETHERITE_SWORD, "Potato Sword",
                "Killing a Marked player with this forges the Warrior's Blade."),
        WARRIORS_BLADE(Material.NETHERITE_SWORD, "Warrior's Blade",
                "Players killed by this blade are soul-banned.", "Destroying it frees every soul."),
        POTATO_HOE(Material.NETHERITE_HOE, "Potato Hoe",
                "1/10 chance of a Golden Potato on harvest.", "Replants crops after harvest."),
        POTATO_PICKAXE(Material.NETHERITE_PICKAXE, "Potato Pickaxe", "A potato-forged pickaxe."),
        POTATO_AXE(Material.NETHERITE_AXE, "Potato Axe", "A potato-forged axe."),
        POTATO_SHOVEL(Material.NETHERITE_SHOVEL, "Potato Shovel", "A potato-forged shovel."),
        POTATO_MACE(Material.MACE, "Potato Mace", "A potato-forged mace."),
        POTATO_SPEAR(Material.NETHERITE_SPEAR, "Potato Spear", "A potato-forged netherite spear."),
        POTATO_HELMET(Material.NETHERITE_HELMET, "Potato Helmet", "Potato-forged armor."),
        POTATO_CHESTPLATE(Material.NETHERITE_CHESTPLATE, "Potato Chestplate", "Potato-forged armor."),
        POTATO_LEGGINGS(Material.NETHERITE_LEGGINGS, "Potato Leggings", "Potato-forged armor."),
        POTATO_BOOTS(Material.NETHERITE_BOOTS, "Potato Boots", "Potato-forged armor."),
        TUFF_SNOWBALL(Material.SNOWBALL, "Tuff Snowball",
                "Throw it and a rain of TNT minecarts, anvils, and dripstone falls.",
                "It can't hurt the one who threw it."),
        POTATO_A5_STRIKE(Material.BREEZE_ROD, "Potato A5 Strike",
                "Launches you above the nearest player, then slams down.",
                "Refused if there are blocks above them or you."),
        ENCHANTED_POTATO_A5_STRIKE(Material.BREEZE_ROD, "Enchanted Potato A5 Strike",
                "A stronger A5 Strike with a shorter cooldown.");

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
        meta.displayName(Text.gradient(kind.displayName()));
        meta.lore(Arrays.stream(kind.lore()).map(Text::parse).toList());
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
        if (!plugin.isEnabled()) {
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
        if (!plugin.isEnabled()) {
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
        if (!plugin.isEnabled()) {
            return;
        }
        plugin.blades().validateInventory(event.getPlayer().getInventory());
    }

    /** Soul-banned players can't log back in until the blade is destroyed. */
    @EventHandler
    public void onLogin(PlayerLoginEvent event) {
        if (!plugin.isEnabled()) {
            return;
        }
        if (plugin.blades().isSoulBanned(event.getPlayer().getUniqueId())) {
            String message = plugin.getConfig().getString("ban_message", "You were soul-banned.");
            event.disallow(PlayerLoginEvent.Result.KICK_BANNED, Text.parse(message));
        }
    }

    @EventHandler
    public void onCreative(InventoryCreativeEvent event) {
        if (!plugin.isEnabled()) {
            return;
        }
        if (isCustom(event.getCursor()) || isCustom(event.getCurrentItem())) {
            event.setCancelled(true);
            event.getWhoClicked().sendMessage(Text.msg("<red>Potato SMP items can't be taken from creative."));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (!plugin.isEnabled()) {
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
        if (!plugin.isEnabled()) {
            return;
        }
        if (isCustom(event.getItem())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEnchant(EnchantItemEvent event) {
        if (!plugin.isEnabled()) {
            return;
        }
        if (isCustom(event.getItem())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onAnvil(PrepareAnvilEvent event) {
        if (!plugin.isEnabled()) {
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
        if (!plugin.isEnabled()) {
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
        if (!plugin.isEnabled()) {
            return;
        }
        if (!validItem(event.getItem())) {
            event.setCancelled(true);
            event.getItem().remove();
        }
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (!plugin.isEnabled()) {
            return;
        }
        if (!validItem(event.getItem())) {
            event.setCancelled(true);
            event.getItem().remove();
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        if (!plugin.isEnabled()) {
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
        if (!plugin.isEnabled()) {
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
        if (!plugin.isEnabled()) {
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
        if (!plugin.isEnabled()) {
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
        if (!plugin.isEnabled()) {
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
        if (!plugin.isEnabled()) {
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

    public void register() {
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
        NamespacedKey key = new NamespacedKey(plugin, id);
        ShapelessRecipe recipe = new ShapelessRecipe(key, items.create(result));
        for (Material material : ingredients) {
            recipe.addIngredient(material);
        }
        add(key, recipe, rule);
    }

    private void shaped(String id, Items.Kind result, Map<Material, Items.Kind> rule, String[] rows,
                        Map<Character, Material> ingredients) {
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
        if (!plugin.isEnabled()) {
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

        double speed = plugin.getConfig().getDouble("tuff_snowball.throw_speed", 1.5);
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
                    cart.remove();
                    cancel();
                    blast(where, thrower);
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
        if (!plugin.isEnabled()) {
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

    /** TNT minecarts from the rain never explode vanilla-style, so they never break blocks. */
    @EventHandler
    public void onCartExplode(EntityExplodeEvent event) {
        if (!plugin.isEnabled()) {
            return;
        }
        if (event.getEntity().getPersistentDataContainer().has(cartMarkerKey, PersistentDataType.BYTE)) {
            event.setCancelled(true);
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

    public static Component parse(String mini) {
        return MM.deserialize(mini);
    }

    public static Component gradient(String text) {
        return MM.deserialize(GRAD_OPEN + esc(text) + GRAD_CLOSE);
    }

    /** A message with the Potato SMP prefix. */
    public static Component msg(String mini) {
        return MM.deserialize(PREFIX + " " + GRAD_OPEN + mini + GRAD_CLOSE);
    }
}
