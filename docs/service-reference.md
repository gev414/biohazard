# Service and class reference

This is the implementation catalog for all production Java classes. The word
"service" is used broadly: managers, adapters, registries, state models,
entities, client controllers, and payload records are all documented because
each is a maintenance boundary.

For cross-system sequences and authority rules, read
[Architecture](architecture.md). Paths below are relative to the repository.

## 1. Composition root

### `Rotwire`

Source: [`Rotwire.java`](../src/main/java/io/github/gev414/rotwire/Rotwire.java)

**Role.** NeoForge entry point and dependency-composition root. It should wire
services together but not contain gameplay rules.

**Owns.** `MOD_ID`, the shared SLF4J logger, and initialization order.

**Direct collaborators.** All deferred registry holders, the five config
classes, FTB defaults/integration, Lost Cities adapter, payload registration,
event adapters, `HandcraftedStorageLoot`, and `DeliveryManager`.

**Entry point.** Constructor injection by NeoForge supplies the mod event bus
and `ModContainer`.

**Maintenance notes.** Every new event listener, config file, deferred
register, or integration initializer must be reachable from here or from an
explicit automatic subscriber. Keep client-only class initialization out of
this common entry point.

## 2. Registry services

Registry holders use NeoForge `DeferredRegister` so objects are created during
the correct loader phase rather than at arbitrary class-load time.

### `ModBlocks`

Source: [`ModBlocks.java`](../src/main/java/io/github/gev414/rotwire/block/ModBlocks.java)

Registers `rotwire:radio_transmitter` as a `RadioTransmitterBlock`. Its
material behavior is brown map color, hardness 3, blast resistance 6, metal
sound, non-occluding shape, and correct-tool-required drops. The matching item,
block entity, model, blockstate, recipe, loot table, tags, translations, and
texture are separate contracts.

Depends on: `Rotwire.MOD_ID`, `RadioTransmitterBlock`, NeoForge registries.

### `ModBlockEntities`

Source: [`ModBlockEntities.java`](../src/main/java/io/github/gev414/rotwire/block/entity/ModBlockEntities.java)

Registers block entity type `rotwire:radio_transmitter`, constructed by
`RadioTransmitterBlockEntity::new` and valid only for the registered transmitter
block.

Depends on: `ModBlocks.RADIO_TRANSMITTER`, `RadioTransmitterBlockEntity`.

### `ModItems`

Source: [`ModItems.java`](../src/main/java/io/github/gev414/rotwire/item/ModItems.java)

Registers:

| Registry id | Java type | Stack size | Purpose |
|---|---|---:|---|
| `rotwire:radio_transmitter` | `BlockItem` | default | Places the radio block |
| `rotwire:tarp` | `TarpItem` | 1 | Deploys the multi-block campsite tarp |
| `rotwire:documents` | `Item` | 64 | Quest evidence/currency |
| `rotwire:research_data` | `Item` | 32 | Quest evidence/currency |
| `rotwire:encrypted_intel` | `Item` | 16 | High-tier quest evidence |
| `rotwire:quartermaster_cache_module` | `CampModuleItem(STORAGE)` | 1 | Installs secure camp storage |
| `rotwire:field_workshop_module` | `CampModuleItem(CRAFTING)` | 1 | Installs kit-based field repair |
| `rotwire:operations_relay_module` | `CampModuleItem(OPERATIONS)` | 1 | Installs camp telemetry and extended radio access |
| `rotwire:field_repair_kit` | `Item` | 16 | Consumable workshop repair input |
| `rotwire:infection_cure` | `InfectionMedicineItem(FULL_CURE)` | 4 | Removes The Hordes infection; epic rarity |
| `rotwire:antiviral_suppressant` | `InfectionMedicineItem(SUPPRESSANT)` | 8 | Delays infection/grants immunity; rare rarity |

Resource models, textures, translations, creative tabs, loot, and recipes refer
to these stable IDs.

### `ModMenus`

Source: [`ModMenus.java`](../src/main/java/io/github/gev414/rotwire/menu/ModMenus.java)

Registers the server-backed `rotwire:camp_radio` and `rotwire:camp_storage`
menu types. Client screens are bound separately by `ClientModEvents`.

### `ModEntities`

Source: [`ModEntities.java`](../src/main/java/io/github/gev414/rotwire/entity/ModEntities.java)

Registers:

- `rotwire:brute`, a monster-sized `BruteEntity` using the class's dimensions
  and eye height, with client tracking range 8;
- `rotwire:brute_rock`, a small miscellaneous projectile updated every 10
  ticks with client tracking range 4.

Attributes and renderers are not part of registry creation; they are registered
by `ModEntityEvents` and `ClientModEvents` respectively.

### `ModDamageTypes`

Source: [`ModDamageTypes.java`](../src/main/java/io/github/gev414/rotwire/damage/ModDamageTypes.java)

Declares the resource keys `rotwire:brute_rock_splash` and
`rotwire:contaminated_rain`. Both damage types are data-backed and included in
Minecraft's `no_anger` damage-type tag. Callers resolve the holder from the
active registry when damage is applied.

## 3. Configuration services

Each config builds its specification once through an idempotent `initialize()`
call. Detailed defaults and operational implications are in
[Configuration and operations](configuration-and-operations.md).

### `EncounterConfig`

Source: [`EncounterConfig.java`](../src/main/java/io/github/gev414/rotwire/config/EncounterConfig.java)

Defines the server-side `rotwire-encounters.toml` contract: master switch,
selection probabilities (including a separate multi-chunk boss chance),
snapshotted spawn mode, activation radius/scan
interval, kill range, active-mob cap, update interval, spawn distances and
attempts, boss warning, container lock/message toggles, regular mob IDs, and
excluded Lost Cities building IDs.

Helper methods normalize reversed min/max settings. `isExcluded` compares the
full resource-location string exactly. Config values are read live by services,
except selection decisions, target kills, and spawn mode already persisted for
a building.

Consumed by: `EncounterManager`, `EncounterSpawner`, `EncounterEvents`.

### `HordeAtmosphereConfig`

Source: [`HordeAtmosphereConfig.java`](../src/main/java/io/github/gev414/rotwire/config/HordeAtmosphereConfig.java)

Defines client-side `rotwire-client.toml`: enabled flag, pre-event fade
duration, and target near/far fog planes. It affects only presentation and is
consumed by `HordeAtmosphereClientEvents`.

### `MobSpawnConfig`

Source: [`MobSpawnConfig.java`](../src/main/java/io/github/gev414/rotwire/config/MobSpawnConfig.java)

Defines server-side `rotwire-mobs.toml`. The `undergroundRestrictions`
section contains the master switch, individual skeleton-family and creeper
switches, minimum depth below the active dimension's sea level, and a
dimension-ID allowlist. The `wildernessZombies` section controls its own
enablement, interval, chance, cap/radius, player distances, position attempts,
and dimension allowlist. Dimension entries are validated as resource
locations. All settings are read live and the feature owns no durable state.

Consumed by: `MobSpawnRestrictions`, `SurfaceZombieSpawner`.

### `RadioQuestConfig`

Source: [`RadioQuestConfig.java`](../src/main/java/io/github/gev414/rotwire/config/RadioQuestConfig.java)

Defines server-side `rotwire-radio-quests.toml`: transmitter range,
calibration duration, and per-category courier delays. It is consumed by radio
block entities, radio proximity search, and `DeliveryCategory.delayTicks()`.

### `SettlementConfig`

Source: [`SettlementConfig.java`](../src/main/java/io/github/gev414/rotwire/config/SettlementConfig.java)

