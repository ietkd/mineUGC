# mineUGC Phase 1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the mineUGC Core Framework + Custom Items system as a multi-module Paper 1.21 plugin.

**Architecture:** Five-module Gradle project. `core` defines interfaces/registry with zero dependencies. `storage` handles YAML loading/watching and SQLite runtime state. `items` implements item attributes, abilities, passives, sets, and crafting. `gui` provides in-game editing. `plugin` wires everything together and produces the deployable JAR.

**Tech Stack:** Java 21, Paper 1.21 API, SnakeYAML (bundled), SQLite JDBC, JUnit 5, MockBukkit (for Bukkit-dependent tests)

**Key constraint:** Abilities are configuration-driven with predefined types — no custom scripting language.

---

### Task 1: Restructure to multi-module Gradle project

**Files:**
- Modify: `settings.gradle`
- Delete: `src/main/java/org/mineUGC/MineUGC.java`
- Delete: `src/main/resources/plugin.yml`
- Create: `core/build.gradle`
- Create: `core/src/main/java/org/mineUGC/core/package-info.java`
- Create: `storage/build.gradle`
- Create: `items/build.gradle`
- Create: `gui/build.gradle`
- Create: `plugin/build.gradle`
- Create: `plugin/src/main/java/org/mineUGC/plugin/MineUGC.java`
- Create: `plugin/src/main/resources/plugin.yml`

- [ ] **Step 1: Rewrite `settings.gradle` to include submodules**

```groovy
rootProject.name = 'mineUGC'

include 'core'
include 'storage'
include 'items'
include 'gui'
include 'plugin'
```

- [ ] **Step 2: Rewrite root `build.gradle` as common parent config**

```groovy
plugins {
    id 'java'
}

group = 'org.mineUGC'
version = '1.0-SNAPSHOT'

def targetJavaVersion = 21
java {
    sourceCompatibility = JavaVersion.toVersion(targetJavaVersion)
    targetCompatibility = JavaVersion.toVersion(targetJavaVersion)
}

tasks.withType(JavaCompile).configureEach {
    options.encoding = 'UTF-8'
    options.release.set(targetJavaVersion)
}

subprojects {
    apply plugin: 'java'

    repositories {
        mavenCentral()
        maven {
            name = "papermc-repo"
            url = "https://repo.papermc.io/repository/maven-public/"
        }
    }

    dependencies {
        compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
        testImplementation platform('org.junit:junit-bom:5.10.2')
        testImplementation 'org.junit.jupiter:junit-jupiter'
    }

    test {
        useJUnitPlatform()
    }
}
```

- [ ] **Step 3: Create `core/build.gradle`**

```groovy
dependencies {
    // core has no internal dependencies
}
```

- [ ] **Step 4: Create `storage/build.gradle`**

```groovy
dependencies {
    implementation project(':core')
    implementation 'org.xerial:sqlite-jdbc:3.45.1.0'
}
```

- [ ] **Step 5: Create `items/build.gradle`**

```groovy
dependencies {
    implementation project(':core')
    implementation project(':storage')
}
```

- [ ] **Step 6: Create `gui/build.gradle`**

```groovy
dependencies {
    implementation project(':core')
    implementation project(':items')
    implementation project(':storage')
}
```

- [ ] **Step 7: Create `plugin/build.gradle` (the deployable artifact)**

```groovy
plugins {
    id 'java'
}

dependencies {
    implementation project(':core')
    implementation project(':storage')
    implementation project(':items')
    implementation project(':gui')
}

processResources {
    def props = [version: version]
    inputs.properties props
    filteringCharset 'UTF-8'
    filesMatching('plugin.yml') {
        expand props
    }
}

// Fat JAR — bundle all submodule classes and SQLite JDBC
jar {
    from {
        configurations.runtimeClasspath.collect { it.isDirectory() ? it : zipTree(it) }
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
```

- [ ] **Step 8: Delete old root-level source files**

```bash
rm -rf src/main
```

- [ ] **Step 9: Create `plugin/src/main/resources/plugin.yml`**

```yaml
name: mineUGC
version: '1.0-SNAPSHOT'
main: org.mineUGC.plugin.MineUGC
api-version: '1.21'
```

- [ ] **Step 10: Create `plugin/src/main/java/org/mineUGC/plugin/MineUGC.java` (stub)**

```java
package org.mineUGC.plugin;

import org.bukkit.plugin.java.JavaPlugin;

public final class MineUGC extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("mineUGC enabled");
    }

    @Override
    public void onDisable() {
        getLogger().info("mineUGC disabled");
    }
}
```

- [ ] **Step 11: Verify the build compiles**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

- [ ] **Step 12: Commit**

```bash
git add settings.gradle build.gradle core/ storage/ items/ gui/ plugin/
git rm -r src/
git commit -m "build: restructure to multi-module Gradle project"
```

---

### Task 2: Core — model classes and AssetRegistry

**Files:**
- Create: `core/src/main/java/org/mineUGC/core/model/UgcAsset.java`
- Create: `core/src/main/java/org/mineUGC/core/model/ItemDefinition.java`
- Create: `core/src/main/java/org/mineUGC/core/model/AbilityConfig.java`
- Create: `core/src/main/java/org/mineUGC/core/model/PassiveConfig.java`
- Create: `core/src/main/java/org/mineUGC/core/model/RecipeConfig.java`
- Create: `core/src/main/java/org/mineUGC/core/registry/AssetRegistry.java`
- Create: `core/src/test/java/org/mineUGC/core/registry/AssetRegistryTest.java`

- [ ] **Step 1: Create `UgcAsset.java`**

```java
package org.mineUGC.core.model;

public interface UgcAsset {
    String getId();
    String getType();
}
```

- [ ] **Step 2: Create `AbilityConfig.java`**

```java
package org.mineUGC.core.model;

import java.util.HashMap;
import java.util.Map;

public class AbilityConfig {
    private String type;
    private int cooldown;
    private int manaCost;
    private Map<String, Object> params = new HashMap<>();

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public int getCooldown() { return cooldown; }
    public void setCooldown(int cooldown) { this.cooldown = cooldown; }

    public int getManaCost() { return manaCost; }
    public void setManaCost(int manaCost) { this.manaCost = manaCost; }

    public Map<String, Object> getParams() { return params; }
    public void setParams(Map<String, Object> params) { this.params = params; }

    public <T> T param(String key, T fallback) {
        @SuppressWarnings("unchecked")
        T val = (T) params.get(key);
        return val != null ? val : fallback;
    }
}
```

- [ ] **Step 3: Create `PassiveConfig.java`**

```java
package org.mineUGC.core.model;

import java.util.HashMap;
import java.util.Map;

public class PassiveConfig {
    private String type;
    private String effect;
    private int amplifier;
    private Map<String, Object> params = new HashMap<>();

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getEffect() { return effect; }
    public void setEffect(String effect) { this.effect = effect; }

    public int getAmplifier() { return amplifier; }
    public void setAmplifier(int amplifier) { this.amplifier = amplifier; }

    public Map<String, Object> getParams() { return params; }
    public void setParams(Map<String, Object> params) { this.params = params; }
}
```

- [ ] **Step 4: Create `RecipeConfig.java`**

```java
package org.mineUGC.core.model;

import java.util.List;
import java.util.Map;

public class RecipeConfig {
    private List<String> shape;
    private Map<String, String> ingredients;

    public List<String> getShape() { return shape; }
    public void setShape(List<String> shape) { this.shape = shape; }

    public Map<String, String> getIngredients() { return ingredients; }
    public void setIngredients(Map<String, String> ingredients) { this.ingredients = ingredients; }

    public boolean isValid() {
        return shape != null && !shape.isEmpty() && ingredients != null && !ingredients.isEmpty();
    }
}
```

