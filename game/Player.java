package game;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.io.PrintWriter;
import static game.GameConstants.*;

/**
 * プレイヤー（自キャラおよび敵キャラ）の状態を管理するクラス。
 * 座標、HP、スキル、バフ/デバフ状態、武器などを保持します。
 */
public class Player {
	public int id;
	public double x, y, angle;
	public int hp = PLAYER_MAX_HP;
	public int maxHp = PLAYER_MAX_HP;
	public Color color;
	public double speed = PLAYER_SPEED;
	public int size = PLAYER_SIZE;

	// --- 状態異常タイマー ---
	public int poisonTimer = 0;
	public int coldTimer = 0;
	public int thirstTimer = 0;
	public int confidenceTimer = 0;

	// --- ガード・防御関連 ---
	public boolean isGuarding = false;
	public int guardTimer = 0;
	public int guardCooldownTimer = 0;

	// --- 所持スキル ---
	public boolean hasSkillTacticalReload = false;
	public boolean hasSkillExclusiveDefense = false;
	public boolean hasSkillInvisible = false;
	public boolean hasSkillEmergencyDefense = false;
	public boolean hasSkillTeleport = false;

	// 新規スキル
	public boolean hasSkillSelfRegen = false; // 自己再生
	public boolean hasSkillTheWorld = false;  // "世界"

	// --- 所持パッシブ ---
	public boolean hasPassiveThirst = false;
	public boolean hasPassiveDelay = false;
	public boolean hasPassiveConfidence = false;

	// 新規パッシブ
	public boolean hasPassiveBuildUp = false; // ビルドアップ

	// --- スキル用内部タイマー・バッファ ---
	public double delayDamageBuffer = 0;
	public int invisibleTimer = 0;
	public int exclusiveDefenseTimer = 0;

	public Weapon weapon;

	/**
	 * コンストラクタ
	 * @param id プレイヤー ID
	 * @param x 初期 X 座標
	 * @param y 初期 Y 座標
	 * @param c プレイヤーカラー
	 */
	public Player(int id, double x, double y, Color c) {
		this.id = id;
		this.x = x;
		this.y = y;
		this.color = c;
		this.weapon = new Weapon(this);
	}

	/**
	 * ラウンド開始時に状態をリセットします。
	 * HPの全回復、タイマーの初期化、武器のリセットを行います。
	 */
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

	/**
	 * 現在の武器エフェクトやスキルに基づいてステータス（HP, サイズ, 速度）を再計算・適用します。
	 */
	public void applyPowerUpStats() {
		maxHp = PLAYER_MAX_HP;
		size = PLAYER_SIZE;
		speed = PLAYER_SPEED;

		for(WeaponEffect e : weapon.effects) {
			if(e instanceof EffectBigBoy) {
				maxHp = (int)(maxHp * POWERUP_TANK_HP_MULT);
				size *= POWERUP_TANK_SIZE_MULT;
			}
			if(e instanceof EffectSmallBoy) {
				maxHp = (int)(maxHp * POWERUP_ROGUE_HP_MULT);
				size *= POWERUP_ROGUE_SIZE_MULT;
				speed *= POWERUP_ROGUE_SPEED_MULT;
			}
			if(e instanceof EffectIdaten) {
				maxHp = (int)(maxHp * POWERUP_IDATEN_HP_MULT);
				speed *= POWERUP_IDATEN_SPEED_MULT;
			}
		}

		if(hasSkillExclusiveDefense) {
			maxHp = (int)(maxHp * SKILL_EXC_DEFENSE_HP_MULT);
		}

		// ビルドアップ (常時30%速度減)
		if(hasPassiveBuildUp) {
			speed *= POWERUP_BUILDUP_SPEED_MULT;
		}

		// HPが上限を超えていた場合の調整
		if(hp > maxHp) hp = maxHp;
	}

