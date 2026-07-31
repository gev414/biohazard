# Sleep survival

Rotwire treats portable sleep as an emergency convenience rather than a free
way to remove the night from survival gameplay.

## Restless Sleep

When a Traveler's Backpack sleeping bag successfully skips the night, the
player receives the harmful `rotwire:restless_sleep` effect for 50 seconds.
Every ten seconds it removes one full hunger icon and one full thirst icon.
The meters are clamped at their normal minimum, so the effect cannot create an
invalid value.

Regular beds do not apply Restless Sleep. A sleeping bag only applies it when
the sleep actually advances the world to daytime; waking up early does not
trigger the penalty.

## Rested campsites

A sleeping-bag night avoids Restless Sleep when all of these requirements are
met:

- the sleeping bag is either directly beneath a complete deployed Rotwire
  tarp or inside a supported SimplyTents structure;
- a lit campfire or soul campfire is within twelve blocks;
- a deployed Traveler's Backpack is within twelve blocks; and
- that backpack contains food whose combined nutrition is strictly greater
  than five.

For a tarp, distances are measured from the sleeping bag. For SimplyTents,
distances are measured from the tent's ground center so a sleeping bag can be
placed anywhere inside without skewing the campsite. The effective radius is
never smaller than the physical shelter: compact tents require at least three
blocks, duo tents and tipis four, large tents five, and the yurt six. The
configured radius can increase these values but cannot reduce them.

While every requirement is satisfied, players inside the campsite radius
receive the beneficial `rotwire:prepared_shelter` notifier. Its campfire icon
disappears within about one second after the player leaves the radius or the
fire, backpack, food, or shelter becomes invalid. This readiness check never
consumes food.

A Radio Transmitter placed inside the same shelter becomes a persistent Camp
Radio and exposes these checks through its Camp Hub. See
[Camp Radio](camp-radio.md).

When the night successfully advances, the smallest qualifying combination of
food is removed from the nearest qualifying backpack. For example, two foods
worth three nutrition each qualify and are consumed; one food worth five does
not. Container items such as bowls are returned to the backpack when space is
available, or dropped beside it if it is full.

The ration is not charged when the player merely enters the sleeping bag or
wakes before the night is skipped. Each sleeping player must pay a separate
ration. If several players share one backpack, it must contain enough food for
each of them.

## New Dawn

A player who remains awake for the complete natural night receives the
beneficial `rotwire:new_dawn` effect at dawn. It lasts 50 seconds and restores
one full hunger icon and one full thirst icon every ten seconds.

New Dawn is forfeited if the player sleeps, dies, changes dimensions, logs in
after the night has begun, or another player skips the night. The player must
also remain in the same dimension for the full tracked night.

## Configuration

The settings are generated in `rotwire-survival.toml` under
`[survivalSystems.sleepSurvival]`:

- `enabled`: enables the sleep-survival system.
- `effectDurationTicks`: effect duration; the default `1000` ticks is 50
  seconds.
- `pulseIntervalTicks`: time between meter changes; the default `200` ticks is
  10 seconds.
- `meterPointsPerPulse`: internal hunger/thirst points changed per pulse; the
  default `2` points equals one full HUD icon.
- `campsiteRadius`: maximum distance from the shelter center to the lit
  campfire and deployed backpack; the default is `12` blocks. SimplyTents
  structures enforce a size-based minimum that covers their complete
  footprint.
- `campsiteFoodNutritionThreshold`: the backpack's food total must be strictly
  greater than this value; the default is `5`.

The pulse interval and points-per-pulse settings are shared by New Dawn and
Restless Sleep. At the defaults, either effect changes each meter by five full
HUD icons over its 50-second duration.

## Testing commands

Run these commands as the player who should receive the effect:

```mcfunction
/effect give @s rotwire:restless_sleep 50 0 true
/effect give @s rotwire:new_dawn 50 0 true
```

The final `true` hides particles while keeping the effect icon visible.
