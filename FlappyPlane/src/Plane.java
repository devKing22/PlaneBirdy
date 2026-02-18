import java.awt.*;
import java.awt.geom.*;

public class Plane {
    private double x, y;
    private double velocity;
    private boolean movingUp, movingDown;
    private static final double MOVE_SPEED = 4.5;
    private static final double FRICTION = 0.85;
    private static final int WIDTH = 50;
    private static final int HEIGHT = 25;
    private double rotation;
    private double propellerAngle;
    private boolean engineOn;
    private boolean mouseMode;
    private int targetY;

    public Plane(int startX, int startY) {
        this.x = startX;
        this.y = startY;
        this.velocity = 0;
        this.rotation = 0;
        this.propellerAngle = 0;
        this.engineOn = false;
        this.mouseMode = false;
        this.targetY = startY;
    }

    public void update() {
        if (mouseMode) {
            double diff = targetY - y;
            velocity = diff * 0.12;
            velocity = Math.max(-MOVE_SPEED, Math.min(MOVE_SPEED, velocity));
            y += velocity;
            engineOn = Math.abs(diff) > 3;
        } else {
            if (movingUp) {
                velocity -= MOVE_SPEED * 0.3;
            } else if (movingDown) {
                velocity += MOVE_SPEED * 0.3;
            } else {
                velocity *= FRICTION;
            }
            velocity = Math.max(-MOVE_SPEED, Math.min(MOVE_SPEED, velocity));
            y += velocity;
            engineOn = movingUp || movingDown;
        }

        // Smooth rotation based on velocity
        double targetRotation = velocity * 4;
        rotation += (targetRotation - rotation) * 0.15;
        rotation = Math.max(-25, Math.min(25, rotation));

        // Propeller animation
        propellerAngle += engineOn ? 30 : 15;
    }

    public void setTargetY(int ty) { this.targetY = ty; }
    public void setMouseMode(boolean b) { this.mouseMode = b; }
    public boolean isMouseMode() { return mouseMode; }

