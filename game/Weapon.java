package game;

import java.util.ArrayList;
import java.io.PrintWriter;
import static game.GameConstants.*;

public class Weapon {
	Player owner;
	public int maxAmmo = WEAPON_DEFAULT_AMMO;
	public int currentAmmo = WEAPON_DEFAULT_AMMO;
	public int reloadDuration = WEAPON_DEFAULT_RELOAD;
	public int damage = WEAPON_DEFAULT_DAMAGE;
	public int bulletSize = WEAPON_DEFAULT_SIZE;
	public double bulletSpeed = WEAPON_DEFAULT_SPEED;

	public boolean isReloading = false;
	public int reloadTimer = 0;

	public int burstQueue = 0;
	public int burstTimer = 0;
	public int bulletsPerBurst = 1;

	public int pelletsPerShot = 1;
	public double spreadAngle = 0.0;
	public boolean randomSpeed = false;

	public int fireInterval = 10;
	public int fireTimer = 0;

	public int extraBounces = 0;

	ArrayList<WeaponEffect> effects = new ArrayList<>();

	public Weapon(Player p) { this.owner = p; }

	public void reset() {
		currentAmmo = maxAmmo;
		isReloading = false;
		reloadTimer = 0;
		burstQueue = 0;
		fireTimer = 0;
	}

	public void addEffect(WeaponEffect e) {
		effects.add(e);
		recalcStats();
		currentAmmo = maxAmmo;
	}

	public void recalcStats() {
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

		for (WeaponEffect e : effects) e.applyStats(this);

		if (maxAmmo < 1) maxAmmo = 1;
	}

	public void update(PrintWriter out, int myId) {
		if (fireTimer > 0) fireTimer--;

		if (isReloading) {
			reloadTimer++;
			if (reloadTimer >= reloadDuration) {
				currentAmmo = maxAmmo;
				isReloading = false;
				reloadTimer = 0;
			}
		}

		if (burstQueue > 0) {
			burstTimer++;
			if (burstTimer >= WEAPON_BURST_INTERVAL) {
				performShot(out, myId);
				burstQueue--;
				burstTimer = 0;
			}
		}
	}


	public void tryShoot(PrintWriter out, int myId) {
		// クールダウン中は撃てない
		if (fireTimer > 0) return;

		// 弾切れ・リロード中の処理
		if (isReloading || currentAmmo <= 0) {
			if (!isReloading && currentAmmo < maxAmmo) startReload();
			// ここにあった緊急防御の判定は消す（空撃ち判定になってしまうため）
			return;
		}

		fireTimer = fireInterval;
		currentAmmo--;

		burstQueue = bulletsPerBurst;
		performShot(out, myId);
		burstQueue--;

		// 弾を撃った結果、0になったらリロード＆緊急防御チェック
		if (currentAmmo <= 0) {
			startReload();

			// 緊急防御スキル持ちなら、クールダウン無視でガード発動
			if (owner.hasSkillEmergencyDefense) {
				owner.forceGuard();
			}
		}
	}

	private void performShot(PrintWriter out, int myId) {
		for (int i = 0; i < pelletsPerShot; i++) {
			double currentAngle = owner.angle;
			double speed = bulletSpeed;

			if (pelletsPerShot > 1) {
				if (randomSpeed) {
					currentAngle += (Math.random() - 0.5) * 1.0;
					speed = bulletSpeed * (0.98 + Math.random() * 0.04);
				} else {
					double step = 0.2;
					double offset = (i - (pelletsPerShot - 1) / 2.0) * step;
					currentAngle += offset;
				}
			}

			fireRaw(owner.x, owner.y, currentAngle, speed, out, myId);
		}
	}

	public void startReload() { isReloading = true; reloadTimer = 0; }

	private void fireRaw(double x, double y, double angle, double spd, PrintWriter out, int myId) {
		int bId = (int)(Math.random() * 1000000);
		int flags = FLAG_NONE;
		for(WeaponEffect e : effects) flags |= e.getFlag();

		out.println("SHOT " + bId + " " + x + " " + y + " " + angle +
				" " + spd + " " + damage + " " + bulletSize + " " + flags + " " + myId + " " + extraBounces);
	}
}

abstract class WeaponEffect {
	public abstract void applyStats(Weapon w);
	public int getFlag() { return FLAG_NONE; }
}

class EffectHill extends WeaponEffect { public void applyStats(Weapon w) { w.damage *= 0.6; } public int getFlag() { return FLAG_HILL; } }
class EffectRising extends WeaponEffect { public void applyStats(Weapon w) { w.bulletSpeed *= 2.0; w.damage *= 0.8; } }
class EffectImpactShot extends WeaponEffect { public void applyStats(Weapon w) { w.damage *= 2.0; w.bulletSpeed *= 2.0; w.reloadDuration *= 1.5; w.fireInterval += 60; } }
class EffectBigBoy extends WeaponEffect { public void applyStats(Weapon w) { } }
class EffectSmallBoy extends WeaponEffect { public void applyStats(Weapon w) { } }
class EffectDanmaku extends WeaponEffect { public void applyStats(Weapon w) { w.pelletsPerShot = 5; w.reloadDuration *= 1.3; } }
class EffectReelGun extends WeaponEffect { public void applyStats(Weapon w) { w.bulletsPerBurst = 3; w.reloadDuration *= 1.3; } }
class EffectShower extends WeaponEffect { public void applyStats(Weapon w) { w.pelletsPerShot = 15; w.randomSpeed = true; w.reloadDuration *= 1.5; w.damage *= 0.3; } }
class EffectReflection extends WeaponEffect { public void applyStats(Weapon w) { w.extraBounces += 2; w.damage *= 1.1; w.reloadDuration *= 1.2; } public int getFlag() { return FLAG_BOUNCE; } }
class EffectOutOfControl extends WeaponEffect { public void applyStats(Weapon w) { w.extraBounces += 5; w.bulletSpeed *= 1.2; w.reloadDuration *= 1.3; } public int getFlag() { return FLAG_BOUNCE; } }
class EffectIdaten extends WeaponEffect { public void applyStats(Weapon w) { w.bulletSpeed *= 2.0; w.damage *= 0.6; } }
class EffectColdShot extends WeaponEffect { public void applyStats(Weapon w) { w.reloadDuration *= 1.5; } public int getFlag() { return FLAG_COLD; } }
class Effect3in1 extends WeaponEffect { public void applyStats(Weapon w) { w.damage *= 2.0; w.bulletSpeed *= 2.0; w.maxAmmo -= 2; w.reloadDuration *= 1.5; } }
class EffectPoisonNew extends WeaponEffect { public void applyStats(Weapon w) { w.reloadDuration *= 1.25; } public int getFlag() { return FLAG_POISON; } }
class EffectGhostShot extends WeaponEffect { public void applyStats(Weapon w) { w.reloadDuration *= 1.25; } public int getFlag() { return FLAG_GHOST; } }
class EffectQuickReload extends WeaponEffect { public void applyStats(Weapon w) { w.reloadDuration /= 2; } }
class EffectExtendedMag extends WeaponEffect { public void applyStats(Weapon w) { w.maxAmmo += 5; } }