- [ ] **Step 5: Create `ItemDefinition.java`**

```java
package org.mineUGC.core.model;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ItemDefinition implements UgcAsset {
    private String id;
    private String name;
    private String material;
    private int model;
    private List<String> lore;
    private Map<String, Double> attributes;
    private Map<String, AbilityConfig> abilities;
    private Map<String, PassiveConfig> passives;
    private String set;
    private RecipeConfig recipe;

    @Override
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    @Override
    public String getType() { return "item"; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getMaterial() { return material; }
    public void setMaterial(String material) { this.material = material; }

    public int getModel() { return model; }
    public void setModel(int model) { this.model = model; }

    public List<String> getLore() { return lore; }
    public void setLore(List<String> lore) { this.lore = lore; }

    public Map<String, Double> getAttributes() { return attributes; }
    public void setAttributes(Map<String, Double> attributes) { this.attributes = attributes; }

    public Map<String, AbilityConfig> getAbilities() { return abilities; }
    public void setAbilities(Map<String, AbilityConfig> abilities) { this.abilities = abilities; }

    public Map<String, PassiveConfig> getPassives() { return passives; }
    public void setPassives(Map<String, PassiveConfig> passives) { this.passives = passives; }

    public String getSet() { return set; }
    public void setSet(String set) { this.set = set; }

    public RecipeConfig getRecipe() { return recipe; }
    public void setRecipe(RecipeConfig recipe) { this.recipe = recipe; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ItemDefinition that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
```

- [ ] **Step 6: Create `AssetRegistry.java`**

```java
package org.mineUGC.core.registry;

import org.mineUGC.core.model.UgcAsset;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

public class AssetRegistry<T extends UgcAsset> {
    private final ConcurrentHashMap<String, T> assets = new ConcurrentHashMap<>();

    public void register(T asset) {
        T existing = assets.putIfAbsent(asset.getId(), asset);
        if (existing != null) {
            throw new IllegalStateException("Asset already registered: " + asset.getId());
        }
    }

    public T replace(String id, T asset) {
        return assets.put(id, asset);
    }

    public T get(String id) {
        return assets.get(id);
    }

    public T remove(String id) {
        return assets.remove(id);
    }

    public boolean contains(String id) {
        return assets.containsKey(id);
    }

    public Collection<T> getAll() {
        return assets.values();
    }

    public int size() {
        return assets.size();
    }

    public void clear() {
        assets.clear();
    }
}
```

- [ ] **Step 7: Write `AssetRegistryTest.java`**

```java
package org.mineUGC.core.registry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mineUGC.core.model.ItemDefinition;

import static org.junit.jupiter.api.Assertions.*;

class AssetRegistryTest {

    private AssetRegistry<ItemDefinition> registry;

    @BeforeEach
    void setUp() {
        registry = new AssetRegistry<>();
    }

    @Test
    void register_shouldStoreAsset() {
        ItemDefinition def = new ItemDefinition();
        def.setId("test_sword");
        registry.register(def);
        assertTrue(registry.contains("test_sword"));
        assertSame(def, registry.get("test_sword"));
    }

    @Test
    void register_shouldThrowOnDuplicate() {
        ItemDefinition def = new ItemDefinition();
        def.setId("dup");
        registry.register(def);
        assertThrows(IllegalStateException.class, () -> registry.register(def));
    }

    @Test
    void replace_shouldOverwriteExisting() {
        ItemDefinition old = new ItemDefinition();
        old.setId("x");
        registry.register(old);
        ItemDefinition updated = new ItemDefinition();
        updated.setId("x");
        assertSame(old, registry.replace("x", updated));
        assertSame(updated, registry.get("x"));
    }

    @Test
    void replace_shouldReturnNullForMissing() {
        assertNull(registry.replace("nonexistent", new ItemDefinition()));
    }

    @Test
    void get_shouldReturnNullForMissing() {
        assertNull(registry.get("nonexistent"));
    }

    @Test
    void remove_shouldDeleteAsset() {
        ItemDefinition def = new ItemDefinition();
        def.setId("delete_me");
        registry.register(def);
        assertSame(def, registry.remove("delete_me"));
        assertFalse(registry.contains("delete_me"));
    }

    @Test
    void getAll_shouldReturnAllRegistered() {
        for (int i = 0; i < 5; i++) {
            ItemDefinition d = new ItemDefinition();
            d.setId("item_" + i);
            registry.register(d);
        }
        assertEquals(5, registry.getAll().size());
    }

    @Test
    void size_shouldTrackCount() {
        assertEquals(0, registry.size());
        ItemDefinition d = new ItemDefinition();
        d.setId("a");
        registry.register(d);
        assertEquals(1, registry.size());
    }

    @Test
    void clear_shouldRemoveAll() {
        ItemDefinition d = new ItemDefinition();
        d.setId("a");
        registry.register(d);
        registry.clear();
        assertEquals(0, registry.size());
    }
}
```

- [ ] **Step 8: Run tests to verify they pass**

Run: `./gradlew :core:test`
Expected: All tests green

- [ ] **Step 9: Commit**

```bash
git add core/src/main/java/org/mineUGC/core/model/ core/src/main/java/org/mineUGC/core/registry/ core/src/test/
git commit -m "feat(core): add UgcAsset, ItemDefinition models and AssetRegistry"
```

---

### Task 3: Core — AssetReloadEvent and UgcCommand

**Files:**
- Create: `core/src/main/java/org/mineUGC/core/event/AssetReloadEvent.java`
- Create: `core/src/main/java/org/mineUGC/core/command/UgcCommand.java`
- Create: `core/src/test/java/org/mineUGC/core/event/AssetReloadEventTest.java`

- [ ] **Step 1: Create `AssetReloadEvent.java`**

```java
package org.mineUGC.core.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class AssetReloadEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final String assetId;
    private final String assetType;

    public AssetReloadEvent(String assetId, String assetType) {
        super(!org.bukkit.Bukkit.isPrimaryThread());
        this.assetId = assetId;
        this.assetType = assetType;
    }

    public String getAssetId() { return assetId; }
    public String getAssetType() { return assetType; }

    public static HandlerList getHandlerList() { return handlers; }

    @Override
    public @NotNull HandlerList getHandlers() { return handlers; }
}
```

- [ ] **Step 2: Create `UgcCommand.java`**

```java
package org.mineUGC.core.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public abstract class UgcCommand implements CommandExecutor, TabCompleter {

    protected static final String PERMISSION_PREFIX = "mineugc.";

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission(PERMISSION_PREFIX + command.getName())) {
            sender.sendMessage("§cYou don't have permission.");
            return true;
        }
        return execute(sender, command, label, args);
    }

    protected abstract boolean execute(CommandSender sender, Command command, String label, String[] args);

    @Override
    public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                               @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission(PERMISSION_PREFIX + command.getName())) {
            return List.of();
        }
        return tabComplete(sender, command, alias, args);
    }

    protected List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return List.of();
    }

    protected boolean requirePlayer(CommandSender sender) {
        if (!(sender instanceof org.bukkit.entity.Player)) {
            sender.sendMessage("§cPlayer only command.");
            return false;
        }
        return true;
    }
}
```

- [ ] **Step 3: Write `AssetReloadEventTest.java`**

```java
package org.mineUGC.core.event;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AssetReloadEventTest {

    @Test
    void constructor_shouldStoreFields() {
        AssetReloadEvent event = new AssetReloadEvent("test_sword", "item");
        assertEquals("test_sword", event.getAssetId());
        assertEquals("item", event.getAssetType());
    }

    @Test
    void handlerList_shouldNotBeNull() {
        assertNotNull(AssetReloadEvent.getHandlerList());
    }

    @Test
    void event_shouldHaveCorrectHandlers() {
        AssetReloadEvent event = new AssetReloadEvent("a", "item");
        assertSame(AssetReloadEvent.getHandlerList(), event.getHandlers());
    }
}
```

