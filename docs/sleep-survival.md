# Sleep survival

Biohazard treats portable sleep as an emergency convenience rather than a free
way to remove the night from survival gameplay.

## Restless Sleep

When a Traveler's Backpack sleeping bag successfully skips the night, the
player receives the harmful `biohazard:restless_sleep` effect for 50 seconds.
Every five seconds it removes one full hunger icon and one full thirst icon.
The meters are clamped at their normal minimum, so the effect cannot create an
invalid value.

Regular beds do not apply Restless Sleep. A sleeping bag only applies it when
the sleep actually advances the world to daytime; waking up early does not
trigger the penalty.

## New Dawn

A player who remains awake for the complete natural night receives the
beneficial `biohazard:new_dawn` effect at dawn. It lasts 50 seconds and restores
one full hunger icon and one full thirst icon every five seconds.

New Dawn is forfeited if the player sleeps, dies, changes dimensions, logs in
after the night has begun, or another player skips the night. The player must
also remain in the same dimension for the full tracked night.

## Configuration

The settings are generated in `biohazard-survival.toml` under
`[survivalSystems.sleepSurvival]`:

- `enabled`: enables the sleep-survival system.
- `effectDurationTicks`: effect duration; the default `1000` ticks is 50
  seconds.
- `pulseIntervalTicks`: time between meter changes; the default `100` ticks is
  5 seconds.
- `meterPointsPerPulse`: internal hunger/thirst points changed per pulse; the
  default `2` points equals one full HUD icon.

## Testing commands

Run these commands as the player who should receive the effect:

```mcfunction
/effect give @s biohazard:restless_sleep 50 0 true
/effect give @s biohazard:new_dawn 50 0 true
```

The final `true` hides particles while keeping the effect icon visible.
