package com.example.players;

import java.io.IOException;

/** Provides one request/reply exchange without exposing the communication mechanism. */
@FunctionalInterface
public interface Exchange {
    String exchange(String message) throws IOException;
}
