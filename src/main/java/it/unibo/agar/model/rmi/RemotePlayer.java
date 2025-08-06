package it.unibo.agar.model.rmi;

import it.unibo.agar.model.Player;
import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Remote interface for Player operations
 */
public interface RemotePlayer extends Remote {
    String getId() throws RemoteException;
    double getX() throws RemoteException;
    double getY() throws RemoteException;
    double getMass() throws RemoteException;
    double getRadius() throws RemoteException;
    Player getPlayerData() throws RemoteException;
    void updatePlayerData(Player player) throws RemoteException;
    boolean isAlive() throws RemoteException;
}