Defines the server-side `rotwire-settlements.toml`: shared ration capacity,
survivor recruitment rules, and siege timing/spawn/raid controls. It is read
live by the Camp Hub flow and settlement upkeep/siege tick.

### `SurvivalSystemsConfig`

Source: [`SurvivalSystemsConfig.java`](../src/main/java/io/github/gev414/rotwire/config/SurvivalSystemsConfig.java)

Defines `rotwire-survival.toml`: weight categories and tier thresholds,
movement penalties, progressive visual-awareness tuning, alert memory, loud
action grace, attention radii, coordinated infected AI/path budgets,
ZombieTactics ownership suppression, and incoming knockback retention for
zombies and players. Runtime behavior values are read live; attaching the
controller and removing third-party goals occurs on entity join. The
specification is initialized idempotently.

### `WeatherConfig`

Source: [`WeatherConfig.java`](../src/main/java/io/github/gev414/rotwire/config/WeatherConfig.java)

Defines `rotwire-weather.toml`: scheduler enablement, optional seasonal
weighting, daily outcome weights, event duration ranges, contaminated exposure
grace/damage, and ordinary-weather suspicion/attention multipliers. Its
`generationRules()` helper snapshots the plan-generation values used for a new
day.

## 4. Event adapters

Event adapters translate framework events into domain calls. They own event
filtering and priority choices, not durable state.

### `EncounterEvents`

Source: [`EncounterEvents.java`](../src/main/java/io/github/gev414/rotwire/event/EncounterEvents.java)

Handles three runtime events:

- `ServerTickEvent.Post` delegates to `EncounterManager.tick`.
- `LivingDeathEvent`, registered at lowest priority, reads an encounter marker
  after earlier handlers had an opportunity to cancel/alter death and delegates
  credit to `EncounterManager.recordDeath`.
- `RightClickBlock` first gives `HandcraftedStorageLoot` a chance to lazily
  stock selected storage. Otherwise it enforces haunted-building locks for
  randomizable containers after resolving and, if necessary, materializing the
  encounter.

Important ordering: selected Handcrafted storage returns before the encounter
lock path, so it is not locked by this implementation.

### `HordeAtmosphereSyncEvents`

Source: [`HordeAtmosphereSyncEvents.java`](../src/main/java/io/github/gev414/rotwire/event/HordeAtmosphereSyncEvents.java)

Server adapter for The Hordes state. Once per second it derives a compact
`HordeAtmospherePayload` per online player. A last-sent map suppresses identical
packets. Login forces an initial payload; logout removes that player's cache;
server stop clears all transient state.

Direct dependencies: The Hordes `HordeEventConfig`, `HordeSavedData`, and
per-player `HordeEvent`; NeoForge `PacketDistributor`.

No state from this class is durable or authoritative. Its cache exists only to
reduce network traffic.

### `ModCreativeTabEvents`

Source: [`ModCreativeTabEvents.java`](../src/main/java/io/github/gev414/rotwire/event/ModCreativeTabEvents.java)

Populates vanilla creative tabs:

- functional blocks: Radio Transmitter;
- ingredients: Documents, Research Data, Encrypted Intel;
- food and drinks: Antiviral Suppressant and Infection Cure.

Adding a registered item does not automatically make it discoverable in
creative mode; update this adapter where appropriate.

### `ModEntityEvents`

Source: [`ModEntityEvents.java`](../src/main/java/io/github/gev414/rotwire/event/ModEntityEvents.java)

Binds the Brute entity type to the attributes produced by
`BruteEntity.createAttributes()`. Missing this binding normally produces an
entity registration/runtime failure.

### `MobSpawnRestrictions`

Source: [`MobSpawnRestrictions.java`](../src/main/java/io/github/gev414/rotwire/mob/MobSpawnRestrictions.java)

Handles NeoForge's early `MobSpawnEvent.SpawnPlacementCheck`. For enabled
dimensions it rejects `NATURAL` spawn attempts above the configured
sea-level-relative maximum for creepers and the vanilla skeleton family
(skeleton, stray, bogged, and wither skeleton). Other entity types and spawn
sources retain NeoForge's default result, including spawners, commands, spawn
eggs, structure triggers, and Rotwire `EVENT` encounters.

The cutoff is inclusive: `sea level - minimumDepthBelowSeaLevel` is allowed;
the next Y position is rejected. The check intentionally avoids sky and
heightmap tests so covered Lost Cities surface structures are not mistaken
for underground space.

### `SurfaceZombieSpawner`

Source: [`SurfaceZombieSpawner.java`](../src/main/java/io/github/gev414/rotwire/mob/SurfaceZombieSpawner.java)

Unifies Rotwire's bounded outdoor zombie population. On each due interval it
classifies a player from Lost Cities chunk metadata:

- city players retain the existing city-street chance, cap, distances, and
  street-only candidate rule from `CityOperationsConfig`;
- non-city players use the much lower wilderness chance and independent cap
  from `MobSpawnConfig`.

Successful candidates come from the loaded
`MOTION_BLOCKING_NO_LEAVES` surface and require no fluid, sturdy footing,
world-border containment, distance from every player, collision clearance,
and normal obstruction checks. The city-street tier additionally requires
open sky; wilderness terrain beneath foliage remains eligible. No darkness
test is applied, so the same path functions during daylight. City and
wilderness entities carry separate transient origin markers for their
respective nearby caps and otherwise despawn normally.

The spawner honors Peaceful difficulty and `doMobSpawning`. The Biohazard
profile's required Hordes configuration disables zombie sunlight ignition;
Rotwire does not cancel ordinary fire or lava damage.

### `SurvivalSystemsEvents` and `SurvivalStatusSync`

Sources: [`SurvivalSystemsEvents.java`](../src/main/java/io/github/gev414/rotwire/event/SurvivalSystemsEvents.java),
[`SurvivalStatusSync.java`](../src/main/java/io/github/gev414/rotwire/event/SurvivalStatusSync.java)

The event facade routes server ticks, target changes, incoming damage, block
breaks, PointBlank sounds, coordinated-hostile joins, login/logout, and server
stop to the encumbrance, stealth, and infected-AI services.
`SurvivalStatusSync` sends changed weight,
tier, quiet state, and maximum nearby suspicion at most every five ticks.

## 5. Lost Cities adapter

### `LostCitiesIntegration`

Source: [`LostCitiesIntegration.java`](../src/main/java/io/github/gev414/rotwire/lostcities/LostCitiesIntegration.java)

Requests `ILostCities` through Lost Cities inter-mod communication during
common setup. The nested `ApiReceiver` stores the supplied API in a nullable
static field. `api()` can therefore return null before the handshake or if the
integration is unavailable; callers must degrade safely.

This class isolates API acquisition from encounter code. If Lost Cities changes
its handshake, only this adapter should need structural changes.

### `LostCitiesBuildingResolver`

Source: [`LostCitiesBuildingResolver.java`](../src/main/java/io/github/gev414/rotwire/lostcities/LostCitiesBuildingResolver.java)

Converts Lost Cities chunk metadata into Rotwire's stable
`BuildingDescriptor`. It can resolve the current chunk or search chunk metadata
for the nearest deduplicated building inside a configured radius.

Algorithm:

1. Require an acquired API and `ILostCityInformation` for the server level.
2. Convert block position to chunk coordinates and request `ILostChunkInfo`.
3. Require a real building ID.
4. For a multi-building, subtract its member offsets to find the root chunk,
   use its width/height, and replace the member building ID with the
   multi-building type.
