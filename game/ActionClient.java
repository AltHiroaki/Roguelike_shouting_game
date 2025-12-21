package game;

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

// 作成したパッケージをインポート
import game.*;
// 定数を直接使えるようにする（GameConstants.MAP_X と書かなくて済む）
import static game.GameConstants.*;

public class ActionClient extends JFrame {

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

	// PlayerやBulletは gameパッケージのものを使用
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

		setTitle("Action Game Client - Refactored");
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
		// 入力状態を渡して更新 (引数が変わりました)
		me.update(panel.keyW, panel.keyS, panel.keyA, panel.keyD, panel.mouseX, panel.mouseY, obstacles, out);

		// 弾の更新
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
			// Random Map
			int obstacleCount = 8;
			for (int i = 0; i < obstacleCount; i++) {
				int margin = 100;
				int x1 = MAP_X + margin + (int)(Math.random() * (MAP_WIDTH - margin * 2));
				int y1 = MAP_Y + margin + (int)(Math.random() * (MAP_HEIGHT - margin * 2));
				int length = 50 + (int)(Math.random() * 100);
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
			// PowerUpFactoryをgameパッケージから呼び出す
			presentedPowerUps = PowerUpFactory.getRandomPowerUps(3);
		}
	}

	private void onPowerUpSelected() {
		if (players.containsKey(myId)) players.get(myId).sendStatus(out);

		out.println("NEXT_ROUND_READY " + myId);
		currentState = GameState.ROUND_END_WAIT;
	}

	private void startCountdown() {
		resetPositions();
		currentState = GameState.COUNTDOWN;
		countdownTimer = 90;

		if (players.containsKey(myId)) players.get(myId).sendStatus(out);
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
				int id = Integer.parseInt(tokens[7]);
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
				int id = Integer.parseInt(tokens[1]);
				int mHp = Integer.parseInt(tokens[2]);
				int sz = Integer.parseInt(tokens[3]);
				if (players.containsKey(id)) {
					Player p = players.get(id);
					p.maxHp = mHp;
					p.size = sz;
				} else {
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
	//  GamePanel Class (Inner Class)
	// ==========================================

	class GamePanel extends JPanel implements KeyListener, MouseListener, MouseMotionListener {
		boolean keyW, keyS, keyA, keyD; int mouseX, mouseY;
		Rectangle startButtonRect = new Rectangle(300, 500, 200, 60);
		Rectangle[] mapButtons = new Rectangle[3];
		// パワーアップカードの描画用

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

			// Playerクラスのdrawメソッド呼び出し (引数に合わせて調整)
			for (Player p : players.values()) p.draw(g2d, imgPlayerMe, imgPlayerEnemy, myId);
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
				players.get(myId).weapon.tryShoot(out, myId); // 修正: 引数を追加
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