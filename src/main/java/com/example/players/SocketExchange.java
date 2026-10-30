package com.example.players;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/** Frames UTF-8 messages as lines over an owned socket and detects missing replies.
 * This exercise's generated messages contain no newline characters.
 */
public final class SocketExchange implements Exchange, AutoCloseable {
    private final Socket socket;
    private final BufferedReader input;
    private final BufferedWriter output;

    public SocketExchange(Socket socket) throws IOException {
        this.socket = socket;
        socket.setSoTimeout(10_000);
        input = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        output = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
    }

    public String read() throws IOException { return input.readLine(); }

    public void write(String message) throws IOException {
        if (message.indexOf('\n') >= 0 || message.indexOf('\r') >= 0) {
            throw new IllegalArgumentException("Messages must be single-line");
        }
        output.write(message);
        output.newLine();
        output.flush();
    }

    @Override
    public String exchange(String message) throws IOException {
        write(message);
        String reply = read();
        if (reply == null) throw new EOFException("Peer closed before replying");
        return reply;
    }

    @Override
    public void close() throws IOException { socket.close(); }
}