5. Compute the base Y from city level.
6. Extend downward by `numCellars * 6` and upward by
   `(numFloors + 1) * 6`, clamped to dimension build bounds.
7. Return a descriptor keyed by dimension and root chunk.

`resolveNearest` applies that conversion across the chunk square intersecting
the configured radius, deduplicates multi-building members by `BuildingKey`,
filters by distance to each descriptor AABB, and returns the closest match.

The constant six-block floor height is an external-format assumption and must
be revalidated on Lost Cities upgrades.

Depends on: `LostCitiesIntegration`, Lost Cities API types, `BuildingKey`, and
`BuildingDescriptor`.

## 6. Encounter domain

### `BuildingKey`

Source: [`BuildingKey.java`](../src/main/java/io/github/gev414/rotwire/encounter/BuildingKey.java)

Immutable identity for one physical building: dimension ID and root chunk X/Z.
It provides direct NBT save/load helpers. Multi-building normalization happens
before construction, in the resolver.

Used as: saved-data map key, deterministic selection seed input, and entity
marker identity.

Compatibility contract: NBT keys `dimension`, `rootChunkX`, `rootChunkZ`.

### `BuildingDescriptor`

Source: [`BuildingDescriptor.java`](../src/main/java/io/github/gev414/rotwire/encounter/BuildingDescriptor.java)

Immutable runtime description returned by the Lost Cities adapter. It combines
identity, building resource ID, horizontal chunk dimensions, and vertical
bounds. Constructor validation rejects null IDs, nonpositive dimensions, and
empty vertical ranges.

`bounds()` creates the full AABB used for loaded-entity searches.
`contains(BlockPos)` performs half-open integer bound checks used for players,
containers, and spawn positions. `isMultiChunk()` identifies footprints larger
than 1x1 for boss probability selection. Interior-floor helpers split Lost Cities'
six-block floor bands and exclude the final roof band. `distanceToSqr(BlockPos)`
supports proximity selection against the building volume rather than its chunk
center.

This object is reconstructed from external metadata and is not itself saved.

### `EncounterSelection`

Source: [`EncounterSelection.java`](../src/main/java/io/github/gev414/rotwire/encounter/EncounterSelection.java)

Pure deterministic selection function returning `haunted`, `bossSelected`, and
`targetKills`. It mixes the world seed, dimension string hash, unsigned root
chunk X, and rotated unsigned root chunk Z with a SplitMix-style 64-bit mixer.
Separate mixed values drive haunted, boss, and inclusive target rolls.

Properties:

- identical seed/key/config yields identical output;
- a non-haunted selection always has zero target, but can carry a boss when
  the caller marks that building as eligible;
- non-haunted, non-eligible selection always has no boss;
- reversed min/max inputs are normalized;
- the target range includes both endpoints.

Because results are persisted immediately, changing this algorithm affects
only previously unseen buildings unless saved data is migrated or removed.

### `EncounterPhase`

Source: [`EncounterPhase.java`](../src/main/java/io/github/gev414/rotwire/encounter/EncounterPhase.java)

State-machine enum:

| Phase | Meaning | Locks containers | Normal outgoing transition |
|---|---|---:|---|
| `SAFE` | Selection was neither haunted nor boss-only | no | none |
| `REGULAR_WAVE` | Kill target is in progress | yes | `BOSS_PENDING` or `CLEARED` |
| `BOSS_PENDING` | Warning delay, or initial boss-only Brute placement | yes | `BOSS_ACTIVE` |
| `BOSS_ACTIVE` | Encounter Brute has been activated | yes | `CLEARED` |
| `CLEARED` | Encounter completed | no | none |

The enum name is serialized. Renaming a constant is a save-format change.

### `BuildingEncounter`

Source: [`BuildingEncounter.java`](../src/main/java/io/github/gev414/rotwire/encounter/BuildingEncounter.java)

Mutable aggregate containing one building's durable encounter state. A
non-haunted boss selection materializes directly as `BOSS_PENDING` with a zero
regular-kill target. Immutable decisions are building ID, boss selection, kill
target, and spawn mode; mutable
progress is phase, regular deaths, successful initial regular spawns, initial
population-attempt state, boss UUID, and boss-ready game time.

Mutation methods enforce legal transitions:

- `recordRegularDeath` ignores terminal phases and caps at the target;
- `beginInitialPopulation` records the one-time instant-population attempt;
- `recordRegularSpawn` advances bounded instant-population progress;
- `beginBossWarning` works only from `REGULAR_WAVE`;
- `activateBoss` works only from `BOSS_PENDING`;
- `clear` rejects `SAFE` and already-cleared encounters.

The manager is responsible for calling `EncounterSavedData.setDirty()` after a
successful mutation. The aggregate does not know its persistence owner.

Serialization keys: `version`, `buildingId`, `bossSelected`, `targetKills`,
`spawnMode`, `phase`, `regularDeaths`, `regularSpawns`,
`initialPopulationAttempted`, optional `bossUuid`, `bossReadyGameTime`.
Legacy records without `spawnMode` load as `WAVE`.

### `EncounterSavedData`

Source: [`EncounterSavedData.java`](../src/main/java/io/github/gev414/rotwire/encounter/EncounterSavedData.java)

Server-wide persistence repository backed by Overworld `DataStorage`, file key
`rotwire_building_encounters`. It stores a linked map from `BuildingKey` to
`BuildingEncounter`.

API:

- `get(server)` loads/creates the repository;
- `find(key)` performs a non-materializing lookup;
- `getOrCreate(key, factory)` inserts once, marks dirty, and returns both the
  aggregate and whether it was newly created;
- `save` writes a list of `{key, encounter}` compounds.

Loading catches runtime failures per entry and retains the rest. It uses
`DataFixTypes.LEVEL` but has no custom data fixer.

### `EncounterEntityData`

Source: [`EncounterEntityData.java`](../src/main/java/io/github/gev414/rotwire/encounter/EncounterEntityData.java)

Utility for attaching/reading a `rotwireEncounter` compound in an entity's
persistent data. The compound embeds `BuildingKey` fields plus lower-case role.
`read` converts malformed or unknown role data to `Optional.empty()`.

`Role.REGULAR` deaths increment regular progress. `Role.BOSS` deaths clear the
encounter. `matches` is used to count/find loaded entities belonging to a
specific building and role.

### `EncounterManager`

Source: [`EncounterManager.java`](../src/main/java/io/github/gev414/rotwire/encounter/EncounterManager.java)

Primary encounter application service. It owns orchestration but not durable
storage.

Public API:

- `tick(server)` throttles proximity scans and scheduled encounter updates;
- `materialize(level, descriptor)` creates and persists initial selection once;
- `recordDeath(server, marker)` applies role-specific progress and marks data
  dirty.

Tick responsibilities:

- filter dead/spectator players;
- resolve each player's nearest building in range and group by `BuildingKey`;
- honor building exclusions;
- create one-time instant populations or maintain wave populations;
- transition to warning/clear at the target;
- find/adopt or spawn the Brute after the warning.

Key semantics:

- encounter spawning pauses when globally disabled, but saved state remains;
- only the nearest building per player is activated on a proximity scan,
  preventing dense-city fan-out;
- the configured spawn mode is snapshotted for each newly discovered building;
- instant populations are persistent and retry missing placements without
  replacing successfully placed members;
- boss buildings stop replacing regular mobs as soon as the target is reached;
- non-boss buildings clear only after loaded marked regulars reach zero;
- `BOSS_ACTIVE` never creates a replacement when the saved boss is absent from
  loaded entity queries, preventing duplicates after chunk unload.

