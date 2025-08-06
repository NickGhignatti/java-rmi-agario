package it.unibo.agar;

import it.unibo.agar.model.rmi.GameServerImpl;

import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class AgarServer {
    private static final int WORLD_WIDTH = 1000;
    private static final int WORLD_HEIGHT = 1000;
    private static final int NUM_FOODS = 100;
    private static final int RMI_PORT = 1099;
    private static final String SERVER_NAME = "AgarGameServer";

    public static void main(String[] args) {
        try {
            // Create and start RMI registry
            Registry registry = LocateRegistry.createRegistry(RMI_PORT);
            System.out.println("RMI Registry started on port " + RMI_PORT);

            // Create game server
            GameServerImpl gameServer = new GameServerImpl(WORLD_WIDTH, WORLD_HEIGHT, NUM_FOODS);

            // Bind server to registry
            String serverURL = "rmi://localhost:" + RMI_PORT + "/" + SERVER_NAME;
            Naming.rebind(serverURL, gameServer);

            System.out.println("Agar Game Server started and bound to: " + serverURL);
            System.out.println("World size: " + WORLD_WIDTH + "x" + WORLD_HEIGHT);
            System.out.println("Initial food count: " + NUM_FOODS);
            System.out.println("Server is ready for players to connect...");

            // Add shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Shutting down server...");
                gameServer.shutdown();
            }));

            // Keep server running
            Object lock = new Object();
            synchronized (lock) {
                lock.wait();
            }

        } catch (Exception e) {
            System.err.println("Server error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}