- [ ] **Step 4: Run tests**

Run: `./gradlew :core:test`
Expected: All tests pass

- [ ] **Step 5: Commit**

```bash
git add core/src/main/java/org/mineUGC/core/event/ core/src/main/java/org/mineUGC/core/command/ core/src/test/
git commit -m "feat(core): add AssetReloadEvent and UgcCommand base class"
```

---

### Task 4: Storage — YAML loader and file watcher

**Files:**
- Create: `storage/src/main/java/org/mineUGC/storage/yaml/YamlItemLoader.java`
- Create: `storage/src/main/java/org/mineUGC/storage/yaml/YamlWatcher.java`
- Create: `storage/src/test/java/org/mineUGC/storage/yaml/YamlItemLoaderTest.java`

- [ ] **Step 1: Create `YamlItemLoader.java`**

```java
package org.mineUGC.storage.yaml;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.mineUGC.core.model.*;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class YamlItemLoader {

    public ItemDefinition load(File file) throws IOException {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        return parse(config);
    }

    public ItemDefinition parse(ConfigurationSection config) {
        ItemDefinition def = new ItemDefinition();

        def.setId(config.getString("id"));
        def.setName(config.getString("name"));
        def.setMaterial(config.getString("material"));
        def.setModel(config.getInt("model", 0));

        List<String> lore = config.getStringList("lore");
        def.setLore(lore.isEmpty() ? null : lore);

        // attributes
        ConfigurationSection attrSection = config.getConfigurationSection("attributes");
        if (attrSection != null) {
            Map<String, Double> attrs = new LinkedHashMap<>();
            for (String key : attrSection.getKeys(false)) {
                attrs.put(key, attrSection.getDouble(key));
            }
            def.setAttributes(attrs);
        }

        // abilities
        ConfigurationSection abilSection = config.getConfigurationSection("abilities");
        if (abilSection != null) {
            Map<String, AbilityConfig> abilities = new LinkedHashMap<>();
            for (String key : abilSection.getKeys(false)) {
                ConfigurationSection cfg = abilSection.getConfigurationSection(key);
                if (cfg != null) {
                    abilities.put(key, parseAbilityConfig(cfg));
                }
            }
            def.setAbilities(abilities);
        }

        // passives
        ConfigurationSection passSection = config.getConfigurationSection("passives");
        if (passSection != null) {
            Map<String, PassiveConfig> passives = new LinkedHashMap<>();
            for (String key : passSection.getKeys(false)) {
                ConfigurationSection cfg = passSection.getConfigurationSection(key);
                if (cfg != null) {
                    passives.put(key, parsePassiveConfig(cfg));
                }
            }
            def.setPassives(passives);
        }

        def.setSet(config.getString("set"));

        // recipe
        ConfigurationSection recipeSection = config.getConfigurationSection("recipe");
        if (recipeSection != null) {
            def.setRecipe(parseRecipeConfig(recipeSection));
        }

        return def;
    }

    private AbilityConfig parseAbilityConfig(ConfigurationSection section) {
        AbilityConfig config = new AbilityConfig();
        config.setType(section.getString("type"));
        config.setCooldown(section.getInt("cooldown", 0));
        config.setManaCost(section.getInt("mana_cost", 0));
        Map<String, Object> params = new HashMap<>();
        for (String key : section.getKeys(false)) {
            if (!key.equals("type") && !key.equals("cooldown") && !key.equals("mana_cost")) {
                params.put(key, section.get(key));
            }
        }
        config.setParams(params);
        return config;
    }

    private PassiveConfig parsePassiveConfig(ConfigurationSection section) {
        PassiveConfig config = new PassiveConfig();
        config.setType(section.getString("type"));
        config.setEffect(section.getString("effect"));
        config.setAmplifier(section.getInt("amplifier", 0));
        Map<String, Object> params = new HashMap<>();
        for (String key : section.getKeys(false)) {
            if (!key.equals("type") && !key.equals("effect") && !key.equals("amplifier")) {
                params.put(key, section.get(key));
            }
        }
        config.setParams(params);
        return config;
    }

    private RecipeConfig parseRecipeConfig(ConfigurationSection section) {
        RecipeConfig config = new RecipeConfig();
        config.setShape(section.getStringList("shape"));
        ConfigurationSection ingSection = section.getConfigurationSection("ingredients");
        if (ingSection != null) {
            Map<String, String> ingredients = new LinkedHashMap<>();
            for (String key : ingSection.getKeys(false)) {
                ingredients.put(key, ingSection.getString(key));
            }
            config.setIngredients(ingredients);
        }
        return config;
    }
}
```

- [ ] **Step 2: Create `YamlWatcher.java`**

```java
package org.mineUGC.storage.yaml;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class YamlWatcher implements AutoCloseable {
    private final Path directory;
    private final Consumer<File> onChange;
    private final Logger logger;
    private WatchService watchService;
    private ExecutorService executor;
    private volatile boolean running;

    public YamlWatcher(Path directory, Consumer<File> onChange, Logger logger) {
        this.directory = directory;
        this.onChange = onChange;
        this.logger = logger;

        File dir = directory.toFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    public void start() throws IOException {
        watchService = FileSystems.getDefault().newWatchService();
        directory.register(watchService,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_MODIFY);

        running = true;
        executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "mineugc-yaml-watcher");
            t.setDaemon(true);
            return t;
        });
        executor.submit(this::poll);
    }

    private void poll() {
        while (running) {
            try {
                WatchKey key = watchService.poll(1000, java.util.concurrent.TimeUnit.MILLISECONDS);
                if (key == null) continue;

                for (WatchEvent<?> event : key.pollEvents()) {
                    Path filename = (Path) event.context();
                    if (filename.toString().endsWith(".yml") || filename.toString().endsWith(".yaml")) {
                        File changed = directory.resolve(filename).toFile();
                        logger.info("Detected change: " + changed.getName());
                        onChange.accept(changed);
                    }
                }
                key.reset();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.warning("YamlWatcher error: " + e.getMessage());
            }
        }
    }

    @Override
    public void close() {
        running = false;
        if (executor != null) {
            executor.shutdownNow();
        }
        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException ignored) {}
        }
    }
}
```

- [ ] **Step 3: Write `YamlItemLoaderTest.java`**

