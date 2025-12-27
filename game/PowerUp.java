package game;

import java.util.ArrayList;
import java.util.Collections;

public abstract class PowerUp {
	public String name, desc, merit, demerit;
	public PowerUp(String n, String d, String m, String dm) { name=n; desc=d; merit=m; demerit=dm; }
	public abstract void apply(Player p);
}

class PowerUpFactory {
	public static ArrayList<PowerUp> getRandomPowerUps(int count) {
		ArrayList<PowerUp> all = new ArrayList<>();

		all.add(new PowerUp("Hill", "吸血能力", "攻撃時HP回復", "威力低下") { public void apply(Player p) { p.weapon.addEffect(new EffectHill()); }});
		all.add(new PowerUp("Rising", "高速弾", "弾速+100%", "威力-20%") { public void apply(Player p) { p.weapon.addEffect(new EffectRising()); }});
		all.add(new PowerUp("Impact", "高威力砲", "威力・弾速2倍", "間隔・リロード増") { public void apply(Player p) { p.weapon.addEffect(new EffectImpactShot()); }});
		all.add(new PowerUp("Danmaku", "弾幕", "5発ばら撒き", "リロード増") { public void apply(Player p) { p.weapon.addEffect(new EffectDanmaku()); }});
		all.add(new PowerUp("Reel Gun", "3点バースト", "3連射(乗算)", "リロード増") { public void apply(Player p) { p.weapon.addEffect(new EffectReelGun()); }});
		all.add(new PowerUp("Shower", "シャワー", "15発拡散", "威力↓ リロード↑") { public void apply(Player p) { p.weapon.addEffect(new EffectShower()); }});
		all.add(new PowerUp("Reflect", "反射", "反射数+2 威力↑", "リロード増") { public void apply(Player p) { p.weapon.addEffect(new EffectReflection()); }});
		all.add(new PowerUp("No Control", "制御不能", "反射+5 速度↑", "リロード増") { public void apply(Player p) { p.weapon.addEffect(new EffectOutOfControl()); }});
		all.add(new PowerUp("Idaten", "韋駄天", "移動・弾速2倍", "HP・威力低下") { public void apply(Player p) { p.weapon.addEffect(new EffectIdaten()); p.applyPowerUpStats(); }});
		all.add(new PowerUp("Cold", "冷却弾", "相手速度-50%", "リロード増") { public void apply(Player p) { p.weapon.addEffect(new EffectColdShot()); }});
		all.add(new PowerUp("3 in 1", "三位一体", "威力・速度+100%", "弾数-2 リロード増") { public void apply(Player p) { p.weapon.addEffect(new Effect3in1()); }});
		all.add(new PowerUp("Poison", "毒", "DOTダメージ", "リロード増") { public void apply(Player p) { p.weapon.addEffect(new EffectPoisonNew()); }});
		all.add(new PowerUp("Ghost", "ゴースト", "壁貫通", "リロード増") { public void apply(Player p) { p.weapon.addEffect(new EffectGhostShot()); }});
		all.add(new PowerUp("Quick", "クイック", "リロード時間半減", "なし") { public void apply(Player p) { p.weapon.addEffect(new EffectQuickReload()); }});

		all.add(new PowerUp("Big Boy", "巨大化", "HP増・巨大化", "被弾判定増") { public void apply(Player p) { p.weapon.addEffect(new EffectBigBoy()); p.applyPowerUpStats(); }});
		all.add(new PowerUp("Small Boy", "小型化", "回避・速度UP", "HP低下") { public void apply(Player p) { p.weapon.addEffect(new EffectSmallBoy()); p.applyPowerUpStats(); }});

		all.add(new PowerUp("Tac. Reload", "戦術リロード", "防御時弾回復", "CD+2秒") { public void apply(Player p) { p.hasSkillTacticalReload = true; }});
		all.add(new PowerUp("Exc. Defense", "専守防衛", "2秒後再発動 HP+", "CD+2秒") { public void apply(Player p) { p.hasSkillExclusiveDefense = true; p.applyPowerUpStats(); }});
		all.add(new PowerUp("Invisible", "透明化", "防御後透明化", "CD+5秒") { public void apply(Player p) { p.hasSkillInvisible = true; }});
		all.add(new PowerUp("Emergency", "緊急防御", "弾切れ時自動防御", "なし") { public void apply(Player p) { p.hasSkillEmergencyDefense = true; }});
		all.add(new PowerUp("Teleport", "テレポート", "防御時瞬間移動", "CD+2秒") { public void apply(Player p) { p.hasSkillTeleport = true; }});
		// Cold Pres. を削除

		all.add(new PowerUp("Thirst", "渇望", "攻撃時加速", "なし") { public void apply(Player p) { p.hasPassiveThirst = true; }});
		all.add(new PowerUp("Delay", "ディレイ", "ダメージ分散", "なし") { public void apply(Player p) { p.hasPassiveDelay = true; }});
		all.add(new PowerUp("Confidence", "自信過剰", "攻撃時HP3倍", "終了時1/3") { public void apply(Player p) { p.hasPassiveConfidence = true; }});

		Collections.shuffle(all);
		return new ArrayList<>(all.subList(0, Math.min(count, all.size())));
	}
}