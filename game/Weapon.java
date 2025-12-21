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
	public int reloadTimer = 0, burstQueue = 0, burstTimer = 0;

	// ★発射間隔制御用
	public int fireInterval = 10; // 10フレーム（約0.16秒）は次が撃てない
	public int fireTimer = 0;

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

	private void recalcStats() {
		maxAmmo = WEAPON_DEFAULT_AMMO;
		reloadDuration = WEAPON_DEFAULT_RELOAD;
		damage = WEAPON_DEFAULT_DAMAGE;
		bulletSpeed = WEAPON_DEFAULT_SPEED;
		bulletSize = WEAPON_DEFAULT_SIZE;
		// エフェクトによる変更がある場合もfireIntervalを戻すならここで設定
		fireInterval = 10;

		for (WeaponEffect e : effects) e.applyStats(this);

		if (maxAmmo < 1) maxAmmo = 1;
	}

	public void update(PrintWriter out, int myId) {
		// ★クールダウンタイマーの更新
		if (fireTimer > 0) fireTimer--;

		if (isReloading) {
			reloadTimer++;
			if (reloadTimer >= reloadDuration) { currentAmmo = maxAmmo; isReloading = false; reloadTimer = 0; }
		}
		if (burstQueue > 0) {
			burstTimer++;
			if (burstTimer >= WEAPON_BURST_INTERVAL) {
				fireRaw(owner.x, owner.y, owner.angle, out, myId);
				burstQueue--;
				burstTimer = 0;
			}
		}
	}

	public void tryShoot(PrintWriter out, int myId) {
		// ★クールダウン中は撃てない
		if (fireTimer > 0) return;

		if (isReloading || currentAmmo <= 0) {
			if (!isReloading && currentAmmo < maxAmmo) startReload();
			return;
		}

		// ★発射したらタイマーセット
		fireTimer = fireInterval;

		boolean hasBurst = false, hasTri = false;
		for(WeaponEffect e : effects) {
			if(e instanceof EffectBurst) hasBurst = true;
			if(e instanceof EffectTrifurcation) hasTri = true;
		}

		if (hasBurst) {
			currentAmmo--; burstQueue = WEAPON_BURST_COUNT; burstTimer = WEAPON_BURST_INTERVAL;
		} else {
			currentAmmo--;
			if (hasTri) {
				fireRaw(owner.x, owner.y, owner.angle, out, myId);
				fireRaw(owner.x, owner.y, owner.angle - WEAPON_TRI_ANGLE_OFFSET, out, myId);
				fireRaw(owner.x, owner.y, owner.angle + WEAPON_TRI_ANGLE_OFFSET, out, myId);
			} else {
				fireRaw(owner.x, owner.y, owner.angle, out, myId);
			}
		}
		if (currentAmmo <= 0) startReload();
	}

	private void startReload() { isReloading = true; reloadTimer = 0; }

	private void fireRaw(double x, double y, double angle, PrintWriter out, int myId) {
		int bId = (int)(Math.random() * 1000000);
		int flags = FLAG_NONE;
		for(WeaponEffect e : effects) flags |= e.getFlag();

		out.println("SHOT " + bId + " " + x + " " + y + " " + angle +
				" " + bulletSpeed + " " + damage + " " + bulletSize + " " + flags + " " + myId);
	}
}

// === 以下、消えてしまっていたエフェクトクラス群（定数使用版） ===

abstract class WeaponEffect {
	public abstract void applyStats(Weapon w);
	public int getFlag() { return FLAG_NONE; }
}
class EffectHill extends WeaponEffect { public void applyStats(Weapon w) { w.damage *= EFFECT_HILL_DAMAGE_MULT; } public int getFlag() { return FLAG_HILL; } }
class EffectBounce extends WeaponEffect { public void applyStats(Weapon w) { w.bulletSpeed *= EFFECT_BOUNCE_SPEED_MULT; w.reloadDuration *= EFFECT_BOUNCE_RELOAD_MULT; } public int getFlag() { return FLAG_BOUNCE; } }
class EffectTrifurcation extends WeaponEffect { public void applyStats(Weapon w) { w.reloadDuration *= EFFECT_TRI_RELOAD_MULT; } }
class EffectRising extends WeaponEffect { public void applyStats(Weapon w) { w.bulletSpeed *= EFFECT_RISING_SPEED_MULT; w.damage *= EFFECT_RISING_DAMAGE_MULT; } }
class EffectImpact extends WeaponEffect { public void applyStats(Weapon w) { w.damage *= EFFECT_IMPACT_DAMAGE_MULT; w.reloadDuration *= EFFECT_IMPACT_RELOAD_MULT; w.maxAmmo -= EFFECT_IMPACT_AMMO_REDUCTION; } }
class EffectBigBall extends WeaponEffect { public void applyStats(Weapon w) { w.bulletSize *= EFFECT_BIG_SIZE_MULT; w.damage *= EFFECT_BIG_DAMAGE_MULT; w.bulletSpeed *= EFFECT_BIG_SPEED_MULT; } }
class EffectPoison extends WeaponEffect { public void applyStats(Weapon w) { w.reloadDuration *= EFFECT_POISON_RELOAD_MULT; } public int getFlag() { return FLAG_POISON; } }
class EffectBurst extends WeaponEffect { public void applyStats(Weapon w) { w.reloadDuration *= EFFECT_BURST_RELOAD_MULT; } }
class EffectExtendedMag extends WeaponEffect { public void applyStats(Weapon w) { w.maxAmmo += EFFECT_EXTMAG_AMOUNT; } }