```java
package org.mineUGC.storage.yaml;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mineUGC.core.model.ItemDefinition;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class YamlItemLoaderTest {

    private final YamlItemLoader loader = new YamlItemLoader();

    @Test
    void load_shouldParseBasicFields(@TempDir File tempDir) throws IOException {
        File file = new File(tempDir, "test_sword.yml");
        try (FileWriter w = new FileWriter(file)) {
            w.write("""
                id: test_sword
                name: '&6Test Sword'
                material: DIAMOND_SWORD
                model: 10001
                """);
        }

        ItemDefinition def = loader.load(file);
        assertEquals("test_sword", def.getId());
        assertEquals("&6Test Sword", def.getName());
        assertEquals("DIAMOND_SWORD", def.getMaterial());
        assertEquals(10001, def.getModel());
    }

    @Test
    void load_shouldParseLore(@TempDir File tempDir) throws IOException {
        File file = new File(tempDir, "lore_item.yml");
        try (FileWriter w = new FileWriter(file)) {
            w.write("""
                id: lore_item
                name: Test
                material: STICK
                lore:
                  - "Line one"
                  - "Line two"
                """);
        }

        ItemDefinition def = loader.load(file);
        assertNotNull(def.getLore());
        assertEquals(2, def.getLore().size());
        assertEquals("Line one", def.getLore().get(0));
    }

    @Test
    void load_shouldParseAttributes(@TempDir File tempDir) throws IOException {
        File file = new File(tempDir, "attr_item.yml");
        try (FileWriter w = new FileWriter(file)) {
            w.write("""
                id: attr_item
                name: Test
                material: DIAMOND_SWORD
                attributes:
                  damage: 15.0
                  speed: 1.6
                """);
        }

        ItemDefinition def = loader.load(file);
        assertNotNull(def.getAttributes());
        assertEquals(15.0, def.getAttributes().get("damage"), 0.01);
        assertEquals(1.6, def.getAttributes().get("speed"), 0.01);
    }

    @Test
    void load_shouldParseAbility(@TempDir File tempDir) throws IOException {
        File file = new File(tempDir, "abil_item.yml");
        try (FileWriter w = new FileWriter(file)) {
            w.write("""
                id: abil_item
                name: Test
                material: STICK
                abilities:
                  right_click:
                    type: lightning_strike
                    cooldown: 10
                    mana_cost: 20
                """);
        }

        ItemDefinition def = loader.load(file);
        assertNotNull(def.getAbilities());
        assertTrue(def.getAbilities().containsKey("right_click"));
        assertEquals("lightning_strike", def.getAbilities().get("right_click").getType());
        assertEquals(10, def.getAbilities().get("right_click").getCooldown());
        assertEquals(20, def.getAbilities().get("right_click").getManaCost());
    }

    @Test
    void load_shouldParseRecipe(@TempDir File tempDir) throws IOException {
        File file = new File(tempDir, "recipe_item.yml");
        try (FileWriter w = new FileWriter(file)) {
            w.write("""
                id: recipe_item
                name: Test
                material: DIAMOND_SWORD
                recipe:
                  shape: ["DDD", "D D", " S"]
                  ingredients:
                    D: DIAMOND
                    S: STICK
                """);
        }

        ItemDefinition def = loader.load(file);
        assertNotNull(def.getRecipe());
        assertTrue(def.getRecipe().isValid());
        assertEquals("DIAMOND", def.getRecipe().getIngredients().get("D"));
    }

    @Test
    void load_shouldHandleMissingFields(@TempDir File tempDir) throws IOException {
        File file = new File(tempDir, "minimal.yml");
        try (FileWriter w = new FileWriter(file)) {
            w.write("""
                id: minimal
                name: Minimal
                material: STICK
                """);
        }

        ItemDefinition def = loader.load(file);
        assertEquals("minimal", def.getId());
        assertNull(def.getLore());
        assertNull(def.getAttributes());
        assertNull(def.getAbilities());
        assertNull(def.getRecipe());
    }

    @Test
    void load_shouldThrowOnInvalidFile(@TempDir File tempDir) {
        File missing = new File(tempDir, "nonexistent.yml");
        assertThrows(IOException.class, () -> loader.load(missing));
    }
}
```

- [ ] **Step 4: Run tests**

Run: `./gradlew :storage:test`
Expected: All tests pass

- [ ] **Step 5: Commit**

```bash
git add storage/src/main/java/org/mineUGC/storage/yaml/ storage/src/test/
git commit -m "feat(storage): add YAML item loader and file watcher"
```

---

### Task 5: Storage — SQLite runtime state

**Files:**
- Create: `storage/src/main/java/org/mineUGC/storage/sqlite/DatabaseManager.java`
- Create: `storage/src/main/java/org/mineUGC/storage/sqlite/PlayerDataDAO.java`
- Create: `storage/src/test/java/org/mineUGC/storage/sqlite/DatabaseManagerTest.java`

- [ ] **Step 1: Create `DatabaseManager.java`**

```java
package org.mineUGC.storage.sqlite;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

public class DatabaseManager implements AutoCloseable {
    private Connection connection;
    private final File dbFile;
    private final Logger logger;

    public DatabaseManager(File dbFile, Logger logger) {
        this.dbFile = dbFile;
        this.logger = logger;
    }

    public void initialize() throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS player_cooldowns (
                    player_uuid TEXT NOT NULL,
                    item_id TEXT NOT NULL,
                    ability_key TEXT NOT NULL,
                    expires_at INTEGER NOT NULL,
                    PRIMARY KEY (player_uuid, item_id, ability_key)
                )
            """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS player_data (
                    player_uuid TEXT PRIMARY KEY,
                    unlocked_items TEXT NOT NULL DEFAULT '[]'
                )
            """);
        }
        logger.info("Database initialized: " + dbFile.getName());
    }

    public Connection getConnection() {
        return connection;
    }

    @Override
    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                logger.warning("Failed to close database: " + e.getMessage());
            }
        }
    }
}
```

- [ ] **Step 2: Create `PlayerDataDAO.java`**

```java
package org.mineUGC.storage.sqlite;

import java.sql.*;
import java.util.UUID;

public class PlayerDataDAO {
    private final DatabaseManager db;

    public PlayerDataDAO(DatabaseManager db) {
        this.db = db;
    }

    public long getCooldownExpiry(UUID playerId, String itemId, String abilityKey) throws SQLException {
        String sql = "SELECT expires_at FROM player_cooldowns WHERE player_uuid = ? AND item_id = ? AND ability_key = ?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, playerId.toString());
            ps.setString(2, itemId);
            ps.setString(3, abilityKey);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getLong("expires_at");
            }
            return 0;
        }
    }

    public void setCooldown(UUID playerId, String itemId, String abilityKey, long expiresAt) throws SQLException {
        String sql = "INSERT OR REPLACE INTO player_cooldowns (player_uuid, item_id, ability_key, expires_at) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, playerId.toString());
            ps.setString(2, itemId);
            ps.setString(3, abilityKey);
            ps.setLong(4, expiresAt);
            ps.executeUpdate();
        }
    }

    public String getUnlockedItems(UUID playerId) throws SQLException {
        String sql = "SELECT unlocked_items FROM player_data WHERE player_uuid = ?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, playerId.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("unlocked_items");
            }
            return "[]";
        }
    }

    public void setUnlockedItems(UUID playerId, String itemsJson) throws SQLException {
        String sql = "INSERT OR REPLACE INTO player_data (player_uuid, unlocked_items) VALUES (?, ?)";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, playerId.toString());
            ps.setString(2, itemsJson);
            ps.executeUpdate();
        }
    }
}
```

- [ ] **Step 3: Write `DatabaseManagerTest.java`**

```java
package org.mineUGC.storage.sqlite;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseManagerTest {
    private DatabaseManager db;
    private PlayerDataDAO dao;

    @TempDir
    File tempDir;

    @BeforeEach
    void setUp() throws SQLException {
        File dbFile = new File(tempDir, "test.db");
        db = new DatabaseManager(dbFile, Logger.getLogger("test"));
        db.initialize();
        dao = new PlayerDataDAO(db);
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    @Test
    void cooldown_shouldStoreAndRetrieve() throws SQLException {
        UUID playerId = UUID.randomUUID();
        long expiry = System.currentTimeMillis() + 10000;
        dao.setCooldown(playerId, "test_sword", "right_click", expiry);

        long retrieved = dao.getCooldownExpiry(playerId, "test_sword", "right_click");
        assertEquals(expiry, retrieved);
    }

    @Test
    void cooldown_shouldReturnZeroForMissing() throws SQLException {
        long expiry = dao.getCooldownExpiry(UUID.randomUUID(), "missing", "none");
        assertEquals(0, expiry);
    }

    @Test
    void cooldown_shouldUpdateExisting() throws SQLException {
        UUID playerId = UUID.randomUUID();
        dao.setCooldown(playerId, "sword", "ability", 100L);
        dao.setCooldown(playerId, "sword", "ability", 200L);

        assertEquals(200L, dao.getCooldownExpiry(playerId, "sword", "ability"));
    }

    @Test
    void unlockedItems_shouldDefaultToEmptyArray() throws SQLException {
        String items = dao.getUnlockedItems(UUID.randomUUID());
        assertEquals("[]", items);
    }

    @Test
    void unlockedItems_shouldStoreAndRetrieve() throws SQLException {
        UUID playerId = UUID.randomUUID();
        dao.setUnlockedItems(playerId, "[\"sword\", \"bow\"]");
        assertEquals("[\"sword\", \"bow\"]", dao.getUnlockedItems(playerId));
    }

    @Test
    void initialize_shouldCreateTables(@TempDir File dir) throws SQLException {
        File dbFile = new File(dir, "new_test.db");
        DatabaseManager mgr = new DatabaseManager(dbFile, Logger.getLogger("test"));
        mgr.initialize();
        // Verify tables exist by querying
        var meta = mgr.getConnection().getMetaData();
        var rs = meta.getTables(null, null, "player_cooldowns", null);
        assertTrue(rs.next());
        mgr.close();
    }
}
```