	/**
	 * プレイヤーのガードを試みます。
	 * クールダウン中でなければガード状態に移行し、関連スキルの発動処理も行います。
	 * @param obstacles テレポートスキル時の壁判定用
	 */
	public void tryGuard(ArrayList<Line2D.Double> obstacles) {
		if (guardCooldownTimer <= 0 && !isGuarding) {
			isGuarding = true;
			guardTimer = GUARD_DURATION;
			int cooldownAdd = 0;

			// 各種ガード連動スキルの発動
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
				teleport(obstacles);
				cooldownAdd += 120;
			}
			// 自己再生: HP回復 + CD2秒
			if (hasSkillSelfRegen) {
				int heal = (int)(maxHp * SKILL_REGEN_RATE);
				hp = Math.min(hp + heal, maxHp);
				cooldownAdd += SKILL_REGEN_CD_ADD;
			}
			// "世界": CD+5秒
			if (hasSkillTheWorld) {
				cooldownAdd += SKILL_THE_WORLD_CD_ADD;
			}

			guardCooldownTimer = GUARD_COOLDOWN + cooldownAdd;
		}
	}

	/**
	 * クールダウンを無視して強制的にガードを発動します（緊急防御スキル用）。
	 * @param obstacles テレポートスキル時の壁判定用
	 */
	public void forceGuard(ArrayList<Line2D.Double> obstacles) {
		if (isGuarding) return;

		isGuarding = true;
		guardTimer = GUARD_DURATION;

		int cooldownAdd = 0;
		// スキル効果の発動（通常ガードと同様）
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
			teleport(obstacles);
			cooldownAdd += 120;
		}
		if (hasSkillSelfRegen) {
			int heal = (int)(maxHp * SKILL_REGEN_RATE);
			hp = Math.min(hp + heal, maxHp);
			cooldownAdd += SKILL_REGEN_CD_ADD;
		}
		if (hasSkillTheWorld) {
			cooldownAdd += SKILL_THE_WORLD_CD_ADD;
		}

		guardCooldownTimer = GUARD_COOLDOWN + cooldownAdd;
	}

	/**
	 * 進行方向に向かって一定距離テレポートします。
	 * @param obstacles 壁情報のリスト
	 */
	private void teleport(ArrayList<Line2D.Double> obstacles) {
		double dist = SKILL_TELEPORT_DISTANCE;
		double tx = x + Math.cos(angle) * dist;
		double ty = y + Math.sin(angle) * dist;

		// 1. マップ範囲内にクランプ (壁の内側に入るようにsizeを考慮)
		if (tx < MAP_X + size) tx = MAP_X + size;
		if (tx > MAP_X + MAP_WIDTH - size) tx = MAP_X + MAP_WIDTH - size;
		if (ty < MAP_Y + size) ty = MAP_Y + size;
		if (ty > MAP_Y + MAP_HEIGHT - size) ty = MAP_Y + MAP_HEIGHT - size;

		// 2. 移動先が壁の中かどうかチェック
		// 移動先が壁と干渉しなければ移動を確定
		if (!checkWall(tx, ty, obstacles)) {
			x = tx;
			y = ty;
		}
		// 壁の中になる場合は移動しない（スタック防止）
	}

	/**
	 * プレイヤーの毎フレームの更新処理。
	 * 移動、状態異常の処理、サーバーへの位置情報送信などを行います。
	 * @param keyW Wキー入力状態
	 * @param keyS Sキー入力状態
	 * @param keyA Aキー入力状態
	 * @param keyD Dキー入力状態
	 * @param mx マウスX座標
	 * @param my マウスY座標
	 * @param obstacles 壁情報
	 * @param out サーバー出力用Writer
	 */
	public void update(boolean keyW, boolean keyS, boolean keyA, boolean keyD, int mx, int my, ArrayList<Line2D.Double> obstacles, PrintWriter out) {
		// ガード状態の更新
		if (isGuarding) {
			guardTimer--;
			if (guardTimer <= 0) isGuarding = false;
		}
		if (guardCooldownTimer > 0) guardCooldownTimer--;

		// 専守防衛（遅延ガード）の処理
		if (exclusiveDefenseTimer > 0) {
			exclusiveDefenseTimer--;
			if (exclusiveDefenseTimer == 0) {
				isGuarding = true;
				guardTimer = GUARD_DURATION;
			}
		}

		// 透明化タイマーの更新
		if (invisibleTimer > 0) invisibleTimer--;

		// ディレイパッシブ（DoTダメージ）の処理
		if (hasPassiveDelay && delayDamageBuffer > 0) {
			if(hp > 0 && GameLogic.frameCount % 20 == 0) {
				int bleed = (int)(delayDamageBuffer * 0.1) + 1;
				hp -= bleed;
				delayDamageBuffer -= bleed;
				if(delayDamageBuffer < 0) delayDamageBuffer = 0;
			}
		}

		// 状態異常処理
		if (poisonTimer > 0) {
			if(GameLogic.frameCount % 30 == 0) hp--;
			poisonTimer--;
		}
		if (coldTimer > 0) coldTimer--;
		if (thirstTimer > 0) thirstTimer--;
		if (confidenceTimer > 0) {
			confidenceTimer--;
			if(confidenceTimer == 0) {
				// 自信過剰の効果終了時、HPを1/3にする
				hp = (int)Math.ceil(hp / 3.0);
			}
		}

		// 移動速度の計算
		double currentSpeed = speed;
		if (coldTimer > 0) currentSpeed *= 0.5;
		if (thirstTimer > 0) currentSpeed *= 1.3;

		// 移動処理（X軸）
		double nextX = x, nextY = y;
		if (keyA) nextX -= currentSpeed;
		if (keyD) nextX += currentSpeed;
		if (!checkWall(nextX, y, obstacles)) x = nextX;

		// 移動処理（Y軸）
		if (keyW) nextY -= currentSpeed;
		if (keyS) nextY += currentSpeed;
		if (!checkWall(x, nextY, obstacles)) y = nextY;

		// 向きの計算
		angle = Math.atan2(my - y, mx - x);

		// サーバーへ状態を送信
		out.println("MOVE " + (int)x + " " + (int)y + " " + angle + " " + hp
				+ " " + weapon.isReloading + " " + weapon.reloadTimer
				+ " " + isGuarding + " " + guardCooldownTimer + " " + (invisibleTimer > 0) + " " + id);

		weapon.update(out, id, obstacles);
	}

	/**
	 * 指定した座標が壁やマップ境界と干渉するか判定します。
	 * @param tx チェックするX座標
	 * @param ty チェックするY座標
	 * @param walls 壁情報のリスト
	 * @return 干渉する場合は true
	 */
	private boolean checkWall(double tx, double ty, ArrayList<Line2D.Double> walls) {
		double checkSize = size + MAP_COLLISION_BUFFER;

		// マップ境界チェック
		if (tx < MAP_X + checkSize || tx > MAP_X + MAP_WIDTH - checkSize) return true;
		if (ty < MAP_Y + checkSize || ty > MAP_Y + MAP_HEIGHT - checkSize) return true;

		// 障害物チェック
		for (Line2D.Double w : walls) {
			if (w.ptSegDist(tx, ty) < checkSize) return true;
		}
		return false;
	}

	/**
	 * プレイヤーの矩形（当たり判定用）を取得します。
	 */
	public Rectangle getBounds() {
		return new Rectangle((int)x - size, (int)y - size, size * 2, size * 2);
	}

	/**
	 * プレイヤーの描画処理。
	 * HPバー、リロードバー、本体などを描画します。
	 */
	public void draw(Graphics2D g2d, BufferedImage imgMe, BufferedImage imgEnemy, int myId) {
		// 敵で、かつ透明化中の場合は描画しない
		if (id != myId && invisibleTimer > 0) return;

		AffineTransform old = g2d.getTransform();
		g2d.translate(x, y);

		// HPバーの描画
		g2d.setColor(Color.RED); g2d.fillRect(-20, UI_BAR_HP_Y_OFFSET, 40, UI_BAR_HEIGHT);
		g2d.setColor(Color.GREEN);
		int barWidth = (int)(40 * (hp / (double)maxHp));
		if (barWidth > 40) barWidth = 40; if (barWidth < 0) barWidth = 0;
		g2d.fillRect(-20, UI_BAR_HP_Y_OFFSET, barWidth, UI_BAR_HEIGHT);

		// リロードバーの描画
		if (weapon.isReloading) {
			g2d.setColor(Color.GRAY); g2d.fillRect(-20, UI_BAR_RELOAD_Y_OFFSET, 40, UI_BAR_HEIGHT);
			g2d.setColor(Color.YELLOW);
			double progress = (double)weapon.reloadTimer / weapon.reloadDuration;
			if (progress > 1.0) progress = 1.0;
			g2d.fillRect(-20, UI_BAR_RELOAD_Y_OFFSET, (int)(40 * progress), UI_BAR_HEIGHT);
		}

		// ガードクールダウンバーの描画
		if (guardCooldownTimer > 0) {
			g2d.setColor(Color.GRAY);
			g2d.fillRect(-20, UI_BAR_GUARD_Y_OFFSET, 40, UI_BAR_HEIGHT);
			g2d.setColor(COLOR_GUARD_COOLDOWN);
			double progress = 1.0 - ((double)guardCooldownTimer / GUARD_COOLDOWN);
			if (progress < 0) progress = 0;
			g2d.fillRect(-20, UI_BAR_GUARD_Y_OFFSET, (int)(40 * progress), UI_BAR_HEIGHT);
		}

		// ガードシールドの描画
		if (isGuarding) {
			g2d.setColor(COLOR_GUARD_SHIELD);
			g2d.fillOval(-size - 5, -size - 5, (size * 2) + 10, (size * 2) + 10);
		}

		g2d.rotate(angle);

		// キャラクター画像の描画
		BufferedImage img = (id == myId) ? imgMe : imgEnemy;
		if (img != null) {
			if(invisibleTimer > 0) {
				// 自分の透明化中は半透明で表示
				Composite c = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f);
				g2d.setComposite(c);
				g2d.drawImage(img, -size, -size, size * 2, size * 2, null);
				g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
			} else {
				g2d.drawImage(img, -size, -size, size * 2, size * 2, null);
			}
		} else {
			// 画像がない場合のフォールバック描画
			g2d.setColor(color);
			g2d.fillOval(-size, -size, size * 2, size * 2);
			g2d.setColor(Color.BLACK);
			g2d.drawLine(0, 0, size + 10, 0);
		}

		// 状態異常エフェクトの描画
		if (poisonTimer > 0) {
			g2d.setColor(new Color(128, 0, 128, 100));
			g2d.fillOval(-size, -size, size * 2, size * 2);
		}
		if (coldTimer > 0) {
			g2d.setColor(new Color(0, 255, 255, 100));
			g2d.fillOval(-size, -size, size * 2, size * 2);
		}

		g2d.setTransform(old);
	}

	/**
	 * サーバーへ現在のステータス（HP最大値など）を送信します。
	 */
	public void sendStatus(PrintWriter out) {
		out.println("STATUS " + id + " " + maxHp + " " + size);
	}
}