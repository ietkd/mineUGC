package org.mineUGC.plugin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.mineUGC.core.command.UgcCommand;
import org.mineUGC.core.event.AssetReloadEvent;
import org.mineUGC.core.model.ItemDefinition;
import org.mineUGC.core.registry.AssetRegistry;
import org.mineUGC.items.InventoryScanner;
import org.mineUGC.items.ItemManager;
import org.mineUGC.items.abilities.AbilityExecutor;
import org.mineUGC.items.attributes.AttributeApplier;
import org.mineUGC.items.crafting.CustomRecipeManager;
import org.mineUGC.items.listeners.ItemListener;
import org.mineUGC.storage.sqlite.DatabaseManager;
import org.mineUGC.storage.sqlite.PlayerDataDAO;
import org.mineUGC.storage.yaml.YamlItemLoader;
import org.mineUGC.storage.yaml.YamlWatcher;
import org.mineUGC.gui.editor.GuiListener;
import org.mineUGC.gui.editor.MainMenuInventory;
import org.mineUGC.gui.fastinv.FastInvManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;

public class MineUGC extends JavaPlugin {
    private AssetRegistry<ItemDefinition> registry;
    private ItemManager itemManager;
    private InventoryScanner inventoryScanner;
    private AbilityExecutor abilityExecutor;
    private DatabaseManager database;
    private PlayerDataDAO playerDataDAO;
    private YamlWatcher yamlWatcher;
    private YamlItemLoader yamlLoader;
    private GuiListener guiListener;
    private CustomRecipeManager recipeManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // Core
        this.registry = new AssetRegistry<>();

        // Storage - YAML
        this.yamlLoader = new YamlItemLoader();
        this.itemManager = new ItemManager(this);
        this.inventoryScanner = new InventoryScanner(itemManager);

