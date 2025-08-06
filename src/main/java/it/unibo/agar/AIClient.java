package it.unibo.agar;

import it.unibo.agar.model.*;
import it.unibo.agar.model.rmi.GameClientImpl;
import it.unibo.agar.model.rmi.GameServer;
import it.unibo.agar.model.rmi.RemoteGameStateManager;
import it.unibo.agar.model.rmi.RemotePlayerImpl;
import it.unibo.agar.view.GlobalView;

import javax.swing.*;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.List;
import java.util.Comparator;
import java.util.Optional;

/**
 * AI Client that connects to the RMI server and plays automatically
 */
public class AIClient {
    private static final String SERVER_URL = "rmi://localhost:1099/AgarGameServer";
    private static final Random random = new Random();
    private static final long AI_UPDATE_INTERVAL = 100; // ms
    private static final double SAFE_DISTANCE = 100.0; // Safe distance from dangerous players

    private static Timer aiTimer;
    private static String currentPlayerId;
    private static GameClientImpl gameClient;
    private static AIDecisionType currentDecision = AIDecisionType.EXPLORE;

    public static void main(String[] args) {
        try {
            currentPlayerId = args.length > 0 ? args[0] : "ai_" + random.nextInt(1000);

            System.out.println("Starting AI client with GUI as " + currentPlayerId + "...");

            GameServer gameServer = (GameServer) Naming.lookup(SERVER_URL);
            System.out.println("AI connected to game server!");

            int worldWidth = gameServer.getWorldWidth();
            int worldHeight = gameServer.getWorldHeight();

            double initialMass = 120.0;
            double initialX = random.nextDouble() * worldWidth;
            double initialY = random.nextDouble() * worldHeight;
            Player initialPlayer = new Player(currentPlayerId, initialX, initialY, initialMass);

            RemotePlayerImpl remotePlayer = new RemotePlayerImpl(initialPlayer);
            gameClient = new GameClientImpl(currentPlayerId);

            // Register both player and client
            gameServer.registerPlayer(remotePlayer);
            System.out.println("AI Player registered");

            gameServer.registerClient(gameClient);
            System.out.println("AI Client registered");

            System.out.println("AI Player " + currentPlayerId + " registered successfully!");

            // Create AI GUI
            SwingUtilities.invokeLater(() -> {
                try {
                    RemoteGameStateManager gameStateManager = new RemoteGameStateManager(
                            gameServer, gameClient, currentPlayerId);

                    GlobalView localView = new GlobalView(gameStateManager);
                    gameClient.setGlobalView(localView);
                    localView.setVisible(true);

                    System.out.println("Game client UI started for " + currentPlayerId);

                    // Force initial repaint after a short delay
                    javax.swing.Timer initialRepaint = new javax.swing.Timer(1000, e -> {
                        System.out.println("Forcing repaint...");
                        localView.repaintView();
                        ((javax.swing.Timer) e.getSource()).stop();
                    });
                    initialRepaint.start();

                } catch (Exception e) {
                    System.err.println("Error creating AI GUI: " + e.getMessage());
                    e.printStackTrace();
                }
            });

            // Start AI behavior
            aiTimer = new Timer(true);
            aiTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    try {
                        runAdvancedAILogic(gameServer, gameClient, currentPlayerId, worldWidth, worldHeight);
                    } catch (Exception e) {
                        System.err.println("AI error for " + currentPlayerId + ": " + e.getMessage());
                    }
                }
            }, 2000, AI_UPDATE_INTERVAL); // Start after 2 seconds

            // Shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    if (aiTimer != null) {
                        aiTimer.cancel();
                    }
                    gameServer.unregisterPlayer(currentPlayerId);
                    System.out.println("AI " + currentPlayerId + " disconnected");
                } catch (RemoteException e) {
                    System.err.println("Error during AI shutdown: " + e.getMessage());
                }
            }));

            // Keep AI running
            Object lock = new Object();
            synchronized (lock) {
                lock.wait();
            }

        } catch (Exception e) {
            System.err.println("AI client error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void runAdvancedAILogic(GameServer gameServer, GameClientImpl gameClient,
                                           String playerId, int worldWidth, int worldHeight)
            throws RemoteException {

        if (!gameClient.isAlive()) {
            System.out.println("AI " + playerId + " is dead, stopping movement");
            return;
        }

        List<Player> players = gameClient.getCurrentPlayers();
        List<Food> foods = gameClient.getCurrentFoods();

        if (players == null || foods == null) {
            return;
        }

        Optional<Player> ourPlayerOpt = players.stream()
                .filter(p -> p.getId().equals(playerId))
                .findFirst();

        if (ourPlayerOpt.isEmpty()) {
            return;
        }

        Player ourPlayer = ourPlayerOpt.get();

        AIDecision decision = makeAIDecision(ourPlayer, players, foods);
        currentDecision = decision.type; // Store for GUI display

        double directionX = 0, directionY = 0;

        switch (decision.type) {
            case FLEE:
                if (decision.targetPlayer != null) {
                    double fleeX = ourPlayer.getX() - decision.targetPlayer.getX();
                    double fleeY = ourPlayer.getY() - decision.targetPlayer.getY();
                    double fleeDistance = Math.hypot(fleeX, fleeY);

                    if (fleeDistance > 0) {
                        directionX = fleeX / fleeDistance;
                        directionY = fleeY / fleeDistance;

                        directionX += (random.nextDouble() - 0.5) * 0.3;
                        directionY += (random.nextDouble() - 0.5) * 0.3;

                        double magnitude = Math.hypot(directionX, directionY);
                        if (magnitude > 0) {
                            directionX /= magnitude;
                            directionY /= magnitude;
                        }
                    }
                }
                break;

            case HUNT:
                if (decision.targetPlayer != null) {
                    double huntX = decision.targetPlayer.getX() - ourPlayer.getX();
                    double huntY = decision.targetPlayer.getY() - ourPlayer.getY();
                    double huntDistance = Math.hypot(huntX, huntY);

                    if (huntDistance > 0) {
                        directionX = huntX / huntDistance;
                        directionY = huntY / huntDistance;
                    }
                }
                break;

            case SEEK_FOOD:
                if (decision.targetFood != null) {
                    double foodX = decision.targetFood.getX() - ourPlayer.getX();
                    double foodY = decision.targetFood.getY() - ourPlayer.getY();
                    double foodDistance = Math.hypot(foodX, foodY);

                    if (foodDistance > 0) {
                        directionX = foodX / foodDistance;
                        directionY = foodY / foodDistance;
                    }
                }
                break;

            case EXPLORE:
                double centerX = worldWidth / 2.0;
                double centerY = worldHeight / 2.0;
                double toCenterX = centerX - ourPlayer.getX();
                double toCenterY = centerY - ourPlayer.getY();
                double toCenterDistance = Math.hypot(toCenterX, toCenterY);

                if (toCenterDistance > worldWidth * 0.3) {
                    directionX = toCenterX / toCenterDistance;
                    directionY = toCenterY / toCenterDistance;
                } else {
                    double angle = random.nextDouble() * 2 * Math.PI;
                    directionX = Math.cos(angle);
                    directionY = Math.sin(angle);
                }
                break;

            case STOP:
            default:
                directionX = 0;
                directionY = 0;
                break;
        }

        double boundaryAvoidance = avoidBoundaries(ourPlayer, worldWidth, worldHeight);
        if (boundaryAvoidance > 0) {
            if (ourPlayer.getX() < boundaryAvoidance) {
                directionX = Math.max(directionX, 0.5);
            }
            if (ourPlayer.getX() > worldWidth - boundaryAvoidance) {
                directionX = Math.min(directionX, -0.5);
            }
            if (ourPlayer.getY() < boundaryAvoidance) {
                directionY = Math.max(directionY, 0.5);
            }
            if (ourPlayer.getY() > worldHeight - boundaryAvoidance) {
                directionY = Math.min(directionY, -0.5);
            }
        }

        gameServer.setPlayerDirection(playerId, directionX, directionY);
    }

    private static AIDecision makeAIDecision(Player ourPlayer, List<Player> allPlayers, List<Food> foods) {
        for (Player other : allPlayers) {
            if (other.getId().equals(ourPlayer.getId())) continue;

            double distance = ourPlayer.distanceTo(other);
            boolean canEatUs = other.getMass() > ourPlayer.getMass() * 1.1;

            if (canEatUs && distance < SAFE_DISTANCE) {
                return new AIDecision(AIDecisionType.FLEE, other, null);
            }
        }

        for (Player other : allPlayers) {
            if (other.getId().equals(ourPlayer.getId())) continue;

            double distance = ourPlayer.distanceTo(other);
            boolean canEatThem = ourPlayer.getMass() > other.getMass() * 1.1;

            if (canEatThem && distance < SAFE_DISTANCE * 1.5) {
                return new AIDecision(AIDecisionType.HUNT, other, null);
            }
        }

        Optional<Food> nearestFood = foods.stream()
                .min(Comparator.comparingDouble(food -> ourPlayer.distanceTo(food)));

        if (nearestFood.isPresent() && ourPlayer.distanceTo(nearestFood.get()) < SAFE_DISTANCE * 2) {
            return new AIDecision(AIDecisionType.SEEK_FOOD, null, nearestFood.get());
        }

        return new AIDecision(AIDecisionType.EXPLORE, null, null);
    }

    private static double avoidBoundaries(Player player, int worldWidth, int worldHeight) {
        double margin = player.getRadius() * 3;

        double distanceToEdge = Math.min(
                Math.min(player.getX(), worldWidth - player.getX()),
                Math.min(player.getY(), worldHeight - player.getY())
        );

        return distanceToEdge < margin ? margin : 0;
    }

    public enum AIDecisionType {
        FLEE, HUNT, SEEK_FOOD, EXPLORE, STOP
    }

    private static class AIDecision {
        final AIDecisionType type;
        final Player targetPlayer;
        final Food targetFood;

        AIDecision(AIDecisionType type, Player targetPlayer, Food targetFood) {
            this.type = type;
            this.targetPlayer = targetPlayer;
            this.targetFood = targetFood;
        }
    }
}