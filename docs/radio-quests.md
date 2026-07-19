# Biohazard radio quests and courier deliveries

This document describes the current implementation of the Survivor Network:
the FTB Quests pager, radio-gated contract flow, delayed courier rewards, and
the data files used to author new deliveries.

## Architecture at a glance

Biohazard uses FTB Quests as the journal and user interface. It does not fork
the FTB Quests screens. Biohazard listens to FTB Quests custom-task and
custom-reward events and gives selected objects radio behavior through tags.

The flow is:

1. A player opens the quest book through a calibrated Radio Transmitter.
2. The player presses the tagged accept button near any calibrated transmitter.
3. Ordinary FTB Quests tasks track the field objectives.
4. The player returns to a transmitter and presses the tagged transmit/turn-in
   button.
5. Tagged item tasks are checked and consumed atomically.
6. When the tagged custom reward is claimed, Biohazard rolls a loot table,
   stores the resulting item stacks, and schedules a mailbox delivery.
7. After the category delay, the package can be collected at a transmitter.

The quest screen can be viewed away from a transmitter, but acceptance,
submission, completion, and collection are server-side radio checks.

## Radio transmitter behavior

The transmitter calibrates after placement or relocation. The default
calibration time is 1,200 server ticks (60 seconds). A player must be within
six blocks of a calibrated transmitter for radio actions. These values are
configurable in `config/biohazard-radio-quests.toml`:

```toml
[radioQuests]
transmitterRange = 6
calibrationTicks = 1200

[radioQuests.deliverySeconds]
supplies = 120
ammunition = 120
medical = 180
equipment = 240
firearm = 300
```

Times are server game time. Leaving the world does not advance a delivery
countdown, and deliveries survive server restarts.

## FTB Quests tagging contract

Tags are ordinary FTB Quests tags, so use lowercase letters, digits, and
underscores only. The integration recognizes these tags:

| Object | Tag | Purpose |
|---|---|---|
| First custom task | `biohazard_radio_accept` | Enables the accept button and checks transmitter range. |
| Physical item task | `biohazard_radio_submit` | Marks an item requirement for atomic radio turn-in. |
| Final custom task | `biohazard_radio_complete` | Enables transmit/turn-in and performs submission checks. |
| Custom reward | `biohazard_radio_delivery` | Sends the reward through the courier system. |
| Custom reward | `biohazard_radio_choice_delivery` | Sends a delayed, player-selectable courier delivery. |
| Delivery reward | `biohazard_manifest_<name>` | Selects `quest_delivery/<name>.json`. |
| Delivery reward | `biohazard_category_<category>` | Selects the delay category. |
| Choice delivery reward | `biohazard_choice_count_<1-9>` | Number of distinct options to generate; defaults to 3. |

Supported categories are `supplies`, `ammunition`, `medical`, `equipment`,
and `firearm`. If no category tag is present, the implementation falls back
to `supplies`. If a delivery reward has no manifest tag, it is rejected and no
delivery is scheduled.

Set radio item tasks to `consume_items: false` and
`only_from_crafting: true`; do not set `task_screen_only`. This keeps their
item icons and required amounts visible in the normal quest page while
preventing FTB Quests from automatically submitting inventory items. The
final radio handler checks every tagged submission task and consumes them as
one transaction; if any item is missing, nothing is consumed and the quest is
not completed.

The delivery reward should use `auto: "enabled"`, as in the bundled quest
files. The reward's manifest name must exactly match the loot-table filename.

## Creating a custom courier loot table

Create a standard Minecraft loot table at:

```text
src/main/resources/data/biohazard/loot_table/quest_delivery/<name>.json
```

The resource ID is `biohazard:quest_delivery/<name>`. A minimal one-item
table is:

```json
{
  "pools": [
    {
      "rolls": 1,
      "entries": [
        {
          "type": "minecraft:item",
          "name": "minecraft:iron_ingot",
          "weight": 1
        }
      ]
    }
  ],
  "random_sequence": "biohazard:quest_delivery/example"
}
```

To make one random item from a pool, add multiple item entries with equal or
weighted `weight` values and keep `rolls` at `1`:

