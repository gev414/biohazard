# Data and resource reference

Biohazard is intentionally data-heavy. Java owns coordination and stateful
rules; JSON, SNBT, textures, and language files own most content, balance, and
cross-mod resource overrides. This document explains where each resource lives,
who consumes it, and what must change with it.

## 1. Resource-pack versus data-pack sides

Minecraft JAR resources are split into two conceptual trees:

- `assets/<namespace>/...` is client-facing resource-pack content: models,
  textures, blockstates, language, and Patchouli presentation.
- `data/<namespace>/...` is server/data-pack content: loot tables, recipes,
  advancements, tags, damage types, and Lost Cities world-generation data.

`src/main/resources/biohazard/ftbquests_defaults` is neither a standard data
pack nor resource pack. It is private classpath content copied by
`QuestDefaultsInstaller` into an empty FTB Quests config directory.

The namespace is semantically important. A resource under another mod's
namespace overrides or augments the ID that mod looks up. This is deliberate in
the `lostcities`, `lcmt`, `pointblank`, and `waystones` trees.

## 2. Namespace inventory

### Data-pack inventory

| Namespace | Resource kind | File count | Purpose |
|---|---|---:|---|
| `biohazard` | advancement | 1 | starter loadout trigger |
| `biohazard` | damage type | 1 | Brute rock splash definition |
| `biohazard` | loot tables | 30 | blocks, chests, entity, starter loadout, courier manifests |
| `biohazard` | Lost Cities | 46 | Handcrafted palettes and decorated LCMT tower floors |
| `biohazard` | Patchouli book definition | 1 | field manual book-level data |
| `biohazard` | recipe | 1 | Radio Transmitter crafting recipe |
| `lcmt` | Lost Cities | 19 | targeted optional LCMT part/building overrides |
| `lostcities` | Lost Cities | 42 | base Lost Cities palette/variant/condition overrides |
| `minecraft` | tags | 3 | mining/tool and damage behavior integration |
| `pointblank` | recipes | 8 | PointBlank recipe replacements/definitions |
| `waystones` | advancements | 4 | recipe unlock alignment for overridden recipes |
| `waystones` | recipes | 4 | portable travel recipe overrides |

Counts describe the 1.1.0 repository and should be updated when families are
added or removed.

### Client asset inventory

| Namespace | Resource kind | File count | Purpose |
|---|---|---:|---|
| `biohazard` | blockstates | 1 | transmitter facing variants |
| `biohazard` | language | 1 | item, block, entity, message, tooltip, and screen strings |
| `biohazard` | models | 7 | one block and six item models |
| `biohazard` | Patchouli content | 20 | categories, entries, and guide images |
| `biohazard` | textures | 10 | block layers, items, Brute, and guide images |
| `ftbquests` | language/theme | 2 | Survivor Network quest-book presentation |

## 3. Biohazard registry-resource contracts

Every registered object needs a coherent set of resources. Missing one often
produces a purple/black model, untranslated key, incorrect drops, or an object
that cannot be obtained normally.

### Radio Transmitter

| Concern | Resource |
|---|---|
| Registry IDs | `biohazard:radio_transmitter` block, item, block entity |
| Blockstate | `assets/biohazard/blockstates/radio_transmitter.json` |
| Block model | `assets/biohazard/models/block/radio_transmitter.json` |
| Item model | `assets/biohazard/models/item/radio_transmitter.json` |
| Textures | five layered files under `textures/block/radio_transmitter_*.png` |
| Recipe | `data/biohazard/recipe/radio_transmitter.json` |
| Block loot | `data/biohazard/loot_table/blocks/radio_transmitter.json` |
| Tool tags | Minecraft mineable-pickaxe and needs-stone-tool tags |
| Translation | `block.biohazard.radio_transmitter` in `en_us.json` |
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
client tint/tooltip behavior from Java. All require `item.biohazard.<id>` and
the medicine tooltips require `tooltip.biohazard.<id>` plus `.detail`.

### Brute and rock splash

- `assets/biohazard/textures/entity/brute.png` supplies the renderer texture.
- `data/biohazard/loot_table/entities/brute.json` is the entity loot table
  selected through the standard entity ID convention.
- `data/biohazard/damage_type/brute_rock_splash.json` defines the registered
  damage type.
- `data/minecraft/tags/damage_type/no_anger.json` includes the splash damage so
  affected mobs do not acquire anger from the anonymous area hit.
- Entity name and death-message translations live in `en_us.json` as required
  by the damage type's message ID.

The rock itself renders as vanilla cobblestone and has no custom item model.

## 4. Loot-table catalog

