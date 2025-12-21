package game;

import java.awt.geom.Line2D;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import static game.GameConstants.*;

public class GameLogic {
	// ゲームデータ
	public ConcurrentHashMap<Integer, Player> players = new ConcurrentHashMap<>();
	public Bullet[] bulletPool = new Bullet[MAX_BULLETS];
	public ArrayList<Line2D.Double> obstacles = new ArrayList<>();
	public Set<Integer> joinedPlayers = Collections.synchronizedSet(new HashSet<>());

	// 勝敗・スコア
	public int myWinCount = 0;
	public int enemyWinCount = 0;
	public boolean isRoundWinner = false;
	public String resultMessage = "";

	// パワーアップ関連
	public ArrayList<PowerUp> presentedPowerUps = new ArrayList<>();

	// ★追加: 前回のマウス状態を記録するフラグ
	private boolean wasMousePressed = false;

	public GameLogic() {
		for (int i = 0; i < MAX_BULLETS; i++) bulletPool[i] = new Bullet();
	}

	// ゲームのメイン更新処理
	public void update(int myId, InputHandler input, PrintWriter out) {
		if (!players.containsKey(myId)) return;
		Player me = players.get(myId);

		// プレイヤーの移動更新
		me.update(input.keyW, input.keyS, input.keyA, input.keyD,
				input.mouseX, input.mouseY, obstacles, out);

		// ★修正: 長押し連射を防止（クリックした瞬間だけ反応）
		if (input.isMousePressed && !wasMousePressed) {
			me.weapon.tryShoot(out, myId);
		}
		// 現在の状態を記録（次のフレームでの比較用）
		wasMousePressed = input.isMousePressed;

		// 弾の移動と当たり判定
		for (Bullet b : bulletPool) {
			if (!b.isActive) continue;
			b.update();
			checkBulletCollision(b, me, myId, out);
		}
	}

	private void checkBulletCollision(Bullet b, Player me, int myId, PrintWriter out) {
		boolean hitBoundary = false;
		if (b.x < MAP_X || b.x > MAP_X + MAP_WIDTH) {
			if (canBounce(b)) { b.angle = Math.PI - b.angle; b.bounceCount++; } else hitBoundary = true;
		}
		if (b.y < MAP_Y || b.y > MAP_Y + MAP_HEIGHT) {
			if (canBounce(b)) { b.angle = -b.angle; b.bounceCount++; } else hitBoundary = true;
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

		if (b.isActive && (b.ownerId != myId || b.lifeTimer > BULLET_SAFE_TIME)) {
			if (me.getBounds().contains(b.x, b.y)) {
				me.hp -= b.damage;
				out.println("BULLET_HIT " + b.id);
				b.deactivate();

				if ((b.typeFlag & FLAG_POISON) != 0) me.poisonTimer = PLAYER_POISON_DURATION;
				if ((b.typeFlag & FLAG_HILL) != 0) out.println("HEAL " + b.ownerId + " " + PLAYER_HEAL_AMOUNT);

				if (me.hp <= 0) {
					me.hp = 0;
					out.println("DEAD " + myId);
				}
			}
		}
	}

	private boolean canBounce(Bullet b) { return (b.typeFlag & FLAG_BOUNCE) != 0 && b.bounceCount < 1; }

	public void spawnBullet(int id, double x, double y, double angle, double speed, int dmg, int size, int flags, int ownerId) {
		for (Bullet b : bulletPool) {
			if (!b.isActive) {
				b.activate(id, x, y, angle, speed, dmg, size, flags, ownerId);
				break;
			}
		}
	}

	public void resetPositions(int myId) {
		// IDが一番小さい人を特定（ホスト判定用）
		int minId = Integer.MAX_VALUE;
		for(int id : players.keySet()) minId = Math.min(minId, id);

		// ★修正: 「自分(me)」だけでなく「全員(players.values())」の位置をリセットする
		for (Player p : players.values()) {
			if (p.id == minId) {
				// ホスト（Player1）の位置
				p.x = MAP_X + 50;
				p.y = MAP_Y + 50;
			} else {
				// ゲスト（Player2）の位置
				p.x = MAP_X + MAP_WIDTH - 50;
				p.y = MAP_Y + MAP_HEIGHT - 50;
			}
			// HP や武器の状態も全員リセットしておく
			p.resetForRound();
		}
	}

	public void prepareNextRound() {
		for(Bullet b : bulletPool) b.deactivate();
		if (!isRoundWinner) {
			presentedPowerUps = PowerUpFactory.getRandomPowerUps(3);
		}
	}
}