    public void draw(Graphics2D g2d) {
        AffineTransform old = g2d.getTransform();
        g2d.translate(x + WIDTH / 2.0, y + HEIGHT / 2.0);
        g2d.rotate(Math.toRadians(rotation));

        // Exhaust particles when moving
        if (engineOn) {
            g2d.setColor(new Color(200, 200, 200, 80));
            for (int i = 0; i < 3; i++) {
                int ex = -WIDTH / 2 - 8 - i * 7;
                int ey = (int)(Math.random() * 8 - 4);
                g2d.fillOval(ex, ey - 3, 8 + i * 2, 6 + i);
            }
        }

        // Tail fin (vertical)
        g2d.setColor(new Color(200, 50, 50));
        int[] tailFinX = {-WIDTH / 2, -WIDTH / 2 - 8, -WIDTH / 2};
        int[] tailFinY = {-2, -14, -HEIGHT / 2 + 2};
        g2d.fillPolygon(tailFinX, tailFinY, 3);
        g2d.setColor(new Color(160, 30, 30));
        g2d.drawPolygon(tailFinX, tailFinY, 3);

        // Tail fin (horizontal)
        g2d.setColor(new Color(180, 45, 45));
        int[] tailHX = {-WIDTH / 2, -WIDTH / 2 - 10, -WIDTH / 2};
        int[] tailHY = {-3, 0, 4};
        g2d.fillPolygon(tailHX, tailHY, 3);

        // Fuselage (body)
        g2d.setColor(new Color(240, 240, 240));
        g2d.fillRoundRect(-WIDTH / 2, -HEIGHT / 2 + 2, WIDTH - 5, HEIGHT - 4, 10, 10);

        // Body stripe
        g2d.setColor(new Color(30, 100, 200));
        g2d.fillRect(-WIDTH / 2 + 5, -1, WIDTH - 15, 4);

        // Red stripe
        g2d.setColor(new Color(200, 40, 40));
        g2d.fillRect(-WIDTH / 2 + 5, 3, WIDTH - 15, 2);

        // Cockpit window
        g2d.setColor(new Color(100, 180, 255));
        g2d.fillRoundRect(WIDTH / 2 - 16, -HEIGHT / 2 + 3, 12, HEIGHT - 6, 6, 6);
        g2d.setColor(new Color(70, 150, 230));
        g2d.drawRoundRect(WIDTH / 2 - 16, -HEIGHT / 2 + 3, 12, HEIGHT - 6, 6, 6);
        // Window shine
        g2d.setColor(new Color(200, 230, 255, 150));
        g2d.fillRoundRect(WIDTH / 2 - 14, -HEIGHT / 2 + 5, 4, 6, 3, 3);

        // Side windows
        g2d.setColor(new Color(130, 200, 255));
        for (int i = 0; i < 3; i++) {
            g2d.fillRoundRect(-WIDTH / 2 + 10 + i * 10, -HEIGHT / 2 + 4, 6, 5, 3, 3);
        }

        // Wings
        g2d.setColor(new Color(220, 220, 220));
        // Top wing
        int[] wingTopX = {-5, 10, 5, -10};
        int[] wingTopY = {-HEIGHT / 2 + 1, -HEIGHT / 2 - 6, -HEIGHT / 2 - 6, -HEIGHT / 2 + 1};
        g2d.fillPolygon(wingTopX, wingTopY, 4);
        g2d.setColor(new Color(180, 180, 180));
        g2d.drawPolygon(wingTopX, wingTopY, 4);

        // Bottom wing
        g2d.setColor(new Color(220, 220, 220));
        int[] wingBotX = {-5, 10, 5, -10};
        int[] wingBotY = {HEIGHT / 2 - 1, HEIGHT / 2 + 6, HEIGHT / 2 + 6, HEIGHT / 2 - 1};
        g2d.fillPolygon(wingBotX, wingBotY, 4);
        g2d.setColor(new Color(180, 180, 180));
        g2d.drawPolygon(wingBotX, wingBotY, 4);

        // Nose cone
        g2d.setColor(new Color(200, 200, 200));
        int[] noseX = {WIDTH / 2 - 5, WIDTH / 2 + 5, WIDTH / 2 - 5};
        int[] noseY = {-HEIGHT / 2 + 3, 0, HEIGHT / 2 - 3};
        g2d.fillPolygon(noseX, noseY, 3);
        g2d.setColor(new Color(160, 160, 160));
        g2d.drawPolygon(noseX, noseY, 3);

        // Propeller
        g2d.setColor(new Color(60, 60, 60));
        AffineTransform propT = g2d.getTransform();
        g2d.translate(WIDTH / 2 + 4, 0);
        g2d.rotate(Math.toRadians(propellerAngle));
        g2d.fillRoundRect(-2, -12, 4, 24, 2, 2);
        g2d.rotate(Math.toRadians(90));
        g2d.fillRoundRect(-2, -12, 4, 24, 2, 2);
        g2d.setTransform(propT);

        // Propeller hub
        g2d.setColor(new Color(80, 80, 80));
        g2d.fillOval(WIDTH / 2 + 1, -3, 6, 6);

        // Body outline
        g2d.setColor(new Color(180, 180, 180));
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.drawRoundRect(-WIDTH / 2, -HEIGHT / 2 + 2, WIDTH - 5, HEIGHT - 4, 10, 10);

        g2d.setTransform(old);
    }

    public Rectangle getBounds() {
        return new Rectangle((int) x + 6, (int) y + 4, WIDTH - 12, HEIGHT - 8);
    }

    public void setMovingUp(boolean b) { movingUp = b; }
    public void setMovingDown(boolean b) { movingDown = b; }
    public double getY() { return y; }
    public void setY(double y) { this.y = y; }
    public double getX() { return x; }
    public int getPlaneWidth() { return WIDTH; }
    public int getPlaneHeight() { return HEIGHT; }
    public void resetVelocity() { velocity = 0; }
}
