package it.unibo.agar;

import it.unibo.agar.model.Player;
import it.unibo.agar.model.rmi.GameClientImpl;
import it.unibo.agar.model.rmi.GameServer;
import it.unibo.agar.model.rmi.RemoteGameStateManager;
import it.unibo.agar.model.rmi.RemotePlayerImpl;
import it.unibo.agar.view.LocalView;

import javax.swing.*;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.Random;

public class AgarClient {
    private static final String SERVER_URL = "rmi://localhost:1099/AgarGameServer";
    private static final Random random = new Random();

    public static void main(String[] args) {
        try {
            String playerId = args.length > 0 ? args[0] : "player_" + random.nextInt(1000);

            System.out.println("Connecting to server as " + playerId + "...");

            GameServer gameServer = (GameServer) Naming.lookup(SERVER_URL);
            System.out.println("Connected to game server!");

            int worldWidth = gameServer.getWorldWidth();
            int worldHeight = gameServer.getWorldHeight();

            double initialMass = 120.0;
            double initialX = random.nextDouble() * worldWidth;
            double initialY = random.nextDouble() * worldHeight;
            Player initialPlayer = new Player(playerId, initialX, initialY, initialMass);

            RemotePlayerImpl remotePlayer = new RemotePlayerImpl(initialPlayer);
            GameClientImpl gameClient = new GameClientImpl(playerId);

            // Register player first
            gameServer.registerPlayer(remotePlayer);
            System.out.println("Player registered");

            // Register client - now using interface method
            gameServer.registerClient(gameClient);
            System.out.println("Client registered");

            System.out.println("Player " + playerId + " registered successfully!");

            SwingUtilities.invokeLater(() -> {
                try {
                    RemoteGameStateManager gameStateManager = new RemoteGameStateManager(
                            gameServer, gameClient, playerId);

                    LocalView localView = new LocalView(gameStateManager, playerId);
                    gameClient.setLocalView(localView);
                    localView.setVisible(true);

                    System.out.println("Game client UI started for " + playerId);

                    // Force initial repaint after a short delay
                    javax.swing.Timer initialRepaint = new javax.swing.Timer(1000, e -> {
                        System.out.println("Forcing repaint...");
                        localView.repaintView();
                        ((javax.swing.Timer) e.getSource()).stop();
                    });
                    initialRepaint.start();

                } catch (Exception e) {
                    System.err.println("Error creating UI: " + e.getMessage());
                    e.printStackTrace();
                }
            });

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    System.out.println("Disconnecting player " + playerId + "...");
                    gameServer.unregisterPlayer(playerId);
                } catch (RemoteException e) {
                    System.err.println("Error during shutdown: " + e.getMessage());
                }
            }));

        } catch (Exception e) {
            System.err.println("Client error: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(null,
                    "Failed to connect to game server.\nMake sure the server is running at " + SERVER_URL,
                    "Connection Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}