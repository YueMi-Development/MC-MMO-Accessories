---
name: minecraft-paper-folia-plugin
description: |
  Write Bukkit/Spigot/Paper plugins that also support Folia, the region-based
  Paper fork. Covers the folia-supported manifest flag, Folia detection,
  and the four Folia scheduler types (global, region, async, entity) with
  code examples for each.

  Use when the task involves any of:
    - Adding Folia support to an existing Paper plugin
    - Writing a plugin that must run on both Paper and Folia servers
    - Converting a Bukkit/Spigot plugin to work on Folia
    - Understanding Folia scheduler concepts (global, region, async, entity)
    - Debugging why a plugin does not work on Folia
metadata:
  scope: folia-compatibility
  targets: [folia-supported, GlobalRegionScheduler, RegionScheduler, AsyncScheduler, EntityScheduler, ServerBuildInfo]
---

# Paper + Folia Plugin Support

Folia is a Paper fork maintained by the PaperMC team. It divides the world
into independently-ticked **regions**, enabling better multi-threaded
performance on multi-core servers. Plugins targeting Folia must use the
correct scheduler type for the operation being performed.

The schedulers described here work on **both Paper and Folia** — on Paper,
they are internally shimmed to provide equivalent behavior. Using them
consistently is the key to cross-platform compatibility.

## Use this skill when

- Adding Folia support to an existing Paper plugin
- Writing a plugin that must run on both Paper and Folia
- Converting a Bukkit/Spigot plugin to work on Folia
- Understanding why a plugin fails on Folia ("No thread context" errors,
  entities teleporting unexpectedly, tasks not executing)
- Choosing the right scheduler for a given task

## Do not use this skill when

- Writing a Paper-only plugin with no Folia compatibility needs
- General plugin development unrelated to threading/region concepts
- Server administration or world management (use server admin skill)

---

# Folia Fundamentals

## How Folia Differs from Paper

Paper executes all world logic on a single server thread. Folia splits
the world into **regions** (chunks grouped spatially), each processed by
its own thread. This means:

- A task running on the main thread in Paper may need to run on a
  **specific region thread** in Folia
- Tasks tied to an **entity** must follow that entity across regions
- Using the wrong scheduler on Folia causes "Invalid thread context"
  errors or tasks that silently never execute

## Mark Plugin as Folia-Supported

Add `folia-supported: true` to your plugin descriptor.

**`plugin.yml`** (Bukkit/Spigot/Paper compatible):
```yaml
name: MyPlugin
version: '1.0.0'
main: com.example.myplugin.MainClass
folia-supported: true
```

**`paper-plugin.yml`** (Paper experimental):
```yaml
name: MyPlugin
version: '1.0.0'
main: com.example.myplugin.MainClass
folia-supported: true
```

Without this flag, Folia will **refuse to load** the plugin.

## Detecting Folia at Runtime

Use `ServerBuildInfo` to check which platform is running. This lets you
branch logic when Paper and Folia require different approaches.

```java
private static boolean isFolia() {
    return ServerBuildInfo.buildInfo().isBrandCompatible(
        Key.key("papermc", "folia")
    );
}
```

Common usage:
```java
// Fallback for Paper async tasks
if (isFolia()) {
    server.getGlobalRegionScheduler().execute(plugin, () -> {
        // Run on Folia global region
    });
} else {
    server.getScheduler().runTaskAsynchronously(plugin, () -> {
        // Run async on Paper
    });
}
```

---

# Scheduler Types

Folia exposes four scheduler types. Each targets a different execution
context. Using the correct one is mandatory on Folia and idiomatic on Paper.

| Scheduler              | Use when task...                            | Folia execution context |
|------------------------|---------------------------------------------|------------------------|
| `GlobalRegionScheduler`| ...does not belong to any region            | Global region thread   |
| `RegionScheduler`      | ...targets a specific location/block        | Region owning the block|
| `AsyncScheduler`       | ...is fully independent of the server tick  | Dedicated async thread |
| `EntityScheduler`      | ...is tied to an entity that may move       | Follows the entity      |

---

## GlobalRegionScheduler

Use for tasks that are **not tied to any specific location or entity** —
global state updates, server-wide timers, cross-world operations.

```java
GlobalRegionScheduler globalScheduler = server.getGlobalRegionScheduler();

// Schedule a repeating task
globalScheduler.runTask(plugin, scheduledTask -> {
    // Runs on the global region (Folia) or main thread (Paper)
}, taskBuilder -> taskBuilder
    .delay(20L)        // 1 second delay (20 ticks)
    .period(20L)       // repeat every 1 second
    .handler(task -> {
        // Task logic
    })
);

// Execute once
globalScheduler.execute(plugin, () -> {
    Bukkit.broadcast(Component.text("Server-wide message"));
});
```

**Do** use this for: broadcast messages, global config reloads,
server statistics collection, cross-world teleportation.

**Do not** use this for: block changes at a specific location
(use `RegionScheduler`), entity manipulations (use `EntityScheduler`).

