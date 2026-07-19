# Authoring Survivor Network quests

This guide is for creating new FTB Quests contracts for the four survivor
network contacts. It assumes the Biohazard courier system is installed and
the player uses a calibrated Radio Transmitter to accept, transmit, and collect
work.

Read `docs/radio-quests.md` for the implementation reference. This document is
the practical authoring workflow and should be used when making new content.

## What the player experiences

Every radio contract follows the same in-world loop:

1. Build and calibrate a Radio Transmitter.
2. Open the quest journal and select a contact's contract.
3. Press **Accept Contract** while within transmitter range.
4. Complete field objectives and gather any required evidence.
5. Return to a transmitter and press **Transmit / Turn In**.
6. Claim the completed reward. The courier rolls its contents now, then starts
   the configured delivery timer.
7. Return to any calibrated transmitter after the timer to collect the package.

The quest page can be viewed anywhere, but network actions are checked by the
server. The default transmitter range is six blocks and calibration takes sixty
seconds after placement or relocation. Both values, along with delivery times,
are configurable in `config/biohazard-radio-quests.toml`.

## Choose the right questgiver

Keep a contact's quests focused on what that person supplies. This gives the
network a readable progression rather than four interchangeable shops.

| Contact | Role | Good objectives | Normal courier category |
|---|---|---|---|
| Dr. Mara Voss — Medic | Medicine and research | Infected kills, tissue, research data, medical-cache recovery | `medical` |
| Elias Ward — Quartermaster | Ammunition and general resupply | Documents, supply recovery, city reconnaissance | `ammunition` or `supplies` |
| Rook — Arms Broker | Guns and combat equipment | Encrypted Intel, Brute kills, hard city objectives | `firearm` or `equipment` |
| Nadia Vale — Surveyor | Transport and route utility | Maps, compasses, research data, rails, travel materials | `equipment` or `supplies` |

The default delivery delays are 120 seconds for supplies/ammunition, 180 for
medical, 240 for equipment, and 300 for firearms. Longer delays are useful for
powerful rewards, but should not be the only balancing tool; objective cost,
repeat cooldown, and loot-table weights matter too.

## Build the quest in the FTB Quests editor

Use the in-game editor on a development world or server. Create the quest in
the appropriate chapter, give it a clear title, description, icon, dependency,
and repeat settings. Let FTB Quests generate IDs; never duplicate an existing
task, quest, or reward ID by hand.

Set `require_sequential_tasks: false` unless the contract deliberately needs a
strict order. The radio accept task prevents progress before acceptance, while
the final radio complete task controls the actual turn-in.

Add these tasks to every courier contract:

1. A **Custom** task tagged `biohazard_radio_accept`. This is the accept button.
2. Any normal field objectives: kill, location, checkmark, item discovery, and
   so on.
3. One or more physical item tasks for evidence/materials, each tagged
   `biohazard_radio_submit`.
4. A final **Custom** task tagged `biohazard_radio_complete`. This is the
   transmit/turn-in button.

The accept and complete custom tasks need an icon because FTB Quests presents
them as clickable buttons. The existing chapters use the `ftbteams:textures/add.png`
icon for accept and `ftblibrary:icons/check` for turn-in; duplicate one of
those tasks in the editor if that is easier than creating the icon data again.

### Configure every submitted item task exactly this way

For an item that should be handed to the contact, configure the FTB item task
as follows:

```snbt
{
  consume_items: false
  count: 4L
  item: { count: 1, id: "biohazard:documents" }
  only_from_crafting: true
  tags: ["biohazard_radio_submit"]
  type: "item"
}
```

`consume_items: false` is intentional. FTB Quests must not take the items by
itself. The transmitter turn-in handles every tagged item task as one atomic
transaction: it verifies all requirements first, then consumes all of them.
If even one requirement is missing, it consumes nothing.

Keep `only_from_crafting: true` so FTB Quests shows the requirement but does
not auto-submit it from the player's inventory. Do not enable `task_screen_only`.
Every physical item that belongs in the radio turn-in needs the submit tag.
An untagged FTB item task is a normal FTB task and uses its normal completion
behavior, which is rarely what a courier contract intends.

## Add the courier reward

Create a **Custom** reward and set `auto: "enabled"`. Give it an icon that
represents the expected shipment, then add one delivery type tag, one manifest
tag, and one category tag.

For a standard fixed or random package:

```text
biohazard_radio_delivery
biohazard_manifest_field_medical_cache
biohazard_category_medical
```

For a selection package, where the player chooses one result when the courier
arrives:

```text
biohazard_radio_choice_delivery
biohazard_manifest_rook_weapon_choice
biohazard_choice_count_3
biohazard_category_firearm
```

