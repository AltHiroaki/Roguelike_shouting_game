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
 * アクションゲームのクライアントクラス。
 * サーバーと通信し、プレイヤーの移動、弾の発射、アイテム取得、障害物判定などの
 * ゲームロジックと描画（GUI）を管理します。
 */
public class ActionClient extends JFrame {

	// ==========================================
	//  【設定・定数エリア】
	// ==========================================

	// --- 通信設定 ---
	private static final String SERVER_IP = "13.208.87.43";
	private static final int SERVER_PORT = 10000;
	private static final int TARGET_GAME_ID = 1;

	// --- 画像ファイル設定 ---
	private static final String IMAGE_PATH_PLAYER_ME = "player_me.png";       // 自機の画像
	private static final String IMAGE_PATH_PLAYER_ENEMY = "player_enemy.png"; // 敵機の画像

	// --- マップ設定 ---
	private static final int MAP_X = 50;
	private static final int MAP_Y = 50;
	private static final int MAP_WIDTH = 700;
	private static final int MAP_HEIGHT = 450;

	// --- ゲーム進行設定 ---
	private static final int TIME_LIMIT_SEC = 60;
	private static final int FPS = 60;

	// --- プレイヤー設定 ---
	private static final int PLAYER_MAX_HP = 100;         // 最大HP
	private static final int PLAYER_MAX_AMMO = 1;         // 最大弾数
	private static final int PLAYER_RELOAD_DURATION = 60; // リロードにかかる時間(フレーム数)
	private static final double PLAYER_SPEED = 3.0;       // 移動速度
	private static final int PLAYER_SIZE = 15;            // 半径（描画サイズはこの2倍）

	// --- 弾(Bullet)設定 ---
	private static final int BULLET_DAMAGE = 10;          // 弾のダメージ
	private static final double BULLET_SPEED = 10.0;      // 弾の速度
	private static final int BULLET_SIZE = 8;             // 弾の直径
	// 弾同士の相殺判定距離の二乗 ( (半径+半径)^2 = 直径^2 )
	private static final int BULLET_COLLISION_DIST_SQ = BULLET_SIZE * BULLET_SIZE;
	private static final int BULLET_SELF_HIT_DELAY = 10;  // 自分の弾が自分に当たるまでの猶予フレーム

	// --- アイテム設定 ---
	private static final double ITEM_SPAWN_CHANCE = 0.005;
	private static final int ITEM_LIFETIME_SEC = 4;
	private static final int ITEM_LIFETIME_LIMIT = (ITEM_LIFETIME_SEC * FPS) / 4;
	private static final int ITEM_HEAL_AMOUNT = 30;       // 回復アイテムの回復量

	// --- 障害物設定 ---
	private static final int OBSTACLE_COUNT = 8;
	private static final int MIN_WALL_LENGTH = 50;
	private static final int MAX_WALL_LENGTH = 150;

	/**
	 * アイテムの種類を定義する列挙型。
	 */
	enum ItemType {
		HEAL(0, 70), AMMO(1, 30);
		final int id; final int weight;
		ItemType(int id, int weight) { this.id = id; this.weight = weight; }
		static ItemType getById(int id) {
			for (ItemType t : values()) if (t.id == id) return t;
			return HEAL;
		}
	}

	// ==========================================
	//  システム変数
	// ==========================================
	private Socket socket;
	private PrintWriter out;
	private BufferedReader in;
	private int myId;

	private GamePanel panel;
	private javax.swing.Timer gameTimer;

	enum GameState { TITLE, WAITING, PLAYING, RESULT }
	private GameState currentState = GameState.TITLE;
	private String resultMessage = "";
	private int timeLeft;
	private Set<Integer> joinedPlayers = Collections.synchronizedSet(new HashSet<>());

	// ==========================================
	//  マップ選択用変数
	// ==========================================
	private static final int MAP_TYPE_RANDOM = 0;
	private static final int MAP_TYPE_A = 1;
	private static final int MAP_TYPE_B = 2;
	private int selectedMapType = MAP_TYPE_RANDOM;

	// ==========================================
	//  画像データ
	// ==========================================
	private BufferedImage imgPlayerMe;
	private BufferedImage imgPlayerEnemy;

