package com.example.players;

import java.util.Objects;

/** Owns one player's name and independent sent/received message counters.
 * Instances are confined to the thread executing their conversation.
 */
public final class Player {
    private final String name;
    private int sent;
    private int received;

    public Player(String name) {
        this.name = Objects.requireNonNull(name);
    }

    /** Creates a message by appending this player's next one-based send counter. */
    public String send(String message) {
        Objects.requireNonNull(message);
        return message + " | " + ++sent;
    }

    public void receive(String message) {
        Objects.requireNonNull(message);
        received++;
    }

    public int sent() { return sent; }
    public int received() { return received; }

    public String summary() {
        return name + " pid=" + ProcessHandle.current().pid()
                + " sent=" + sent + " received=" + received;
    }
}
