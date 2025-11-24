/// src/main/java/dev/badkraft/aurora/Loader.java
///
/// Copyright (c) 2025 Quantum Override. All rights reserved.
/// Author: The Badkraft
/// Date: November 12, 2025
///
/// MIT License
/// Permission is hereby granted, free of charge, to any person obtaining a copy
/// of this software and associated documentation files (the "Software"), to deal
/// in the Software without restriction, including without limitation the rights
/// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
/// copies of the Software, and to permit persons to whom the Software is
/// furnished to do so, subject to the following conditions:
/// The above copyright notice and this permission notice shall be included in all
/// copies or substantial portions of the Software.
/// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
/// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
/// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
/// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
/// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
/// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
/// SOFTWARE.
package dev.badkraft.aurora;

import dev.badkraft.aurora.auth.MinecraftAuth;
import com.google.gson.*;
import dev.badkraft.aurora.mapping.MappingBuilder;
import dev.badkraft.anvil.api.*;

import java.io.*;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.util.*;

import static dev.badkraft.aurora.utils.AuroraLogger.debug;
import static dev.badkraft.aurora.utils.AuroraLogger.info;
import static dev.badkraft.aurora.utils.Directories.*;

public class Loader {
    private static final String AMVP_VERSION = "0.3.1";
    public  static final String MC_VERSION = "1.21.10";
    private static final Gson GSON = new GsonBuilder().create();
    private static final boolean DEBUG = Boolean.getBoolean("aurora.debug");
    private static final String AURORA_MAPPINGS = MAPPINGS_DIR.resolve("mc-" + MC_VERSION + "-official.aurora").toString();

    // Logging
    public static void log(String msg, Object ... args) {
        info(msg, args);
    }

    public static void main(String[] args) throws Exception {
        boolean vanilla = false;
        boolean buildMaps = false;
        Path mappingsFile = Paths.get(AURORA_MAPPINGS);
        boolean hasMappings = Files.exists(mappingsFile);

        // Validate system properties
        log("minecraft.version: %s", System.getProperty("minecraft.version"));
        log("minecraft.client.version: %s", System.getProperty("minecraft.client.version"));
        log("mc.version: %s", System.getProperty("mc.version"));
        log("mcp.version: %s", System.getProperty("mcp.version"));

        for (String arg : args) {
            if ("--vanilla".equals(arg)) vanilla = true;
            if ("--build-maps".equals(arg)  || !hasMappings) buildMaps = true;
        }

        Path dotMinecraft = DOT_MINECRAFT_DIR;
        if (buildMaps) {
            if (!hasMappings) {
                log("No mappings found at %s — generating...", mappingsFile);
            } else {
                log("`--build-maps` requested — generating mappings...");
            }
            if (dotMinecraft == null) {
                throw new IllegalStateException(".minecraft not found — cannot build mappings");
            }
            Path vanillaJar = dotMinecraft
                    .resolve("versions")
                    .resolve(MC_VERSION)
                    .resolve(MC_VERSION + ".jar");
            if (!Files.exists(vanillaJar)) {
                throw new FileNotFoundException("Vanilla JAR not found: " + vanillaJar);
            }

            MappingBuilder.generateMappings(vanillaJar, mappingsFile);
            log("[Aurora] Mappings generated → %s", mappingsFile);
            if (buildMaps) {
                log("[Aurora] --build-maps complete. Exiting.");
                return;
            }
        }

        if (vanilla) {
            // --vanilla: launch clean Minecraft
            launchMinecraft();
            return;
        }

        // Normal launch with mappings (if needed)
        Path auroraRoot = Paths.get(System.getProperty("aurora.home", "aurora-mvp")).toAbsolutePath();
        if (dotMinecraft == null) {
            throw new IllegalStateException("Could not find .minecraft directory");
        }

        Path vanillaJar = dotMinecraft.resolve("versions").resolve(MC_VERSION).resolve(MC_VERSION + ".jar");

        // TODO: Load mappings into MethodHandle cache when ready
        // ReflectionMapper.buildCache(mappingsFile);

        launchMinecraft();
    }

