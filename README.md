# GolemCraft

**GolemCraft** is a NeoForge Minecraft mod that introduces customizable, automated copper and specialty golems designed to assist players in automation, combat, exploration, and resource management.

---

## Features

### Golem Variants
Starting from a crafted **Base Golem**, equip or specialize your golems into dedicated roles:
- **Flower Golem**: Cultivates, plants, and collects flowers and decorative flora.
- **Farmer Golem**: Tills farmland, harvests mature crops, replants seeds, and deposits yields into nearby storage containers.
- **Lumberjack Golem**: Automatically cuts down trees within its territory, plants saplings, and gathers timber.
- **Fisherman Golem**: Equips fishing rods, fishes in water bodies, and stores catches.
- **Soldier Golem**: Defends your base, uses melee weapons or bows to attack hostile mobs, and features guard/follow modes.
- **Depth Golem**: Adapted to deep caves, emitting sonic blasts and glowing in dark environments.
- **Explorer Golem**: Accompanies the player, tracks coordinates, marks paths, and recovers lost items upon death.
- **Leveler Golem (Golem Spianatore)**: Levels terrain within an area designated by **Area Markers**, excavating high ground with shovels and filling depressions with collected dirt.

### Mechanics & Automation
- **Oxidation Stages**: Golems gradually oxidize over time (`Unaffected` → `Exposed` → `Weathered` → `Oxidized`). Fully oxidized golems enter a stationary statue state until cleaned with an axe.
- **Charged Golems**: Exposure to lightning or special effects charges golems, significantly speeding up their tasks and empowering their combat capabilities.
- **Golem Beacon**: A specialized beacon block and menu providing area-wide enhancements, automation orders, and oxidation immunity.
- **Golem Compass**: Links directly to a specific golem's UUID to easily track its current location in the world.
- **Area Marker**: Defines custom rectangular boundaries and elevation levels for automation golems (such as the Leveler Golem).
- **Golem Manual**: In-game illustrated guide book detailing all golem recipes, mechanics, and usage instructions.

---

## Installation & Requirements

- **Minecraft**: 1.21.1 / 26.2 (as defined by NeoForge toolchain)
- **Mod Loader**: NeoForge
- **Java**: Java 21+ (Java 25 toolchain used for development)

---

## Development & Building

The project is managed with Gradle and ModDevGradle:

```bash
# Compile and build mod JAR
./gradlew build

# Launch client development environment
./gradlew runClient

# Launch dedicated server development environment
./gradlew runServer
```
