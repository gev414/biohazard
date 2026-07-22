# Service and class reference

This is the implementation catalog for all production Java classes. The word
"service" is used broadly: managers, adapters, registries, state models,
entities, client controllers, and payload records are all documented because
each is a maintenance boundary.

For cross-system sequences and authority rules, read
[Architecture](architecture.md). Paths below are relative to the repository.

## 1. Composition root

### `Biohazard`

Source: [`Biohazard.java`](../src/main/java/io/github/gev414/biohazard/Biohazard.java)

**Role.** NeoForge entry point and dependency-composition root. It should wire
services together but not contain gameplay rules.

**Owns.** `MOD_ID`, the shared SLF4J logger, and initialization order.

**Direct collaborators.** All deferred registry holders, the three config
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

Source: [`ModBlocks.java`](../src/main/java/io/github/gev414/biohazard/block/ModBlocks.java)

Registers `biohazard:radio_transmitter` as a `RadioTransmitterBlock`. Its
material behavior is brown map color, hardness 3, blast resistance 6, metal
sound, non-occluding shape, and correct-tool-required drops. The matching item,
block entity, model, blockstate, recipe, loot table, tags, translations, and
texture are separate contracts.

Depends on: `Biohazard.MOD_ID`, `RadioTransmitterBlock`, NeoForge registries.

### `ModBlockEntities`

Source: [`ModBlockEntities.java`](../src/main/java/io/github/gev414/biohazard/block/entity/ModBlockEntities.java)

Registers block entity type `biohazard:radio_transmitter`, constructed by
`RadioTransmitterBlockEntity::new` and valid only for the registered transmitter
block.

Depends on: `ModBlocks.RADIO_TRANSMITTER`, `RadioTransmitterBlockEntity`.

### `ModItems`

Source: [`ModItems.java`](../src/main/java/io/github/gev414/biohazard/item/ModItems.java)

Registers:

| Registry id | Java type | Stack size | Purpose |
|---|---|---:|---|
| `biohazard:radio_transmitter` | `BlockItem` | default | Places the radio block |
| `biohazard:documents` | `Item` | 64 | Quest evidence/currency |
| `biohazard:research_data` | `Item` | 32 | Quest evidence/currency |
| `biohazard:encrypted_intel` | `Item` | 16 | High-tier quest evidence |
| `biohazard:infection_cure` | `InfectionMedicineItem(FULL_CURE)` | 4 | Removes The Hordes infection; epic rarity |
| `biohazard:antiviral_suppressant` | `InfectionMedicineItem(SUPPRESSANT)` | 8 | Delays infection/grants immunity; rare rarity |

Resource models, textures, translations, creative tabs, loot, and recipes refer
to these stable IDs.

### `ModEntities`

Source: [`ModEntities.java`](../src/main/java/io/github/gev414/biohazard/entity/ModEntities.java)

Registers:

- `biohazard:brute`, a monster-sized `BruteEntity` using the class's dimensions
  and eye height, with client tracking range 8;
- `biohazard:brute_rock`, a small miscellaneous projectile updated every 10
  ticks with client tracking range 4.

Attributes and renderers are not part of registry creation; they are registered
by `ModEntityEvents` and `ClientModEvents` respectively.

### `ModDamageTypes`

Source: [`ModDamageTypes.java`](../src/main/java/io/github/gev414/biohazard/damage/ModDamageTypes.java)

Declares the resource key `biohazard:brute_rock_splash`. The actual damage type
is data-backed by `data/biohazard/damage_type/brute_rock_splash.json` and is
added to Minecraft's `no_anger` damage-type tag. The projectile resolves the
holder from the active registry at impact time.

## 3. Configuration services

Each config builds its specification once through an idempotent `initialize()`
call. Detailed defaults and operational implications are in
[Configuration and operations](configuration-and-operations.md).

### `EncounterConfig`

Source: [`EncounterConfig.java`](../src/main/java/io/github/gev414/biohazard/config/EncounterConfig.java)

Defines the server-side `biohazard-encounters.toml` contract: master switch,
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