```json
{
  "pools": [
    {
      "rolls": 1,
      "entries": [
        {"type":"minecraft:item","name":"pointblank:mp5","weight":1},
        {"type":"minecraft:item","name":"pointblank:g36c","weight":1},
        {"type":"minecraft:item","name":"pointblank:ak47","weight":1}
      ]
    }
  ],
  "random_sequence": "biohazard:quest_delivery/weapons_random"
}
```

Useful loot-table features such as `functions`, `set_count`, conditions, and
multiple pools are supported because the table is rolled by Minecraft's normal
loot-table manager. Ensure every item ID exists on the server and client.
An empty or invalid table produces no package and logs an error.

Choice tables normally roll one candidate per loot-table roll. For a choice
delivery, Biohazard rerolls the table until it has the requested number of
distinct candidates (up to eight attempts per candidate), then saves those
exact item stacks. Once the courier is ready, interacting with a calibrated
transmitter opens a selection screen. The selection is validated by the server
and then converted to a normal mailbox package. If the inventory is full, the
chosen item remains in the mailbox until it can be collected.

## Connecting the table to a quest

For a table named `attachments_random`, the custom reward needs tags like:

```text
biohazard_radio_delivery
biohazard_manifest_attachments_random
biohazard_category_equipment
```

The manifest suffix is case-sensitive and must contain only the table name,
without `.json`. The reward's visible FTB icon is independent of the actual
courier contents; use an appropriate representative icon or reward preview.

For a three-weapon selection instead, use:

```text
biohazard_radio_choice_delivery
biohazard_manifest_weapons_choice
biohazard_choice_count_3
biohazard_category_firearm
```

## Delivery lifecycle and persistence

Loot is rolled when the reward is claimed, not when the package is collected.
The exact generated stacks are stored in world saved data under
`biohazard_radio_deliveries`. This prevents a datapack reload or a changed loot
table from rerolling an already-earned reward.

Each delivery belongs to the player who claimed the reward. It is not attached
to a particular transmitter, so moving or breaking the transmitter does not
erase an in-transit package. When the package is ready, collection inserts as
much as the mailbox/inventory can accept; any remainder stays available for a
later collection.

## Default quest files and editing workflow

Bundled defaults are under:

```text
src/main/resources/biohazard/ftbquests_defaults/
```

Biohazard copies them into `config/ftbquests/quests` only when the destination
quest directory is absent or empty. Existing books are never merged or
overwritten. Therefore:

- Use the in-game FTB Quests editor for a live instance.
- Use “Save on server now” before testing server-side changes.
- To make an edited quest a mod default, copy the resulting SNBT back into the
  matching `src/main/resources/biohazard/ftbquests_defaults` file.
- Keep custom loot tables in `src/main/resources/data/biohazard/...` so they
  are packaged into the jar.

When distributing a rebuilt mod, the resource files and Java implementation
ship together. A table copied only into an instance config is not part of the
mod's default data and will not be present in a fresh installation.

## Checklist for a new radio contract

1. Create or edit the chapter and quest in FTB Quests.
2. Add the accept, submit, complete, delivery, manifest, and category tags.
3. Add each submitted item's submission tag and keep FTB item consumption disabled; the radio turn-in consumes the whole set atomically.
4. Create `quest_delivery/<manifest>.json` and validate all item IDs.
5. Make sure the task order leaves the final custom task reachable.
6. Add translations and a representative quest/reward icon.
7. Test range denial, successful atomic turn-in, restart persistence, delay,
   and full-inventory collection.

## Common mistakes

- Using `biohazard_manifest_name` without the matching `name.json` file.
- Putting the manifest tag on the quest instead of the custom reward.
- Omitting the category tag and unintentionally receiving the supplies delay.
- Expecting a normal FTB reward or FTB reward table to use the courier system.
- Using an invalid item ID, which makes the table roll empty.
- Marking a choice reward with `biohazard_radio_delivery` instead of
  `biohazard_radio_choice_delivery`.
- Editing the live config but forgetting to save on the server or copy the
  change into the source defaults.
