# City operations

City operations turn cleared Lost Cities buildings into shared local progress.
A Radio Transmitter is the terminal for that progress: it maps the surrounding
city, reports its condition, and is required when a Survivor Network contract
uses a city-operation objective.

## Player loop

1. Place a Radio Transmitter in or near a Lost Cities city.
2. Leave it in place while it calibrates and maps connected city chunks. Both
   processes begin on placement and must finish before the radio connects.
3. Interact with the connected radio to open the Survivor Network.
4. Open the compact **`< CITY`** tab beside the right side of the quest screen
   to review the mapped city's status. Select **`CITY >`** to collapse it.
5. Clear encounter buildings in that city. The city's cleared-building total,
   danger level, and infected health bonus update as progress grows.

The default calibration duration is 60 seconds. During setup, the radio can
report either a remaining calibration time or a city-grid survey count in chat.
The survey is deliberately paced across server ticks to avoid a large
generation spike. A transmitter is usable only after both jobs complete.

## What counts as a cleared building

Only an encounter that reaches **`CLEARED`** adds one unique building to city
progress. A building selected as **`SAFE`** does not count, and revisiting an
already cleared building does not add progress again.

For a regular encounter, all marked encounter infected must be defeated. For
an encounter that includes a Brute, killing the marked Brute immediately
finishes and clears that building. This deliberately avoids asking players to
hunt down a stray regular infected after the boss is dead.

Progress belongs to the city zone, not to an individual transmitter. Multiple
radios mapped to the same city report the same total. A radio outside a mapped
Lost Cities zone still provides ordinary Survivor Network access, but reports
that no city zone is mapped and cannot accept a city-operation objective.

## Danger and infected scaling

By default, every five unique cleared encounter buildings increase the city's
danger by one level, up to level 12. Each level adds 10% of base maximum health
to tagged infected; the Brute has a separate setting with the same default.

| Cleared buildings | Default danger | Default maximum-health bonus |
|---:|---:|---:|
| 0-4 | 0 | +0% |
| 5-9 | 1 | +10% |
| 10-14 | 2 | +20% |
| 15-19 | 3 | +30% |

Danger applies inside the mapped city and, by default, a five-chunk (80-block)
perimeter around it. When a city gains a level, loaded tagged infected are
upgraded immediately; unloaded ones are updated the next time they load. Once
an infected has received a higher city danger level, it does not lose that
health bonus merely by leaving the city.

## City status drawer

The radio sends a snapshot when it opens the Survivor Network. The drawer
contains:

- **Buildings cleared** - unique encounter buildings that count for this city.
- **Danger level** - current and maximum configured danger.
- **Infected health** - the current normal-infected maximum-health bonus.
- **Clears to next level** - the number of additional unique clears required
  before the next danger increase.

The drawer is intentionally collapsed by default so the FTB Quests map and
contacts remain usable. It is a report from the radio interaction, not a live
HUD tracker; reopen the radio to request an updated snapshot after making
progress.

## City-operation contracts

Quest authors can create a city objective with a Custom task tagged
`biohazard_city_operation`. The optional
`biohazard_city_buildings_<count>` tag sets the required number of new clears;
without it, the target is five. Accepting the contract through a mapped radio
binds the task to that city and records its current clear total. Only later,
unique clears in that same city advance the objective.

See [Quest authoring](authoring-survivor-network-quests.md#add-a-city-operation-objective)
for the exact FTB Quests setup.

## Server tuning and persistence

All city settings are in the world's
`serverconfig/biohazard-city-operations.toml`. Operators can adjust survey
budget/caps, city connectivity, influence perimeter, clears per level, maximum
danger, and normal/Brute health scaling. See
[Configuration and operations](configuration-and-operations.md#4-city-operations-config)
for the full table and persistence notes.

City footprints, unique clears, and operation bindings are saved with the
world. Existing encounter records already marked `CLEARED` are imported when a
radio first maps their city, so established worlds retain legitimate prior
progress.