Depends on: config, resolver, selection/aggregate/repository, spawner, Brute,
Minecraft player/server APIs, translations.

### `EncounterSpawner`

Source: [`EncounterSpawner.java`](../src/main/java/io/github/gev414/rotwire/encounter/EncounterSpawner.java)

Entity query and spawn service for encounters.

Public operations:

- count loaded marked regulars in a descriptor AABB;
- find a loaded marked Brute boss;
- spawn one configured regular hostile mob;
- spawn a bounded one-time batch of persistent regular hostile mobs;
- spawn one persistent full-health Brute.

Regular pool resolution is deliberately late-bound from config strings through
the entity registry. Entries are rejected when missing, equal to the Brute, or
not in `MobCategory.MONSTER`; each invalid string warns only once per process.

Wave search first samples a distance and angle around a random nearby player.
If that cannot reach the building, and for instant population directly, search
samples the building's interior six-block floor bands. Initial instant-spawn
attempts rotate across those bands to distribute mobs vertically. The final
roof band is excluded, and each candidate must have a sturdy interior ceiling.
Both paths try vertical offsets `0, +1, -1, +2, -2, +3, -3` while keeping
building-wide candidates within their selected floor. Candidates must also be
at least the minimum distance from every nearby player, loaded, inside the
world border, dry, supported by a sturdy upper face, fully within the building
AABB, collision-free, and unobstructed according to the mob. The near-player
path also requires the configured maximum distance from at least one player.

The helper positions the temporary mob during validation. Regular mobs then run
normal event-spawn initialization before insertion. Wave zombies receive an
ambient sound after successful spawn. Instant regulars and bosses require
persistence and are marked before insertion.

## 7. Brute entity domain

### `BruteEntity`

Source: [`BruteEntity.java`](../src/main/java/io/github/gev414/rotwire/entity/BruteEntity.java)

Custom boss based on vanilla `Zombie` and implementing `RangedAttackMob`.

Static gameplay values:

| Property | Value |
|---|---:|
| Scale | 1.5 |
| Hitbox | 0.9 x 2.925 blocks |
| Eye height | 2.61 blocks |
| Max health | 250 |
| Attack damage | 8 |
| Attack knockback | 1 |
| Armor | 6 |
| Knockback resistance | 0.5 |
| Movement speed | 0.23 |
| Follow range | 40 |
| XP reward | 100 |
| Rock range | 6 to 18 blocks |
| Rock windup | 20 ticks |
| Rock cooldown | 120 ticks |

Goal priority 0 installs `RockThrowAttackGoal` alongside inherited zombie
goals. `performRangedAttack` aims above the target based on horizontal distance,
fires at speed 1.4 with distance-scaled inaccuracy, plays an iron-golem attack
sound, and adds the projectile server-side.

Boss bar behavior is combat-scoped, not simply tracking-scoped. The class
tracks players who can see the entity, players who attacked or became its
target, and whether engagement is active. Only eligible tracked combat
participants see the red progress bar. Initial attackers are retained for a
40-tick target-acquisition grace period. Losing a valid player target hides the
bar and clears combat participation. Death/removal clears all bar state.

The tracking and participant collections are transient. If future design needs
combat participation across reloads, it requires explicit persistence.

### `RockThrowAttackGoal`

Source: [`RockThrowAttackGoal.java`](../src/main/java/io/github/gev414/rotwire/entity/ai/RockThrowAttackGoal.java)

Generic ranged goal for a type that is both `Mob` and `RangedAttackMob`.
Constructor validation rejects invalid ranges/timings. It claims MOVE and LOOK,
requires a living visible target within the inclusive range band, stops
navigation during windup, tracks the target every tick, attacks when windup
reaches zero, and sets an absolute game-time cooldown.

If target identity, line of sight, life, or range becomes invalid during
windup, the goal stops without attacking. It uses squared distance until the
final normalized distance factor.

### `BruteRockProjectile`

Source: [`BruteRockProjectile.java`](../src/main/java/io/github/gev414/rotwire/entity/projectile/BruteRockProjectile.java)

Throwable item projectile rendered as cobblestone.

On first server-side hit it:

1. offsets block impact positions 0.01 blocks outward to avoid buried effects;
2. plays stone-break sound and sends 12 cobblestone block particles;
3. deals 6 thrown direct damage to a non-owner direct target;
4. applies 0.6 extra knockback in flight direction if direct damage succeeds;
5. searches a two-block-radius AABB for other living targets;
6. excludes owner/direct target, checks bounding-box distance and line of
   exposure with `Explosion.getSeenPercent`;
7. deals 4 `rotwire:brute_rock_splash` damage;
8. discards itself.

Splash damage has an impact position but no causing/direct entity in the
constructed `DamageSource`. That affects attribution, armor/enchantment rules,
and anger behavior; the damage type is also tagged `no_anger`.

### `BruteRenderer`

Source: [`BruteRenderer.java`](../src/main/java/io/github/gev414/rotwire/client/renderer/BruteRenderer.java)

Client-only renderer extending vanilla `ZombieRenderer`. It uses
`textures/entity/brute.png`, multiplies shadow radius by the Brute scale, and
scales the pose stack uniformly by 1.5. Entity hitbox dimensions are already
scaled in the registered entity type; renderer scale controls visual size.

## 8. Radio block and network proximity

### `RadioTransmitterBlock`

Source: [`RadioTransmitterBlock.java`](../src/main/java/io/github/gev414/rotwire/block/RadioTransmitterBlock.java)

Horizontal, non-full-cube block with four direction-specific voxel shapes and a
block entity. Placement faces the player and begins calibration server-side.
Rotation and mirror operations preserve correct facing.

Server interaction first refreshes camp identity. A sheltered radio opens the
Camp Hub even when its campsite later becomes incomplete. An ordinary radio
continues through calibration/survey feedback and then opens the established
Survivor Network flow. Using a `CampModuleItem` on an online owned Camp Radio
installs its typed extension and consumes the item outside creative mode.
Removing the block drops installed modules and all cached stacks.

The client returns success immediately to provide normal interaction feedback;
all meaningful work remains on the server.

Direct dependencies: FTB Quests and Architectury networking, `RadioNetwork`,
`DeliveryManager`, transmitter block entity.

### `RadioTransmitterBlockEntity`

Source: [`RadioTransmitterBlockEntity.java`](../src/main/java/io/github/gev414/rotwire/block/entity/RadioTransmitterBlockEntity.java)

Persists the absolute `ready_at` game time, city survey state/key, camp UUID,
owner UUID, sanitized module bitmask, and twenty-seven-slot cache. Its
once-per-second server tick establishes identity whenever the radio becomes
sheltered and advances the city survey.

It authorizes owner-only module installation, cache access, and workshop use.
Installation requires an active campsite and connected radio. The workshop
also requires a damaged main-hand item and one Field Repair Kit, then restores
`max(1, maxDurability / 4)` damage and consumes the kit outside creative mode.
Cache access remains available while camp readiness is offline. Block removal
empties the cache and returns one item for every installed module.

Because the absolute deadline is saved, changing `calibrationTicks` does not
retroactively change a transmitter that already has `ready_at`.

### `SettlementManager` and `SettlementSavedData`

Sources: [`SettlementManager.java`](../src/main/java/io/github/gev414/rotwire/settlement/SettlementManager.java),
[`SettlementSavedData.java`](../src/main/java/io/github/gev414/rotwire/settlement/SettlementSavedData.java)

