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

/**
 * ゲームのクライアントメインクラス。
 * ウィンドウの生成、サーバーとの通信、ゲームループの管理を行います。
 */
public class ActionClient extends JFrame {
	// --- 通信関連 ---
	private Socket socket;
	private PrintWriter out;
	private BufferedReader in;
	public int myId;

	// --- ゲームロジックコンポーネント ---
	public GameLogic logic = new GameLogic();
	public InputHandler input = new InputHandler();
	private GamePanel panel;
	private javax.swing.Timer gameTimer;

	// --- ゲーム進行状態 ---
	// ABILITY_INFO, CONTROLS_INFO を追加
	public enum GameState { TITLE, ABILITY_INFO, CONTROLS_INFO, WAITING, PLAYING, ROUND_END_SELECT, ROUND_END_WAIT, COUNTDOWN, GAME_OVER }
	public GameState currentState = GameState.TITLE;

	public int selectedMapType = MapGenerator.MAP_TYPE_C;
	public int countdownTimer = 0;

	// --- リソース ---
	public BufferedImage imgPlayerMe, imgPlayerEnemy;

	/**
	 * コンストラクタ。
	 * 画像のロード、接続、ウィンドウ設定、ループの開始を行います。
	 */
	public ActionClient() {
		loadImages();
		setupConnection(SERVER_IP, SERVER_PORT);

		setTitle("Action Game Client");
		setSize(1060, 790);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setResizable(false);

		panel = new GamePanel(this, logic, input);
		add(panel);

		// ゲームループ (60FPS)
		gameTimer = new javax.swing.Timer(1000 / FPS, e -> gameLoop());
		gameTimer.start();
		setVisible(true);
	}

	/**
	 * プレイヤー画像をファイルから読み込みます。
	 */
	private void loadImages() {
		try {
			File f1 = new File(IMAGE_PATH_PLAYER_ME); if (f1.exists()) imgPlayerMe = ImageIO.read(f1);
			File f2 = new File(IMAGE_PATH_PLAYER_ENEMY); if (f2.exists()) imgPlayerEnemy = ImageIO.read(f2);
		} catch (Exception e) {
			System.out.println("画像読み込みエラー (無視して続行): " + e.getMessage());
		}
	}

	/**
	 * サーバーへのソケット接続を確立し、受信スレッドを開始します。
	 */
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

	/**
	 * メインゲームループ。
	 * タイマーにより定期的に呼び出され、ロジックの更新と画面の再描画を行います。
	 */
	private void gameLoop() {
		if (currentState == GameState.COUNTDOWN) {
			countdownTimer--;
			if (countdownTimer <= 0) currentState = GameState.PLAYING;
		} else if (currentState == GameState.PLAYING) {
			logic.update(myId, input, out);
		}
		panel.repaint();
	}

	/**
	 * ゲームに参加リクエストを送信します。
	 */
	public void joinGame() {
		out.println("JOIN " + TARGET_GAME_ID);
		currentState = GameState.WAITING;
	}

	/**
	 * 新しいマッチ（またはラウンド）を開始する準備を行います。
	 * マップ生成権限を持つ場合（ID最小値）はマップデータを送信します。
	 */
	public void startNewMatch() {
		logic.myWinCount = 0;
		logic.enemyWinCount = 0;
		int minId = Integer.MAX_VALUE;
		for(int id : logic.players.keySet()) minId = Math.min(minId, id);

		// IDが一番小さいプレイヤーがホスト役としてマップを生成
		// 注意: ここは「対戦参加者の中での最小ID」なので connectedPlayerIds ではなく players.keySet() を使う
		if (myId == minId) {
			logic.obstacles = MapGenerator.generate(selectedMapType);
			sendObstacleData();
		}
		startCountdown();
	}

	/**
	 * パワーアップ選択後に呼ばれます。次のラウンドの準備完了をサーバーへ通知します。
	 * 引数に取得した能力名を追加
	 */
	public void onPowerUpSelected(String abilityName) {
		// 能力名だけ送る（例: "ABILITY Hill"）
		// サーバーがこれを "ABILITY Hill <myId>" に変換して全員に配る
		out.println("ABILITY " + abilityName);

		if (logic.players.containsKey(myId)) logic.players.get(myId).sendStatus(out);
		out.println("NEXT_ROUND_READY " + myId);
		currentState = GameState.ROUND_END_WAIT;
	}

