package game;

import java.util.ArrayList;
import java.util.Collections;
import java.awt.geom.Line2D;
import java.io.PrintWriter;
import static game.GameConstants.*;

/**
 * プレイヤーの武器を管理するクラス。
 * 弾薬数、リロード、発射レート、およびバフ効果（WeaponEffect）を管理します。
 */
public class Weapon {
	Player owner;
	public int maxAmmo = WEAPON_DEFAULT_AMMO;
	public int currentAmmo = WEAPON_DEFAULT_AMMO;
	public int reloadDuration = WEAPON_DEFAULT_RELOAD;
	public int damage = WEAPON_DEFAULT_DAMAGE;
	public int bulletSize = WEAPON_DEFAULT_SIZE;
	public double bulletSpeed = WEAPON_DEFAULT_SPEED;

	// --- リロード・連射制御 ---
	public boolean isReloading = false;
	public int reloadTimer = 0;
	public int fireInterval = 10;
	public int fireTimer = 0;

	// --- バースト・拡散設定 ---
	public int burstQueue = 0;
	public int burstTimer = 0;
	public int bulletsPerBurst = 1;
	public int pelletsPerShot = 1;
	public double spreadAngle = 0.0;
	public boolean randomSpeed = false;
	public int extraBounces = 0;

	// 新規: 弾の最大寿命
	public int bulletLifeTime = BULLET_DEFAULT_LIFE;

	// 適用されているエフェクトリスト
	ArrayList<WeaponEffect> effects = new ArrayList<>();

	public Weapon(Player p) { this.owner = p; }

	public void reset() {
		currentAmmo = maxAmmo;
		isReloading = false;
		reloadTimer = 0;
		burstQueue = 0;
		fireTimer = 0;
	}

	/**
	 * 新しい効果（パワーアップ）を追加し、ステータスを再計算します。
	 */
	public void addEffect(WeaponEffect e) {
		effects.add(e);
		recalcStats();
		currentAmmo = maxAmmo;
	}

	/**
	 * 全エフェクトを適用して武器の基礎ステータスを再計算します。
	 */
	public void recalcStats() {
		// デフォルト値にリセット
		maxAmmo = WEAPON_DEFAULT_AMMO;
		reloadDuration = WEAPON_DEFAULT_RELOAD;
		damage = WEAPON_DEFAULT_DAMAGE;
		bulletSpeed = WEAPON_DEFAULT_SPEED;
		bulletSize = WEAPON_DEFAULT_SIZE;
		fireInterval = 10;
		bulletsPerBurst = 1;
		pelletsPerShot = 1;
		spreadAngle = 0.0;
		randomSpeed = false;
		extraBounces = 0;
		bulletLifeTime = BULLET_DEFAULT_LIFE;

		// 全エフェクト適用
		for (WeaponEffect e : effects) e.applyStats(this);

		if (maxAmmo < 1) maxAmmo = 1;
	}

	/**
	 * 武器の更新処理。リロード進行やバースト射撃の処理を行います。
	 */
	public void update(PrintWriter out, int myId, ArrayList<Line2D.Double> obstacles) {
		if (fireTimer > 0) fireTimer--;

		if (isReloading) {
			reloadTimer++;
			if (reloadTimer >= reloadDuration) {
				currentAmmo = maxAmmo;
				isReloading = false;
				reloadTimer = 0;
			}
		}

		// バースト射撃の残弾処理
		if (burstQueue > 0) {
			burstTimer++;
			if (burstTimer >= WEAPON_BURST_INTERVAL) {
				performShot(out, myId);
				burstQueue--;
				burstTimer = 0;
			}
		}
	}

	/**
	 * 射撃を試みます。弾切れの場合はリロードを開始します。
	 */
	public void tryShoot(PrintWriter out, int myId, ArrayList<Line2D.Double> obstacles) {
		if (fireTimer > 0) return;

		if (isReloading || currentAmmo <= 0) {
			if (!isReloading && currentAmmo < maxAmmo) startReload(obstacles);
			return;
		}

		fireTimer = fireInterval;
		currentAmmo--;

		// バースト射撃開始
		burstQueue = bulletsPerBurst;
		performShot(out, myId);
		burstQueue--; // 1発目は即時発射

		if (currentAmmo <= 0) {
			startReload(obstacles);
			// 緊急防御スキル: 弾切れ時に自動ガード
			if (owner.hasSkillEmergencyDefense) {
				owner.forceGuard(obstacles);
			}
		}
	}