	// ==========================================
	//  データリスト
	// ==========================================
	private ConcurrentHashMap<Integer, Player> players = new ConcurrentHashMap<>();
	private CopyOnWriteArrayList<Bullet> bullets = new CopyOnWriteArrayList<>();
	private CopyOnWriteArrayList<Item> items = new CopyOnWriteArrayList<>();
	private ArrayList<Line2D.Double> obstacles = new ArrayList<>();

	public ActionClient() {
		loadImages();
		setupConnection(SERVER_IP, SERVER_PORT);

		setTitle("Action Game Client - Game ID: " + TARGET_GAME_ID);
		setSize(800, 650);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		panel = new GamePanel();
		add(panel);

		gameTimer = new javax.swing.Timer(1000 / FPS, e -> gameLoop());
		gameTimer.start();

		setVisible(true);
	}

	private void loadImages() {
		try {
			File fileMe = new File(IMAGE_PATH_PLAYER_ME);
			if (fileMe.exists()) {
				imgPlayerMe = ImageIO.read(fileMe);
				System.out.println("自機画像を読み込みました: " + fileMe.getAbsolutePath());
			}

			File fileEnemy = new File(IMAGE_PATH_PLAYER_ENEMY);
			if (fileEnemy.exists()) {
				imgPlayerEnemy = ImageIO.read(fileEnemy);
				System.out.println("敵画像を読み込みました: " + fileEnemy.getAbsolutePath());
			}
		} catch (IOException e) {
			System.err.println("画像の読み込みに失敗しました: " + e.getMessage());
		}
	}

	private void setupConnection(String host, int port) {
		try {
			socket = new Socket(host, port);
			out = new PrintWriter(socket.getOutputStream(), true);
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			new Thread(this::receiveLoop).start();
		} catch (IOException e) {
			JOptionPane.showMessageDialog(this, "サーバーに接続できませんでした: " + host + ":" + port);
			System.exit(0);
		}
	}

	private void gameLoop() {
		if (!players.containsKey(myId)) return;
		Player me = players.get(myId);

		switch (currentState) {
			case TITLE:
			case WAITING:
			case RESULT:
				checkStartCondition();
				break;
			case PLAYING:
				updateGame(me);
				break;
		}
		panel.repaint();
	}

