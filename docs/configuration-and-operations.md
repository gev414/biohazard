# Configuration and operations

This guide is for maintainers and server operators. It lists the complete
Biohazard configuration surface, explains when values are read, and describes
the persistent files that must be protected during upgrades or recovery.

## 1. Configuration file ownership

Biohazard registers three NeoForge configuration files:

| File | NeoForge type | Effective location | Authority |
|---|---|---|---|
| `biohazard-encounters.toml` | server | a world's `serverconfig` directory | logical server |
| `biohazard-radio-quests.toml` | server | a world's `serverconfig` directory | logical server |
| `biohazard-client.toml` | client | instance `config` directory | each client |

For a local development world, server config is typically under
`run/saves/<world>/serverconfig`. On a dedicated server it is typically under
`<world>/serverconfig`. Client config is per installation, so players may choose
different fog presentation without changing server gameplay.

NeoForge creates files from defaults when missing. Stop the server before
editing world server config unless a supported config reload path is known;
otherwise in-memory values or a later save can surprise the operator.

## 2. Encounter config

File: `biohazard-encounters.toml`

Section: `[encounters]`

| Key | Type/range | Default | Read by | Existing-state effect |
|---|---|---:|---|---|
| `enabled` | boolean | `true` | manager and container event | Immediately stops new encounter scans/spawns and locking when false; does not delete progress or mobs |
| `hauntedChance` | 0.0 to 1.0 | `0.70` | materialization | Newly materialized buildings only |
| `bossChance` | 0.0 to 1.0 | `0.20` | materialization | Newly materialized haunted 1x1-chunk buildings only |
| `largeBuildingBossChance` | 0.0 to 1.0 | `0.50` | materialization | Newly materialized multi-chunk buildings, whether haunted or safe; boss-only safe buildings skip regular mobs |
| `spawnMode` | `INSTANT` or `WAVE` | `INSTANT` | materialization | Newly materialized buildings only; version-1 saved encounters load as `WAVE` |
| `activationRadius` | 0 to 256 blocks | `64.0` | proximity scan | Live for future scans |
| `activationScanIntervalTicks` | 1 to 1,200 | `40` | manager throttle | Live after the current scan delay; 40 ticks is about 2 seconds |
| `minRegularKills` | 0 to 10,000 | `8` | materialization helper | Newly materialized haunted buildings only |
| `maxRegularKills` | 0 to 10,000 | `15` | materialization helper | Newly materialized haunted buildings only |
| `maxActiveRegularMobs` | 1 to 128 | `4` | wave update | Live; prevents replacements above cap but does not remove existing mobs |
| `updateIntervalTicks` | 1 to 72,000 | `200` | encounter progression/retry | Live after next throttle reset; 200 ticks is about 10 seconds |
| `minSpawnDistance` | 0 to 128 | `8.0` | spawn search | Live for future spawn attempts |
| `maxSpawnDistance` | 0 to 128 | `16.0` | spawn search | Live for future spawn attempts |
| `spawnPositionAttempts` | 1 to 128 | `16` | spawn search | Live; more attempts increase success and cost |
| `bossWarningTicks` | 1 to 1,200 | `200` | transition to pending | Applied when warning begins; already-saved `bossReadyGameTime` is unchanged |
| `lockRandomizableContainers` | boolean | `true` | interaction event | Live for future interactions |
| `announceStateChanges` | boolean | `true` | manager | Live for future boss-warning and cleared announcements |
| `regularMobs` | list of entity ID strings | zombie, husk | spawn pool | Resolved live on each regular spawn attempt |
| `excludedBuildings` | list of Lost Cities IDs | empty | manager/lock event | Live; persisted building ID is used when encounter already exists |

### Normalization behavior

The Java helpers make these settings tolerant of reversed order:

- effective minimum kills is the smaller configured kill value;
- effective maximum kills is the larger;
- effective minimum spawn distance is the smaller distance;
- effective maximum spawn distance is the larger.