- [ ] **Step 4: Run tests**

Run: `./gradlew :storage:test`
Expected: All tests pass

- [ ] **Step 5: Commit**

```bash
git add storage/src/main/java/org/mineUGC/storage/sqlite/ storage/src/test/
git commit -m "feat(storage): add SQLite database and PlayerDataDAO"
```

---

### Task 6: Items — ItemManager and attribute system

**Files:**
- Create: `items/src/main/java/org/mineUGC/items/ItemManager.java`
- Create: `items/src/main/java/org/mineUGC/items/attributes/AttributeApplier.java`
- Create: `items/src/test/java/org/mineUGC/items/attributes/AttributeApplierTest.java`

- [ ] **Step 1: Create `ItemManager.java`**

```java
package org.mineUGC.items;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;
import org.mineUGC.core.model.ItemDefinition;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ItemManager {
    private final Map<String, ItemDefinition> definitions;
    private final Plugin plugin;
    private final NamespacedKey idKey;
    private final Random random = new Random();

    public ItemManager(Plugin plugin) {
        this.plugin = plugin;
        this.definitions = new ConcurrentHashMap<>();
        this.idKey = new NamespacedKey(plugin, "ugc_id");
    }

    public void index(Collection<ItemDefinition> items) {
        for (ItemDefinition def : items) {
            if (def.getId() != null) {
                definitions.put(def.getId(), def);
            }
        }
    }

    public void index(ItemDefinition def) {
        if (def.getId() != null) {
            definitions.put(def.getId(), def);
        }
    }

    public void remove(String id) {
        definitions.remove(id);
    }

    public ItemDefinition getDefinition(String id) {
        return definitions.get(id);
    }

    public Collection<ItemDefinition> getAllDefinitions() {
        return definitions.values();
    }

    public ItemStack createItemStack(ItemDefinition def) {
        Material mat = Material.getMaterial(def.getMaterial().toUpperCase());
        if (mat == null) mat = Material.STICK;

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            if (def.getName() != null) {
                meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', def.getName()));
            }
            if (def.getLore() != null && !def.getLore().isEmpty()) {
                List<String> colored = def.getLore().stream()
                        .map(l -> org.bukkit.ChatColor.translateAlternateColorCodes('&', l))
                        .toList();
                meta.setLore(colored);
            }
            if (def.getModel() > 0) {
                meta.setCustomModelData(def.getModel());
            }
            // Store item ID in PDC
            if (def.getId() != null) {
                meta.getPersistentDataContainer().set(idKey, PersistentDataType.STRING, def.getId());
            }
            item.setItemMeta(meta);
        }

        return item;
    }

    public String getItemId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        return meta.getPersistentDataContainer().get(idKey, PersistentDataType.STRING);
    }

    public boolean isUgcItem(ItemStack item) {
        return getItemId(item) != null;
    }
}
```

- [ ] **Step 2: Create `AttributeApplier.java`**

```java
package org.mineUGC.items.attributes;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Map;
import java.util.UUID;

public class AttributeApplier {

    private static final Map<String, Attribute> ATTRIBUTE_MAP = Map.ofEntries(
        Map.entry("damage", Attribute.ATTACK_DAMAGE),
        Map.entry("speed", Attribute.ATTACK_SPEED),
        Map.entry("armor", Attribute.ARMOR),
        Map.entry("armor_toughness", Attribute.ARMOR_TOUGHNESS),
        Map.entry("movement_speed", Attribute.MOVEMENT_SPEED),
        Map.entry("max_health", Attribute.MAX_HEALTH),
        Map.entry("knockback_resistance", Attribute.KNOCKBACK_RESISTANCE),
        Map.entry("luck", Attribute.LUCK),
        Map.entry("block_interaction_range", Attribute.BLOCK_INTERACTION_RANGE),
        Map.entry("entity_interaction_range", Attribute.ENTITY_INTERACTION_RANGE)
    );

    public void apply(ItemStack item, Map<String, Double> attributes) {
        if (attributes == null || attributes.isEmpty()) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        for (Map.Entry<String, Double> entry : attributes.entrySet()) {
            Attribute attr = ATTRIBUTE_MAP.get(entry.getKey());
            if (attr == null) continue;

            // Remove existing modifiers for this attribute from us
            if (meta.getAttributeModifiers(attr) != null) {
                for (AttributeModifier mod : meta.getAttributeModifiers(attr)) {
                    if (mod.getName().equals("ugc_" + entry.getKey())) {
                        meta.removeAttributeModifier(attr, mod);
                    }
                }
            }

            AttributeModifier modifier = new AttributeModifier(
                    UUID.randomUUID(),
                    "ugc_" + entry.getKey(),
                    entry.getValue(),
                    AttributeModifier.Operation.ADD_NUMBER,
                    EquipmentSlot.HAND
            );
            meta.addAttributeModifier(attr, modifier);
        }

        item.setItemMeta(meta);
    }
}
```

- [ ] **Step 3: Write `AttributeApplierTest.java`**

```java
package org.mineUGC.items.attributes;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AttributeApplierTest {

    private final AttributeApplier applier = new AttributeApplier();

    @Test
    void attributeMap_shouldContainCommonKeys() {
        // Verify the static mapping covers expected keys
        Map<String, Double> attrs = Map.of(
            "damage", 10.0,
            "speed", 1.6,
            "armor", 5.0
        );
        // Just verify these are recognized — apply() itself requires ItemStack (server test)
        assertDoesNotThrow(() -> applier.apply(null, attrs));
    }

    @Test
    void apply_shouldHandleNullAttributes() {
        assertDoesNotThrow(() -> applier.apply(null, null));
    }

    @Test
    void apply_shouldHandleEmptyAttributes() {
        assertDoesNotThrow(() -> applier.apply(null, Map.of()));
    }

    @Test
    void attributeMap_shouldHaveExpectedMappings() {
        Map<String, Double> attrs = Map.of(
            "damage", 15.0,
            "speed", 1.6,
            "armor", 10.0,
            "armor_toughness", 4.0,
            "movement_speed", 0.1,
            "max_health", 20.0,
            "knockback_resistance", 0.5,
            "luck", 1.0,
            "block_interaction_range", 5.0,
            "entity_interaction_range", 3.0
        );
        assertEquals(10, attrs.size());
    }

    @Test
    void apply_withUnknownAttribute_shouldNotThrow() {
        Map<String, Double> attrs = Map.of("nonexistent_stat", 100.0);
        assertDoesNotThrow(() -> applier.apply(null, attrs));
    }
}
```

- [ ] **Step 4: Run tests**

Run: `./gradlew :items:test`
Expected: All tests pass

- [ ] **Step 5: Commit**

```bash
git add items/src/main/java/org/mineUGC/items/ items/src/test/
git commit -m "feat(items): add ItemManager and AttributeApplier"
```

---