`SettlementManager` maps an active, surveyed Camp Radio to the server-wide
settlement record for its `CityZoneKey`. The first complete camp becomes the
fixed primary hub; later radios in that city become relays. It exposes the
server-authoritative naming, Camp Hub upgrade, physical stockpile scan and
consumption, population, live siege scheduling, and radio-status operations.
`SettlementSavedData` persists those records under `rotwire_settlements` and
skips malformed individual entries when loading.

`SettlementSiegeManager` advances `CALM`, `WARNING`, `ACTIVE`, and `RECOVERY`
for named, online Camp Hubs with at least three living survivors. It never
force-loads chunks: active assault spawns and stockpile raids require a nearby
player, and spawn candidates must be in entity-ticking chunks. Siege mobs
receive a persisted camp objective consumed by
`CoordinatedHostileAi`; a living target becomes `HUNT`, otherwise the camp is
an `ASSAULT` objective. `CoordinatedHostileGoal` owns the approach, melee,
segmented long-distance paths, and tagged progressive breaching without a
second siege-specific navigation goal. Only repeated route failures or a truly
stuck navigation state permit a bounded direct-line and forward collision
corridor inspection, covering multipart modded fences without treating a
completed path segment as a blockage. `SiegeCoordinationManager` shares
survivor contacts, expiring one-report-per-infected failure evidence, and one
guarded structural breach lane for each camp. Only blocked infected are
assigned to that lane; members with valid paths retain their local approach.
Each destroyed block suspends the lane and broadcasts an opening revision so
local and shared breachers cancel obsolete adjacent targets and repath. The
lane continues only after renewed failures from its participants, and its
selector rejects ordinary one-block steps with clear headroom.
The opening is also retained as a collision-validated, camp-oriented gateway;
outside infected path to its approach and receive bounded direct steering
through the passage. Gateway geometry ignores transient entity occupancy and
is invalidated only by block geometry. Below-grade trapped groups may share a
local upward-only escape ramp without redirecting distant siege members.
Partial Minecraft paths remain useful for movement but
count as failures when their unreachable endpoint is reached. Shared survivor
targets are adopted only within the local survivor-scan radius, so distant
siege members continue toward the fixed camp instead of repeatedly repathing
to a moving guard.
`SiegeBreachRules` maps fragile and
reinforced resistance tags, with uncategorized allowed blocks using the
standard duration. Its structural fallback accepts ordinary solid materials
but rejects technical, block-entity, fluid, negative-hardness, protected, and
below-floor targets. Armed survivors remember close threats, maintain a
weapon-specific minimum distance, and use cooldown-driven lateral repositioning
when a solid obstacle blocks their shot. Civilians and empty guards retreat.

### `RadioNetwork`

Source: [`RadioNetwork.java`](../src/main/java/io/github/gev414/rotwire/quest/RadioNetwork.java)

Server proximity service. `findConnectedTransmitter` scans the larger of the
configured transmitter and campsite radii, skips unloaded or disconnected
radios, and accepts the ordinary spherical transmitter range. A connected
radio with an Operations Relay is also accepted anywhere inside its currently
active campsite radius. The first match is returned; callers need only proof
of connection.

### `CampRadioMenu` and `CampStorageMenu`

Sources: [`CampRadioMenu.java`](../src/main/java/io/github/gev414/rotwire/menu/CampRadioMenu.java),
[`CampStorageMenu.java`](../src/main/java/io/github/gev414/rotwire/menu/CampStorageMenu.java)

`CampRadioMenu` synchronizes shelter requirements, nutrition, calibration,
ownership, installed modules, and—while the Operations Relay is active—weather,
nearby hostiles, mapped danger, and ready/pending courier counts once per
second. Its validated buttons open contracts, storage, or perform a repair.
`CampStorageMenu` exposes the persistent 3x9 cache plus player inventory and
supports shift-click transfer in both directions. Both remain valid only while
the same radio exists within eight blocks.

Complexity grows cubically with configured range. The allowed maximum of 32 can
mean scanning up to 65 cubed positions per button press. Keep this in mind if
increasing the upper bound or calling it from a tick loop.

`calibrationSecondsRemaining` rounds ticks upward to seconds.

## 9. FTB Quests integration

### `QuestDefaultsInstaller`

Source: [`QuestDefaultsInstaller.java`](../src/main/java/io/github/gev414/rotwire/quest/QuestDefaultsInstaller.java)

Installs nine packaged files from `/rotwire/ftbquests_defaults/` into
`config/ftbquests/quests` during mod construction. If the destination directory
exists and has any entry, it logs and preserves it. An absent or empty directory
receives the complete default set, creating parents and replacing individual
targets during that initial operation.

The explicit `DEFAULT_FILES` list is a release contract and is mirrored by
`QuestDefaultsResourceTest`. Adding a default chapter requires updating both.
The backup chapter present under resources is not installed.

### `FTBQuestsIntegration`

Source: [`FTBQuestsIntegration.java`](../src/main/java/io/github/gev414/rotwire/quest/FTBQuestsIntegration.java)

Adapter to FTB Quests' Architectury events. Initialization is idempotent.

Task protocol:

- server-side custom task with `rotwire_radio_accept`: configure a one-point,
  button-enabled task whose callback requires a connected radio and completes
  it;
- server-side custom task with `rotwire_radio_complete`: configure the same
  button mechanics, then delegate atomic turn-in to `RadioSubmission`.

Reward protocol:

- `rotwire_radio_delivery`: schedule generated items;
- `rotwire_radio_choice_delivery`: schedule distinct options;
- `rotwire_manifest_<name>`: required manifest suffix;
- `rotwire_category_<name>`: optional, defaults to supplies;
- `rotwire_choice_count_<1..9>`: optional, defaults to 3.

If both delivery tags are present, choice behavior wins. Missing manifest shows
an error but the event still returns pass; FTB Quests' reward lifecycle remains
its own responsibility. Full authoring behavior is documented in
[radio-quests.md](radio-quests.md).

### `RadioSubmission`

Source: [`RadioSubmission.java`](../src/main/java/io/github/gev414/rotwire/quest/RadioSubmission.java)

Package-private atomic turn-in service. It first verifies every quest task
except the final completion task, tagged submission item tasks, and tasks FTB
considers optional for progression. Any incomplete required objective aborts.

For each `ItemTask` tagged `rotwire_radio_submit`, it uses `task.test(stack)`
against main inventory slots and an in-memory remaining-count array. Allocation
records prevent two requirements from spending the same item count. If every
task reaches `getMaxProgress`, it shrinks allocated stacks, adds only missing
FTB progress, marks inventory changed, and broadcasts container changes. Then
the custom completion task is completed.

Scope caution: only `player.getInventory().items` is scanned. Armor, offhand,
external inventories, Curios-like slots, and nearby containers are not valid
submission sources.

## 10. Courier delivery domain

### `DeliveryCategory`

Source: [`DeliveryCategory.java`](../src/main/java/io/github/gev414/rotwire/quest/delivery/DeliveryCategory.java)

Enum of `SUPPLIES`, `AMMUNITION`, `MEDICAL`, `EQUIPMENT`, and `FIREARM`.
It maps reward tags to a category, category to configured seconds, seconds to
ticks with exact multiplication, and enum values to lower-case serialized
names. Missing or future unknown names safely fall back to supplies.

Adding a category affects configuration, serialization, translations, quest
authoring, tests, and potentially existing saves.

### `DeliveryKind`

Source: [`DeliveryKind.java`](../src/main/java/io/github/gev414/rotwire/quest/delivery/DeliveryKind.java)