	private void updateGame(Player me) {
		if (timeLeft > 0) timeLeft--; else checkTimeUp();

		me.update(panel.mouseX, panel.mouseY, panel, obstacles);

		if (!players.isEmpty()) {
			int minId = Integer.MAX_VALUE;
			for(int id : players.keySet()) minId = Math.min(minId, id);
			if (myId == minId) {
				if (Math.random() < ITEM_SPAWN_CHANCE) spawnRandomItem();
			}
		}

		for (Item item : items) {
			boolean isAlive = item.update();
			if (!isAlive) { items.remove(item); continue; }
			if (item.isCollected) continue;
			if (me.getBounds().intersects(item.getBounds())) {
				item.isCollected = true;
				out.println("ITEM_GET " + item.uniqueId + " " + myId);
			}
		}

		// --- 弾の処理 ---
		for (Bullet b : bullets) {
			b.update();

			// 1. 外枠との反射判定
			boolean hitBoundary = false;
			if (b.x < MAP_X) {
				if (b.bounceCount < 1) {
					b.bounceCount++; b.angle = Math.PI - b.angle; b.x = MAP_X; hitBoundary = true;
				} else { bullets.remove(b); continue; }
			} else if (b.x > MAP_X + MAP_WIDTH) {
				if (b.bounceCount < 1) {
					b.bounceCount++; b.angle = Math.PI - b.angle; b.x = MAP_X + MAP_WIDTH; hitBoundary = true;
				} else { bullets.remove(b); continue; }
			}

			if (b.y < MAP_Y) {
				if (b.bounceCount < 1) {
					b.bounceCount++; b.angle = -b.angle; b.y = MAP_Y; hitBoundary = true;
				} else { bullets.remove(b); continue; }
			} else if (b.y > MAP_Y + MAP_HEIGHT) {
				if (b.bounceCount < 1) {
					b.bounceCount++; b.angle = -b.angle; b.y = MAP_Y + MAP_HEIGHT; hitBoundary = true;
				} else { bullets.remove(b); continue; }
			}

			if (!hitBoundary) {
				// 2. 障害物との反射判定
				boolean hitObstacle = false;
				for (Line2D.Double wall : obstacles) {
					if (wall.ptSegDist(b.x, b.y) < 5) {
						if (b.bounceCount < 1) {
							b.bounceCount++;
							if (Math.abs(wall.y1 - wall.y2) < 1.0) b.angle = -b.angle; // 横壁
							else b.angle = Math.PI - b.angle; // 縦壁
							hitObstacle = true; break;
						} else {
							bullets.remove(b); hitObstacle = true; break;
						}
					}
				}
				if (hitObstacle && !bullets.contains(b)) continue;
			}

			// 3. プレイヤーへのヒット判定 (自爆あり、定数使用)
			if (b.ownerId != myId || b.lifeTimer > BULLET_SELF_HIT_DELAY) {
				if (me.getBounds().contains(b.x, b.y)) {
					me.hp -= BULLET_DAMAGE; // 定数ダメージ

					// ★修正: 弾が当たったことを全員に通知して消す
					out.println("BULLET_HIT " + b.id);
					bullets.remove(b); // 自分の画面でも即座に消す

					if (me.hp <= 0) {
						me.hp = 0;
						out.println("DEAD " + myId);
						setGameOver("YOU LOSE");
					}
				}
			}
		}

		// --- 弾同士の相殺判定 (定数使用) ---
		Set<Bullet> collidedBullets = new HashSet<>();
		for (int i = 0; i < bullets.size(); i++) {
			Bullet b1 = bullets.get(i);
			for (int j = i + 1; j < bullets.size(); j++) {
				Bullet b2 = bullets.get(j);

				if (b1.ownerId == b2.ownerId) continue;

				double distSq = (b1.x - b2.x)*(b1.x - b2.x) + (b1.y - b2.y)*(b1.y - b2.y);
				// 定数 BULLET_COLLISION_DIST_SQ を使用
				if (distSq < BULLET_COLLISION_DIST_SQ) {
					collidedBullets.add(b1);
					collidedBullets.add(b2);
				}
			}
		}
		bullets.removeAll(collidedBullets);

		out.println("MOVE " + (int)me.x + " " + (int)me.y + " " + me.angle + " " + me.hp
				+ " " + me.isReloading + " " + me.reloadTimer);
	}

	private void spawnRandomItem() {
		int totalWeight = 0;
		for (ItemType t : ItemType.values()) totalWeight += t.weight;
		int r = (int)(Math.random() * totalWeight);
		int selectedTypeId = 0;
		int currentWeight = 0;
		for (ItemType t : ItemType.values()) {
			currentWeight += t.weight;
			if (r < currentWeight) { selectedTypeId = t.id; break; }
		}
		int x = MAP_X + 20 + (int)(Math.random() * (MAP_WIDTH - 40));
		int y = MAP_Y + 20 + (int)(Math.random() * (MAP_HEIGHT - 40));
		int uniqueId = (int)(Math.random() * 1000000);
		out.println("ITEM_SPAWN " + uniqueId + " " + selectedTypeId + " " + x + " " + y);
	}

	private void checkStartCondition() {
		if (currentState == GameState.WAITING && joinedPlayers.size() >= 2) {
			startGame();
		}
	}

	private void startGame() {
		System.out.println("Game Start!");
		currentState = GameState.PLAYING;
		timeLeft = TIME_LIMIT_SEC * FPS;
		bullets.clear();
		items.clear();

		int minId = Integer.MAX_VALUE;
		for(int id : players.keySet()) minId = Math.min(minId, id);

		if (myId == minId) {
			obstacles.clear();
			generateObstacles();
			sendObstacleData();
		}

		ArrayList<Integer> sortedIds = new ArrayList<>(players.keySet());
		Collections.sort(sortedIds);

		for (int i = 0; i < sortedIds.size(); i++) {
			int pid = sortedIds.get(i);
			Player p = players.get(pid);
			if (p != null) {
				p.reset();
				if (i == 0) { p.x = MAP_X + 50; p.y = MAP_Y + 50; }
				else { p.x = MAP_X + MAP_WIDTH - 50; p.y = MAP_Y + MAP_HEIGHT - 50; }
			}
		}
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
		if (currentState == GameState.RESULT) return;
		currentState = GameState.RESULT;
		resultMessage = msg;
	}

