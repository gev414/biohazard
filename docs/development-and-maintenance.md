# Development and maintenance guide

This guide covers the engineering workflow for changing Biohazard safely after
release. It assumes the repository root is the working directory.

## 1. Supported toolchain

| Component | Pinned/current value | Source of truth |
|---|---|---|
| Java toolchain | 21 | `build.gradle` |
| Gradle Wrapper | 9.2.1 | `gradle/wrapper/gradle-wrapper.properties` |
| ModDevGradle | 2.0.141 | `build.gradle` plugins |
| Minecraft | 1.21.1 | `gradle.properties` |
| NeoForge | 21.1.235 | `gradle.properties` |
| Parchment mappings | 2024.11.17 for 1.21.1 | `gradle.properties` |
| JUnit Jupiter | 5.11.4 | `build.gradle` |

Use the checked-in wrapper (`gradlew.bat` on Windows, `./gradlew` on Unix) so
contributors and CI use the same Gradle version. Do not depend on a globally
installed Gradle.

## 2. Build model

Plugins:

- `java-library` compiles Java and exposes standard source/test tasks;
- `maven-publish` defines a local Maven publication;
- `net.neoforged.moddev` creates the NeoForge user-development environment,
  runs, mappings, unit-test integration, and remapping;
- `idea` configures source/Javadoc download.

The archive base name is the mod ID. Project group and version come from
`gradle.properties`. Java compilation is UTF-8 and enables deprecation lint.

### Generated mod metadata

`src/main/templates/META-INF/neoforge.mods.toml` contains placeholders.
`generateModMetadata` expands project/dependency versions into
`build/generated/sources/modMetadata`, which is added to main resources and to
IDE synchronization.

When adding a metadata placeholder:

1. add the property to `gradle.properties`;
2. add it to `replaceProperties` in `build.gradle`;
3. reference it in the template;
4. build and inspect the generated TOML/JAR.

A missing replacement fails processing, which is preferable to shipping an
unexpanded value.

### Resource inputs

Main resources include `src/main/resources` and `src/generated/resources`.
Blockbench `.bbmodel` and data-generator cache files are excluded from final
outputs. Review generated resources before committing; generation does not
make content correct automatically.

## 3. Common tasks

On Windows PowerShell:

```powershell
.\gradlew.bat test
.\gradlew.bat build
.\gradlew.bat runClient
.\gradlew.bat runServer
.\gradlew.bat runGameTestServer
.\gradlew.bat runData
```

Task intent:

| Task | Use |
|---|---|
| `test` | JUnit/NeoForge unit test suite |
| `build` | compile, process resources/metadata, test, and assemble release artifact |
| `runClient` | integrated development client in `run/` |
| `runServer` | dedicated development server with `--nogui` |
| `runGameTestServer` | NeoForge GameTest run; currently useful after GameTests are added |
| `runData` | generate data into `src/generated/resources` using current resources as existing input |
| `publish` | publish configured Maven artifact to local `repo/`; not the public mod release flow by itself |

Run configs use debug logging and the `REGISTRIES` marker. This is useful for
integration diagnosis but noisy; do not interpret all debug output as failure.

## 4. Dependency model and upgrades

### Repositories

- FTB Maven supplies FTB Quests.
- Architectury Maven supplies direct/transitive Architectury artifacts.
- Modrinth Maven supplies pinned mod artifacts through exclusive content.

### Version sources

Human-facing compatibility minimums/ranges and exact development artifact IDs
are both stored in `gradle.properties`. The generated TOML uses compatibility
versions; Gradle development dependencies use exact Maven/Modrinth identifiers.

### Upgrade checklist

For any required mod upgrade:

1. Read its release notes and API/schema changes.
2. Update exact development artifact/version pins.
3. Update generated metadata minimum/range only after compatibility is proven.
4. Refresh IDE dependencies and compile.
5. Search Java imports, registry IDs, recipe IDs, loot items, quest IDs/icons,
   Patchouli references, and foreign-namespace resources for that mod.
6. Run unit tests and a clean build.
7. Launch the full development modpack on client and dedicated server.
8. Exercise the feature-specific acceptance tests below.
9. Open an existing backed-up world and a new world.
10. Update docs, notices, and release notes.

#### Lost Cities

Revalidate IMC API acquisition, chunk/multibuilding methods, six-block floor
assumption, building IDs, schemas, all `lostcities:` overrides, new chunk
generation, and multi-chunk encounter identity.

#### LCMT

Diff every maintained `lcmt:` override and copied/decorated part against the
new upstream version. Validate symbols, slices, building-to-part references,
and all building families in fresh generation. Adjust the bounded metadata
range only after this work.

#### Handcrafted

Validate every palette block/state and every storage allowlist ID. Confirm block
entities still implement `Container` and persistent data remains available.

#### FTB Quests/Architectury

