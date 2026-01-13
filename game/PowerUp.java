package game;

import java.util.ArrayList;
import java.util.Collections;

/**
 * ラウンド敗北時に選択できるパワーアップ（強化カード）の基底クラス。
 */
public abstract class PowerUp {
	public String name, desc, merit, demerit;
	// 能力詳細・運用方法のテキスト
	public String flavorText;

	public PowerUp(String n, String d, String m, String dm) {
		this(n, d, m, dm, "詳細不明。");
	}

	public PowerUp(String n, String d, String m, String dm, String flavor) {
		name = n; desc = d; merit = m; demerit = dm; flavorText = flavor;
	}

	public abstract void apply(Player p);
}

/**
 * パワーアップ一覧を生成・提供するファクトリークラス。
 */
class PowerUpFactory {
	/**
	 * ランダムに指定数のパワーアップを取得します。
	 * @param count 取得する数
	 * @return ランダムなPowerUpリスト
	 */
	public static ArrayList<PowerUp> getRandomPowerUps(int count) {
		ArrayList<PowerUp> all = getAllPowerUps();
		Collections.shuffle(all);
		return new ArrayList<>(all.subList(0, Math.min(count, all.size())));
	}

	/**
	 * 全能力のリストを取得（能力紹介画面用）
	 */
	public static ArrayList<PowerUp> getAllPowerUps() {
		ArrayList<PowerUp> all = new ArrayList<>();

		// --- 武器効果系 ---

		all.add(new PowerUp("Hill", "吸血能力", "攻撃時HP回復", "威力60%",
				"【解説】\n弾が命中するたびに少量のHPを回復する。\n【運用】\n威力は下がるため、一撃の重さより手数を優先せよ。\n体力を削り合う消耗戦に持ち込めば確実に競り勝てる。") {
			public void apply(Player p) { p.weapon.addEffect(new EffectHill()); }});

		all.add(new PowerUp("Rising", "高速弾", "弾速2倍", "威力80%",
				"【解説】\n弾速が倍になり、敵が反応して避けるのが困難になる。\n【運用】\n遠距離からの狙撃に最適。\n威力が下がる分、確実に当て続けるエイム力が問われる。") {
			public void apply(Player p) { p.weapon.addEffect(new EffectRising()); }});

		all.add(new PowerUp("Impact", "高威力砲", "威力・弾速2倍", "間隔増 リロード1.5倍",
				"【解説】\n単発火力が劇的に向上するが、連射が効かなくなる。\n【運用】\n無駄撃ちは厳禁。相手の硬直を狙って撃つこと。\n「Shotgun」などと組み合わせるとリスクを軽減できる。") {
			public void apply(Player p) { p.weapon.addEffect(new EffectImpactShot()); }});

		all.add(new PowerUp("Danmaku", "弾幕", "5発ばら撒き", "リロード1.3倍",
				"【解説】\n扇状に5発の弾を発射する。\n【運用】\n「面」での制圧が可能になる。\n相手の移動先を予測してばら撒き、回避ルートを限定させろ。") {
			public void apply(Player p) { p.weapon.addEffect(new EffectDanmaku()); }});

		all.add(new PowerUp("Reel Gun", "3点バースト", "3連射(乗算)", "リロード1.3倍",
				"【解説】\n1回の射撃で3発連続発射する。他の拡散効果と重複する。\n【運用】\n瞬間火力が高い。中距離で全弾命中させれば致命傷になる。\nリロードが重くなるため、残弾管理に注意。") {
			public void apply(Player p) { p.weapon.addEffect(new EffectReelGun()); }});

		all.add(new PowerUp("Shower", "シャワー", "15発拡散", "威力30% リロード1.5倍",
				"【解説】\n広範囲にランダム速度で多数の弾をばら撒く。\n【運用】\n狙いが甘くても当たるため、近〜中距離の押し付けが強力。\n「Hill（吸血）」や「Poison（毒）」と相性抜群。") {
			public void apply(Player p) { p.weapon.addEffect(new EffectShower()); }});

		all.add(new PowerUp("Reflect", "反射", "反射数+2 威力1.1倍", "リロード1.2倍",
				"【解説】\n弾が壁で跳ね返る回数が増え、威力も微増する。\n【運用】\n障害物の多いマップで真価を発揮する。\n壁裏に隠れた敵を、反射を利用して一方的に攻撃せよ。") {
			public void apply(Player p) { p.weapon.addEffect(new EffectReflection()); }});

		all.add(new PowerUp("No Control", "制御不能", "反射+5 速度1.2倍", "リロード1.3倍",
				"【解説】\n反射回数が大幅に増え弾速も上がるが、制御が難しい。\n【運用】\n狭い通路や部屋に撃ち込むことで弾幕地獄を作り出せる。\n自爆のリスクもあり、難しい。") {
			public void apply(Player p) { p.weapon.addEffect(new EffectOutOfControl()); }});

		all.add(new PowerUp("Idaten", "韋駄天", "移動・弾速1.5倍", "HP半減 威力60%",
				"【解説】\n移動速度と弾速が跳ね上がるが、非常に撃たれ弱くなる。\n【運用】\n被弾＝死と心得よ。常に動き回り、相手の死角を取り続けろ。\nヒット＆アウェイ戦法の極致。") {
			public void apply(Player p) { p.weapon.addEffect(new EffectIdaten()); p.applyPowerUpStats(); }});

		all.add(new PowerUp("Cold", "冷却弾", "相手速度50%", "リロード1.5倍",
				"【解説】\n命中した相手の移動速度を一時的に半減させる。\n【運用】\n初弾を当てれば相手は回避困難になる。\nそこへ高威力の次弾を叩き込むコンボの始動として優秀。") {
			public void apply(Player p) { p.weapon.addEffect(new EffectColdShot()); }});

		all.add(new PowerUp("3 in 1", "三位一体", "威力・速度2倍", "弾数-2 リロード1.5倍",
				"【解説】\n基本性能が大幅に強化されるが、装弾数が激減する。\n【運用】\n「Big Capacity」等で弾数を補わないとすぐに弾切れになる。\nリロードの隙をカバーできるスキルとの併用が推奨される。") {
			public void apply(Player p) { p.weapon.addEffect(new Effect3in1()); }});

		all.add(new PowerUp("Poison", "毒", "毒スタック付与", "リロード1.25倍 威力60%",
				"【解説】\n命中毎に毒スタック蓄積。スタック数に応じて継続ダメージ増加。\n【運用】\n当て続ければダメージが加速的に増える。\n「Shower」等でスタックを一気に溜める戦法が極悪。") {
			public void apply(Player p) { p.weapon.addEffect(new EffectPoisonNew()); }});

		all.add(new PowerUp("Ghost", "ゴースト", "短時間壁貫通", "リロード1.25倍",
				"【解説】\n発射直後の短時間、壁をすり抜ける弾になる。\n【運用】\n壁を挟んで睨み合っている状況を打破できる。\n相手の安全地帯をなくし、一方的に攻撃を通せる。") {
			public void apply(Player p) { p.weapon.addEffect(new EffectGhostShot()); }});

		all.add(new PowerUp("Quick", "クイック", "リロード時間半減", "なし",
				"【解説】\nリロード時間が半分になる。\n【運用】\n地味だが強力。絶え間なく弾幕を張り続けられる。\n装弾数の少ない高火力武器の弱点を打ち消すのに最適。") {
			public void apply(Player p) { p.weapon.addEffect(new EffectQuickReload()); }});

		// --- ステータス変更系 ---

		all.add(new PowerUp("Big Boy", "巨大化", "HP1.5倍・巨大化", "被弾判定2倍",
				"【解説】\nHPが1.5倍になるが、当たり判定も2倍になる。\n【運用】\n回避を捨てて体力で受けるタンク戦術向け。\n「Build Up」と組み合わせれば、\n要塞のような耐久力を得る。") {
			public void apply(Player p) { p.weapon.addEffect(new EffectBigBoy()); p.applyPowerUpStats(); }});

		all.add(new PowerUp("Small Boy", "小型化", "回避・速度1.3倍", "HP半減",
				"【解説】\nHPが半減するが、体が小さくなり回避しやすくなる。\n【運用】\n通常の弾幕を隙間ですり抜けられるようになる。\n操作精度に自信がある上級者向け。") {
			public void apply(Player p) { p.weapon.addEffect(new EffectSmallBoy()); p.applyPowerUpStats(); }});

		// --- スキル取得系 ---

		all.add(new PowerUp("Tac. Reload", "戦術リロード", "防御時弾回復", "CD+2秒",
				"【解説】\nガード成功時に弾薬が全回復する。\n【運用】\n撃ち切った後のリロード時間を踏み倒せる。\n「撃つ→ガード→即撃つ」というループが可能になる。") {
			public void apply(Player p) { p.hasSkillTacticalReload = true; }});

		all.add(new PowerUp("Exc. Defense", "専守防衛", "2秒後再発動 HP1.3倍", "CD+2秒",
				"【解説】\nHPが増え、\nガード終了の2秒後にもう一度ガードが発動する。\n【運用】\n守りは固くなるが、自分の攻撃ターンが遅れる点に注意。\n「Poison」などでリードを奪ってから守る時に強い。") {
			public void apply(Player p) { p.hasSkillExclusiveDefense = true; p.applyPowerUpStats(); }});

		all.add(new PowerUp("Invisible", "透明化", "防御後透明化", "CD+5秒",
				"【解説】\nガード発動後、少しの間姿が消える。\n【運用】\n姿を消してからの奇襲や、不利な状況からの逃走に。\n相手は予測射撃しかできなくなるため、心理戦で有利。") {
			public void apply(Player p) { p.hasSkillInvisible = true; }});

		all.add(new PowerUp("Emergency", "緊急防御", "弾切れ時自動防御", "なし",
				"【解説】\n弾を撃ち尽くした瞬間、自動でガードが発動する。\n【運用】\nリロード中の無防備な時間をカバーできる保険。\n攻めに集中しすぎて防御がおろそかになりがちな人へ。") {
			public void apply(Player p) { p.hasSkillEmergencyDefense = true; }});

		all.add(new PowerUp("Teleport", "テレポート", "防御時瞬間移動", "CD+2秒",
				"【解説】\nガード発動時、進行方向に一定距離ワープする。\n【運用】\n壁抜けが可能。敵の背後に回ったり、\n囲まれた状況からの緊急脱出に使ったりと応用幅が広い。") {
			public void apply(Player p) { p.hasSkillTeleport = true; }});

		// --- パッシブ取得系 ---

		all.add(new PowerUp("Thirst", "渇望", "攻撃時加速", "なし",
				"【解説】\n自分の弾が命中すると、移動速度が一時的に上がる。\n【運用】\n逃げる敵を追い詰めるチェイサー向け。\n一度捕まえれば、加速し続けて粘着攻撃が可能。") {
			public void apply(Player p) { p.hasPassiveThirst = true; }});

		all.add(new PowerUp("Delay", "ディレイ", "ダメージ分散", "なし",
				"【解説】\n受けたダメージを即座に減らさず、時間をかけて減らす。\n【運用】\n即死級のダメージを受けても即座には死なない。\nHPが尽きる前に相手を倒し切る、刺し違え覚悟の戦法に。") {
			public void apply(Player p) { p.hasPassiveDelay = true; }});

		all.add(new PowerUp("Confidence", "自信過剰", "攻撃時HP3倍", "終了時1/3",
				"【解説】\n攻撃命中時にHPが3倍になる。効果終了時に1/3になる。\n【運用】\n攻撃し続けている間はほぼ無敵だが、\n手が止まると瀕死になる。\n常に攻め続けられるアグレッシブなプレイヤー専用。") {
			public void apply(Player p) { p.hasPassiveConfidence = true; }});

		all.add(new PowerUp("Big Capacity", "大容量", "弾数+3 リロード0.9倍", "なし",
				"【解説】\n装弾数が増え、リロードも少し速くなる。\n【運用】\n単純にして強力な基礎ステータス強化。\nどんな武器と組み合わせても\n腐ることがない安定の選択肢。") {
			public void apply(Player p) { p.weapon.addEffect(new EffectBigCapacity()); }});

		all.add(new PowerUp("Shotgun", "ショットガン", "20発発射 威力1.1倍", "弾寿命1秒",
				"【解説】\n射程は短いが、凄まじい密度の弾を発射する。\n【運用】\n接近戦最強火力。\n「Teleport」や「Invisible」で接近し、\nゼロ距離で叩き込めばタンク系でも即死させられる。") {
			public void apply(Player p) { p.weapon.addEffect(new EffectShotgun()); }});

		all.add(new PowerUp("Self Regen", "自己再生", "防御時30%回復", "CD+2秒",
				"【解説】\nガードを発動するたびにHPを回復する。\n【運用】\nダメージを受けても、\n逃げてガードを繰り返せば立て直せる。\n生存率が大幅に上がるため、優先的に取得したい。") {
			public void apply(Player p) { p.hasSkillSelfRegen = true; }});

		all.add(new PowerUp("Build Up", "ビルドアップ", "威力1.3倍 被ダメ30%減", "速度30%減",
				"【解説】\n常時速度が落ちる代わりに、攻撃力と防御力が上がる。\n【運用】\n正面からの撃ち合い（ダメージレース）に特化した性能。\n鈍足になるため、被弾覚悟で殴り合う胆力が必要。") {
			public void apply(Player p) { p.hasPassiveBuildUp = true; p.weapon.addEffect(new EffectBuildUp()); p.applyPowerUpStats(); }});

		all.add(new PowerUp("The World", "\"世界\"", "防御時周囲弾消去", "CD+5秒",
				"【解説】\nガード発動の瞬間、周囲の敵弾を消滅させる。\n【運用】\n「Shower」や「Danmaku」などの\n弾幕への完全なカウンター。\n囲まれた際の緊急回避手段としても極めて優秀。") {
			public void apply(Player p) { p.hasSkillTheWorld = true; }});

		return all;
	}
}