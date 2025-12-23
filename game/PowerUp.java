package game;

import java.util.ArrayList;
import java.util.Collections;
import static game.GameConstants.*;

public abstract class PowerUp {
	public String name, desc, merit, demerit;
	public PowerUp(String n, String d, String m, String dm) { name=n; desc=d; merit=m; demerit=dm; }
	public abstract void apply(Player p);
}

class PowerUpFactory {
	public static ArrayList<PowerUp> getRandomPowerUps(int count) {
		ArrayList<PowerUp> all = new ArrayList<>();

		all.add(new PowerUp("Hill", "Vampire", "Heal on hit", "Less Dmg") { public void apply(Player p) { p.weapon.addEffect(new EffectHill()); }});
		all.add(new PowerUp("Bounce", "Bounce", "Wall bounce", "Slow/Reload+") { public void apply(Player p) { p.weapon.addEffect(new EffectBounce()); }});
		all.add(new PowerUp("Tri-Shot", "Shotgun", "3-Way Shot", "Reload++") { public void apply(Player p) { p.weapon.addEffect(new EffectTrifurcation()); }});
		all.add(new PowerUp("Rising", "Sniper", "High Speed", "Less Dmg") { public void apply(Player p) { p.weapon.addEffect(new EffectRising()); }});
		all.add(new PowerUp("Impact", "Cannon", "High Dmg", "Ammo--") { public void apply(Player p) { p.weapon.addEffect(new EffectImpact()); }});
		all.add(new PowerUp("Big Ball", "Giant", "Huge Bullet", "Slow") { public void apply(Player p) { p.weapon.addEffect(new EffectBigBall()); }});
		all.add(new PowerUp("Poison", "Venom", "Slow Enemy", "Reload+") { public void apply(Player p) { p.weapon.addEffect(new EffectPoison()); }});
		all.add(new PowerUp("Reel Gun", "Burst", "3-Round Burst", "Reload+") { public void apply(Player p) { p.weapon.addEffect(new EffectBurst()); }});

		// 定数使用
		all.add(new PowerUp("Big Boy", "Tank", "HP++", "Hitbox++") {
			public void apply(Player p) {
				p.maxHp += POWERUP_TANK_HP_BONUS; p.hp = p.maxHp; p.size *= POWERUP_TANK_SIZE_MULT;
			}
		});
		all.add(new PowerUp("Small Boy", "Rogue", "Evasion UP", "HP--") {
			public void apply(Player p) {
				p.maxHp = POWERUP_ROGUE_FIXED_HP;
				p.hp = p.maxHp;
				p.size *= POWERUP_ROGUE_SIZE_MULT;
				p.speed *= POWERUP_ROGUE_SPEED_MULT;
			}
		});

		all.add(new PowerUp("Ext. Mag", "Capacity", "Ammo +"+EFFECT_EXTMAG_AMOUNT, "None") {
			public void apply(Player p) { p.weapon.addEffect(new EffectExtendedMag()); }
		});

		Collections.shuffle(all);
		return new ArrayList<>(all.subList(0, Math.min(count, all.size())));
	}
}