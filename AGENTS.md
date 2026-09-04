# MC-MMO-Accessories

Accessory/equipment system for PaperMC servers. Players equip accessories in specialized slots validated against MmoItems types.

## Stack
- PaperMC 1.21+, Java 21, Gradle (Kotlin DSL)
- Soft-depends: YueMiLibs, MmoItems
- Uses MC-Config-Libs for migrations

## Default Slots
| Slot | Max | Required Type |
|------|-----|---------------|
| BELT | 1 | BELT |
| GLOVES | 2 | GLOVES |
| RING | 2 | RING |
| NECKLACE | 1 | NECKLACE |

## API Access
```java
AccessoriesApi api = AccessoriesApiProvider.getApi();
api.equipAccessory(player, AccessorySlot.BELT, 0, item);
api.getEquipped(player, AccessorySlot.RING, 0);
api.canEquipToSlot(item, AccessorySlot.RING);
```

## Config (config.yml)
```yaml
config-version: 2
slot-types:
  BELT:
    display-name: "Belt"
    max-slots: 1
    required-type: "BELT"
  # ... more slots
```

## Storage
Player PDC: `accessories:accessories` as pipe-delimited string

## Conventions
- Packages: `org.yuemi.mmoaccessories.{api,plugin}`
- Config migrations in `plugin.config.migration`
