# Weather forecast and contaminated precipitation

Rotwire turns Overworld weather into a planning system without dividing the
world into permanent hazard zones. A calibrated Radio Transmitter exposes a
collapsible `WX` drawer beside the existing city panel. It reports the current
condition and transition, today's scheduled weather window, tomorrow's
forecast, and the current Serene Seasons season when that mod is installed.

## Daily schedule

The logical server deterministically generates and persists one plan for each
Minecraft day. The rolling forecast retains enough neighboring plans to keep
today and tomorrow stable across restarts. Five outcomes are available:

- clear;
- ordinary rain;
- ordinary storm;
- contaminated rain;
- contaminated storm.

Wet conditions occupy a bounded window between ticks 2,000 and 23,000 rather
than automatically lasting the whole day. The radio formats those ticks as a
24-hour clock. A clear plan has no weather window.

The default selection weights are 40 clear, 30 rain, 15 storm, 10 contaminated
rain, and 5 contaminated storm. Serene Seasons adjusts those relative weights:
spring and autumn favor rain, summer favors clear weather and storms, and
winter disables storms while favoring precipitation. Without Serene Seasons,
the temperate base weights are used.

Safety rules keep the system from overwhelming a new or unlucky world:

- contaminated weather cannot be selected before world day 5;
- contaminated weather cannot occur on consecutive days;
- a contaminated storm is downgraded to an ordinary storm on deterministic
  Horde days;
- winter does not select either kind of storm.

## Rain and storms

Rain reduces
infected visual-suspicion gain to 85% and loud-action investigation range to
80%. Storms reduce those values to 70% and 60%. These multipliers apply to the
existing stealth and attention systems; they do not make the player invisible
or silence an action completely. Contaminated variants retain this masking,
but add the exposure hazard described below.

## Contaminated exposure

Contaminated weather is hazardous only when precipitation can reach the
player. A roof or other solid shelter immediately clears exposure progress.
Creative and spectator players are ignored.

Default behavior:

| Condition | Grace | Damage after grace |
|---|---:|---:|
| Contaminated rain | 4 seconds | 1 heart every 5 seconds |
| Contaminated storm | 2 seconds | 1 heart every 3 seconds |

Direct exposure first adds a dirty-green edge vignette and the text
`CONTAMINATED RAIN - SEEK SHELTER`. Once the grace period expires, the vignette
becomes a pulsing red-orange and damage begins. There is deliberately no grace
period bar: the vignette and text are the warning. The overlay fades after the
player reaches shelter.

The damage uses the data-backed `rotwire:contaminated_rain` damage type and is
tagged not to provoke neutral mobs.

## Persistence and authority

The schedule is stored in Overworld `SavedData` under
`rotwire_weather_schedule`, typically
`<world>/data/rotwire_weather_schedule.dat`. The server owns schedule
generation, Minecraft weather state, shelter checks, exposure timing, and
damage. Clients receive only compact forecast and exposure snapshots for
presentation.

Changing weights or durations does not rewrite already-persisted day plans.
Future days are generated from the new settings as the rolling schedule moves
forward.

## Operator testing commands

Rotwire provides permission-level-2 commands that temporarily override the
active condition without bypassing forecasts, exposure, shelter checks, or
damage:

```mcfunction
/rotwire weather force clear [duration]
/rotwire weather force rain [duration]
/rotwire weather force storm [duration]
/rotwire weather force contaminated_rain [duration]
/rotwire weather force contaminated_storm [duration]
/rotwire weather status
/rotwire weather clear
```

The optional duration defaults to `12000t` (ten real-time minutes at 20 TPS).
Minecraft time suffixes are accepted: `t` for ticks, `s` for seconds, and `d`
for Minecraft days. Examples:

```mcfunction
/rotwire weather force contaminated_storm 30s
/rotwire weather force contaminated_rain 6000t
/rotwire weather force storm 1d
```

`force clear` creates a timed clear-weather override. In contrast,
`weather clear` cancels any override immediately and resumes the underlying
persisted schedule. `weather status` reports the condition and remaining
ticks.

Overrides are stored alongside the schedule and survive a server restart.
They expire against server game time, so their countdown pauses while the
server is stopped. They do not rewrite today or tomorrow's normal plan. While
active, the radio shows the forced current condition and a live
`TEST OVERRIDE` countdown above the unchanged daily forecasts.

The vanilla `/weather` command is not suitable while Rotwire scheduling is
enabled because the authoritative manager restores its selected condition
within five ticks.

## Client presentation tuning

The vignette is deliberately code-owned rather than a gameplay config. Its
current geometry uses 14 three-pixel layers and a peak alpha of 110 in
`WeatherExposureClient`. `VIGNETTE_LAYERS` controls inward reach,
`LAYER_WIDTH` controls each band, the alpha expression controls strength, and
`FADE_PER_SECOND` controls response speed. Changing these values requires a
client rebuild; it does not change grace, damage, shelter checks, or server
authority.

## Modpack compatibility

Rotwire must be the authority that changes Minecraft's rain and thunder state
when this feature is enabled. Visual and audio weather mods can continue to
render precipitation, particles, fog, ambience, and storm sounds from the
vanilla state Rotwire sets.

In the reference Biohazard instance,
`sereneseasons/seasons.toml` sets `change_weather_frequency = false`. Serene
Seasons still supplies its calendar, seasonal colors, snow/ice behavior, and
season identity; Rotwire alone selects the daily weather plan. Re-enable the
Serene Seasons weather-frequency option only if Rotwire's weather scheduler is
disabled.

All balance values are exposed in the world's `rotwire-weather.toml`. See
[Configuration and operations](configuration-and-operations.md#6-weather-forecast-and-hazard-config).
