package game;

import javax.swing.*;
import java.awt.Color;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.*;
import java.net.*;
import javax.swing.SwingUtilities;
import java.util.*;

import static game.GameConstants.*;

public class ActionClient extends JFrame {
	private Socket socket;
	private PrintWriter out;
	private BufferedReader in;
	public int myId;

	public GameLogic logic = new GameLogic();
	public InputHandler input = new InputHandler();
	private GamePanel panel;
	private javax.swing.Timer gameTimer;

	public enum GameState { TITLE, WAITING, PLAYING, ROUND_END_SELECT, ROUND_END_WAIT, COUNTDOWN, GAME_OVER }
	public GameState currentState = GameState.TITLE;

	public int selectedMapType = MapGenerator.MAP_TYPE_C;
	public int countdownTimer = 0;

	public BufferedImage imgPlayerMe, imgPlayerEnemy;

	public ActionClient() {
		loadImages();
		setupConnection(SERVER_IP, SERVER_PORT);

		setTitle("Action Game Client - Modular");
		setSize(800, 700);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setResizable(false);

		panel = new GamePanel(this, logic, input);
		add(panel);

		gameTimer = new javax.swing.Timer(1000 / FPS, e -> gameLoop());
		gameTimer.start();
		setVisible(true);
	}

	private void loadImages() {
		try {
			File f1 = new File(IMAGE_PATH_PLAYER_ME); if (f1.exists()) imgPlayerMe = ImageIO.read(f1);
			File f2 = new File(IMAGE_PATH_PLAYER_ENEMY); if (f2.exists()) imgPlayerEnemy = ImageIO.read(f2);
		} catch (Exception e) {}
	}

	private void setupConnection(String host, int port) {
		try {
			socket = new Socket(host, port);
			out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
			in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
			new Thread(this::receiveLoop).start();
		} catch (IOException e) {
			JOptionPane.showMessageDialog(this, "Connection failed: " + host + ":" + port);
			System.exit(0);
		}
	}

	private void gameLoop() {
		if (currentState == GameState.COUNTDOWN) {
			countdownTimer--;
			if (countdownTimer <= 0) currentState = GameState.PLAYING;
		} else if (currentState == GameState.PLAYING) {
			logic.update(myId, input, out);
		}
		panel.repaint();
	}

	public void joinGame() {
		out.println("JOIN " + TARGET_GAME_ID);
		currentState = GameState.WAITING;
	}

	public void startNewMatch() {
		logic.myWinCount = 0;
		logic.enemyWinCount = 0;
		int minId = Integer.MAX_VALUE;
		for(int id : logic.players.keySet()) minId = Math.min(minId, id);

		if (myId == minId) {
			logic.obstacles = MapGenerator.generate(selectedMapType);
			sendObstacleData();
		}
		startCountdown();
	}

	public void onPowerUpSelected() {
		if (logic.players.containsKey(myId)) logic.players.get(myId).sendStatus(out);
		out.println("NEXT_ROUND_READY " + myId);
		currentState = GameState.ROUND_END_WAIT;
	}

	public void backToTitle() {
		currentState = GameState.TITLE; logic.joinedPlayers.clear(); logic.players.clear();
		logic.myWinCount = 0; logic.enemyWinCount = 0; logic.resultMessage = "";
		logic.players.put(myId, new Player(myId, MAP_X + 100, MAP_Y + 200, COLOR_PLAYER_ME));
	}

	private void startCountdown() {
		logic.resetPositions(myId); currentState = GameState.COUNTDOWN; countdownTimer = COUNTDOWN_FRAMES;
		if (logic.players.containsKey(myId)) logic.players.get(myId).sendStatus(out);
	}

	private void receiveLoop() {
		try {
			String line;
			while ((line = in.readLine()) != null) {
				String[] tokens = line.split(" ");
				SwingUtilities.invokeLater(() -> processCommand(tokens[0], tokens));
			}
		} catch (Exception e) {}
	}