Source: [`HordeAtmosphereConfig.java`](../src/main/java/io/github/gev414/biohazard/config/HordeAtmosphereConfig.java)

Defines client-side `biohazard-client.toml`: enabled flag, pre-event fade
duration, and target near/far fog planes. It affects only presentation and is
consumed by `HordeAtmosphereClientEvents`.

### `RadioQuestConfig`

Source: [`RadioQuestConfig.java`](../src/main/java/io/github/gev414/biohazard/config/RadioQuestConfig.java)

Defines server-side `biohazard-radio-quests.toml`: transmitter range,
calibration duration, and per-category courier delays. It is consumed by radio
block entities, radio proximity search, and `DeliveryCategory.delayTicks()`.

## 4. Event adapters

Event adapters translate framework events into domain calls. They own event
filtering and priority choices, not durable state.

### `EncounterEvents`

Source: [`EncounterEvents.java`](../src/main/java/io/github/gev414/biohazard/event/EncounterEvents.java)

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

Source: [`HordeAtmosphereSyncEvents.java`](../src/main/java/io/github/gev414/biohazard/event/HordeAtmosphereSyncEvents.java)

Server adapter for The Hordes state. Once per second it derives a compact
`HordeAtmospherePayload` per online player. A last-sent map suppresses identical
packets. Login forces an initial payload; logout removes that player's cache;
server stop clears all transient state.

Direct dependencies: The Hordes `HordeEventConfig`, `HordeSavedData`, and
per-player `HordeEvent`; NeoForge `PacketDistributor`.

No state from this class is durable or authoritative. Its cache exists only to
reduce network traffic.

### `ModCreativeTabEvents`

Source: [`ModCreativeTabEvents.java`](../src/main/java/io/github/gev414/biohazard/event/ModCreativeTabEvents.java)

Populates vanilla creative tabs:

- functional blocks: Radio Transmitter;
- ingredients: Documents, Research Data, Encrypted Intel;
- food and drinks: Antiviral Suppressant and Infection Cure.

Adding a registered item does not automatically make it discoverable in
creative mode; update this adapter where appropriate.

### `ModEntityEvents`

Source: [`ModEntityEvents.java`](../src/main/java/io/github/gev414/biohazard/event/ModEntityEvents.java)

Binds the Brute entity type to the attributes produced by
`BruteEntity.createAttributes()`. Missing this binding normally produces an
entity registration/runtime failure.

## 5. Lost Cities adapter

### `LostCitiesIntegration`

Source: [`LostCitiesIntegration.java`](../src/main/java/io/github/gev414/biohazard/lostcities/LostCitiesIntegration.java)

Requests `ILostCities` through Lost Cities inter-mod communication during
common setup. The nested `ApiReceiver` stores the supplied API in a nullable
static field. `api()` can therefore return null before the handshake or if the
integration is unavailable; callers must degrade safely.

This class isolates API acquisition from encounter code. If Lost Cities changes
its handshake, only this adapter should need structural changes.

### `LostCitiesBuildingResolver`

Source: [`LostCitiesBuildingResolver.java`](../src/main/java/io/github/gev414/biohazard/lostcities/LostCitiesBuildingResolver.java)

Converts Lost Cities chunk metadata into Biohazard's stable
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

Source: [`BuildingKey.java`](../src/main/java/io/github/gev414/biohazard/encounter/BuildingKey.java)

Immutable identity for one physical building: dimension ID and root chunk X/Z.
It provides direct NBT save/load helpers. Multi-building normalization happens
before construction, in the resolver.

Used as: saved-data map key, deterministic selection seed input, and entity
marker identity.

Compatibility contract: NBT keys `dimension`, `rootChunkX`, `rootChunkZ`.

### `BuildingDescriptor`

Source: [`BuildingDescriptor.java`](../src/main/java/io/github/gev414/biohazard/encounter/BuildingDescriptor.java)

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

Source: [`EncounterSelection.java`](../src/main/java/io/github/gev414/biohazard/encounter/EncounterSelection.java)

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

