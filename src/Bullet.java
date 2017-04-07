import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

class Bullet implements Ball {
	double locX, locY, dx, dy;
	int tank; // index of tank that fired this bullet
	int index;//index of bullet in tank bullet array
	boolean alive = false;
	int ttl; // time to live
	public static final int LIFETIME = 70;
	public static final double SPEED = 8.0;
	public static final int radius = 7;

	public Bullet(int t, int i) {
		tank = t;
		index = i;
	}

	void update() {
		int i;
		// Check if this bullet is worn out
		ttl--;
		if (ttl < 0)
			alive = false;
		if (!alive)
			return;
		// If not worn out, update position
		locX += SPEED * dx;
		locY += SPEED * dy;
		// check for collisions with rocks
		i = NetTankWar.hitAnItem(this, NetTankWar.rocks);
		if (i >= 0) {
			alive = false;
			NetTankWar.send("bullet " + index + " " + alive + " " + locX + " " + locY + " rock: " + i);
			// Ask the game to deactivate this rock
			NetTankWar.removeRock(i);
		}
		// check for collisions with tanks (other than
		// our tank)
		i = NetTankWar.hitAnItem(this, NetTankWar.tanks);
		if ((i >= 0) && (i != tank)) {
			alive = false;
			// Tell game a tank was hit
			NetTankWar.send("bullet " + index + " " + alive + " " + locX + " " + locY + " tank: " + i);
			NetTankWar.tankHit(i);
		}
	}

	public double getX() {
		return locX;
	}

	public double getY() {
		return locY;
	}

	public double getRadius() {
		return radius;
	}

	void setLocation(double x, double y) {
		locX = x;
		locY = y;
	}

	void setDirection(double angle) {
		dx = Math.cos(Math.PI * angle / 180);
		dy = Math.sin(Math.PI * angle / 180);
	}

	void render(GraphicsContext gc) {
		if (alive) {
			gc.setFill(Color.BLACK);
			gc.fillOval(locX - radius, locY - radius, 2 * radius, 2 * radius);
		}
	}

	public boolean isAlive() {
		return alive;
	}

	void reset() {
		ttl = LIFETIME;
		alive = true;
	}
}