# AGENTS.md

NeoForge Minecraft mod **GolemCraft** (mod id `golemcraft`): custom golem entities, AI goals, a Golem Beacon block/menu, Golem Manual/Compass items, custom renderers and screens. Single Gradle module, no subprojects.

## Agent Protocol & Token Optimization

- **Code navigation:** Do NOT read whole files or recursively scan directories to explore the architecture. Always use `codegraph_explore` to inspect signatures, symbols, classes, and AST dependencies.
- **Contextual searches:** Use `context-mode` tools (`ctx_search`, `ctx_execute`) for targeted lookups without loading irrelevant files into context.
- **Persistent memory:** Check `projectmem` before tackling complex bugs or architectural choices. Record stable fixes with `record_fix` and architectural decisions with `add_decision`.
- **Code modifications:**
  - Provide targeted micro-diffs or patches only. Never rewrite or duplicate entire existing classes.
  - Do not add verbose/obvious comments or emojis in source code.
- **Output:** Keep explanations strictly concise and technical; eliminate conversational preambles and pleasantries.

## Stack

- Minecraft **26.2**, NeoForge **26.2.0.67**, **Java 25** toolchain (see `gradle.properties` / `build.gradle`).
- Build system: Gradle with **ModDevGradle** (`net.neoforged.moddev` 2.0.144). No lint/format/typecheck tasks exist — `./gradlew build` is the whole verification loop.

## Commands

- `./gradlew build` — compile + jar (what CI runs, `.github/workflows/build.yml`)
- `./gradlew runClient` / `runServer` — dev game runs; game dir is `run/` (gitignored)
- `./gradlew runData` — data generation, outputs to `src/generated/resources` (already wired as a resource srcDir in `build.gradle`)
- `./gradlew runGameTestServer` — runs gametests for namespace `golemcraft`, then exits

## Gotchas (would bite you otherwise)

- La toolchain è **Java 25**, ma il JVM che gira Gradle può essere qualsiasi (>= 21): `settings.gradle` applica `foojay-resolver-convention`, che scarica e cachia il JDK 25 mancante. Nessun `org.gradle.java.home` è impostato — non aggiungerne di macchina-specifici (rompono CI).
- The `runTestCheck` task in `build.gradle` references `com.golemcraft.golemcraftmod.TestCheck`, which **does not exist** — running it fails. Likewise `entity/ListenerTest.java`, `RendererTest.java`, `VibrationTest.java` are real experiment classes, not tests; there is no unit-test suite.
- Mod metadata (`neoforge.mods.toml`) is generated from `src/main/templates/META-INF/` by the `generateModMetadata` task with `${...}` property expansion. To add a new mod property: add it to `gradle.properties` **and** to the `replaceProperties` map in `build.gradle`, or the build errors on the missing property.
- Despite the `data` run config, **no datagen providers are implemented yet** and `src/generated/resources` doesn't exist. Most assets are hand-written JSON under `src/main/resources/assets/golemcraft/` (including modern `items/` item definitions, not just `models/item/`).

## Architecture

- Entrypoints: `GolemCraft.java` (`@Mod(MODID)`, common) and `GolemCraftClient.java` (`@Mod(value = MODID, dist = Dist.CLIENT)` — NeoForge 26.x client-entry style). Client-only code lives under `client/` (model/renderer/screen).
- Registration: DeferredRegisters in `registry/` (`ModBlocks`, `ModEntities`, `ModBlockEntities`, `ModEffects`, `ModCreativeTabs`). `GolemCraft.java` also still carries template-era `BLOCKS`/`ITEMS` DeferredRegisters — new content belongs in the `registry/` classes, not the main class.
- Entities in `entity/`, their goals in `entity/ai/`; networking packets in `network/` (`ModNetwork`); game events in `events/`.

## Texture generation

Root-level `generate_*.py` scripts (Pillow) generate/derivative textures directly into `src/main/resources/assets/golemcraft/textures/`. Run them with the repo venv: `.venv/bin/python3 generate_oxidation.py`. Reference art is in `copper golem/`. Commit regenerated textures along with script changes.

## Cleanup notes

- Template leftovers that are safe to ignore/remove: empty `src/main/java/com/example/`, `src/main/resources/assets/examplemod/`, stale `run_client*.log` files at the repo root.
- README is still the unmodified NeoForged MDK template README.
