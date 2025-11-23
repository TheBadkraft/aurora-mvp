## Aurora MVP: Unified Technical Specification

**Executive Summary**

Aurora MVP (Modding Virtualization Platform) is a non-invasive, version-resilient Minecraft mod loader that creates a virtualization layer between mods and Minecraft, providing sustainable modding through abstraction and extensibility.

The platform represents three core principles:

· Modding - The primary purpose, enabling content creation and game modification
· Virtualization - Abstracting Minecraft internals to provide version resilience and stability
· Platform - Foundation for extensible modding that supports both simple scripts and complex Java extensions

This architecture combines a custom Aurora scripting language with Java companion support, using runtime mapping analysis and hot-reloadable components to create an ecosystem where mods survive across Minecraft versions. By leveraging officially published NeoForge mappings through proper attribution, Aurora provides a legal, ethical foundation for modern Minecraft modding that fundamentally changes the upgrade cycle from "mods break on update" to "mods adapt automatically."

The system operates through non-invasive class analysis, pattern-based method identification, and a version-aware function registry that translates Aurora language calls to appropriate Minecraft version APIs. This approach allows mod developers to write once and run across multiple Minecraft versions, while maintaining the full power of Java for complex modifications through the companion system.

**Core Architecture**

System Overview

```java
/**
 * Aurora Core Bootstrap
 * Non-invasive entry point using Java Instrumentation API
 */
public class AuroraBootstrap {
    private static AuroraEngine engine;
    
    public static void premain(String args, Instrumentation inst) {
        // Phase 1: Non-invasive setup
        inst.addTransformer(new AuroraClassAnalyzer());
        
        // Phase 2: Runtime mapping initialization
        MappingService mappingService = new NeoForgeMappingAdapter();
        AuroraFunctionRegistry registry = new VersionAwareFunctionRegistry();
        
        // Phase 3: Engine startup
        engine = new AuroraEngine(registry, mappingService);
        engine.initialize();
    }
}
```

Non-Invasive Class Analysis

```java
/**
 * Analyzes Minecraft classes without modification
 * Uses ASM for bytecode pattern recognition
 */
public class AuroraClassAnalyzer implements ClassFileTransformer {
    @Override
    public byte[] transform(ClassLoader loader, String className, 
                          Class<?> classBeingRedefined, 
                          ProtectionDomain protectionDomain, 
                          byte[] classfileBuffer) {
        // ANALYSIS ONLY - no bytecode modification
        ClassAnalysis analysis = BytecodeAnalyzer.analyze(classfileBuffer);
        PatternRegistry.registerPatterns(className, analysis);
        return null; // Return null means no transformation
    }
}
```

Aurora Language Specification (Pseudo-Code)

Basic Syntax Structure

```pseudo
// MOD DECLARATION
mod "mod_identifier" {
    version = "1.0.0"
    minecraft_versions = ["1.20", "1.21"]
    author = "ModAuthor"
    
    // INITIALIZATION BLOCK
    on_initialize {
        // VARIABLE DECLARATION
        let my_block = register_block("mymod:custom_block")
        let my_item = register_item("mymod:custom_item")
        
        // CONTROL FLOW
        if config.enable_feature {
            set_block_properties(my_block, {
                hardness: 2.0,
                resistance: 10.0,
                luminance: 5
            })
        }
        
        // LOOPS
        for color in ["red", "blue", "green"] {
            register_variant(my_item, color)
        }
    }
    
    // EVENT HANDLERS
    on_player_interact {
        if event.item == my_item && event.block == GRASS_BLOCK {
            spawn_particles(event.position, ParticleTypes.HAPPY_VILLAGER)
            play_sound(event.position, SoundEvents.ENTITY_VILLAGER_YES)
        }
    }
    
    on_block_placed {
        if event.block == my_block {
            schedule_timer(20, -> {
                explode(event.position, 3.0)
            })
        }
    }
}
```

Recipe System Example

