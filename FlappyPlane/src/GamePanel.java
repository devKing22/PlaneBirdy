import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class GamePanel extends JPanel implements ActionListener, KeyListener, MouseMotionListener, MouseListener {
    public static final int WIDTH = 500;
    public static final int HEIGHT = 600;
    private static final int GROUND_HEIGHT = 60;
    private static final int BASE_SPEED = 3;

    private enum GameState { MENU, CONTROL_SELECT, PLAYING, GAME_OVER }
    private enum ControlMode { KEYBOARD, MOUSE }

    private GameState state;
    private ControlMode controlMode;
    private int selectedOption; // 0 = teclado, 1 = mouse
    private Plane plane;
    private ArrayList<Obstacle> obstacles;
    private Timer gameTimer;
    private Random random;
    private int score;
    private int bestScore;
    private int obstacleSpeed;
    private int spawnTimer;
    private int spawnInterval;
    private int groundOffset;
    private int lastSpeedUpScore;

    // Parallax backgrounds
    private double bgMountainOffset;
    private double bgCityOffset;

    // Clouds
    private double[] cloudX;
    private int[] cloudY;
    private double[] cloudSpeed;

    // Visual
    private float menuPlaneBob;
    private int flashAlpha;
    private long gameOverTime;
    private int mouseY;

    // Music
    private MusicPlayer musicPlayer;
    private boolean musicEnabled;
    private static final String MUSIC_PATH = "C:\\Users\\Samsung\\OneDrive\\pasta_marco\\Projetos Java\\music.mp3";

    public GamePanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setFocusable(true);
        addKeyListener(this);
        addMouseMotionListener(this);
        addMouseListener(this);

        random = new Random();
        bestScore = 0;
        selectedOption = 0;
        controlMode = ControlMode.KEYBOARD;
        mouseY = HEIGHT / 2;

        // Init clouds
        cloudX = new double[5];
        cloudY = new int[5];
        cloudSpeed = new double[5];
        for (int i = 0; i < 5; i++) {
            cloudX[i] = random.nextInt(WIDTH + 100);
            cloudY[i] = random.nextInt(200) + 20;
            cloudSpeed[i] = 0.3 + random.nextDouble() * 0.5;
        }

        // Init music
        musicEnabled = true;
        if (new File(MUSIC_PATH).exists()) {
            musicPlayer = new MusicPlayer(MUSIC_PATH);
            musicPlayer.play();
        } else {
            System.out.println("Arquivo de musica nao encontrado: " + MUSIC_PATH);
            musicEnabled = false;
        }

        resetGame();

        gameTimer = new Timer(16, this); // ~60 FPS
        gameTimer.start();
    }

    private void resetGame() {
        plane = new Plane(80, HEIGHT / 2 - 12);
        obstacles = new ArrayList<>();
        score = 0;
        obstacleSpeed = BASE_SPEED;
        spawnTimer = 0;
        spawnInterval = 95;
        groundOffset = 0;
        bgMountainOffset = 0;
        bgCityOffset = 0;
        menuPlaneBob = 0;
        flashAlpha = 0;
        gameOverTime = 0;
        lastSpeedUpScore = 0;
        state = GameState.MENU;
    }

    private void startGame() {
        plane = new Plane(80, HEIGHT / 2 - 12);
        plane.setMouseMode(controlMode == ControlMode.MOUSE);
        if (controlMode == ControlMode.MOUSE) {
            setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        } else {
            setCursor(Cursor.getDefaultCursor());
        }
        obstacles.clear();
        score = 0;
        obstacleSpeed = BASE_SPEED;
        spawnTimer = 0;
        flashAlpha = 0;
        lastSpeedUpScore = 0;
        state = GameState.PLAYING;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        update();
        repaint();
    }

    private void update() {
        // Clouds always animate
        for (int i = 0; i < cloudX.length; i++) {
            cloudX[i] -= cloudSpeed[i];
            if (cloudX[i] < -100) {
                cloudX[i] = WIDTH + random.nextInt(50);
                cloudY[i] = random.nextInt(180) + 20;
            }
        }

        if (state == GameState.MENU || state == GameState.CONTROL_SELECT) {
            menuPlaneBob += 0.04f;
            plane.setY(HEIGHT / 2 - 12 + Math.sin(menuPlaneBob) * 20);
            groundOffset = (groundOffset + 2) % 30;
            bgMountainOffset = (bgMountainOffset + 0.3) % WIDTH;
            bgCityOffset = (bgCityOffset + 0.8) % WIDTH;
            return;
        }

        if (state == GameState.GAME_OVER) {
            if (flashAlpha > 0) flashAlpha -= 10;
            return;
        }

        // Playing
        if (controlMode == ControlMode.MOUSE) {
            plane.setTargetY(mouseY - plane.getPlaneHeight() / 2);
        }
        plane.update();
        groundOffset = (groundOffset + obstacleSpeed) % 30;
        bgMountainOffset = (bgMountainOffset + obstacleSpeed * 0.2) % WIDTH;
        bgCityOffset = (bgCityOffset + obstacleSpeed * 0.5) % WIDTH;

        // Spawn obstacles
        spawnTimer++;
        if (spawnTimer >= spawnInterval) {
            int minGapY = 70;
            int maxGapY = HEIGHT - GROUND_HEIGHT - 230;
            int gapY = random.nextInt(maxGapY - minGapY) + minGapY;
            obstacles.add(new Obstacle(WIDTH, gapY, HEIGHT, GROUND_HEIGHT));
            spawnTimer = 0;
        }

        // Update obstacles
        Iterator<Obstacle> it = obstacles.iterator();
        while (it.hasNext()) {
            Obstacle obs = it.next();
            obs.update(obstacleSpeed);

            if (!obs.isScored() && obs.getX() + obs.getWidth() < plane.getX()) {
                obs.setScored(true);
                score++;

                // +2 velocidade a cada 10 pontos
                int speedLevel = score / 10;
                int expectedSpeed = BASE_SPEED + speedLevel * 2;
                if (expectedSpeed != obstacleSpeed && expectedSpeed <= 15) {
                    obstacleSpeed = expectedSpeed;
                }

                if (score % 10 == 0 && spawnInterval > 55) {
                    spawnInterval -= 5;
                }
            }

            if (obs.isOffScreen()) {
                it.remove();
            }
        }

        checkCollisions();
    }

    private void checkCollisions() {
        Rectangle planeBounds = plane.getBounds();
        int groundTop = HEIGHT - GROUND_HEIGHT;

        // Ground and ceiling
        if (plane.getY() + plane.getPlaneHeight() > groundTop || plane.getY() < 0) {
            gameOver();
            return;
        }

        // Obstacles
        for (Obstacle obs : obstacles) {
            if (planeBounds.intersects(obs.getTopBounds()) ||
                planeBounds.intersects(obs.getBottomBounds())) {
                gameOver();
                return;
            }
        }
    }

    private void gameOver() {
        state = GameState.GAME_OVER;
        flashAlpha = 200;
        gameOverTime = System.currentTimeMillis();
        setCursor(Cursor.getDefaultCursor());
        if (score > bestScore) bestScore = score;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawSky(g2d);
        drawClouds(g2d);
        drawMountains(g2d);
        drawCityBackground(g2d);

        for (Obstacle obs : obstacles) {
            obs.draw(g2d);
        }

        drawGround(g2d);
        plane.draw(g2d);

        switch (state) {
            case MENU:
                drawMenu(g2d);
                break;
            case CONTROL_SELECT:
                drawControlSelect(g2d);
                break;
            case PLAYING:
                drawHUD(g2d);
                if (controlMode == ControlMode.MOUSE) {
                    drawMouseGuide(g2d);
                }
                break;
            case GAME_OVER:
                drawHUD(g2d);
                drawGameOver(g2d);
                break;
        }

        if (flashAlpha > 0) {
            g2d.setColor(new Color(255, 255, 255, Math.min(flashAlpha, 255)));
            g2d.fillRect(0, 0, WIDTH, HEIGHT);
        }
    }

    private void drawSky(Graphics2D g2d) {
        GradientPaint sky = new GradientPaint(0, 0, new Color(25, 80, 150),
                                               0, HEIGHT, new Color(120, 180, 240));
        g2d.setPaint(sky);
        g2d.fillRect(0, 0, WIDTH, HEIGHT);
    }

    private void drawClouds(Graphics2D g2d) {
        for (int i = 0; i < cloudX.length; i++) {
            g2d.setColor(new Color(255, 255, 255, 140));
            int cx = (int) cloudX[i];
            int cy = cloudY[i];
            g2d.fillOval(cx, cy, 70, 25);
            g2d.fillOval(cx + 15, cy - 10, 45, 25);
            g2d.fillOval(cx + 35, cy, 55, 22);
        }
    }

    private void drawMountains(Graphics2D g2d) {
        int baseY = HEIGHT - GROUND_HEIGHT - 30;
        g2d.setColor(new Color(60, 90, 60, 120));
        int offset = (int) bgMountainOffset;
        for (int i = -1; i < 5; i++) {
            int mx = i * 140 - offset;
            int[] xp = {mx, mx + 70, mx + 140};
            int[] yp = {baseY, baseY - 90 - (i % 3) * 30, baseY};
            g2d.fillPolygon(xp, yp, 3);
        }
        g2d.setColor(new Color(50, 80, 50, 100));
        for (int i = -1; i < 5; i++) {
            int mx = i * 120 - offset / 2 + 60;
            int[] xp = {mx, mx + 60, mx + 120};
            int[] yp = {baseY, baseY - 60 - (i % 2) * 25, baseY};
            g2d.fillPolygon(xp, yp, 3);
        }
    }

    private void drawCityBackground(Graphics2D g2d) {
        int baseY = HEIGHT - GROUND_HEIGHT;
        int offset = (int) bgCityOffset;
        int[] heights = {50, 80, 35, 65, 90, 45, 70, 55, 85, 40, 75, 60};

        for (int i = -1; i < 12; i++) {
            int bx = i * 50 - offset;
            int h = heights[Math.abs(i) % heights.length];
            g2d.setColor(new Color(40, 50, 70, 150));
            g2d.fillRect(bx, baseY - h, 40, h);
            g2d.setColor(new Color(255, 230, 140, 80));
            for (int wy = baseY - h + 5; wy < baseY - 5; wy += 12) {
                for (int wx = bx + 5; wx < bx + 35; wx += 10) {
                    if (random.nextInt(10) < 7) {
                        g2d.fillRect(wx, wy, 5, 6);
                    }
                }
            }
        }
    }

    private void drawGround(Graphics2D g2d) {
        int groundY = HEIGHT - GROUND_HEIGHT;

        g2d.setColor(new Color(70, 75, 80));
        g2d.fillRect(0, groundY, WIDTH, GROUND_HEIGHT);

        g2d.setColor(new Color(200, 200, 200));
        g2d.setStroke(new BasicStroke(3));
        for (int i = -groundOffset; i < WIDTH + 30; i += 30) {
            g2d.fillRect(i, groundY + GROUND_HEIGHT / 2 - 2, 15, 4);
        }

        g2d.setColor(new Color(255, 200, 50));
        g2d.fillRect(0, groundY, WIDTH, 3);
        g2d.fillRect(0, groundY + GROUND_HEIGHT - 3, WIDTH, 3);

        g2d.setStroke(new BasicStroke(1));
        for (int i = -groundOffset; i < WIDTH + 30; i += 60) {
            g2d.setColor(new Color(50, 255, 50, 180));
            g2d.fillOval(i, groundY + 6, 6, 6);
            g2d.fillOval(i, groundY + GROUND_HEIGHT - 12, 6, 6);
        }
    }

    // ==================== MENU PRINCIPAL ====================
    private void drawMenu(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 60));
        g2d.fillRect(0, 0, WIDTH, 200);

        // Title
        g2d.setFont(new Font("Arial", Font.BOLD, 52));
        String title = "FLAPPY PLANE";
        FontMetrics fm = g2d.getFontMetrics();
        int titleX = (WIDTH - fm.stringWidth(title)) / 2;

        g2d.setColor(new Color(20, 40, 80));
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                g2d.drawString(title, titleX + dx, 120 + dy);
            }
        }
        g2d.setColor(new Color(255, 255, 255));
        g2d.drawString(title, titleX, 120);

        // Subtitle
        g2d.setFont(new Font("Arial", Font.ITALIC, 16));
        g2d.setColor(new Color(200, 220, 255));
        String sub = "Aventura nos Ceus!";
        fm = g2d.getFontMetrics();
        g2d.drawString(sub, (WIDTH - fm.stringWidth(sub)) / 2, 150);

        // Start prompt
        g2d.setFont(new Font("Arial", Font.BOLD, 22));
        int alpha = (int)(180 + 75 * Math.sin(menuPlaneBob * 2.5));
        g2d.setColor(new Color(255, 255, 100, alpha));
        String start = "Pressione ENTER para voar!";
        fm = g2d.getFontMetrics();
        g2d.drawString(start, (WIDTH - fm.stringWidth(start)) / 2, HEIGHT / 2 + 80);

        // Best score
        if (bestScore > 0) {
            g2d.setFont(new Font("Arial", Font.BOLD, 16));
            g2d.setColor(new Color(255, 215, 0));
            String best = "Recorde: " + bestScore;
            fm = g2d.getFontMetrics();
            g2d.drawString(best, (WIDTH - fm.stringWidth(best)) / 2, HEIGHT / 2 + 115);
        }

        // Music indicator
        drawMusicIndicator(g2d);
    }

    // ==================== TELA DE SELECAO DE CONTROLE ====================
    private void drawControlSelect(Graphics2D g2d) {
        // Overlay
        g2d.setColor(new Color(0, 0, 0, 100));
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        // Panel
        int panelW = 380;
        int panelH = 340;
        int panelX = (WIDTH - panelW) / 2;
        int panelY = (HEIGHT - panelH) / 2 - 20;

        GradientPaint panelBg = new GradientPaint(panelX, panelY, new Color(30, 45, 75),
                                                    panelX, panelY + panelH, new Color(20, 30, 55));
        g2d.setPaint(panelBg);
        g2d.fillRoundRect(panelX, panelY, panelW, panelH, 20, 20);

        g2d.setColor(new Color(100, 150, 255, 180));
        g2d.setStroke(new BasicStroke(3));
        g2d.drawRoundRect(panelX, panelY, panelW, panelH, 20, 20);

        // Title
        g2d.setFont(new Font("Arial", Font.BOLD, 28));
        g2d.setColor(Color.WHITE);
        String title = "MODO DE CONTROLE";
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(title, (WIDTH - fm.stringWidth(title)) / 2, panelY + 40);

        // Divider
        g2d.setColor(new Color(100, 140, 200, 80));
        g2d.fillRect(panelX + 20, panelY + 55, panelW - 40, 2);

        // Instruction
        g2d.setFont(new Font("Arial", Font.PLAIN, 14));
        g2d.setColor(new Color(180, 200, 230));
        String inst = "Use W/S para escolher, ENTER para confirmar";
        fm = g2d.getFontMetrics();
        g2d.drawString(inst, (WIDTH - fm.stringWidth(inst)) / 2, panelY + 78);

        // Option 1: Keyboard
        drawControlOption(g2d, panelX + 25, panelY + 95, panelW - 50, 100,
                selectedOption == 0,
                "TECLADO", "W / S",
                "Use W para subir e S para descer.",
                "Controle preciso com as teclas.");

        // Option 2: Mouse
        drawControlOption(g2d, panelX + 25, panelY + 210, panelW - 50, 100,
                selectedOption == 1,
                "MOUSE", "\u2191\u2193",
                "O aviao segue a posicao do mouse.",
                "Controle suave e intuitivo.");
    }

    private void drawControlOption(Graphics2D g2d, int x, int y, int w, int h,
                                    boolean selected, String name, String icon,
                                    String desc1, String desc2) {
        // Background
        if (selected) {
            GradientPaint selBg = new GradientPaint(x, y, new Color(40, 80, 160),
                                                      x, y + h, new Color(30, 60, 130));
            g2d.setPaint(selBg);
            g2d.fillRoundRect(x, y, w, h, 12, 12);

            // Glowing border
            g2d.setColor(new Color(100, 180, 255, 220));
            g2d.setStroke(new BasicStroke(3));
            g2d.drawRoundRect(x, y, w, h, 12, 12);

            // Arrow indicator
            g2d.setColor(new Color(255, 220, 80));
            int[] arrowX = {x - 15, x - 5, x - 15};
            int[] arrowY = {y + h / 2 - 8, y + h / 2, y + h / 2 + 8};
            g2d.fillPolygon(arrowX, arrowY, 3);
        } else {
            g2d.setColor(new Color(40, 50, 70, 150));
            g2d.fillRoundRect(x, y, w, h, 12, 12);
            g2d.setColor(new Color(80, 100, 140, 100));
            g2d.setStroke(new BasicStroke(1.5f));
            g2d.drawRoundRect(x, y, w, h, 12, 12);
        }

        // Icon box
        int iconBoxSize = 55;
        int iconX = x + 15;
        int iconY = y + (h - iconBoxSize) / 2;
        g2d.setColor(selected ? new Color(60, 100, 180) : new Color(50, 60, 80));
        g2d.fillRoundRect(iconX, iconY, iconBoxSize, iconBoxSize, 10, 10);
        g2d.setColor(selected ? new Color(120, 180, 255) : new Color(80, 100, 130));
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRoundRect(iconX, iconY, iconBoxSize, iconBoxSize, 10, 10);

        // Icon text
        g2d.setFont(new Font("Arial", Font.BOLD, 18));
        g2d.setColor(selected ? Color.WHITE : new Color(160, 170, 190));
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(icon, iconX + (iconBoxSize - fm.stringWidth(icon)) / 2,
                       iconY + iconBoxSize / 2 + 7);

        // Name
        int textX = iconX + iconBoxSize + 18;
        g2d.setFont(new Font("Arial", Font.BOLD, 20));
        g2d.setColor(selected ? Color.WHITE : new Color(160, 170, 190));
        g2d.drawString(name, textX, y + 35);

        // Description
        g2d.setFont(new Font("Arial", Font.PLAIN, 13));
        g2d.setColor(selected ? new Color(190, 210, 240) : new Color(120, 130, 150));
        g2d.drawString(desc1, textX, y + 58);
        g2d.drawString(desc2, textX, y + 76);
    }

    private void drawMouseGuide(Graphics2D g2d) {
        // Thin horizontal line showing mouse Y position
        g2d.setColor(new Color(255, 255, 255, 40));
        g2d.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0,
                new float[]{6, 6}, 0));
        g2d.drawLine(0, mouseY, WIDTH, mouseY);

        // Small crosshair at plane X
        int cx = (int) plane.getX() + plane.getPlaneWidth() / 2;
        g2d.setColor(new Color(255, 255, 255, 60));
        g2d.setStroke(new BasicStroke(1));
        g2d.drawOval(cx - 8, mouseY - 8, 16, 16);
    }

    private void drawHUD(Graphics2D g2d) {
        // Score background
        g2d.setColor(new Color(0, 0, 0, 80));
        g2d.fillRoundRect(WIDTH / 2 - 40, 10, 80, 50, 10, 10);

        String s = String.valueOf(score);
        g2d.setFont(new Font("Arial", Font.BOLD, 42));
        FontMetrics fm = g2d.getFontMetrics();
        int x = (WIDTH - fm.stringWidth(s)) / 2;

        g2d.setColor(new Color(20, 40, 80));
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                g2d.drawString(s, x + dx, 50 + dy);
            }
        }
        g2d.setColor(Color.WHITE);
        g2d.drawString(s, x, 50);

        // Speed indicator
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        g2d.setColor(new Color(255, 255, 255, 150));
        g2d.drawString("VEL: " + obstacleSpeed, 10, 25);

        // Altitude indicator
        int alt = (int)((HEIGHT - GROUND_HEIGHT - plane.getY()) / 5);
        g2d.drawString("ALT: " + Math.max(0, alt) + "m", 10, 42);

        // Control mode indicator
        String modeStr = controlMode == ControlMode.MOUSE ? "MOUSE" : "TECLADO";
        g2d.setFont(new Font("Arial", Font.PLAIN, 11));
        g2d.setColor(new Color(255, 255, 255, 100));
        fm = g2d.getFontMetrics();
        g2d.drawString(modeStr, WIDTH - fm.stringWidth(modeStr) - 10, 25);

        // Next speed up indicator
        int nextSpeedAt = ((score / 10) + 1) * 10;
        g2d.setColor(new Color(255, 200, 50, 120));
        g2d.drawString("+VEL em: " + (nextSpeedAt - score) + " pts", WIDTH - fm.stringWidth("+VEL em: 00 pts") - 10, 42);

        // Music indicator
        drawMusicIndicator(g2d);
    }

    private void drawGameOver(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 140));
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        int panelW = 300;
        int panelH = 260;
        int panelX = (WIDTH - panelW) / 2;
        int panelY = (HEIGHT - panelH) / 2 - 20;

        GradientPaint panelBg = new GradientPaint(panelX, panelY, new Color(40, 55, 85),
                                                    panelX, panelY + panelH, new Color(25, 35, 60));
        g2d.setPaint(panelBg);
        g2d.fillRoundRect(panelX, panelY, panelW, panelH, 20, 20);

        g2d.setColor(new Color(100, 140, 200));
        g2d.setStroke(new BasicStroke(3));
        g2d.drawRoundRect(panelX, panelY, panelW, panelH, 20, 20);

        // Crash text
        g2d.setFont(new Font("Arial", Font.BOLD, 38));
        g2d.setColor(new Color(255, 80, 60));
        String crash = "CRASH!";
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(crash, (WIDTH - fm.stringWidth(crash)) / 2, panelY + 50);

        g2d.setColor(new Color(100, 130, 180, 100));
        g2d.fillRect(panelX + 20, panelY + 65, panelW - 40, 2);

        // Score
        g2d.setFont(new Font("Arial", Font.BOLD, 24));
        g2d.setColor(Color.WHITE);
        String sc = "Distancia: " + score;
        fm = g2d.getFontMetrics();
        g2d.drawString(sc, (WIDTH - fm.stringWidth(sc)) / 2, panelY + 105);

        // Max speed reached
        g2d.setFont(new Font("Arial", Font.PLAIN, 16));
        g2d.setColor(new Color(180, 200, 230));
        String spd = "Vel. Max: " + obstacleSpeed;
        fm = g2d.getFontMetrics();
        g2d.drawString(spd, (WIDTH - fm.stringWidth(spd)) / 2, panelY + 130);

        // Best
        g2d.setFont(new Font("Arial", Font.BOLD, 24));
        g2d.setColor(new Color(255, 215, 0));
        String best = "Recorde: " + bestScore;
        fm = g2d.getFontMetrics();
        g2d.drawString(best, (WIDTH - fm.stringWidth(best)) / 2, panelY + 162);

        // Rank
        String rank;
        Color rankColor;
        if (score >= 40) { rank = "ACE PILOTO"; rankColor = new Color(255, 215, 0); }
        else if (score >= 25) { rank = "CAPITAO"; rankColor = new Color(192, 192, 192); }
        else if (score >= 15) { rank = "TENENTE"; rankColor = new Color(205, 127, 50); }
        else if (score >= 5) { rank = "CADETE"; rankColor = new Color(100, 180, 100); }
        else { rank = "NOVATO"; rankColor = new Color(150, 150, 150); }

        g2d.setColor(rankColor.darker());
        g2d.fillRoundRect(panelX + panelW / 2 - 60, panelY + 178, 120, 30, 8, 8);
        g2d.setColor(rankColor);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRoundRect(panelX + panelW / 2 - 60, panelY + 178, 120, 30, 8, 8);

        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        g2d.setColor(Color.WHITE);
        fm = g2d.getFontMetrics();
        g2d.drawString(rank, (WIDTH - fm.stringWidth(rank)) / 2, panelY + 200);

        // Wings decoration
        g2d.setColor(rankColor);
        g2d.setStroke(new BasicStroke(2));
        int wingY = panelY + 193;
        g2d.drawLine(panelX + panelW / 2 - 65, wingY, panelX + panelW / 2 - 85, wingY - 5);
        g2d.drawLine(panelX + panelW / 2 - 65, wingY, panelX + panelW / 2 - 85, wingY + 5);
        g2d.drawLine(panelX + panelW / 2 + 65, wingY, panelX + panelW / 2 + 85, wingY - 5);
        g2d.drawLine(panelX + panelW / 2 + 65, wingY, panelX + panelW / 2 + 85, wingY + 5);

        // New best
        if (score == bestScore && score > 0) {
            g2d.setFont(new Font("Arial", Font.BOLD, 14));
            g2d.setColor(new Color(255, 100, 100));
            String newBest = "NOVO RECORDE!";
            fm = g2d.getFontMetrics();
            g2d.drawString(newBest, (WIDTH - fm.stringWidth(newBest)) / 2, panelY + 235);
        }

        // Restart
        g2d.setFont(new Font("Arial", Font.PLAIN, 16));
        g2d.setColor(new Color(200, 220, 255, 180));
        String restart = "ENTER para decolar novamente";
        fm = g2d.getFontMetrics();
        g2d.drawString(restart, (WIDTH - fm.stringWidth(restart)) / 2, panelY + panelH + 35);
    }

    private void drawKey(Graphics2D g2d, int x, int y, String key, String desc) {
        g2d.setColor(new Color(60, 80, 120));
        g2d.fillRoundRect(x, y, 32, 28, 6, 6);
        g2d.setColor(new Color(100, 140, 200));
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.drawRoundRect(x, y, 32, 28, 6, 6);

        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        g2d.setColor(Color.WHITE);
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(key, x + (32 - fm.stringWidth(key)) / 2, y + 20);

        g2d.setFont(new Font("Arial", Font.PLAIN, 15));
        g2d.setColor(new Color(200, 220, 255));
        g2d.drawString(desc, x + 45, y + 19);
    }

    private void drawMusicIndicator(Graphics2D g2d) {
        int ix = WIDTH - 55;
        int iy = HEIGHT - GROUND_HEIGHT - 25;

        g2d.setColor(new Color(0, 0, 0, 60));
        g2d.fillRoundRect(ix - 5, iy - 12, 55, 18, 6, 6);

        g2d.setFont(new Font("Arial", Font.PLAIN, 11));

        if (musicEnabled) {
            g2d.setColor(new Color(100, 255, 100, 180));
            g2d.drawString("M: ON", ix, iy);
            // Animated note
            double noteOffset = Math.sin(System.currentTimeMillis() * 0.005) * 3;
            g2d.setFont(new Font("Arial", Font.PLAIN, 13));
            g2d.drawString("\u266A", ix + 38, iy - 1 + (int) noteOffset);
        } else {
            g2d.setColor(new Color(255, 100, 100, 180));
            g2d.drawString("M: OFF", ix, iy);
        }
    }

    // ==================== INPUT ====================
    private void toggleMusic() {
        if (musicPlayer == null) return;
        if (musicEnabled) {
            musicPlayer.stop();
            musicEnabled = false;
        } else {
            musicPlayer = new MusicPlayer(MUSIC_PATH);
            musicPlayer.play();
            musicEnabled = true;
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();

        // M to toggle music works in any state
        if (key == KeyEvent.VK_M) {
            toggleMusic();
            return;
        }

        switch (state) {
            case MENU:
                if (key == KeyEvent.VK_ENTER || key == KeyEvent.VK_SPACE) {
                    state = GameState.CONTROL_SELECT;
                    selectedOption = 0;
                }
                break;

            case CONTROL_SELECT:
                if (key == KeyEvent.VK_W || key == KeyEvent.VK_UP) {
                    selectedOption = 0;
                }
                if (key == KeyEvent.VK_S || key == KeyEvent.VK_DOWN) {
                    selectedOption = 1;
                }
                if (key == KeyEvent.VK_ENTER || key == KeyEvent.VK_SPACE) {
                    controlMode = selectedOption == 0 ? ControlMode.KEYBOARD : ControlMode.MOUSE;
                    startGame();
                }
                if (key == KeyEvent.VK_ESCAPE) {
                    state = GameState.MENU;
                }
                break;

            case PLAYING:
                if (controlMode == ControlMode.KEYBOARD) {
                    if (key == KeyEvent.VK_W || key == KeyEvent.VK_UP) {
                        plane.setMovingUp(true);
                    }
                    if (key == KeyEvent.VK_S || key == KeyEvent.VK_DOWN) {
                        plane.setMovingDown(true);
                    }
                }
                break;

            case GAME_OVER:
                if (System.currentTimeMillis() - gameOverTime > 500) {
                    if (key == KeyEvent.VK_ENTER || key == KeyEvent.VK_SPACE) {
                        resetGame();
                    }
                }
                break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_W || key == KeyEvent.VK_UP) {
            plane.setMovingUp(false);
        }
        if (key == KeyEvent.VK_S || key == KeyEvent.VK_DOWN) {
            plane.setMovingDown(false);
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    // Mouse input
    @Override
    public void mouseMoved(MouseEvent e) {
        mouseY = e.getY();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        mouseY = e.getY();
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (state == GameState.CONTROL_SELECT) {
            // Click on options to select
            int panelW = 380;
            int panelH = 340;
            int panelX = (WIDTH - panelW) / 2;
            int panelY = (HEIGHT - panelH) / 2 - 20;

            int optY1 = panelY + 95;
            int optY2 = panelY + 210;
            int optH = 100;
            int mx = e.getX();
            int my = e.getY();

            if (my >= optY1 && my <= optY1 + optH && mx >= panelX + 25 && mx <= panelX + panelW - 25) {
                selectedOption = 0;
                controlMode = ControlMode.KEYBOARD;
                startGame();
            } else if (my >= optY2 && my <= optY2 + optH && mx >= panelX + 25 && mx <= panelX + panelW - 25) {
                selectedOption = 1;
                controlMode = ControlMode.MOUSE;
                startGame();
            }
        }
    }

    @Override public void mousePressed(MouseEvent e) {}
    @Override public void mouseReleased(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}
}
