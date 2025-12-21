import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Action Game Client (Optimized Network)
 * - Optimization: 'maxHp' and 'size' are now sent via 'STATUS' command only when changed,
 * instead of every frame in 'MOVE' command.
 */
public class ActionClient extends JFrame {

	// ==========================================
	//  Settings & Constants
	// ==========================================

	private static final String SERVER_IP = "127.0.0.1";
	private static final int SERVER_PORT = 10000;
	private static final int TARGET_GAME_ID = 1;

	private static final String IMAGE_PATH_PLAYER_ME = "player_me.png";
	private static final String IMAGE_PATH_PLAYER_ENEMY = "player_enemy.png";

	private static final int MAP_X = 50;
	private static final int MAP_Y = 80;
	private static final int MAP_WIDTH = 700;
	private static final int MAP_HEIGHT = 450;

	private static final int FPS = 60;
	private static final int MAX_WINS = 3;

	private static final int PLAYER_MAX_HP = 100;
	private static final double PLAYER_SPEED = 3.0;
	private static final int PLAYER_SIZE = 15;

	private static final int MAX_BULLETS = 500;

	private static final int FLAG_NONE = 0;
	private static final int FLAG_HILL = 1 << 0;
	private static final int FLAG_BOUNCE = 1 << 1;
	private static final int FLAG_POISON = 1 << 2;

	private static final int OBSTACLE_COUNT = 8;
	private static final int MIN_WALL_LENGTH = 50;
	private static final int MAX_WALL_LENGTH = 150;

	// ==========================================
	//  System Variables
	// ==========================================
	private Socket socket;
	private PrintWriter out;
	private BufferedReader in;
	private int myId;

	private GamePanel panel;
	private javax.swing.Timer gameTimer;

	enum GameState { TITLE, WAITING, PLAYING, ROUND_END_SELECT, ROUND_END_WAIT, COUNTDOWN, GAME_OVER }
	private GameState currentState = GameState.TITLE;

	private String resultMessage = "";
	private Set<Integer> joinedPlayers = Collections.synchronizedSet(new HashSet<>());

	private static final int MAP_TYPE_RANDOM = 0;
	private static final int MAP_TYPE_A = 1;
	private static final int MAP_TYPE_B = 2;
	private int selectedMapType = MAP_TYPE_RANDOM;

	private BufferedImage imgPlayerMe;
	private BufferedImage imgPlayerEnemy;

	private ConcurrentHashMap<Integer, Player> players = new ConcurrentHashMap<>();
	private Bullet[] bulletPool = new Bullet[MAX_BULLETS];
	private ArrayList<Line2D.Double> obstacles = new ArrayList<>();

	private int myWinCount = 0;
	private int enemyWinCount = 0;
	private ArrayList<PowerUp> presentedPowerUps = new ArrayList<>();
	private Rectangle[] cardRects = new Rectangle[3];
	private boolean isRoundWinner = false;
	private int countdownTimer = 0;

	public ActionClient() {
		for (int i = 0; i < MAX_BULLETS; i++) bulletPool[i] = new Bullet();

		loadImages();
		setupConnection(SERVER_IP, SERVER_PORT);

		setTitle("Action Game Client - Best of 3");
		setSize(800, 700);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setResizable(false);

		panel = new GamePanel();
		add(panel);

		gameTimer = new javax.swing.Timer(1000 / FPS, e -> gameLoop());
		gameTimer.start();

		setVisible(true);
	}

	private void loadImages() {
		try {
			File f1 = new File(IMAGE_PATH_PLAYER_ME);
			if (f1.exists()) imgPlayerMe = ImageIO.read(f1);
			File f2 = new File(IMAGE_PATH_PLAYER_ENEMY);
			if (f2.exists()) imgPlayerEnemy = ImageIO.read(f2);
		} catch (Exception e) {}
	}

	private void setupConnection(String host, int port) {
		try {
			socket = new Socket(host, port);
			out = new PrintWriter(socket.getOutputStream(), true);
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			new Thread(this::receiveLoop).start();
		} catch (IOException e) {
			JOptionPane.showMessageDialog(this, "Connection failed: " + host + ":" + port);
			System.exit(0);
		}
	}

	private void gameLoop() {
		if (!players.containsKey(myId)) return;
		Player me = players.get(myId);

		switch (currentState) {
			case TITLE: case WAITING: case ROUND_END_WAIT: case ROUND_END_SELECT: case GAME_OVER: break;
			case COUNTDOWN:
				countdownTimer--;
				if (countdownTimer <= 0) currentState = GameState.PLAYING;
				break;
			case PLAYING:
				updateGame(me);
				break;
		}
		panel.repaint();
	}

	private void updateGame(Player me) {
		me.update(panel.mouseX, panel.mouseY, panel, obstacles);
		me.weapon.update();

		for (Bullet b : bulletPool) {
			if (!b.isActive) continue;
			b.update();
			checkBulletCollision(b, me);
		}
	}