```pseudo
mod "simple_crafts" {
    on_initialize {
        let emerald_sword = register_item("simple_crafts:emerald_sword")
        
        // SHAPED RECIPE
        add_shaped_recipe(emerald_sword, [
            [" E "],
            [" E "],
            [" S "]
        ], {
            'E': EMERALD,
            'S': STICK
        })
        
        // SHAPELESS RECIPE  
        add_shapeless_recipe(emerald_sword, [
            EMERALD, EMERALD, STICK
        ])
    }
}
```

Extensible Function Registry System

Core Registry Implementation

```java
/**
 * Version-aware function registry
 * Maps Aurora language functions to Minecraft operations
 */
public class AuroraFunctionRegistry {
    private final Map<String, AuroraFunction> functions = new ConcurrentHashMap<>();
    private final VersionAdapter versionAdapter;
    
    public void registerMinecraftAPI() {
        // BLOCK/ITEM FUNCTIONS
        register("register_block", new RegisterBlockAdapter());
        register("register_item", new RegisterItemAdapter());
        register("set_block_properties", new BlockPropertiesAdapter());
        
        // WORLD INTERACTION
        register("spawn_particles", new ParticleAdapter());
        register("play_sound", new SoundAdapter());
        register("get_players", new PlayerQueryAdapter());
        
        // RECIPE SYSTEM
        register("add_shaped_recipe", new ShapedRecipeAdapter());
        register("add_shapeless_recipe", new ShapelessRecipeAdapter());
        
        // EVENT SYSTEM
        register("on_event", new EventRegistrationAdapter());
    }
    
    public void register(String name, VersionAdapter adapter) {
        functions.put(name, new AuroraFunction(name, adapter));
    }
    
    public Object invoke(String functionName, List<Object> args) {
        AuroraFunction function = functions.get(functionName);
        if (function == null) {
            throw new AuroraRuntimeException("Unknown function: " + functionName);
        }
        return function.getAdapter().invokeForVersion(
            versionAdapter.getCurrentVersion(), args
        );
    }
}
```

Version Adapter Pattern

```java
/**
 * Adapts function calls to specific Minecraft versions
 */
public interface VersionAdapter {
    GameVersion[] getSupportedVersions();
    Object invokeForVersion(GameVersion version, List<Object> args);
}

/**
 * Example: Block registration across versions
 */
public class RegisterBlockAdapter implements VersionAdapter {
    private static final GameVersion[] SUPPORTED = {
        GameVersion.V1_20, GameVersion.V1_21
    };
    
    @Override
    public GameVersion[] getSupportedVersions() {
        return SUPPORTED;
    }
    
    @Override
    public Object invokeForVersion(GameVersion version, List<Object> args) {
        String blockId = (String) args.get(0);
        
        switch (version) {
            case V1_20:
                return registerBlock_1_20(blockId, args.subList(1, args.size()));
            case V1_21:
                return registerBlock_1_21(blockId, args.subList(1, args.size()));
            default:
                throw new UnsupportedVersionException(version);
        }
    }
    
    private Object registerBlock_1_20(String blockId, List<Object> properties) {
        // Use NeoForge mappings to find correct method
        String mappedClass = MappingService.getClassMapping("Block");
        String mappedMethod = MappingService.getMethodMapping("Blocks", "register");
        
        // Reflection-based invocation
        return ReflectionHelper.invokeStaticMethod(mappedClass, mappedMethod, blockId, properties);
    }
    
    private Object registerBlock_1_21(String blockId, List<Object> properties) {
        // Different registration system in 1.21
        String mappedClass = MappingService.getClassMapping("BlockRegistry");
        String mappedMethod = MappingService.getMethodMapping("BlockRegistry", "register");
        
        return ReflectionHelper.invokeStaticMethod(mappedClass, mappedMethod, blockId, properties);
    }
}
```

NeoForge Mapping Integration & Attribution

Legal Mapping Usage

