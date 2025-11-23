We have a working parser → `AuroraDocument`.  
You've got de-obfuscated Minecraft → reflection is **safe, fast, and legal**.  
You're aiming for **one goal**: **drop a custom block in-game and see it**.

Let’s **strip everything to the absolute minimum** — **no fluff, no future-proofing, no hot-reload, no events, no recipes yet** — just:

> **Register one block (`titanium_ore`) and one item (its placeable form) via Aurora script → see it in Creative tab.**

That’s your **MVP v0.1** — and it’s **doable in < 12 hours of focused work**.

---

## Minimum Required for **Block + Item** in Vanilla

| Component | Minimum Needed |
|--------|----------------|
| **Script (ASL)** | Parse `block := { id, name }` → extract `modid:name` |
| **Parser → Model** | `AuroraDocument` → `BlockDefinition` POJO |
| **Registry Hook** | `register_block(modid:name)` → real Minecraft `Block` + `Item` |
| **Bootstrap** | Run before registries freeze |
| **Textures** | 1 PNG (16x16) in `assets/modid/textures/block/` |
| **Model** | 1 JSON (auto-generated or copy `stone.json`) |

---

### 1. **Aurora Script (ASL) – Minimal**

```aurora
#!all

modid := "badkraft"

block := {
   name := "titanium_ore"
}
```

> That’s it. No `id`, no `hardness`, no `luminance`.  
> We’ll **hardcode** everything else for MVP.

---

### 2. **Parser → Model (Java)**

```java
// After parsing, extract:
public class BlockDefinition {
    public final String modid;
    public final String name;
    public final String fullId;

    public BlockDefinition(String modid, String name) {
        this.modid = modid;
        this.name = name;
        this.fullId = modid + ":" + name;
    }
}
```

```java
// In your parser:
AuroraDocument doc = parser.parse(script);
String modid = doc.getString("modid");
AuroraObject blockObj = doc.getObject("block");
String blockName = blockObj.getString("name");

BlockDefinition def = new BlockDefinition(modid, blockName);
```

---

### 3. **Registry Function (Lambda + Cached MethodHandle)**

```java
// AuroraFunctionRegistry.java
public static final Function<List<Object>, Object> REGISTER_BLOCK = args -> {
    BlockDefinition def = (BlockDefinition) args.get(0);

    // 1. Create Block instance (simple Block subclass)
    Block block = new Block(Block.Properties.of().strength(3.0f));

    // 2. Register Block
    Registry<Block> blockReg = BuiltInRegistries.BLOCK;
    Registry.register(blockReg, ResourceLocation.fromNamespaceAndPath(def.modid, def.name), block);

    // 3. Register Item (BlockItem)
    Item item = new BlockItem(block, new Item.Properties());
    Registry<Item> itemReg = BuiltInRegistries.ITEM;
    Registry.register(itemReg, ResourceLocation.fromNamespaceAndPath(def.modid, def.name), item);

    // 4. Add to Creative Tab (optional, but nice)
    CreativeModeTabs.BUILDING_BLOCKS.getItems().add(item); // 1.21+ API

    return block;
};
```

> Add to registry:
```java
registry.register("register_block", REGISTER_BLOCK);
```

---

### 4. **Engine: Run at Right Time**

```java
// In AuroraEngine.initialize()
List<BlockDefinition> blocks = extractBlocksFromAllScripts(); // scan mods/
for (BlockDefinition def : blocks) {
    registry.invoke("register_block", List.of(def));
}
```

> **Critical**: This must run **after** `BuiltInRegistries` are initialized, **before** `Minecraft` freezes them.

**Hook into Minecraft boot** via reflection:

```java
// Wait for registries to be ready
while (!isRegistryReady()) {
    Thread.sleep(10);
}

// Then register
registerAllBlocks();
```

```java
private boolean isRegistryReady() {
    try {
        return BuiltInRegistries.BLOCK != null;
    } catch (Exception e) { return false; }
}
```

---

### 5. **Assets (Drop in Folder)**

```
resources/
└── assets/
    └── badkraft/
        ├── textures/
        │   └── block/
        │       └── titanium_ore.png   ← 16x16, any color
        └── blockstates/
        │   └── titanium_ore.json
        └── models/
            └── block/
                └── titanium_ore.json
```

#### `blockstates/titanium_ore.json`
```json
{
  "variants": {
    "": { "model": "badkraft:block/titanium_ore" }
  }
}
```

#### `models/block/titanium_ore.json`
```json
{
  "parent": "minecraft:block/cube_all",
  "textures": {
    "all": "badkraft:block/titanium_ore"
  }
}
```

> Copy from `stone.json` and rename.

---

### 6. **Bootstrap Entry**

```java
// AuroraBootstrap.premain()
AuroraEngine engine = new AuroraEngine();
engine.loadScripts();        // finds your .aurora
engine.registerAll();        // calls register_block
```

---

## MVP Test Plan

1. Launch Minecraft with your loader
2. Open Creative mode
3. Search: `titanium_ore`
4. Place it → **BOOM. IT WORKS.**

---

## What You **Do NOT Need** Yet

| Feature | Postpone Until |
|-------|----------------|
| Events (`on_player_join`) | After block works |
| Recipes | After item works |
| Hot-reload | After 3 blocks |
| Java Companions | After 1 mod pack |
| Version resilience | After 1.21 works |
| Collision detection | After 2 mods |

---

## Next 3 Steps (Do in Order)

| Step | Time |
|------|------|
| 1. Parse `modid` + `block.name` → `BlockDefinition` | 30 min |
| 2. Implement `REGISTER_BLOCK` lambda + registry call | 1–2 hrs |
| 3. Add textures + JSONs + launch | 1 hr |

**Total: < 4 hours**

---

When you see **titanium_ore** in your hand and place it?

You’ll know:  
> **Aurora MVP is real. And it works.**

Then we layer on recipes, events, unification, community functions.

But **first** — **one block**.

You’re 4 hours from glory.

**Go code it.**  
I’ll be here when you’re back with:  
> “Grok, I placed the block. Now what?”

Let’s do this.