	private void checkTimeUp() {
		if (currentState != GameState.PLAYING) return;
		Player me = players.get(myId);
		Player enemy = null;
		for (Integer id : players.keySet()) if (id != myId) enemy = players.get(id);

		if (enemy == null) return;
		if (me.hp > enemy.hp) setGameOver("YOU WIN (TIME UP)");
		else if (me.hp < enemy.hp) setGameOver("YOU LOSE (TIME UP)");
		else setGameOver("DRAW (TIME UP)");
	}

	private void backToTitle() {
		currentState = GameState.TITLE;
		joinedPlayers.clear();
		resultMessage = "";
	}

	private void handleDisconnection() {
		SwingUtilities.invokeLater(() -> {
			JOptionPane.showMessageDialog(this, "サーバーとの接続が切れました。");
			backToTitle();
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
				players.put(myId, new Player(MAP_X + 100, MAP_Y + 200, Color.BLUE));
			} else if (cmd.equals("MOVE")) {
				int x = Integer.parseInt(tokens[1]); int y = Integer.parseInt(tokens[2]);
				double angle = Double.parseDouble(tokens[3]); int hp = Integer.parseInt(tokens[4]);
				boolean isReloading = Boolean.parseBoolean(tokens[5]);
				int reloadTimer = Integer.parseInt(tokens[6]); int id = Integer.parseInt(tokens[7]);
				if (id != myId) {
					Player p = players.computeIfAbsent(id, k -> new Player(0, 0, Color.RED));
					p.x = x; p.y = y; p.angle = angle; p.hp = hp;
					p.isReloading = isReloading; p.reloadTimer = reloadTimer;
				}
			} else if (cmd.equals("SHOT")) {
				if (currentState == GameState.PLAYING) {
					// ★修正: SHOT bulletId x y angle speed ownerId
					int bId = Integer.parseInt(tokens[1]);
					double x = Double.parseDouble(tokens[2]);
					double y = Double.parseDouble(tokens[3]);
					double angle = Double.parseDouble(tokens[4]);
					double speed = Double.parseDouble(tokens[5]);
					int id = Integer.parseInt(tokens[6]);
					bullets.add(new Bullet(bId, x, y, angle, speed, id));
				}
			} else if (cmd.equals("BULLET_HIT")) {
				int targetBulletId = Integer.parseInt(tokens[1]);
				bullets.removeIf(b -> b.id == targetBulletId);

			} else if (cmd.equals("LEAVE")) {
				int id = Integer.parseInt(tokens[1]);
				players.remove(id); joinedPlayers.remove(id);
			} else if (cmd.equals("DEAD")) {
				if (currentState == GameState.PLAYING) {
					int deadPlayerId = Integer.parseInt(tokens[1]);
					if (deadPlayerId != myId) setGameOver("YOU WIN");
				}
			} else if (cmd.equals("JOIN")) {
				int gameId = Integer.parseInt(tokens[1]); int playerId = Integer.parseInt(tokens[2]);
				if (gameId == TARGET_GAME_ID) {
					joinedPlayers.add(playerId);
					if (!players.containsKey(playerId)) {
						players.put(playerId, new Player(0, 0, Color.RED));
					}
				}
			} else if (cmd.equals("ITEM_SPAWN")) {
				if (currentState == GameState.PLAYING) {
					int uId = Integer.parseInt(tokens[1]); int tId = Integer.parseInt(tokens[2]);
					int x = Integer.parseInt(tokens[3]); int y = Integer.parseInt(tokens[4]);
					boolean exists = false;
					for(Item i : items) if(i.uniqueId == uId) exists = true;
					if (!exists) items.add(new Item(uId, tId, x, y));
				}
			} else if (cmd.equals("ITEM_GET")) {
				int uId = Integer.parseInt(tokens[1]); int pId = Integer.parseInt(tokens[2]);
				Item target = null;
				for (Item i : items) { if (i.uniqueId == uId) { target = i; break; } }
				if (target != null) {
					items.remove(target);
					if (pId == myId) applyItemEffect(target.typeId);
				}
			} else if (cmd.equals("MAP_DATA")) {
				obstacles.clear();
				for (int i = 1; i < tokens.length - 1; i += 4) {
					try {
						double x1 = Double.parseDouble(tokens[i]); double y1 = Double.parseDouble(tokens[i+1]);
						double x2 = Double.parseDouble(tokens[i+2]); double y2 = Double.parseDouble(tokens[i+3]);
						obstacles.add(new Line2D.Double(x1, y1, x2, y2));
					} catch(Exception e) { break; }
				}
				int minId = Integer.MAX_VALUE;
				for(int id : players.keySet()) minId = Math.min(minId, id);
				Player me = players.get(myId);
				if (me != null) {
					if (myId == minId) { me.x = MAP_X + 50; me.y = MAP_Y + 50; }
					else { me.x = MAP_X + MAP_WIDTH - 50; me.y = MAP_Y + MAP_HEIGHT - 50; }
				}
			}
		} catch (Exception e) { e.printStackTrace(); }
	}

