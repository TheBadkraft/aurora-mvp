Sample Anvil source:
```anvl
//	server.anvl
#!aml
@[version=2, experimental]

server @[core] := {
    name := "Anvil Survival"
    port := 25565
    motd := "Forged in fire."
}

world @[seed=1337] := {
    spawn @[respawn] := (0, 64, 0)
    rules @[hardcore] := [
        "pvp", "keepInventory=false", "naturalRegeneration=true"
    ]
}

motd @[pinned] := {
    readme := @md`*** # Anvil Server Forged in fire. Built to last.***`
}

player := ("Notch", 100, true)
```
... and example practical use:
```java
public final class AnvilServer {

    private final root config;

    private AnvilServer(root config) {
        this.config = config;
    }

    public static AnvilServer load(Path path) throws IOException {
        return new AnvilServer(Anvil.load(path));
    }

    // ──────────────────────────────────────────────────────────────
    // Real-world usage — the way a server actually reads its config
    // ──────────────────────────────────────────────────────────────

    public String serverName() {
        return config.node("server").get("name").asString();
    }

    public int port() {
        return config.node("server").get("port").asInt();
    }

    public String motd() {
        return config.node("server").get("motd").asString();
    }

    public long worldSeed() {
        return config.node("world").attribute("seed").asLong();
    }

    public Vector3i spawnPoint() {
        var spawn = config.node("world").get("spawn");
        return new Vector3i(
            spawn.get(0).asInt(),
            spawn.get(1).asInt(),
            spawn.get(2).asInt()
        );
    }

    public List<String> gameRules() {
        return config.node("world")
                     .get("rules")
                     .asArray()
                     .elements()
                     .stream()
                     .map(v -> v.asString())
                     .toList();
    }

    public String readmeMarkdown() {
        return config.node("motd")
                     .get("readme")
                     .asBlob()
                     .content();
    }

    public boolean isHardcore() {
        return config.node("world")
                     .get("rules")
                     .hasAttribute("hardcore");
    }

    public PlayerInfo player() {
        var p = config.node("player");
        return new PlayerInfo(
            p.get(0).asString(),           // name
            p.get(1).asInt(),              // health
            p.get(2).asBoolean()           // isFounder flag
        );
    }

    public boolean isExperimental() {
        return config.hasAttribute("experimental");
    }

    // ──────────────────────────────────────────────────────────────
    // Tiny value objects — because sometimes you want a real type
    // ──────────────────────────────────────────────────────────────

    public record Vector3i(int x, int y, int z) {}
    public record PlayerInfo(String name, int health, boolean founder) {}
}
```
