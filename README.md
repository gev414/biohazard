Biohazard

Biohazard is a Minecraft 1.21.1 NeoForge mod developed as part of a curated zombie-survival modpack. Rather than acting as a standalone gameplay overhaul, it serves as the integration layer that coordinates persistent encounters, custom entities, progression systems, and multiple third-party mods into a cohesive experience.

The project emphasizes modular Java design, clear separation of responsibilities, and data-driven systems wherever practical.

Design Goals

Biohazard is built around several architectural principles:

modular feature separation
event-driven gameplay using NeoForge's event bus
data-driven content through JSON resources and datapacks
minimal hard-coded integration logic
clear separation between gameplay logic and modpack configuration
reusable components that can be extended independently

Where possible, gameplay behavior is exposed through data packs, loot tables, advancements, configuration files, and Patchouli content instead of Java code.

Major Systems
Encounter System

The encounter package implements persistent building encounters for Lost Cities.

Responsibilities include:

encounter lifecycle management
building selection
encounter persistence
mob spawning
boss site selection
server-side state synchronization

Encounter progress survives world saves and server restarts.

Entity System

Custom entities are isolated inside the entity package.

The current implementation includes:

Biohazard Brute
BruteRockProjectile

Entity-specific AI remains inside the entity implementation while generic navigation and mining behaviour is delegated to ZombieTactics-Profiled.

Lost Cities Integration

Rather than modifying Lost Cities directly, Biohazard consumes building metadata exposed by Lost Cities and layers encounter behaviour on top of generated structures.

This keeps the integration loosely coupled and easier to maintain across Lost Cities updates.

Patchouli Documentation

The in-game Survivor's Field Manual is treated as part of the project itself rather than external documentation.

Patchouli pages are maintained alongside the source code, allowing gameplay documentation to evolve together with implementation.

Data-Driven Resources

The project intentionally moves as much content as possible into resource packs.

Examples include:

loot tables
advancements
damage types
Patchouli entries
crafting overrides
recipe removals

This reduces hard-coded behaviour and makes balancing significantly easier.

External Configuration

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
PointBlank	Firearms and ammunition
Waystones	Travel mechanics
Tough As Nails	Survival systems
Patchouli	In-game documentation
The Hordes	Horde scheduling and infection

Development dependencies are resolved through Modrinth Maven and are not bundled into the final artifact.

Testing
The project contains unit tests covering encounter selection and encounter state behaviour.
Resource-heavy changes (Patchouli, loot tables, integrations) should additionally be validated inside the complete development modpack.
