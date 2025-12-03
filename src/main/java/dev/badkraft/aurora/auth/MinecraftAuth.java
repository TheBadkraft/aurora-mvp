/// src/main/java/dev/badkraft/aurora/auth/MinecraftAuth.java
///
/// Copyright (c) 2025 Quantum Override. All rights reserved.
/// Author: The Badkraft
/// Date: November 12, 2025
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
package dev.badkraft.aurora.auth;

import com.google.gson.*;
import com.sun.net.httpserver.HttpServer;

import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.regex.*;

import static dev.badkraft.aurora.Loader.extractValue;
import static dev.badkraft.aurora.Loader.log;

public class MinecraftAuth {

    // Official Minecraft client ID — REQUIRED for online mode
    private static final String CLIENT_ID     = "3963c466-60f2-4cf4-928e-287187933c94";
    private static final String MC_CLIENT_ID  = "00000000441cc96b";
    private static final String REDIRECT_URI  = "http://localhost:8080/";
    private static final String TENANT        = "consumers";

    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final Gson GSON = new Gson();

    record TokenResponse(String access_token, String refresh_token, int expires_in) {}
    record XblResponse(String Token, JsonObject DisplayClaims) {}
    record XstsResponse(String Token, JsonObject DisplayClaims) {}
    record McAuthResponse(String access_token, int expires_in) {}
    record McProfile(String id, String name) {}

    public static void loginAndSave() throws Exception {
        log("[Aurora] Starting Microsoft login...");

        String authUrl = String.format(
                "https://login.microsoftonline.com/%s/oauth2/v2.0/authorize?" +
                        "client_id=%s&response_type=code&redirect_uri=%s&scope=%s",
                TENANT,
                CLIENT_ID,
                URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8),
                URLEncoder.encode("XboxLive.signin offline_access", StandardCharsets.UTF_8)
        );