This prevents a broken range but does not correct the TOML. Keep operator intent
clear by writing them in normal ascending order.

### Regular mob validation

Every `regularMobs` entry must resolve to a registered hostile entity whose
category is `MONSTER`. `biohazard:brute` is explicitly rejected. Invalid IDs are
ignored and logged once per server process. An entirely invalid/empty pool means
regular encounters cannot populate and therefore cannot naturally reach their
kill target.

### Spawn modes and proximity

Each proximity scan resolves only the nearest real Lost Cities building within
`activationRadius` for each player. This avoids activating every building in a
dense 64-block city radius at once. Players selecting the same physical
building are grouped before it is updated.

`INSTANT` creates the selected target population once. Successful mobs are
initialized using normal mob spawn setup, marked as encounter entities, and
made persistent so ordinary despawning cannot invalidate the fixed kill
target. Failed placements are retried during the regular update interval, but
killed or otherwise removed members are never replaced after their successful
initial placement.

`WAVE` preserves the original behavior: the manager maintains up to
`maxActiveRegularMobs` loaded marked mobs and creates replacements until the
kill target is reached. A building snapshots the configured mode when first
materialized, so changing `spawnMode` does not rewrite encounters already in
progress.

### Excluding buildings

Entries are exact full resource locations, for example:

```toml
excludedBuildings = ["lostcities:some_building", "lcmt:building8"]
```

Use the building or multi-building ID returned by Lost Cities, not a part or
palette ID. A multibuilding resolves to its multibuilding type.

## 3. Radio and courier config

File: `biohazard-radio-quests.toml`

Section: `[radioQuests]`, with delays under
`[radioQuests.deliverySeconds]`.

| Key | Type/range | Default | Timing semantics |
|---|---|---:|---|
| `transmitterRange` | 1 to 32 blocks | `6` | Read on each quest action/choice validation |
| `calibrationTicks` | 0 to 72,000 | `1,200` | Applied when a transmitter begins calibration; default is 60 seconds |
| `deliverySeconds.supplies` | 0 to 86,400 | `120` | Applied when reward is claimed |
| `deliverySeconds.ammunition` | 0 to 86,400 | `120` | Applied when reward is claimed |
| `deliverySeconds.medical` | 0 to 86,400 | `180` | Applied when reward is claimed |
| `deliverySeconds.equipment` | 0 to 86,400 | `240` | Applied when reward is claimed |
| `deliverySeconds.firearm` | 0 to 86,400 | `300` | Applied when reward is claimed |

Times use server game time. A stopped server does not advance them. Lag that
slows game ticks also slows wall-clock delivery time.

Changing a delay does not affect deliveries already persisted with `ready_at`.
Changing calibration duration does not affect transmitter block entities that
already persisted `ready_at`. Range is checked live.

Performance note: proximity scanning checks a cube and then filters it to a
sphere-like radius. The maximum range can scan hundreds of thousands of block
positions per accept/turn-in/choice action. It is not called every tick, but a
large value should still be load-tested.

## 4. Client horde-atmosphere config

File: `biohazard-client.toml`

Section: `[hordeAtmosphere]`

| Key | Type/range | Default | Effect |
|---|---|---:|---|
| `enabled` | boolean | `true` | Master client-only fog toggle |
| `fadeDurationTicks` | 0 to 72,000 | `12,000` | Duration before The Hordes' start time over which fog fades in |
| `targetNearPlane` | 0 to 1,024 | `24.0` | Desired full-strength fog start distance |
| `targetFarPlane` | 1 to 4,096 | `96.0` | Desired full-strength fog end distance |

If near is configured beyond far, the renderer uses far for both at most. Fog
planes are only moved closer than their current values, so weather, fluids, and
other fog-producing mods retain denser fog.

This config does not change The Hordes schedule. Server payloads use The Hordes'
`dayLength`, `hordeStartTime`, enabled state, command-only mode, and per-player
horde state.

## 5. Persistent files

Back up the world before changing or recovering these files.

### Building encounter data

Logical name: `biohazard_building_encounters`

