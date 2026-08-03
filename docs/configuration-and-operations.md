# Configuration and operations

This guide is for maintainers and server operators. It lists the complete
Rotwire configuration surface, explains when values are read, and describes
the persistent files that must be protected during upgrades or recovery.

## 1. Configuration file ownership

Rotwire registers eight NeoForge configuration files:

| File | NeoForge type | Effective location | Authority |
|---|---|---|---|
| `rotwire-encounters.toml` | server | a world's `serverconfig` directory | logical server |
| `rotwire-radio-quests.toml` | server | a world's `serverconfig` directory | logical server |
| `rotwire-city-operations.toml` | server | a world's `serverconfig` directory | logical server |
| `rotwire-settlements.toml` | server | a world's `serverconfig` directory | logical server |
| `rotwire-mobs.toml` | server | a world's `serverconfig` directory | logical server |
| `rotwire-survival.toml` | server | a world's `serverconfig` directory | logical server |
| `rotwire-weather.toml` | server | a world's `serverconfig` directory | logical server |
| `rotwire-client.toml` | client | instance `config` directory | each client |

For a local development world, server config is typically under
`run/saves/<world>/serverconfig`. On a dedicated server it is typically under
`<world>/serverconfig`. Client config is per installation, so players may choose
different fog presentation without changing server gameplay.

NeoForge creates files from defaults when missing. Stop the server before
editing world server config unless a supported config reload path is known;
otherwise in-memory values or a later save can surprise the operator.
Changing a Java default does not replace a value already assigned in a
generated TOML. Delete the file to regenerate every key, or edit only the
intended assignments while the game or server is stopped.

## 2. Encounter config

File: `rotwire-encounters.toml`

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
category is `MONSTER`. `rotwire:brute` is explicitly rejected. Invalid IDs are
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

File: `rotwire-radio-quests.toml`

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

## 4. City operations config

File: `rotwire-city-operations.toml`

Section: `[cityOperations]`, with sub-sections `[cityOperations.survey]`,
`[cityOperations.danger]`, and `[cityOperations.streetSpawns]`.

| Key | Type/range | Default | Existing-state effect |
|---|---|---:|---|
| `enabled` | boolean | `true` | Stops new city surveys/progress/scaling and ambient street spawning when false; saved zones remain intact |
| `survey.chunksPerTick` | 1 to 256 | `16` | Live for loaded, incomplete surveys; higher values finish sooner at greater tick cost |
| `survey.maxChunks` | 64 to 262,144 | `16,384` | Live cap for incomplete surveys; capped surveys use a stable fallback sector |
| `survey.diagonalConnectivity` | boolean | `false` | Live for incomplete surveys; determines whether diagonal city chunks join one zone |
| `survey.fallbackSectorSizeChunks` | 8 to 256 | `32` | Used only for surveys that hit the cap; does not rewrite existing zones |
| `danger.influencePerimeterChunks` | 0 to 32 | `5` | Live for future infected danger lookups; 5 chunks is 80 blocks |
| `danger.clearedBuildingsPerLevel` | 1 to 1,000 | `5` | Live progression calculation from the persisted unique-clear set |
| `danger.maxLevel` | 0 to 100 | `12` | Live cap; lowering it does not remove an infected's already remembered higher level |
| `danger.healthPerLevel` | 0.0 to 10.0 | `0.10` | Live for future normal-infected upgrades; `0.10` is +10% base maximum health per level |
| `danger.bruteHealthPerLevel` | 0.0 to 10.0 | `0.10` | Live for future Brute upgrades; separate because Brutes have much higher base health |
| `streetSpawns.enabled` | boolean | `true` | Enables uncommon non-encounter zombies on outdoor Lost Cities streets |
| `streetSpawns.intervalTicks` | 20 to 72,000 | `200` | Live delay between one spawn roll per eligible player |
| `streetSpawns.chance` | 0.0 to 1.0 | `0.15` | Live chance that an interval roll searches for one spawn position |
| `streetSpawns.nighttimeChanceMultiplier` | 1.0 to 16.0 | `3.0` | Live multiplier for the chance at night; effective chance is capped at 1.0 |
| `streetSpawns.nearbyCap` | 0 to 128 | `4` | Live cap on Rotwire street zombies near each player; zero disables spawning |
| `streetSpawns.nearbyCapRadius` | 16 to 256 | `96` | Live horizontal radius used for the nearby cap |
| `streetSpawns.minimumDistance` | 1 to 128 | `28` | Live minimum horizontal distance from every non-spectating player |
| `streetSpawns.maximumDistance` | 1 to 256 | `64` | Live maximum horizontal distance from the anchor player |
| `streetSpawns.positionAttempts` | 1 to 128 | `16` | Live candidate positions tested after a successful roll |

