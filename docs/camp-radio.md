# Camp Radio

Rotwire promotes a Radio Transmitter to a persistent Camp Radio when it is
placed beneath a complete Rotwire tarp or anywhere inside a supported
SimplyTents structure.

## Camp identity

The first shelter check assigns the radio a persistent camp UUID and records
the placing player's UUID as its initial owner. The identity remains attached
to the radio block entity if the shelter is temporarily dismantled, allowing
the Camp Hub to report an inactive camp without erasing future progression.
Breaking and replacing the radio creates a new camp identity.

The saved identity is the stable foundation for later team ownership,
progression, and modular extensions. It does not currently grant exclusive
access or install upgrades.

## Camp Hub

Using an established Camp Radio opens a server-backed Camp Hub. The status
panel refreshes once per second and reports:

- the shelter type containing the radio;
- the effective campsite coverage radius;
- a Traveler's Backpack sleeping bag sheltered by the same structure;
- a lit campfire or soul campfire within the campsite radius;
- a deployed Traveler's Backpack within the radius;
- whether one backpack contains a qualifying ration;
- the greatest total food nutrition found in one nearby backpack; and
- the Radio Transmitter's Survivor Network connection.

The camp is online only when every campsite requirement is present. Status
inspection never consumes food.

The Contracts button becomes available after radio calibration. It preserves
the existing radio workflow: ready deliveries are collected, city and weather
status are synchronized, and the FTB Quests Survivor Network opens.

Storage, crafting, and operations slots are visible as locked extension points.
They are intentionally placeholders for later module progression.

## Ordinary radios

A Radio Transmitter that has never been sheltered remains an ordinary radio
and retains its previous behavior. Once a radio receives a camp identity, it
continues to open the Camp Hub even if its shelter becomes invalid.

## Server authority

The server locates the shelter and validates every camp component. The client
receives only the synchronized status values required to render the screen.
Menu actions revalidate that the player is close to the same radio and that
the Survivor Network connection is active.