Source: [`EncounterPhase.java`](../src/main/java/io/github/gev414/biohazard/encounter/EncounterPhase.java)

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

Source: [`BuildingEncounter.java`](../src/main/java/io/github/gev414/biohazard/encounter/BuildingEncounter.java)

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

Source: [`EncounterSavedData.java`](../src/main/java/io/github/gev414/biohazard/encounter/EncounterSavedData.java)

Server-wide persistence repository backed by Overworld `DataStorage`, file key
`biohazard_building_encounters`. It stores a linked map from `BuildingKey` to
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

Source: [`EncounterEntityData.java`](../src/main/java/io/github/gev414/biohazard/encounter/EncounterEntityData.java)

Utility for attaching/reading a `biohazardEncounter` compound in an entity's
persistent data. The compound embeds `BuildingKey` fields plus lower-case role.
`read` converts malformed or unknown role data to `Optional.empty()`.

`Role.REGULAR` deaths increment regular progress. `Role.BOSS` deaths clear the
encounter. `matches` is used to count/find loaded entities belonging to a
specific building and role.

### `EncounterManager`

Source: [`EncounterManager.java`](../src/main/java/io/github/gev414/biohazard/encounter/EncounterManager.java)

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

Source: [`EncounterSpawner.java`](../src/main/java/io/github/gev414/biohazard/encounter/EncounterSpawner.java)

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

Source: [`BruteEntity.java`](../src/main/java/io/github/gev414/biohazard/entity/BruteEntity.java)

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

Source: [`RockThrowAttackGoal.java`](../src/main/java/io/github/gev414/biohazard/entity/ai/RockThrowAttackGoal.java)

Generic ranged goal for a type that is both `Mob` and `RangedAttackMob`.
Constructor validation rejects invalid ranges/timings. It claims MOVE and LOOK,
requires a living visible target within the inclusive range band, stops
navigation during windup, tracks the target every tick, attacks when windup
reaches zero, and sets an absolute game-time cooldown.

If target identity, line of sight, life, or range becomes invalid during
windup, the goal stops without attacking. It uses squared distance until the
final normalized distance factor.

### `BruteRockProjectile`

Source: [`BruteRockProjectile.java`](../src/main/java/io/github/gev414/biohazard/entity/projectile/BruteRockProjectile.java)

Throwable item projectile rendered as cobblestone.

On first server-side hit it:

1. offsets block impact positions 0.01 blocks outward to avoid buried effects;
2. plays stone-break sound and sends 12 cobblestone block particles;
3. deals 6 thrown direct damage to a non-owner direct target;
4. applies 0.6 extra knockback in flight direction if direct damage succeeds;
5. searches a two-block-radius AABB for other living targets;
6. excludes owner/direct target, checks bounding-box distance and line of
   exposure with `Explosion.getSeenPercent`;
7. deals 4 `biohazard:brute_rock_splash` damage;
8. discards itself.

Splash damage has an impact position but no causing/direct entity in the
constructed `DamageSource`. That affects attribution, armor/enchantment rules,
and anger behavior; the damage type is also tagged `no_anger`.

### `BruteRenderer`

Source: [`BruteRenderer.java`](../src/main/java/io/github/gev414/biohazard/client/renderer/BruteRenderer.java)

Client-only renderer extending vanilla `ZombieRenderer`. It uses
`textures/entity/brute.png`, multiplies shadow radius by the Brute scale, and
scales the pose stack uniformly by 1.5. Entity hitbox dimensions are already
scaled in the registered entity type; renderer scale controls visual size.

## 8. Radio block and network proximity

### `RadioTransmitterBlock`

Source: [`RadioTransmitterBlock.java`](../src/main/java/io/github/gev414/biohazard/block/RadioTransmitterBlock.java)

Horizontal, non-full-cube block with four direction-specific voxel shapes and a
block entity. Placement faces the player and begins calibration server-side.
Rotation and mirror operations preserve correct facing.

Server interaction order:

1. If uncalibrated, show rounded-up seconds remaining and stop.
2. Collect ready non-choice deliveries.
3. If a ready choice delivery exists, send/open its screen and stop.
4. Report remaining mailbox status.
5. Use Architectury/FTB's `OpenQuestBookMessage` to open the standard quest
   book.