	/**
	 * ゲームオーバー後、タイトル画面に戻る処理です。
	 * resetGame()を呼び出して弾丸なども全て消去します。
	 */
	public void backToTitle() {
		currentState = GameState.TITLE;
		// ゲームロジック側で一括リセット（弾丸、スコア、プレイヤー等）
		logic.resetGame();

		// 自分のプレイヤーオブジェクトを再生成して初期位置へ配置
		logic.players.put(myId, new Player(myId, MAP_X + 100, MAP_Y + 200, COLOR_PLAYER_ME));
	}

	private void startCountdown() {
		logic.resetPositions(myId);
		currentState = GameState.COUNTDOWN;
		countdownTimer = COUNTDOWN_FRAMES;
		// ステータス（最大HPなど）を更新して送信
		if (logic.players.containsKey(myId)) logic.players.get(myId).sendStatus(out);
	}

	/**
	 * サーバーからのメッセージ受信ループ。
	 * 受信したコマンドはEvent Dispatch Threadで処理されます。
	 */
	private void receiveLoop() {
		try {
			String line;
			while ((line = in.readLine()) != null) {
				String[] tokens = line.split(" ");
				SwingUtilities.invokeLater(() -> processCommand(tokens[0], tokens));
			}
		} catch (Exception e) {}
	}

