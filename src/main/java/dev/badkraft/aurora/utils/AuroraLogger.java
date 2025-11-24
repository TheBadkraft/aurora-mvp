/// src/main/java/dev/badkraft/aurora/utils/AuroraLogger.java
///
/// Copyright (c) 2025 Quantum Override. All rights reserved.
/// Author: The Badkraft
/// Date: November 24, 2025
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
package dev.badkraft.aurora.utils;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static dev.badkraft.aurora.agent.RuntimeAgent.logDir;

/// Global early + runtime logger.
/// Current: writes to run/minecraft/logs/aurora.log
/// Future:  will delegate to Minecraft's net.minecraft.util.logging.Logger
///          via MethodHandle when available.
/// Expected MC logger signature (1.21.10):
///   void info(String message, Object... params)
///   void error(String message, Object... params)
/// We'll bind to one of those (prefer info) and forward all calls.
/*  After Minecraft classloader is ready
    AuroraLogger.bindToMinecraftLogger();
    AuroraLogger.info("Aurora now owns logging — from boot to game.");
 */
public final class AuroraLogger {

    private static final Path LOG_FILE = logDir().resolve("aurora.log");
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private static MethodHandle mcLogHandle = null;

    static {
        try {
            Files.createDirectories(LOG_FILE.getParent());
            Files.writeString(LOG_FILE, "", StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            log("INFO", "Aurora-MVP: Logger initialized");
        } catch (Exception ignored) {}
    }

    private AuroraLogger() {}

    public static void info(String msg, Object... args) {
        log("INFO", msg, args);
    }

    public static void error(String msg, Object... args) {
        log("ERROR", msg, args);
    }

    public static void debug(String msg, Object... args) {
        log("DEBUG", msg, args);
    }

    private static void log(String level, String msg, Object... args) {
        String formatted = args.length == 0 ? msg : String.format(msg, args);
        String tag = String.format("%-15s", String.format("[Aurora/%s]:", level));
        String line = "[%s] %s %s".formatted(
                LocalDateTime.now().format(TS), tag, formatted
        );

        // 1. Early: always write to file
        try {
            Files.writeString(LOG_FILE, line + "\n",
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception ignored) {}

        // 2. Runtime: forward to Minecraft logger if available
        if (mcLogHandle != null) {
            try {
                mcLogHandle.invokeExact(formatted);
            } catch (Throwable ignored) {}
        } else {
            // Fallback: stdout (early boot)
            System.out.println(line);
        }
    }

    /** Called once when Minecraft logger is available */
    public static void bindToMinecraftLogger() {
        try {
            Class<?> loggerClass = Class.forName("net.minecraft.util.logging.Logger");
            MethodHandle info = MethodHandles.lookup()
                    .findStatic(loggerClass, "info", MethodType.methodType(void.class, String.class, Object[].class));

            mcLogHandle = info;
            info("AuroraLogger hooked into Minecraft logging");
        } catch (Throwable t) {
            // Silent — expected if class not loaded yet
        }
    }
}