All paths below are relative to `data/biohazard/loot_table` and are loaded by
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

The advancement `biohazard:starter_loadout_v1` is the grant trigger. Changing a
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
`biohazard:quest_delivery/<manifest-suffix>`. The suffix in an FTB reward tag
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
src/main/resources/biohazard/ftbquests_defaults/
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
| Task/reward callback behavior | Biohazard Java integration |
| Team progress and quest lifecycle | FTB Quests |
| Physical turn-in inventory mutation | Biohazard `RadioSubmission` |
| Shipment contents | Biohazard loot-table manifests |
| Delivery timer/mailbox | Biohazard saved data |
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

- `data/biohazard/patchouli_books/field_manual/book.json`

English content is located at:

- `assets/biohazard/patchouli_books/field_manual/en_us/`

Categories:

- `quick_start`;
- `survival`;
- `threats`;
- `travel`;
- `guns`;
- `firearms`.

Entries:

- first day;
- base equipment, hydration, and temperature;
- Brute, horde events, and infested buildings;
- portable travel and Waystones;
- using guns;
- Radio Transmitter.

The field manual is player-facing product documentation. Whenever gameplay,
config defaults, recipes, or progression change, check whether the matching
entry now lies. JSON validity alone cannot detect stale advice.

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
  rubble alternatives.

Because these IDs are in `lostcities:`, they override or extend resources by
exact ID. When upgrading Lost Cities, compare upstream copies and schemas. Do
not assume a previously compatible override still matches new palettes or
symbols.

### `data/lcmt/lostcities`

These target optional Lost Cities Modern Tweaks IDs:

- eight building definitions (`building1` through `building8`);
- selected town, cafeteria, factory, shop, center/civic, library, and railway
  part overrides.

If LCMT is absent, Lost Cities has no reason to resolve these IDs. If present,
the mod metadata constrains supported LCMT versions to 2.0.7 through below 2.1.

### `data/biohazard/lostcities`

Biohazard-owned content consists of four Handcrafted palettes and 42 decorated
tower floor parts:

- `handcrafted_cafeteria`;
- `handcrafted_furnishings`;
- `handcrafted_library`;
- `handcrafted_transit`;
- `parts/building1` through `parts/building8`.

The decorated parts preserve LCMT's normal room shells and merge Handcrafted
furniture markers into the slices used by the corresponding building
definitions. Empty, roof, and special parts remain owned by LCMT. This design
keeps the exact generation files editable in source and avoids runtime Java
patching.

#### Lost Cities editing rules

1. Keep every part's character legend synchronized with its slice symbols.
2. Do not reuse a character already meaningful to the copied upstream part.
3. Preserve 16x16 slice width/height exactly.
4. Place furniture only where collision and door/window clearance remain
   usable.
5. Treat foreign-namespace files as maintained forks: record upstream version
   and re-diff on upgrade.
6. Test new chunks. Existing generated chunks will not be retroactively rebuilt.
7. Test both with and without optional LCMT to confirm graceful resource
   selection.
8. Verify every referenced Handcrafted block ID and state exists in the pinned
   Handcrafted version.

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
- changed balance making Biohazard recipes inappropriate;
- quest and loot item IDs no longer resolving.

### Waystones

Four recipe overrides define blank, portal, return, and warp scroll recipes.
Matching recipe-unlock advancements under the Waystones namespace keep the
recipe book behavior aligned.

Recipe and advancement IDs must move together. Test crafting and unlock state
after upgrades because a valid JSON file can still point to a removed item or
changed criterion.

## 9. Minecraft tags

Three tag resources integrate Biohazard with standard mechanics:

| Tag | Purpose |
|---|---|
| `minecraft:mineable/pickaxe` | transmitter is efficiently mined with pickaxes |
| `minecraft:needs_stone_tool` | transmitter drop/tool tier requirement |
| `minecraft:no_anger` damage type | Brute rock splash does not cause anger |

Check each file's `replace` field before editing. A mistaken `replace: true`
under the Minecraft namespace could erase contributions from vanilla or other
mods.

## 10. Localization contracts

`assets/biohazard/lang/en_us.json` contains several classes of key:

- `block.biohazard.*` and `item.biohazard.*` registry display names;
- `entity.biohazard.brute`;
- `tooltip.biohazard.*` and `.detail`;
- `message.biohazard.encounter.*`;
- `message.biohazard.radio.*`;
- `message.biohazard.delivery.*`;
- `message.biohazard.infection.*`;
- `screen.biohazard.courier_choice.*`;
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
- Exact `biohazard_manifest_<filename>` reward tag.
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