Package-private enum distinguishing normal `ITEMS` from unresolved `CHOICE`.
Unknown serialized values fall back to items, prioritizing recoverable
collection over an unusable screen.

### `RadioDelivery`

Source: [`RadioDelivery.java`](../src/main/java/io/github/gev414/rotwire/quest/delivery/RadioDelivery.java)

Package-private mutable persistent aggregate for one shipment. Constructor and
mutators copy item stacks to avoid aliasing caller-owned stacks.

Behavior:

- readiness compares current game time with `readyAt`;
- notification is a persisted one-way flag;
- partial collection replaces the saved item list with remainders;
- choice selection copies exactly one indexed option and changes kind to
  `ITEMS` so normal collection can finish it.

NBT keys: `id`, `owner`, `reward_id`, `manifest`, `category`, `ordered_at`,
`ready_at`, `kind`, `notified`, and list `items` using standard `ItemStack`
serialization.

### `DeliverySavedData`

Source: [`DeliverySavedData.java`](../src/main/java/io/github/gev414/rotwire/quest/delivery/DeliverySavedData.java)

Package-private repository in Overworld data storage under
`rotwire_radio_deliveries`. It exposes the mutable delivery list only within
the package and marks itself dirty when adding. Managers must mark dirty after
mutating/removing existing records.

Loading skips malformed records with an error log and skips records whose item
list is empty. There is no explicit file-format version or custom migration.

### `DeliveryManager`

Source: [`DeliveryManager.java`](../src/main/java/io/github/gev414/rotwire/quest/delivery/DeliveryManager.java)

Primary courier application service.

**Scheduling.** Rolls a named loot table with player origin/entity/luck. Empty
output logs an error and aborts. Otherwise it creates a UUID, calculates ready
time from Overworld game time and category config, persists the already-rolled
items/options, and notifies the player of the delay.

**Choice generation.** Repeatedly rolls the same manifest, retaining candidates
that differ by item and components, until the requested count or
`count * 8` attempts. A table may therefore yield fewer options than requested,
but not zero.

**Tick.** Once per second, finds ready unnotified deliveries whose owner is
online, marks them notified, sends a category message, and dirties the
repository.

**Collection.** Iterates ready, owned, non-choice deliveries. When collection
originates from an owned radio with the Quartermaster Cache, insertion targets
that cache first and the player inventory second. Leftover amounts replace
persisted contents. Fully inserted deliveries are removed. The player is told
how many whole delivery records were collected, not how many stacks.

**Choice opening.** Sends the first ready owned choice delivery. Only registry
item IDs are sent, not count or components; current manifests use one-item
options, so the display is representative. The full selected saved stack is
retained server-side after selection.

**Selection.** Requires a connected transmitter, valid UUID, owner, choice
kind, readiness, and valid index. It converts and collects on success.

**Status.** Reports ready count, pending count plus soonest rounded-up seconds,
or no deliveries. A ready message uses the `inventory_full` translation because
normal collection already ran before status is requested.

## 11. Network protocol

### `ModPayloads`

Source: [`ModPayloads.java`](../src/main/java/io/github/gev414/rotwire/network/ModPayloads.java)

Registers protocol version string `5` and seven play-phase payloads:

| Payload ID | Direction | Handler |
|---|---|---|
| `rotwire:horde_atmosphere` | server to client | update transient fog state |
| `rotwire:city_status` | server to client | update the radio-linked QuestScreen panel |
| `rotwire:survival_status` | server to client | update load and stealth HUD state |
| `rotwire:weather_forecast` | server to client | update the radio `WX` drawer |
| `rotwire:weather_exposure` | server to client | update the contaminated-weather warning |
| `rotwire:courier_choice_open` | server to client | open choice screen |
| `rotwire:courier_choice_select` | client to server | validate and apply choice |

Changing field encoding or compatibility may require a protocol version change.
Keep handlers side-safe.

### `HordeAtmospherePayload`

Source: [`HordeAtmospherePayload.java`](../src/main/java/io/github/gev414/rotwire/network/HordeAtmospherePayload.java)

Record containing two booleans and two variable-length integers. Its handler
replaces `HordeAtmosphereState`. Day length and start time are normalized again
inside the client snapshot, providing defense against invalid values.

### `CourierChoiceOpenPayload`

Source: [`CourierChoiceOpenPayload.java`](../src/main/java/io/github/gev414/rotwire/network/CourierChoiceOpenPayload.java)

Server-to-client record with a 36-character delivery UUID string and a list of
item ID strings, each capped at 256 characters. The count is a VarInt. Current
server generation caps authored choice count at nine, but the decoder itself
does not impose a list-count bound; protocol evolution should add compatible
defensive bounds if input trust requirements change.

Handler delegates to `CourierChoiceClient`.

### `CityStatusPayload`

Source: [`CityStatusPayload.java`](../src/main/java/io/github/gev414/rotwire/network/CityStatusPayload.java)

Server-to-client snapshot sent when a calibrated transmitter opens the Survivor
Network. The client renders it as a compact, collapsed-by-default city-status
drawer on the right edge of the FTB Quests `QuestScreen`; it is cleared when
that screen closes. An unmapped radio uses the same payload with `mapped=false`,
so city status no longer occupies Minecraft chat.

### `SurvivalStatusPayload`

Source: [`SurvivalStatusPayload.java`](../src/main/java/io/github/gev414/rotwire/network/SurvivalStatusPayload.java)

Server-to-client transient HUD snapshot containing tenths of weight, tier
ordinal, enabled state, quiet state, clamped suspicion percentage, all three
server tier boundaries, and all three non-Light speed penalties. Including
configuration values keeps the inventory tooltip authoritative on dedicated
servers.

### `WeatherForecastPayload` and `WeatherExposurePayload`

Sources: [`WeatherForecastPayload.java`](../src/main/java/io/github/gev414/rotwire/network/WeatherForecastPayload.java),
[`WeatherExposurePayload.java`](../src/main/java/io/github/gev414/rotwire/network/WeatherExposurePayload.java)

The forecast payload is sent when a calibrated Overworld transmitter opens the
Survivor Network. It carries availability, today/tomorrow type and window
ordinals, season, and optional forced-condition/expiry fields for an operator
test override. The exposure payload is a deduplicated four-boolean snapshot:
contaminated, storm, exposed, and harmful. Neither lets the client generate
schedules, time grace, or decide damage.

### `CourierChoiceSelectPayload`

Source: [`CourierChoiceSelectPayload.java`](../src/main/java/io/github/gev414/rotwire/network/CourierChoiceSelectPayload.java)

Client-to-server record with delivery UUID string and VarInt option index. The
handler requires `context.player()` to be a `ServerPlayer` and delegates all
authorization/validation to `DeliveryManager.selectChoice`.

## 12. Client presentation

### `ClientModEvents`

Source: [`ClientModEvents.java`](../src/main/java/io/github/gev414/rotwire/client/ClientModEvents.java)

Client-only automatic event subscriber. It:

- attaches fog rendering and logout cleanup at client setup;
- registers `BruteRenderer`;
- registers a small `ThrownItemRenderer` for the Brute rock;
- binds the Camp Hub and Quartermaster Cache menu screens;
- tints the suppressant's base model layer regeneration pink (`0xCD5CAB`);
- renders the survival HUD, radio Horde Watch, weather drawer, and contaminated
  vignette, and clears transient state at logout/screen close.

Its `Dist.CLIENT` restriction prevents dedicated-server classloading failures.

### `CampRadioScreen` and `CampStorageScreen`

