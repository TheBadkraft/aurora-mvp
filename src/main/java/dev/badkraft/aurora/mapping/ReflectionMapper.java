/// src/main/java/dev/badkraft/aurora/mapping/ReflectionMapper.java
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
package dev.badkraft.aurora.mapping;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ReflectionMapper {

    private static final Map<String, MethodHandle> CACHE = new ConcurrentHashMap<>();
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    public static void buildCache(Path mappingFile) throws Exception {
        System.out.println("[Aurora] Loading mappings from " + mappingFile);
        List<String> lines = Files.readAllLines(mappingFile);

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) continue;

            if (line.startsWith("method ")) {
                String[] parts = line.substring(7).split(" -> ");
                String sig = parts[0];
                String name = parts[1];

                int paren = sig.indexOf('(');
                String className = sig.substring(0, paren);
                String desc = sig.substring(paren);

                try {
                    Class<?> clazz = Class.forName(className);
                    MethodType mt = descriptorToMethodType(desc);
                    MethodHandle mh = LOOKUP.findVirtual(clazz, name, mt);
                    CACHE.put(clazz.getSimpleName() + "." + name, mh);
                } catch (Throwable t) {
                    System.err.println("[Aurora] Failed to map: " + line);
                }
            }
        }
        System.out.println("[Aurora] Loaded " + CACHE.size() + " method handles.");
    }

    public static void apply(Object target, Map<String, Object> fields) {
        String classKey = target.getClass().getSimpleName();
        fields.forEach((key, value) -> {
            MethodHandle mh = CACHE.get(classKey + "." + key);
            if (mh != null) {
                try {
                    mh.invokeExact(target, value);
                } catch (Throwable t) {
                    System.err.println("[Aurora] Failed to apply " + key + " = " + value);
                }
            } else {
                System.out.println("[Aurora] Unmapped field: " + key);
            }
        });
    }

    private static MethodType descriptorToMethodType(String desc) throws ClassNotFoundException {
        int paren = desc.indexOf(')');
        String params = desc.substring(1, paren);
        String ret = desc.substring(paren + 1);

        List<Class<?>> paramTypes = new ArrayList<>();
        int i = 0;
        while (i < params.length()) {
            char c = params.charAt(i);
            if (c == 'L') {
                int semi = params.indexOf(';', i);
                String className = params.substring(i + 1, semi).replace("/", ".");
                paramTypes.add(Class.forName(className));
                i = semi + 1;
            } else if (c == '[') {
                int end = i + 1;
                while (params.charAt(end) == '[') end++;
                if (params.charAt(end) == 'L') {
                    int semi = params.indexOf(';', end);
                    String className = params.substring(end + 1, semi).replace("/", ".");
                    paramTypes.add(Class.forName("[L" + className + ";"));
                    i = semi + 1;
                } else {
                    paramTypes.add(primitiveToClass(params.charAt(end)));
                    i = end + 1;
                }
            } else {
                paramTypes.add(primitiveToClass(c));
                i++;
            }
        }

        Class<?> returnType = ret.equals("V") ? void.class :
                ret.startsWith("L") ? Class.forName(ret.substring(1, ret.length()-1).replace("/", ".")) :
                        primitiveToClass(ret.charAt(0));

        return MethodType.methodType(returnType, paramTypes);
    }

    private static Class<?> primitiveToClass(char c) {
        return switch (c) {
            case 'Z' -> boolean.class;
            case 'B' -> byte.class;
            case 'C' -> char.class;
            case 'S' -> short.class;
            case 'I' -> int.class;
            case 'F' -> float.class;
            case 'J' -> long.class;
            case 'D' -> double.class;
            case 'V' -> void.class;
            default -> throw new IllegalArgumentException("Unknown primitive: " + c);
        };
    }
}