	private void checkBulletCollision(Bullet b, Player me) {
		boolean hitBoundary = false;
		if (b.x < MAP_X || b.x > MAP_X + MAP_WIDTH) {
			if (canBounce(b)) { b.angle = Math.PI - b.angle; b.bounceCount++; }
			else hitBoundary = true;
		}
		if (b.y < MAP_Y || b.y > MAP_Y + MAP_HEIGHT) {
			if (canBounce(b)) { b.angle = -b.angle; b.bounceCount++; }
			else hitBoundary = true;
		}
		if (!hitBoundary) {
			for (Line2D.Double wall : obstacles) {
				if (wall.ptSegDist(b.x, b.y) < b.size) {
					if (canBounce(b)) {
						if (Math.abs(wall.y1 - wall.y2) < 1.0) b.angle = -b.angle; else b.angle = Math.PI - b.angle;
						b.bounceCount++;
					} else hitBoundary = true;
					break;
				}
			}
		}
		if (hitBoundary) b.deactivate();

		if (b.isActive && (b.ownerId != myId || b.lifeTimer > 10)) {
			if (me.getBounds().contains(b.x, b.y)) {
				me.hp -= b.damage;
				out.println("BULLET_HIT " + b.id);
				b.deactivate();

				if ((b.typeFlag & FLAG_POISON) != 0) me.poisonTimer = 180;
				if ((b.typeFlag & FLAG_HILL) != 0) out.println("HEAL " + b.ownerId + " 5");

				if (me.hp <= 0) {
					me.hp = 0;
					out.println("DEAD " + myId);
				}
			}
		}
	}

	private boolean canBounce(Bullet b) {
		return (b.typeFlag & FLAG_BOUNCE) != 0 && b.bounceCount < 1;
	}

	private void spawnBullet(int id, double x, double y, double angle, double speed, int dmg, int size, int flags, int ownerId) {
		for (Bullet b : bulletPool) {
			if (!b.isActive) {
				b.activate(id, x, y, angle, speed, dmg, size, flags, ownerId);
				break;
			}
		}
	}

	private void startNewMatch() {
		myWinCount = 0;
		enemyWinCount = 0;

		int minId = Integer.MAX_VALUE;
		for(int id : players.keySet()) minId = Math.min(minId, id);

		if (myId == minId) {
			obstacles.clear();
			generateObstacles();
			sendObstacleData();
		}
		startCountdown();
	}

	private void generateObstacles() {
		obstacles.clear();
		if (selectedMapType == MAP_TYPE_A) {
			int cx = MAP_X + MAP_WIDTH / 2;
			int cy = MAP_Y + MAP_HEIGHT / 2;
			int len = 150;
			obstacles.add(new Line2D.Double(cx - len, cy, cx - 30, cy));
			obstacles.add(new Line2D.Double(cx + 30, cy, cx + len, cy));
			obstacles.add(new Line2D.Double(cx, cy - len, cx, cy - 30));
			obstacles.add(new Line2D.Double(cx, cy + 30, cx, cy + len));
			obstacles.add(new Line2D.Double(MAP_X + 100, MAP_Y + 100, MAP_X + 200, MAP_Y + 100));
			obstacles.add(new Line2D.Double(MAP_X + MAP_WIDTH - 200, MAP_Y + MAP_HEIGHT - 100, MAP_X + MAP_WIDTH - 100, MAP_Y + MAP_HEIGHT - 100));
		} else if (selectedMapType == MAP_TYPE_B) {
			for (int i = 1; i <= 3; i++) {
				int y = MAP_Y + (MAP_HEIGHT / 4) * i;
				int gap = 100;
				obstacles.add(new Line2D.Double(MAP_X + 50, y, MAP_X + MAP_WIDTH/2 - gap/2, y));
				obstacles.add(new Line2D.Double(MAP_X + MAP_WIDTH/2 + gap/2, y, MAP_X + MAP_WIDTH - 50, y));
			}
			obstacles.add(new Line2D.Double(MAP_X + MAP_WIDTH/2, MAP_Y + 50, MAP_X + MAP_WIDTH/2, MAP_Y + 150));
			obstacles.add(new Line2D.Double(MAP_X + MAP_WIDTH/2, MAP_Y + MAP_HEIGHT - 150, MAP_X + MAP_WIDTH/2, MAP_Y + MAP_HEIGHT - 50));
		} else {
			for (int i = 0; i < OBSTACLE_COUNT; i++) {
				int margin = 100;
				int x1 = MAP_X + margin + (int)(Math.random() * (MAP_WIDTH - margin * 2));
				int y1 = MAP_Y + margin + (int)(Math.random() * (MAP_HEIGHT - margin * 2));
				int length = MIN_WALL_LENGTH + (int)(Math.random() * (MAX_WALL_LENGTH - MIN_WALL_LENGTH));
				boolean isHorizontal = Math.random() < 0.5;
				int x2 = x1; int y2 = y1;
				if (isHorizontal) x2 += length; else y2 += length;
				if (x2 > MAP_X + MAP_WIDTH) x2 = MAP_X + MAP_WIDTH - 10;
				if (y2 > MAP_Y + MAP_HEIGHT) y2 = MAP_Y + MAP_HEIGHT - 10;
				obstacles.add(new Line2D.Double(x1, y1, x2, y2));
			}
		}
	}