	private void processCommand(String cmd, String[] tokens) {
		try {
			if (cmd.equals("START")) {
				myId = Integer.parseInt(tokens[1]);
				logic.players.put(myId, new Player(myId, MAP_X + 100, MAP_Y + 200, COLOR_PLAYER_ME));
			} else if (cmd.equals("JOIN")) {
				int pid = Integer.parseInt(tokens[2]);
				logic.joinedPlayers.add(pid);
				if (!logic.players.containsKey(pid)) logic.players.put(pid, new Player(pid, 0, 0, COLOR_PLAYER_ENEMY));
				if (logic.joinedPlayers.size() >= 2 && currentState == GameState.WAITING) startNewMatch();
			} else if (cmd.equals("LEAVE")) {
				int leaveId = Integer.parseInt(tokens[1]);
				if (leaveId != myId) {
					setGameOver("完全勝利！(相手が退出しました)");
				}
			} else if (cmd.equals("MOVE")) {
				int id = Integer.parseInt(tokens[10]);
				if (id != myId) {
					Player p = logic.players.computeIfAbsent(id, k -> new Player(id, 0, 0, COLOR_PLAYER_ENEMY));
					p.x = Double.parseDouble(tokens[1]); p.y = Double.parseDouble(tokens[2]);
					p.angle = Double.parseDouble(tokens[3]); p.hp = Integer.parseInt(tokens[4]);
					p.weapon.isReloading = Boolean.parseBoolean(tokens[5]); p.weapon.reloadTimer = Integer.parseInt(tokens[6]);
					p.isGuarding = Boolean.parseBoolean(tokens[7]);
					p.guardCooldownTimer = Integer.parseInt(tokens[8]);
					p.invisibleTimer = Boolean.parseBoolean(tokens[9]) ? 10 : 0;
				}
			} else if (cmd.equals("STATUS")) {
				int id = Integer.parseInt(tokens[1]);
				Player p = logic.players.computeIfAbsent(id, k -> new Player(id, 0, 0, (id==myId)?COLOR_PLAYER_ME:COLOR_PLAYER_ENEMY));
				p.maxHp = Integer.parseInt(tokens[2]); p.size = Integer.parseInt(tokens[3]);
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
				int extraBounces = (tokens.length > 10) ? Integer.parseInt(tokens[10]) : 0;

				logic.spawnBullet(bId, x, y, angle, speed, damage, size, flags, ownerId, extraBounces);
			} else if (cmd.equals("BULLET_HIT")) {
				int targetBulletId = Integer.parseInt(tokens[1]);
				for (Bullet b : logic.bulletPool) {
					if (b.isActive && b.id == targetBulletId) {
						if (b.ownerId == myId) {
							Player me = logic.players.get(myId);
							if (me != null) {
								if (me.hasPassiveThirst) me.thirstTimer = PLAYER_THIRST_DURATION;
								if (me.hasPassiveConfidence) {
									if(me.confidenceTimer == 0) me.hp *= 3;
									me.confidenceTimer = PLAYER_CONFIDENCE_DURATION;
								}
							}
						}
						b.deactivate();
						break;
					}
				}
			} else if (cmd.equals("HEAL")) {
				int tid = Integer.parseInt(tokens[1]); int amount = Integer.parseInt(tokens[2]);
				if (logic.players.containsKey(tid)) { Player p = logic.players.get(tid); p.hp = Math.min(p.hp + amount, p.maxHp); }
			} else if (cmd.equals("DEAD")) {
				handleRoundEnd(Integer.parseInt(tokens[1]));
			} else if (cmd.equals("NEXT_ROUND_READY")) {
				if (currentState == GameState.ROUND_END_WAIT) startCountdown();
			} else if (cmd.equals("MAP_DATA")) {
				logic.obstacles.clear();
				for (int i = 1; i < tokens.length - 1; i += 4) {
					logic.obstacles.add(new Line2D.Double(Double.parseDouble(tokens[i]), Double.parseDouble(tokens[i+1]),
							Double.parseDouble(tokens[i+2]), Double.parseDouble(tokens[i+3])));
				}
				logic.resetPositions(myId);
			}
		} catch (Exception e) { e.printStackTrace(); }
	}

	private void handleRoundEnd(int deadId) {
		if (currentState != GameState.PLAYING) return;
		logic.isRoundWinner = (deadId != myId);
		if (logic.players.containsKey(deadId)) logic.players.get(deadId).hp = 0;
		if (logic.isRoundWinner) { logic.myWinCount++; logic.resultMessage = "ラウンド勝利！"; }
		else { logic.enemyWinCount++; logic.resultMessage = "ラウンド敗北..."; }

		if (logic.myWinCount >= MAX_WINS) setGameOver("完全勝利！おめでとう！");
		else if (logic.enemyWinCount >= MAX_WINS) setGameOver("完全敗北...ドンマイ！");
		else { logic.prepareNextRound(); currentState = (logic.isRoundWinner) ? GameState.ROUND_END_WAIT : GameState.ROUND_END_SELECT; }
	}

	private void setGameOver(String msg) { currentState = GameState.GAME_OVER; logic.resultMessage = msg; }

	private void sendObstacleData() {
		StringBuilder sb = new StringBuilder("MAP_DATA");
		for (Line2D.Double line : logic.obstacles) sb.append(" ").append((int)line.x1).append(" ").append((int)line.y1)
				.append(" ").append((int)line.x2).append(" ").append((int)line.y2);
		out.println(sb.toString());
	}

	public static void main(String[] args) { new ActionClient(); }
}