        // Storage - SQLite
        File dbFile = new File(getDataFolder(), getConfig().getString("database.filename", "mineugc.db"));
        this.database = new DatabaseManager(dbFile, getLogger());
        try {
            database.initialize();
        } catch (SQLException e) {
            getLogger().log(Level.SEVERE, "Failed to initialize database", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        this.playerDataDAO = new PlayerDataDAO(database);

        // Items
        this.abilityExecutor = new AbilityExecutor(getLogger());
        this.recipeManager = new CustomRecipeManager(this, itemManager);

        // GUI
        this.guiListener = new GuiListener(this, itemManager, getItemsDirectory());

        // Load items from files
        loadAllItems();

        // Register recipes
        recipeManager.reloadRecipes();

        // Register listeners
        FastInvManager.register(this);
        getServer().getPluginManager().registerEvents(
                new ItemListener(itemManager, abilityExecutor, playerDataDAO, getLogger()), this);
        getServer().getPluginManager().registerEvents(guiListener, this);

        // Register commands
        registerCommands();

        // Start file watcher for hot reload
        startFileWatcher();

        getLogger().info("mineUGC v" + getPluginMeta().getVersion() + " enabled");
    }

    @Override
    public void onDisable() {
        if (yamlWatcher != null) yamlWatcher.close();
        if (database != null) database.close();
        if (recipeManager != null) recipeManager.unregisterAll();
        getLogger().info("mineUGC disabled");
    }

    private void loadAllItems() {
        File itemsDir = getItemsDirectory();
        if (!itemsDir.exists()) {
            itemsDir.mkdirs();
            return;
        }

        File[] files = itemsDir.listFiles((dir, name) ->
                name.endsWith(".yml") || name.endsWith(".yaml"));
        if (files == null) return;

        for (File file : files) {
            try {
                ItemDefinition def = yamlLoader.load(file);
                if (def.getId() == null) {
                    getLogger().warning("Item file missing 'id' field: " + file.getName());
                    continue;
                }
                registry.register(def);
                itemManager.index(def);
                getLogger().info("Loaded item: " + def.getId());
            } catch (IOException e) {
                getLogger().log(Level.WARNING, "Failed to load item: " + file.getName(), e);
            } catch (IllegalStateException e) {
                getLogger().warning("Duplicate item ID in file: " + file.getName());
            }
        }
        getLogger().info("Loaded " + registry.size() + " items");
    }

    private void startFileWatcher() {
        Path itemsPath = getItemsDirectory().toPath();
        try {
            yamlWatcher = new YamlWatcher(itemsPath, file -> {
                getLogger().info("Hot reloading: " + file.getName());
                try {
                    ItemDefinition def = yamlLoader.load(file);
                    if (def.getId() == null) {
                        getLogger().warning("Reloaded file missing 'id': " + file.getName());
                        return;
                    }
                    registry.replace(def.getId(), def);
                    itemManager.index(def);
                    recipeManager.reloadRecipes();
                    getServer().getPluginManager().callEvent(new AssetReloadEvent(def.getId(), "item"));

                    // Active push using InventoryScanner
                    getServer().getOnlinePlayers().forEach(player -> {
                        int count = inventoryScanner.replaceInInventory(player, def);
                        if (count > 0) {
                            getLogger().fine("Updated " + count + " items for " + player.getName());
                        }
                    });

                    getLogger().info("Hot reload complete: " + def.getId());
                } catch (IOException e) {
                    getLogger().log(Level.WARNING, "Failed to hot reload: " + file.getName(), e);
                }
            }, getLogger());
            yamlWatcher.start();
        } catch (IOException e) {
            getLogger().log(Level.WARNING, "Failed to start file watcher", e);
        }
    }

    private void registerCommands() {
        var cmd = getServer().getPluginCommand("ugc");
        if (cmd != null) {
            cmd.setExecutor(new UgcCommand() {
                @Override
                protected boolean execute(CommandSender sender, Command command, String label, String[] args) {
                    if (args.length == 0) {
                        sender.sendMessage("§6mineUGC v" + getPluginMeta().getVersion());
                        return true;
                    }

                    switch (args[0].toLowerCase()) {
                        case "list" -> {
                            var items = itemManager.getAllDefinitions();
                            sender.sendMessage("§6Items (" + items.size() + "):");
                            items.forEach(i -> sender.sendMessage(" §7- §f" + i.getId()));
                        }
                        case "give" -> {
                            if (!requirePlayer(sender)) return true;
                            if (args.length < 2) {
                                sender.sendMessage("§cUsage: /ugc give <item_id>");
                                return true;
                            }
                            ItemDefinition def = itemManager.getDefinition(args[1]);
                            if (def == null) {
                                sender.sendMessage("§cItem not found: " + args[1]);
                                return true;
                            }
                            Player p = (Player) sender;
                            var item = itemManager.createItemStack(def);
                            new AttributeApplier().apply(item, def.getAttributes());
                            p.getInventory().addItem(item);
                            p.sendMessage("§aReceived " + def.getName() + "§a.");
                        }
                        case "reload" -> {
                            registry.clear();
                            for (ItemDefinition d : itemManager.getAllDefinitions()) {
                                itemManager.remove(d.getId());
                            }
                            loadAllItems();
                            recipeManager.reloadRecipes();
                            sender.sendMessage("§aAll items reloaded.");
                        }
                        case "edit" -> {
                            if (!requirePlayer(sender)) return true;
                            Player p = (Player) sender;
                            new MainMenuInventory(p, itemManager, guiListener).open(p);
                        }
                        default ->
                            sender.sendMessage("§cUsage: /ugc <list|give|reload|edit>");
                    }
                    return true;
                }

                @Override
                protected List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args) {
                    if (args.length == 1) {
                        return List.of("list", "give", "reload", "edit");
                    }
                    if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
                        return itemManager.getAllDefinitions().stream()
                                .map(ItemDefinition::getId)
                                .filter(id -> id.startsWith(args[1].toLowerCase()))
                                .toList();
                    }
                    return List.of();
                }
            });
        }
    }

    private File getItemsDirectory() {
        return new File(getDataFolder(), getConfig().getString("items-directory", "items"));
    }
}
