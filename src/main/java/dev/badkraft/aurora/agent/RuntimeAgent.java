/// src/main/java/dev/badkraft/aurora/agent/RuntimeAgent.java
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
package dev.badkraft.aurora.agent;

import java.lang.instrument.Instrumentation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static dev.badkraft.aurora.utils.AuroraLogger.info;

public class RuntimeAgent {
    private static final Path RUN_DIR;
    private static final Path LOG_DIR;

    static {
        String root = System.getProperty("aurora.project.root");
        if (root == null) {
            // Fallback for JAR launch — still works
            root = findProjectRootFromJar();
        }
        RUN_DIR = Paths.get(root).resolve("run").resolve("minecraft");
        System.setProperty("user.dir", RUN_DIR.toAbsolutePath().toString());
        // set aurora directory property
        Path auroraDir = RUN_DIR.getParent().resolve( "aurora");
        System.setProperty("aurora.dir", auroraDir.toString());
        // set 'logDir/logFile'
        LOG_DIR = auroraDir.resolve("logs");

        try {
            if(!Files.isDirectory(LOG_DIR)) {
                Files.createDirectories(LOG_DIR);
            }
        } catch (Exception ioEx){
            throw new ExceptionInInitializerError("Failed to create log directory: " + ioEx);
        }

        // now we can instantate the logger without importing any static loading
    }
    public static void premain(String agentArgs, Instrumentation inst) {

        System.out.println("[Aurora:RuntimeAgent] Detected exec path. Set working: " + RUN_DIR);
    }

    public static Path logDir() {
        return LOG_DIR;
    }

    private static String findProjectRootFromJar() {
        try {
            String path = RuntimeAgent.class
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI()
                    .getPath();

            Path p = Paths.get(path);
            while (p != null && !Files.exists(p.resolve("build.gradle.kts"))) {
                p = p.getParent();
            }
            return p != null ? p.toString() : System.getProperty("user.dir");
        } catch (Exception e) {
            return System.getProperty("user.dir");
        }
    }

    private static void log(String msg, Object ... args){
        info(msg, args);
    }
}