	/**
	 * 受信したコマンド文字列を解析し、適切な処理を実行します。
	 * @param cmd コマンド名
	 * @param tokens 分割された引数配列
	 */
	private void processCommand(String cmd, String[] tokens) {
		try {
			if (cmd.equals("START")) {
				// 初回接続時、自分のIDを受信
				myId = Integer.parseInt(tokens[1]);
				logic.players.put(myId, new Player(myId, MAP_X + 100, MAP_Y + 200, COLOR_PLAYER_ME));
				// 自分を接続リストに追加
				logic.connectedPlayerIds.add(myId);
			} else if (cmd.equals("ENTER")) {
				// 追加: 誰かが接続した
				int pid = Integer.parseInt(tokens[1]);
				logic.connectedPlayerIds.add(pid);
			} else if (cmd.equals("USERS")) {
				// 既に参加しているユーザーの一覧
				for(int i=1; i<tokens.length; i++) {
					logic.connectedPlayerIds.add(Integer.parseInt(tokens[i]));
				}
			} else if (cmd.equals("JOIN")) {
				// 他プレイヤーの参加通知 (対戦参加)
				int pid = Integer.parseInt(tokens[2]);
				logic.joinedPlayers.add(pid);
				if (!logic.players.containsKey(pid)) logic.players.put(pid, new Player(pid, 0, 0, COLOR_PLAYER_ENEMY));
				// 2人揃ったら開始
				if (logic.joinedPlayers.size() >= 2 && currentState == GameState.WAITING) startNewMatch();
			} else if (cmd.equals("LEAVE")) {
				// 切断検知
				int leaveId = Integer.parseInt(tokens[1]);
				// 接続リストから削除
				logic.connectedPlayerIds.remove(leaveId);

				// ゲーム中に対戦相手が落ちた場合のみ勝利判定
				if (currentState == GameState.PLAYING || currentState == GameState.ROUND_END_SELECT || currentState == GameState.ROUND_END_WAIT) {
					if (leaveId != myId && logic.joinedPlayers.contains(leaveId)) {
						setGameOver("完全勝利！(相手が退出しました)");
					}
				}
			} else if (cmd.equals("MOVE")) {
				// トークン構成: [0]MOVE [1]x [2]y [3]angle [4]hp [5]reloadTimer [6]guardTimer [7]flags [8]id

				int id = Integer.parseInt(tokens[8]);

				if (id != myId) {
					Player p = logic.players.computeIfAbsent(id, k -> new Player(id, 0, 0, COLOR_PLAYER_ENEMY));

					// 座標・角度・HP
					p.x = Double.parseDouble(tokens[1]);
					p.y = Double.parseDouble(tokens[2]);
					p.angle = Double.parseDouble(tokens[3]);
					p.hp = Integer.parseInt(tokens[4]);

					// タイマー類
					p.weapon.reloadTimer = Integer.parseInt(tokens[5]);
					p.guardCooldownTimer = Integer.parseInt(tokens[6]);

					// フラグの展開 (ビット演算で復元)
					int flags = Integer.parseInt(tokens[7]);
					p.weapon.isReloading = (flags & P_FLAG_RELOAD) != 0;
					p.isGuarding         = (flags & P_FLAG_GUARD) != 0;
					boolean isInvisible  = (flags & P_FLAG_INVISIBLE) != 0;
					boolean isPoisoned   = (flags & P_FLAG_POISON) != 0;

					// 透明化タイマーの簡易設定 (表示用なので0か正の値があればOK)
					p.invisibleTimer = isInvisible ? 10 : 0;
					// 毒タイマーの簡易設定
					p.poisonTimer = isPoisoned ? 10 : 0;

					// "世界"の発動フラグチェック
					boolean triggeredTheWorld = (flags & P_FLAG_THE_WORLD) != 0;
					if (triggeredTheWorld) {
						// 相手が発動したので、自分の画面の弾も消去
						logic.executeTheWorld(p);
					}
				}
			} else if (cmd.equals("STATUS")) {
				// STATUS id maxHp size reloadDuration maxGuardCooldown
				int id = Integer.parseInt(tokens[1]);
				Player p = logic.players.computeIfAbsent(id, k -> new Player(id, 0, 0, (id==myId)?COLOR_PLAYER_ME:COLOR_PLAYER_ENEMY));

				p.maxHp = Integer.parseInt(tokens[2]);
				p.size = Integer.parseInt(tokens[3]);

				// リロード時間の同期
				if(tokens.length > 4) {
					p.weapon.reloadDuration = Integer.parseInt(tokens[4]);
				}

				// 【追加】最大ガードクールダウンの同期
				if(tokens.length > 5) {
					p.currentMaxGuardCooldown = Integer.parseInt(tokens[5]);
				}

			} else if (cmd.equals("SHOT")) {
				// 弾の発射情報受信
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
				int maxLife = (tokens.length > 11) ? Integer.parseInt(tokens[11]) : BULLET_DEFAULT_LIFE;

				logic.spawnBullet(bId, x, y, angle, speed, damage, size, flags, ownerId, extraBounces, maxLife);
			} else if (cmd.equals("BULLET_HIT")) {
				// 弾の命中通知。自分が撃った弾ならパッシブ効果を発動
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
				// 回復処理
				int tid = Integer.parseInt(tokens[1]); int amount = Integer.parseInt(tokens[2]);
				if (logic.players.containsKey(tid)) { Player p = logic.players.get(tid); p.hp = Math.min(p.hp + amount, p.maxHp); }
			} else if (cmd.equals("DEAD")) {
				// プレイヤー死亡通知 -> ラウンド終了処理
				handleRoundEnd(Integer.parseInt(tokens[1]));
			} else if (cmd.equals("NEXT_ROUND_READY")) {
				// 相手の準備完了通知
				if (currentState == GameState.ROUND_END_WAIT) startCountdown();
			} else if (cmd.equals("MAP_DATA")) {
				// マップデータの受信
				logic.obstacles.clear();
				for (int i = 1; i < tokens.length - 1; i += 4) {
					logic.obstacles.add(new Line2D.Double(Double.parseDouble(tokens[i]), Double.parseDouble(tokens[i+1]),
							Double.parseDouble(tokens[i+2]), Double.parseDouble(tokens[i+3])));
				}
				logic.resetPositions(myId);
			} else if (cmd.equals("ABILITY")) {
				// 修正: スペースを含む能力名("Self Regen"など)に対応
				// サーバーからの形式: ABILITY <Name Part1> <Name Part2> ... <SenderID>
				// 必ず最後のトークンがID

				if (tokens.length >= 3) {
					int idIndex = tokens.length - 1; // 最後のインデックス
					int pid = Integer.parseInt(tokens[idIndex]); // ID取得

					// 名前部分は index 1 から idIndex-1 まで
					StringBuilder sb = new StringBuilder();
					for(int i=1; i<idIndex; i++) {
						sb.append(tokens[i]);
						if(i < idIndex - 1) sb.append(" "); // 間にスペースを入れる
					}
					String aName = sb.toString();

					if (logic.players.containsKey(pid)) {
						logic.players.get(pid).abilityNames.add(aName);
					}
				}
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
		else {
			logic.prepareNextRound();
			// 勝者は待機、敗者はパワーアップ選択へ
			currentState = (logic.isRoundWinner) ? GameState.ROUND_END_WAIT : GameState.ROUND_END_SELECT;
		}
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