	private void applyItemEffect(int typeId) {
		Player me = players.get(myId);
		if (me == null) return;
		ItemType type = ItemType.getById(typeId);
		if (type == ItemType.HEAL) {
			me.hp += ITEM_HEAL_AMOUNT; if (me.hp > PLAYER_MAX_HP) me.hp = PLAYER_MAX_HP;
			System.out.println("Healed!");
		} else if (type == ItemType.AMMO) {
			me.currentAmmo = me.maxAmmo;
			System.out.println("Ammo Full!");
		}
	}

	// ==========================================
	//  内部クラス
	// ==========================================

	class Item {
		int uniqueId; int typeId; int x, y; int size = 20; boolean isCollected = false;
		int lifeTimer; int maxLife = ITEM_LIFETIME_SEC * FPS;
		public Item(int uniqueId, int typeId, int x, int y) {
			this.uniqueId = uniqueId; this.typeId = typeId; this.x = x; this.y = y; this.lifeTimer = maxLife;
		}
		public boolean update() { lifeTimer--; return lifeTimer > 0; }
		public Rectangle getBounds() { return new Rectangle(x - size/2, y - size/2, size, size); }
		public void draw(Graphics2D g2d) {
			if (lifeTimer < ITEM_LIFETIME_LIMIT) { if ((lifeTimer / 5) % 2 == 0) return; }
			AffineTransform old = g2d.getTransform(); g2d.translate(x, y);
			ItemType type = ItemType.getById(typeId);
			if (type == ItemType.HEAL) {
				g2d.setColor(Color.PINK);
				Path2D path = new Path2D.Double();
				path.moveTo(0, -5); path.curveTo(5, -15, 15, -5, 0, 10); path.curveTo(-15, -5, -5, -15, 0, -5); path.closePath();
				g2d.fill(path);
			} else if (type == ItemType.AMMO) {
				g2d.setColor(Color.ORANGE); g2d.fillRect(-8, -8, 16, 16);
				g2d.setColor(Color.BLACK); g2d.drawRect(-8, -8, 16, 16); g2d.drawString("A", -4, 5);
			}
			g2d.setTransform(old);
		}
	}

	class Player {
		double x, y, angle; int hp = PLAYER_MAX_HP; Color color;
		int maxAmmo = PLAYER_MAX_AMMO; int currentAmmo = PLAYER_MAX_AMMO;
		int reloadDuration = PLAYER_RELOAD_DURATION;
		double speed = PLAYER_SPEED; int size = PLAYER_SIZE;
		boolean isReloading = false; int reloadTimer = 0;

		public Player(double x, double y, Color c) { this.x = x; this.y = y; this.color = c; }
		public void reset() { hp = PLAYER_MAX_HP; currentAmmo = maxAmmo; isReloading = false; reloadTimer = 0; }