```java
/**
 * NeoForge Mapping Adapter
 * LEGAL USAGE: Only uses officially published mapping files
 * ATTRIBUTION: This product uses mappings derived from NeoForge mappings
 *              (https://github.com/neoforged/NeoForge)
 */
public class NeoForgeMappingAdapter implements MappingService {
    private final MappingCache cache = new MappingCache();
    
    @Override
    public void initialize() {
        // Load from officially published NeoForge mapping files
        loadOfficialMappings();
        applyAttribution();
    }
    
    private void loadOfficialMappings() {
        // Load from Gradle cache - legally obtained mapping files
        Path gradleCache = Paths.get(
            System.getProperty("user.home"), 
            ".gradle", "caches", "neoform"
        );
        
        loadMappingsFromFiles(gradleCache);
    }
    
    private void applyAttribution() {
        AuroraLogger.info(
            "Aurora MVP uses method and field mappings derived from NeoForge mappings. " +
            "NeoForge mappings are provided under their respective licenses. " +
            "https://github.com/neoforged/NeoForge"
        );
    }
    
    @Override
    public String getClassMapping(String officialName) {
        return cache.resolveClass(officialName);
    }
    
    @Override
    public String getMethodMapping(String className, String methodDescriptor) {
        return cache.resolveMethod(className, methodDescriptor);
    }
}
```

Pattern-Based Fallback System

```java
/**
 * When mappings are incomplete, use behavioral patterns
 */
public class PatternBasedResolver {
    public String identifyClassByPattern(Class<?> targetClass) {
        // Analyze class behavior rather than relying on names
        ClassPattern pattern = analyzeClassPattern(targetClass);
        
        // Match against known Minecraft archetypes
        return matchAgainstArchetypes(pattern);
    }
    
    private ClassPattern analyzeClassPattern(Class<?> clazz) {
        ClassPattern pattern = new ClassPattern();
        
        // Analyze methods
        for (Method method : clazz.getDeclaredMethods()) {
            pattern.addMethodSignature(
                method.getName(),
                method.getParameterCount(),
                method.getReturnType().getSimpleName()
            );
        }
        
        // Analyze fields
        for (Field field : clazz.getDeclaredFields()) {
            pattern.addField(field.getType().getSimpleName());
        }
        
        return pattern;
    }
}
```

Java Companion System

Companion Interface

```java
/**
 * Interface for Java-based mod companions
 * Allows complex logic in Java while exposing simple Aurora API
 */
public interface AuroraCompanion {
    /**
     * Register Java functions as Aurora language functions
     */
    void registerFunctions(AuroraFunctionRegistry registry);
    
    /**
     * Called when mod is initialized
     */
    void onInitialize();
    
    /**
     * Called when mod is unloaded
     */
    default void onUnload() {}
    
    /**
     * Provide custom configuration for the mod
     */
    default AuroraConfig getConfig() {
        return AuroraConfig.DEFAULT;
    }
}
```

Example Java Companion

```java
/**
 * Complex physics mod with Java backend, Aurora frontend
 */
public class PhysicsCompanion implements AuroraCompanion {
    private PhysicsEngine engine;
    
    @Override
    public void registerFunctions(AuroraFunctionRegistry registry) {
        // Expose complex Java functionality to Aurora scripts
        registry.register("create_physics_body", this::createPhysicsBody);
        registry.register("apply_force", this::applyForce);
        registry.register("simulate_physics", this::simulatePhysics);
    }
    
    @Override
    public void onInitialize() {
        // Heavy initialization in Java
        this.engine = new AdvancedPhysicsEngine();
        engine.initialize();
    }
    
    private Object createPhysicsBody(List<Object> args) {
        Vector3d position = (Vector3d) args.get(0);
        PhysicsProperties properties = (PhysicsProperties) args.get(1);
        
        PhysicsBody body = engine.createBody(position, properties);
        return body.getId();
    }
    
    private Object applyForce(List<Object> args) {
        String bodyId = (String) args.get(0);
        Vector3d force = (Vector3d) args.get(1);
        
        PhysicsBody body = engine.getBody(bodyId);
        body.applyForce(force);
        return null;
    }
}
```

Hot-Reload System

Dynamic Script Reloading