The client returns success immediately to provide normal interaction feedback;
all meaningful work remains on the server.

Direct dependencies: FTB Quests and Architectury networking, `RadioNetwork`,
`DeliveryManager`, transmitter block entity.

### `RadioTransmitterBlockEntity`

Source: [`RadioTransmitterBlockEntity.java`](../src/main/java/io/github/gev414/biohazard/block/entity/RadioTransmitterBlockEntity.java)

Persists one absolute `ready_at` game time. `beginCalibration` applies the
current server config and marks the block entity dirty. `onLoad` initializes
legacy/missing state. `isConnected` and `ticksUntilConnected` compare against
the level's current game time.

Because the absolute deadline is saved, changing `calibrationTicks` does not
retroactively change a transmitter that already has `ready_at`.

### `RadioNetwork`

Source: [`RadioNetwork.java`](../src/main/java/io/github/gev414/biohazard/quest/RadioNetwork.java)

Server proximity service. `findConnectedTransmitter` scans all block positions
in the configured cube around the player's block position, rejects points
outside radius `range + 0.5`, skips unloaded positions, requires the registered
block, then requires a connected block entity. The first match is returned;
there is no nearest-radio sorting because callers need only proof of connection.

Complexity grows cubically with configured range. The allowed maximum of 32 can
mean scanning up to 65 cubed positions per button press. Keep this in mind if
increasing the upper bound or calling it from a tick loop.

`calibrationSecondsRemaining` rounds ticks upward to seconds.

## 9. FTB Quests integration

### `QuestDefaultsInstaller`

Source: [`QuestDefaultsInstaller.java`](../src/main/java/io/github/gev414/biohazard/quest/QuestDefaultsInstaller.java)

Installs nine packaged files from `/biohazard/ftbquests_defaults/` into
`config/ftbquests/quests` during mod construction. If the destination directory
exists and has any entry, it logs and preserves it. An absent or empty directory
receives the complete default set, creating parents and replacing individual
targets during that initial operation.

The explicit `DEFAULT_FILES` list is a release contract and is mirrored by
`QuestDefaultsResourceTest`. Adding a default chapter requires updating both.
The backup chapter present under resources is not installed.

### `FTBQuestsIntegration`

Source: [`FTBQuestsIntegration.java`](../src/main/java/io/github/gev414/biohazard/quest/FTBQuestsIntegration.java)

Adapter to FTB Quests' Architectury events. Initialization is idempotent.

Task protocol:

- server-side custom task with `biohazard_radio_accept`: configure a one-point,
  button-enabled task whose callback requires a connected radio and completes
  it;
- server-side custom task with `biohazard_radio_complete`: configure the same
  button mechanics, then delegate atomic turn-in to `RadioSubmission`.

Reward protocol:

- `biohazard_radio_delivery`: schedule generated items;
- `biohazard_radio_choice_delivery`: schedule distinct options;
- `biohazard_manifest_<name>`: required manifest suffix;
- `biohazard_category_<name>`: optional, defaults to supplies;
- `biohazard_choice_count_<1..9>`: optional, defaults to 3.

If both delivery tags are present, choice behavior wins. Missing manifest shows
an error but the event still returns pass; FTB Quests' reward lifecycle remains
its own responsibility. Full authoring behavior is documented in
[radio-quests.md](radio-quests.md).

### `RadioSubmission`

Source: [`RadioSubmission.java`](../src/main/java/io/github/gev414/biohazard/quest/RadioSubmission.java)

Package-private atomic turn-in service. It first verifies every quest task
except the final completion task, tagged submission item tasks, and tasks FTB
considers optional for progression. Any incomplete required objective aborts.

For each `ItemTask` tagged `biohazard_radio_submit`, it uses `task.test(stack)`
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

Source: [`DeliveryCategory.java`](../src/main/java/io/github/gev414/biohazard/quest/delivery/DeliveryCategory.java)