---

## RegionScheduler

Use for tasks that operate on **blocks at a specific location**. The
scheduler routes the task to the region owning that chunk.

```java
RegionScheduler regionScheduler = server.getRegionScheduler();

// Schedule at a location
Location<World> locationToChange = player.getLocation();

regionScheduler.execute(plugin, locationToChange, () -> {
    locationToChange.getBlock().setType(Material.BEEHIVE);
});
```

The `location` parameter is captured at call time. If the block at that
location changes chunk ownership between scheduling and execution (e.g.
an entity moves it), Folia handles the routing automatically.

```java
// Delayed block change
regionScheduler.runTask(plugin, scheduledTask -> {
    // Runs in the region that owns this block at execution time
}, taskBuilder -> taskBuilder
    .delay(20L * 5)  // 5 second delay
    .handler(task -> {
        targetBlock.setType(Material.DIAMOND_BLOCK);
    })
).compose()
  .thenAcceptAsync(task -> {}, server.getAsyncScheduler());
```

**Do** use this for: block placement/removal, chunk loading, container
interactions at a known location.

**Do not** use this for: entity-related operations (entities can cross
region boundaries — use `EntityScheduler` instead).

---

## AsyncScheduler

Use for tasks that should run **completely independently** of the server
tick. No world access, no entity access, no chunk access.

```java
AsyncScheduler asyncScheduler = server.getAsyncScheduler();

// One-shot async task
asyncScheduler.runDelayed(plugin, scheduledTask -> {
    // Runs on a dedicated async thread
    // No world state access allowed
    performExternalHttpRequest();
}, taskBuilder -> taskBuilder
    .delay(20L * 10)  // 10 second delay
    .handler(task -> {
        // Clean up temp data, sync to external service, etc.
    })
);

// Repeating async task
asyncScheduler.runAtFixedRate(plugin, scheduledTask -> {
    // Runs every N ticks on an async thread
    saveDataToDatabase();
}, taskBuilder -> taskBuilder
    .initialDelay(20L)       // first run after 1 second
    .period(20L * 60)         // then every 60 seconds
    .handler(task -> {
        // Periodic sync or cleanup
    })
);
```

**Do** use this for: HTTP requests, database operations, file I/O,
computation not involving world state.

**Do not** use this for: anything that reads or writes world data,
entity state, or chunk contents. Attempting to do so on Folia throws
an `IllegalStateException`.

---

## EntityScheduler

Use for tasks that must **follow an entity** as it moves across region
boundaries. The entity's scheduler ensures the task always executes on
the correct region thread, even if the entity teleports mid-flight.

```java
EntityScheduler entityScheduler = entity.getScheduler();

// Execute when the entity is next safe to interact with
entityScheduler.execute(plugin, () -> {
    // Runs on the region thread that currently owns the entity
    entity.remove();

    // Or schedule a delayed task that follows the entity
}, TaskableInput.of(
    () -> entity.getLocation(),  // safety check location
    0L                           // timeout ticks (0 = no timeout)
));

// Schedule a delayed task tied to an entity
entityScheduler.run(plugin, scheduledTask -> {
    // This runs on the region that owns the entity at execution time
    // If the entity moved between scheduling and execution, Folia
    // re-routes to the new region automatically
    entity.getWorld().strikeLightningEffect(entity.getLocation());
}, TaskableInput.of(
    () -> entity.getLocation(),  // cancellation check: if entity is far
    20L * 10                      // timeout: cancel after 10 seconds
));
```

The `TaskableInput` provides:
- A **location supplier** for cancellation/safety checks
- A **timeout** in ticks (after which the task is cancelled if not run)

```java
// Full example: buff effect that tracks entity movement
entityScheduler.run(plugin, scheduledTask -> {
    if (scheduledTask.isCancelled()) return;

    // Apply poison effect
    entity.getWorld().spawnParticle(
        Particle.SOUL, entity.getLocation().add(0, 1, 0),
        10, 0.3, 0.5, 0.3, 0.02
    );
}, TaskableInput.of(
    () -> entity.getLocation(),
    20L * 30  // Cancel if entity not reached in 30 seconds
));
```

**Do** use this for: entity-targeted effects, entity-following tasks,
entity removal, entity inventory manipulation.

**Do not** use this for: block operations (use `RegionScheduler`),
global state (use `GlobalRegionScheduler`), or async I/O
(use `AsyncScheduler`).

---

# Common Patterns

## Converting Bukkit.runTask() / runTaskTimer()

| Bukkit (Paper)         | Folia-compatible equivalent                         |
|------------------------|-----------------------------------------------------|
| `Bukkit.getScheduler().runTask(plugin, run)` | `server.getGlobalRegionScheduler().execute(plugin, run)` |
| `Bukkit.getScheduler().runTaskLater(plugin, run, delay)` | `server.getGlobalRegionScheduler().run(plugin, ...)` with `.delay()` |
| `Bukkit.getScheduler().runTaskTimer(plugin, run, delay, period)` | `server.getGlobalRegionScheduler().run(plugin, ...)` with `.delay().period()` |
| `Bukkit.getScheduler().runTaskAsynchronously(plugin, run)` | `server.getAsyncScheduler().run(plugin, ...)` |
| `entity.spigot().scheduleOnRegion(entity, run)` | `entity.getScheduler().execute(plugin, run)` |

