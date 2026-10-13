# MC-MMO-Accessories

A slot-based accessory system for PaperMC servers. Players equip items in dedicated GUI slots (BELT, GLOVES, RING, NECKLACE, plus custom slots) and trigger MMO Mechanics skills on equip, unequip, attack, and right-click.

<a href="https://modrinth.com/plugin/4OB0zRTT" target="_blank" rel="noopener noreferrer"><img alt="modrinth" height="56" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/modrinth_vector.svg"></a>

![](https://img.shields.io/bstats/players/34674?color=green&label=Players)
![](https://img.shields.io/bstats/servers/34674?color=green&label=Servers)

---

## Features

### 1. Slot-Based Accessory System
- **Four Built-in Slots**: `BELT`, `GLOVES` (×2), `RING` (×2), `NECKLACE`.
- **Custom Slots**: any string id is accepted; just add a `slots:` entry to `config.yml`.
- **Multi-Type Matching**: each slot has an `accepted-types` list, matched any-of against the item's MMO Items `types` (e.g. a single RING slot accepts any item with the `RING` type tag).
- **Per-Slot Config**: `display-name`, `max-slots`, `accepted-types`, and a `gui:` sub-block for slot positions, placeholder icon, and lore.

### 2. Drag-and-Drop GUI
- **Built on YueMiLibs GuiApi**: a layered Background / Slots / Actions model with per-slot `SlotBehavior` flags.
- **Smooth Flow**: drag from inventory to slot equips; drag from slot to inventory unequips. Auto-refresh on every state change.
- **Background Non-Interactive**: panes use `SlotBehavior.NONE` so they cannot be picked up.

### 3. MMO Mechanics Integration
- **Equip / Unequip Triggers**: every successful equip or unequip fires `MmoMechanicsApi.fireTrigger(uuid, "onEquip"/"onUnequip")`. MMO Mechanics owns the trigger names and runs the bound skills.
- **In-Slot Triggers**: while worn, accessories react to `onAttack` and `onRightClick` through `MmoMechanicsApi.castSkill`.
- **Cross-Plugin Condition**: MMO Mechanics ships the `isequippedasaccessory` condition that reads accessory state via the public `AccessoriesApi` (reflection-based soft-dep).

### 4. Pluggable Storage
- **Default `pdc`** (player `PersistentDataContainer`).
- **Optional Backends**: `sqlite`, `mysql` (HikariCP), `mongodb`.
- **Switchable**: change `storage.type` in `config.yml` and reload. Each backend's driver library is `compileOnly` so the jar stays small.

---

## Commands

| Command | Description | Permission | Sender |
| :--- | :--- | :--- | :--- |
| `/accessories open` | Open your own accessories GUI | `accessories.command.open` | player |
| `/accessories reload` | Reload `config.yml` | `accessories.command.reload` | any |
| `/accessories list <player>` | List target's accessories | `accessories.command.list` | any |
| `/accessories view <player>` | Open another player's accessories GUI | `accessories.command.view` | player |
| `/accessories wipe <player>` | Wipe target's accessories | `accessories.command.wipe` | any |
