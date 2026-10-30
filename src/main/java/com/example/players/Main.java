package com.example.players;

import java.io.IOException;
import java.net.*;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/** Selects execution mode, owns communication/process lifecycles, and reports failures. */
public final class Main {
    private Main() { }

    public static void main(String[] args) {
        try {
            if (args.length == 1 && args[0].equals("same-process")) {
                sameProcess();
            } else if (args.length == 1 && args[0].equals("separate-process")) {
                separateProcess();
            } else if (args.length == 2 && args[0].equals("responder")) {
                respond(Integer.parseInt(args[1]));
            } else {
                throw new IllegalArgumentException("Usage: Main same-process|separate-process");
            }
        } catch (Exception e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            System.err.println("Conversation failed: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void sameProcess() throws IOException {
        Player initiator = new Player("initiator");
        Player responder = new Player("responder");
        Conversation.run(initiator, message -> {
            responder.receive(message);
            return responder.send(message);
        });
        System.out.println(initiator.summary());
        System.out.println(responder.summary());
    }

    private static void separateProcess() throws Exception {
        Player initiator = new Player("initiator");
        // Bind before launching the child: no port guessing or startup sleeps.
        try (ServerSocket server = new ServerSocket()) {
            server.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
            server.setSoTimeout(10_000);
            String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
            Process child = new ProcessBuilder(java, "-cp", System.getProperty("java.class.path"),
                    Main.class.getName(), "responder", Integer.toString(server.getLocalPort()))
                    .inheritIO().start();
            try {
                try (Socket socket = server.accept(); SocketExchange exchange = new SocketExchange(socket)) {
                    Conversation.run(initiator, exchange);
                } // EOF tells the responder there are no more messages.
                if (!child.waitFor(10, TimeUnit.SECONDS)) {
                    throw new IOException("Responder did not terminate");
                }
                if (child.exitValue() != 0) throw new IOException("Responder exited with " + child.exitValue());
                System.out.println(initiator.summary());
            } finally {
                if (child.isAlive()) {
                    child.destroy();
                    try {
                        if (!child.waitFor(2, TimeUnit.SECONDS)) {
                            child.destroyForcibly().waitFor();
                        }
                    } catch (InterruptedException e) {
                        child.destroyForcibly();
                        Thread.currentThread().interrupt();
                        throw e;
                    }
                }
            }
        }
    }

    private static void respond(int port) throws IOException {
        Player responder = new Player("responder");
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 10_000);
            try (SocketExchange exchange = new SocketExchange(socket)) {
                String message;
                while ((message = exchange.read()) != null) {
                    responder.receive(message);
                    exchange.write(responder.send(message));
                }
            }
        }
        if (responder.sent() != Conversation.MESSAGE_LIMIT
                || responder.received() != Conversation.MESSAGE_LIMIT) {
            throw new IOException("Conversation ended before ten round trips");
        }
        System.out.println(responder.summary());
    }
}
