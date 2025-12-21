package game;

import java.awt.Color;

/**
 * ゲーム全体で使用する定数クラス。
 * マジックナンバーを排除し、設定値を一元管理する。
 */
public class GameConstants {
	// === サーバー・通信設定 ===
	public static final String SERVER_IP = "127.0.0.1";
	public static final int SERVER_PORT = 10000;
	public static final int TARGET_GAME_ID = 1;

	// === マップ・描画設定 ===
	public static final int MAP_X = 50;
	public static final int MAP_Y = 80;
	public static final int MAP_WIDTH = 700;
	public static final int MAP_HEIGHT = 450;

	// UIレイアウト
	public static final int UI_AMMO_Y_OFFSET = 30; // 弾数表示のY位置オフセット
	public static final int UI_BAR_WIDTH = 40;     // HP/リロードバーの幅
	public static final int UI_BAR_HEIGHT = 5;     // HP/リロードバーの高さ
	public static final int UI_BAR_HP_Y_OFFSET = -35;
	public static final int UI_BAR_RELOAD_Y_OFFSET = -45;
	public static final int UI_TEXT_POISON_Y_OFFSET = -40;

	// パワーアップ選択画面のレイアウト
	public static final int UI_CARD_START_X = 100;
	public static final int UI_CARD_Y = 250;
	public static final int UI_CARD_WIDTH = 180;
	public static final int UI_CARD_HEIGHT = 250;
	public static final int UI_CARD_GAP = 20;

	// === ゲームルール設定 ===
	public static final int FPS = 60;
	public static final int MAX_WINS = 3;
	public static final int MAX_BULLETS = 500;
	public static final int COUNTDOWN_FRAMES = 90; // 1.5秒 (60fps * 1.5)

	// === プレイヤー基本設定 ===
	public static final int PLAYER_MAX_HP = 100;
	public static final double PLAYER_SPEED = 3.0;
	public static final int PLAYER_SIZE = 15;
	public static final int PLAYER_POISON_DURATION = 180; // 3秒
	public static final double PLAYER_POISON_SLOW_RATE = 0.5;
	public static final int PLAYER_HEAL_AMOUNT = 5;

	// === 武器・弾丸 基本設定 ===
	public static final int WEAPON_DEFAULT_AMMO = 5;
	public static final int WEAPON_DEFAULT_RELOAD = 60; // 1秒
	public static final int WEAPON_DEFAULT_DAMAGE = 20;
	public static final double WEAPON_DEFAULT_SPEED = 10.0;
	public static final int WEAPON_DEFAULT_SIZE = 8;

	// バースト射撃設定
	public static final int WEAPON_BURST_COUNT = 3;
	public static final int WEAPON_BURST_INTERVAL = 5;

	// 3方向射撃の角度
	public static final double WEAPON_TRI_ANGLE_OFFSET = 0.3;

	// === パワーアップ・エフェクト倍率設定 ===
	// Hill (Vampire)
	public static final double EFFECT_HILL_DAMAGE_MULT = 0.9;

	// Bounce
	public static final double EFFECT_BOUNCE_SPEED_MULT = 0.8;
	public static final double EFFECT_BOUNCE_RELOAD_MULT = 1.2;

	// Tri-Shot
	public static final double EFFECT_TRI_RELOAD_MULT = 1.5;

	// Rising
	public static final double EFFECT_RISING_SPEED_MULT = 2.0;
	public static final double EFFECT_RISING_DAMAGE_MULT = 0.7;

	// Impact
	public static final double EFFECT_IMPACT_DAMAGE_MULT = 2.0;
	public static final double EFFECT_IMPACT_RELOAD_MULT = 1.3;
	public static final int EFFECT_IMPACT_AMMO_REDUCTION = 2; // 弾数減少量

	// Big Ball
	public static final int EFFECT_BIG_SIZE_MULT = 3;
	public static final double EFFECT_BIG_DAMAGE_MULT = 0.8;
	public static final double EFFECT_BIG_SPEED_MULT = 0.8;

	// Poison
	public static final double EFFECT_POISON_RELOAD_MULT = 1.1;

	// Burst
	public static final double EFFECT_BURST_RELOAD_MULT = 1.2;

	// Extended Mag
	public static final int EFFECT_EXTMAG_AMOUNT = 5; // 弾数増加量

	// Big Boy (Tank)
	public static final int POWERUP_TANK_HP_BONUS = 50;
	public static final double POWERUP_TANK_SIZE_MULT = 1.8;

	// Small Boy (Rogue)
	public static final int POWERUP_ROGUE_FIXED_HP = 70;
	public static final double POWERUP_ROGUE_SIZE_MULT = 0.6;
	public static final double POWERUP_ROGUE_SPEED_MULT = 1.3;

	// === マップ生成用パラメータ ===
	public static final int MAP_GEN_MARGIN = 100;
	public static final int MAP_GEN_MIN_LEN = 50;
	public static final int MAP_GEN_LEN_RANGE = 100;
	public static final int MAP_GEN_OBSTACLE_COUNT = 8;

	// マップC（要塞）用
	public static final int MAP_C_CROSS_SIZE = 100;
	public static final int MAP_C_CORNER_MARGIN = 80;
	public static final int MAP_C_CORNER_SIZE = 50;

	// 画像ファイルパス
	public static final String IMAGE_PATH_PLAYER_ME = "player_me.png";
	public static final String IMAGE_PATH_PLAYER_ENEMY = "player_enemy.png";

	// 弾の特殊効果フラグ
	public static final int FLAG_NONE = 0;
	public static final int FLAG_HILL = 1 << 0;
	public static final int FLAG_BOUNCE = 1 << 1;
	public static final int FLAG_POISON = 1 << 2;

	// 弾の寿命設定
	public static final int BULLET_SAFE_TIME = 10; // 発射直後の無敵時間（フレーム）

	// === デザイン用カラーパレット（ネオン風） ===
	public static final Color COLOR_BG = new Color(20, 25, 35);
	public static final Color COLOR_GRID = new Color(40, 50, 70);
	public static final Color COLOR_WALL = new Color(0, 255, 255);
	public static final Color COLOR_PLAYER_ME = new Color(100, 150, 255);
	public static final Color COLOR_PLAYER_ENEMY = new Color(255, 100, 100);
	public static final Color COLOR_TEXT = new Color(220, 220, 220);
}