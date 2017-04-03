import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.transform.Affine;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.*;
import java.net.*;
import java.io.*;

/*
 * Implement a simple 2-player
 * tank battle game.
 *
 * written by mike slattery - mar 2007
 * Ported to JavaFX - mcs, mar 2017
 */

public class NetTankWar extends Application {
	final String appName = "NetTankWar";
	final int FPS = 30; // frames per second
	final static int WIDTH = 800;
	final static int HEIGHT = 600;

	GraphicsContext gc; // declare here to use in handlers

	Thread anim = null; // animation thread

	public static ArrayList<Rock> rocks; // obstacles on the field
	public static ArrayList<Tank> tanks;
	public static ArrayList<Bullet> bullets;
	public static final int RED = 0;
	public static final int BLUE = 1;

	int playerID = -1; // my subscript in tank array

	Image redtank;
	Image bluetank;

	static boolean roundOver = true;
	static int loser;

	static boolean ready = false;

	Font font = Font.font("Monospaced", FontPosture.REGULAR, 30.0);

	private Socket sock;
	private static PrintWriter out;

	private static final int PORT = 1234; // server details
	private static final String HOST = "localhost";
	// private static final String HOST = "pascal.mscs.mu.edu";

	public void initialize() {
		redtank = new Image("redtank.png");
		bluetank = new Image("bluetank.png");

		makeContact();
	}

	private void makeContact()
	// contact the NetWarServer
	{
		try {
			sock = new Socket(HOST, PORT);
			BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			out = new PrintWriter(sock.getOutputStream(), true); // autoflush

			NetWarWatcher nww = new NetWarWatcher(this, in);
			nww.setDaemon(true); // tell Java this is a supplemental thread
			nww.start(); // start watching for server msgs
		} catch (Exception e) {
			System.out.println("Cannot contact the NetTankWar Server");
			System.exit(1);
		}
	} // end of makeContact()

	public static void send(String msg) {
		// send a message to the other player (via server)
		out.println(msg);
	}

	public void update() {
		if (!roundOver) { // Freeze the action between rounds
			tanks.get(playerID).update(true);
			tanks.get(1 - playerID).update(false);
			
			//TODO update all bullets here
			for(int i = 0; i < tanks.get(playerID-1).MAXBULLETS; i++){
				
			}
		}
	}

	synchronized void resetRound() {
		// Build semi-random rock field
		rocks = new ArrayList<Rock>();
		int edge = (WIDTH + HEIGHT) / 20;
		int halfW = WIDTH / 2;
		int halfH = HEIGHT / 2;
		placeRocks(40, edge, halfH, halfW, edge, 0.2);
		placeRocks(40, halfW, HEIGHT - edge, WIDTH - edge, halfH, 0.2);
		placeRocks(10, halfW, 0, halfW, HEIGHT, 0.1);

		// Place tanks
		tanks = new ArrayList<Tank>();
		tanks.add(new Tank(WIDTH - edge, HEIGHT - edge, Math.PI, RED, redtank));
		tanks.add(new Tank(edge, edge, 0.0, BLUE, bluetank));

		roundOver = false;
		ready = true;
	}

	public void setPlayerID(int id) {
		playerID = id;
	}

	public int getPlayerID() {
		return playerID;
	}

	public void setRocks(String config) {
		// Read rock string from other player
		stringToRocks(config);
		// Place tanks
		int edge = (int) (WIDTH + HEIGHT) / 20;
		tanks = new ArrayList<Tank>();
		tanks.add(new Tank(WIDTH - edge, HEIGHT - edge, Math.PI, RED, redtank));
		tanks.add(new Tank(edge, edge, 0.0, BLUE, bluetank));

		roundOver = false;
		ready = true;
	}

	public void sendRocks() {
		// Lay out rocks and send to other player
		resetRound();
		out.println("rocks " + rocksToString());
	}

	synchronized void placeRocks(int n, int x1, int y1, int x2, int y2, double aspect) {
		// place n rocks randomly located within distance r of
		// the line from (x1,y1) to (x2,y2) where r is the length of
		// this line times aspect.
		int x, y;
		double s, tx, ty;

		double len = Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
		double r = len * aspect;
		for (int i = 0; i < n; i++) {
			s = Rock.rockGen.nextDouble();
			tx = r * (Rock.rockGen.nextDouble() - 0.5);
			ty = r * (Rock.rockGen.nextDouble() - 0.5);
			x = (int) (x1 + s * (x2 - x1) + tx);
			y = (int) (y1 + s * (y2 - y1) + ty);
			rocks.add(new Rock(x, y, (int) (r / 2), (int) r));
		}

	}

