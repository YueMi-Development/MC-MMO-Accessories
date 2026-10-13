# MC-MMO-Accessories

Accessory/equipment system for PaperMC servers. Players equip accessories in specialized slots. Items are validated against MmoItems types via per-slot `accepted-types` lists (any-of matching). Pluggable storage (PDC default), config v5, drag-and-drop GUI built on YueMiLibs.

## Stack

- PaperMC 1.21+, Java 21, Gradle (Kotlin DSL)
- Soft-depends: YueMiLibs, MmoItems, MmoMechanics
- Uses MC-Config-Libs for migrations

## Default Slots

| Slot | Max | Required Type | Accepted Types |
|------|-----|---------------|----------------|
| BELT | 1 | BELT | BELT, WAIST |
| GLOVES | 2 | GLOVES | GLOVES, HAND |
| RING | 2 | RING | RING |
| NECKLACE | 1 | NECKLACE | NECKLACE |

A slot accepts an item if any of the item's MmoItems types is in its `accepted-types` list.

## API Access
```java
AccessoriesApi api = AccessoriesApiProvider.getApi();
api.equipAccessory(player, AccessorySlot.BELT, 0, item);
Optional<ItemStack> ring = api.getEquipped(player, AccessorySlot.RING, 0);
boolean ok = api.canEquipToSlot(item, AccessorySlot.RING);
api.openGui(player);
```

## Config (config.yml) — v5

```yaml
config-version: 5

storage:
  type: pdc                       # pdc | sqlite | mysql | mongodb
  sqlite:
    file: "accessories.db"
  mysql:
    host: "localhost"
    port: 3306
    database: "minecraft"
    username: "root"
    password: ""
    pool-size: 10
  mongodb:
    uri: "mongodb://localhost:27017"
    database: "minecraft"
    collection: "accessories"

slots:
  BELT:
    display-name: "Belt"
    max-slots: 1
    accepted-types:               # any-of matching
      - "BELT"
      - "WAIST"
    gui:
      slots: [20]
      icon: "minecraft:light_gray_stained_glass_pane"
      label-name: "<gold><bold>Belt Slot</bold></gold>"
      label-lore:
        - "<gray>Type: <green>BELT</green></gray>"
```

## Storage

Pluggable via `AccessoryStorage` interface. Backends in `plugin/storage/`:

| Backend | Class | Notes |
|---------|-------|-------|
| pdc (default) | `PdcStorage` | Player `PersistentDataContainer` |
| sqlite | `SqliteStorage` | Local file DB |
| mysql | `MySqlStorage` | Connection pool, host/port/creds in config |
| mongodb | `MongoStorage` | URI + database + collection |

Add new backends by implementing `AccessoryStorage` and registering with `StorageProvider` (compileOnly dependency is enough — not added to plugin jar unless used).

## Migrations

`v1 → v2 → v3 → v4 → v5` — steps live in `plugin/config/migration/`. v5 introduces the storage block.

## Commands

| Subcommand | Permission | Description |
|------------|-----------|-------------|
| `/accessories open` | `accessories.command.open` | Open own GUI |
| `/accessories reload` | `accessories.command.reload` | Reload config |
| `/accessories list <player>` | `accessories.command.list` | List target's accessories |
| `/accessories view <player>` | `accessories.command.view` | View target's GUI (player sender) |
| `/accessories wipe <player>` | `accessories.command.wipe` | Wipe target's accessories |

GUI-bound commands require a player sender; pure admin commands (`list`, `reload`, `wipe`) also run from console. Subclasses of `BaseSubCommand` per command.

## GUI

- Built on YueMiLibs `GuiApi`
- Configurable layout per slot: slot positions, empty-placeholder icon, label-name, label-lore (MiniMessage)
- Drag item from inventory into slot → equipped; drag from slot into inventory → unequipped
- Auto-refresh of slot icons + labels after every equip/unequip action

## Conventions

- Packages: `org.yuemi.mmoaccessories.{api, plugin}`
- API interfaces in `core-api/src/main/java/org/yuemi/mmoaccessories/api/`
- Implementation in `core-plugin/src/main/java/org/yuemi/mmoaccessories/plugin/`
- Config migrations in `plugin.config.migration`
- Storage backends in `plugin.storage`
- Soft-depends declared in `plugin.yml` so the plugin still loads when any are missing