	private void sendObstacleData() {
		StringBuilder sb = new StringBuilder("MAP_DATA");
		for (Line2D.Double line : obstacles) {
			sb.append(" ").append((int)line.x1).append(" ").append((int)line.y1)
					.append(" ").append((int)line.x2).append(" ").append((int)line.y2);
		}
		out.println(sb.toString());
	}

	private void setGameOver(String msg) {
		currentState = GameState.GAME_OVER;
		resultMessage = msg;
	}

	private void handleRoundEnd(int deadPlayerId) {
		if (currentState != GameState.PLAYING) return;
		boolean iAmDead = (deadPlayerId == myId);
		isRoundWinner = !iAmDead;

		if (players.containsKey(deadPlayerId)) players.get(deadPlayerId).hp = 0;

		if (isRoundWinner) {
			myWinCount++;
			resultMessage = "YOU WIN THE ROUND!";
		} else {
			enemyWinCount++;
			resultMessage = "YOU LOST THE ROUND...";
		}

		if (myWinCount >= MAX_WINS) setGameOver("VICTORY! YOU WON THE MATCH!");
		else if (enemyWinCount >= MAX_WINS) setGameOver("DEFEAT... YOU LOST THE MATCH.");
		else prepareNextRound();
	}

	private void prepareNextRound() {
		for(Bullet b : bulletPool) b.deactivate();
		if (isRoundWinner) currentState = GameState.ROUND_END_WAIT;
		else {
			currentState = GameState.ROUND_END_SELECT;
			presentedPowerUps = PowerUpFactory.getRandomPowerUps(3);
		}
	}

	private void onPowerUpSelected() {
		// Optimization: Send STATUS (size, maxHp) only when stats change
		if (players.containsKey(myId)) players.get(myId).sendStatus();

		out.println("NEXT_ROUND_READY " + myId);
		currentState = GameState.ROUND_END_WAIT;
	}

	private void startCountdown() {
		resetPositions();
		currentState = GameState.COUNTDOWN;
		countdownTimer = 90;

		// Sync status at round start
		if (players.containsKey(myId)) players.get(myId).sendStatus();
	}

	private void resetPositions() {
		int minId = Integer.MAX_VALUE;
		for(int id : players.keySet()) minId = Math.min(minId, id);
		Player me = players.get(myId);
		if (me != null) {
			if (myId == minId) { me.x = MAP_X + 50; me.y = MAP_Y + 50; }
			else { me.x = MAP_X + MAP_WIDTH - 50; me.y = MAP_Y + MAP_HEIGHT - 50; }
			me.resetForRound();
		}
	}

	private void backToTitle() {
		currentState = GameState.TITLE;
		joinedPlayers.clear();
		players.clear();
		myWinCount = 0;
		enemyWinCount = 0;
		resultMessage = "";
		players.put(myId, new Player(myId, MAP_X + 100, MAP_Y + 200, Color.BLUE));
	}

	private void handleDisconnection() {
		SwingUtilities.invokeLater(() -> {
			JOptionPane.showMessageDialog(this, "Disconnected from server.");
			System.exit(0);
		});
	}

	private void receiveLoop() {
		try {
			String line;
			while ((line = in.readLine()) != null) {
				String[] tokens = line.split(" ");
				final String[] finalTokens = tokens;
				String cmd = tokens[0];
				SwingUtilities.invokeLater(() -> { processCommand(cmd, finalTokens); });
			}
		} catch (Exception e) {
		} finally { handleDisconnection(); }
	}

