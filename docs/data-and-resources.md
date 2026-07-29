# Data and resource reference

Rotwire is intentionally data-heavy. Java owns coordination and stateful
rules; JSON, SNBT, textures, and language files own most content, balance, and
cross-mod resource overrides. This document explains where each resource lives,
who consumes it, and what must change with it.

## 1. Resource-pack versus data-pack sides

Minecraft JAR resources are split into two conceptual trees:

- `assets/<namespace>/...` is client-facing resource-pack content: models,
  textures, blockstates, language, and Patchouli presentation.
- `data/<namespace>/...` is server/data-pack content: loot tables, recipes,
  advancements, tags, damage types, and Lost Cities world-generation data.

`src/main/resources/rotwire/ftbquests_defaults` is neither a standard data
pack nor resource pack. It is private classpath content copied by
`QuestDefaultsInstaller` into an empty FTB Quests config directory.

The namespace is semantically important. A resource under another mod's
namespace overrides or augments the ID that mod looks up. This is deliberate in
the `lostcities`, `lcmt`, `pointblank`, and `waystones` trees.

## 2. Namespace inventory

### Data-pack inventory

| Namespace | Resource kind | File count | Purpose |
|---|---|---:|---|
| `rotwire` | advancement | 1 | starter loadout trigger |
| `rotwire` | damage type | 2 | Brute rock splash and contaminated-rain definitions |
| `rotwire` | loot tables | 30 | blocks, chests, entity, starter loadout, courier manifests |
| `rotwire` | Lost Cities | 98 | Handcrafted palettes, articulated towers, stair retrofits, custom buildings, and biome-aware overgrown parks |
| `rotwire` | Patchouli book definition | 1 | field manual book-level data |
| `rotwire` | recipe | 1 | Radio Transmitter crafting recipe |
| `rotwire` | tags | 9 | city scaling, stealth targeting, and encumbrance weight overrides |
| `lcmt` | Lost Cities | 31 | targeted optional LCMT part, palette, building, street, and city-style overrides |
| `lostcities` | Lost Cities | 59 | base palettes, variants, conditions, city styles, street greenery, and stair-building overrides |
| `minecraft` | tags | 3 | mining/tool and damage behavior integration |
| `pointblank` | recipes | 8 | PointBlank recipe replacements/definitions |
| `waystones` | advancements | 4 | recipe unlock alignment for overridden recipes |
| `waystones` | recipes | 4 | portable travel recipe overrides |

Counts describe the current repository and should be updated when families are
added or removed.

### Client asset inventory

| Namespace | Resource kind | File count | Purpose |
|---|---|---:|---|
| `rotwire` | blockstates | 1 | transmitter facing variants |
| `rotwire` | language | 1 | item, block, entity, message, tooltip, and screen strings |
| `rotwire` | models | 7 | one block and six item models |
| `rotwire` | Patchouli content | 23 | categories, entries, and guide images |
| `rotwire` | textures | 10 | block layers, items, Brute, and guide images |
| `ftbquests` | language/theme | 2 | Survivor Network quest-book presentation |

## 3. Rotwire registry-resource contracts

Every registered object needs a coherent set of resources. Missing one often
produces a purple/black model, untranslated key, incorrect drops, or an object
that cannot be obtained normally.

### Radio Transmitter

| Concern | Resource |
|---|---|
| Registry IDs | `rotwire:radio_transmitter` block, item, block entity |
| Blockstate | `assets/rotwire/blockstates/radio_transmitter.json` |
| Block model | `assets/rotwire/models/block/radio_transmitter.json` |
| Item model | `assets/rotwire/models/item/radio_transmitter.json` |
| Textures | five layered files under `textures/block/radio_transmitter_*.png` |
| Recipe | `data/rotwire/recipe/radio_transmitter.json` |
| Block loot | `data/rotwire/loot_table/blocks/radio_transmitter.json` |
| Tool tags | Minecraft mineable-pickaxe and needs-stone-tool tags |
| Translation | `block.rotwire.radio_transmitter` in `en_us.json` |
| Manual | `entries/firearms/radio_transmitter.json` plus image |