Typical file: `<world>/data/biohazard_building_encounters.dat`

Contains selection and progress for all dimensions. Removing it forgets every
building's safe/haunted roll, kill target, phase, and boss UUID. Buildings will
materialize again from current config when occupied or locked-container
interaction occurs. Because selection is deterministic for the same seed/key
and probabilities, unchanged config usually reproduces the same roll; changed
config or algorithm may not.

### Courier data

Logical name: `biohazard_radio_deliveries`

Typical file: `<world>/data/biohazard_radio_deliveries.dat`

Contains every pending/ready shipment and choice for all players. Removing it
permanently deletes uncollected deliveries. Do not use removal as routine
troubleshooting.

### Radio block entity data

`ready_at` lives inside the chunk's block entity NBT, not in a standalone
Biohazard file. Normal chunk backups protect it. Breaking/replacing the block
creates a new calibration deadline.

### Encounter entity markers

`biohazardEncounter` lives inside each spawned mob's entity data. Removing only
the saved encounter repository without also considering already-loaded/saved
marked mobs can leave old mobs whose deaths no longer match a materialized
record until the building is recreated.

### Live FTB quest book

Typical global instance path: `config/ftbquests/quests`.

It is installed from the JAR only when absent/empty. Back it up separately from
the world when it contains server-authored changes. Team/player quest progress
is owned by FTB Quests and follows that mod's own persistence rules.

## 6. Backup and upgrade procedure

Before a Biohazard or required-mod upgrade:

1. Stop the server cleanly.
2. Back up the entire world, especially `data/` and chunk/entity data.
3. Back up `config/ftbquests/quests` and all Biohazard TOML files.
4. Record current Biohazard and dependency versions from the mod list.
5. Keep a copy of the previous working mod JAR set.
6. Upgrade in a cloned/staging instance first.
7. Inspect startup and data-reload logs for missing registry IDs, malformed
   JSON/SNBT, dependency range errors, or skipped saved entries.
8. Run the smoke test in the next section.
9. Only then upgrade the production world.

Do not test world-generation compatibility only in old chunks. Generate fresh
Lost Cities terrain in staging.

## 7. Operational smoke test

After installation or upgrade, verify at least:

1. Dedicated server reaches ready state with the full required mod set.
2. A client joins and no missing-payload/protocol error occurs.
3. Radio Transmitter crafts, places, rotates, calibrates, drops, and opens the
   FTB quest book.
4. Accept fails out of range and succeeds in range.
5. Atomic submission with one missing requirement consumes nothing.
6. A complete contract schedules the expected manifest/category.
7. Restart during a pending delivery preserves it.
8. Full-inventory collection preserves remainders.
9. Choice delivery opens, validates range, and gives only the chosen item.
10. A fresh Lost Cities building resolves, spawns only inside its volume,
    advances on marked deaths, locks expected containers, and clears.
11. A Brute has the correct size, boss bar, rock attack, splash, loot, and death
    completion behavior.
12. Cure/suppressant behavior matches The Hordes and synchronizes visibly.
13. Scheduled horde fog fades on the client and resets after logout/event.
14. Generated Handcrafted storage stocks once while player-placed storage does
    not receive free loot.
15. Patchouli categories/entries and FTB quest strings/icons render correctly.

## 8. Diagnostics and recovery

### Encounter does not start

Check in this order:

- server config `enabled`;
- player is alive, not spectator, and within `activationRadius` of the nearest
  Lost Cities building;
- Lost Cities API initialized without errors;
- building ID is not excluded;
- difficulty is not Peaceful;
- regular mob pool has at least one valid hostile ID;
- spawn distance/attempts permit a collision-free loaded position inside the
  building;
- activation scan interval has elapsed; incomplete instant placement retries
  use the regular update interval.

A safe building is expected behavior for the configured percentage. A persisted
safe selection remains safe even if `hauntedChance` later increases.

### Kill does not advance