	private void processCommand(String cmd, String[] tokens) {
		try {
			if (cmd.equals("START")) {
				myId = Integer.parseInt(tokens[1]);
				players.put(myId, new Player(myId, MAP_X + 100, MAP_Y + 200, Color.BLUE));
			} else if (cmd.equals("JOIN")) {
				int gid = Integer.parseInt(tokens[1]); int pid = Integer.parseInt(tokens[2]);
				if (gid == TARGET_GAME_ID) {
					joinedPlayers.add(pid);
					if (!players.containsKey(pid)) players.put(pid, new Player(pid, 0, 0, Color.RED));
					if (joinedPlayers.size() >= 2 && currentState == GameState.WAITING) startNewMatch();
				}
			} else if (cmd.equals("MOVE")) {
				// Optimized Protocol: MOVE x y angle hp reloading timer id
				// (maxHp and size removed from here)
				int id = Integer.parseInt(tokens[7]); // ID at index 7
				if (id != myId) {
					Player p = players.computeIfAbsent(id, k -> new Player(id, 0, 0, Color.RED));
					p.x = Double.parseDouble(tokens[1]);
					p.y = Double.parseDouble(tokens[2]);
					p.angle = Double.parseDouble(tokens[3]);
					p.hp = Integer.parseInt(tokens[4]);
					p.weapon.isReloading = Boolean.parseBoolean(tokens[5]);
					p.weapon.reloadTimer = Integer.parseInt(tokens[6]);
				}
			} else if (cmd.equals("STATUS")) {
				// New Protocol: STATUS id maxHp size
				int id = Integer.parseInt(tokens[1]);
				int mHp = Integer.parseInt(tokens[2]);
				int sz = Integer.parseInt(tokens[3]);
				if (players.containsKey(id)) {
					Player p = players.get(id);
					p.maxHp = mHp;
					p.size = sz;
				} else {
					// Create if not exists (for joiners)
					Player p = new Player(id, 0, 0, (id == myId) ? Color.BLUE : Color.RED);
					p.maxHp = mHp;
					p.size = sz;
					players.put(id, p);
				}
			} else if (cmd.equals("SHOT")) {
				int bId = Integer.parseInt(tokens[1]);
				double x = Double.parseDouble(tokens[2]);
				double y = Double.parseDouble(tokens[3]);
				double angle = Double.parseDouble(tokens[4]);
				double speed = Double.parseDouble(tokens[5]);
				int damage = Integer.parseInt(tokens[6]);
				int size = Integer.parseInt(tokens[7]);
				int flags = Integer.parseInt(tokens[8]);
				int ownerId = Integer.parseInt(tokens[9]);
				spawnBullet(bId, x, y, angle, speed, damage, size, flags, ownerId);

			} else if (cmd.equals("BULLET_HIT")) {
				int targetBulletId = Integer.parseInt(tokens[1]);
				for (Bullet b : bulletPool) {
					if (b.isActive && b.id == targetBulletId) {
						b.deactivate(); break;
					}
				}
			} else if (cmd.equals("HEAL")) {
				int targetId = Integer.parseInt(tokens[1]);
				int amount = Integer.parseInt(tokens[2]);
				if (players.containsKey(targetId)) {
					Player p = players.get(targetId);
					p.hp = Math.min(p.hp + amount, p.maxHp);
				}
			} else if (cmd.equals("DEAD")) {
				int deadId = Integer.parseInt(tokens[1]);
				handleRoundEnd(deadId);
			} else if (cmd.equals("NEXT_ROUND_READY")) {
				if (currentState == GameState.ROUND_END_WAIT) startCountdown();
			} else if (cmd.equals("MAP_DATA")) {
				obstacles.clear();
				for (int i = 1; i < tokens.length - 1; i += 4) {
					try {
						double x1 = Double.parseDouble(tokens[i]); double y1 = Double.parseDouble(tokens[i+1]);
						double x2 = Double.parseDouble(tokens[i+2]); double y2 = Double.parseDouble(tokens[i+3]);
						obstacles.add(new Line2D.Double(x1, y1, x2, y2));
					} catch(Exception e) { break; }
				}
				resetPositions();
			}
		} catch (Exception e) { e.printStackTrace(); }
	}

	// ==========================================
	//  Inner Classes
	// ==========================================

	class Player {
		int id;
		double x, y, angle;
		int hp = PLAYER_MAX_HP; int maxHp = PLAYER_MAX_HP;
		Color color;
		double speed = PLAYER_SPEED; int size = PLAYER_SIZE;
		int poisonTimer = 0;
		Weapon weapon;

		public Player(int id, double x, double y, Color c) {
			this.id = id;
			this.x = x; this.y = y; this.color = c;
			this.weapon = new Weapon(this);
		}

		public void resetForRound() {
			hp = maxHp;
			poisonTimer = 0;
			weapon.reset();
		}

		// New Method: Send static stats
		public void sendStatus() {
			out.println("STATUS " + id + " " + maxHp + " " + size);
		}

		public void update(int mx, int my, GamePanel panel, ArrayList<Line2D.Double> obstacles) {
			double currentSpeed = speed;
			if (poisonTimer > 0) {
				poisonTimer--;
				currentSpeed *= 0.5;
			}

			double nextX = x, nextY = y;
			if (panel.keyA) nextX -= currentSpeed;
			if (panel.keyD) nextX += currentSpeed;
			if (!checkWall(nextX, y, obstacles)) x = nextX;

			if (panel.keyW) nextY -= currentSpeed;
			if (panel.keyS) nextY += currentSpeed;
			if (!checkWall(x, nextY, obstacles)) y = nextY;

			angle = Math.atan2(my - y, mx - x);

			// Optimization: Only send dynamic data
			out.println("MOVE " + (int)x + " " + (int)y + " " + angle + " " + hp
					+ " " + weapon.isReloading + " " + weapon.reloadTimer + " " + myId);
		}