	/**
	 * 実際に弾（または複数の弾）を発射します。
	 * 拡散やランダム速度の計算もここで行います。
	 */
	private void performShot(PrintWriter out, int myId) {
		for (int i = 0; i < pelletsPerShot; i++) {
			double currentAngle = owner.angle;
			double speed = bulletSpeed;

			if (pelletsPerShot > 1) {
				if (randomSpeed) {
					// spreadAngleが設定されていればその範囲で、そうでなければデフォルト(1.0ラジアン)で拡散
					double range = (spreadAngle > 0) ? spreadAngle : 1.0;
					currentAngle += (Math.random() - 0.5) * range;
					// 速度のバラつき
					speed = bulletSpeed * (0.9 + Math.random() * 0.2);
				} else {
					double step = 0.2;
					// 拡散角度が設定されている場合、その範囲内に等間隔で配置
					if (spreadAngle > 0) {
						step = spreadAngle / (pelletsPerShot - 1);
					}
					double offset = (i - (pelletsPerShot - 1) / 2.0) * step;
					currentAngle += offset;
				}
			}

			fireRaw(owner.x, owner.y, currentAngle, speed, out, myId);
		}
	}

	public void startReload(ArrayList<Line2D.Double> obstacles) {
		isReloading = true;
		reloadTimer = 0;
	}

	private void fireRaw(double x, double y, double angle, double spd, PrintWriter out, int myId) {
		int bId = (int)(Math.random() * 1000000);
		int flags = FLAG_NONE;
		for(WeaponEffect e : effects) flags |= e.getFlag();

		out.println("SHOT " + bId + " " + x + " " + y + " " + angle +
				" " + spd + " " + damage + " " + bulletSize + " " + flags + " " + myId + " " + extraBounces + " " + bulletLifeTime);
	}
}

/**
 * 武器に特殊効果を与えるための基底クラス (Strategy Pattern)
 */
abstract class WeaponEffect {
	public abstract void applyStats(Weapon w);
	public int getFlag() { return FLAG_NONE; }
}

// === 各種効果の実装 ===

class EffectHill extends WeaponEffect {
	public void applyStats(Weapon w) {
		w.damage *= POWERUP_HILL_DAMAGE_MULT;
	} public int getFlag() {
		return FLAG_HILL;
	}
}

class EffectRising extends WeaponEffect {
	public void applyStats(Weapon w) {
		w.bulletSpeed *= POWERUP_RISING_SPEED_MULT;
		w.damage *= POWERUP_RISING_DAMAGE_MULT;
	}
}

class EffectImpactShot extends WeaponEffect {
	public void applyStats(Weapon w) {
		w.damage *= POWERUP_IMPACT_DAMAGE_MULT;
		w.bulletSpeed *= POWERUP_IMPACT_SPEED_MULT;
		w.reloadDuration *= POWERUP_IMPACT_RELOAD_MULT;
		w.fireInterval += POWERUP_IMPACT_INTERVAL_ADD;
	}
}

class EffectBigBoy extends WeaponEffect {
	public void applyStats(Weapon w) {
		/* Player側で適用 */
	}
}
class EffectSmallBoy extends WeaponEffect {
	public void applyStats(Weapon w) {
		/* Player側で適用 */
	}
}

class EffectDanmaku extends WeaponEffect {
	public void applyStats(Weapon w) {
		w.pelletsPerShot += POWERUP_DANMAKU_PELLETS_ADD;
		w.reloadDuration *= POWERUP_DANMAKU_RELOAD_MULT;
	}
}

class EffectReelGun extends WeaponEffect {
	public void applyStats(Weapon w) {
		w.bulletsPerBurst += POWERUP_REEL_BURST_ADD;
		w.reloadDuration *= POWERUP_REEL_RELOAD_MULT;
	}
}

