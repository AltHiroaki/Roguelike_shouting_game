package game;

import java.awt.geom.Line2D;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import static game.GameConstants.*;

/**
 * ゲームのロジック判定を行うクラス。
 * プレイヤーの更新、弾丸の管理、衝突判定（当たり判定）などを担当します。
 */
public class GameLogic {
	// スレッドセーフなマップでプレイヤーを管理
	public ConcurrentHashMap<Integer, Player> players = new ConcurrentHashMap<>();
	public Bullet[] bulletPool = new Bullet[MAX_BULLETS];
	public ArrayList<Line2D.Double> obstacles = new ArrayList<>();

	// 対戦参加中のプレイヤー（ゲーム画面にいる人）
	public Set<Integer> joinedPlayers = Collections.synchronizedSet(new HashSet<>());

	// サーバーに接続中の全プレイヤーID（ロビー含む）
	public Set<Integer> connectedPlayerIds = new HashSet<>();

	public int myWinCount = 0;
	public int enemyWinCount = 0;
	public boolean isRoundWinner = false;
	public String resultMessage = "";

	public ArrayList<PowerUp> presentedPowerUps = new ArrayList<>();
	private boolean wasMousePressed = false;
	public static int frameCount = 0;

	public GameLogic() {
		// オブジェクトプールパターンのため、弾丸インスタンスを事前生成
		for (int i = 0; i < MAX_BULLETS; i++) bulletPool[i] = new Bullet();
	}

	// 現在のホスト（IDが一番小さい人）を取得する
	public int getHostId() {
		if (connectedPlayerIds.isEmpty()) return 1; // 誰もいなければデフォルト1
		return Collections.min(connectedPlayerIds);
	}

	/**
	 * 毎フレーム呼ばれる更新処理。
	 */
	public void update(int myId, InputHandler input, PrintWriter out) {
		frameCount++;
		if (!players.containsKey(myId)) return;
		Player me = players.get(myId);

		// 右クリックでガード試行（テレポート安全化のため obstacles を渡す）
		if (input.isRightMousePressed) {
			boolean wasGuarding = me.isGuarding;
			me.tryGuard(obstacles);

			// ガード開始の瞬間、かつスキル持ちなら発動
			if (!wasGuarding && me.isGuarding && me.hasSkillTheWorld) {
				executeTheWorld(me);
			}
		}

		// プレイヤー自身の移動・更新
		me.update(input.keyW, input.keyS, input.keyA, input.keyD,
				input.mouseX, input.mouseY, obstacles, out);

		// 左クリックで射撃試行（押しっぱなし判定防止のためフラグ管理）
		if (input.isMousePressed && !wasMousePressed) {
			boolean wasGuarding = me.isGuarding;
			// 緊急防御スキル判定のため obstacles を渡す
			me.weapon.tryShoot(out, myId, obstacles);

			// 射撃時の緊急防御などでガードが発動した場合もチェック
			if (!wasGuarding && me.isGuarding && me.hasSkillTheWorld) {
				executeTheWorld(me);
			}
		}
		wasMousePressed = input.isMousePressed;

		// 弾丸の更新と衝突判定
		for (Bullet b : bulletPool) {
			if (!b.isActive) continue;
			b.update();
			checkBulletCollision(b, me, myId, out);
		}
	}

	/**
	 * "世界"スキル発動：一瞬だけ周囲の弾丸を消去する
	 */
	public void executeTheWorld(Player p) {
		for (Bullet b : bulletPool) {
			if (b.isActive) {
				// 距離チェック
				double dist = Math.sqrt(Math.pow(b.x - p.x, 2) + Math.pow(b.y - p.y, 2));
				if (dist < SKILL_THE_WORLD_RANGE) {
					// 弾を消す
					b.deactivate();
				}
			}
		}
	}

