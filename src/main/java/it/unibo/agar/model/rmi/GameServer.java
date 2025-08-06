package it.unibo.agar.model.rmi;

import it.unibo.agar.model.Food;
import it.unibo.agar.model.Player;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Remote interface for the game server
 */
public interface GameServer extends Remote {
    void registerPlayer(RemotePlayer player) throws RemoteException;
    void registerClient(GameClient client) throws RemoteException; // Added to interface
    void unregisterPlayer(String playerId) throws RemoteException;
    void setPlayerDirection(String playerId, double dx, double dy) throws RemoteException;
    List<Player> getAllPlayers() throws RemoteException;
    List<Food> getAllFoods() throws RemoteException;
    int getWorldWidth() throws RemoteException;
    int getWorldHeight() throws RemoteException;
    void notifyPlayerEaten(String playerId) throws RemoteException;
    boolean isPlayerAlive(String playerId) throws RemoteException;
}