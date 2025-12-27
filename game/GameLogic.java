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

		// 修正: obstaclesを渡す
		if (input.isRightMousePressed) me.tryGuard(obstacles);

		me.update(input.keyW, input.keyS, input.keyA, input.keyD,
				input.mouseX, input.mouseY, obstacles, out);

		if (input.isMousePressed && !wasMousePressed) {
			// 修正: obstaclesを渡す
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

		if (b.x < MAP_X || b.x > MAP_X + MAP_WIDTH) {
			if (canBounce(b)) { b.angle = Math.PI - b.angle; b.bounceCount++; } else hitBoundary = true;
		}
		if (b.y < MAP_Y || b.y > MAP_Y + MAP_HEIGHT) {
			if (canBounce(b)) { b.angle = -b.angle; b.bounceCount++; } else hitBoundary = true;
		}

		if (!hitBoundary && (b.typeFlag & FLAG_GHOST) == 0) {
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