Sources: [`CampRadioScreen.java`](../src/main/java/io/github/gev414/rotwire/client/CampRadioScreen.java),
[`CampStorageScreen.java`](../src/main/java/io/github/gev414/rotwire/client/CampStorageScreen.java)

The Camp Hub renders live campsite readiness, module controls, network state,
and Operations Relay telemetry from synchronized menu data. Buttons remain
disabled unless their server-enforced requirements are met. The storage screen
renders the 27 cache slots and player inventory in the same field-terminal
visual language; neither screen makes authorization or gameplay decisions.

### `HordeAtmosphereState`

Source: [`HordeAtmosphereState.java`](../src/main/java/io/github/gev414/rotwire/client/HordeAtmosphereState.java)

Thread-visible transient holder using a volatile immutable `Snapshot`. The
inactive default is no horde, 24,000-tick day, 18,000 start. Snapshot
construction clamps day length to at least one and start time into the day.
Logout resets to default.

### `HordeAtmosphereFog`

Source: [`HordeAtmosphereFog.java`](../src/main/java/io/github/gev414/rotwire/client/HordeAtmosphereFog.java)

Pure math service, intentionally separated for unit testing.

`strength` returns:

- zero on a non-horde day;
- one during an active horde;
- otherwise a smoothstep value during the configured interval before start;
- zero before the interval, at/after start when not active, or with zero fade.

`blendTowardCloserPlane` clamps strength and interpolates only toward the
smaller of current/target distance. This avoids undoing denser fog from another
source.

### `HordeAtmosphereClientEvents`

Source: [`HordeAtmosphereClientEvents.java`](../src/main/java/io/github/gev414/rotwire/client/HordeAtmosphereClientEvents.java)

Consumes render-fog events only when enabled, using terrain fog, no fluid fog,
and an Overworld client level. It computes strength, clamps configured near
against far, blends both planes, cancels the event only when it changed a
distance, and writes the new planes. Logout resets transient state.

### `CourierChoiceClient`

Source: [`CourierChoiceClient.java`](../src/main/java/io/github/gev414/rotwire/client/CourierChoiceClient.java)

Tiny side boundary that converts a received payload into a
`CourierChoiceScreen` through the Minecraft client singleton. Keeping this
indirection prevents the payload record from owning UI construction details.

### `CourierChoiceScreen`

Source: [`CourierChoiceScreen.java`](../src/main/java/io/github/gev414/rotwire/client/CourierChoiceScreen.java)

Non-pausing screen that resolves payload item IDs through the client registry,
lays out up to three columns of 78x48 buttons, renders a dark panel, item icons,
names, and hover tooltips, and sends the clicked zero-based index to the server.

The UI shows a default one-count stack for each item ID. Counts and components
are not transmitted, so enchantments/custom data would not be visible before
choice even though the server would deliver them. Expanding choice manifests to
component-rich stacks should be accompanied by a protocol/UI design change.

### Survival and radio HUD clients

Sources: [`SurvivalStatusClient.java`](../src/main/java/io/github/gev414/rotwire/client/SurvivalStatusClient.java),
[`InventoryEncumbranceClient.java`](../src/main/java/io/github/gev414/rotwire/client/InventoryEncumbranceClient.java),
[`RadioHordeStatusClient.java`](../src/main/java/io/github/gev414/rotwire/client/RadioHordeStatusClient.java),
[`RadioClock.java`](../src/main/java/io/github/gev414/rotwire/client/RadioClock.java)

`SurvivalStatusClient` renders the load tier, quiet/exposed state, and
suspicion bar from the latest server payload. `InventoryEncumbranceClient`
anchors a compact weight glyph, numeric load, and segmented tier bar to the
vanilla player inventory; its hover tooltip lists the live server thresholds
and penalties. It follows recipe-book layout shifts and hides behind the
full-width narrow-screen recipe book. `RadioHordeStatusClient` renders
only while a radio-opened FTB Quests session owns a city-status snapshot. It
uses the existing authoritative horde snapshot and `RadioClock`'s pure
Minecraft-tick-to-24-hour conversion; it does not calculate a countdown.

### Weather presentation clients

Sources: [`WeatherForecastClient.java`](../src/main/java/io/github/gev414/rotwire/client/WeatherForecastClient.java),
[`WeatherExposureClient.java`](../src/main/java/io/github/gev414/rotwire/client/WeatherExposureClient.java)

`WeatherForecastClient` owns the second collapsible right-edge radio tab and
renders current state, transition, today/tomorrow windows, and season. It and
`CityStatusClient` collapse each other so only one drawer occupies the screen.
`WeatherExposureClient` renders code-native layered edge gradients: dirty green
during grace and pulsing red-orange after the server marks exposure harmful.
It displays a seek-shelter message and intentionally has no countdown bar.
The presentation is source-tuned rather than configured: 14 three-pixel layers
give the current inward reach, peak alpha 110 controls strength, and separate
pulse/fade constants control movement and response speed.

## 13. Survival gameplay domain

### Encumbrance services

Sources: [`EncumbranceManager.java`](../src/main/java/io/github/gev414/rotwire/encumbrance/EncumbranceManager.java),
[`TravelersBackpackIntegration.java`](../src/main/java/io/github/gev414/rotwire/encumbrance/TravelersBackpackIntegration.java),
[`EncumbranceMath.java`](../src/main/java/io/github/gev414/rotwire/encumbrance/EncumbranceMath.java),
[`EncumbranceTier.java`](../src/main/java/io/github/gev414/rotwire/encumbrance/EncumbranceTier.java),
[`EncumbranceSnapshot.java`](../src/main/java/io/github/gev414/rotwire/encumbrance/EncumbranceSnapshot.java),
[`ModItemTags.java`](../src/main/java/io/github/gev414/rotwire/encumbrance/ModItemTags.java)

The manager periodically totals inventory, armor, offhand, and optional
Traveler's Backpack state and owns one replaceable transient movement modifier.
The optional integration is class-isolated so its types are never loaded when
the mod is absent. Backpack reads include storage, tools, upgrades, and both
fluid tanks. Stackable categories multiply a per-item value by the actual item
count without a minimum-stack charge or maximum-stack-size scaling. Weightless
and equipment tags override firearm and armor detection; density tags override
block, unstackable, and default classification.

### Awareness and attention services

Sources: [`AwarenessManager.java`](../src/main/java/io/github/gev414/rotwire/stealth/AwarenessManager.java),
[`AwarenessMath.java`](../src/main/java/io/github/gev414/rotwire/stealth/AwarenessMath.java),
[`AttentionManager.java`](../src/main/java/io/github/gev414/rotwire/stealth/AttentionManager.java),
[`PointBlankAttention.java`](../src/main/java/io/github/gev414/rotwire/stealth/PointBlankAttention.java)

Awareness owns transient mob/player suspicion, alert memory, and player
loud-action grace. It suppresses only automatic targets against quiet players,
then promotes them after view-cone/line-of-sight suspicion completes. Horde
capability mobs bypass suppression and Brutes multiply suspicion gain.
Attention submits bounded sound positions as `INVESTIGATE` intents; it never
writes navigation directly. `HUNT` and `ASSAULT` take precedence over those
intents.
PointBlank integration matches the resolved server fire sound to the shooter
and classifies active suppressor attachments.

### Coordinated hostile AI

Sources: [`CoordinatedHostileAi.java`](../src/main/java/io/github/gev414/rotwire/mob/ai/CoordinatedHostileAi.java),
[`CoordinatedHostileGoal.java`](../src/main/java/io/github/gev414/rotwire/mob/ai/CoordinatedHostileGoal.java),
[`ZombieTacticsCompatibility.java`](../src/main/java/io/github/gev414/rotwire/mob/ai/ZombieTacticsCompatibility.java)