class EffectShower extends WeaponEffect {
	public void applyStats(Weapon w) {
		w.pelletsPerShot += POWERUP_SHOWER_PELLETS_ADD;
		w.randomSpeed = true;
		w.reloadDuration *= POWERUP_SHOWER_RELOAD_MULT;
		w.damage *= POWERUP_SHOWER_DAMAGE_MULT;
	}
}

class EffectReflection extends WeaponEffect {
	public void applyStats(Weapon w) {
		w.extraBounces += POWERUP_REFLECT_BOUNCE_ADD;
		w.damage *= POWERUP_REFLECT_DAMAGE_MULT;
		w.reloadDuration *= POWERUP_REFLECT_RELOAD_MULT;
	} public int getFlag() {
		return FLAG_BOUNCE;
	}
}

class EffectOutOfControl extends WeaponEffect {
	public void applyStats(Weapon w) {
		w.extraBounces += POWERUP_CONTROL_BOUNCE_ADD;
		w.bulletSpeed *= POWERUP_CONTROL_SPEED_MULT;
		w.reloadDuration *= POWERUP_CONTROL_RELOAD_MULT;
	} public int getFlag() {
		return FLAG_BOUNCE;
	}
}

class EffectIdaten extends WeaponEffect {
	public void applyStats(Weapon w) {
		w.bulletSpeed *= POWERUP_IDATEN_SPEED_MULT;
		w.damage *= POWERUP_IDATEN_DAMAGE_MULT;
	}
}

class EffectColdShot extends WeaponEffect {
	public void applyStats(Weapon w) {
		w.reloadDuration *= POWERUP_COLD_RELOAD_MULT;
	} public int getFlag() {
		return FLAG_COLD;
	}
}

class Effect3in1 extends WeaponEffect {
	public void applyStats(Weapon w) {
		w.damage *= POWERUP_3IN1_DAMAGE_MULT;
		w.bulletSpeed *= POWERUP_3IN1_SPEED_MULT;
		w.maxAmmo = Math.max(1, w.maxAmmo - POWERUP_3IN1_AMMO_SUB);
		w.reloadDuration *= POWERUP_3IN1_RELOAD_MULT;
	}
}

class EffectPoisonNew extends WeaponEffect {
	public void applyStats(Weapon w) {
		w.reloadDuration *= POWERUP_POISON_RELOAD_MULT;
		w.damage *= POWERUP_POISON_DAMAGE_MULT;
	}
	public int getFlag() { return FLAG_POISON; }
}

class EffectGhostShot extends WeaponEffect {
	public void applyStats(Weapon w) {
		w.reloadDuration *= POWERUP_GHOST_RELOAD_MULT;
	} public int getFlag() {
		return FLAG_GHOST;
	}
}

class EffectQuickReload extends WeaponEffect {
	public void applyStats(Weapon w) {
		w.reloadDuration *= POWERUP_QUICK_RELOAD_MULT;
	}
}

class EffectBigCapacity extends WeaponEffect {
	public void applyStats(Weapon w) {
		w.maxAmmo += POWERUP_BIGCAP_AMMO_ADD;
		w.reloadDuration *= POWERUP_BIGCAP_RELOAD_MULT;
	}
}
class EffectShotgun extends WeaponEffect {
	public void applyStats(Weapon w) {
		w.pelletsPerShot += POWERUP_SHOTGUN_PELLETS_ADD;
		w.damage *= POWERUP_SHOTGUN_DAMAGE_MULT;
		// 距離(300) / 速度 = 寿命時間
		w.bulletLifeTime = (int)(POWERUP_SHOTGUN_RANGE / w.bulletSpeed);
		w.spreadAngle = POWERUP_SHOTGUN_SPREAD; // 拡散角度を設定
		w.randomSpeed = true;
	}
}
class EffectBuildUp extends WeaponEffect {
	public void applyStats(Weapon w) {
		w.damage *= POWERUP_BUILDUP_DAMAGE_MULT;
	}
}