The Java voxel shapes and JSON model rotations are separate. If the visual
model changes footprint, check all four shapes in `RadioTransmitterBlock`.

### Evidence and medicine items

Item models exist for:

- `documents`;
- `research_data`;
- `encrypted_intel`;
- `infection_cure`;
- `antiviral_suppressant`.

Evidence items use dedicated textures. The suppressant/cure use item models and
client tint/tooltip behavior from Java. All require `item.rotwire.<id>` and
the medicine tooltips require `tooltip.rotwire.<id>` plus `.detail`.

### Brute and rock splash

- `assets/rotwire/textures/entity/brute.png` supplies the renderer texture.
- `data/rotwire/loot_table/entities/brute.json` is the entity loot table
  selected through the standard entity ID convention.
- `data/rotwire/damage_type/brute_rock_splash.json` and
  `data/rotwire/damage_type/contaminated_rain.json` define the registered
  damage types.
- `data/minecraft/tags/damage_type/no_anger.json` includes both damage types so
  affected mobs do not acquire anger from anonymous area or weather damage.
- Entity name and death-message translations live in `en_us.json` as required
  by the damage type's message ID.

The rock itself renders as vanilla cobblestone and has no custom item model.

## 4. Loot-table catalog

All paths below are relative to `data/rotwire/loot_table` and are loaded by
Minecraft's reloadable registry.

### Block and entity loot

| Table | Consumer | Function |
|---|---|---|
| `blocks/radio_transmitter.json` | vanilla block drops | returns the transmitter item when broken under valid conditions |
| `entities/brute.json` | vanilla entity death loot | Brute-specific reward pool |

### Gameplay loot

| Table | Consumer | Function |
|---|---|---|
| `gameplay/starter_loadout_v1.json` | advancement reward | initial curated survival equipment |

The advancement `rotwire:starter_loadout_v1` is the grant trigger. Changing a
starter loadout after a player already completed the advancement does not
automatically re-grant it. Versioning the advancement/table ID is the safest
way to intentionally grant a new one-time loadout.

### World chest and role loot

| Table | Intended content |
|---|---|
| `chests/armory_cache.json` | weapons/ammunition-oriented cache |
| `chests/medical_cache.json` | medicine/survival cache |
| `chests/rare_cache.json` | low-probability valuable guns, attachments, cure, and evidence |
| `chests/records_cache.json` | Documents, Research Data, Encrypted Intel, and records supplies |
| `chests/handcrafted_storage.json` | lazy loot for allowlisted generated Handcrafted containers |
| `chests/builder.json` | builder-role supplies |
| `chests/farmer.json` | farmer-role supplies |
| `chests/food.json` | food supplies |
| `chests/miner.json` | miner-role supplies |

Most world-generation associations are expressed through Lost Cities palettes,
parts, or conditions rather than Java. `handcrafted_storage.json` is the
exception: Java rolls it on first interaction.

### Courier manifests

Courier code addresses these as
`rotwire:quest_delivery/<manifest-suffix>`. The suffix in an FTB reward tag
must match the filename exactly without `.json`.

| Manifest | Intended use |
|---|---|
| `starter_signal_cache` | first-contact/starter shipment |
| `basic_ammunition` | general ammunition shipment |
| `medical_resupply` | standard medical shipment |
| `advanced_medical` | higher-tier medical shipment |
| `shotgun_requisition` | fixed/specific firearm requisition |
| `brute_bounty` | equipment reward for Brute objective |
| `warp_stone_requisition` | Waystones travel reward |
| `rail_setup` | rail/travel setup reward |
| `attachments_random` | random PointBlank attachment |
| `weapons_choice` | broad weapon pool rolled repeatedly into distinct choices |
| `weapons_random` | broad weapon pool rolled once as normal delivery |
| `ward_12gauge_resupply` | 12-gauge ammunition |
| `ward_45acp_resupply` | .45 ACP ammunition |
| `ward_545_resupply` | 5.45 ammunition |
| `ward_556_resupply` | 5.56 ammunition |
| `ward_57_resupply` | 5.7 ammunition |
| `ward_762_resupply` | 7.62 ammunition |
| `ward_9mm_resupply` | 9mm ammunition |