        CompletableFuture<String> codeFuture = new CompletableFuture<>();
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", exchange -> {
            String query = exchange.getRequestURI().getQuery();
            String code = query != null ? parseParam(query, "code") : null;
            String html = code != null
                    ? "<h1>Login successful!</h1><p>You may close this window.</p>"
                    : "<h1>Error</h1><p>No code received.</p>";
            exchange.sendResponseHeaders(200, html.getBytes().length);
            exchange.getResponseBody().write(html.getBytes());
            exchange.close();
            if (code != null) codeFuture.complete(code);
        });
        server.start();

        java.awt.Desktop.getDesktop().browse(URI.create(authUrl));
        String code = codeFuture.get();
        server.stop(0);

        TokenResponse ms      = exchangeCode(code);
        XblResponse   xbl     = getXbl(ms.access_token);
        XstsResponse  xsts    = getXsts(xbl.Token);
        McAuthResponse mc     = authenticateMinecraft(getUhs(xbl.DisplayClaims), xsts.Token);
        McProfile     profile = getProfile(mc.access_token);
        String xuid           = getXuid(xbl.DisplayClaims);

        Path config = Paths.get("config.aurora");
        String content = """
                #!aml
                
                auth := {
                  access_token := "%s"
                  refresh_token := "%s"
                  username      := "%s"
                  uuid          := "%s"
                  xuid          := "%s"
                  client_id     := "%s"
                  expires_at    := %d
                }
                """.formatted(
                mc.access_token,
                ms.refresh_token,
                profile.name,
                profile.id,
                xuid,
                MC_CLIENT_ID,
                Instant.now().getEpochSecond() + mc.expires_in
        );
        Files.writeString(config, content);
        log("[Aurora] Login successful – config.aurora written.");
    }

    private static String getXuid(JsonObject claims) {
        JsonArray xui = claims.getAsJsonArray("xui");
        if (xui == null || xui.isEmpty()) {
            log("[Auth] No xui array — this should not happen");
            return "2535444887286849";
        }
        JsonObject user = xui.get(0).getAsJsonObject();
        // 2024–2025: Microsoft removed "xid", only "uhs" is returned
        // Minecraft accepts the raw uhs as auth_xuid
        return user.get("uhs").getAsString();
    }

    public static void refreshSession() throws Exception {
        log("[Aurora] Refreshing session...");
        Path config = Paths.get("config.aurora");
        if (!Files.exists(config)) {
            log("[Aurora] No config.aurora – starting full login");
            loginAndSave();
            return;
        }

        String content = Files.readString(config);
        String refreshToken = extractValue(content, "refresh_token");
        if (refreshToken == null || refreshToken.isEmpty() || "null".equals(refreshToken)) {
            log("[Aurora] Invalid refresh token – full login required");
            loginAndSave();
            return;
        }

        TokenResponse ms    = refreshMsToken(refreshToken);
        XblResponse xbl     = getXbl(ms.access_token);
        XstsResponse xsts   = getXsts(xbl.Token);
        McAuthResponse mc   = authenticateMinecraft(getUhs(xbl.DisplayClaims), xsts.Token);
        McProfile profile   = getProfile(mc.access_token);
        String xuid         = getXuid(xbl.DisplayClaims);

        long expiresAt = Instant.now().getEpochSecond() + mc.expires_in;
        String newContent = """
                #!aml
                
                auth := {
                  access_token := "%s"
                  refresh_token := "%s"
                  username      := "%s"
                  uuid          := "%s"
                  xuid          := "%s"
                  client_id     := "%s"
                  expires_at    := %d
                }
                """.formatted(mc.access_token, ms.refresh_token, profile.name, profile.id, xuid, CLIENT_ID, expiresAt);

        Files.writeString(config, newContent);
        log("[Aurora] Session refreshed – %s", profile.name);
    }

    private static TokenResponse exchangeCode(String code) throws Exception {
        String body = "client_id=%s&code=%s&redirect_uri=%s&grant_type=authorization_code"
                .formatted(CLIENT_ID, code, REDIRECT_URI);
        String json = post("https://login.microsoftonline.com/consumers/oauth2/v2.0/token", body, "application/x-www-form-urlencoded");
        return GSON.fromJson(json, TokenResponse.class);
    }

    private static TokenResponse refreshMsToken(String refreshToken) throws Exception {
        String body = "client_id=%s&refresh_token=%s&grant_type=refresh_token".formatted(CLIENT_ID, refreshToken);
        String json = post("https://login.microsoftonline.com/consumers/oauth2/v2.0/token", body, "application/x-www-form-urlencoded");
        return GSON.fromJson(json, TokenResponse.class);
    }

    private static XblResponse getXbl(String msToken) throws Exception {
        String payload = "{\"Properties\":{\"AuthMethod\":\"RPS\",\"SiteName\":\"user.auth.xboxlive.com\",\"RpsTicket\":\"d=%s\"},\"RelyingParty\":\"http://auth.xboxlive.com\",\"TokenType\":\"JWT\"}"
                .formatted(msToken);
        String json = postJson("https://user.auth.xboxlive.com/user/authenticate", payload);
        return GSON.fromJson(json, XblResponse.class);
    }

    private static XstsResponse getXsts(String xblToken) throws Exception {
        String payload = "{\"Properties\":{\"SandboxId\":\"RETAIL\",\"UserTokens\":[\"%s\"]},\"RelyingParty\":\"rp://api.minecraftservices.com/\",\"TokenType\":\"JWT\"}"
                .formatted(xblToken);
        String json = postJson("https://xsts.auth.xboxlive.com/xsts/authorize", payload);
        return GSON.fromJson(json, XstsResponse.class);
    }

    private static McAuthResponse authenticateMinecraft(String uhs, String xstsToken) throws Exception {
        String payload = "{\"identityToken\":\"XBL3.0 x=%s;%s\"}".formatted(uhs, xstsToken);
        String json = postJson("https://api.minecraftservices.com/authentication/login_with_xbox", payload);
        return GSON.fromJson(json, McAuthResponse.class);
    }

    // CORRECT ENDPOINT — Minecraft profile by token
    private static McProfile getProfile(String mcToken) throws Exception {
        String json = get("https://api.minecraftservices.com/minecraft/profile", mcToken);
        return GSON.fromJson(json, McProfile.class);
    }

    private static String getUhs(JsonObject claims) {
        return claims.getAsJsonArray("xui")
                .get(0).getAsJsonObject()
                .get("uhs").getAsString();
    }

    private static String parseParam(String query, String key) {
        Matcher m = Pattern.compile(key + "=([^&]+)").matcher(query);
        return m.find() ? m.group(1) : null;
    }

    private static String post(String url, String body, String contentType) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", contentType)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> resp = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 400) throw new RuntimeException("HTTP " + resp.statusCode() + ": " + resp.body());
        return resp.body();
    }

    private static String postJson(String url, String json) throws Exception {
        return post(url, json, "application/json");
    }

    private static String get(String url, String token) throws Exception {
        if (token == null || token.isBlank()) throw new IllegalArgumentException("Missing token");
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();
        HttpResponse<String> resp = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 400) throw new RuntimeException("HTTP " + resp.statusCode() + ": " + resp.body());
        return resp.body();
    }
}