Compile task/reward event APIs, custom task checks, `TeamData` progress methods,
item matching, reward IDs/tags, open-book networking, SNBT schema, and both
client/dedicated server. Respect the current upper bound below 2102 until a
new-major migration is tested.

#### The Hordes/Atlas Lib

This is the most direct compatibility hotspot. Compile and test config values,
saved-data access, per-player event semantics, capability lookup, infection
effects, native infection bookkeeping, cure packet creation/sending, and
command-only horde behavior. Current metadata intentionally excludes 1.7+.

#### PointBlank, Waystones, Tough As Nails, Patchouli

Most coupling is by resource ID and data format, so Java compilation is weak
evidence. Validate recipes, item IDs, loot rolls, quest icons/objectives,
starter items, and all manual pages in-game.

## 5. Test architecture

### Current automated coverage

| Test class | Contract |
|---|---|
| `EncounterSelectionTest` | deterministic keying, inclusive target range, safe invariants, dimension participation |
| `BuildingEncounterTest` | state transitions and serialization of boss state/UUID; safe state |
| `HordeAtmosphereFogTest` | fade timing, active state, and never-expanding fog planes |
| `InfectionMedicineItemTest` | suppressant extension and cap arithmetic |
| `DeliveryCategoryTest` | tag mapping, serialization, fallback behavior |
| `QuestDefaultsResourceTest` | installed default resources and listed manifest resources exist |

CI runs `./gradlew build` on every push and pull request using Temurin JDK 21.

### What unit tests do not cover

Current tests do not prove:

- Lost Cities API resolution against a real generated city;
- collision-safe spawning or event ordering;
- persistence through real world save/restart;
- FTB task/reward hook behavior against a live quest graph;
- inventory insertion/atomic submission in a running server;
- network protocol and choice screen interaction;
- The Hordes capability/packet integration;
- JSON/SNBT semantic validity against all installed mods;
- model, texture, UI, Patchouli, or world-generation appearance.

Use acceptance tests and add focused GameTests/integration tests as the project
grows.

### Recommended next automated tests

High-value additions, in order:

1. Resource scanner that extracts every default reward manifest tag and asserts
   a matching loot table, avoiding a hand-maintained partial list.
2. Serialization round trips for every encounter phase and every delivery kind,
   including malformed-entry containment.
3. Radio submission allocation tests for overlapping item matchers and atomic
   failure.
4. Delivery choice uniqueness, selection, and partial-inventory tests using a
   NeoForge test server.
5. Lost Cities descriptor tests for multi-building root normalization and
   vertical bounds using an adapter/fake interface.
6. GameTests for container locking and spawn containment.
7. Static checks for translation keys emitted by Java and referenced registry
   model/loot companions.

## 6. Change workflows

### Adding a gameplay service

1. Define the owning domain and its server/client authority.
2. Keep framework event adaptation separate from rule logic where practical.
3. Decide whether state is transient, entity/block-owned, player-owned, or
   world-owned.
4. Use `SavedData` or normal entity/block serialization for durable state;
   never rely on a static map as the only copy.
5. Define stable registry IDs, translation keys, payload IDs, config keys, and
   NBT names before release.
6. Wire initialization from `Biohazard` or a side-safe subscriber.
7. Add unit tests around pure rules and serialization.
8. Add resources and acceptance tests.
9. Document the class in `service-reference.md` and the flow in
   `architecture.md`.

### Adding a network payload

1. Choose direction and minimum data; do not send authoritative state the
   receiver can derive safely.
2. Define a namespaced payload ID and bounded codec fields.
3. Register it in `ModPayloads` with the correct direction.
4. Keep client handlers free of server classes and server handlers free of
   client classes.
5. Revalidate every client-provided identifier/index/value server-side.
6. Consider compatibility and whether `PROTOCOL_VERSION` must change.
7. Test dedicated server, multiplayer, stale requests, malformed bounds, and
   disconnect/reconnect.

### Changing persistent NBT

1. Treat existing field names and enum strings as public compatibility API.
2. Add a format version if one does not exist.
3. Load old and new formats; write only the new format.
4. Preserve unknown/malformed-entry containment.
5. Test round trip and a real copy of an older world.
6. Document backup and rollback behavior.
7. Never silently reinterpret a field in a way that loses player progress.

### Changing config

1. Decide server versus client ownership.
2. Choose safe bounds and comments.
3. Document default, unit, read timing, and existing-state effect.
4. Add normalization when harmless operator mistakes can be recovered.
5. Test missing file/default generation and changed values.
6. Update player-facing manual text if behavior is visible.

### Data-only content change