## Plugin Enable / Disable Lifecycle

```java
public class MyPlugin extends JavaPlugin {

    private final Set<BukkitTask> activeTasks = new HashSet<>();

    @Override
    public void onEnable() {
        // Global repeating task
        BukkitTask task1 = getServer().getGlobalRegionScheduler()
            .runTask(this, scheduledTask -> {
                // Periodic world save
            }, builder -> builder
                .delay(20L * 60)
                .period(20L * 60 * 5)
            );
        activeTasks.add(task1);

        // Async startup
        getServer().getAsyncScheduler().runDelayed(this, scheduledTask -> {
            initializeData();
        }, builder -> builder.delay(1));
    }

    @Override
    public void onDisable() {
        // Cancel all scheduled tasks
        for (BukkitTask task : activeTasks) {
            task.cancel();
        }
        activeTasks.clear();
    }
}
```

## Checking if Code Runs on the Correct Thread

```java
// In a region scheduler context, verify the chunk is loaded
regionScheduler.execute(plugin, location, () -> {
    Chunk chunk = location.getChunk();
    if (!chunk.isLoaded()) {
        chunk.load();
    }
    // Now safe to modify block
    location.getBlock().setType(Material.STONE);
});

// In an entity scheduler context, the entity is guaranteed to be valid
entityScheduler.execute(plugin, () -> {
    // Entity is guaranteed to exist and be on the correct region
    entity.sendMessage(Component.text("Hello!"));
});
```

---

# Troubleshooting

## "Invalid thread context" error

**Cause**: Task was scheduled on the wrong scheduler type for the
operation being performed.

**Fix**: See the scheduler table above. Block ops → `RegionScheduler`;
entity ops → `EntityScheduler`; global ops → `GlobalRegionScheduler`.

## Task executes but has no effect

**Cause**: Task may be running in the wrong region. On Folia, a block
operation submitted to the global scheduler may run on a region that does
not own the target block.

**Fix**: Always pass the location to `RegionScheduler.execute()`.
Never use the global scheduler for region-specific operations.

## Entity teleports during task execution

**Cause**: Entity-following tasks must use `EntityScheduler`, not
`RegionScheduler`. A task scheduled on a region will execute on that
region even if the entity moved.

**Fix**: Use `entity.getScheduler().run(...)` so Folia re-routes the
task to wherever the entity currently is.

## "Plugin not marked as folia-supported" warning

**Cause**: `folia-supported: true` is missing from `plugin.yml` or
`paper-plugin.yml`.

**Fix**: Add the field to the plugin descriptor. Without it, Folia
refuses to load the plugin.

## Async task throwing IllegalStateException on Folia

**Cause**: `AsyncScheduler` tasks must not access world, chunk, or entity
state. Any such access throws immediately on Folia.

**Fix**: Move world-dependent code to `GlobalRegionScheduler` or
`RegionScheduler`. Keep `AsyncScheduler` for pure I/O and computation.

---

# Key Takeaways

## Manifest
✅ Add `folia-supported: true` to `plugin.yml` or `paper-plugin.yml`
✅ Without this flag, Folia refuses to load the plugin

## Detection
✅ Use `ServerBuildInfo.buildInfo().isBrandCompatible(Key.key("papermc", "folia"))` to detect Folia
✅ Branch logic only when Paper and Folia truly require different code paths

## Schedulers
✅ `GlobalRegionScheduler` — global tasks, no location/entity dependency
✅ `RegionScheduler` — block operations at a specific location (pass location as param)
✅ `AsyncScheduler` — fully async: HTTP, database, I/O only, no world access
✅ `EntityScheduler` — tasks that follow an entity across region boundaries

❌ Don't use `Bukkit.getScheduler().runTask()` on Folia — it may not execute in the correct thread context
❌ Don't use `RegionScheduler` for entity operations — entities cross region boundaries
❌ Don't access world/chunk/entity state from `AsyncScheduler` — it runs on a dedicated async thread
❌ Don't assume a block's region is static — always pass the location to `RegionScheduler.execute()`

## Migration from Bukkit/Spigot
✅ Replace `runTask()` / `runTaskLater()` / `runTaskTimer()` with the appropriate Folia scheduler
✅ Replace entity spigot scheduler calls with `entity.getScheduler()`
✅ Test on Folia (or a Folia test environment) before declaring Folia support

---

# Reference

- [PaperMC Folia Support Docs](https://docs.papermc.io/paper/dev/folia-support/)
- [PaperMC Plugin API](https://docs.papermc.io/paper/dev/api/)
- [Folia Project](https://github.com/PaperMC/Folia)