Surveying starts alongside transmitter calibration. A radio does not connect
until both are complete. City state is shared by mapped radios and stored in
`rotwire_city_zones`; deleting config values does not reset that world data.
Read [City operations](city-operations.md) before changing progression values
on an established world. Street zombies are ordinary despawning mobs and do
not add persistent city or encounter records.

## 4.1 Settlement config

File: `rotwire-settlements.toml`

Section: `[settlements]`

| Key | Type/range | Default | Existing-state effect |
|---|---|---:|---|
| `rationsPerSettlerPerDay` | 0 to 1,000 | `1` | Hunger points consumed by each civilian or guard at the next Minecraft-day boundary |
| `stockpileScanIntervalTicks` | 20 to 1,200 | `100` | Live interval for discovering edible items in active, loaded campsite containers |

The Camp Hub Module must be installed by the primary camp owner at the online,
complete primary radio before campsite storage supplies the city. Every edible
item in a chest, barrel, Handcrafted storage block, or other standard inventory
within an active camp zone contributes its hunger value; food is not copied into
a radio inventory. When upkeep removes a whole food item, surplus nutrition is
kept as prepared rations so no hunger value is lost. `rotwire_settlements`
persists the latest observed total, prepared rations, and source count, while
the source containers remain authoritative and work through all relays. With no
settlers, the displayed daily use is zero; the same persisted upkeep rule
becomes active as population is added.

## 5. Survival systems config

File: `rotwire-survival.toml`

Root section: `[survivalSystems]`

| Key | Default | Meaning |
|---|---:|---|
| `enabled` | `true` | Master switch for encumbrance, stealth, attention, sleep survival, and the survival HUD |
| `updateIntervalTicks` | `10` | Inventory/backpack weight recalculation interval |
| `encumbrance.lightMaxWeight` | `16.0` | Highest weight that permits crouched quiet movement |
| `encumbrance.burdenedMaxWeight` | `25.0` | Upper Burdened boundary |
| `encumbrance.heavyMaxWeight` | `40.0` | Upper Heavy boundary |
| `encumbrance.burdenedSpeedPenalty` | `0.10` | Burdened movement reduction |
| `encumbrance.heavySpeedPenalty` | `0.20` | Heavy movement reduction |
| `encumbrance.overloadedSpeedPenalty` | `0.35` | Overloaded movement reduction |
| `stealth.scanIntervalTicks` | `5` | Visual suspicion scan interval |
| `stealth.detectionRange` | `24.0` | Maximum progressive sight range |
| `stealth.closeDetectionRange` | `2.5` | Visible range that detects immediately |
| `stealth.fieldOfViewDegrees` | `140.0` | Infected visual cone |
| `stealth.suspicionPerSecond` | `35.0` | Base suspicion gain before distance scaling |
| `stealth.suspicionDecayPerSecond` | `20.0` | Suspicion decay without favorable sight |
| `stealth.bruteDetectionMultiplier` | `2.5` | Brute suspicion multiplier |
| `stealth.alertMemoryTicks` | `400` | Acquired-target memory; default 20 seconds |
| `stealth.loudActionGraceTicks` | `40` | Delay before crouching can become quiet again |
| `attention.suppressedFireRange` | `12.0` | Suppressed PointBlank shot radius |
| `attention.unsuppressedFireRange` | `96.0` | Unsuppressed PointBlank shot radius |
| `attention.meleeRange` | `16.0` | Melee investigation radius |
| `attention.blockBreakRange` | `20.0` | Non-instant block-break radius |
| `attention.replaceZombieTacticsMarkers` | `true` | Replace automatic markers with Rotwire-approved loud-gun markers |
| `knockback.enabled` | `true` | Enable reduced knockback for zombies and players |
| `knockback.zombieRetention` | `0.15` | Fraction of normal knockback retained by Zombie subclasses |
| `knockback.playerRetention` | `0.30` | Fraction of normal knockback retained by players |
| `sleepSurvival.enabled` | `true` | Enable Restless Sleep and New Dawn |
| `sleepSurvival.effectDurationTicks` | `1000` | Both effects last 50 seconds |
| `sleepSurvival.pulseIntervalTicks` | `200` | Change hunger and thirst every 10 seconds |
| `sleepSurvival.meterPointsPerPulse` | `2` | Change one full HUD icon per pulse |
| `sleepSurvival.campsiteRadius` | `12` | Maximum shelter-center distance to the lit campfire and inventory-capable food blocks; SimplyTents enforces footprint-sized minimums |
| `sleepSurvival.campsiteFoodNutritionThreshold` | `5` | Backpack food must total strictly more than this; the smallest qualifying ration is consumed |

