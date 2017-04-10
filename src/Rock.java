import java.util.Random;
import java.util.Scanner;
import javafx.scene.image.Image;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

class Rock implements Ball {
	// Obstacles scattered about the play field.
	// Create a random generator to use for rock sizes and
	// placement
	public static Random rockGen = new Random();

	int locX, locY, diameter, radius; // int properties easier to
	boolean alive = true; // send over network
	Image image;

	public Rock(int x, int y, int minD, int maxD, Image im) {
		locX = x;
		locY = y;
		diameter = minD + rockGen.nextInt(maxD - minD + 1);
		radius = diameter / 2;
		image = im; 
	}

	public Rock(int x, int y, int d, Image im) {
		locX = x;
		locY = y;
		diameter = d;
		radius = diameter / 2;
		image = im;
	}

	void demolish() {
		// "Turn off" this rock - won't paint or be hit
		alive = false;
	}

	public boolean isAlive() {
		return alive;
	}

	public double getX() {
		return (double) locX;
	}

	public double getY() {
		return (double) locY;
	}

	public double getRadius() {
		return (double) radius;
	}

	public int getIntX() {
		return locX;
	}

	public int getIntY() {
		return locY;
	}

	public int getDiameter() {
		return diameter;
	}

	void render(GraphicsContext gc) {
		if (alive) {
			gc.drawImage(image,locX - radius, locY - radius, diameter, diameter);
		}
	}

	public void setAlive(boolean b) {
		alive = b;
	}
}
