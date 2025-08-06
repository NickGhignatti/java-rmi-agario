package it.unibo.agar.model.rmi;

import it.unibo.agar.model.Player;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class RemotePlayerImpl extends UnicastRemoteObject implements RemotePlayer {
    private Player player;
    private boolean alive;

    public RemotePlayerImpl(Player player) throws RemoteException {
        super();
        this.player = player;
        this.alive = true;
    }

    @Override
    public String getId() throws RemoteException {
        return player.getId();
    }

    @Override
    public double getX() throws RemoteException {
        return player.getX();
    }

    @Override
    public double getY() throws RemoteException {
        return player.getY();
    }

    @Override
    public double getMass() throws RemoteException {
        return player.getMass();
    }

    @Override
    public double getRadius() throws RemoteException {
        return player.getRadius();
    }

    @Override
    public Player getPlayerData() throws RemoteException {
        return player;
    }

    @Override
    public void updatePlayerData(Player player) throws RemoteException {
        this.player = player;
    }

    @Override
    public boolean isAlive() throws RemoteException {
        return alive;
    }

    public void setAlive(boolean alive) {
        this.alive = alive;
    }
}