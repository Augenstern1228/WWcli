package com.WWcli.mcp.transport;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StdioCommandResolverTest {

    @Test
    void resolvesBareCommandUsingPathExt(@TempDir Path tempDir) throws Exception {
        Path nodeBin = Files.createDirectories(tempDir.resolve("node-bin"));
        Path npx = Files.writeString(nodeBin.resolve("npx.cmd"), "@echo off\r\n");

        String resolved = StdioCommandResolver.resolveWindowsCommand(
                "npx", nodeBin.toString(), ".EXE;.CMD");

        assertEquals(npx.toAbsolutePath().normalize().toString(), resolved);
    }

    @Test
    void resolvesCommandThatAlreadyHasKnownExtension(@TempDir Path tempDir) throws Exception {
        Path nodeBin = Files.createDirectories(tempDir.resolve("node-bin"));
        Path node = Files.writeString(nodeBin.resolve("node.exe"), "placeholder");

        String resolved = StdioCommandResolver.resolveWindowsCommand(
                "node.exe", nodeBin.toString(), ".EXE;.CMD");

        assertEquals(node.toAbsolutePath().normalize().toString(), resolved);
    }

    @Test
    void keepsExplicitPathUnchanged(@TempDir Path tempDir) {
        String absolute = tempDir.resolve("npx.cmd").toAbsolutePath().toString();
        String relative = ".\\tools\\npx.cmd";

        assertEquals(absolute, StdioCommandResolver.resolveWindowsCommand(
                absolute, tempDir.toString(), ".CMD"));
        assertEquals(relative, StdioCommandResolver.resolveWindowsCommand(
                relative, tempDir.toString(), ".CMD"));
    }

    @Test
    void usesFirstMatchingPathEntryAndAcceptsQuotedDirectories(@TempDir Path tempDir) throws Exception {
        Path first = Files.createDirectories(tempDir.resolve("first bin"));
        Path second = Files.createDirectories(tempDir.resolve("second-bin"));
        Path expected = Files.writeString(first.resolve("tool.cmd"), "@echo off\r\n");
        Files.writeString(second.resolve("tool.cmd"), "@echo off\r\n");
        String pathValue = "\"" + first + "\";" + second;

        String resolved = StdioCommandResolver.resolveWindowsCommand(
                "tool", pathValue, "CMD");

        assertEquals(expected.toAbsolutePath().normalize().toString(), resolved);
    }

    @Test
    void leavesUnknownCommandForProcessBuilderToReport(@TempDir Path tempDir) {
        assertEquals("missing-tool", StdioCommandResolver.resolveWindowsCommand(
                "missing-tool", tempDir.toString(), ".EXE;.CMD"));
    }
}