		private boolean checkWall(double tx, double ty, ArrayList<Line2D.Double> walls) {
			if (tx < MAP_X + size || tx > MAP_X + MAP_WIDTH - size) return true;
			if (ty < MAP_Y + size || ty > MAP_Y + MAP_HEIGHT - size) return true;
			for (Line2D.Double w : walls) if (w.ptSegDist(tx, ty) < size) return true;
			return false;
		}

		public Rectangle getBounds() { return new Rectangle((int)x-size, (int)y-size, size*2, size*2); }

		public void draw(Graphics2D g2d) {
			AffineTransform old = g2d.getTransform(); g2d.translate(x, y);

			g2d.setColor(Color.RED); g2d.fillRect(-20, -35, 40, 5);
			g2d.setColor(Color.GREEN);
			int barWidth = (int)(40 * (hp / (double)maxHp));
			if (barWidth > 40) barWidth = 40;
			g2d.fillRect(-20, -35, barWidth, 5);

			if (poisonTimer > 0) { g2d.setColor(Color.MAGENTA); g2d.drawString("POISON", -20, -40); }

			if (weapon.isReloading) {
				g2d.setColor(Color.GRAY); g2d.fillRect(-20, -45, 40, 5);
				g2d.setColor(Color.YELLOW);
				double progress = (double)weapon.reloadTimer / weapon.reloadDuration;
				g2d.fillRect(-20, -45, (int)(40 * progress), 5);
			}

			g2d.rotate(angle);
			BufferedImage img = (id == myId) ? imgPlayerMe : imgPlayerEnemy;
			if (img != null) g2d.drawImage(img, -size, -size, size*2, size*2, null);
			else {
				g2d.setColor(color); g2d.fillOval(-size, -size, size*2, size*2);
				g2d.setColor(Color.BLACK); g2d.drawLine(0, 0, size+10, 0);
			}
			g2d.setTransform(old);
		}
	}

	class Weapon {
		Player owner;
		int maxAmmo = 1, currentAmmo = 1, reloadDuration = 60;
		int damage = 20, bulletSize = 8;
		double bulletSpeed = 10.0;
		boolean isReloading = false;
		int reloadTimer = 0, burstQueue = 0, burstTimer = 0;
		ArrayList<WeaponEffect> effects = new ArrayList<>();

		public Weapon(Player p) { this.owner = p; }
		public void reset() { currentAmmo = maxAmmo; isReloading = false; reloadTimer = 0; burstQueue = 0; }
		public void addEffect(WeaponEffect e) { effects.add(e); recalcStats(); }
		private void recalcStats() {
			maxAmmo = 1; reloadDuration = 60; damage = 20; bulletSpeed = 10.0; bulletSize = 8;
			for (WeaponEffect e : effects) e.applyStats(this);
		}
		public void update() {
			if (isReloading) {
				reloadTimer++;
				if (reloadTimer >= reloadDuration) { currentAmmo = maxAmmo; isReloading = false; reloadTimer = 0; }
			}
			if (burstQueue > 0) {
				burstTimer++;
				if (burstTimer >= 5) {
					fireRaw(owner.x, owner.y, owner.angle); burstQueue--; burstTimer = 0;
				}
			}
		}
		public void tryShoot() {
			if (isReloading || currentAmmo <= 0) {
				if (!isReloading && currentAmmo < maxAmmo) startReload();
				return;
			}
			boolean hasBurst = false, hasTri = false;
			for(WeaponEffect e : effects) {
				if(e instanceof EffectBurst) hasBurst = true;
				if(e instanceof EffectTrifurcation) hasTri = true;
			}
			if (hasBurst) { currentAmmo--; burstQueue = 3; burstTimer = 5; }
			else {
				currentAmmo--;
				if (hasTri) {
					fireRaw(owner.x, owner.y, owner.angle);
					fireRaw(owner.x, owner.y, owner.angle - 0.3);
					fireRaw(owner.x, owner.y, owner.angle + 0.3);
				} else fireRaw(owner.x, owner.y, owner.angle);
			}
			if (currentAmmo <= 0) startReload();
		}
		private void startReload() { isReloading = true; reloadTimer = 0; }
		private void fireRaw(double x, double y, double angle) {
			int bId = (int)(Math.random() * 1000000);
			int flags = FLAG_NONE;
			for(WeaponEffect e : effects) flags |= e.getFlag();
			out.println("SHOT " + bId + " " + x + " " + y + " " + angle +
					" " + bulletSpeed + " " + damage + " " + bulletSize + " " + flags + " " + myId);
		}
	}