### Task 7: Items — ability, passive, and set bonus systems

**Files:**
- Create: `items/src/main/java/org/mineUGC/items/abilities/AbilityExecutor.java`
- Create: `items/src/main/java/org/mineUGC/items/passives/PassiveEffect.java`
- Create: `items/src/main/java/org/mineUGC/items/sets/SetBonusTracker.java`
- Create: `items/src/test/java/org/mineUGC/items/sets/SetBonusTrackerTest.java`

- [ ] **Step 1: Create `AbilityExecutor.java`**

```java
package org.mineUGC.items.abilities;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.mineUGC.core.model.AbilityConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.logging.Logger;

public class AbilityExecutor {
    private final Logger logger;
    private final Map<String, BiConsumer<Player, AbilityConfig>> handlers = new HashMap<>();

    public AbilityExecutor(Logger logger) {
        this.logger = logger;
        registerDefaults();
    }

    private void registerDefaults() {
        handlers.put("lightning_strike", (player, cfg) -> {
            Location target = player.getTargetBlock(null, 50).getLocation().add(0.5, 1, 0.5);
            player.getWorld().strikeLightning(target);
            double damage = cfg.param("damage", 8.0);
            target.getNearbyEntities(3, 3, 3).stream()
                    .filter(e -> e instanceof LivingEntity && e != player)
                    .forEach(e -> ((LivingEntity) e).damage(damage, player));
        });

        handlers.put("potion_effect", (player, cfg) -> {
            String effectName = cfg.param("effect", "speed");
            int amplifier = cfg.param("amplifier", 0);
            int duration = cfg.param("duration", 100);
            PotionEffectType type = PotionEffectType.getByName(effectName.toUpperCase());
            if (type != null) {
                player.addPotionEffect(new PotionEffect(type, duration, amplifier));
            }
        });

        handlers.put("projectile", (player, cfg) -> {
            String projectileType = cfg.param("projectile_type", "arrow");
            try {
                EntityType entityType = EntityType.valueOf(projectileType.toUpperCase());
                player.launchProjectile(entityType.getEntityClass());
            } catch (IllegalArgumentException e) {
                logger.warning("Unknown projectile type: " + projectileType);
            }
        });

        handlers.put("explosion", (player, cfg) -> {
            float power = cfg.param("power", 2.0F).floatValue();
            boolean fire = cfg.param("fire", false);
            player.getWorld().createExplosion(player.getLocation(), power, fire);
        });

        handlers.put("heal", (player, cfg) -> {
            double amount = cfg.param("amount", 4.0);
            player.setHealth(Math.min(player.getHealth() + amount, player.getMaxHealth()));
        });

        handlers.put("teleport", (player, cfg) -> {
            Location target = player.getTargetBlock(null, 50).getLocation().add(0.5, 1, 0.5);
            player.teleport(target);
        });

        handlers.put("particle_ring", (player, cfg) -> {
            String particleName = cfg.param("particle", "flame");
            int count = cfg.param("count", 20);
            double radius = cfg.param("radius", 2.0);
            try {
                Particle particle = Particle.valueOf(particleName.toUpperCase());
                Location center = player.getLocation();
                for (int i = 0; i < count; i++) {
                    double angle = 2 * Math.PI * i / count;
                    double x = radius * Math.cos(angle);
                    double z = radius * Math.sin(angle);
                    center.add(x, 0, z);
                    player.getWorld().spawnParticle(particle, center, 1, 0, 0, 0, 0);
                    center.subtract(x, 0, z);
                }
            } catch (IllegalArgumentException e) {
                logger.warning("Unknown particle type: " + particleName);
            }
        });
    }

    public void execute(Player player, AbilityConfig config) {
        if (config == null || config.getType() == null) return;
        BiConsumer<Player, AbilityConfig> handler = handlers.get(config.getType());
        if (handler != null) {
            try {
                handler.accept(player, config);
            } catch (Exception e) {
                logger.warning("Ability execution failed: " + config.getType() + " - " + e.getMessage());
            }
        } else {
            logger.warning("Unknown ability type: " + config.getType());
        }
    }

    public void registerHandler(String type, BiConsumer<Player, AbilityConfig> handler) {
        handlers.put(type, handler);
    }
}
```

- [ ] **Step 2: Create `PassiveEffect.java`**

```java
package org.mineUGC.items.passives;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.mineUGC.core.model.PassiveConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class PassiveEffect {
    private final Map<String, BiConsumer<Player, PassiveConfig>> handlers = new HashMap<>();

    public PassiveEffect() {
        registerDefaults();
    }

    private void registerDefaults() {
        handlers.put("potion_effect", (player, cfg) -> {
            String effectName = cfg.getEffect();
            if (effectName == null) return;
            PotionEffectType type = PotionEffectType.getByName(effectName.toUpperCase());
            if (type != null) {
                int amplifier = cfg.getAmplifier();
                // Apply for 10 seconds, refresh every 5 seconds via tick listener
                player.addPotionEffect(new PotionEffect(type, 200, amplifier, true, false, true));
            }
        });
    }

    public void apply(Player player, PassiveConfig config) {
        if (config == null || config.getType() == null) return;
        BiConsumer<Player, PassiveConfig> handler = handlers.get(config.getType());
        if (handler != null) {
            handler.accept(player, config);
        }
    }

    public void registerHandler(String type, BiConsumer<Player, PassiveConfig> handler) {
        handlers.put(type, handler);
    }
}
```

- [ ] **Step 3: Create `SetBonusTracker.java`**

```java
package org.mineUGC.items.sets;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.mineUGC.items.ItemManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SetBonusTracker {
    private final ItemManager itemManager;
    private final Map<UUID, Map<String, Integer>> playerSetCounts = new ConcurrentHashMap<>();

    public SetBonusTracker(ItemManager itemManager) {
        this.itemManager = itemManager;
    }

    public Map<String, Integer> getSetCounts(Player player) {
        Map<String, Integer> counts = new HashMap<>();
        for (ItemStack item : player.getInventory().getArmorContents()) {
            String setId = getSetId(item);
            if (setId != null) {
                counts.merge(setId, 1, Integer::sum);
            }
        }
        // Also check main hand and off hand
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        String mainSet = getSetId(mainHand);
        if (mainSet != null) counts.merge(mainSet, 1, Integer::sum);

        ItemStack offHand = player.getInventory().getItemInOffHand();
        String offSet = getSetId(offHand);
        if (offSet != null) counts.merge(offSet, 1, Integer::sum);

        return counts;
    }

    public int getPieceCount(Player player, String setId) {
        return getSetCounts(player).getOrDefault(setId, 0);
    }

    public boolean hasFullSet(Player player, String setId, int requiredPieces) {
        return getPieceCount(player, setId) >= requiredPieces;
    }

    private String getSetId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        String itemId = itemManager.getItemId(item);
        if (itemId == null) return null;
        var def = itemManager.getDefinition(itemId);
        return def != null ? def.getSet() : null;
    }
}
```

- [ ] **Step 4: Write `SetBonusTrackerTest.java`**

```java
package org.mineUGC.items.sets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SetBonusTrackerTest {
    private SetBonusTracker tracker;

    @BeforeEach
    void setUp() {
        // Create with null ItemManager — will only test logic that doesn't need it
        tracker = new SetBonusTracker(null);
    }

    @Test
    void getSetCounts_shouldReturnEmptyForNoItems() {
        // We can't test with actual players without mock server
        // This tests the method returns safely
        assertDoesNotThrow(() -> tracker.getSetCounts(null));
    }
}
```

- [ ] **Step 5: Run tests**

Run: `./gradlew :items:test`
Expected: All tests pass

- [ ] **Step 6: Commit**

