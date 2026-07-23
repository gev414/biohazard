# Stealth, attention, and encumbrance

Biohazard adds a server-authoritative survival loop around what the player
carries and how much attention their actions create. The system is configured
in `biohazard-survival.toml`.

## Player loop

A player is **quiet** only while all three conditions are true:

1. the player is crouching;
2. carried weight is in the `LIGHT` tier;
3. the player has not fired, attacked, or broken a non-instant block during
   the configured loud-action grace period.

The HUD reports **EXPOSED** whenever any quiet condition is not met. Exposed
does not mean the player has already been detected; it means infected may use
their normal targeting behavior instead of the quiet-player suspicion window.

Quiet players are not automatically assigned as infected targets. Infected
that can see a quiet player inside their view cone instead build suspicion.
Suspicion rises faster at close range, immediately completes inside the close
detection radius, and decays while sight is broken or the player stays outside
the view cone. Brutes use the configured suspicion multiplier.

Detection is not an instant disengage tool. Once an infected has acquired a
player, it retains that target for the alert-memory interval. Directly
attacking an infected always reveals the attacker to that mob.

Mobs spawned by The Hordes bypass stealth suppression. The Hordes remains the
owner of its tracking goal, so horde mobs continue pursuing their assigned
player without a suspicion window or Biohazard distance cap.

## Noise and investigation

Loud actions create a position and radius for nearby infected to investigate:

| Action | Default radius |
|---|---:|
| Suppressed PointBlank fire | 12 blocks |
| Unsuppressed PointBlank fire | 96 blocks |
| Melee attack against affected infected | 16 blocks |
| Breaking a non-instant block | 20 blocks |

PointBlank suppression is determined from the active attachment item IDs and
attachment groups, not from a guessed sound-name convention.

When ZombieTactics 1.3.3 is installed and
`replaceZombieTacticsMarkers = true`, Biohazard rejects that mod's automatic
marker entities. It creates an approved marker only for unsuppressed gunfire
and only when ZombieTactics' marker range does not exceed the configured
loud-gun radius. Smaller sounds use
Biohazard's radius-bounded investigation navigation so a 12-block suppressed
shot cannot leak into ZombieTactics' global marker range.

## Carried weight

Weight includes the main inventory, armor, offhand, and an equipped Traveler's
Backpack. Stored Traveler's Backpack items also include their storage slots,
tools, upgrades, and fluid tanks. Biohazard only reads backpack state.

Stack count scales a category's configured weight from 25% for a nearly empty
stack to 100% for a full stack. Tags take precedence over automatic categories:

- `biohazard:encumbrance/weightless`
- `biohazard:encumbrance/light`
- `biohazard:encumbrance/heavy`
- `biohazard:encumbrance/very_heavy`

Items not covered by a tag are classified as firearm, armor, block, or default.
The default tiers and movement penalties are:

| Tier | Weight | Speed penalty | Quiet movement |
|---|---:|---:|---:|
| Light | up to 16 | 0% | yes, while crouched |
| Burdened | over 16 through 25 | 10% | no |
| Heavy | over 25 through 40 | 20% | no |
| Overloaded | over 40 | 35% | no |

The compact lower-right HUD shows current load, tier, quiet/exposed state, and
a suspicion bar while any nearby infected is evaluating the player. It shifts
up while chat is open so the text-entry area remains unobstructed.

The player inventory also shows a compact weight indicator to the right of the
recipe-book button. Its number is current carried weight, its color is the
current tier, and its segmented bar shows progress toward the configured Heavy
boundary. Hovering it lists the server's live tier thresholds and movement
penalties.

## Configuration and displayed values

The Java values in `SurvivalSystemsConfig` are defaults for a newly generated
`biohazard-survival.toml`. NeoForge preserves assignments already stored in an
existing TOML when the mod is rebuilt with different defaults.

Encumbrance calculations, movement penalties, the lower-right HUD, and the
inventory tooltip all use the same active logical-server configuration. The
server sends the live thresholds and penalties to the client, so the display
does not maintain a second hard-coded balance table. Stop the game or server
before editing the TOML. Deleting it regenerates every survival-system setting,
not only the encumbrance tiers.

## Radio horde watch

Opening FTB Quests through a calibrated Radio Transmitter activates a static
Horde Watch panel in the lower-right corner. It reports whether the current day
is a horde day, whether a horde is currently active, and the current Minecraft
time as `00:00` through `23:59`.

The panel deliberately has no countdown. It consumes the same authoritative
The Hordes snapshot as the existing fog feature and does not alter horde
scheduling or pre-horde fog.