	static abstract class WeaponEffect { public abstract void applyStats(Weapon w); public int getFlag() { return FLAG_NONE; } }
	static class EffectHill extends WeaponEffect { public void applyStats(Weapon w) { w.damage *= 0.9; } public int getFlag() { return FLAG_HILL; } }
	static class EffectBounce extends WeaponEffect { public void applyStats(Weapon w) { w.bulletSpeed *= 0.8; w.reloadDuration *= 1.2; } public int getFlag() { return FLAG_BOUNCE; } }
	static class EffectTrifurcation extends WeaponEffect { public void applyStats(Weapon w) { w.reloadDuration *= 1.5; } }
	static class EffectRising extends WeaponEffect { public void applyStats(Weapon w) { w.bulletSpeed *= 2.0; w.damage *= 0.7; } }
	static class EffectImpact extends WeaponEffect { public void applyStats(Weapon w) { w.damage *= 2.0; w.reloadDuration *= 1.3; } }
	static class EffectBigBall extends WeaponEffect { public void applyStats(Weapon w) { w.bulletSize *= 3; w.damage *= 0.8; w.bulletSpeed *= 0.8; } }
	static class EffectPoison extends WeaponEffect { public void applyStats(Weapon w) { w.reloadDuration *= 1.1; } public int getFlag() { return FLAG_POISON; } }
	static class EffectBurst extends WeaponEffect { public void applyStats(Weapon w) { w.reloadDuration *= 1.2; } }

	static abstract class PowerUp {
		String name, desc, merit, demerit;
		public PowerUp(String n, String d, String m, String dm) { name=n; desc=d; merit=m; demerit=dm; }
		public abstract void apply(Player p);
	}

	static class PowerUpFactory {
		public static ArrayList<PowerUp> getRandomPowerUps(int count) {
			ArrayList<PowerUp> all = new ArrayList<>();
			all.add(new PowerUp("Hill", "Vampire", "Heal on hit", "Less Dmg") { public void apply(Player p) { p.weapon.addEffect(new EffectHill()); }});
			all.add(new PowerUp("Bounce", "Bounce", "Wall bounce", "Slow/Reload+") { public void apply(Player p) { p.weapon.addEffect(new EffectBounce()); }});
			all.add(new PowerUp("Tri-Shot", "Shotgun", "3-Way Shot", "Reload++") { public void apply(Player p) { p.weapon.addEffect(new EffectTrifurcation()); }});
			all.add(new PowerUp("Rising", "Sniper", "High Speed", "Less Dmg") { public void apply(Player p) { p.weapon.addEffect(new EffectRising()); }});
			all.add(new PowerUp("Impact", "Cannon", "High Dmg", "Reload+") { public void apply(Player p) { p.weapon.addEffect(new EffectImpact()); }});
			all.add(new PowerUp("Big Ball", "Giant", "Huge Bullet", "Slow") { public void apply(Player p) { p.weapon.addEffect(new EffectBigBall()); }});
			all.add(new PowerUp("Poison", "Venom", "Slow Enemy", "Reload+") { public void apply(Player p) { p.weapon.addEffect(new EffectPoison()); }});
			all.add(new PowerUp("Reel Gun", "Burst", "3-Round Burst", "Reload+") { public void apply(Player p) { p.weapon.addEffect(new EffectBurst()); }});
			all.add(new PowerUp("Big Boy", "Tank", "HP++", "Hitbox++") { public void apply(Player p) { p.maxHp += 50; p.hp = p.maxHp; p.size *= 1.8; }});
			all.add(new PowerUp("Small Boy", "Rogue", "Evasion UP", "HP--") { public void apply(Player p) { p.maxHp = 70; p.hp = p.maxHp; p.size *= 0.6; p.speed *= 1.3; }});
			Collections.shuffle(all);
			return new ArrayList<>(all.subList(0, Math.min(count, all.size())));
		}
	}

	class Bullet {
		boolean isActive = false;
		int id, ownerId, damage, size, typeFlag;
		double x, y, angle, speed;
		int bounceCount = 0, lifeTimer = 0;
		public Bullet() {}
		public void activate(int id, double x, double y, double a, double s, int d, int sz, int f, int o) {
			this.id=id; this.x=x; this.y=y; this.angle=a; this.speed=s;
			this.damage=d; this.size=sz; this.typeFlag=f; this.ownerId=o;
			isActive = true; bounceCount = 0; lifeTimer = 0;
		}
		public void deactivate() { isActive = false; }
		public void update() { x += Math.cos(angle) * speed; y += Math.sin(angle) * speed; lifeTimer++; }
		public void draw(Graphics2D g2d) {
			if (!isActive) return;
			g2d.setColor(Color.YELLOW);
			if((typeFlag & FLAG_POISON)!=0) g2d.setColor(Color.MAGENTA);
			g2d.fillOval((int)x-size/2, (int)y-size/2, size, size);
		}
	}

	class GamePanel extends JPanel implements KeyListener, MouseListener, MouseMotionListener {
		boolean keyW, keyS, keyA, keyD; int mouseX, mouseY;
		Rectangle startButtonRect = new Rectangle(300, 500, 200, 60);
		Rectangle[] mapButtons = new Rectangle[3];
		Rectangle[] cardRects = new Rectangle[3];

