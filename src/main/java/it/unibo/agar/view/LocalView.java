package it.unibo.agar.view;

import it.unibo.agar.model.GameStateManager;
import it.unibo.agar.model.Player;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.Optional;

public class LocalView extends JFrame {
    private static final double SENSITIVITY = 2;
    private final GamePanel gamePanel;
    private final GameStateManager gameStateManager;
    private final String playerId;
    private final JLabel statusLabel;

    public LocalView(GameStateManager gameStateManager, String playerId) {
        this.gameStateManager = gameStateManager;
        this.playerId = playerId;

        setTitle("Agar.io - Local View (" + playerId + ") (Java RMI)");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setPreferredSize(new Dimension(600, 600));
        setLayout(new BorderLayout());

        // Create status panel
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusLabel = new JLabel("Connected as " + playerId);
        statusPanel.add(statusLabel);
        add(statusPanel, BorderLayout.NORTH);

        // Create game panel
        this.gamePanel = new GamePanel(gameStateManager, playerId);
        add(this.gamePanel, BorderLayout.CENTER);

        setupMouseControls();

        pack();
        setLocationRelativeTo(null);
    }

    private void setupMouseControls() {
        gamePanel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                Optional<Player> playerOpt = gameStateManager.getWorld().getPlayerById(playerId);
                if (playerOpt.isPresent()) {
                    Point mousePos = e.getPoint();
                    double viewCenterX = gamePanel.getWidth() / 2.0;
                    double viewCenterY = gamePanel.getHeight() / 2.0;

                    double dx = mousePos.x - viewCenterX;
                    double dy = mousePos.y - viewCenterY;

                    double magnitude = Math.hypot(dx, dy);
                    if (magnitude > 0) {
                        gameStateManager.setPlayerDirection(playerId,
                                (dx / magnitude) * SENSITIVITY,
                                (dy / magnitude) * SENSITIVITY);
                    } else {
                        gameStateManager.setPlayerDirection(playerId, 0, 0);
                    }
                }
            }
        });
    }

    public void repaintView() {
        if (gamePanel != null) {
            SwingUtilities.invokeLater(() -> {
                gamePanel.repaint();
                updateStatus();
            });
        }
    }

    private void updateStatus() {
        Optional<Player> playerOpt = gameStateManager.getWorld().getPlayerById(playerId);
        playerOpt.ifPresent(player -> statusLabel.setText(String.format("%s - Mass: %.0f, Position: (%.0f, %.0f)",
                playerId, player.getMass(), player.getX(), player.getY())));
    }

    public void showDeathMessage() {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText(playerId + " - DEAD");
            statusLabel.setForeground(Color.RED);

            JOptionPane.showMessageDialog(this,
                    "You have been eaten! Game Over.",
                    "Game Over",
                    JOptionPane.INFORMATION_MESSAGE);

            // Disable mouse controls
            for (var listener : gamePanel.getMouseMotionListeners()) {
                gamePanel.removeMouseMotionListener(listener);
            }
        });
    }
}