		public void update(int mx, int my, GamePanel panel, ArrayList<Line2D.Double> obstacles) {
			double nextX = x;
			if (panel.keyA) nextX -= speed;
			if (panel.keyD) nextX += speed;
			if (nextX < MAP_X + size) nextX = MAP_X + size;
			if (nextX > MAP_X + MAP_WIDTH - size) nextX = MAP_X + MAP_WIDTH - size;
			boolean collisionX = false;
			for (Line2D.Double wall : obstacles) {
				if (wall.ptSegDist(nextX, y) < size) { collisionX = true; break; }
			}
			if (!collisionX) x = nextX;

			double nextY = y;
			if (panel.keyW) nextY -= speed;
			if (panel.keyS) nextY += speed;
			if (nextY < MAP_Y + size) nextY = MAP_Y + size;
			if (nextY > MAP_Y + MAP_HEIGHT - size) nextY = MAP_Y + MAP_HEIGHT - size;
			boolean collisionY = false;
			for (Line2D.Double wall : obstacles) {
				if (wall.ptSegDist(x, nextY) < size) { collisionY = true; break; }
			}
			if (!collisionY) y = nextY;

			angle = Math.atan2(my - y, mx - x);
			if (isReloading) {
				reloadTimer++;
				if (reloadTimer >= reloadDuration) {
					currentAmmo = maxAmmo; isReloading = false; reloadTimer = 0;
				}
			}
		}
		public void startReload() { if (currentAmmo < maxAmmo && !isReloading) { isReloading = true; reloadTimer = 0; } }
		public Rectangle getBounds() { return new Rectangle((int)x-15, (int)y-15, 30, 30); }

		public void draw(Graphics2D g2d) {
			AffineTransform old = g2d.getTransform(); g2d.translate(x, y);

			// HPバー
			g2d.setColor(Color.RED); g2d.fillRect(-20, -35, 40, 5);
			g2d.setColor(Color.GREEN); g2d.fillRect(-20, -35, (int)(40 * (hp / (double)PLAYER_MAX_HP)), 5);

			// リロードバー
			if (isReloading) {
				g2d.setColor(Color.GRAY); g2d.fillRect(-20, -45, 40, 5);
				g2d.setColor(Color.YELLOW);
				double progress = (double)reloadTimer / reloadDuration;
				g2d.fillRect(-20, -45, (int)(40 * progress), 5);
			}

			// 本体描画 (回転)
			g2d.rotate(angle);

			BufferedImage img = (this.color == Color.BLUE) ? imgPlayerMe : imgPlayerEnemy;

			if (img != null) {
				g2d.drawImage(img, -size, -size, size*2, size*2, null);
			} else {
				if (isReloading) g2d.setColor(color.darker()); else g2d.setColor(color);
				g2d.fillRect(-size, -size, size*2, size*2);
				g2d.setColor(Color.BLACK); g2d.drawLine(0, 0, 25, 0); // 銃口
			}
			g2d.setTransform(old);
		}
	}

	class Bullet {
		// ★追加: 弾ID
		int id;
		double x, y, angle, speed; int ownerId;
		int bounceCount = 0; // 反射回数カウント
		int lifeTimer = 0;   // 生存時間(フレーム数)

		// ★修正: コンストラクタにidを追加
		public Bullet(int id, double x, double y, double angle, double speed, int ownerId) {
			this.id = id;
			this.x = x; this.y = y; this.angle = angle; this.speed = speed; this.ownerId = ownerId;
		}
		public void update() {
			lifeTimer++;
			x += Math.cos(angle) * speed;
			y += Math.sin(angle) * speed;
		}
		public void draw(Graphics2D g2d) { g2d.setColor(Color.YELLOW); g2d.fillOval((int)x-BULLET_SIZE/2, (int)y-BULLET_SIZE/2, BULLET_SIZE, BULLET_SIZE); }
	}

	class GamePanel extends JPanel implements KeyListener, MouseListener, MouseMotionListener {
		boolean keyW, keyS, keyA, keyD; int mouseX, mouseY;

		// マップ選択ボタンの定義
		Rectangle startButtonRect = new Rectangle(300, 450, 200, 60); // STARTボタン位置調整
		Rectangle[] mapButtons = new Rectangle[3];

