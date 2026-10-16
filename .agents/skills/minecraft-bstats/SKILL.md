---
name: minecraft-bstats
description: |
  bStats metrics integration for Minecraft and Hytale plugins — covers
  plugin setup via the Metrics class, custom chart types (pie, line, bar,
  drilldown), the REST API for embedding metrics in sites or tooling, and
  troubleshooting data-not-appearing issues.

  Use when the task involves any of:
    - Adding bStats metrics to a Bukkit/Spigot/Paper or Hytale plugin
    - Implementing custom charts (SimplePie, AdvancedPie, DrilldownPie,
      SingleLineChart, MultiLineChart, SimpleBarChart, AdvancedBarChart)
    - Querying plugin metrics via the bStats REST API
    - Debugging why metrics data is not showing up on the dashboard
---

# bStats Metrics Integration

bStats collects plugin metrics across all major Minecraft and Hytale server
platforms. It tracks server counts, version adoption, and custom metrics. Free
to use, privacy-respecting, and integrates in minutes.

## Use this skill when

- Adding bStats metrics to a new or existing plugin
- Implementing any custom chart (pie, line, bar, drilldown)
- Embedding bStats data in a website or tooling via the REST API
- Troubleshooting why metric data is not appearing on the dashboard
- Understanding the data visibility timeline

## Do not use this skill when

- The task is unrelated to server metrics or plugin telemetry
- You need help with general plugin development (use plugin manifest skill)
- You need runtime event handling or command logic (separate from metrics)

---

# Getting Started

## Add your plugin

