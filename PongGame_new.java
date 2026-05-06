import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;

public class PongGame extends JPanel implements ActionListener, KeyListener {

    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private static final int PADDLE_WIDTH = 100;
    private static final int PADDLE_HEIGHT = 15;
    private static final int BALL_RADIUS = 10;
    private static final int BRICK_ROWS = 5;
    private static final int BRICK_COLS = 10;
    private static final int BRICK_WIDTH = 70;
    private static final int BRICK_HEIGHT = 25;
    private static final int BRICK_GAP = 5;

    private int paddleX = WIDTH / 2 - PADDLE_WIDTH / 2;
    private int paddleY = HEIGHT - 50;

    private double ballX = WIDTH / 2.0;
    private double ballY = HEIGHT / 2.0;
    private double ballDX = 5;
    private double ballDY = -5;
    private double ballSpeed = 5.0;

    private Color ballColor = Color.WHITE;

    private int score = 0;
    private long startTime;
    private long lastSpeedIncreaseTime;
    private final long GAME_DURATION = 120_000; // 2 minutes
    private final long SPEED_INCREASE_INTERVAL = 10_000; // 10 seconds

    private boolean gameOver = false;
    private boolean leftPressed = false;
    private boolean rightPressed = false;

    // Bricks
    private ArrayList<Brick> bricks = new ArrayList<>();
    private final Random rand = new Random();

    private final Timer timer;

    // Inner Brick Class
    private class Brick {
        int x, y;
        boolean visible = true;
        Color color;

        Brick(int x, int y, Color color) {
            this.x = x;
            this.y = y;
            this.color = color;
        }
    }

    public PongGame() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(0, 0, 40)); // Dark Blue
        setFocusable(true);
        addKeyListener(this);

        timer = new Timer(16, this);
        createBricks();
        startNewGame();
        timer.start();
    }

    private void createBricks() {
        bricks.clear();
        Color[] colors = {Color.RED, Color.ORANGE, Color.YELLOW, Color.GREEN, Color.CYAN};

        for (int row = 0; row < BRICK_ROWS; row++) {
            for (int col = 0; col < BRICK_COLS; col++) {
                int x = 40 + col * (BRICK_WIDTH + BRICK_GAP);
                int y = 80 + row * (BRICK_HEIGHT + BRICK_GAP);
                bricks.add(new Brick(x, y, colors[row % colors.length]));
            }
        }
    }

    private void startNewGame() {
        paddleX = WIDTH / 2 - PADDLE_WIDTH / 2;
        score = 0;
        startTime = System.currentTimeMillis();
        lastSpeedIncreaseTime = startTime;
        gameOver = false;
        ballSpeed = 5.0;
        createBricks();
        resetBall();
    }

    private void resetBall() {
        ballX = WIDTH / 2.0;
        ballY = HEIGHT / 2.0;
        ballDX = rand.nextBoolean() ? 5 : -5;
        ballDY = -5;
        ballColor = Color.WHITE;
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

        // Draw Bricks
        for (Brick brick : bricks) {
            if (brick.visible) {
                g2d.setColor(brick.color);
                g2d.fillRect(brick.x, brick.y, BRICK_WIDTH, BRICK_HEIGHT);
                g2d.setColor(Color.BLACK);
                g2d.drawRect(brick.x, brick.y, BRICK_WIDTH, BRICK_HEIGHT);
            }
        }

        // Paddle
        g2d.setColor(Color.WHITE);
        g2d.fillRect(paddleX, paddleY, PADDLE_WIDTH, PADDLE_HEIGHT);

        // Ball
        g2d.setColor(ballColor);
        g2d.fillOval((int) (ballX - BALL_RADIUS), (int) (ballY - BALL_RADIUS), 
                     BALL_RADIUS * 2, BALL_RADIUS * 2);

        // HUD
        g2d.setFont(new Font("Arial", Font.BOLD, 24));
        g2d.setColor(Color.WHITE);
        g2d.drawString("Score: " + score, 20, 35);

        long timeLeft = Math.max(0, GAME_DURATION - (System.currentTimeMillis() - startTime)) / 1000;
        g2d.drawString("Time: " + timeLeft, WIDTH - 160, 35);
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
            changeBallColor();
        }
        if (ballY - BALL_RADIUS <= 50) {
            ballDY = -ballDY;
            changeBallColor();
        }

        // Paddle collision
        if (ballY + BALL_RADIUS >= paddleY && 
            ballY - BALL_RADIUS <= paddleY + PADDLE_HEIGHT &&
            ballX >= paddleX && ballX <= paddleX + PADDLE_WIDTH) {
            
            handlePaddleCollision();
        }

        // Brick collision
        for (Brick brick : bricks) {
            if (brick.visible) {
                if (ballX + BALL_RADIUS > brick.x && ballX - BALL_RADIUS < brick.x + BRICK_WIDTH &&
                    ballY + BALL_RADIUS > brick.y && ballY - BALL_RADIUS < brick.y + BRICK_HEIGHT) {
                    
                    brick.visible = false;
                    ballDY = -ballDY;
                    score += 20;
                    changeBallColor();
                    break;
                }
            }
        }

        // Bottom miss
        if (ballY + BALL_RADIUS > HEIGHT) {
            gameOver = true;
        }

        // Time up
        if (System.currentTimeMillis() - startTime > GAME_DURATION) {
            gameOver = true;
        }

        // Speed increase every 10 seconds (More noticeable)
        if (System.currentTimeMillis() - lastSpeedIncreaseTime >= SPEED_INCREASE_INTERVAL) {
            ballSpeed = Math.min(ballSpeed + 0.8, 15.0);
            lastSpeedIncreaseTime = System.currentTimeMillis();

            // Apply new speed
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

        ballDY = -Math.abs(ballDY);
        ballDX = ballSpeed * angleFactor * 1.8;
        score += 10;
        changeBallColor();
    }

    private void changeBallColor() {
        Color[] colors = {Color.WHITE, Color.CYAN, Color.YELLOW, Color.MAGENTA, 
                          Color.ORANGE, Color.PINK, Color.GREEN};
        ballColor = colors[rand.nextInt(colors.length)];
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
        JFrame frame = new JFrame("Brick Breaker Pong Survival");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(new PongGame());
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setVisible(true);
    }
}