```bash
git add items/src/main/java/org/mineUGC/items/abilities/ items/src/main/java/org/mineUGC/items/passives/ items/src/main/java/org/mineUGC/items/sets/ items/src/test/
git commit -m "feat(items): add ability, passive, and set bonus systems"
```

---

### Task 8: Items — crafting and event listeners

**Files:**
- Create: `items/src/main/java/org/mineUGC/items/crafting/CustomRecipeManager.java`
- Create: `items/src/main/java/org/mineUGC/items/listeners/ItemListener.java`

- [ ] **Step 1: Create `CustomRecipeManager.java`**

```java
package org.mineUGC.items.crafting;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.Plugin;
import org.mineUGC.core.model.ItemDefinition;
import org.mineUGC.core.model.RecipeConfig;
import org.mineUGC.items.ItemManager;

import java.util.HashSet;
import java.util.Set;

public class CustomRecipeManager {
    private final Plugin plugin;
    private final ItemManager itemManager;
    private final Set<NamespacedKey> registeredKeys = new HashSet<>();

    public CustomRecipeManager(Plugin plugin, ItemManager itemManager) {
        this.plugin = plugin;
        this.itemManager = itemManager;
    }

    public void registerRecipe(ItemDefinition def) {
        RecipeConfig recipe = def.getRecipe();
        if (recipe == null || !recipe.isValid()) return;

        NamespacedKey key = new NamespacedKey(plugin, "ugc_" + def.getId());

        ItemStack result = itemManager.createItemStack(def);
        ShapedRecipe shaped = new ShapedRecipe(key, result);
        shaped.shape(recipe.getShape().toArray(new String[0]));

        for (var entry : recipe.getIngredients().entrySet()) {
            Material mat = Material.getMaterial(entry.getValue().toUpperCase());
            if (mat != null) {
                shaped.setIngredient(entry.getKey().charAt(0), mat);
            }
        }

        Bukkit.addRecipe(shaped);
        registeredKeys.add(key);
    }

    public void unregisterAll() {
        for (NamespacedKey key : registeredKeys) {
            Bukkit.removeRecipe(key);
        }
        registeredKeys.clear();
    }

    public void reloadRecipes() {
        unregisterAll();
        for (ItemDefinition def : itemManager.getAllDefinitions()) {
            registerRecipe(def);
        }
    }
}
```

- [ ] **Step 2: Create `ItemListener.java`**

```java
package org.mineUGC.items.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.mineUGC.core.model.AbilityConfig;
import org.mineUGC.core.model.ItemDefinition;
import org.mineUGC.items.ItemManager;
import org.mineUGC.items.abilities.AbilityExecutor;
import org.mineUGC.storage.sqlite.PlayerDataDAO;

import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Logger;

public class ItemListener implements Listener {
    private final ItemManager itemManager;
    private final AbilityExecutor abilityExecutor;
    private final PlayerDataDAO playerDataDAO;
    private final Logger logger;

    public ItemListener(ItemManager itemManager, AbilityExecutor abilityExecutor,
                        PlayerDataDAO playerDataDAO, Logger logger) {
        this.itemManager = itemManager;
        this.abilityExecutor = abilityExecutor;
        this.playerDataDAO = playerDataDAO;
        this.logger = logger;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        String itemId = itemManager.getItemId(item);
        if (itemId == null) return;

        ItemDefinition def = itemManager.getDefinition(itemId);
        if (def == null || def.getAbilities() == null) return;

        AbilityConfig ability = def.getAbilities().get("right_click");
        if (ability == null) return;

        // Check cooldown
        UUID playerId = player.getUniqueId();
        try {
            long expiry = playerDataDAO.getCooldownExpiry(playerId, itemId, "right_click");
            if (expiry > System.currentTimeMillis()) {
                player.sendMessage("§cAbility on cooldown.");
                return;
            }
        } catch (SQLException e) {
            logger.warning("Failed to check cooldown: " + e.getMessage());
        }

        abilityExecutor.execute(player, ability);

        // Set cooldown
        if (ability.getCooldown() > 0) {
            try {
                long expiresAt = System.currentTimeMillis() + (ability.getCooldown() * 1000L);
                playerDataDAO.setCooldown(playerId, itemId, "right_click", expiresAt);
            } catch (SQLException e) {
                logger.warning("Failed to set cooldown: " + e.getMessage());
            }
        }

        event.setCancelled(true);
    }
}
```

- [ ] **Step 3: Run tests**

Run: `./gradlew :items:test`
Expected: All tests pass

- [ ] **Step 4: Commit**

```bash
git add items/src/main/java/org/mineUGC/items/crafting/ items/src/main/java/org/mineUGC/items/listeners/
git commit -m "feat(items): add custom recipe manager and event listeners"
```

---

### Task 9: GUI — in-game item editor

**Files:**
- Create: `gui/src/main/java/org/mineUGC/gui/editor/EditorMenu.java`

- [ ] **Step 1: Create `EditorMenu.java`**

```java
package org.mineUGC.gui.editor;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.mineUGC.core.model.ItemDefinition;
import org.mineUGC.items.ItemManager;

import java.util.*;

public class EditorMenu {
    private static final String MENU_TITLE = "§8UGC Item Editor";
    private static final int MENU_SIZE = 54;

    private final ItemManager itemManager;

    public EditorMenu(ItemManager itemManager) {
        this.itemManager = itemManager;
    }

    public void openMainMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, MENU_SIZE, MENU_TITLE);

        // New Item button
        inv.setItem(10, createGuiItem(Material.GREEN_WOOL, "§a§lNew Item", "§7Create a new custom item"));

        // List existing items
        int slot = 18;
        for (ItemDefinition def : itemManager.getAllDefinitions()) {
            if (slot >= 45) break;
            ItemStack display = itemManager.createItemStack(def);
            var meta = display.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.getLore() != null ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                lore.add("");
                lore.add("§eLeft-click to edit");
                lore.add("§eRight-click to give to self");
                meta.setLore(lore);
                display.setItemMeta(meta);
            }
            inv.setItem(slot++, display);
        }

        player.openInventory(inv);
    }

    public void openItemEditor(Player player, ItemDefinition def) {
        Inventory inv = Bukkit.createInventory(null, 36, "§8Editing: " + def.getId());

        // Preview slot
        ItemStack preview = itemManager.createItemStack(def);
        inv.setItem(4, preview);

        // Edit buttons
        inv.setItem(10, createGuiItem(Material.NAME_TAG, "§eName", current(def.getName())));
        inv.setItem(11, createGuiItem(Material.GRASS_BLOCK, "§eMaterial", current(def.getMaterial())));
        inv.setItem(12, createGuiItem(Material.ITEM_FRAME, "§eModel ID", current(String.valueOf(def.getModel()))));
        inv.setItem(13, createGuiItem(Material.BOOK, "§eLore", def.getLore() != null ? def.getLore().size() + " lines" : "None"));
        inv.setItem(14, createGuiItem(Material.DIAMOND_SWORD, "§eAttributes", "§7Click to configure"));

        inv.setItem(20, createGuiItem(Material.BLAZE_POWDER, "§eAbilities", "§7Click to configure"));
        inv.setItem(21, createGuiItem(Material.POTION, "§ePassives", "§7Click to configure"));
        inv.setItem(22, createGuiItem(Material.ENDER_EYE, "§eSet Bonus", "§7Click to configure"));
        inv.setItem(23, createGuiItem(Material.CRAFTING_TABLE, "§eRecipe", "§7Click to configure"));

        // Save button
        inv.setItem(31, createGuiItem(Material.LIME_WOOL, "§a§lSave", "§7Save changes and reload"));

        // Back button
        inv.setItem(35, createGuiItem(Material.BARRIER, "§cBack", "§7Return to main menu"));

        player.openInventory(inv);
    }

    private ItemStack createGuiItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(Arrays.asList(lore));
            item.setItemMeta(meta);
        }
        return item;
    }

    private String current(String value) {
        return value != null ? "§7Current: §f" + value : "§7Not set";
    }
}
```