		public GamePanel() {
			setFocusable(true); setBackground(Color.DARK_GRAY);
			addKeyListener(this); addMouseListener(this); addMouseMotionListener(this);
			int btnW = 120; int btnH = 40; int startX = 200; int y = 380;
			mapButtons[0] = new Rectangle(startX, y, btnW, btnH);
			mapButtons[1] = new Rectangle(startX + 140, y, btnW, btnH);
			mapButtons[2] = new Rectangle(startX + 280, y, btnW, btnH);
		}

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			Graphics2D g2d = (Graphics2D) g;
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			if (currentState == GameState.TITLE) drawTitleScreen(g2d);
			else if (currentState == GameState.WAITING) drawWaitingScreen(g2d);
			else {
				drawGameScreen(g2d);
				if (currentState == GameState.ROUND_END_SELECT) drawPowerUpSelection(g2d);
				if (currentState == GameState.ROUND_END_WAIT) drawRoundEndWait(g2d);
				if (currentState == GameState.COUNTDOWN) drawCountdown(g2d);
				if (currentState == GameState.GAME_OVER) drawGameOver(g2d);
			}
		}

		private String getStars(int count) {
			StringBuilder sb = new StringBuilder();
			for(int i=0; i<count; i++) sb.append("*");
			return sb.toString();
		}

		private void drawTitleScreen(Graphics2D g2d) {
			g2d.setColor(Color.CYAN); g2d.setFont(new Font("Arial", Font.BOLD, 50));
			String title = "BATTLE GAME"; int tw = g2d.getFontMetrics().stringWidth(title);
			g2d.drawString(title, (800 - tw) / 2, 200);

			g2d.setFont(new Font("Arial", Font.BOLD, 16));
			String[] labels = {"Random", "Map A", "Map B"};
			for (int i = 0; i < 3; i++) {
				Rectangle btn = mapButtons[i];
				if (selectedMapType == i) g2d.setColor(Color.YELLOW);
				else g2d.setColor(Color.LIGHT_GRAY);
				g2d.fill(btn);
				g2d.setColor(Color.BLACK);
				String lb = labels[i];
				int sw = g2d.getFontMetrics().stringWidth(lb);
				g2d.drawString(lb, btn.x + (btn.width - sw)/2, btn.y + 26);
			}

			g2d.setColor(Color.GREEN); g2d.fill(startButtonRect);
			g2d.setColor(Color.BLACK); g2d.setFont(new Font("Arial", Font.BOLD, 30));
			g2d.drawString("START", startButtonRect.x + 50, startButtonRect.y + 40);
		}

		private void drawWaitingScreen(Graphics2D g2d) {
			g2d.setColor(Color.WHITE); g2d.setFont(new Font("Arial", Font.BOLD, 30));
			String msg = "WAITING FOR OPPONENT..."; int tw = g2d.getFontMetrics().stringWidth(msg);
			g2d.drawString(msg, (800 - tw) / 2, 300);
			g2d.setFont(new Font("Arial", Font.PLAIN, 20));
			g2d.drawString("Joined: " + joinedPlayers.size(), (800 - tw) / 2, 350);
		}

		private void drawGameScreen(Graphics2D g2d) {
			g2d.setFont(new Font("Arial", Font.BOLD, 30));
			g2d.setColor(Color.CYAN); g2d.drawString("ME: " + getStars(myWinCount), 50, 40);
			g2d.setColor(Color.RED);  g2d.drawString("ENEMY: " + getStars(enemyWinCount), 500, 40);

			g2d.setColor(Color.WHITE); g2d.setStroke(new BasicStroke(3));
			g2d.drawRect(MAP_X, MAP_Y, MAP_WIDTH, MAP_HEIGHT);
			g2d.setColor(Color.LIGHT_GRAY); g2d.setStroke(new BasicStroke(5));
			for (Line2D.Double wall : obstacles) g2d.draw(wall);
			g2d.setStroke(new BasicStroke(1));

			for (Player p : players.values()) p.draw(g2d);
			for (Bullet b : bulletPool) b.draw(g2d);

			if (players.containsKey(myId)) {
				Player me = players.get(myId);
				g2d.setFont(new Font("Monospaced", Font.BOLD, 18));
				if (me.weapon.currentAmmo == 0) g2d.setColor(Color.RED); else g2d.setColor(Color.CYAN);
				String ammoText = me.weapon.isReloading ? "RELOADING..." : "AMMO: " + me.weapon.currentAmmo + "/" + me.weapon.maxAmmo;
				g2d.drawString(ammoText, MAP_X, MAP_Y + MAP_HEIGHT + 30);
			}
		}