The [integration wizard](https://bstats.org) guides you through:
1. Registering your plugin on bStats
2. Integrating the `Metrics` class into your code
3. Creating charts on the website and assigning chart IDs

## Basic integration

```java
// Using Maven or Gradle (preferred)
<dependency>
    <groupId>org.bstats</groupId>
    <artifactId>bstats-bukkit</artifactId>
    <version>3.0.2</version>
</dependency>

// In your plugin's onEnable():
Metrics metrics = new Metrics(this, PLUGIN_ID);
```

For copy-paste class integration, download the `Metrics` class directly from
the bStats website and include it in your project.

---

# Custom Charts

Adding a custom chart requires two steps:
1. **Provide the data** in your plugin code
2. **Register the chart** on the website under your plugin page → Edit → Add chart

## Chart Types Overview

| Chart Type       | Use Case                                      | Data Shape                  |
|------------------|-----------------------------------------------|-----------------------------|
| SimplePie        | Single categorical value per server           | `String`                    |
| AdvancedPie      | Multiple category/value pairs per server      | `Map<String, Integer>`      |
| DrilldownPie     | Hierarchical categories with drill-down       | `Map<String, Map<String, Integer>>` |
| SingleLineChart  | One value trend over time                     | `Integer`                   |
| MultiLineChart   | Multiple value trends compared side-by-side   | `Map<String, Integer>`      |
| SimpleBarChart   | Single value per category (ranking)           | `Map<String, Integer>`      |
| AdvancedBarChart | Multiple bars per category                    | `Map<String, int[]>`        |

---

## SimplePie

Categorical data with one value per server (e.g., language choice).

```java
metrics.addCustomChart(new SimplePie("used_language", () -> {
    return config.getString("language", "English");
}));
```

---

## AdvancedPie

Multiple category/value pairs per server (e.g., player gamemode distribution).

```java
metrics.addCustomChart(new Metrics.AdvancedPie("player_gamemodes", () -> {
    Map<String, Integer> gamemodeMap = new HashMap<>();

    for (Player player : Bukkit.getOnlinePlayers()) {
        String gamemode = player.getGameMode().name();
        gamemodeMap.put(gamemode, gamemodeMap.getOrDefault(gamemode, 0) + 1);
    }

    return gamemodeMap;
}));
```

---

## DrilldownPie

Hierarchical categories — click slices to drill into subcategories.

```java
metrics.addCustomChart(new DrilldownPie("java_version", () -> {
    Map<String, Map<String, Integer>> data = new HashMap<>();

    String javaVersion = System.getProperty("java.version");
    Map<String, Integer> subCategory = new HashMap<>();
    subCategory.put(javaVersion, 1);

    if (javaVersion.startsWith("1.7")) {
        data.put("Java 7", subCategory);
    } else if (javaVersion.startsWith("1.8")) {
        data.put("Java 8", subCategory);
    } else if (javaVersion.startsWith("1.9")) {
        data.put("Java 9", subCategory);
    } else {
        data.put("Other", subCategory);
    }

    return data;
}));
```

---

## SingleLineChart

Track a single value over time (players, servers, any metric).

```java
metrics.addCustomChart(new SingleLineChart("players", () -> {
    return Bukkit.getOnlinePlayers().size();
}));
```

---

## MultiLineChart

Compare multiple metrics side-by-side on one chart.

```java
metrics.addCustomChart(new MultiLineChart("players_and_servers", () -> {
    Map<String, Integer> valueMap = new HashMap<>();
    valueMap.put("servers", 1);
    valueMap.put("players", Bukkit.getOnlinePlayers().size());
    return valueMap;
}));
```

> Note: Multi-line charts are still in development.

---

## SimpleBarChart

Ranking or option adoption — one value per category.

```java
metrics.addCustomChart(new SimpleBarChart("features", () -> {
    Map<String, Integer> map = new HashMap<>();
    map.put("Feature A", 1);
    return map;
}));
```

---

## AdvancedBarChart

Multiple bars per category.

```java
metrics.addCustomChart(new AdvancedBarChart("features", () -> {
    Map<String, int[]> map = new HashMap<>();
    map.put("Feature A", new int[]{0, 1});
    map.put("Feature B", new int[]{1, 0});
    return map;
}));
```

---

# REST API

All endpoints are read-only, return JSON, and require no authentication.
Base URL: `https://bstats.org`

---

## Endpoints

### List all plugins

```
GET /api/v1/plugins
```

Returns all plugins registered on bStats.

```json
[
  {
    "id": 1337,
    "name": "ExamplePlugin",
    "owner": { "id": 42, "name": "ExampleUser" },
    "software": { "id": 1, "name": "Bukkit / Spigot", "url": "bukkit" },
    "isGlobal": false
  }
]
```

---

### Plugin details

```
GET /api/v1/plugins/{pluginId}
```

Returns metadata plus chart definitions for a specific plugin.

```json
{
  "id": 1337,
  "name": "ExamplePlugin",
  "owner": { "id": 42, "name": "ExampleUser" },
  "charts": {
    "players": {
      "uid": 1234,
      "type": "single_linechart",
      "position": 0,
      "title": "Players",
      "isDefault": true,
      "data": {
        "lineName": "Players",
        "filter": { "enabled": false, "maxValue": null, "minValue": null }
      }
    }
  }
}
```

---

### Charts for a plugin

```
GET /api/v1/plugins/{pluginId}/charts
```

Returns the chart registry (chart ID → metadata map) for a plugin.

```json
{
  "players": {
    "uid": 1234,
    "type": "single_linechart",
    "position": 0,
    "title": "Players",
    "isDefault": true,
    "data": { "lineName": "Players", "filter": { "enabled": false, "maxValue": null, "minValue": null } }
  }
}
```

---

### Chart metadata

```
GET /api/v1/plugins/{pluginId}/charts/{chartId}
```

Returns metadata for a single chart.

```json
{
  "uid": 1234,
  "type": "single_linechart",
  "position": 0,
  "title": "Players",
  "isDefault": true,
  "data": {
    "lineName": "Players",
    "filter": { "enabled": false, "maxValue": null, "minValue": null }
  }
}
```

---

### Chart data

```
GET /api/v1/plugins/{pluginId}/charts/{chartId}/data
```

Returns raw chart data. For line charts, each entry is `[timestamp, value]`.

```json
[
  [1479799800000, 122],
  [1479801600000, 121],
  [1479803400000, 124],
  [1479805200000, 134]
]
```

For line charts only, append `?maxElements={amount}` to limit the number of
returned entries.

---

### Supported software

```
GET /api/v1/software
```

Returns all supported server platforms.

```json
[
  {
    "id": 1,
    "name": "Bukkit / Spigot",
    "url": "bukkit",
    "globalPlugin": { "id": 1, "name": "_bukkit_" }
  }
]
```

---

# Troubleshooting

## How long until data appears?

| Stage              | Duration                          |
|--------------------|-----------------------------------|
| Initial delay      | 3–6 minutes after server startup  |
| Publication cycle  | Data published at hh:00 and hh:30 |
| Max visibility lag | Up to 30 minutes after sending    |
| **Total**          | **3–36 minutes** after startup    |

After the server starts, the first data is sent after a random 3–6 minute
delay. The website publishes updates every 30 minutes, so it may take up to
30 additional minutes before your first data point appears on the dashboard.

---

## Data not showing up checklist

If you've waited longer than 36 minutes and still see no data:

1. **Verify the plugin loaded correctly** — check server startup logs for bStats connection messages
2. **Confirm the Metrics class initialized** — ensure `new Metrics(this, pluginId)` was called in `onEnable()` and did not throw
3. **Check network access** — the server must be able to reach `https://bstats.org`
4. **Validate the plugin ID** — ensure the ID matches what bStats assigned to your plugin on the website
5. **Register charts on the website** — custom charts must be created on the bStats plugin page before data is accepted
6. **Check for firewall/proxy interference** — some hosts block outbound connections to port 443
7. **Review server logs** — look for errors or connection failures related to bStats

---

## Still having issues?

Reach out on [Discord](https://discord.gg/bstats) or [open an issue on GitHub](https://github.com/bstats-org/bstats-bukkit/issues).

---

# Key Takeaways

## Getting Started
✅ Use the [integration wizard](https://bstats.org) to register your plugin and get an ID
✅ Initialize `Metrics` in `onEnable()` with your plugin ID
✅ Create each custom chart on the website after adding it in code

❌ Don't skip chart registration on the website — data will be rejected
❌ Don't assume data appears immediately — budget 3–36 minutes

## Custom Charts
✅ Choose the right chart type for your data shape
✅ Use SimplePie for single values, AdvancedPie for multi-value distributions
✅ Use DrilldownPie for hierarchical categorical data
✅ Use SingleLineChart for trends, MultiLineChart for comparisons
✅ Use SimpleBarChart for rankings, AdvancedBarChart for grouped comparisons

❌ Don't use SimplePie when you need multiple values per server
❌ Don't forget to register charts on the website with matching IDs

## REST API
✅ All endpoints are GET-only and require no authentication
✅ Append `?maxElements={amount}` to line chart data to limit entries
✅ Store plugin IDs and chart UIDs — they are stable identifiers

❌ Don't assume real-time data — the API reflects the last publication cycle
❌ Don't poll aggressively — respect the 30-minute publication window

## Troubleshooting
✅ Remember the 3–6 minute initial random delay + 30 minute publication cycle
✅ Check server logs first for connection errors or initialization failures
✅ Confirm network access to `bstats.org` from the server environment

❌ Don't panic if data doesn't appear within 5 minutes — this is expected
❌ Don't skip the website chart registration step for custom charts
