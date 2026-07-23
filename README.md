## Biohazard

Biohazard is a Minecraft 1.21.1 NeoForge mod developed as part of a curated zombie-survival modpack. Rather than acting as a standalone gameplay overhaul, it serves as the integration layer that coordinates persistent encounters, custom entities, progression systems, and multiple third-party mods into a cohesive experience.

The project emphasizes modular Java design, clear separation of responsibilities, and data-driven systems wherever practical.

## Maintainer documentation

The professional engineering handbook starts at
[`docs/README.md`](docs/README.md). It covers the system architecture, every
production Java service and its dependencies, persistent state, network and
runtime flows, the complete configuration surface, data/resource ownership,
testing, upgrades, operations, and release maintenance.

Design Goals

Biohazard is built around several architectural principles:

modular feature separation
event-driven gameplay using NeoForge's event bus
data-driven content through JSON resources and datapacks
minimal hard-coded integration logic
clear separation between gameplay logic and modpack configuration
reusable components that can be extended independently

Where possible, gameplay behavior is exposed through data packs, loot tables, advancements, configuration files, and Patchouli content instead of Java code.

## Major Systems
Encounter System

The encounter package implements persistent building encounters for Lost Cities.

By default, the nearest building within 64 blocks is activated and receives a
one-time persistent population before the player enters. Server configuration
can switch newly discovered buildings back to capped, replenishing wave mode.

Responsibilities include:

encounter lifecycle management
building selection
encounter persistence
mob spawning
boss site selection
server-side state synchronization

Encounter progress survives world saves and server restarts.

Horde Atmosphere

Biohazard reads The Hordes' authoritative per-player event state on the
server and synchronizes a compact atmospheric state to each client. Scheduled
horde days gradually draw in fog before the event without reimplementing The
Hordes' calendar, random variation, or player-time rules.

## Survivor Network radio quests

FTB Quests powers a radio-contact progression layer styled as a low-light field
pager. Players browse contacts as text chapters, accept and turn in contracts
near a calibrated Radio Transmitter, and collect persistent courier deliveries
after category-based delays. The quest book remains usable as a journal away
from the radio, but server-side task checks prevent remote acceptance or final
submission. See [the radio quest authoring guide](docs/radio-quests.md) for the
tag contract, delays, persistence rules, and extension notes.

## City operations

Radio Transmitters also map local Lost Cities zones and report shared city
progress through a compact drawer in the Survivor Network screen. Fully
cleared encounter buildings raise city danger, which increases tagged
infected's maximum health without weakening them when they later leave the
city. See [City operations](docs/city-operations.md) for the gameplay loop,
status drawer, progression defaults, and server tuning.

Entity System

Custom entities are isolated inside the entity package.

The current implementation includes:

Biohazard Brute
BruteRockProjectile

Entity-specific AI remains inside the entity implementation while generic navigation and mining behaviour is delegated to ZombieTactics-Profiled.

## Lost Cities Integration

Rather than modifying Lost Cities directly, Biohazard consumes building metadata exposed by Lost Cities and layers encounter behaviour on top of generated structures.

This keeps the integration loosely coupled and easier to maintain across Lost Cities updates.

Handcrafted is a required dependency. Its furniture is placed through data-driven
Lost Cities palettes in selected libraries and civic interiors. When Lost Cities
Modern Tweaks 2.0.7 is installed, Biohazard also supplies narrowly targeted
`lcmt:` part overrides for those interiors and railway seating; the common empty
town, library, civic, factory, and shop interior slots are also replaced with
small Handcrafted furniture arrangements so generated floors are rarely blank.
The regular LCMT apartment tower families (`lcmt:building1` through
`lcmt:building8`) use Biohazard-owned decorated copies of their normal floor
parts. Each decorated part retains the original walls, windows, doors, lights,
chests, and other LCMT/vanilla objects, with Handcrafted furniture merged into
the same 16x16 slices. This makes the exact files used by world generation easy
to edit in-place. Empty, roof, and special parts remain supplied by LCMT.
Furniture is confined to `slices[1]`, directly above the untouched floor in
`slices[0]`. Its circled symbols (`①` through `⑳` and `ⓐ` through `ⓜ`) are
reserved for Biohazard and do not overlap any character used by LCMT 2.0.7.
To customize a normal floor, edit the corresponding full part under
`data/biohazard/lostcities/parts/building1` through `building8`; the original
room shell and its vanilla objects remain visible around the furniture markers.
The rest of the Modern Tweaks world-generation data remains untouched.

## Patchouli Documentation

The in-game Survivor's Field Manual is treated as part of the project itself rather than external documentation.

Patchouli pages are maintained alongside the source code, allowing gameplay documentation to evolve together with implementation.

## Data-Driven Resources

The project intentionally moves as much content as possible into resource packs.

Examples include:

loot tables
advancements
damage types
Patchouli entries
crafting overrides
recipe removals

This reduces hard-coded behaviour and makes balancing significantly easier.

## External Configuration

Biohazard intentionally does not hard-code every gameplay rule.

Several balancing decisions are delegated to external configuration supplied by the surrounding modpack, including:

Waystones
Tough As Nails
The Hordes
Lost Cities
PointBlank

This separation allows gameplay tuning without requiring Java changes or recompilation.

 Dependencies
Dependency	Purpose
Lost Cities	World generation integration
Handcrafted	Generated furniture palettes
PointBlank	Firearms and ammunition
Waystones	Travel mechanics
Tough As Nails	Survival systems
Patchouli	In-game documentation
The Hordes	Horde scheduling and infection
FTB Quests	Survivor Network contacts, contracts, and quest journal

Development dependencies are resolved through Modrinth Maven and are not bundled into the final artifact.

## Testing
The project contains unit tests covering encounter selection, encounter state
behaviour, city danger progression, and bounded connected-city surveying.
Resource-heavy changes (Patchouli, loot tables, integrations) should additionally be validated inside the complete development modpack.


## License

Biohazard's original code and resources are licensed under the [MIT License](LICENSE).

Portions of the Lost Cities data resources are copied or adapted from The Lost Cities and remain subject to their original MIT license and copyright notice. See [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

Third-party mods and libraries are not bundled with Biohazard and remain subject to their respective licenses.
