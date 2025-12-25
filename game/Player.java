package game;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.io.PrintWriter;
import static game.GameConstants.*;

public class Player {
	public int id;
	public double x, y, angle;
	public int hp = PLAYER_MAX_HP;
	public int maxHp = PLAYER_MAX_HP;
	public Color color;
	public double speed = PLAYER_SPEED;
	public int size = PLAYER_SIZE;

	public int poisonTimer = 0;
	public int coldTimer = 0;
	public int thirstTimer = 0;
	public int confidenceTimer = 0;

	public boolean isGuarding = false;
	public int guardTimer = 0;
	public int guardCooldownTimer = 0;

	public boolean hasSkillTacticalReload = false;
	public boolean hasSkillExclusiveDefense = false;
	public boolean hasSkillInvisible = false;
	public boolean hasSkillEmergencyDefense = false;
	public boolean hasSkillTeleport = false;

	public boolean hasPassiveThirst = false;
	public boolean hasPassiveDelay = false;
	public boolean hasPassiveConfidence = false;

	public double delayDamageBuffer = 0;
	public int invisibleTimer = 0;
	public int exclusiveDefenseTimer = 0;

	public Weapon weapon;

	public Player(int id, double x, double y, Color c) {
		this.id = id;
		this.x = x;
		this.y = y;
		this.color = c;
		this.weapon = new Weapon(this);
	}

	public void resetForRound() {
		hp = maxHp;
		poisonTimer = 0;
		coldTimer = 0;
		thirstTimer = 0;
		confidenceTimer = 0;
		isGuarding = false;
		guardTimer = 0;
		guardCooldownTimer = 0;
		delayDamageBuffer = 0;
		invisibleTimer = 0;
		exclusiveDefenseTimer = 0;
		weapon.reset();
	}

	public void applyPowerUpStats() {
		maxHp = PLAYER_MAX_HP;
		size = PLAYER_SIZE;
		speed = PLAYER_SPEED;
		for(WeaponEffect e : weapon.effects) {
			if(e instanceof EffectBigBoy) { maxHp += 50; size *= 2; }
			if(e instanceof EffectSmallBoy) { maxHp = 50; size /= 2; speed *= 1.3; }
			if(e instanceof EffectIdaten) { maxHp = (int)(maxHp * 0.8); speed *= 2.0; }
		}
		if(hasSkillExclusiveDefense) maxHp = (int)(maxHp * 1.3);
		if(hp > maxHp) hp = maxHp;
	}

	public void tryGuard() {
		if (guardCooldownTimer <= 0 && !isGuarding) {
			isGuarding = true;
			guardTimer = GUARD_DURATION;
			int cooldownAdd = 0;

			if (hasSkillTacticalReload) {
				weapon.currentAmmo = weapon.maxAmmo;
				cooldownAdd += 120;
			}
			if (hasSkillExclusiveDefense) {
				exclusiveDefenseTimer = 120;
				cooldownAdd += 120;
			}
			if (hasSkillInvisible) {
				invisibleTimer = 30;
				cooldownAdd += 300;
			}
			if (hasSkillTeleport) {
				teleport();
				cooldownAdd += 120;
			}

			guardCooldownTimer = GUARD_COOLDOWN + cooldownAdd;
		}
	}

	public void forceGuard() {
		if (isGuarding) return;

		isGuarding = true;
		guardTimer = GUARD_DURATION;

		int cooldownAdd = 0;
		if (hasSkillTacticalReload) {
			weapon.currentAmmo = weapon.maxAmmo;
			cooldownAdd += 120;
		}
		if (hasSkillExclusiveDefense) {
			exclusiveDefenseTimer = 120;
			cooldownAdd += 120;
		}
		if (hasSkillInvisible) {
			invisibleTimer = 30;
			cooldownAdd += 300;
		}
		if (hasSkillTeleport) {
			teleport();
			cooldownAdd += 120;
		}

		guardCooldownTimer = GUARD_COOLDOWN + cooldownAdd;
	}

	private void teleport() {
		double dist = 150.0;
		double tx = x + Math.cos(angle) * dist;
		double ty = y + Math.sin(angle) * dist;
		if (tx < MAP_X) tx = MAP_X + 10;
		if (tx > MAP_X + MAP_WIDTH) tx = MAP_X + MAP_WIDTH - 10;
		if (ty < MAP_Y) ty = MAP_Y + 10;
		if (ty > MAP_Y + MAP_HEIGHT) ty = MAP_Y + MAP_HEIGHT - 10;
		x = tx; y = ty;
	}