The first eleven are asserted by `QuestDefaultsResourceTest` because the
original default quest set references them. Keep the test list synchronized
with every shipped default manifest; newer Ward manifests should also be added
to that contract if their quests are part of installed defaults.

#### Manifest execution semantics

- The table uses a standard chest loot context.
- Origin and player entity are present; player luck is honored.
- It is rolled when the FTB custom reward is claimed, not at arrival.
- Generated stacks are copied into world saved data.
- A normal manifest may return multiple stacks.
- A choice manifest works best when each roll returns one candidate. If one
  roll returns several candidates, each can become a separate choice.
- Choice uniqueness compares item and components, not count alone.
- Empty output aborts scheduling and produces a player-visible error plus log.

## 5. FTB Quests defaults

Bundled source root:

```text
src/main/resources/rotwire/ftbquests_defaults/
|-- data.snbt
|-- chapter_groups.snbt
|-- chapters/
|   |-- survivor_network.snbt
|   |-- medic.snbt
|   |-- quartermaster.snbt
|   |-- arms_broker.snbt
|   |-- surveyor.snbt
|   |-- builder.snbt
|   `-- backup/arms_broker.snbt   not installed
`-- lang/en_us.snbt
```

`QuestDefaultsInstaller.DEFAULT_FILES` is the exact installation list. The
backup file is source/reference material only and does not ship into the live
quest directory through the installer.

### Ownership split

| Concern | Owner |
|---|---|
| Graph, IDs, dependencies, task/reward tags | SNBT defaults or live FTB config |
| Task/reward callback behavior | Rotwire Java integration |
| Team progress and quest lifecycle | FTB Quests |
| Physical turn-in inventory mutation | Rotwire `RadioSubmission` |
| Shipment contents | Rotwire loot-table manifests |
| Delivery timer/mailbox | Rotwire saved data |
| Quest-facing strings | FTB SNBT language file and/or FTB assets language |

### Default installation behavior

On startup the installer resolves `config/ftbquests/quests`. If that directory
has any entry, nothing is copied or merged. This prevents releases from
overwriting server-authored quests, but it also means changes to packaged SNBT
will not appear in an existing development instance.

Use the detailed workflow in
[Authoring Survivor Network quests](authoring-survivor-network-quests.md), and
the protocol contract in [Radio quests](radio-quests.md).

## 6. Patchouli Survivor's Field Manual

The book-level data is located at:

- `data/rotwire/patchouli_books/field_manual/book.json`

English content is located at:

- `assets/rotwire/patchouli_books/field_manual/en_us/`

Categories:

- `quick_start`;
- `survival`;
- `threats`;
- `travel`;
- `guns`;
- `firearms`.

Entries:

- first day;
- base equipment, hydration, temperature, weather forecasts, contaminated
  precipitation, stealth/load, and sleep;
- Brute, horde events, and infested buildings;
- portable travel and Waystones;
- using guns;
- Radio Transmitter.

The field manual is player-facing product documentation. Whenever gameplay,
config defaults, recipes, or progression change, check whether the matching
entry now lies. JSON validity alone cannot detect stale advice.

The book definition currently declares content version `2`, including the
weather entry. Increment that value for later substantial manual revisions so
Patchouli can recognize that the packaged guide changed.

Guide-specific images are under the book's `images` directory and referenced by
resource location from entry pages. Renaming them requires updating every page
reference.

## 7. Lost Cities world-generation layer

This is the largest resource family and has three namespaces with different
compatibility meanings.

### `data/lostcities/lostcities`

These resources target IDs owned by base Lost Cities:

- `conditions/chestloot.json` controls conditional chest/loot placement;
- palette families define common blocks, brick/desert variants, glass colors
  and forms, an oil rig, rails, and related generation materials;
- variant families cover stone, brick, quartz, blackstone, and deepslate, with
  rubble alternatives;