The `[survivalSystems.encumbrance.weights]` section controls per-item category
weights. Stackable categories are `tinyItem = 0.02`, `lightItem = 0.04`,
`defaultItem = 0.06`, `denseItem = 0.08`, `veryDenseItem = 0.12`, and
`blockItem = 0.10`. Equipment fallbacks are `unstackableItem = 0.75`,
`lightEquipment = 1.25`, `armorItem = 2.0`, `firearmItem = 2.5`, and
`heavyEquipment = 4.0`. A backpack adds `backpackBase = 2.0`, plus its
contents and `backpackFluidPerBucket = 1.0`.

Stackable weight is the configured per-item value multiplied by count. There
is no partial-stack minimum and maximum stack size is not part of the formula.
Tags can classify whole item families, while firearm, armor, block, unstackable,
and default fallbacks keep unmapped vanilla and modded items functional.

All state is transient. Changing thresholds or category weights takes effect on
the next recalculation; movement modifiers are replaced rather than stacked.
Disabling the system removes Rotwire's movement modifier and hides its HUD.
The HUD and inventory weight tooltip receive their thresholds and penalties
from the logical server. They show the same active values used for tier
selection and movement modifiers, including TOML overrides; there is no
separate client balance table.

Knockback retention is applied by the logical server after other handlers have
had an opportunity to alter knockback strength. The zombie value applies to
all `Zombie` subclasses, including husks, drowned, zombie villagers, and
Rotwire brutes. Other living entities retain normal knockback. Set a retention
to `1.0` for vanilla strength or `0.0` to remove knockback for that category.

Read [Stealth, attention, and encumbrance](stealth-attention-and-encumbrance.md)
before changing noise radii or ZombieTactics marker ownership.
Read [Sleep survival](sleep-survival.md) for the sleeping-bag trigger, complete
night-awake requirements, and shared meter-pulse behavior.

## 6. Weather forecast and hazard config

File: `rotwire-weather.toml`

Section: `[weather]`, with `[weather.weights]`, `[weather.duration]`,
`[weather.exposure]`, and `[weather.stealth]`.