Use only one of `biohazard_radio_delivery` and
`biohazard_radio_choice_delivery`. The latter presents distinct choices from
the same loot table; it does not deliver all candidates. Choice counts are 1
through 9, with 3 used when no count tag is supplied.

Supported category suffixes are `supplies`, `ammunition`, `medical`,
`equipment`, and `firearm`. If you omit the category, it falls back to
`supplies`. The manifest suffix is case-sensitive and must match its loot-table
filename exactly, without `.json`.

## Create the shipment manifest

For a manifest tag named `biohazard_manifest_field_medical_cache`, create:

```text
src/main/resources/data/biohazard/loot_table/quest_delivery/field_medical_cache.json
```

This minimal manifest always sends four purified water bottles:

```json
{
  "pools": [
    {
      "rolls": 1,
      "entries": [
        {
          "type": "minecraft:item",
          "name": "toughasnails:purified_water_bottle",
          "functions": [
            {
              "function": "minecraft:set_count",
              "count": 4
            }
          ]
        }
      ]
    }
  ],
  "random_sequence": "biohazard:quest_delivery/field_medical_cache"
}
```

For a weighted one-of-many shipment, use one pool with `rolls: 1` and several
entries. For a package that intentionally includes several items, use multiple
pools. Standard Minecraft loot-table functions and conditions work, and all
item IDs must exist on both the server and client.

The table is rolled when the reward is claimed, not when it is collected. The
resulting stacks are saved per player, so later loot-table changes cannot reroll
an already-earned delivery.

## Worked example: Voss tissue analysis

Create a repeatable Medic quest with a modest repeat cooldown.

- Add `biohazard_radio_accept` to the first custom task.
- Add a kill task for 10 infected targets, if desired.
- Add an item task requiring 6 `biohazard:infected_tissue` with
  `biohazard_radio_submit`, `consume_items: false`, and
  `only_from_crafting: true`.
- Add the `biohazard_radio_complete` custom task.
- Add an auto-enabled custom reward tagged:

  ```text
  biohazard_radio_delivery
  biohazard_manifest_voss_analysis_cache
  biohazard_category_medical
  ```

- Create `quest_delivery/voss_analysis_cache.json` with a sensible mix of
  medical supplies, such as purified water, bandages, or a low-weight antiviral.

The same structure works for Ward, Rook, and Vale. Change the evidence,
field objective, category, and manifest to match the contact's role.

## Make it a shipped default

The source defaults live in:

```text
src/main/resources/biohazard/ftbquests_defaults/
```

Each chapter is an SNBT file, for example `chapters/medic.snbt` or
`chapters/arms_broker.snbt`. After editing in game, use **Save on server now**,
then copy the resulting quest data into the matching source default. Add its
player-facing strings to `lang/en_us.snbt` in the same defaults directory.

Biohazard only installs its defaults when the target quest directory is missing
or empty. It never merges or overwrites an existing quest book. To test a new
default install, use a fresh instance or deliberately move aside the test
instance's `config/ftbquests/quests` directory after confirming its exact path.
Do not expect rebuilding the mod to replace a live quest book automatically.

## Test every contract before shipping

1. Verify accept fails away from a transmitter and succeeds near a calibrated one.
2. Verify normal field objectives track only after acceptance.
3. Verify turn-in fails with missing evidence and consumes nothing.
4. Verify a successful turn-in removes every submitted item exactly once.
5. Verify the delivery uses the intended category delay.
6. Restart the server while a package is in transit, then verify it remains.
7. Verify collection works from another calibrated transmitter.
8. Fill the inventory before collection and verify remaining items stay in the mailbox.
9. For choice deliveries, verify the offered options are distinct and only the selected one is delivered.

## Fast troubleshooting

| Symptom | Most likely cause |
|---|---|
| Accept or turn-in does nothing | Missing/misspelled accept or complete tag, or no calibrated transmitter in range. |
| Turn-in says requirements are missing | One required item task lacks the item, count, submit tag, or correct FTB item configuration. |
| Reward claims but no package appears | Missing manifest tag, filename mismatch, invalid/empty loot table, or server log error. |
| Package arrives immediately or too slowly | Wrong/missing category tag or a changed server delivery-time configuration. |
| Choice screen never opens | Used normal delivery tag instead of `biohazard_radio_choice_delivery`. |
| Source change does not appear in game | You edited the packaged default, but the instance already has a populated live FTB quest directory. |




cheat sheat for new quests.
example - quartermaster new task:

The safest workflow is:
Open the FTB Quests editor.
Duplicate an existing Quartermaster quest, or create a new quest/task/reward.
Let FTB Quests generate the IDs.
Edit the title, description, item requirements, tags, and reward manifest.
Use Save on server now.
Copy the generated SNBT into your source quartermaster.snbt if this should become a default quest.