The entity-type tag `rotwire:coordinated_hostiles` opts mobs into one
intent/action controller. Intent precedence is `HUNT`, `ASSAULT`,
`INVESTIGATE`, and `IDLE`; actions are `WAIT`, `MOVE`, `ATTACK`, and `BREACH`.
The shared per-level path budget bounds synchronous path creation and reduces
its effective per-tick cap as the active horde grows, while random cooldowns
and bounded route segments avoid synchronized retries and permit 90-100-block
camp approaches. The diagnostic command reports live calculations, deferrals,
and timing. ZombieTactics mining, marker movement, and marker cleanup goals are
removed by class name from opted-in mobs; its global collision-climbing boost
is disabled separately while Rotwire owns movement.

## 14. Item behavior

### `CampModuleItem`, `CampModuleType`, and `CampWorkshopRules`

Sources: [`CampModuleItem.java`](../src/main/java/io/github/gev414/rotwire/item/CampModuleItem.java),
[`CampModuleType.java`](../src/main/java/io/github/gev414/rotwire/camp/CampModuleType.java),
[`CampWorkshopRules.java`](../src/main/java/io/github/gev414/rotwire/camp/CampWorkshopRules.java)

`CampModuleItem` carries one of the three extension types. `CampModuleType`
assigns stable bit positions and strips unknown persisted bits during load.
`CampWorkshopRules` clamps malformed durability input and calculates the
quarter-maximum repair amount as a pure, unit-tested function.

### `InfectionMedicineItem`

Source: [`InfectionMedicineItem.java`](../src/main/java/io/github/gev414/rotwire/item/InfectionMedicineItem.java)

Potion-bottle item parameterized by `Kind`.

Full cure path:

- fails at `use` when the player lacks The Hordes `INFECTED` effect, avoiding
  consumption;
- on server completion, gets the infection capability and calls
  `increaseInfection` to match The Hordes' native future-infection bookkeeping;
- removes the effect and sends `CureEntityMessage` through The Hordes' handler;
- shows translated status to server players.

Suppressant path:

- if infected, rebuilds the effect with the same amplifier/visual flags and a
  duration of `min(current + 6000, 12000)` ticks;
- always adds 6000 ticks of The Hordes immunity;
- shows suppression or immunity status.

Both variants always have enchantment glint, return a stable description ID,
and append two translated tooltip lines. Static duration math is unit-tested.

## 15. Loot integration

### `HandcraftedStorageLoot`

Source: [`HandcraftedStorageLoot.java`](../src/main/java/io/github/gev414/rotwire/loot/HandcraftedStorageLoot.java)

Lazy one-time loot service for an explicit allowlist of nine Handcrafted block
IDs: three cupboards, three drawers, oak shelf, oak nightstand, and oak desk.

It uses block-entity persistent booleans to distinguish player placement and
already-stocked state. World-generated allowlisted containers are filled from
`rotwire:chests/handcrafted_storage` on first interaction with a standard
chest context, interacting player, position, and luck. It also triggers the
vanilla generate-loot criterion for server players.

`tryStock` returns true for any allowlisted storage even when it did not fill
it. The caller uses that return to stop encounter lock processing. Adding a
Handcrafted storage ID requires confirming that its block entity implements
`Container`; otherwise it will be recognized but not filled.

## 16. Weather forecast and exposure domain

### Schedule model and generation

Sources: [`ScheduledWeather.java`](../src/main/java/io/github/gev414/rotwire/weather/ScheduledWeather.java),
[`WeatherSeason.java`](../src/main/java/io/github/gev414/rotwire/weather/WeatherSeason.java),
[`WeatherDayPlan.java`](../src/main/java/io/github/gev414/rotwire/weather/WeatherDayPlan.java),
[`WeatherOverride.java`](../src/main/java/io/github/gev414/rotwire/weather/WeatherOverride.java),
[`WeatherGenerationRules.java`](../src/main/java/io/github/gev414/rotwire/weather/WeatherGenerationRules.java),
[`WeatherScheduleGenerator.java`](../src/main/java/io/github/gev414/rotwire/weather/WeatherScheduleGenerator.java)

The enums define five schedule outcomes and five season modes. A day plan
contains its zero-based day, weather type, and start/end tick and supplies
window/transition helpers plus NBT serialization. The pure generator selects a
deterministic plan from seed, day, season, previous outcome, Horde-day status,
and an immutable config snapshot. It enforces the new-world, consecutive
contamination, Horde-day, seasonal, and daily-window safeguards and is covered
by unit tests. `WeatherOverride` models a saturated, monotonic game-time
deadline and its NBT representation.

### Schedule persistence and runtime authority

Sources: [`WeatherScheduleSavedData.java`](../src/main/java/io/github/gev414/rotwire/weather/WeatherScheduleSavedData.java),
[`WeatherManager.java`](../src/main/java/io/github/gev414/rotwire/weather/WeatherManager.java),
[`WeatherCommands.java`](../src/main/java/io/github/gev414/rotwire/weather/WeatherCommands.java),
[`SereneSeasonsWeather.java`](../src/main/java/io/github/gev414/rotwire/weather/SereneSeasonsWeather.java)

Saved data retains the rolling plans and optional test override under
`rotwire_weather_schedule`.
`WeatherManager` refreshes the Overworld rain/thunder state every five ticks,
sends radio forecasts, exposes ordinary-weather stealth multipliers, and
clears transient state on server stop. The class-isolated optional Serene
Seasons adapter maps its current season into Rotwire's local enum; absence or
API failure falls back to temperate weights. `WeatherCommands` registers the
permission-level-2 force, status, and clear branches and accepts vanilla time
arguments without modifying the underlying day plans.

### `WeatherExposureManager`

Source: [`WeatherExposureManager.java`](../src/main/java/io/github/gev414/rotwire/weather/WeatherExposureManager.java)

Server-only per-player exposure service. Every five ticks it checks current
contamination and whether vanilla precipitation reaches the player. Shelter
clears progress. After the configured rain/storm grace it applies
`rotwire:contaminated_rain` damage at the configured interval and sends only
deduplicated presentation state. Creative, spectator, dead, non-Overworld, and
offline players do not retain exposure.

## 17. Dependency-by-service summary

| Service family | Minecraft/NeoForge | Lost Cities | FTB Quests/Architectury | The Hordes/Atlas | Handcrafted | Rotwire data |
|---|---:|---:|---:|---:|---:|---:|
| Bootstrap/registries/config | yes | initialization | initialization | config registration context | no | metadata |
| Encounters | yes | API metadata | no | no | special container ordering | translations, loot |
| Brute | yes | spawned by encounter | no | no | no | entity loot, texture, damage type |
| Radio block/network | yes | no | opens book | no | no | block assets/recipe/loot |
| Quest submission | yes | no | direct API | no | no | bundled SNBT |
| Courier delivery | yes | via neither | direct reward hook | no | no | manifests/translations |
| Horde atmosphere | yes | no | no | direct API | no | translations not required |
| Stealth/encumbrance | targets, attributes, sound/block/damage events | no | radio screen context | horde capability bypass | no | entity/item tags, HUD translations |
| Weather forecast/hazard | Overworld weather, damage, `SavedData` | no | radio screen context | deterministic horde-day guard | no | damage type, translations |
| Infection medicine | yes | no | no | direct API/packet | no | models, translations, loot |
| Handcrafted storage loot | yes | generated placement context | no | no | block IDs/entities | chest loot table |