| Key | Type/range | Default | Effect |
|---|---|---:|---|
| `enabled` | boolean | `true` | Makes Rotwire authoritative for Overworld weather and enables forecasts, stealth modifiers, and exposure |
| `seasonalWeighting` | boolean | `true` | Reads optional Serene Seasons state to adjust selection weights |
| `hazardousStartDay` | integer, 0 to 1,000,000 | `5` | Earliest zero-based day that may select contaminated precipitation |
| `weights.clear` | integer, 0 to 10,000 | `40` | Relative daily clear-weather weight |
| `weights.rain` | integer, 0 to 10,000 | `30` | Relative ordinary-rain weight |
| `weights.storm` | integer, 0 to 10,000 | `15` | Relative ordinary-storm weight |
| `weights.contaminatedRain` | integer, 0 to 10,000 | `10` | Relative contaminated-rain weight |
| `weights.contaminatedStorm` | integer, 0 to 10,000 | `5` | Relative contaminated-storm weight |
| `duration.normalMinimumTicks` | integer, 200 to 23,000 | `6000` | Minimum ordinary precipitation duration |
| `duration.normalMaximumTicks` | integer, 200 to 23,000 | `12000` | Maximum ordinary precipitation duration |
| `duration.hazardousMinimumTicks` | integer, 200 to 23,000 | `4000` | Minimum contaminated precipitation duration |
| `duration.hazardousMaximumTicks` | integer, 200 to 23,000 | `7000` | Maximum contaminated precipitation duration |
| `exposure.rainGraceTicks` | integer, 0 to 1,200 | `80` | Four-second outdoor warning before contaminated-rain damage |
| `exposure.stormGraceTicks` | integer, 0 to 1,200 | `40` | Two-second outdoor warning before contaminated-storm damage |
| `exposure.rainDamageIntervalTicks` | integer, 1 to 1,200 | `100` | Five seconds between rain damage pulses |
| `exposure.stormDamageIntervalTicks` | integer, 1 to 1,200 | `60` | Three seconds between storm damage pulses |
| `exposure.damageAmount` | decimal, 0.0 to 100.0 | `2.0` | Damage points per pulse; two points are one heart |
| `stealth.rainSuspicionMultiplier` | decimal, 0.0 to 1.0 | `0.85` | Visual suspicion gain during rain |
| `stealth.stormSuspicionMultiplier` | decimal, 0.0 to 1.0 | `0.70` | Visual suspicion gain during storms |
| `stealth.rainAttentionMultiplier` | decimal, 0.0 to 1.0 | `0.80` | Loud-action investigation range during rain |
| `stealth.stormAttentionMultiplier` | decimal, 0.0 to 1.0 | `0.60` | Loud-action investigation range during storms |

Durations accept reversed minimum/maximum values and normalize them when a plan
is generated. Weights are relative; setting all five to zero produces clear
days. Existing persisted plans retain the values selected when they were
generated.

The enable switch, exposure values, and stealth multipliers are read live by
their server services. Seasonal weighting, selection weights, start day, and
duration ranges affect only plans generated after the change; they do not
rewrite today/tomorrow entries already stored in `SavedData`.

When Serene Seasons is installed, disable its
`weather_settings.change_weather_frequency` option while Rotwire weather is
enabled. This leaves seasonal presentation intact while preventing two systems
from competing for rain timing. See
[Weather forecast and contaminated precipitation](weather-forecast-and-hazards.md).

Permission-level-2 operators can exercise the complete system with
`/rotwire weather force <condition> [duration]`, inspect it with
`/rotwire weather status`, and return to the persisted schedule with
`/rotwire weather clear`. The force command accepts `clear`, `rain`, `storm`,
`contaminated_rain`, and `contaminated_storm`. It defaults to 12,000 ticks and
accepts Minecraft `t`, `s`, and `d` suffixes.

The vignette's size, opacity, colors, pulse, and fade speed are client source
constants in `WeatherExposureClient`; they are not exposed in
`rotwire-client.toml`. Changing them requires rebuilding and distributing the
client JAR, but does not alter server hazard balance.

## 7. Mob spawning config

File: `rotwire-mobs.toml`

Sections: `[mobSpawning.undergroundRestrictions]` and
`[mobSpawning.wildernessZombies]`

