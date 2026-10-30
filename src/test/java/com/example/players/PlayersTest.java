package com.example.players;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.*;
import static org.junit.jupiter.api.Assertions.*;

/** Verifies player state, full message progression, and both real execution modes. */
class PlayersTest {
    @TempDir Path temp;

    @Test void countersAreIndependentAndOneBased() {
        Player first = new Player("first");
        Player second = new Player("second");
        assertEquals("hello | 1", first.send("hello"));
        assertEquals("hello | 2", first.send("hello"));
        second.receive("hello | 2");
        assertEquals("hello | 2 | 1", second.send("hello | 2"));
        assertEquals(0, first.received());
        assertEquals(1, second.received());
        assertThrows(NullPointerException.class, () -> first.send(null));
        assertEquals(2, first.sent());
    }

    @Test void conversationStopsAfterTenthReplyAndPreservesPayload() throws Exception {
        Player initiator = new Player("initiator");
        Player responder = new Player("responder");
        List<String> requests = new ArrayList<>();
        Conversation.run(initiator, message -> {
            requests.add(message);
            responder.receive(message);
            return responder.send(message);
        });
        String expected = "Hello";
        for (int i = 1; i <= 10; i++) {
            expected += " | " + i;
            assertEquals(expected, requests.get(i - 1));
            expected += " | " + i;
        }
        assertEquals(10, requests.size());
        assertEquals(10, initiator.sent());
        assertEquals(10, initiator.received());
        assertEquals(10, responder.sent());
        assertEquals(10, responder.received());
    }

    @Test void communicationFailureIsPropagated() {
        Player initiator = new Player("initiator");
        assertThrows(IOException.class, () -> Conversation.run(initiator, message -> {
            throw new IOException("disconnected");
        }));
        assertEquals(1, initiator.sent());
        assertEquals(0, initiator.received());
    }

    @Test void sameProcessUsesOnePidAndTerminates() throws Exception { verifyMode("same-process", 1); }
    @Test void separateProcessUsesTwoPidsAndTerminates() throws Exception { verifyMode("separate-process", 2); }

    private void verifyMode(String mode, int expectedPids) throws Exception {
        Path output = temp.resolve(mode + ".log");
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        Process process = new ProcessBuilder(java, "-cp", Path.of("target/classes").toAbsolutePath().toString(),
                Main.class.getName(), mode).redirectErrorStream(true).redirectOutput(output.toFile()).start();
        try {
            assertTrue(process.waitFor(25, TimeUnit.SECONDS), "Program did not terminate");
            String log = Files.readString(output);
            assertEquals(0, process.exitValue(), log);
            Matcher summaries = Pattern.compile("(initiator|responder) pid=(\\d+) sent=10 received=10").matcher(log);
            Set<String> roles = new HashSet<>();
            Set<String> pids = new HashSet<>();
            int count = 0;
            while (summaries.find()) { count++; roles.add(summaries.group(1)); pids.add(summaries.group(2)); }
            assertEquals(2, count, log);
            assertEquals(Set.of("initiator", "responder"), roles, log);
            assertEquals(expectedPids, pids.size(), log);
            assertEquals(10, log.lines().filter(line -> line.startsWith("Reply ")).count(), log);
            assertFalse(log.contains("Reply 11:"), log);
        } finally {
            process.descendants().forEach(ProcessHandle::destroyForcibly);
            if (process.isAlive()) process.destroyForcibly().waitFor();
        }
    }
}
