# Aurora MVP – The Mod Loader That Ends All Mod Loaders

### For Mod Developers (and anyone tired of the old way)

| Problem Today | Aurora MVP |
|---------------|------------|
| You pick a loader (Forge / Fabric / Quilt / NeoForge) and you’re locked in forever. | **No loader dependency.** Aurora MVP **is** the loader. You ship one folder. That’s it. |
| You write Java (or Kotlin/Scala) and recompile every time Minecraft updates. | **No Java required.** You write declarative AML (Aurora Modelling Language) and optional ASL scripts. Zero recompiling. |
| You maintain separate mods for 1.12, 1.16, 1.21… | **One mod works on every version from 1.12 → 1.21+** automatically. Aurora handles the mapping differences at launch. |
| You ship JSON for blockstates, models, loot tables, lang files… | **You ship nothing.** All JSON is generated on-the-fly from your AML. No more copy-paste, no more version drift. |
| You fight with mixins, access wideners, and reflection that breaks every update. | **Safe, automatic reflection.** Aurora downloads Mojang’s official mappings, builds a perfect MethodHandle cache at launch, and calls the exact methods you need — no ASM, no crashes. |
| You have to learn a new loader API every time you switch ecosystems. | **One tiny, human-readable format.** Drop a folder in `aurora-mvp/mods/`, write a `manifest.aurora` and a few `.aml` files — done. |
| Your mod is a JAR that can conflict with other JARs. | Your mod is a **folder** with plain text files. No class-loader hell, no version conflicts, instant hot-reload. |

### How it actually works (the simple version)

1. You launch Minecraft with `aurora-loader.jar` as the main class (just like you do with Fabric or Forge today).  
2. Aurora checks for a mappings file. If missing, it downloads Mojang’s official ProGuard mappings and turns them into a clean AML file once.  
3. It scans `aurora-mvp/mods/` for any folder that contains a `manifest.aurora`.  
4. It parses your `.aml` files with the built-in Aurora engine, turns them into live Minecraft objects using the cached MethodHandles, and injects your assets from the `assets/` folder.  
5. Minecraft starts — you’re playing with your mod on any version.

### What this means for you as a mod author

- **Write once, run forever.** No more “1.20.1 port”, “1.21 port”, “Fabric vs Forge” branches.  
- **No Java knowledge required.** Designers, artists, or junior devs can create full blocks, items, and simple logic in plain English-like AML.  
- **Instant iteration.** Change a number → save file → the game updates in <1 second.  
- **Future-proof.** When Minecraft 1.22 drops, you just run the game once — Aurora rebuilds the mappings automatically. Your mod still works.  
- **Zero boilerplate.** No `modid.json`, no registration events, no `@ObjectHolder`, no `DeferredRegister`. Just describe what you want.

### Bottom line

Aurora MVP isn’t another loader you have to learn.  
It’s the **last** loader you’ll ever need.

Drop a folder. Write some text. Play on any version.  
That’s it.

We’re not asking you to switch ecosystems.  
We’re asking you to stop needing them.

Welcome to the future of Minecraft modding.  
Welcome to Aurora.