	public static synchronized void removeRock(int index) {
		// Deactivate the index-th rock in rocks
		rocks.get(index).demolish();
	}

	public static synchronized String rocksToString() {
		// Return a string describing the rocks: x1 y1 d1;x2 y2 d2; ...
		StringBuilder sb = new StringBuilder();
		for (Rock r : rocks)
			sb.append(r.getIntX() + " " + r.getIntY() + " " + r.getDiameter() + ";");
		return new String(sb);
	}

	public static void stringToRocks(String s) {
		// read the rocks string and fill in the ArrayList
		int x, y, d;

		rocks = new ArrayList<Rock>();
		String specs[] = s.split(";");
		for (String sp : specs) {
			Scanner sc = new Scanner(sp);
			x = sc.nextInt();
			y = sc.nextInt();
			d = sc.nextInt();
			sc.close();
			rocks.add(new Rock(x, y, d));
		}
	}

	public void processMove(String s) {
		// Send command to tank not controlled by
		// this player
		tanks.get(1 - playerID).processMove(s);
	}

	public static int hitAnItem(Ball b, ArrayList<? extends Ball> c) {
		// Check if b has run into any element of c. If so
		// return the index of the first item that was hit,
		// or -1 if nothing hit.
		// Use generics so the same code can check for collisions
		// with rocks and tanks.
		for (Ball r : c) {
			if (!r.isAlive())
				continue;
			double dx = b.getX() - r.getX();
			double dy = b.getY() - r.getY();
			double bound = b.getRadius() + r.getRadius();
			if ((dx * dx + dy * dy) < (bound * bound))
				return c.indexOf(r);
		}
		return -1;
	}

	public static void tankHit(int k) {
		// If a tank is hit, the round is over
		if (!roundOver) {
			roundOver = true;
			loser = k;
		}
	}

	public void render(GraphicsContext gc) {

		// Draw a background
		gc.setFill(Color.YELLOW);
		gc.fillRect(0, 0, WIDTH, HEIGHT);

		if (!ready) { // Just display the message and return
			gc.setFill(Color.BLACK);
			gc.setFont(font);
			gc.fillText("Waiting for setup...", 200, 250);
			return;
		}

		// Draw rocks
		synchronized (this) {
			for (Rock r : rocks)
				r.render(gc);
		}

		// Draw tanks (and their bullets)
		for (Tank t : tanks)
			t.render(gc);

		if (roundOver) {
			gc.setFill(Color.BLACK);
			gc.setFont(font);
			gc.fillText("Round Over: " + (loser == RED ? "Blue" : "Red") + " tank wins!", 150, 200);
		}
	}

	void setHandlers(Scene scene) {
		scene.setOnKeyPressed(e -> {
			KeyCode c = e.getCode();
			switch (c) {
			case LEFT:
				tanks.get(playerID).turnL = true;
				break;
			case RIGHT:
				tanks.get(playerID).turnR = true;
				break;
			case UP:
				tanks.get(playerID).forth = true;
				break;
			case SPACE:
				tanks.get(playerID).fire = true;
				break;
			default:
				break;
			}
		});

		scene.setOnKeyReleased(e -> {
			KeyCode c = e.getCode();
			switch (c) {
			case LEFT:
				tanks.get(playerID).turnL = false;
				break;
			case RIGHT:
				tanks.get(playerID).turnR = false;
				break;
			case UP:
				tanks.get(playerID).forth = false;
				break;
			case SPACE:
				tanks.get(playerID).fire = false;
				break;
			default:
				break;
			}
		});
	}

	/*
	 * Begin boiler-plate code... [Animation and events with initialization]
	 */
	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage theStage) {
		theStage.setTitle(appName);

		Group root = new Group();
		Scene theScene = new Scene(root);
		theStage.setScene(theScene);

		Canvas canvas = new Canvas(WIDTH, HEIGHT);
		root.getChildren().add(canvas);

		GraphicsContext gc = canvas.getGraphicsContext2D();

		// Initial setup
		initialize();
		setHandlers(theScene);

		// Setup and start animation loop (Timeline)
		KeyFrame kf = new KeyFrame(Duration.millis(1000 / FPS), e -> {
			// update position
			update();
			// draw frame
			render(gc);
		});
		Timeline mainLoop = new Timeline(kf);
		mainLoop.setCycleCount(Animation.INDEFINITE);
		mainLoop.play();

		theStage.show();
	}
	/*
	 * ... End boiler-plate code
	 */

	public void updateBulletLocations(String line) {
		//TODO : make bullets communicate constantly between tanks
		System.out.println(line);

	}

}
