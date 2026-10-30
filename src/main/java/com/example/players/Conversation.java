package com.example.players;

import java.io.IOException;
import java.util.Objects;

/** Runs the initiator's ten round trips and stops before an eleventh send. */
public final class Conversation {
    public static final int MESSAGE_LIMIT = 10;

    private Conversation() { }

    public static void run(Player initiator, Exchange exchange) throws IOException {
        String message = "Hello";
        for (int round = 0; round < MESSAGE_LIMIT; round++) {
            String outgoing = initiator.send(message);
            message = Objects.requireNonNull(exchange.exchange(outgoing), "Missing reply");
            initiator.receive(message);
            System.out.println("Reply " + initiator.received() + ": " + message);
        }
    }
}
