# AGENTS.md

NeoForge Minecraft mod **GolemCraft** (mod id `golemcraft`): custom golem entities, AI goals, Golem Beacon block/menu, Golem Manual/Compass items, custom renderers and screens. Single Gradle module.

## Stack

- Minecraft **26.2**, NeoForge **26.2.0.67**, Java 25 toolchain (`gradle.properties` / `build.gradle`).
- Build: Gradle con **ModDevGradle** (`net.neoforged.moddev` 2.0.144). `./gradlew build` = compile + jar (CI).

## Commands

- `./gradlew build` — compile + jar (CI)
- `./gradlew runClient` / `runServer` — dev game; game dir `run/` (gitignored)
- `./gradlew runData` — datagen, outputs to `src/generated/resources`
- `./gradlew runGameTestServer` — gametests namespace `golemcraft`, exits

## Gotchas

- Java 25 toolchain, ma Gradle JVM può essere qualsiasi >= 21. `settings.gradle` usa `foojay-resolver-convention`. Nessun `org.gradle.java.home` — non aggiungerlo (rompe CI).
- Mod metadata (`neoforge.mods.toml`) generato da `src/main/templates/META-INF/` con `${...}` property expansion. Nuova proprietà: aggiungere in `gradle.properties` **e** `replaceProperties` in `build.gradle`.
- `src/generated/resources` non esiste di default (wiring in build.gradle ok). Assets scritti a mano sotto `src/main/resources/assets/golemcraft/` (incluso `items/` moderno).

## Architecture

- Entrypoints: `GolemCraft.java` (`@Mod(MODID)`, common), `GolemCraftClient.java` (`@Mod(value = MODID, dist = Dist.CLIENT)`). Client code sotto `client/`.
- Registration: DeferredRegisters in `registry/` (`ModBlocks`, `ModEntities`, `ModBlockEntities`, `ModEffects`, `ModCreativeTabs`). `GolemCraft.java` ha template-era `BLOCKS`/`ITEMS` DeferredRegisters — nuovo contenuto va in `registry/`.
- Entities: `entity/`, AI goals `entity/ai/`, networking `network/` (`ModNetwork`), events `events/`.

## Texture generation

Script Pillow root-level generano textures in `src/main/resources/assets/golemcraft/textures/`. Venv: `.venv/bin/python3 generate_oxidation.py`. Reference art: `copper golem/`. Commit textures rigenerate con script changes.
