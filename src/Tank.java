import java.util.Scanner;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.transform.Affine;

class Tank implements Ball {

	double locX, locY, radius, angle;
	int self; // index of this tank in NetTankWar.tanks
	public boolean turnL, turnR, forth, back, fire;
	boolean prevtL, prevtR, prevfo, prevFire;
	Color color;
	Image image;

	public static final double turnRate = 180.0 / 8;
	public static final double speed = 4.0;
	public static final int RELOAD = 8; // delay between bullets
	int count; // timer for reloading

	public static final int MAXBULLETS = 7; // max simultaneous shots
	Bullet bullets[] = new Bullet[MAXBULLETS];

	public Tank(double x, double y, double a, int index, Image im) {
		locX = x;
		locY = y;
		angle = a;
		self = index;
		image = im;
		radius = 22;
		// create bullets for this tank
		for (int i = 0; i < bullets.length; i++)
			bullets[i] = new Bullet(self);
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

	public boolean isAlive() {
		return true;
	}

	void update(Boolean local) {
		if (turnL)
			turnLeft(turnRate);
		if (turnR)
			turnRight(turnRate);
		if (forth) {
			moveForward();
			// Check for rocks
			if (NetTankWar.hitAnItem(this, NetTankWar.rocks) >= 0)
				backUp();
		}
		if (fire) {
			fireBullet();
		}
		if (local) {
			if (turnL != prevtL) {
				NetTankWar.send("turnL " + turnL + " " + locX + " " + locY + " " + angle);
				prevtL = turnL;
			}
			if (turnR != prevtR) {
				NetTankWar.send("turnR " + turnR + " " + locX + " " + locY + " " + angle);
				prevtR = turnR;
			}
			if (forth != prevfo) {
				NetTankWar.send("forth " + forth + " " + locX + " " + locY + " " + angle);
				prevfo = forth;
			}
			if (fire != prevFire) {
				NetTankWar.send("fire " + fire + " " + locX + " " + locY + " " + angle);
				prevFire = fire;
			}
		}

		if (count > 0)
			count--;
		// Update all of our bullets
		for (Bullet b : bullets)
			b.update();
	}

	public void processMove(String s) {
		// Update movement parameters based on s
		Scanner sc = new Scanner(s);
		// Get the flag change
		String command = sc.next();
		boolean value = sc.nextBoolean();
		if (command.equals("turnL"))
			turnL = value;
		else if (command.equals("turnR"))
			turnR = value;
		else if (command.equals("forth"))
			forth = value;
		else if (command.equals("fire"))
			fire = value;
		else
			System.out.println("Unexpected move: " + command);
		// then unpack position update
		locX = sc.nextDouble();
		locY = sc.nextDouble();
		angle = sc.nextDouble();
	}

	void render(GraphicsContext gc) {
		// Use the affine transform
		// to easily rotate the tank's image.
		gc.save();
		Affine trans = new Affine();
		trans.appendTranslation(locX, locY);
		trans.appendRotation(angle);
		// draw the image using the specified transform
		gc.setTransform(trans);
		gc.drawImage(image, -radius, -radius);
		// Reset the transform (this is important)
		gc.restore();
		// Then draw bullets
		for (Bullet b : bullets)
			b.render(gc);

	}

	void fireBullet() {
		// If it has been long enough since the last shot...

		if (count > 0)
			return;
		// ...and if all the bullets aren't currently in use...
		int slot = getAvailableBullet();
		if (slot < 0)
			return;
		// ...then launch a new bullet
		bullets[slot].setLocation(locX, locY);
		bullets[slot].setDirection(angle);
		bullets[slot].reset();
		// Reset the timer
		count = RELOAD;
	}

	int getAvailableBullet() {
		for (int i = 0; i < bullets.length; i++)
			if (!bullets[i].isAlive())
				return i;
		return -1;
	}

	void turnRight(double a) {
		angle += a;
		if (angle > 360)
			angle -= 360;
	}

	void turnLeft(double a) {
		angle -= a;
		if (angle < 0.0)
			angle += 360;
	}

	void moveForward() {
		locX += speed * Math.cos(Math.PI * angle / 180);
		locY += speed * Math.sin(Math.PI * angle / 180);
	}

	void backUp() {
		locX -= speed * Math.cos(Math.PI * angle / 180);
		locY -= speed * Math.sin(Math.PI * angle / 180);
	}


	public void registerHit(boolean alive) {
		// TODO Auto-generated method stub
		System.out.println("Player " + self + " registered a hit");
	}

}
