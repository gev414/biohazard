# City operations and danger zones - design notes

Status: implemented gameplay direction. These notes preserve the design
decisions and the configuration points used by the first implementation.

## Confirmed direction

- A city operation may require five fully cleared buildings.
- Progress must come from buildings whose Biohazard encounter actually reaches
  `CLEARED`. Buildings materialized as `SAFE` do not count.
- Building progress is unique by `BuildingKey`. A multi-chunk building already
  resolves to one root key and therefore counts once.
- City danger is shared city state rather than state owned by an individual
  Radio Transmitter.
- Radios act as local terminals for the city zone containing them. Multiple
  radios in one city share its progress and danger; radios in separate cities
  report separate city states.
- City danger increases gradually. The current candidate rule is one danger
  level per five cleared buildings, with a configurable maximum around level
  10-15.
- The current candidate health scaling is +10% maximum health per danger level.
- The Brute must scale with the danger zone as well. It is part of the infected
  apocalypse and should not be exempt from city escalation. Brute-specific
  tuning may still be useful so its high base health remains enjoyable to fight.
- The exact Lost Cities city footprint should drive building progress. A wider
  influence perimeter should drive infected scaling so horde mobs spawned just
  outside the city are still affected.

## City-zone discovery

Lost Cities exposes whether an individual chunk is a city chunk, but it does
not expose a persistent city ID, city center, or complete chunk list. Biohazard
will therefore need to derive a city zone by scanning connected city chunks and
persisting its own stable zone identity and membership.

The city scan must be deliberately paced to prevent generation or server-tick
stutter. A good direction is to reuse the Radio Transmitter's existing 60-second
calibration period as the survey window:

1. A newly placed radio begins its normal calibration.
2. During calibration, the server incrementally scans a small, bounded number
   of candidate chunks per tick.
3. The radio can present the process as surveying or mapping the local city
   grid.
4. When both calibration and surveying are complete, the radio binds to the
   resolved city zone.
5. Very large or effectively infinite city profiles still require a hard scan
   cap and a fallback sector strategy.

The per-tick scan budget, maximum scanned chunks, city connectivity rule, and
fallback sector size should be configurable or centralized constants and must
be load-tested in the complete modpack.

## Danger influence

The persisted city footprint contains the exact connected Lost Cities chunks.
For infected scaling, the footprint should gain a configurable perimeter. The
current horde spawn distance is about 75 blocks, so an initial five-chunk
(80-block) perimeter is a sensible candidate.

If influence perimeters from two cities overlap, the higher applicable danger
level should win. An infected mob should never receive duplicate health
modifiers.

Infected snapshot the highest city danger that has affected them. Loaded
infected are upgraded when a city gains a level, while unloaded infected are
upgraded when they next load. The stored danger never decreases merely because
the infected leaves the city. A data-pack entity-type tag controls which
infected are affected and includes the Brute.

## Encounter and quest integration

The encounter manager should expose one centralized transition for marking a
building cleared. Both regular encounter completion and Brute death should use
it. That transition can notify the city-zone service and add the building key
to the zone's unique cleared-building set.

An FTB custom task can then derive progress from that set instead of blindly
incrementing a counter. A task accepted through a radio should be bound to the
radio's city zone so that clearing buildings in a different city cannot satisfy
the contract accidentally.

`CLEARED` is assigned only through the centralized encounter-finished
transition. A regular-only encounter finishes after its objective and remaining
loaded encounter population are gone. In any encounter with a Brute, killing
the Brute finishes and clears the building even when a marked regular infected
is still hidden inside.

## Implementation controls

- Radio calibration also runs a paced connected-city survey.
- Surveys have a per-radio chunk budget, a hard scan cap, configurable
  cardinal/diagonal connectivity, and a stable fallback sector for city
  profiles that reach the cap.
- City progress, exact footprints, danger, cleared building keys, and operation
  bindings are stored in `biohazard_city_zones`.
- `biohazard:city_scaled_infected` is the data-pack entity-type tag for health
  scaling.
- FTB custom tasks tagged `biohazard_city_operation` track five new unique
  clears by default. `biohazard_city_buildings_<count>` changes the target.
- All server tuning is in `biohazard-city-operations.toml`.

## Candidate progression example

- 0-4 cleared encounter buildings: danger level 0, +0% maximum health.
- 5-9: danger level 1, +10% maximum health.
- 10-14: danger level 2, +20% maximum health.
- Continue at five buildings per level up to the configured cap.

The radio should report the local zone's cleared-building count, current danger
level, health modifier, and progress toward the next escalation.
