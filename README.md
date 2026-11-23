# GeckoBetterFPS — Fabric 1.21.1

Performance mod for [GeckoLib](https://github.com/bernie-g/geckolib): animation tick
optimization, a Molang AST optimizer with bytecode compilation, bone render-plan
selection, and Sodium-compatible cube rendering.

## Requirements

| Dependency | Version |
|------------|---------|
| Minecraft  | 1.21.1  |
| Fabric Loader | 0.19.5+ |
| GeckoLib (Fabric) | 4.9.3 |
| Sodium (Fabric) | 0.6.13 |
| Iris (Fabric, optional) | 1.8.8 |

MixinLite 0.2.0 is bundled inside the jar at `META-INF/jars/` — no separate install needed.

## Build

```
gradlew build
```

Output: `build/libs/gbf-1.0.2.jar`

## Notes

- Built with Mojang official mappings (`loom.officialMojangMappings()`).
- `gbf.accesswidener` makes `BufferBuilder.format` accessible for the Sodium vertex path.
- Iris support is optional and detected at runtime via `FabricLoader.isModLoaded("iris")`.
