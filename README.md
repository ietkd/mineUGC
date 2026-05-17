# mineUGC

A Paper 1.21 plugin for defining and managing custom items through YAML configuration with hot-reload support. Features include an item registry system, SQLite-based player data persistence, an in-game GUI editor, custom crafting recipes, and dynamic item abilities/attributes.

## Modules

| Module    | Description |
|-----------|-------------|
| **core**  | Shared API and interfaces |
| **storage** | SQLite player data persistence |
| **items** | Item registry, attributes, abilities, recipes |
| **gui**   | In-game item editor UI |
| **plugin** | Main plugin class wiring all modules |

## Usage

```bash
# Build the plugin
./gradlew build

# Run a Paper server with the plugin
./gradlew run

# Stage, commit, and push all files (excludes .claude, other/, run/)
./gradlew gread -PcommitMsg="your message"
```
