/// src/main/java/dev/badkraft/aurora/utils/Directories.java
///
/// Copyright (c) 2025 Quantum Override. All rights reserved.
/// Author: The Badkraft
/// Date: November 23, 2025
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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import static dev.badkraft.aurora.Loader.log;
import static dev.badkraft.aurora.utils.AuroraLogger.debug;

public final class Directories {
    public static final Path ROOT_DIR;
    public static final Path MAPPINGS_DIR;
    public static final Path AURORA_DIR;
    public static final Path RUN_DIR;
    public static final Path DOT_MINECRAFT_DIR = findDotMinecraft();

    static {
        try {
            RUN_DIR = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
            ROOT_DIR = RUN_DIR.getParent().getParent(); // ../../
            AURORA_DIR = Paths.get(System.getProperty("aurora.dir")).toAbsolutePath().normalize();
            MAPPINGS_DIR = AURORA_DIR.resolve("mappings");
        } catch (Exception e) {
            throw new ExceptionInInitializerError("Failed to initialize directories: " + e);
        }

        log(new Directories().toString());
    }

    private Directories() {} // no instances

    @Override
    public String toString() {
        return """
            Aurora Directories:
              Exec Root      → %s
              Mappings       → %s
              Aurora Maps    → %s
              Run Directory  → %s
            """.formatted(ROOT_DIR, MAPPINGS_DIR, AURORA_DIR, RUN_DIR);
    }

    private static Path findDotMinecraft() {
        Path p = Paths.get(System.getProperty("user.home"), ".minecraft");
        if (Files.isDirectory(p)) {
            debug(".minecraft contents: %s", Arrays.toString(p.toFile().list()));
            return p;
        }
        return null;
    }
}