Enum of `SUPPLIES`, `AMMUNITION`, `MEDICAL`, `EQUIPMENT`, and `FIREARM`.
It maps reward tags to a category, category to configured seconds, seconds to
ticks with exact multiplication, and enum values to lower-case serialized
names. Missing or future unknown names safely fall back to supplies.

Adding a category affects configuration, serialization, translations, quest
authoring, tests, and potentially existing saves.

### `DeliveryKind`

Source: [`DeliveryKind.java`](../src/main/java/io/github/gev414/biohazard/quest/delivery/DeliveryKind.java)

Package-private enum distinguishing normal `ITEMS` from unresolved `CHOICE`.
Unknown serialized values fall back to items, prioritizing recoverable
collection over an unusable screen.

### `RadioDelivery`

Source: [`RadioDelivery.java`](../src/main/java/io/github/gev414/biohazard/quest/delivery/RadioDelivery.java)

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

Source: [`DeliverySavedData.java`](../src/main/java/io/github/gev414/biohazard/quest/delivery/DeliverySavedData.java)

Package-private repository in Overworld data storage under
`biohazard_radio_deliveries`. It exposes the mutable delivery list only within
the package and marks itself dirty when adding. Managers must mark dirty after
mutating/removing existing records.

Loading skips malformed records with an error log and skips records whose item
list is empty. There is no explicit file-format version or custom migration.

### `DeliveryManager`

Source: [`DeliveryManager.java`](../src/main/java/io/github/gev414/biohazard/quest/delivery/DeliveryManager.java)

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

**Collection.** Iterates ready, owned, non-choice deliveries. Inventory insertion
mutates a copied stack; leftover amounts replace persisted contents. Fully
inserted deliveries are removed. The player is told how many whole delivery
records were collected, not how many stacks.

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

Source: [`ModPayloads.java`](../src/main/java/io/github/gev414/biohazard/network/ModPayloads.java)

Registers protocol version string `1` and three play-phase payloads:

| Payload ID | Direction | Handler |
|---|---|---|
| `biohazard:horde_atmosphere` | server to client | update transient fog state |
| `biohazard:courier_choice_open` | server to client | open choice screen |
| `biohazard:courier_choice_select` | client to server | validate and apply choice |

Changing field encoding or compatibility may require a protocol version change.
Keep handlers side-safe.

### `HordeAtmospherePayload`

Source: [`HordeAtmospherePayload.java`](../src/main/java/io/github/gev414/biohazard/network/HordeAtmospherePayload.java)

Record containing two booleans and two variable-length integers. Its handler
replaces `HordeAtmosphereState`. Day length and start time are normalized again
inside the client snapshot, providing defense against invalid values.

### `CourierChoiceOpenPayload`

Source: [`CourierChoiceOpenPayload.java`](../src/main/java/io/github/gev414/biohazard/network/CourierChoiceOpenPayload.java)

Server-to-client record with a 36-character delivery UUID string and a list of
item ID strings, each capped at 256 characters. The count is a VarInt. Current
server generation caps authored choice count at nine, but the decoder itself
does not impose a list-count bound; protocol evolution should add compatible
defensive bounds if input trust requirements change.

Handler delegates to `CourierChoiceClient`.

### `CourierChoiceSelectPayload`

Source: [`CourierChoiceSelectPayload.java`](../src/main/java/io/github/gev414/biohazard/network/CourierChoiceSelectPayload.java)

Client-to-server record with delivery UUID string and VarInt option index. The
handler requires `context.player()` to be a `ServerPlayer` and delegates all
authorization/validation to `DeliveryManager.selectChoice`.

## 12. Client presentation

### `ClientModEvents`

Source: [`ClientModEvents.java`](../src/main/java/io/github/gev414/biohazard/client/ClientModEvents.java)

Client-only automatic event subscriber. It:

- attaches fog rendering and logout cleanup at client setup;
- registers `BruteRenderer`;
- registers a small `ThrownItemRenderer` for the Brute rock;
- tints the suppressant's base model layer regeneration pink (`0xCD5CAB`).

Its `Dist.CLIENT` restriction prevents dedicated-server classloading failures.

