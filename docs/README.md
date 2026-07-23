# Biohazard engineering documentation

This directory is the technical handbook for maintaining Biohazard. It
documents the implementation that is present in this
repository, not an aspirational architecture. When code and documentation
disagree, the code is authoritative and the documentation should be corrected
in the same change.

Documented baseline: Biohazard `1.1.3`.

Biohazard is a Minecraft 1.21.1 NeoForge integration mod. Its main job is to
turn a curated collection of third-party mods and data resources into one
cohesive zombie-survival experience. The project contains conventional Java
gameplay code, NeoForge event wiring, persistent world state, network payloads,
client-only presentation, FTB Quests hooks, and a large data-driven content
layer.

## Reading paths

Choose the path that matches the work you are doing:

| Goal | Start here | Continue with |
|---|---|---|
| Understand the whole mod | [Architecture](architecture.md) | [Service reference](service-reference.md) |
| Find the owner of a behavior | [Service reference](service-reference.md) | [Architecture](architecture.md#runtime-flows) |
| Change config or operate a server | [Configuration and operations](configuration-and-operations.md) | [Persistence](architecture.md#state-ownership-and-persistence) |
| Add or balance content | [Data and resources](data-and-resources.md) | [Quest authoring](authoring-survivor-network-quests.md) |
| Add a radio contract | [Quest authoring](authoring-survivor-network-quests.md) | [Radio quest protocol](radio-quests.md) |
| Understand or tune city progression | [City operations](city-operations.md) | [Configuration and operations](configuration-and-operations.md#4-city-operations-config) |
| Tune stealth, noise, or carried weight | [Stealth and encumbrance](stealth-attention-and-encumbrance.md) | [Configuration and operations](configuration-and-operations.md#5-survival-systems-config) |
| Build, test, release, or upgrade | [Development and maintenance](development-and-maintenance.md) | [Dependency matrix](architecture.md#external-dependency-map) |
| Investigate a broken save or delivery | [Configuration and operations](configuration-and-operations.md#diagnostics-and-recovery) | [State ownership](architecture.md#state-ownership-and-persistence) |

## Documentation map

- [Architecture](architecture.md) explains module boundaries, event buses,
  server/client authority, external dependencies, state ownership, and the
  end-to-end runtime flows.
- [Service reference](service-reference.md) is the detailed Java catalog. Every
  production class is listed with its responsibility, collaborators, state,
  entry points, and maintenance cautions.
- [Data and resources](data-and-resources.md) explains every resource namespace,
  loot-table family, Lost Cities compatibility layer, Patchouli book, quest
  defaults, models, recipes, tags, and localization contract.
- [Configuration and operations](configuration-and-operations.md) lists every
  config key and default, explains which changes affect existing state, and
  gives server-operation and recovery procedures.
- [Development and maintenance](development-and-maintenance.md) covers local
  setup, Gradle tasks, test strategy, change checklists, release procedure,
  dependency upgrades, compatibility, and documentation upkeep.
- [Radio quest protocol](radio-quests.md) is the implementation-facing contract
  for FTB Quests tags, atomic submission, manifests, delivery persistence, and
  choice rewards.
- [Quest authoring](authoring-survivor-network-quests.md) is the content-author
  workflow for building new Survivor Network contracts.
- [City operations](city-operations.md) explains the player-facing radio,
  building-clear, danger, and infected-scaling loop.
- [Stealth, attention, and encumbrance](stealth-attention-and-encumbrance.md)
  explains quiet movement, progressive suspicion, noise radii, backpack weight,
  movement penalties, and the radio Horde Watch panel.
- [Modrinth gameplay descriptions](modrinth-gameplay-mechanics.md) contains
  short standalone paragraphs for presenting each new survival loop.

## Project at a glance

| Property | Current value |
|---|---|
| Mod id | `biohazard` |
| Released version represented here | `1.1.3` |
| Minecraft | `1.21.1` |
| NeoForge | `21.1.235` |
| Java | `21` |
| Build tool | Gradle Wrapper `9.2.1`, ModDevGradle `2.0.141` |
| Base package | `io.github.gev414.biohazard` |
| License | MIT, with third-party resource notices in `THIRD_PARTY_NOTICES.md` |
| CI | GitHub Actions runs `./gradlew build` on pushes and pull requests |

## Source layout

```text
biohazard/
|-- build.gradle                     Build, dependencies, runs, metadata
|-- gradle.properties                Version pins and project identity
|-- src/main/java/.../biohazard/     Production Java
|   |-- block/                       Radio block and block entity
|   |-- client/                      Client state, screens, fog, renderers
|   |-- config/                      NeoForge config specifications
|   |-- damage/                      Data-backed damage-type keys
|   |-- encounter/                   Persistent Lost Cities encounters
|   |-- encumbrance/                 Carried weight and backpack integration
|   |-- entity/                      Brute, projectile, and AI
|   |-- event/                       NeoForge event adapters
|   |-- item/                        Item registration and infection medicine
|   |-- loot/                        Handcrafted storage stocking
|   |-- lostcities/                  Lost Cities API adapter
|   |-- network/                     Custom payload protocol
|   |-- stealth/                     Awareness and noise investigation
|   `-- quest/                       FTB Quests and courier deliveries
|-- src/main/resources/
|   |-- assets/                      Client assets and Patchouli content
|   |-- biohazard/ftbquests_defaults Bundled initial quest book
|   `-- data/                        Server data packs and compatibility data
|-- src/main/templates/              Expanded NeoForge mod metadata
|-- src/test/java/                   JUnit unit and resource-contract tests
|-- docs/                            Maintainer and author documentation
`-- .github/workflows/build.yml      Continuous integration
```

Generated and local-only directories are not source:

- `build/` contains build output and generated metadata.
- `.gradle/` contains Gradle caches.
- `run/` contains the local development game instance.
- `.idea/` contains IDE state.
- `src/generated/resources/`, if created by data generation, is included by the
  build but should be reviewed like any other generated artifact.

## Architectural rules of thumb

These are the invariants worth preserving during maintenance:

1. The logical server is authoritative for encounters, infection medicine,
   quest acceptance/submission, delivery scheduling, delivery selection, and
   inventory changes.
2. Client code presents server decisions; it does not decide gameplay state.
3. World-persistent state belongs in `SavedData`, not static collections.
4. Static collections are caches only and must be cleared on lifecycle events.
5. External-mod state is consumed through its published API or event hooks.
   Biohazard does not reimplement another mod's calendar or world generator.
6. Content that can be expressed through standard resources should remain
   data-driven.
7. A bundled default quest book is installed only into an empty destination.
   Existing server content is never silently overwritten.
8. A client-to-server request is always validated again on the server.
9. Persistent formats and network payloads are compatibility contracts. Treat
   changes to NBT keys, identifiers, and codecs as migrations, not refactors.
10. A dependency upgrade is not complete until Java integration, resource IDs,
    metadata ranges, and an in-game test instance have all been checked.

## Keeping this handbook current

Documentation is part of the definition of done. Update it when a change adds
or alters any of the following:

- a production class or package;
- a config key or default;
- a registry id, translation key, network payload, or NBT field;
- an event subscription or lifecycle hook;
- an external dependency or supported version range;
- a resource namespace, loot-table family, quest tag, or authoring workflow;
- a build, test, release, or recovery procedure.

For class-level changes, update [service-reference.md](service-reference.md).
For behavior spanning several classes, update the relevant runtime flow in
[architecture.md](architecture.md). For data-only work, update
[data-and-resources.md](data-and-resources.md).