| Key | Type/range | Default | Effect |
|---|---|---:|---|
| `undergroundRestrictions.enabled` | boolean | `true` | Master restriction switch, read live for every eligible spawn-placement check |
| `undergroundRestrictions.restrictSkeletons` | boolean | `true` | Restricts natural skeleton, stray, bogged, and wither-skeleton spawns |
| `undergroundRestrictions.restrictCreepers` | boolean | `true` | Restricts natural creeper spawns |
| `undergroundRestrictions.minimumDepthBelowSeaLevel` | integer, 0 to 384 | `16` | Restricted mobs may spawn naturally only at or below `sea level - depth` |
| `undergroundRestrictions.dimensions` | list of dimension IDs | `minecraft:overworld` | Applies the depth rule only in listed dimensions |
| `wildernessZombies.enabled` | boolean | `true` | Enables scarce surface zombies outside Lost Cities |
| `wildernessZombies.intervalTicks` | 20 to 72,000 | `200` | Delay between one wilderness roll per eligible player |
| `wildernessZombies.chance` | 0.0 to 1.0 | `0.025` | Chance that an interval roll searches for one spawn position |
| `wildernessZombies.nighttimeChanceMultiplier` | 1.0 to 16.0 | `3.0` | Multiplier for the chance at night; effective chance is capped at 1.0 |
| `wildernessZombies.nearbyCap` | 0 to 128 | `2` | Maximum Rotwire wilderness zombies within the cap radius; zero disables spawning |
| `wildernessZombies.nearbyCapRadius` | 16 to 256 | `128` | Horizontal radius used for the wilderness population cap |
| `wildernessZombies.minimumDistance` | 1 to 128 | `32` | Minimum horizontal spawn distance from every non-spectating player |
| `wildernessZombies.maximumDistance` | 1 to 256 | `72` | Maximum horizontal spawn distance from the anchor player |
| `wildernessZombies.positionAttempts` | 1 to 128 | `24` | Candidate outdoor positions tested after a successful roll |
| `wildernessZombies.dimensions` | list of dimension IDs | `minecraft:overworld` | Dimensions where wilderness surface zombies may appear |

At the vanilla Overworld sea level of 63, the default depth produces a maximum
natural-spawn position of Y 47. Y 47 is allowed and Y 48 is rejected. This
absolute vertical rule deliberately does not use sky visibility or a heightmap:
Lost Cities roofs and skyscraper floors therefore cannot make surface-level
interiors count as caves.

Only `MobSpawnType.NATURAL` is rejected. Mob spawners, commands, spawn eggs,
structure or trigger spawns, and Rotwire's scripted `EVENT` encounter mobs
retain their normal behavior at every height. The default dimension list also
leaves Nether wither-skeleton spawning unchanged. Adding
`"minecraft:the_nether"` applies the same sea-level-relative rule there.

All values are read live for future spawn attempts and create no persistent
state. Setting `minimumDepthBelowSeaLevel` to zero still rejects natural
spawns above sea level; disable the feature or an individual mob switch to
restore unrestricted vertical spawning.

Wilderness zombies use Rotwire's bounded ambient spawner rather than replacing
the vanilla zombie spawn predicate. It samples the
`MOTION_BLOCKING_NO_LEAVES` surface heightmap and requires an empty non-fluid
position, solid footing, loaded chunks, world-border containment, and
collision clearance. Forest-floor candidates may sit beneath foliage, but
underground positions are never sampled. It deliberately omits the darkness
check, so the rule can run during every part of the day and night. At night,
the configured multiplier raises the roll chance from `0.025` to `0.075` by
default. Candidates in Lost Cities city chunks are rejected by this tier;
those remain owned by the denser `cityOperations.streetSpawns` settings,
whose default nighttime chance similarly rises from `0.15` to `0.45`.

