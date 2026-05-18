package org.mineUGC.plugin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.mineUGC.core.command.UgcCommand;
import org.mineUGC.core.event.AssetReloadEvent;
import org.mineUGC.core.message.Messages;
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
import org.mineUGC.game.GameManager;
import org.mineUGC.game.GameRegistry;
import org.mineUGC.game.GameTickTask;
import org.mineUGC.game.YamlGameLoader;
import org.mineUGC.game.listeners.GamePlayerListener;
import org.mineUGC.game.model.GameSession;

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
    private Messages messages;
    private GameRegistry gameRegistry;
    private YamlGameLoader gameLoader;
    private GameManager gameManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.messages = new Messages();

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
        this.guiListener = new GuiListener(this, itemManager, getItemsDirectory(), messages);

        // Game engine
        this.gameRegistry = new GameRegistry();
        this.gameLoader = new YamlGameLoader();
        this.gameManager = new GameManager(this, gameRegistry);

        // Load game definitions from YAML
        loadGameDefinitions();

        // Register game listener
        getServer().getPluginManager().registerEvents(
                new GamePlayerListener(gameManager), this);

        // Start game tick task (1 tick per second)
        new GameTickTask(gameManager).runTaskTimer(this, 0L, 20L);

        // Load items from files
        loadAllItems();

        // Register recipes
        recipeManager.reloadRecipes();

        // Register listeners
        FastInvManager.register(this);
        getServer().getPluginManager().registerEvents(
                new ItemListener(itemManager, abilityExecutor, playerDataDAO, getLogger(), messages), this);
        getServer().getPluginManager().registerEvents(guiListener, this);

        // Register commands
        registerCommands();

        // Start file watcher for hot reload
        startFileWatcher();

        getLogger().info("mineUGC v" + getPluginMeta().getVersion() + " enabled");
    }

    @Override
    public void onDisable() {
        if (gameManager != null) gameManager.shutdown();
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

    private void loadGameDefinitions() {
        File gamesDir = new File(getDataFolder(), "games");
        if (!gamesDir.exists()) {
            gamesDir.mkdirs();
            // Save example definition
            saveResource("games/classic_battle_royale.yml", false);
        }

        File[] files = gamesDir.listFiles((dir, name) ->
                name.endsWith(".yml") || name.endsWith(".yaml"));
        if (files == null) return;

        for (File file : files) {
            try {
                var def = gameLoader.load(file);
                if (def.getId() == null) continue;
                gameRegistry.register(def);
                getLogger().info("Loaded game: " + def.getId());
            } catch (Exception e) {
                getLogger().warning("Failed to load game: " + file.getName());
            }
        }
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
        GameRegistry gameReg = this.gameRegistry;
        GameManager gm = this.gameManager;
        var cmd = getServer().getPluginCommand("ugc");
        if (cmd != null) {
            cmd.setExecutor(new UgcCommand(messages) {
                @Override
                protected boolean execute(CommandSender sender, Command command, String label, String[] args) {
                    if (args.length == 0) {
                        sender.sendMessage(messages.get("command.version", getPluginMeta().getVersion()));
                        return true;
                    }

                    switch (args[0].toLowerCase()) {
                        case "list" -> {
                            var items = itemManager.getAllDefinitions();
                            sender.sendMessage(messages.get("command.items-header", items.size()));
                            items.forEach(i -> sender.sendMessage(messages.get("command.item-entry", i.getId())));
                        }
                        case "give" -> {
                            if (!requirePlayer(sender)) return true;
                            if (args.length < 2) {
                                sender.sendMessage(messages.get("command.give-usage"));
                                return true;
                            }
                            ItemDefinition def = itemManager.getDefinition(args[1]);
                            if (def == null) {
                                sender.sendMessage(messages.get("command.item-not-found", args[1]));
                                return true;
                            }
                            Player p = (Player) sender;
                            var item = itemManager.createItemStack(def);
                            new AttributeApplier().apply(item, def.getAttributes());
                            p.getInventory().addItem(item);
                            p.sendMessage(messages.get("command.item-received", def.getName()));
                        }
                        case "reload" -> {
                            registry.clear();
                            for (ItemDefinition d : itemManager.getAllDefinitions()) {
                                itemManager.remove(d.getId());
                            }
                            loadAllItems();
                            recipeManager.reloadRecipes();
                            sender.sendMessage(messages.get("command.all-reloaded"));
                        }
                        case "edit" -> {
                            if (!requirePlayer(sender)) return true;
                            Player p = (Player) sender;
                            new MainMenuInventory(p, itemManager, guiListener).open(p);
                        }
                        case "game" -> {
                            if (!requirePlayer(sender)) return true;
                            Player p = (Player) sender;
                            if (args.length < 2) {
                                p.sendMessage(messages.get("command.game-usage"));
                                return true;
                            }
                            switch (args[1].toLowerCase()) {
                                case "create" -> {
                                    if (args.length < 3) {
                                        p.sendMessage("§c/ugc game create <gameId>");
                                        return true;
                                    }
                                    String worldName = args.length > 3 ? args[3] : null;
                                    GameSession session = gm.createSession(args[2], worldName);
                                    if (session != null) {
                                        p.sendMessage("§a游戏已创建! ID: " + session.getId());
                                    } else {
                                        p.sendMessage("§c创建失败，未找到游戏定义: " + args[2]);
                                    }
                                }
                                case "join" -> {
                                    if (args.length < 3) {
                                        p.sendMessage("§c/ugc game join <sessionId>");
                                        return true;
                                    }
                                    if (gm.joinSession(args[2], p)) {
                                        p.sendMessage("§a已加入游戏 " + args[2]);
                                    } else {
                                        p.sendMessage("§c加入失败");
                                    }
                                }
                                case "leave" -> {
                                    gm.leaveSession(p);
                                    p.sendMessage("§a已离开游戏");
                                }
                                case "start" -> {
                                    if (args.length < 3) {
                                        // Try to start the session the player is in
                                        GameSession s = gm.getPlayerSession(p);
                                        if (s != null && gm.startSession(s.getId())) {
                                            p.sendMessage("§a已开始游戏");
                                        } else {
                                            p.sendMessage("§c未找到游戏或无法开始");
                                        }
                                        return true;
                                    }
                                    if (gm.startSession(args[2])) {
                                        p.sendMessage("§a已开始游戏 " + args[2]);
                                    } else {
                                        p.sendMessage("§c开始失败");
                                    }
                                }
                                case "stop" -> {
                                    String id = args.length > 2 ? args[2] : null;
                                    if (id == null) {
                                        GameSession s = gm.getPlayerSession(p);
                                        if (s != null) id = s.getId();
                                    }
                                    if (id != null && gm.stopSession(id)) {
                                        p.sendMessage("§a已停止游戏 " + id);
                                    } else {
                                        p.sendMessage("§c停止失败");
                                    }
                                }
                                case "list" -> {
                                    var sessions = gm.getActiveSessions();
                                    if (sessions.isEmpty()) {
                                        p.sendMessage("§7暂无活跃游戏");
                                    } else {
                                        p.sendMessage("§e活跃游戏:");
                                        sessions.forEach(s -> p.sendMessage(
                                                " §7" + s.getId() + " (" + s.getDefinition().getName()
                                                + ") §f" + s.getPhase()));
                                    }
                                }
                                default -> p.sendMessage("§c用法: /ugc game create|join|leave|start|stop|list");
                            }
                        }
                        default ->
                            sender.sendMessage(messages.get("command.usage"));
                    }
                    return true;
                }

                @Override
                protected List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args) {
                    if (args.length == 1) {
                        return List.of("list", "give", "reload", "edit", "game");
                    }
                    if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
                        return itemManager.getAllDefinitions().stream()
                                .map(ItemDefinition::getId)
                                .filter(id -> id.startsWith(args[1].toLowerCase()))
                                .toList();
                    }
                    if (args.length == 2 && args[0].equalsIgnoreCase("game")) {
                        return List.of("create", "join", "leave", "start", "stop", "list");
                    }
                    if (args.length == 3 && args[0].equalsIgnoreCase("game") &&
                        (args[1].equalsIgnoreCase("create"))) {
                        return gameReg.getAll().stream()
                                .map(g -> g.getId())
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
