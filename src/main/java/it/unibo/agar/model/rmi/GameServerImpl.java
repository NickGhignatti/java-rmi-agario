package it.unibo.agar.model.rmi;

import it.unibo.agar.model.*;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class GameServerImpl extends UnicastRemoteObject implements GameServer {
    private static final double PLAYER_SPEED = 2.0;
    private static final int MAX_FOOD_ITEMS = 150;
    private static final Random random = new Random();

    private final int worldWidth;
    private final int worldHeight;
    private final Map<String, RemotePlayer> remotePlayers;
    private final Map<String, GameClient> clients;
    private final Map<String, Position> playerDirections;
    private List<Food> foods;
    private final Timer gameTimer;

    public GameServerImpl(int worldWidth, int worldHeight, int numFoods) throws RemoteException {
        super();
        this.worldWidth = worldWidth;
        this.worldHeight = worldHeight;
        this.remotePlayers = new ConcurrentHashMap<>();
        this.clients = new ConcurrentHashMap<>();
        this.playerDirections = new ConcurrentHashMap<>();
        this.foods = new CopyOnWriteArrayList<>(GameInitializer.initialFoods(numFoods, worldWidth, worldHeight));

        System.out.println("GameServer initialized with " + numFoods + " foods");

        // Start game loop
        this.gameTimer = new Timer(true);
        this.gameTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                tick();
            }
        }, 1000, 30); // Start after 1 second, then 30ms intervals

        System.out.println("Game loop started");
    }

    @Override
    public synchronized void registerPlayer(RemotePlayer player) throws RemoteException {
        String playerId = player.getId();
        remotePlayers.put(playerId, player);
        playerDirections.put(playerId, Position.ZERO);
        System.out.println("Player " + playerId + " registered. Total players: " + remotePlayers.size());
    }

    @Override
    public synchronized void registerClient(GameClient client) throws RemoteException {
        String playerId = client.getPlayerId();
        clients.put(playerId, client);
        System.out.println("Client " + playerId + " registered. Total clients: " + clients.size());

        // Send initial game state immediately
        try {
            List<Player> allPlayers = getAllPlayers();
            List<Food> allFoods = getAllFoods();
            System.out.println("Sending initial state to " + playerId + ": " + allPlayers.size() + " players, " + allFoods.size() + " foods");
            client.updateGameState(allPlayers, allFoods);
            System.out.println("Initial game state sent to " + playerId);
        } catch (RemoteException e) {
            System.err.println("Failed to send initial game state to " + playerId + ": " + e.getMessage());
        }
    }

    @Override
    public synchronized void unregisterPlayer(String playerId) throws RemoteException {
        remotePlayers.remove(playerId);
        clients.remove(playerId);
        playerDirections.remove(playerId);
        System.out.println("Player " + playerId + " unregistered");
    }

    @Override
    public void setPlayerDirection(String playerId, double dx, double dy) throws RemoteException {
        if (remotePlayers.containsKey(playerId)) {
            playerDirections.put(playerId, Position.of(dx, dy));
        }
    }

    @Override
    public List<Player> getAllPlayers() throws RemoteException {
        List<Player> players = new ArrayList<>();
        for (RemotePlayer remotePlayer : remotePlayers.values()) {
            try {
                if (remotePlayer.isAlive()) {
                    players.add(remotePlayer.getPlayerData());
                }
            } catch (RemoteException e) {
                System.err.println("Error getting player data: " + e.getMessage());
            }
        }
        return players;
    }

    @Override
    public List<Food> getAllFoods() throws RemoteException {
        return new ArrayList<>(foods);
    }

    @Override
    public int getWorldWidth() throws RemoteException {
        return worldWidth;
    }

    @Override
    public int getWorldHeight() throws RemoteException {
        return worldHeight;
    }

    @Override
    public void notifyPlayerEaten(String playerId) throws RemoteException {
        RemotePlayer player = remotePlayers.get(playerId);
        if (player instanceof RemotePlayerImpl) {
            ((RemotePlayerImpl) player).setAlive(false);
        }

        GameClient client = clients.get(playerId);
        if (client != null) {
            try {
                client.notifyPlayerDeath();
            } catch (RemoteException e) {
                System.err.println("Error notifying client of death: " + e.getMessage());
            }
        }
    }

    @Override
    public boolean isPlayerAlive(String playerId) throws RemoteException {
        RemotePlayer player = remotePlayers.get(playerId);
        return player != null && player.isAlive();
    }

    private synchronized void tick() {
        try {
            moveAllPlayers();
            handleEating();
            notifyClients();
        } catch (Exception e) {
            System.err.println("Error in game tick: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void moveAllPlayers() throws RemoteException {
        for (Map.Entry<String, RemotePlayer> entry : remotePlayers.entrySet()) {
            String playerId = entry.getKey();
            RemotePlayer remotePlayer = entry.getValue();

            try {
                if (!remotePlayer.isAlive()) continue;

                Position direction = playerDirections.getOrDefault(playerId, Position.ZERO);
                Player currentPlayer = remotePlayer.getPlayerData();

                double newX = Math.max(0, Math.min(worldWidth, currentPlayer.getX() + direction.x() * PLAYER_SPEED));
                double newY = Math.max(0, Math.min(worldHeight, currentPlayer.getY() + direction.y() * PLAYER_SPEED));

                Player updatedPlayer = currentPlayer.moveTo(newX, newY);
                remotePlayer.updatePlayerData(updatedPlayer);
            } catch (RemoteException e) {
                System.err.println("Error moving player " + playerId + ": " + e.getMessage());
            }
        }
    }

    private void handleEating() throws RemoteException {
        List<Player> currentPlayers = getAllPlayers();
        List<Food> foodsToRemove = new ArrayList<>();
        List<String> playersToRemove = new ArrayList<>();

        for (Player player : currentPlayers) {
            RemotePlayer remotePlayer = remotePlayers.get(player.getId());
            try {
                if (!remotePlayer.isAlive()) continue;

                List<Food> eatenFoods = foods.stream()
                        .filter(food -> EatingManager.canEatFood(player, food))
                        .toList();

                List<Player> eatenPlayers = currentPlayers.stream()
                        .filter(other -> !other.getId().equals(player.getId()))
                        .filter(other -> EatingManager.canEatPlayer(player, other)).toList();

                Player grownPlayer = player;
                for (Food food : eatenFoods) {
                    grownPlayer = grownPlayer.grow(food);
                    foodsToRemove.add(food);
                }
                for (Player eatenPlayer : eatenPlayers) {
                    grownPlayer = grownPlayer.grow(eatenPlayer);
                    playersToRemove.add(eatenPlayer.getId());
                }

                if (grownPlayer.getMass() != player.getMass()) {
                    remotePlayer.updatePlayerData(grownPlayer);
                }
            } catch (RemoteException e) {
                System.err.println("Error handling eating for player " + player.getId() + ": " + e.getMessage());
            }
        }

        foods.removeAll(foodsToRemove);

        for (String playerId : playersToRemove) {
            notifyPlayerEaten(playerId);
        }

        while (foods.size() < MAX_FOOD_ITEMS) {
            String foodId = "f" + System.currentTimeMillis() + "_" + random.nextInt(1000);
            Food newFood = new Food(foodId, random.nextInt(worldWidth), random.nextInt(worldHeight), Food.DEFAULT_MASS);
            foods.add(newFood);
        }
    }

    private void notifyClients() {
        if (clients.isEmpty()) return;

        try {
            List<Player> allPlayers = getAllPlayers();
            List<Food> allFoods = getAllFoods();

            Iterator<Map.Entry<String, GameClient>> iterator = clients.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, GameClient> entry = iterator.next();
                String playerId = entry.getKey();
                GameClient client = entry.getValue();

                try {
                    client.updateGameState(allPlayers, allFoods);
                } catch (RemoteException e) {
                    System.err.println("Failed to notify client " + playerId + ", removing: " + e.getMessage());
                    iterator.remove();
                    try {
                        unregisterPlayer(playerId);
                    } catch (RemoteException ex) {
                        // Client is dead
                    }
                }
            }
        } catch (RemoteException e) {
            System.err.println("Error notifying clients: " + e.getMessage());
        }
    }

    public void shutdown() {
        if (gameTimer != null) {
            gameTimer.cancel();
        }
    }
}