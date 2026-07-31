package com.WWcli.mcp.transport;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class StdioCommandResolver {
    private static final String DEFAULT_WINDOWS_PATHEXT = ".COM;.EXE;.BAT;.CMD";

    private StdioCommandResolver() {
    }

    static String resolve(String command, Map<String, String> env) {
        if (!isWindows()) {
            return command;
        }
        return resolveWindowsCommand(
                command,
                environmentValue(env, "PATH"),
                environmentValue(env, "PATHEXT"));
    }

    static String resolveWindowsCommand(String command, String pathValue, String pathExtValue) {
        if (command == null || command.isBlank() || hasExplicitPath(command) || pathValue == null) {
            return command;
        }

        List<String> extensions = parsePathExtensions(pathExtValue);
        List<String> candidates = new ArrayList<>();
        candidates.add(command);
        if (!hasKnownExtension(command, extensions)) {
            for (String extension : extensions) {
                candidates.add(command + extension);
            }
        }

        for (String rawDirectory : pathValue.split(";", -1)) {
            String directory = stripOuterQuotes(rawDirectory.trim());
            if (directory.isEmpty()) {
                continue;
            }
            for (String candidateName : candidates) {
                try {
                    Path candidate = Path.of(directory, candidateName);
                    if (Files.isRegularFile(candidate)) {
                        return candidate.toAbsolutePath().normalize().toString();
                    }
                } catch (InvalidPathException ignored) {
                    // Ignore malformed PATH entries and continue with the remaining directories.
                }
            }
        }
        return command;
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "")
                .toLowerCase(Locale.ROOT)
                .contains("win");
    }

    private static boolean hasExplicitPath(String command) {
        return command.indexOf('/') >= 0 || command.indexOf('\\') >= 0;
    }

    private static List<String> parsePathExtensions(String pathExtValue) {
        String value = pathExtValue == null || pathExtValue.isBlank()
                ? DEFAULT_WINDOWS_PATHEXT
                : pathExtValue;
        List<String> extensions = new ArrayList<>();
        for (String rawExtension : value.split(";")) {
            String extension = rawExtension.trim();
            if (extension.isEmpty()) {
                continue;
            }
            String normalized = extension.startsWith(".") ? extension : "." + extension;
            extensions.add(normalized.toLowerCase(Locale.ROOT));
        }
        return extensions;
    }

    private static boolean hasKnownExtension(String command, List<String> extensions) {
        String lowerCommand = command.toLowerCase(Locale.ROOT);
        return extensions.stream()
                .map(extension -> extension.toLowerCase(Locale.ROOT))
                .anyMatch(lowerCommand::endsWith);
    }

    private static String stripOuterQuotes(String value) {
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private static String environmentValue(Map<String, String> overrides, String name) {
        if (overrides != null) {
            for (Map.Entry<String, String> entry : overrides.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(name)) {
                    return entry.getValue();
                }
            }
        }
        for (Map.Entry<String, String> entry : System.getenv().entrySet()) {
            if (entry.getKey().equalsIgnoreCase(name)) {
                return entry.getValue();
            }
        }
        return null;
    }
}