	/**
	 * 弾丸の衝突判定を行います。
	 * 壁との反射、プレイヤーへの命中などを処理します。
	 */
	private void checkBulletCollision(Bullet b, Player me, int myId, PrintWriter out) {
		boolean hitBoundary = false;

		// 画面端の判定
		if (b.x < MAP_X || b.x > MAP_X + MAP_WIDTH) {
			if (canBounce(b)) { b.angle = Math.PI - b.angle; b.bounceCount++; } else hitBoundary = true;
		}
		if (b.y < MAP_Y || b.y > MAP_Y + MAP_HEIGHT) {
			if (canBounce(b)) { b.angle = -b.angle; b.bounceCount++; } else hitBoundary = true;
		}

		// 壁(障害物)との判定
		// ゴースト弾でも一定時間経過したら壁判定を行う
		if (!hitBoundary && ((b.typeFlag & FLAG_GHOST) == 0 || b.lifeTimer > GHOST_VALID_TIME)) {

			for (Line2D.Double wall : obstacles) {
				if (wall.ptSegDist(b.x, b.y) < b.size) {
					if (canBounce(b)) {
						// 壁の「面」と「端(角)」を区別して反射方向を決定
						boolean isHorizontal = Math.abs(wall.y1 - wall.y2) < 1.0;

						if (isHorizontal) {
							// 横壁の場合
							double minX = Math.min(wall.x1, wall.x2);
							double maxX = Math.max(wall.x1, wall.x2);

							if (b.x >= minX && b.x <= maxX) {
								// 壁の側面に当たった -> Y軸反転
								b.angle = -b.angle;
							} else {
								// 壁の端（角）に当たった -> X軸反転
								b.angle = Math.PI - b.angle;
							}
						} else {
							// 縦壁の場合
							double minY = Math.min(wall.y1, wall.y2);
							double maxY = Math.max(wall.y1, wall.y2);

							if (b.y >= minY && b.y <= maxY) {
								// 壁の側面に当たった -> X軸反転
								b.angle = Math.PI - b.angle;
							} else {
								// 壁の端（角）に当たった -> Y軸反転
								b.angle = -b.angle;
							}
						}

						b.bounceCount++;
					} else hitBoundary = true;
					break;
				}
			}
		}

		if (hitBoundary) b.deactivate();

		// プレイヤーへのヒット判定
		// 自分の弾は一定時間(BULLET_SAFE_TIME)当たらず、その後当たるようになる
		if (b.isActive && (b.ownerId != myId || b.lifeTimer > BULLET_SAFE_TIME)) {
			if (me.getBounds().contains(b.x, b.y)) {

				int finalDamage = b.damage;

				// ビルドアップ: 常時30%カット
				if (me.hasPassiveBuildUp) {
					finalDamage = (int)(finalDamage * POWERUP_BUILDUP_DEFENSE_RATE);
				}

				// ガード時のダメージ計算
				if (me.isGuarding) {
					finalDamage = (int)(finalDamage * GUARD_DAMAGE_CUT_RATE);
					if (finalDamage < 1) finalDamage = 1;
				}

				// PassiveDelay (ダメージ分散) の処理
				if (me.hasPassiveDelay) {
					me.delayDamageBuffer += finalDamage;
					finalDamage = 0;
				}

				me.hp -= finalDamage;
				out.println("BULLET_HIT " + b.id);
				b.deactivate();

				// 弾の特殊効果適用
				if ((b.typeFlag & FLAG_POISON) != 0) {
					me.poisonTimer = PLAYER_POISON_DURATION; // タイマー更新
					me.poisonStack++; // スタック加算
				}
				if ((b.typeFlag & FLAG_COLD) != 0) me.coldTimer = PLAYER_COLD_DURATION;
				if ((b.typeFlag & FLAG_HILL) != 0) out.println("HEAL " + b.ownerId + " " + (b.damage/2));

				if (me.hp <= 0) {
					me.hp = 0;
					out.println("DEAD " + myId);
				}
			}
		}
	}

	/**
	 * 弾が反射可能か判定します。
	 */
	private boolean canBounce(Bullet b) {
		return (b.typeFlag & FLAG_BOUNCE) != 0 && b.bounceCount < b.maxBounces;
	}

	/**
	 * 弾丸プールから未使用の弾を探して発射（アクティブ化）します。
	 */
	public void spawnBullet(int id, double x, double y, double angle, double speed, int dmg, int size, int flags, int ownerId, int extraBounces) {
		// デフォルト寿命
		spawnBullet(id, x, y, angle, speed, dmg, size, flags, ownerId, extraBounces, BULLET_DEFAULT_LIFE);
	}

	public void spawnBullet(int id, double x, double y, double angle, double speed, int dmg, int size, int flags, int ownerId, int extraBounces, int maxLife) {
		for (Bullet b : bulletPool) {
			if (!b.isActive) {
				b.activate(id, x, y, angle, speed, dmg, size, flags, ownerId, maxLife);

				// 反射回数の設定
				if(extraBounces > 0) {
					b.typeFlag |= FLAG_BOUNCE;
					b.maxBounces = extraBounces;
				} else if ((flags & FLAG_BOUNCE) != 0) {
					b.maxBounces = 2; // デフォルト反射数
				}
				break;
			}
		}
	}

	/**
	 * ラウンド開始時に全プレイヤーの位置をリセットします。
	 */
	public void resetPositions(int myId) {
		int minId = Integer.MAX_VALUE;
		for(int id : players.keySet()) minId = Math.min(minId, id);
		for (Player p : players.values()) {
			// IDが小さい方が左側、大きい方が右側スタート
			if (p.id == minId) { p.x = MAP_X + 50; p.y = MAP_Y + 50; }
			else { p.x = MAP_X + MAP_WIDTH - 50; p.y = MAP_Y + MAP_HEIGHT - 50; }
			p.resetForRound();
		}
	}

	/**
	 * 次のラウンドの準備。弾を消去し、敗者にはパワーアップを提示します。
	 */
	public void prepareNextRound() {
		for(Bullet b : bulletPool) b.deactivate();
		if (!isRoundWinner) {
			presentedPowerUps = PowerUpFactory.getRandomPowerUps(3);
		}
	}

	/**
	 * ゲーム終了時やタイトルに戻る際に、ゲームの状態を完全にリセットします。
	 */
	public void resetGame() {
		// 1. 弾丸の全消去
		for(Bullet b : bulletPool) b.deactivate();

		// 2. プレイヤー情報のクリア
		players.clear();
		joinedPlayers.clear();

		// 注意: connectedPlayerIds (接続リスト) はここではクリアしない
		// (タイトル画面に戻ってもサーバーには繋がっているため)

		// 3. スコア・障害物のリセット
		myWinCount = 0;
		enemyWinCount = 0;
		resultMessage = "";
		obstacles.clear();
		presentedPowerUps.clear();
	}
}