	public void update(boolean keyW, boolean keyS, boolean keyA, boolean keyD, int mx, int my, ArrayList<Line2D.Double> obstacles, PrintWriter out) {
		if (isGuarding) {
			guardTimer--;
			if (guardTimer <= 0) isGuarding = false;
		}
		if (guardCooldownTimer > 0) guardCooldownTimer--;

		if (exclusiveDefenseTimer > 0) {
			exclusiveDefenseTimer--;
			if (exclusiveDefenseTimer == 0) {
				isGuarding = true;
				guardTimer = GUARD_DURATION;
			}
		}

		if (invisibleTimer > 0) invisibleTimer--;

		// 修正: ディレイダメージの処理
		// 固定値(-1)ではなく、バッファ残量に応じたダメージ(-10% + 1)を与えることで、
		// ダメージが蓄積するほど減るペースが速くなります（要望の動作を実現）
		if (hasPassiveDelay && delayDamageBuffer > 0) {
			if(hp > 0 && GameLogic.frameCount % 20 == 0) { // 頻度調整 (例: 0.3秒ごと)
				int bleed = (int)(delayDamageBuffer * 0.1) + 1;
				hp -= bleed;
				delayDamageBuffer -= bleed;
				if(delayDamageBuffer < 0) delayDamageBuffer = 0;
			}
		}

		if (poisonTimer > 0) {
			if(GameLogic.frameCount % 30 == 0) hp--;
			poisonTimer--;
		}
		if (coldTimer > 0) coldTimer--;
		if (thirstTimer > 0) thirstTimer--;
		if (confidenceTimer > 0) {
			confidenceTimer--;
			if(confidenceTimer == 0) {
				hp = (int)Math.ceil(hp / 3.0);
			}
		}

		double currentSpeed = speed;
		if (coldTimer > 0) currentSpeed *= 0.5;
		if (thirstTimer > 0) currentSpeed *= 1.3;

		double nextX = x, nextY = y;
		if (keyA) nextX -= currentSpeed;
		if (keyD) nextX += currentSpeed;
		if (!checkWall(nextX, y, obstacles)) x = nextX;

		if (keyW) nextY -= currentSpeed;
		if (keyS) nextY += currentSpeed;
		if (!checkWall(x, nextY, obstacles)) y = nextY;

		angle = Math.atan2(my - y, mx - x);

		out.println("MOVE " + (int)x + " " + (int)y + " " + angle + " " + hp
				+ " " + weapon.isReloading + " " + weapon.reloadTimer
				+ " " + isGuarding + " " + guardCooldownTimer + " " + (invisibleTimer > 0) + " " + id);

		weapon.update(out, id);
	}

	private boolean checkWall(double tx, double ty, ArrayList<Line2D.Double> walls) {
		if (tx < MAP_X + size || tx > MAP_X + MAP_WIDTH - size) return true;
		if (ty < MAP_Y + size || ty > MAP_Y + MAP_HEIGHT - size) return true;
		for (Line2D.Double w : walls) if (w.ptSegDist(tx, ty) < size) return true;
		return false;
	}

	public Rectangle getBounds() {
		return new Rectangle((int)x - size, (int)y - size, size * 2, size * 2);
	}

	public void draw(Graphics2D g2d, BufferedImage imgMe, BufferedImage imgEnemy, int myId) {
		if (id != myId && invisibleTimer > 0) return;

		AffineTransform old = g2d.getTransform();
		g2d.translate(x, y);

		// HPバー
		g2d.setColor(Color.RED); g2d.fillRect(-20, UI_BAR_HP_Y_OFFSET, 40, UI_BAR_HEIGHT);
		g2d.setColor(Color.GREEN);
		int barWidth = (int)(40 * (hp / (double)maxHp));
		if (barWidth > 40) barWidth = 40; if (barWidth < 0) barWidth = 0;
		g2d.fillRect(-20, UI_BAR_HP_Y_OFFSET, barWidth, UI_BAR_HEIGHT);

		// 修正: 状態異常を文字ではなく「靄」で表現 (本体の描画後に被せるため、ここでは描画せず後述するか、下地として描く)
		// ここでは一旦保留し、本体描画の前後に処理します

		// リロードバー
		if (weapon.isReloading) {
			g2d.setColor(Color.GRAY); g2d.fillRect(-20, UI_BAR_RELOAD_Y_OFFSET, 40, UI_BAR_HEIGHT);
			g2d.setColor(Color.YELLOW);
			double progress = (double)weapon.reloadTimer / weapon.reloadDuration;
			// 修正: 相手側のリロード時間がずれている場合にバーが突き抜けるのを防ぐ (最大1.0でクリップ)
			if (progress > 1.0) progress = 1.0;
			g2d.fillRect(-20, UI_BAR_RELOAD_Y_OFFSET, (int)(40 * progress), UI_BAR_HEIGHT);
		}

		// 修正: ガードクールダウンバーを「自分以外」にも表示するよう id check を削除
		if (guardCooldownTimer > 0) {
			g2d.setColor(Color.GRAY);
			g2d.fillRect(-20, UI_BAR_GUARD_Y_OFFSET, 40, UI_BAR_HEIGHT);
			g2d.setColor(COLOR_GUARD_COOLDOWN);
			double progress = 1.0 - ((double)guardCooldownTimer / GUARD_COOLDOWN);
			if (progress < 0) progress = 0; // 安全策
			g2d.fillRect(-20, UI_BAR_GUARD_Y_OFFSET, (int)(40 * progress), UI_BAR_HEIGHT);
		}

		if (isGuarding) {
			g2d.setColor(COLOR_GUARD_SHIELD);
			g2d.fillOval(-size - 5, -size - 5, (size * 2) + 10, (size * 2) + 10);
		}

		g2d.rotate(angle);

		// 本体描画
		BufferedImage img = (id == myId) ? imgMe : imgEnemy;
		if (img != null) {
			if(invisibleTimer > 0) {
				Composite c = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f);
				g2d.setComposite(c);
				g2d.drawImage(img, -size, -size, size * 2, size * 2, null);
				g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
			} else {
				g2d.drawImage(img, -size, -size, size * 2, size * 2, null);
			}
		} else {
			g2d.setColor(color);
			g2d.fillOval(-size, -size, size * 2, size * 2);
			g2d.setColor(Color.BLACK);
			g2d.drawLine(0, 0, size + 10, 0);
		}

		if (poisonTimer > 0) {
			g2d.setColor(new Color(128, 0, 128, 100)); // 半透明の紫
			g2d.fillOval(-size, -size, size * 2, size * 2);
		}
		if (coldTimer > 0) {
			g2d.setColor(new Color(0, 255, 255, 100)); // 半透明のシアン
			g2d.fillOval(-size, -size, size * 2, size * 2);
		}

		g2d.setTransform(old);
	}

	public void sendStatus(PrintWriter out) {
		out.println("STATUS " + id + " " + maxHp + " " + size);
	}
}