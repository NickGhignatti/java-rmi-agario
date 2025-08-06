package it.unibo.agar.model.rmi;

import it.unibo.agar.model.Player;
import it.unibo.agar.model.Food;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Remote interface for game client callbacks
 */
public interface GameClient extends Remote {
    void updateGameState(List<Player> players, List<Food> foods) throws RemoteException;
    void notifyPlayerDeath() throws RemoteException;
    String getPlayerId() throws RemoteException;
}