Only entities spawned/marked by Biohazard count. Naturally spawned zombies do
not. Check that the entity has its `biohazardEncounter` persistent compound and
that its building key matches the saved encounter. Death events canceled or
replaced by another mod before Biohazard's lowest-priority handler may also
change behavior.

### Boss does not appear

- Confirm target kills were reached and encounter phase is `BOSS_PENDING`.
- Wait the saved warning deadline, not only the newly configured delay.
- Confirm non-Peaceful difficulty and a valid Brute spawn position.
- If phase is already `BOSS_ACTIVE`, absence from loaded queries is treated as
  chunk unload; the manager intentionally will not duplicate the boss. Search
  entity/chunk data before altering saved state.

### Container remains locked

Confirm the encounter actually reached `CLEARED`, not only its regular kill
target. Boss buildings require the marked Brute's death. Non-boss buildings
require all loaded marked regulars to be gone. Also confirm the clicked block
is in the same descriptor as the player.

### Quest defaults did not update

Expected when `config/ftbquests/quests` is non-empty. The installer never merges
or overwrites it. For development, save/export live edits, move the directory
aside only after verifying its path and backup, then start with an empty target.
For production, migrate intentionally through FTB tooling or curated file
changes.

### Quest button says out of range

- Confirm a Radio Transmitter block exists within configured three-dimensional
  range.
- Confirm its chunk is loaded.
- Wait for its persisted calibration deadline.
- Breaking and replacing restarts calibration.
- The quest book can be viewed remotely, but tagged accept/complete buttons are
  server-gated.

### Turn-in consumed nothing

This is normal on any failed atomic validation. Check required non-optional
objectives and every tagged item task. Submission scans main inventory slots
only. The FTB item task should be tagged `biohazard_radio_submit`, use the right
count/matcher, and leave consumption to Biohazard.

### Reward claims but no delivery appears

Check server log for `generated no items`, then verify:

- custom reward has exactly the intended delivery tag;
- a nonblank `biohazard_manifest_` tag exists;
- matching table exists under `data/biohazard/loot_table/quest_delivery`;
- all item IDs/conditions are valid in the full modpack;
- table produces nonempty output in a chest context.

### Delivery never becomes ready

Timers use Overworld game time, not wall clock. Confirm the server is ticking,
the record's `ready_at`, and the category applied when scheduled. A config
change does not rewrite an existing deadline.

### Ready items do not all collect

Normal when inventory has insufficient room. The manager saves exact
remainders. Free slots and use a calibrated transmitter again. A ready choice
must be selected before it becomes collectible.

### Choice screen or selection fails

Check both network directions are registered at protocol version `1`, client
and server run compatible Biohazard builds, the item IDs exist on the client,
and the player remains within a calibrated radio's range when clicking. The
server rejects stale screens, wrong owners, early deliveries, invalid UUIDs,
and out-of-range indices without trusting the client.

### Horde fog is absent

- Client config must be enabled.
- Player must be in the Overworld.
- Render mode must be terrain fog with no fluid fog.
- The Hordes event must be enabled server-side.
- The server's per-player state must report horde day or active event.
- Current day time must fall inside the pre-event fade interval, unless active.
- Another source may already impose closer fog; Biohazard never pushes it
  farther away.

### Handcrafted storage is empty

Only the explicit allowlist is handled. Player-placed blocks are intentionally
excluded. Confirm the block entity implements `Container`, lacks the
player-placed and stocked flags, and that
`biohazard:chests/handcrafted_storage` produces loot for the context.

## 9. Recovery principles

- Work on a backup or cloned world first.
- Prefer correcting config/resources and restoring a clean backup over editing
  compressed NBT by hand.
- Never delete the whole world `data` directory to reset one system.
- If surgical NBT editing is unavoidable, stop the server, preserve the
  original file, document exact edits, and verify checksums/backups.
- Do not treat removing encounter or courier files as harmless cache clearing;
  both contain authoritative player/world progress.
- When a malformed-entry log appears, preserve the file before letting the
  server save again so the skipped source record remains available for forensic
  recovery.
