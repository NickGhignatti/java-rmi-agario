package it.unibo.agar.model.rmi;

import it.unibo.agar.model.Food;
import it.unibo.agar.model.GameStateManager;
import it.unibo.agar.model.Player;
import it.unibo.agar.model.World;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

public class RemoteGameStateManager implements GameStateManager {
    private final GameServer gameServer;
    private final GameClientImpl gameClient;
    private final String playerId;

    public RemoteGameStateManager(GameServer gameServer, GameClientImpl gameClient, String playerId) {
        this.gameServer = gameServer;
        this.gameClient = gameClient;
        this.playerId = playerId;
        System.out.println("RemoteGameStateManager created for " + playerId);
    }

    @Override
    public World getWorld() {
        try {
            List<Player> players = gameClient.getCurrentPlayers();
            List<Food> foods = gameClient.getCurrentFoods();
            int width = gameServer.getWorldWidth();
            int height = gameServer.getWorldHeight();

            if (players == null) {
                players = new ArrayList<>();
            }
            if (foods == null) {
                foods = new ArrayList<>();
            }

            return new World(width, height, players, foods);
        } catch (RemoteException e) {
            System.err.println("Error getting world state: " + e.getMessage());
            return new World(1000, 1000, new ArrayList<>(), new ArrayList<>());
        }
    }

    @Override
    public void setPlayerDirection(String playerId, double dx, double dy) {
        try {
            gameServer.setPlayerDirection(playerId, dx, dy);
        } catch (RemoteException e) {
            System.err.println("Error setting player direction: " + e.getMessage());
        }
    }

    @Override
    public void tick() {
        // Tick is handled by the server
    }

    public boolean isPlayerAlive() {
        return gameClient.isAlive();
    }
}