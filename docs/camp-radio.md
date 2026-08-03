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

## City settlements and relays

Once a complete Camp Radio has finished mapping a Lost Cities zone, it becomes
that city's **primary hub**. The owner is prompted to name the settlement from
the Camp Hub. The name is attached to the city zone—not to a particular player
or block position—so it remains the city identity used by future survivor,
fast-travel, and siege systems.

The first completed camp in a mapped city stays primary. Any later sheltered
radio mapped to the same city is a **relay**: it retains its own private cache
and installed camp modules, but reports into the same settlement population,
rations, city-wide upgrades, and siege state. The Camp Hub identifies whether
the current radio is the primary hub or a relay and shows the shared radio and
resource totals.

Breaking the primary radio marks that primary hub destroyed rather than quietly
promoting a relay. This keeps future travel destinations and siege targets
stable; rebuilding/recovery rules can be added deliberately instead of letting
block replacement move them unexpectedly.

### Camp Hub and ration stockpile

The named primary hub can install a **Camp Hub Module** while its camp and
radio link are online. Craft the module from Documents, Research Data, a copper
block, and iron; use it directly on the primary Camp Radio. This unlocks the
city-wide ration stockpile for every connected primary or relay radio in that
settlement.

Every inventory-capable container inside an active campsite automatically
supplies its city's stockpile. This includes vanilla chests and barrels plus
Handcrafted desks, nightstands, cupboards, drawers, shelves, and other storage
that exposes a standard inventory. Each food stack contributes its Minecraft
nutrition value, so a loaf of bread supplies more rations than a weak snack.
Food remains in its original container until daily upkeep needs it. When a
whole item supplies more hunger points than the current day needs, its unused
value becomes prepared settlement rations rather than being wasted. Overlapping
camp zones never count the same container twice, and returned food containers
such as bowls are put back into the source inventory or dropped beside it if
there is no room.

The panel shows the shared ration total, number of food-supplying containers,
and daily usage. Active camp zones are rescanned on the server at the configured
interval (five seconds by default); only currently loaded camp chunks can
contribute. Every civilian or guard consumes the configured number of hunger
points at each Minecraft-day boundary. There are no settlers yet, so a new
settlement shows zero daily use, but supplies can be prepared before recruitment
arrives.

The saved identity owns modular progression and private infrastructure. The
placing player is the camp owner: only that player can install modules, use the
workshop, or open the Quartermaster Cache. Breaking the radio removes the camp
identity but safely drops every installed module and cached item.

## Camp Hub

Using an established Camp Radio opens a server-backed Camp Hub. The status
panel refreshes once per second and reports:

- the shelter type containing the radio;
- the effective campsite coverage radius;
- a Traveler's Backpack sleeping bag sheltered by the same structure;
- a lit campfire or soul campfire within the campsite radius;
- at least one inventory-capable block within the radius;
- whether nearby containers hold a qualifying ration;
- the total food nutrition across nearby containers; and
- the Radio Transmitter's Survivor Network connection.

The camp is online only when every campsite requirement is present. Status
inspection never consumes food.

The Contracts button becomes available after radio calibration. It preserves
the existing radio workflow: ready deliveries are collected, city and weather
status are synchronized, and the FTB Quests Survivor Network opens.

The first Survivor Network setup contract schedules the Starter Signal Cache
as a supplies delivery. In addition to food, water, and ammunition, that cache
contains one Rotwire Tarp. The normal supplies delay applies (120 seconds by
default), giving every new player an obtainable shelter for establishing a
Camp Radio and unlocking the trader network.

## Modular extensions

An established camp can install one extension in each of three categories.
Installation is performed by using the module item on the Camp Radio. Only
the player recorded as the camp owner can install or operate private camp
infrastructure, and installation requires both a complete campsite and a
calibrated Survivor Network connection.

- The **Quartermaster Cache** adds twenty-seven secure storage slots. Ready
  courier deliveries are inserted into the cache before the player's
  inventory. The cache remains accessible if the fire goes out or another
  campsite requirement becomes unavailable, so stored equipment is never
  trapped by an offline camp.
- The **Field Workshop** consumes one Field Repair Kit to restore one quarter
  of the maximum durability of the damaged item held in the owner's main
  hand. Repairs require the camp to remain online.
- The **Operations Relay** adds live weather, nearby hostile, mapped city
  danger, and courier counts to the Camp Hub. It also extends Survivor
  Network access from the transmitter's short interaction range to the
  complete active campsite radius.

Modules are permanent for the lifetime of the camp identity. Breaking the
radio creates a new identity as before, but every installed module and every
item in the Quartermaster Cache is dropped for recovery.

Repeatable Survivor Network contracts provide replacement modules through the
normal equipment-courier flow:

- Ward's **Camp Logistics** contract provides the Quartermaster Cache;
- Reed's **Field Workshop** contract provides the workshop hardware; and
- Vale's **Signal Uplink** contract provides the Operations Relay.

Each commission consumes recovered Documents, Research Data, or Encrypted
Intel alongside ordinary construction materials, so city exploration feeds
directly into camp development. The contracts repeat after a five-minute
cooldown, allowing destroyed or otherwise lost modules to be replaced.

## Supported field shelters

The Camp Radio recognizes a complete Rotwire Tarp and the SimplyTents Tunnel,
Wall, Canopy, Zip-Up, Small Tipi, Duo, Large, Tipi, and Yurt families. Variant
sizes establish a minimum effective campsite radius large enough to cover the
physical shelter. The radio and sleeping bag must resolve to the same shelter;
equipment merely placed beside a tent does not count as sheltered equipment.

The Patchouli Base Equipment entry shows the Zip-Up Tent, Large Tent, Canopy
Tent, and Canvas Wall recipes. The Sleep and Dawn and Radio Transmitter entries
provide illustrated setup and Camp Hub instructions.

## Ordinary radios

A Radio Transmitter that has never been sheltered remains an ordinary radio
and retains its previous behavior. Once a radio receives a camp identity, it
continues to open the Camp Hub even if its shelter becomes invalid.

## Server authority

The server locates the shelter and validates every camp component. The client
receives only the synchronized status values required to render the screen.
Menu actions revalidate that the player is close to the same radio and that
the Survivor Network connection is active.