Follow the resource-specific checklist in
[Data and resources](data-and-resources.md#resource-change-checklists). Even a
data-only change requires `build` plus in-game validation when it references
third-party registries or world generation.

## 7. Release procedure

### Before version bump

- Working tree contains only intentional changes.
- `THIRD_PARTY_NOTICES.md` reflects copied/adapted assets or data.
- Required dependency metadata and Gradle pins agree.
- Documentation describes the release behavior.
- Complete build and full-modpack acceptance test pass.
- Test an upgrade from the last released version using backed-up world/config
  data.

### Version and artifact

1. Set `mod_version` in `gradle.properties` using the project's semantic
   versioning policy.
2. Run a clean build with Java 21.
3. Inspect generated `neoforge.mods.toml` inside build output for mod version,
   dependency ranges, license, author, and description.
4. Inspect the produced JAR for required assets, data, quest defaults, and no
   development-only `.bbmodel`/cache files.
5. Run the built JAR in the separate complete Modrinth acceptance instance, not
   only Gradle's source-set run.
6. Tag/release only the exact commit whose artifact passed acceptance.

### Release notes should call out

- player-visible features and balance changes;
- config additions/default changes;
- dependency minimum/range changes;
- world-generation changes affecting new chunks;
- save or quest-default migration behavior;
- known incompatibilities and operator actions;
- whether existing quest directories receive changes automatically (normally
  they do not).

## 8. Known risk areas and design debt

These are not necessarily current bugs; they are places where future changes
deserve extra scrutiny.

### Persistent format versioning

`BuildingEncounter` writes version 1 but does not yet branch while loading.
Courier data has no explicit version. Introduce real migrations before the
first incompatible format change.

### Global static throttles

Encounter and courier interval counters are process-static. Courier resets on
server stop; encounter currently does not. In normal one-server client/server
lifecycles this is usually harmless, but automated multi-server JVM tests or
unusual lifecycle reuse may observe carry-over. Prefer lifecycle-owned state or
explicit reset if such scenarios are introduced.

### Boss-active recovery

`BOSS_ACTIVE` intentionally refuses replacement when the Brute is not loaded,
preventing duplication. If a boss is permanently deleted by commands, another
mod, or corrupted entity data, the encounter can remain locked with no automatic
recovery. A future administrative recovery command should validate saved UUID,
loaded entities, and operator intent.

### Choice payload fidelity

The choice UI receives only item IDs. It cannot preview counts, components,
enchantments, or custom names. Current content is compatible with this
assumption. Rich choice rewards require a bounded component-aware display
protocol without trusting client-returned stacks.

### Choice decode bounds

The server authors at most nine options, but the client decoder reads the
encoded count without an explicit local maximum before allocation. The server
is trusted in the normal model. If protocol hardening becomes a priority, add
compatible bounds.

### Radio scan cost

The block-position scan scales with the cube of range. It is action-driven and
the default is small, but the allowed maximum is expensive. A spatial index or
point-of-interest query would be appropriate if radios become numerous, ranges
grow, or checks move to ticks.

### Handcrafted lock ordering

Allowlisted Handcrafted storage bypasses haunted-building container locks
because stocking is attempted first and returns handled. This is current
behavior, but future design changes should make the desired security/loot order
explicit and test it.

### Foreign-namespace resource forks

`lostcities:`, `lcmt:`, `pointblank:`, and `waystones:` resources are tightly
coupled to upstream IDs/schemas but cannot be validated by Java compilation.
Maintain them as versioned compatibility code and re-diff on upgrades.

### Quest default migration

Preserving non-empty quest directories is safe, but there is no automated merge
or versioned migration for improvements to shipped defaults. Server operators
need an explicit migration process when future releases must update an existing
book.

### Resource contract coverage

The manifest existence test uses a fixed list that does not currently enumerate
every manifest in the directory. Automated extraction from shipped SNBT would
reduce drift.

## 9. Code quality conventions

- Keep public entry points small and name them by domain action.
- Prefer immutable records for messages, descriptors, keys, and snapshots.
- Put invariant-enforcing transitions on aggregates (`BuildingEncounter`,
  `RadioDelivery`) rather than mutating fields from adapters.
- Mark persistence dirty immediately after successful mutation.
- Use resource locations instead of ad-hoc string concatenation at API
  boundaries, while preserving exact serialized/tag protocols.
- Bound retry loops and expensive searches.
- Log actionable integration failures with identifiers and player/building
  context without leaking unnecessary personal data.
- Use translatable components for player-facing text.
- Keep client types under client-only initialization paths.
- Preserve malformed-entry containment in save loaders.
- Add comments for "why" and compatibility constraints, not line-by-line syntax.

## 10. Documentation maintenance checklist

Before merging a feature or maintenance change, ask:

- Did the source tree or class responsibilities change? Update service reference.
- Did an end-to-end flow or ownership boundary change? Update architecture.
- Did a config key/default/read-time change? Update operations.
- Did a resource family, namespace, ID, manifest, or authoring contract change?
  Update data/resource and quest docs.
- Did a build, dependency, test, upgrade, or release step change? Update this
  guide.
- Did player behavior change? Update Patchouli and release notes.
- Did copied/adapted third-party material change? Update notices and license
  attribution.