		private void drawPowerUpSelection(Graphics2D g2d) {
			g2d.setColor(new Color(0,0,0,180)); g2d.fillRect(0,0,getWidth(),getHeight());
			g2d.setColor(Color.WHITE); g2d.setFont(new Font("Arial", Font.BOLD, 40));
			centerString(g2d, "CHOOSE AN UPGRADE", 150);

			int startX = 100, y = 250, w = 180, h = 250, gap = 20;
			for(int i=0; i<presentedPowerUps.size(); i++) {
				PowerUp p = presentedPowerUps.get(i);
				Rectangle rect = new Rectangle(startX + (w+gap)*i, y, w, h);
				cardRects[i] = rect;
				if(rect.contains(mouseX, mouseY)) { g2d.setColor(Color.YELLOW); g2d.setStroke(new BasicStroke(3)); }
				else { g2d.setColor(Color.WHITE); g2d.setStroke(new BasicStroke(1)); }
				g2d.draw(rect); g2d.setColor(new Color(50,50,50)); g2d.fill(rect);
				g2d.setColor(Color.WHITE); g2d.setFont(new Font("Arial", Font.BOLD, 20));
				g2d.drawString(p.name, rect.x+10, rect.y+40);
				g2d.setFont(new Font("Arial", Font.PLAIN, 12));
				g2d.drawString(p.desc, rect.x+10, rect.y+70);
				g2d.setColor(Color.CYAN); g2d.drawString("Good: "+p.merit, rect.x+10, rect.y+120);
				g2d.setColor(Color.PINK); g2d.drawString("Bad: "+p.demerit, rect.x+10, rect.y+150);
			}
		}

		private void drawRoundEndWait(Graphics2D g2d) {
			g2d.setColor(new Color(0,0,0,150)); g2d.fillRect(0,0,getWidth(),getHeight());
			g2d.setColor(Color.WHITE); g2d.setFont(new Font("Arial", Font.BOLD, 40));
			centerString(g2d, resultMessage, 200);
			centerString(g2d, "Waiting for Opponent...", 300);
		}

		private void drawCountdown(Graphics2D g2d) {
			g2d.setColor(Color.YELLOW); g2d.setFont(new Font("Arial", Font.BOLD, 100));
			centerString(g2d, String.valueOf(countdownTimer/FPS + 1), 300);
		}

		private void drawGameOver(Graphics2D g2d) {
			g2d.setColor(new Color(0,0,0,200)); g2d.fillRect(0,0,getWidth(),getHeight());
			g2d.setColor(resultMessage.contains("VICTORY") ? Color.YELLOW : Color.GRAY);
			g2d.setFont(new Font("Arial", Font.BOLD, 50));
			centerString(g2d, resultMessage, 300);
			g2d.setColor(Color.WHITE); g2d.setFont(new Font("Arial", Font.PLAIN, 20));
			centerString(g2d, "CLICK TO RETURN TITLE", 400);
		}

		private void centerString(Graphics2D g, String s, int y) {
			int w = g.getFontMetrics().stringWidth(s);
			g.drawString(s, (getWidth()-w)/2, y);
		}

		public void mousePressed(MouseEvent e) {
			int mx = e.getX(); int my = e.getY();
			if (currentState == GameState.TITLE) {
				for (int i = 0; i < 3; i++) {
					if (mapButtons[i].contains(mx, my)) {
						selectedMapType = i;
						repaint();
						return;
					}
				}
				if (startButtonRect.contains(mx, my)) { out.println("JOIN " + TARGET_GAME_ID); currentState = GameState.WAITING; }
			} else if (currentState == GameState.PLAYING) {
				if (!players.containsKey(myId)) return;
				players.get(myId).weapon.tryShoot();
			} else if (currentState == GameState.ROUND_END_SELECT) {
				for(int i=0; i<presentedPowerUps.size(); i++) {
					if(cardRects[i] != null && cardRects[i].contains(mx, my)) {
						presentedPowerUps.get(i).apply(players.get(myId));
						onPowerUpSelected();
						break;
					}
				}
			} else if (currentState == GameState.GAME_OVER) {
				backToTitle();
			}
		}

		public void keyPressed(KeyEvent e) {
			if (currentState != GameState.PLAYING) return;
			int k = e.getKeyCode();
			if (k == KeyEvent.VK_W) keyW = true; if (k == KeyEvent.VK_S) keyS = true;
			if (k == KeyEvent.VK_A) keyA = true; if (k == KeyEvent.VK_D) keyD = true;
			if (k == KeyEvent.VK_R) if (players.containsKey(myId)) { }
		}

		public void keyReleased(KeyEvent e) {
			if (currentState != GameState.PLAYING) return;
			int k = e.getKeyCode();
			if (k == KeyEvent.VK_W) keyW = false; if (k == KeyEvent.VK_S) keyS = false;
			if (k == KeyEvent.VK_A) keyA = false; if (k == KeyEvent.VK_D) keyD = false;
		}

		public void mouseMoved(MouseEvent e) { mouseX = e.getX(); mouseY = e.getY(); }
		public void mouseDragged(MouseEvent e) { mouseX = e.getX(); mouseY = e.getY(); }
		public void mouseClicked(MouseEvent e) {} public void mouseEntered(MouseEvent e) {}
		public void mouseExited(MouseEvent e) {} public void mouseReleased(MouseEvent e) {}
		public void keyTyped(KeyEvent e) {}
	}

	public static void main(String[] args) { new ActionClient(); }
}