    private static void launchMinecraft() throws Exception {
        log("Minecraft: Aurora :: Launcher %s+%s", AMVP_VERSION, MC_VERSION);

        // Build paths, symlink assets, get minecraft.jar
        LaunchPaths paths = LaunchPaths.build();
        String classpath = buildClasspathFromVersionJson(paths.versionJson(), paths.dotMinecraft());
        String[] entries = classpath.split(":");
        log("Classpath appended %d entries]", entries.length);
        debug("Classpath entries: %s", Arrays.toString(entries));

        // Build classloader
        List<URL>urls = new ArrayList<>();
        for (String path : entries) {
            urls.add(new File(path).toURI().toURL());
        }
       URLClassLoader auroraMCLoader = getAuroraClassLoader(urls);

        // Build launch args
        List<String> launchArgs = buildLaunchArgs(paths, auroraMCLoader);
        // Launch Minecraft
        log("Launching ...");
        try{
            // Launch using the custom classloader
            Class<?> mainClass = auroraMCLoader.loadClass("net.minecraft.client.main.Main");
            Method mainMethod = mainClass.getMethod("main", String[].class);
            mainMethod.invoke(null, (Object) launchArgs.toArray(new String[0]));
        }
        catch (Exception e) {
            log("Exception during launch: %s", e);

            e.printStackTrace();
        }
    }
    private record Session(
            String accessToken,
            String username,
            String uuid,
            String clientId,
            String xuId) {}
    private static Session loadSession() throws Exception {
        Path config = Paths.get("config.aurora");
        // if config.aurora does not exist, perform login
        if (!Files.exists(config)) {
            log("No login found. Starting MS Login ...");
            MinecraftAuth.loginAndSave(); // TODO: Re-enable when ready
        }

        // and the file will be created
        AnvilModule module = Anvil.parse(config);
        AnvilObject auth = module.getObject("auth").asObject();

        String expiresAtStr = auth.getString("expires_at");
        if (expiresAtStr != null) {
            // this step is the first time we need to persist changes back to config.aurora
            long expiresAt = Long.parseLong(expiresAtStr);
            long now = System.currentTimeMillis() / 1000;
            if (now >= expiresAt) {
                log("Token expired. Refreshing...");
                MinecraftAuth.refreshSession();
                // this is where we need to be able to update the value and save
                //content = Files.readString(config); // Re-read after refresh
            }
        }

        return new Session(
                auth.getString("access_token"),
                auth.getString("username"),
                auth.getString("uuid").replace("-", ""),
                auth.getString("client_id"),
                auth.getString("xuid")
        );
    }
    private static String buildClasspathFromVersionJson(Path versionJson, Path dotMinecraft) throws Exception {
        JsonObject json = GSON.fromJson(Files.newBufferedReader(versionJson), JsonObject.class);
        JsonArray libs = json.getAsJsonArray("libraries");
        List<String> cp = new ArrayList<>();
        cp.add(versionJson.getParent().resolve(MC_VERSION + ".jar").toString());

        Path libsDir = dotMinecraft.resolve("libraries");
        for (JsonElement e : libs) {
            JsonObject lib = e.getAsJsonObject();
            if (!lib.has("downloads") || !lib.has("name")) continue;
            JsonObject artifact = lib.getAsJsonObject("downloads").getAsJsonObject("artifact");
            if (artifact == null) continue;
            String path = artifact.get("path").getAsString();
            Path libJar = libsDir.resolve(path);
            if (Files.exists(libJar)) {
                cp.add(libJar.toString());
            }
        }
        return String.join(":", cp);
    }
    private static void symlinkIfNeeded(Path link, Path target, String name) throws IOException {
        if (Files.exists(link)) {
            if (Files.isSymbolicLink(link) && Files.readSymbolicLink(link).equals(target)) return;
            Files.delete(link);
        }
        log("Creating symlink: %s -> %s", link, target);
        Files.createSymbolicLink(link, target);
    }
    private static List<String> loadVersionArgs(JsonObject versionJson) {
        JsonArray game = versionJson
                .getAsJsonObject("arguments")
                .getAsJsonArray("game");

        List<String> versionArgs = new ArrayList<>();
        Iterator<JsonElement> iterator = game.iterator();
        //noinspection WhileLoopReplaceableByForEach
        while (iterator.hasNext()) {
            JsonElement element = iterator.next();
            if (element.isJsonPrimitive()) {
                versionArgs.add(element.getAsString());
            }
        }
        return versionArgs;
    }
    private static List<String> buildLaunchArgs(LaunchPaths paths, ClassLoader auroraMCLoader) throws Exception {
        ClassLoader original = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(auroraMCLoader);
            Session session = loadSession();

            JsonObject versionInfo = GSON.fromJson(Files.newBufferedReader(paths.versionJson()), JsonObject.class);
            String assetIndex = versionInfo.getAsJsonObject("assetIndex").get("id").getAsString();

            List<String> args = loadVersionArgs(versionInfo);
            log("Loaded %d version arguments", args.size());

            for (int i = 0; i < args.size(); i++) {
                String value = args.get(i);
                switch (value) {
                    case "${version_name}"       -> args.set(i, MC_VERSION);
                    case "${version_type}"       -> args.set(i, "release");
                    case "${auth_player_name}"   -> args.set(i, session.username.replace("\"", ""));
                    case "${auth_uuid}"          -> args.set(i, session.uuid.replace("\"", ""));
                    case "${auth_access_token}"  -> args.set(i, session.accessToken.replace("\"", ""));
                    case "${user_type}"          -> args.set(i, "msa");
                    case "${clientid}"           -> args.set(i, "00000000441cc96b");           // REQUIRED
                    case "${auth_xuid}"          -> args.set(i, session.xuId.replace("\"", ""));            // optional but nice
                    case "${game_directory}"     -> args.set(i, paths.gameDir().toString());
                    case "${assets_root}"        -> args.set(i, paths.assetsRoot().toString()); // ← CORRECT
                    case "${assets_index_name}"  -> args.set(i, assetIndex);
                    case "${user_properties}"    -> args.set(i, "{}");
                }
            }

            // Log final args
            for (int i = 0; i < args.size(); i += 2) {
                String key = args.get(i);
                String val = i + 1 < args.size() ? args.get(i + 1) : "";
                if (key.startsWith("--accessToken") || key.contains("Token")) {
                    val = val.substring(0, 20) + "...";
                }
                log("Arg: %s = %s", key, val);
            }

            return args;
        } finally {
            Thread.currentThread().setContextClassLoader(original);
        }
    }
    private record LaunchPaths(
            Path dotMinecraft,     // ~/.minecraft
            Path gameDir,          // run/minecraft
            Path assetsRoot,       // run/minecraft/assets → symlink
            Path versionJson       // ~/.minecraft/versions/1.21.10/1.21.10.json
    ) {
        static LaunchPaths build() throws IOException {
            Path dotMinecraft = DOT_MINECRAFT_DIR;
            if (dotMinecraft == null) {
                throw new IllegalStateException("Could not find .minecraft directory");
            }

            Path gameDir = RUN_DIR;
            Path assetsRoot = gameDir.resolve("assets");
            Path versionJson = dotMinecraft
                    .resolve("versions")
                    .resolve(MC_VERSION)
                    .resolve(MC_VERSION + ".json");

            if (!Files.exists(versionJson)) {
                throw new IllegalStateException("Missing version JSON: " + versionJson);
            }

            // Clean run dir ... not unless we want to do something like resetting game directory
//            if (Files.exists(gameDir)) {
//                try (var stream = Files.walk(gameDir)) {
//                    stream.sorted(Comparator.reverseOrder())
//                            .map(Path::toFile)
//                            .forEach(File::delete);
//                } catch (Exception ignored) {}
//            }

            Files.createDirectories(gameDir);
            Files.createDirectories(assetsRoot);

            // ONE symlink: entire assets folder
            symlinkIfNeeded(assetsRoot, dotMinecraft.resolve("assets"), "assets");

            log("Game directory: %s", gameDir);
            log("Assets root: %s → %s", assetsRoot, Files.readSymbolicLink(assetsRoot));
            log("Version JSON: %s", versionJson);

            return new LaunchPaths(dotMinecraft, gameDir, assetsRoot, versionJson);
        }
    }
    private static URLClassLoader getAuroraClassLoader(List<URL> urls) throws Exception {
        List<URL> allUrls = new ArrayList<>(urls);

        // If we're running from a jar, add it to the classpath
        URL selfUrl = Loader.class.getProtectionDomain().getCodeSource().getLocation();
        if (selfUrl.getPath().endsWith(".jar")) {
            allUrls.add(selfUrl);
            debug("Added self JAR to classloader: %s", selfUrl);
        }

        URLClassLoader minecraftClassLoader = new URLClassLoader(allUrls.toArray(new URL[0]), null) {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (name.startsWith("net.minecraft.") || name.startsWith("com.mojang.")) {
                    synchronized (getClassLoadingLock(name)) {
                        forceMinecraftVersion();
                    }
                }
                return super.loadClass(name, resolve);
            }
        };

        Thread.currentThread().setContextClassLoader(minecraftClassLoader);
        return minecraftClassLoader;
    }
    private static void forceMinecraftVersion() {
        try {
            // Try to set Minecraft's internal version through reflection
            Class<?> sharedConstants = Class.forName("net.minecraft.SharedConstants");
            java.lang.reflect.Field versionField = sharedConstants.getDeclaredField("VERSION");
            versionField.setAccessible(true);

            // Get the version object and ensure it's initialized
            Object version = versionField.get(null);
            if (version != null) {
                return; // Version already set
            }
        } catch (Exception e) {
            // Fall back to system properties
            System.setProperty("minecraft.version", MC_VERSION);
        }
    }
    // extracts values from anvil model
    public static String extractValue(String content, String key) {
        var pattern = java.util.regex.Pattern.compile(key + "\\s*:=\\s*\"([^\"]+)\"");
        var matcher = pattern.matcher(content);
        return matcher.find() ? matcher.group(1) : null;
    }
}