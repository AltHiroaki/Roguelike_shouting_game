package game;

import java.awt.*;
import static game.GameConstants.*;

/**
 * ゲーム内の弾丸を表すクラス。
 * インスタンスは生成・破棄を繰り返さず、
 * オブジェクトプール（GameLogic.bulletPool）によって再利用。
 */
public class Bullet {
	// --- 状態フラグ ---
	/** この弾丸が現在使用中（画面内に存在）かどうか */
	public boolean isActive = false;

	// --- 基本パラメータ ---
	public int id;
	public int ownerId; // 撃ったプレイヤーのID
	public int damage;
	public int size;
	public int typeFlag; // 特殊効果フラグ（毒、反射など）

	// --- 物理パラメータ ---
	public double x, y;
	public double angle;
	public double speed;

	// --- 寿命・反射管理 ---
	public int bounceCount = 0;
	public int maxBounces = 0; // 反射回数上限
	public int lifeTimer = 0;  // 生成されてからの経過フレーム数

	// 寿命上限
	public int maxLife = BULLET_DEFAULT_LIFE;

	public Bullet() {}

	/**
	 * 弾丸を初期化して有効化（発射）します。
	 * GameLogicのspawnBulletから呼び出されます。
	 * @param id 弾丸の一意なID
	 * @param x 初期X座標
	 * @param y 初期Y座標
	 * @param a 進行角度（ラジアン）
	 * @param s 移動速度
	 * @param d ダメージ量
	 * @param sz 描画サイズ
	 * @param f 特殊効果フラグ
	 * @param o 発射したプレイヤーのID
	 */
	public void activate(int id, double x, double y, double a, double s, int d, int sz, int f, int o, int ml) {
		this.id = id;
		this.x = x;
		this.y = y;
		this.angle = a;
		this.speed = s;
		this.damage = d;
		this.size = sz;
		this.typeFlag = f;
		this.ownerId = o;
		this.maxLife = ml;
		this.isActive = true;

		// カウンタのリセット
		this.bounceCount = 0;
		this.lifeTimer = 0;
		this.maxBounces = 0;
	}

	/**
	 * 弾丸を無効化し、プールに戻します。
	 */
	public void deactivate() {
		isActive = false;
	}

	/**
	 * 弾丸の毎フレームの更新処理。
	 * 位置の更新と寿命タイマーのカウントアップを行います。
	 */
	public void update() {
		x += Math.cos(angle) * speed;
		y += Math.sin(angle) * speed;
		lifeTimer++;
		// 寿命が尽きたら消える
		if (lifeTimer >= maxLife) {
			deactivate();
		}
	}

	/**
	 * 弾丸の描画処理。
	 * 特殊効果フラグに応じて色を変化させます。
	 * @param g2d Graphics2Dオブジェクト
	 */
	public void draw(Graphics2D g2d) {
		if (!isActive) return;

		// デフォルト色
		g2d.setColor(Color.YELLOW);

		// 特殊効果による色のオーバーライド
		if ((typeFlag & FLAG_POISON) != 0) g2d.setColor(Color.MAGENTA); // 毒
		if ((typeFlag & FLAG_COLD) != 0) g2d.setColor(Color.CYAN);    // 冷却
		if ((typeFlag & FLAG_GHOST) != 0) g2d.setColor(new Color(255, 255, 255, 150)); // ゴースト（半透明）

		// 中心座標に合わせて描画
		g2d.fillOval((int)x - size / 2, (int)y - size / 2, size, size);
	}
}