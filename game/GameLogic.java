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
	public ConcurrentHashMap<Integer, Player> players = new ConcurrentHashMap<>();
	public Bullet[] bulletPool = new Bullet[MAX_BULLETS];
	public ArrayList<Line2D.Double> obstacles = new ArrayList<>();
	public Set<Integer> joinedPlayers = Collections.synchronizedSet(new HashSet<>());

	public int myWinCount = 0;
	public int enemyWinCount = 0;
	public boolean isRoundWinner = false;
	public String resultMessage = "";

	public ArrayList<PowerUp> presentedPowerUps = new ArrayList<>();
	private boolean wasMousePressed = false;
	public static int frameCount = 0;

	public GameLogic() {
		for (int i = 0; i < MAX_BULLETS; i++) bulletPool[i] = new Bullet();
	}

	public void update(int myId, InputHandler input, PrintWriter out) {
		frameCount++;
		if (!players.containsKey(myId)) return;
		Player me = players.get(myId);

		// 修正: テレポート安全化のため obstacles を渡す
		if (input.isRightMousePressed) me.tryGuard(obstacles);

		me.update(input.keyW, input.keyS, input.keyA, input.keyD,
				input.mouseX, input.mouseY, obstacles, out);

		if (input.isMousePressed && !wasMousePressed) {
			// 修正: 緊急防御スキル判定のため obstacles を渡す
			me.weapon.tryShoot(out, myId, obstacles);
		}
		wasMousePressed = input.isMousePressed;

		for (Bullet b : bulletPool) {
			if (!b.isActive) continue;
			b.update();
			checkBulletCollision(b, me, myId, out);
		}
	}

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
						// 壁の「面」と「端(角)」を区別して反射方向を決める

						// 壁が水平(横向き)かどうか
						boolean isHorizontal = Math.abs(wall.y1 - wall.y2) < 1.0;

						if (isHorizontal) {
							// 横壁の場合
							double minX = Math.min(wall.x1, wall.x2);
							double maxX = Math.max(wall.x1, wall.x2);

							// 弾のX座標が壁の範囲内なら「側面(上下)」に当たった -> Y反転 (-angle)
							if (b.x >= minX && b.x <= maxX) {
								b.angle = -b.angle;
							} else {
								// 範囲外なら「端(左右)」に当たった -> X反転 (PI - angle)
								b.angle = Math.PI - b.angle;
							}
						} else {
							// 縦壁の場合
							double minY = Math.min(wall.y1, wall.y2);
							double maxY = Math.max(wall.y1, wall.y2);

							// 弾のY座標が壁の範囲内なら「側面(左右)」に当たった -> X反転 (PI - angle)
							if (b.y >= minY && b.y <= maxY) {
								b.angle = Math.PI - b.angle;
							} else {
								// 範囲外なら「端(上下)」に当たった -> Y反転 (-angle)
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
		if (b.isActive && (b.ownerId != myId || b.lifeTimer > BULLET_SAFE_TIME)) {
			if (me.getBounds().contains(b.x, b.y)) {

				int finalDamage = b.damage;
				if (me.isGuarding) {
					finalDamage = (int)(finalDamage * GUARD_DAMAGE_CUT_RATE);
					if (finalDamage < 1) finalDamage = 1;
				}

				if (me.hasPassiveDelay) {
					me.delayDamageBuffer += finalDamage;
					finalDamage = 0;
				}

				me.hp -= finalDamage;
				out.println("BULLET_HIT " + b.id);
				b.deactivate();

				if ((b.typeFlag & FLAG_POISON) != 0) me.poisonTimer = PLAYER_POISON_DURATION;
				if ((b.typeFlag & FLAG_COLD) != 0) me.coldTimer = PLAYER_COLD_DURATION;
				if ((b.typeFlag & FLAG_HILL) != 0) out.println("HEAL " + b.ownerId + " " + (b.damage/2));

				if (me.hp <= 0) {
					me.hp = 0;
					out.println("DEAD " + myId);
				}
			}
		}
	}

	private boolean canBounce(Bullet b) {
		return (b.typeFlag & FLAG_BOUNCE) != 0 && b.bounceCount < b.maxBounces;
	}

	public void spawnBullet(int id, double x, double y, double angle, double speed, int dmg, int size, int flags, int ownerId, int extraBounces) {
		for (Bullet b : bulletPool) {
			if (!b.isActive) {
				b.activate(id, x, y, angle, speed, dmg, size, flags, ownerId);

				// // 反射回数の設定ロジック修正
				if(extraBounces > 0) {
					b.typeFlag |= FLAG_BOUNCE;
					b.maxBounces = extraBounces;
				} else if ((flags & FLAG_BOUNCE) != 0) {
					b.maxBounces = 2;
				}
				break;
			}
		}
	}

	public void resetPositions(int myId) {
		int minId = Integer.MAX_VALUE;
		for(int id : players.keySet()) minId = Math.min(minId, id);
		for (Player p : players.values()) {
			if (p.id == minId) { p.x = MAP_X + 50; p.y = MAP_Y + 50; }
			else { p.x = MAP_X + MAP_WIDTH - 50; p.y = MAP_Y + MAP_HEIGHT - 50; }
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