### `HordeAtmosphereState`

Source: [`HordeAtmosphereState.java`](../src/main/java/io/github/gev414/biohazard/client/HordeAtmosphereState.java)

Thread-visible transient holder using a volatile immutable `Snapshot`. The
inactive default is no horde, 24,000-tick day, 18,000 start. Snapshot
construction clamps day length to at least one and start time into the day.
Logout resets to default.

### `HordeAtmosphereFog`

Source: [`HordeAtmosphereFog.java`](../src/main/java/io/github/gev414/biohazard/client/HordeAtmosphereFog.java)

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

Source: [`HordeAtmosphereClientEvents.java`](../src/main/java/io/github/gev414/biohazard/client/HordeAtmosphereClientEvents.java)

Consumes render-fog events only when enabled, using terrain fog, no fluid fog,
and an Overworld client level. It computes strength, clamps configured near
against far, blends both planes, cancels the event only when it changed a
distance, and writes the new planes. Logout resets transient state.

### `CourierChoiceClient`

Source: [`CourierChoiceClient.java`](../src/main/java/io/github/gev414/biohazard/client/CourierChoiceClient.java)

Tiny side boundary that converts a received payload into a
`CourierChoiceScreen` through the Minecraft client singleton. Keeping this
indirection prevents the payload record from owning UI construction details.

### `CourierChoiceScreen`

Source: [`CourierChoiceScreen.java`](../src/main/java/io/github/gev414/biohazard/client/CourierChoiceScreen.java)

Non-pausing screen that resolves payload item IDs through the client registry,
lays out up to three columns of 78x48 buttons, renders a dark panel, item icons,
names, and hover tooltips, and sends the clicked zero-based index to the server.

The UI shows a default one-count stack for each item ID. Counts and components
are not transmitted, so enchantments/custom data would not be visible before
choice even though the server would deliver them. Expanding choice manifests to
component-rich stacks should be accompanied by a protocol/UI design change.

## 13. Item behavior

### `InfectionMedicineItem`

Source: [`InfectionMedicineItem.java`](../src/main/java/io/github/gev414/biohazard/item/InfectionMedicineItem.java)

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

## 14. Loot integration

### `HandcraftedStorageLoot`

Source: [`HandcraftedStorageLoot.java`](../src/main/java/io/github/gev414/biohazard/loot/HandcraftedStorageLoot.java)

Lazy one-time loot service for an explicit allowlist of nine Handcrafted block
IDs: three cupboards, three drawers, oak shelf, oak nightstand, and oak desk.

It uses block-entity persistent booleans to distinguish player placement and
already-stocked state. World-generated allowlisted containers are filled from
`biohazard:chests/handcrafted_storage` on first interaction with a standard
chest context, interacting player, position, and luck. It also triggers the
vanilla generate-loot criterion for server players.

`tryStock` returns true for any allowlisted storage even when it did not fill
it. The caller uses that return to stop encounter lock processing. Adding a
Handcrafted storage ID requires confirming that its block entity implements
`Container`; otherwise it will be recognized but not filled.

## 15. Dependency-by-service summary

| Service family | Minecraft/NeoForge | Lost Cities | FTB Quests/Architectury | The Hordes/Atlas | Handcrafted | Biohazard data |
|---|---:|---:|---:|---:|---:|---:|
| Bootstrap/registries/config | yes | initialization | initialization | config registration context | no | metadata |
| Encounters | yes | API metadata | no | no | special container ordering | translations, loot |
| Brute | yes | spawned by encounter | no | no | no | entity loot, texture, damage type |
| Radio block/network | yes | no | opens book | no | no | block assets/recipe/loot |
| Quest submission | yes | no | direct API | no | no | bundled SNBT |
| Courier delivery | yes | via neither | direct reward hook | no | no | manifests/translations |
| Horde atmosphere | yes | no | no | direct API | no | translations not required |
| Infection medicine | yes | no | no | direct API/packet | no | models, translations, loot |
| Handcrafted storage loot | yes | generated placement context | no | no | block IDs/entities | chest loot table |