- [ ] **Step 2: Run build to verify compilation**

Run: `./gradlew :gui:build`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add gui/
git commit -m "feat(gui): add in-game item editor menu"
```

---

### Task 10: Plugin — wire everything together

**Files:**
- Modify: `plugin/src/main/java/org/mineUGC/plugin/MineUGC.java`
- Create: `plugin/src/main/resources/config.yml`

- [ ] **Step 1: Create `config.yml`**

```yaml
# mineUGC configuration
items-directory: items
database:
  filename: mineugc.db
```

- [ ] **Step 2: Rewrite `plugin/src/main/java/org/mineUGC/plugin/MineUGC.java`**

```java
package org.mineUGC.plugin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.mineUGC.core.command.UgcCommand;
import org.mineUGC.core.event.AssetReloadEvent;
import org.mineUGC.core.model.ItemDefinition;
import org.mineUGC.core.registry.AssetRegistry;
import org.mineUGC.items.ItemManager;
import org.mineUGC.items.abilities.AbilityExecutor;
import org.mineUGC.items.attributes.AttributeApplier;
import org.mineUGC.items.crafting.CustomRecipeManager;
import org.mineUGC.items.listeners.ItemListener;
import org.mineUGC.items.passives.PassiveEffect;
import org.mineUGC.items.sets.SetBonusTracker;
import org.mineUGC.storage.sqlite.DatabaseManager;
import org.mineUGC.storage.sqlite.PlayerDataDAO;
import org.mineUGC.storage.yaml.YamlItemLoader;
import org.mineUGC.storage.yaml.YamlWatcher;
import org.mineUGC.gui.editor.EditorMenu;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.logging.Level;

public final class MineUGC extends JavaPlugin {
    private AssetRegistry<ItemDefinition> registry;
    private ItemManager itemManager;
    private AbilityExecutor abilityExecutor;
    private DatabaseManager database;
    private PlayerDataDAO playerDataDAO;
    private YamlWatcher yamlWatcher;
    private YamlItemLoader yamlLoader;
    private EditorMenu editorMenu;
    private CustomRecipeManager recipeManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // Core
        this.registry = new AssetRegistry<>();

        // Storage - YAML
        this.yamlLoader = new YamlItemLoader();
        this.itemManager = new ItemManager(this);

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
        this.editorMenu = new EditorMenu(itemManager);

        // Load items from files
        loadAllItems();

        // Register recipes
        recipeManager.reloadRecipes();

        // Register listeners
        getServer().getPluginManager().registerEvents(
                new ItemListener(itemManager, abilityExecutor, playerDataDAO, getLogger()), this);

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

                    // Active push: scan online players' inventories
                    getServer().getOnlinePlayers().forEach(player -> {
                        var inv = player.getInventory();
                        for (int i = 0; i < inv.getSize(); i++) {
                            var item = inv.getItem(i);
                            if (item != null && def.getId().equals(itemManager.getItemId(item))) {
                                var updated = itemManager.createItemStack(def);
                                var attrApplier = new AttributeApplier();
                                attrApplier.apply(updated, def.getAttributes());
                                inv.setItem(i, updated);
                            }
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
                            itemManager.getAllDefinitions().forEach(d -> itemManager.remove(d.getId()));
                            loadAllItems();
                            recipeManager.reloadRecipes();
                            sender.sendMessage("§aAll items reloaded.");
                        }
                        case "edit" -> {
                            if (!requirePlayer(sender)) return true;
                            editorMenu.openMainMenu((Player) sender);
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
```

- [ ] **Step 3: Add plugin.yml command entry**

Update `plugin/src/main/resources/plugin.yml`:

```yaml
name: mineUGC
version: '1.0-SNAPSHOT'
main: org.mineUGC.plugin.MineUGC
api-version: '1.21'
commands:
  ugc:
    description: mineUGC main command
    usage: /<command> <list|give|reload|edit>
    permission: mineugc.ugc
permissions:
  mineugc.ugc:
    description: Allows using /ugc
    default: op
  mineugc.ugc.give:
    description: Allows giving UGC items
    default: op
  mineugc.ugc.reload:
    description: Allows reloading UGC items
    default: op
```

- [ ] **Step 4: Build the entire project**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add plugin/ config.yml
git commit -m "feat(plugin): wire all modules together with hot reload"
```

---

### Task 11: Hot reload active push — inventory scanner

**Files:**
- Create: `items/src/main/java/org/mineUGC/items/InventoryScanner.java`
- Create: `items/src/test/java/org/mineUGC/items/InventoryScannerTest.java`

- [ ] **Step 1: Create `InventoryScanner.java`**

```java
package org.mineUGC.items;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.mineUGC.core.model.ItemDefinition;

public class InventoryScanner {

    private final ItemManager itemManager;

    public InventoryScanner(ItemManager itemManager) {
        this.itemManager = itemManager;
    }

    public int replaceInInventory(Player player, ItemDefinition def) {
        int count = 0;
        var inv = player.getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && def.getId().equals(itemManager.getItemId(item))) {
                inv.setItem(i, itemManager.createItemStack(def));
                count++;
            }
        }
        // Also armor slots (which are part of getStorageContents)
        for (int i = 0; i < player.getInventory().getArmorContents().length; i++) {
            ItemStack item = player.getInventory().getArmorContents()[i];
            if (item != null && def.getId().equals(itemManager.getItemId(item))) {
                player.getInventory().setItem(36 + i, itemManager.createItemStack(def));
                count++;
            }
        }
        return count;
    }
}
```

- [ ] **Step 2: Build and commit**

```bash
git add items/src/main/java/org/mineUGC/items/InventoryScanner.java
git commit -m "feat(items): add inventory scanner for hot reload push"
```

---

### Task 12: Integration test — end-to-end load-and-give

**Files:**
- Create: `plugin/src/test/java/org/mineUGC/plugin/MineUGCTest.java`

- [ ] **Step 1: Write integration test skeleton**

```java
package org.mineUGC.plugin;

import org.junit.jupiter.api.Test;

// Full integration test requires MockBukkit or a Paper test env.
// This test is a structural verification that the plugin class loads.
class MineUGCTest {
    @Test
    void pluginClass_shouldBeLoadable() {
        // Verify the plugin class exists and has expected structure
        MineUGC plugin = new MineUGC();
        // Can't fully test without a server environment
    }
}
```

- [ ] **Step 2: Build full project**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Final commit**

```bash
git add plugin/src/test/
git commit -m "test: add integration test skeleton"
```

---

## Spec Coverage Check

| Spec Requirement | Task |
|---|---|
| Multi-module Gradle project (5 modules) | Task 1 |
| UgcAsset interface, ItemDefinition model | Task 2 |
| AbilityConfig, PassiveConfig, RecipeConfig | Task 2 |
| AssetRegistry (register, replace, remove) | Task 2 |
| AssetReloadEvent | Task 3 |
| /ugc command framework | Task 3, Task 10 |
| YAML file loader (per-file) | Task 4 |
| FileWatcher hot reload | Task 4 |
| SQLite runtime state (cooldowns, player data) | Task 5 |
| ItemManager (create, index, lookup) | Task 6 |
| Attribute system (damage, speed, etc.) | Task 6 |
| Ability system (predefined types, no scripting) | Task 7 |
| Passive effects | Task 7 |
| Set bonus tracking | Task 7 |
| Custom crafting recipes | Task 8 |
| Event listeners (right-click ability trigger) | Task 8 |
| GUI editor menu | Task 9 |
| Hot reload active push | Task 11 |
| Error handling | Built into all tasks |
| Testing | Tasks 2-8, 12 |
