import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Random;

public class PongGame extends JPanel implements ActionListener, KeyListener {

    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private static final int PADDLE_WIDTH = 100;
    private static final int PADDLE_HEIGHT = 15;
    private static final int BALL_RADIUS = 10;

    private int paddleX = WIDTH / 2 - PADDLE_WIDTH / 2;
    private int paddleY = HEIGHT - 50;

    private double ballX = WIDTH / 2.0;
    private double ballY = HEIGHT / 2.0;
    private double ballDX = 5;
    private double ballDY = -5;
    private double ballSpeed = 5.0;

    private int score = 0;
    private long startTime;
    private long lastSpeedIncreaseTime;
    private final long GAME_DURATION = 120_000; // 2 minutes
    private final long SPEED_INCREASE_INTERVAL = 10_000; // 10 seconds

    private boolean gameOver = false;
    private boolean leftPressed = false;
    private boolean rightPressed = false;

    private final Timer timer;

    public PongGame() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);

        timer = new Timer(16, this); // ~60 FPS
        startNewGame();
        timer.start();
    }

    private void startNewGame() {
        paddleX = WIDTH / 2 - PADDLE_WIDTH / 2;
        score = 0;
        startTime = System.currentTimeMillis();
        lastSpeedIncreaseTime = startTime;
        gameOver = false;
        resetBall();
    }

    private void resetBall() {
        ballX = WIDTH / 2.0;
        ballY = HEIGHT / 2.0;
        Random rand = new Random();
        ballDX = rand.nextBoolean() ? 5 : -5;
        ballDY = -5;
        ballSpeed = 5.0;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Top wall
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(4));
        g2d.drawLine(0, 50, WIDTH, 50);

        // Paddle
        g2d.fillRect(paddleX, paddleY, PADDLE_WIDTH, PADDLE_HEIGHT);

        // Ball
        g2d.fillOval((int) (ballX - BALL_RADIUS), (int) (ballY - BALL_RADIUS), 
                     BALL_RADIUS * 2, BALL_RADIUS * 2);

        // HUD
        g2d.setFont(new Font("Arial", Font.BOLD, 24));
        g2d.setColor(Color.WHITE);
        g2d.drawString("Score: " + score, 20, 35);

        long timeLeft = Math.max(0, GAME_DURATION - (System.currentTimeMillis() - startTime)) / 1000;
        g2d.drawString("Time: " + timeLeft, WIDTH - 160, 35);

        // Show current speed (optional)
        g2d.setFont(new Font("Arial", Font.PLAIN, 18));
        g2d.drawString("Speed: " + String.format("%.1f", ballSpeed), 20, 65);

        if (gameOver) {
            g2d.setFont(new Font("Arial", Font.BOLD, 48));
            g2d.setColor(Color.RED);
            String msg = "GAME OVER";
            int strWidth = g2d.getFontMetrics().stringWidth(msg);
            g2d.drawString(msg, (WIDTH - strWidth) / 2, HEIGHT / 2 - 30);

            g2d.setFont(new Font("Arial", Font.PLAIN, 24));
            g2d.setColor(Color.WHITE);
            String restart = "Press R to Restart";
            strWidth = g2d.getFontMetrics().stringWidth(restart);
            g2d.drawString(restart, (WIDTH - strWidth) / 2, HEIGHT / 2 + 30);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!gameOver) {
            updateGame();
        }
        repaint();
    }

    private void updateGame() {
        // Paddle movement
        if (leftPressed) paddleX -= 8;
        if (rightPressed) paddleX += 8;
        paddleX = Math.max(0, Math.min(WIDTH - PADDLE_WIDTH, paddleX));

        // Ball movement
        ballX += ballDX;
        ballY += ballDY;

        // Wall collisions
        if (ballX - BALL_RADIUS <= 0 || ballX + BALL_RADIUS >= WIDTH) {
            ballDX = -ballDX;
        }
        if (ballY - BALL_RADIUS <= 50) {
            ballDY = -ballDY;
        }

        // Paddle collision
        if (ballY + BALL_RADIUS >= paddleY && 
            ballY - BALL_RADIUS <= paddleY + PADDLE_HEIGHT &&
            ballX >= paddleX && ballX <= paddleX + PADDLE_WIDTH) {
            
            handlePaddleCollision();
        }

        // Bottom miss
        if (ballY + BALL_RADIUS > HEIGHT) {
            gameOver = true;
        }

        // Timer end
        if (System.currentTimeMillis() - startTime > GAME_DURATION) {
            gameOver = true;
        }

        // Increase speed every 10 seconds
        if (System.currentTimeMillis() - lastSpeedIncreaseTime >= SPEED_INCREASE_INTERVAL) {
            ballSpeed = Math.min(ballSpeed + 0.6, 14.0);  // Adjust 0.6 as needed
            lastSpeedIncreaseTime = System.currentTimeMillis();
            
            // Optional: Make current direction faster
            double currentSpeed = Math.sqrt(ballDX * ballDX + ballDY * ballDY);
            if (currentSpeed > 0) {
                ballDX = ballDX / currentSpeed * ballSpeed;
                ballDY = ballDY / currentSpeed * ballSpeed;
            }
        }
    }

    private void handlePaddleCollision() {
        double hitPos = (ballX - paddleX) / PADDLE_WIDTH;
        double angleFactor = (hitPos - 0.5) * 2;

        ballDY = -Math.abs(ballDY);           // Bounce up
        ballDX = ballSpeed * angleFactor * 1.8;

        score += 10;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_A) leftPressed = true;
        if (key == KeyEvent.VK_RIGHT || key == KeyEvent.VK_D) rightPressed = true;

        if (gameOver && key == KeyEvent.VK_R) {
            startNewGame();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_A) leftPressed = false;
        if (key == KeyEvent.VK_RIGHT || key == KeyEvent.VK_D) rightPressed = false;
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    public static void main(String[] args) {
        JFrame frame = new JFrame("1-Player Pong Survival");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(new PongGame());
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setVisible(true);
    }
}