- building families 1 through 8 override the vanilla definitions with
  Rotwire's furnished, consistently stair-connected floors and roof exits;
- all seven base street shapes retain their road footprint while adding four
  ruined dirt-planter candidates, with a 93.75% tree roll at each planter;
- `citystyle_standard.json` and `citystyle_desert.json` append Rotwire's
  standalone, multibuilding, and biome-appropriate overgrown-park selectors to
  the base style inheritance chain.

Because these IDs are in `lostcities:`, they override or extend resources by
exact ID. When upgrading Lost Cities, compare upstream copies and schemas. Do
not assume a previously compatible override still matches new palettes or
symbols.

### `data/lcmt/lostcities`

These target optional Lost Cities Modern Tweaks IDs:

- eight building definitions (`building1` through `building8`);
- all eight families force five or more floors and select only Rotwire
  stair-connected ground, furnished, and roof parts;
- selected town, cafeteria, factory, shop, center/civic, library, and railway
  part overrides;
- a pinned copy of LCMT's street palette with reserved dirt and sapling
  markers, plus the seven `street_*_base` part variants using those markers;
- pinned copies of LCMT's standard, desert, jungle, and snowy child city styles
  that retain their 2.0.7-specific fields and append Rotwire's building and
  biome-appropriate park selectors.

If LCMT is absent, Lost Cities has no reason to resolve these IDs. If present,
the mod metadata constrains supported LCMT versions to 2.0.7 through below 2.1.

### `data/rotwire/lostcities`

Rotwire-owned content consists of six palettes, 83 building parts, seven
custom building definitions, and two multibuilding definitions:

- `handcrafted_cafeteria`;
- `handcrafted_furnishings`;
- `handcrafted_library`;
- `handcrafted_transit`;
- `custom_buildings`;
- `furnished_facades`, which combines the Handcrafted symbols with fixed
  lore-specific masonry, glass, metal, timber, and balcony-rail materials;
- `parts/building1` through `parts/building8`;
- `parts/stair_retrofits` with dedicated ground floors and enclosed roof exits
  for all eight inherited tower families;
- `parts/custom_buildings` with three standalone tower families and four
  connected hospital quadrants;
- `parts/urban_greenery` with temperate, arid, jungle, and snowy abandoned
  planter parks;
- `buildings/custom_buildings` with forced multi-floor, no-cellar definitions;
- `multibuildings/custom_buildings/quarantine_hospital`, a connected 2x2
  hospital with stair cores in opposite corners;
- `multibuildings/custom_buildings/emergency_block`, a 3x2 city block assembled
  from the standalone towers.

The decorated parts preserve LCMT's normal room shells and merge Handcrafted
furniture markers into the slices used by the corresponding building
definitions. All eight families use a continuous two-wide stair flight with
clear, full-height vestibules, at least three accessible Handcrafted storage
blocks per furnished floor, and no ladder markers. Their definitions override
the same IDs in both `lostcities:` and `lcmt:`, so inherited selectors and
multibuildings resolve the validated stair versions. This design keeps
generation data-driven and avoids runtime Java patching.

Each inherited family also has a dedicated exterior profile. Depending on its
lore role, a floor can use corner cutbacks, one-block recessed window walls,
vertical fins, industrial bays, bunker buttresses, or usable balcony strips.
Ground entrances are cleared through both the outer and recessed façade lines,
and roofs repeat the family's footprint/material language. The custom solo
towers use three additional silhouettes, while hospital shaping is restricted
to true external edges so the four connected chunks remain open internally.

Street and park greenery deliberately uses staged vanilla sapling block states
instead of hard-coded trunks and leaf cubes. Lost Cities schedules those
saplings through its normal post-generation growth path. With Dynamic Trees
installed, its grow-feature event replaces them with the matching dynamic
species; without Dynamic Trees they remain a vanilla-compatible fallback.
Each palette fills Lost Cities' 128-entry random table with 120 tree rolls and
8 empty rolls, so ruined planters remain irregular without becoming scarce.