With both default intervals, the wilderness `0.025` chance is one sixth of the
city-street `0.15` chance. This averages one successful wilderness roll roughly
every six to seven minutes per eligible player before position failures, with
at most two marked wanderers nearby. The mobs use ordinary despawning and do
not count as building encounters or persist indefinitely. These surface rolls
do not alter the vanilla zombie spawn predicate, mob cap, or placement checks;
natural zombies therefore continue spawning underground normally.

The required Hordes configuration controls sunlight ignition independently.
The Biohazard profile uses `hordes-common.toml` with `zombiesBurn = false`, so
daylight surface zombies continue roaming while retaining ordinary fire and
lava vulnerability. Changing that setting to true makes open-sky zombies burn
normally after spawning.

## 8. Client horde-atmosphere config

File: `rotwire-client.toml`

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

## 9. Persistent files

Back up the world before changing or recovering these files.

### Building encounter data

Logical name: `rotwire_building_encounters`

Typical file: `<world>/data/rotwire_building_encounters.dat`

Contains selection and progress for all dimensions. Removing it forgets every
building's safe/haunted roll, kill target, phase, and boss UUID. Buildings will
materialize again from current config when occupied or locked-container
interaction occurs. Because selection is deterministic for the same seed/key
and probabilities, unchanged config usually reproduces the same roll; changed
config or algorithm may not.

### Courier data

Logical name: `rotwire_radio_deliveries`

Typical file: `<world>/data/rotwire_radio_deliveries.dat`

Contains every pending/ready shipment and choice for all players. Removing it
permanently deletes uncollected deliveries. Do not use removal as routine
troubleshooting.

### City operations data

Logical name: `rotwire_city_zones`

Typical file: `<world>/data/rotwire_city_zones.dat`

Contains mapped city footprints or capped fallback sectors, unique cleared
building keys, and active FTB city-operation bindings. Removing it forgets
city progress and can cause existing operations to lose their recorded city
baseline. Back it up with the encounter repository.

### Settlement state

Logical name: `rotwire_settlements`

Typical file: `<world>/data/rotwire_settlements.dat`

Contains one settlement record for every city where a complete mapped camp has
been established: its chosen name, fixed primary hub, relay radio state,
population, rations, upgrades, and future siege schedule. Do not delete it to
move a primary hub; that would erase the city's future settlement progress.

### Weather schedule data

Logical name: `rotwire_weather_schedule`

Typical file: `<world>/data/rotwire_weather_schedule.dat`

Contains deterministic persisted day plans around the rolling today/tomorrow
forecast plus any active operator override and its game-time expiry. Removing
it regenerates plans from the current world seed and configuration and cancels
the override. Do not remove it mid-day unless a forecast change is intended.

### Radio block entity data

`ready_at` lives inside the chunk's block entity NBT, not in a standalone
Rotwire file. Normal chunk backups protect it. Breaking/replacing the block
creates a new calibration deadline.

### Encounter entity markers

`rotwireEncounter` lives inside each spawned mob's entity data. Removing only
the saved encounter repository without also considering already-loaded/saved
marked mobs can leave old mobs whose deaths no longer match a materialized
record until the building is recreated.

### Live FTB quest book

Typical global instance path: `config/ftbquests/quests`.

It is installed from the JAR only when absent/empty. Back it up separately from
the world when it contains server-authored changes. Team/player quest progress
is owned by FTB Quests and follows that mod's own persistence rules.

## 10. Backup and upgrade procedure

Before a Rotwire or required-mod upgrade:

1. Stop the server cleanly.
2. Back up the entire world, especially `data/` and chunk/entity data.
3. Back up `config/ftbquests/quests` and all Rotwire TOML files.
4. Record current Rotwire and dependency versions from the mod list.
5. Keep a copy of the previous working mod JAR set.
6. Upgrade in a cloned/staging instance first.
7. Inspect startup and data-reload logs for missing registry IDs, malformed
   JSON/SNBT, dependency range errors, or skipped saved entries.
8. Run the smoke test in the next section.
9. Only then upgrade the production world.