		public GamePanel() {
			setFocusable(true); setBackground(Color.DARK_GRAY);
			addKeyListener(this); addMouseListener(this); addMouseMotionListener(this);

			// マップ選択ボタンの配置
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
			else if (currentState == GameState.PLAYING) drawGameScreen(g2d);
			else if (currentState == GameState.RESULT) { drawGameScreen(g2d); drawResultScreen(g2d); }
		}
		private void drawTitleScreen(Graphics2D g2d) {
			g2d.setColor(Color.CYAN); g2d.setFont(new Font("Arial", Font.BOLD, 50));
			String title = "BATTLE GAME"; int tw = g2d.getFontMetrics().stringWidth(title);
			g2d.drawString(title, (800 - tw) / 2, 200);

			// マップ選択ボタン描画
			g2d.setFont(new Font("Arial", Font.BOLD, 16));
			String[] labels = {"Random", "Map A", "Map B"};
			for (int i = 0; i < 3; i++) {
				Rectangle btn = mapButtons[i];
				if (selectedMapType == i) g2d.setColor(Color.YELLOW); // 選択中は黄色
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
			g2d.setColor(Color.WHITE); g2d.setStroke(new BasicStroke(3));
			g2d.drawRect(MAP_X, MAP_Y, MAP_WIDTH, MAP_HEIGHT);
			g2d.setColor(Color.LIGHT_GRAY); g2d.setStroke(new BasicStroke(5));
			for (Line2D.Double wall : obstacles) g2d.draw(wall);
			g2d.setStroke(new BasicStroke(1));
			for (Item item : items) item.draw(g2d);
			for (Player p : players.values()) p.draw(g2d);
			for (Bullet b : bullets) b.draw(g2d);
			drawGameStatus(g2d);
		}
		private void drawGameStatus(Graphics2D g2d) {
			int textY = MAP_Y + MAP_HEIGHT + 40;
			g2d.setColor(Color.WHITE); g2d.setFont(new Font("Monospaced", Font.BOLD, 24));
			int seconds = timeLeft / FPS;
			g2d.drawString("TIME: " + seconds, MAP_X + 300, 30);
			if (players.containsKey(myId)) {
				Player me = players.get(myId);
				g2d.setFont(new Font("Monospaced", Font.BOLD, 18));
				if (me.currentAmmo == 0) g2d.setColor(Color.RED); else g2d.setColor(Color.CYAN);
				String ammoText = me.isReloading ? "RELOADING..." : "AMMO: " + me.currentAmmo + "/" + me.maxAmmo;
				g2d.drawString(ammoText, MAP_X, textY);
			}
		}
		private void drawResultScreen(Graphics2D g2d) {
			g2d.setColor(new Color(0, 0, 0, 150)); g2d.fillRect(0, 0, 800, 650);
			g2d.setFont(new Font("Arial", Font.BOLD, 60));
			if (resultMessage.contains("WIN")) g2d.setColor(Color.YELLOW);
			else if (resultMessage.contains("LOSE")) g2d.setColor(Color.MAGENTA);
			else g2d.setColor(Color.WHITE);
			int tw = g2d.getFontMetrics().stringWidth(resultMessage);
			g2d.drawString(resultMessage, (800 - tw) / 2, 300);
			g2d.setColor(Color.WHITE); g2d.setFont(new Font("Arial", Font.PLAIN, 20));
			String guide = "CLICK TO RETURN TITLE"; int gw = g2d.getFontMetrics().stringWidth(guide);
			g2d.drawString(guide, (800 - gw) / 2, 400);
		}
		public void mousePressed(MouseEvent e) {
			int mx = e.getX(); int my = e.getY();
			if (currentState == GameState.TITLE) {
				// マップボタンクリック判定
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
				Player me = players.get(myId);
				if (me.isReloading) return;
				if (me.currentAmmo > 0) {
					me.currentAmmo--;
					// ★修正: 弾IDを生成して送信
					int bulletId = (int)(Math.random() * 1000000);
					out.println("SHOT " + bulletId + " " + me.x + " " + me.y + " " + me.angle + " " + BULLET_SPEED);
				} else { me.startReload(); }
			} else if (currentState == GameState.RESULT) { backToTitle(); }
		}
		public void keyPressed(KeyEvent e) {
			if (currentState != GameState.PLAYING) return;
			int k = e.getKeyCode();
			if (k == KeyEvent.VK_W) keyW = true; if (k == KeyEvent.VK_S) keyS = true;
			if (k == KeyEvent.VK_A) keyA = true; if (k == KeyEvent.VK_D) keyD = true;
			if (k == KeyEvent.VK_R) if (players.containsKey(myId)) players.get(myId).startReload();
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