package it.unibo.agar.model.rmi;


import it.unibo.agar.model.Food;
import it.unibo.agar.model.Player;
import it.unibo.agar.view.GlobalView;
import it.unibo.agar.view.LocalView;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import javax.swing.SwingUtilities;

public class GameClientImpl extends UnicastRemoteObject implements GameClient {
    private final String playerId;
    private LocalView localView;
    private GlobalView globalView;
    private volatile List<Player> currentPlayers;
    private volatile List<Food> currentFoods;
    private volatile boolean isAlive = true;

    public GameClientImpl(String playerId) throws RemoteException {
        super();
        this.playerId = playerId;
        System.out.println("GameClient created for " + playerId);
    }

    @Override
    public void updateGameState(List<Player> players, List<Food> foods) throws RemoteException {
        this.currentPlayers = players;
        this.currentFoods = foods;

        // Debug logging
        if (players != null && foods != null) {
            System.out.println("GameState update received: " + players.size() + " players, " + foods.size() + " foods");
        }

        if (localView != null) {
            SwingUtilities.invokeLater(() -> localView.repaintView());
        }

        if (globalView != null) {
            SwingUtilities.invokeLater(() -> globalView.repaintView());
        }
    }

    @Override
    public void notifyPlayerDeath() throws RemoteException {
        this.isAlive = false;
        System.out.println("Player " + playerId + " has died!");
        SwingUtilities.invokeLater(() -> {
            if (localView != null) {
                localView.setTitle("Agar.io - " + playerId + " (DEAD)");
                localView.showDeathMessage();
            }
        });
    }

    @Override
    public String getPlayerId() throws RemoteException {
        return playerId;
    }

    public void setLocalView(LocalView localView) {
        this.localView = localView;
        System.out.println("LocalView set for " + playerId);
    }

    public void setGlobalView(GlobalView globalView) {
        this.globalView = globalView;
        System.out.println("GlobalView set for " + playerId);
    }

    public List<Player> getCurrentPlayers() {
        return currentPlayers;
    }

    public List<Food> getCurrentFoods() {
        return currentFoods;
    }

    public boolean isAlive() {
        return isAlive;
    }
}