Do not test world-generation compatibility only in old chunks. Generate fresh
Lost Cities terrain in staging.

## 11. Operational smoke test

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
16. A radio in a Lost Cities city finishes its survey, reports a city status
    drawer, preserves a previously cleared building, and raises danger after
    the configured number of unique encounter clears.
17. The radio `WX` drawer opens independently of the city drawer and reports
    stable today/tomorrow plans after a restart.
18. Ordinary rain applies configured stealth modifiers; contaminated rain
    warns under open sky, damages after four seconds, and clears under a roof.
19. A contaminated storm warns and damages on its shorter configured timing,
    with no grace-period bar visible.
20. `/rotwire weather force contaminated_storm 30s` immediately updates actual
    weather, the radio's test-override line, vignette, and open-sky damage;
    `/rotwire weather clear` resumes the unchanged daily plan.
21. At Y 48 in the Overworld, natural skeleton-family and creeper spawns are
    rejected while another natural hostile type remains unaffected.
22. At Y 47, restricted natural spawns are allowed; above the cutoff, a dungeon
    spawner or spawn egg can still create the same mob.
23. During daylight, a wilderness player eventually receives an outdoor
    Rotwire zombie while city chunks remain excluded from the wilderness tier.
24. Wilderness ambient zombies stop at two within 128 blocks by default and
    remain substantially less frequent than city-street zombies.

## 12. Diagnostics and recovery

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

Only entities spawned/marked by Rotwire count. Naturally spawned zombies do
not. Check that the entity has its `rotwireEncounter` persistent compound and
that its building key matches the saved encounter. Death events canceled or
replaced by another mod before Rotwire's lowest-priority handler may also
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

### Radio never connects to city operations

- Confirm `cityOperations.enabled` is true in the world's
  `rotwire-city-operations.toml`.
- Wait for both the calibration deadline and the city-grid survey to finish;
  the radio reports either remaining seconds or scanned candidate chunks while
  incomplete.
- Confirm the radio is in a dimension where Lost Cities exposes city chunks.
  An ordinary radio outside a mapped zone is expected to show no city status.
- Breaking and replacing the transmitter restarts both calibration and the
  paced survey.

### Turn-in consumed nothing

This is normal on any failed atomic validation. Check required non-optional
objectives and every tagged item task. Submission scans main inventory slots
only. The FTB item task should be tagged `rotwire_radio_submit`, use the right
count/matcher, and leave consumption to Rotwire.

### Reward claims but no delivery appears

Check server log for `generated no items`, then verify:

- custom reward has exactly the intended delivery tag;
- a nonblank `rotwire_manifest_` tag exists;
- matching table exists under `data/rotwire/loot_table/quest_delivery`;
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
and server run compatible Rotwire builds, the item IDs exist on the client,
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
- Another source may already impose closer fog; Rotwire never pushes it
  farther away.

### Forecast and actual weather disagree

- Confirm `weather.enabled` is true in the world's `rotwire-weather.toml`.
- Confirm the player and transmitter are in the Overworld.
- Disable Serene Seasons' `change_weather_frequency` option.
- Check for another mod that writes vanilla rain/thunder state rather than
  only rendering it.
- Remember that a listed wet day has a start/end window and is clear outside
  that window.
- Run `/rotwire weather status` to detect a persisted operator override.

### Contaminated warning is absent

- Confirm the current scheduled weather is contaminated, not ordinary.
- Stand where vanilla precipitation reaches the player's position; roofs,
  overhangs, and biomes without active rain prevent exposure.
- Creative and spectator players intentionally ignore the hazard.
- Check that client and server use the same Rotwire network protocol build.

### Handcrafted storage is empty

Only the explicit allowlist is handled. Player-placed blocks are intentionally
excluded. Confirm the block entity implements `Container`, lacks the
player-placed and stocked flags, and that
`rotwire:chests/handcrafted_storage` produces loot for the context.

## 13. Recovery principles

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