`LostCitiesOvergrowth` runs after chunks are fully available. It persists a
per-chunk completion attachment and adds up to three mature trees to safe
positions in each new non-building city chunk. Chunks marked by the previous
two-tree pass receive one additional tree once. It prefers existing dirt, can
cut a small dirt tree pit into an unobstructed stone sidewalk, and calls
Dynamic Trees' biome species API when that optional mod is present. The
vanilla sapling-growth fallback keeps the feature functional without Dynamic
Trees. The same service revisits loaded building borders and generates
deterministic, broken vertical vine runners on every exposed face. It reads
`VINE_CHANCE` and the world style's directional vine states directly, closing
the east/south generation-order holes without replacing the profile setting.
Even high profile chances preserve visible facade material, and the
reconciliation pass removes excess vines left by the earlier dense algorithm.
Generated trees and vines remain ordinary world foliage for Serene Seasons and
Immersive Snow to tint or cover.

Custom-building JSON is generated deterministically by
`tools/generate_custom_buildings.py`; edit the generator and rerun it instead of
hand-editing those 31 generated resources. The decorated ladder-family
retrofits and their 16 building overrides are generated by
`tools/generate_stair_retrofits.py`.

#### Lost Cities editing rules

1. Keep every part's character legend synchronized with its slice symbols.
2. Do not reuse a character already meaningful to the copied upstream part.
3. Preserve 16x16 slice width/height exactly.
4. Place furniture only where collision and door/window clearance remain
   usable.
5. Treat foreign-namespace files as maintained forks: record upstream version
   and re-diff on upgrade.
6. Test new chunks for part changes. Existing chunks are not rebuilt, although
   the persisted overgrowth pass can add its one-time tree upgrade and
   reconcile facade vines whenever their borders load.
7. Test both with and without optional LCMT to confirm graceful resource
   selection.
8. Verify every referenced Handcrafted block ID and state exists in the pinned
   Handcrafted version.
9. Keep all active Rotwire tower vertical travel stair-only. Do not add a
   ladder palette entry or the lowercase `l` ladder marker.
10. Keep façade recesses and projections inside the owning 16x16 chunk, and
    never close an internal multibuilding seam.
11. Keep urban tree markers on dirt support, use vanilla `stage=1` saplings,
    fill all 128 palette rolls, and reserve at most 16 rolls for air.
12. Re-diff the pinned LCMT `streets` palette and all seven `street_*_base`
    overrides when upgrading LCMT.
13. Keep the Dynamic Trees dependency optional and preserve the vanilla
    fallback in `LostCitiesOvergrowth`.

## 8. Cross-mod recipe and advancement overrides

### PointBlank

`data/pointblank/recipe` contains eight recipes for processor, printer,
gunmetal intermediates/ingot conversion, and gun internals. These use the
PointBlank namespace so they replace or define IDs consumed as PointBlank
recipes.

Risks when upgrading PointBlank:

- renamed items or recipe serializers;
- changed native recipe IDs causing an old recipe to coexist instead of
  replace;
- changed balance making Rotwire recipes inappropriate;
- quest and loot item IDs no longer resolving.

### Waystones

Four recipe overrides define blank, portal, return, and warp scroll recipes.
Matching recipe-unlock advancements under the Waystones namespace keep the
recipe book behavior aligned.

Recipe and advancement IDs must move together. Test crafting and unlock state
after upgrades because a valid JSON file can still point to a removed item or
changed criterion.

## 9. Tags

Three Minecraft-namespace tags integrate Rotwire with standard mechanics:

| Tag | Purpose |
|---|---|
| `minecraft:mineable/pickaxe` | transmitter is efficiently mined with pickaxes |
| `minecraft:needs_stone_tool` | transmitter drop/tool tier requirement |
| `minecraft:no_anger` damage type | Brute rock splash and contaminated rain do not cause anger |

Check each file's `replace` field before editing. A mistaken `replace: true`
under the Minecraft namespace could erase contributions from vanilla or other
mods.

Nine Rotwire-namespace tags expose gameplay classification:

| Tag | Registry | Purpose |
|---|---|---|
| `rotwire:city_scaled_infected` | entity type | entities eligible for persistent city-danger health scaling |
| `rotwire:stealth_affected_infected` | entity type | infected governed by quiet-target suppression, suspicion, and bounded investigation |
| `rotwire:encumbrance/weightless` | item | forces a carried stack to contribute no weight |
| `rotwire:encumbrance/tiny` | item | applies the tiny per-item weight |
| `rotwire:encumbrance/light` | item | applies the light per-item weight |
| `rotwire:encumbrance/dense` | item | applies the dense per-item weight |
| `rotwire:encumbrance/very_dense` | item | applies the very-dense per-item weight |
| `rotwire:encumbrance/equipment/light` | item | applies the flat light-equipment weight per item |
| `rotwire:encumbrance/equipment/heavy` | item | applies the flat heavy-equipment weight per item |

Weightless and equipment tags take precedence over firearm and armor detection.
Density tags then take precedence over block, unstackable, and default
automatic classification. Keep the entity tags distinct: an entity may
participate in city scaling, stealth awareness, both, or neither.

## 10. Localization contracts

`assets/rotwire/lang/en_us.json` contains several classes of key:

- `block.rotwire.*` and `item.rotwire.*` registry display names;
- `entity.rotwire.brute`;
- `tooltip.rotwire.*` and `.detail`;
- `message.rotwire.encounter.*`;
- `message.rotwire.radio.*`;
- `message.rotwire.delivery.*`;
- `message.rotwire.infection.*`;
- `screen.rotwire.courier_choice.*`;
- `screen.rotwire.radio_horde.*`;
- `hud.rotwire.encumbrance.*` and `hud.rotwire.stealth.*`;
- `screen.rotwire.weather.*` and `hud.rotwire.weather.*`;
- `tooltip.rotwire.encumbrance.*`;
- damage/death-message keys required by the damage type.

Java should emit translation keys and parameters, not assembled English
sentences. When adding a key, test formatting with all arguments and plural-like
counts. Minecraft translations do not provide automatic grammatical plural
rules.

FTB Quests has its own `assets/ftbquests/lang/en_us.json`, theme file, and SNBT
language data. Search both locations before assuming a quest string is unused.

## 11. Resource change checklists

### New registered block

- Java deferred registration and, if stateful, block entity type.
- Block item registration.
- Blockstate and model(s).
- Texture(s).
- Block loot table.
- Mining/tool tags.
- Recipe or other acquisition path.
- Translation and creative-tab entry.
- Patchouli/quest/loot integration if player-facing.
- Dedicated-server and client in-game test.

### New item

- Java registration and behavior.
- Item model and texture/tint.
- Translation and tooltips.
- Creative tab and acquisition path.
- Loot/quest/manual references.
- Stack size, rarity, and recipe balance test.

### New courier manifest

- JSON under `loot_table/quest_delivery`.
- Exact `rotwire_manifest_<filename>` reward tag.
- Category and delivery-kind tags.
- Resource-contract test update when used by shipped defaults.
- In-game roll test with all required mods present.
- Restart, full-inventory, and choice validation where applicable.

### New Lost Cities part/palette

- Correct namespace and schema for the pinned integration version.
- Unique resource ID and valid symbol mappings.
- All block IDs/states available.
- Referenced by a building/part/palette that generation actually selects.
- Tested in freshly generated chunks with relevant optional mods.
- Third-party notice updated when copied/adapted upstream content is introduced.

## 12. Data validation strategy

`./gradlew build` validates compilation and current unit/resource tests but does
not prove all cross-mod JSON IDs or world-generation layouts are correct. Use a
three-level validation model:

1. **Static:** parse JSON/SNBT where tooling supports it; search references;
   ensure expected companion files exist.
2. **Automated:** add JUnit resource-contract tests for lists that must remain in
   sync, such as installed defaults and courier manifests.
3. **Acceptance:** launch the complete pinned modpack, inspect logs after data
   reload, generate fresh Lost Cities chunks, use recipes, open Patchouli/FTB
   content, and exercise reward/loot paths.
