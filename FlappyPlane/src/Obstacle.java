import java.awt.*;

public class Obstacle {
    private int x;
    private int gapY;
    private static final int WIDTH = 55;
    private static final int GAP_SIZE = 160;
    private boolean scored;
    private int screenHeight;
    private int groundHeight;

    public Obstacle(int x, int gapY, int screenHeight, int groundHeight) {
        this.x = x;
        this.gapY = gapY;
        this.screenHeight = screenHeight;
        this.groundHeight = groundHeight;
        this.scored = false;
    }

    public void update(int speed) {
        x -= speed;
    }

    public void draw(Graphics2D g2d) {
        int bottomY = gapY + GAP_SIZE;
        int groundTop = screenHeight - groundHeight;

        // Top obstacle (building/tower from top)
        drawBuilding(g2d, x, 0, WIDTH, gapY, true);

        // Bottom obstacle (building from ground)
        drawBuilding(g2d, x, bottomY, WIDTH, groundTop - bottomY, false);
    }

    private void drawBuilding(Graphics2D g2d, int bx, int by, int w, int h, boolean fromTop) {
        if (h <= 0) return;

        // Main structure
        g2d.setColor(new Color(100, 110, 130));
        g2d.fillRect(bx, by, w, h);

        // Darker side panel
        g2d.setColor(new Color(80, 90, 110));
        g2d.fillRect(bx + w - 10, by, 10, h);

        // Lighter panel
        g2d.setColor(new Color(120, 130, 150));
        g2d.fillRect(bx + 3, by, 8, h);

        // Windows
        g2d.setColor(new Color(180, 210, 240));
        int windowW = 8;
        int windowH = 10;
        int startY = fromTop ? by + (h % 18) + 5 : by + 8;
        int endY = fromTop ? by + h - 5 : by + h - 5;

        for (int wy = startY; wy + windowH < endY; wy += 18) {
            for (int wx = bx + 8; wx + windowW < bx + w - 10; wx += 14) {
                // Some windows lit, some dark
                if ((wx + wy) % 3 == 0) {
                    g2d.setColor(new Color(255, 230, 140, 200)); // Lit window
                } else {
                    g2d.setColor(new Color(140, 170, 200)); // Dark window
                }
                g2d.fillRect(wx, wy, windowW, windowH);
                g2d.setColor(new Color(70, 80, 100));
                g2d.drawRect(wx, wy, windowW, windowH);
            }
        }

        // Top/bottom cap (antenna platform or base)
        if (fromTop) {
            // Bottom cap of top building
            g2d.setColor(new Color(200, 60, 60));
            g2d.fillRect(bx - 3, by + h - 6, w + 6, 6);
            g2d.setColor(new Color(150, 40, 40));
            g2d.drawRect(bx - 3, by + h - 6, w + 6, 6);

            // Warning light
            g2d.setColor(new Color(255, 50, 50, 180 + (int)(75 * Math.sin(System.currentTimeMillis() * 0.005))));
            g2d.fillOval(bx + w / 2 - 4, by + h - 10, 8, 8);
        } else {
            // Top cap of bottom building
            g2d.setColor(new Color(200, 60, 60));
            g2d.fillRect(bx - 3, by, w + 6, 6);
            g2d.setColor(new Color(150, 40, 40));
            g2d.drawRect(bx - 3, by, w + 6, 6);

            // Warning light
            g2d.setColor(new Color(255, 50, 50, 180 + (int)(75 * Math.sin(System.currentTimeMillis() * 0.005))));
            g2d.fillOval(bx + w / 2 - 4, by + 2, 8, 8);
        }

        // Outline
        g2d.setColor(new Color(60, 65, 80));
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.drawRect(bx, by, w, h);
    }

    public Rectangle getTopBounds() {
        return new Rectangle(x - 1, 0, WIDTH + 2, gapY);
    }

    public Rectangle getBottomBounds() {
        int bottomY = gapY + GAP_SIZE;
        return new Rectangle(x - 1, bottomY, WIDTH + 2, screenHeight - bottomY);
    }

    public int getX() { return x; }
    public int getWidth() { return WIDTH; }
    public boolean isScored() { return scored; }
    public void setScored(boolean scored) { this.scored = scored; }
    public boolean isOffScreen() { return x + WIDTH < 0; }
}
