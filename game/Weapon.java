package game;

import java.util.ArrayList;
import java.io.PrintWriter;

public class Weapon {
	Player owner;
	public int maxAmmo = 1, currentAmmo = 1, reloadDuration = 60;
	public int damage = 20, bulletSize = 8;
	public double bulletSpeed = 10.0;
	public boolean isReloading = false;
	public int reloadTimer = 0, burstQueue = 0, burstTimer = 0;
	ArrayList<WeaponEffect> effects = new ArrayList<>();

	public Weapon(Player p) { this.owner = p; }

	public void reset() {
		currentAmmo = maxAmmo;
		isReloading = false;
		reloadTimer = 0;
		burstQueue = 0;
	}

	public void addEffect(WeaponEffect e) {
		effects.add(e);
		recalcStats();
	}

	private void recalcStats() {
		maxAmmo = 1; reloadDuration = 60; damage = 20; bulletSpeed = 10.0; bulletSize = 8;
		for (WeaponEffect e : effects) e.applyStats(this);
	}

	public void update(PrintWriter out, int myId) {
		if (isReloading) {
			reloadTimer++;
			if (reloadTimer >= reloadDuration) { currentAmmo = maxAmmo; isReloading = false; reloadTimer = 0; }
		}
		if (burstQueue > 0) {
			burstTimer++;
			if (burstTimer >= 5) {
				fireRaw(owner.x, owner.y, owner.angle, out, myId);
				burstQueue--;
				burstTimer = 0;
			}
		}
	}

	public void tryShoot(PrintWriter out, int myId) {
		if (isReloading || currentAmmo <= 0) {
			if (!isReloading && currentAmmo < maxAmmo) startReload();
			return;
		}
		boolean hasBurst = false, hasTri = false;
		for(WeaponEffect e : effects) {
			if(e instanceof EffectBurst) hasBurst = true;
			if(e instanceof EffectTrifurcation) hasTri = true;
		}
		if (hasBurst) {
			currentAmmo--; burstQueue = 3; burstTimer = 5;
		} else {
			currentAmmo--;
			if (hasTri) {
				fireRaw(owner.x, owner.y, owner.angle, out, myId);
				fireRaw(owner.x, owner.y, owner.angle - 0.3, out, myId);
				fireRaw(owner.x, owner.y, owner.angle + 0.3, out, myId);
			} else {
				fireRaw(owner.x, owner.y, owner.angle, out, myId);
			}
		}
		if (currentAmmo <= 0) startReload();
	}

	private void startReload() { isReloading = true; reloadTimer = 0; }

	private void fireRaw(double x, double y, double angle, PrintWriter out, int myId) {
		int bId = (int)(Math.random() * 1000000);
		int flags = GameConstants.FLAG_NONE;
		for(WeaponEffect e : effects) flags |= e.getFlag();

		// サーバーへ送信
		out.println("SHOT " + bId + " " + x + " " + y + " " + angle +
				" " + bulletSpeed + " " + damage + " " + bulletSize + " " + flags + " " + myId);
	}
}

// エフェクト類もここにまとめておきます
abstract class WeaponEffect { public abstract void applyStats(Weapon w); public int getFlag() { return GameConstants.FLAG_NONE; } }
class EffectHill extends WeaponEffect { public void applyStats(Weapon w) { w.damage *= 0.9; } public int getFlag() { return GameConstants.FLAG_HILL; } }
class EffectBounce extends WeaponEffect { public void applyStats(Weapon w) { w.bulletSpeed *= 0.8; w.reloadDuration *= 1.2; } public int getFlag() { return GameConstants.FLAG_BOUNCE; } }
class EffectTrifurcation extends WeaponEffect { public void applyStats(Weapon w) { w.reloadDuration *= 1.5; } }
class EffectRising extends WeaponEffect { public void applyStats(Weapon w) { w.bulletSpeed *= 2.0; w.damage *= 0.7; } }
class EffectImpact extends WeaponEffect { public void applyStats(Weapon w) { w.damage *= 2.0; w.reloadDuration *= 1.3; } }
class EffectBigBall extends WeaponEffect { public void applyStats(Weapon w) { w.bulletSize *= 3; w.damage *= 0.8; w.bulletSpeed *= 0.8; } }
class EffectPoison extends WeaponEffect { public void applyStats(Weapon w) { w.reloadDuration *= 1.1; } public int getFlag() { return GameConstants.FLAG_POISON; } }
class EffectBurst extends WeaponEffect { public void applyStats(Weapon w) { w.reloadDuration *= 1.2; } }