```java
/**
 * Manages hot-reload of Aurora scripts and Java companions
 */
public class HotReloadManager {
    private final WatchService watchService;
    private final Map<String, ModContainer> loadedMods = new ConcurrentHashMap<>();
    
    public void initialize() {
        // Watch for file changes
        setupFileWatchers();
        
        // Setup periodic mapping refresh
        setupMappingRefresh();
    }
    
    private void setupFileWatchers() {
        Thread watcherThread = new Thread(() -> {
            while (true) {
                WatchKey key = watchService.take();
                for (WatchEvent<?> event : key.pollEvents()) {
                    handleFileChange(event);
                }
                key.reset();
            }
        });
        watcherThread.setDaemon(true);
        watcherThread.start();
    }
    
    private void handleFileChange(WatchEvent<?> event) {
        Path changedFile = (Path) event.context();
        
        if (changedFile.toString().endsWith(".aurora")) {
            // Reload Aurora script
            reloadAuroraScript(changedFile);
        } else if (changedFile.toString().endsWith(".jar")) {
            // Reload Java companion
            reloadJavaCompanion(changedFile);
        }
    }
    
    private void reloadAuroraScript(Path scriptFile) {
        String modId = extractModId(scriptFile);
        ModContainer mod = loadedMods.get(modId);
        
        if (mod != null) {
            mod.stop();
            mod.loadNewScript(scriptFile);
            mod.start();
            AuroraLogger.info("Hot-reloaded Aurora script: " + modId);
        }
    }
}
```

Version Resilience System

Automatic Version Detection

```java
/**
 * Detects and adapts to different Minecraft versions
 */
public class VersionDetectionSystem {
    public GameVersion detectVersion() {
        // Multiple detection strategies
        return firstNonNull(
            detectFromPackageStructure(),
            detectFromClassSignatures(),
            detectFromManifest(),
            GameVersion.UNKNOWN
        );
    }
    
    private GameVersion detectFromClassSignatures() {
        try {
            // Try to load version-specific classes
            if (classExists("net.minecraft.world.item.ItemStack")) {
                return GameVersion.V1_20;
            }
            if (classExists("net.minecraft.core.ItemStack")) {
                return GameVersion.V1_21;
            }
        } catch (Exception e) {
            // Fall through to next detection method
        }
        return null;
    }
    
    public void handleVersionChange(GameVersion newVersion) {
        AuroraLogger.info("Minecraft version changed, adapting...");
        
        // Update all version-aware components
        functionRegistry.updateVersion(newVersion);
        mappingService.refreshMappings(newVersion);
        
        // Hot-reload all mods with new version context
        hotReloadManager.reloadAllMods();
    }
}
```

Development Roadmap

Phase 1: Core MVP (Weeks 1-4)

· Aurora language parser and interpreter
· Basic function registry
· Non-invasive class analysis
· NeoForge mapping integration

Phase 2: Minecraft Integration (Weeks 5-8)

· Block/Item registration system
· Event handling framework
· Recipe system
· Basic Java companion loader

Phase 3: Advanced Features (Weeks 9-12)

· Hot-reload system
· Version detection and adaptation
· Advanced event system
· Performance optimization

Phase 4: Tooling & Ecosystem (Weeks 13-16)

· Development tools
· Documentation generator
· Example mod library
· Community resources

Legal & Ethical Compliance

Mapping Usage Policy

· ✅ Only uses officially published mapping files
· ✅ Proper attribution to NeoForge and contributors
· ✅ No redistribution of Minecraft code
· ✅ Runtime-only usage, no permanent modification

License Compliance

```java
/**
 * Legal compliance and attribution
 */
public class LegalCompliance {
    public static final String ATTRIBUTION_TEXT = 
        "Aurora MVP uses mappings derived from NeoForge mappings. " +
        "NeoForge is licensed under its respective terms. " +
        "Minecraft is a trademark of Mojang Studios. " +
        "This project is not affiliated with Mojang or Microsoft.";
    
    public void displayAttribution() {
        AuroraLogger.info(ATTRIBUTION_TEXT);
    }
}
```