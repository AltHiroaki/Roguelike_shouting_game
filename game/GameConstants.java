package game;

import java.awt.Color;

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

	// 衝突判定のバッファ (壁にめり込まないようにする余白)
	public static final double MAP_COLLISION_BUFFER = 2.0;

	// UI レイアウト
	public static final int UI_AMMO_Y_OFFSET = 30;
	public static final int UI_BAR_HEIGHT = 5;
	public static final int UI_BAR_HP_Y_OFFSET = -35;
	public static final int UI_BAR_RELOAD_Y_OFFSET = -45;
	public static final int UI_BAR_GUARD_Y_OFFSET = -55;
	public static final int UI_TEXT_POISON_Y_OFFSET = -40;

	public static final int UI_CARD_START_X = 100;
	public static final int UI_CARD_Y = 250;
	public static final int UI_CARD_WIDTH = 180;
	public static final int UI_CARD_HEIGHT = 250;
	public static final int UI_CARD_GAP = 20;

	// === ゲームルール設定 ===
	public static final int FPS = 60;
	public static final int MAX_WINS = 5;
	public static final int MAX_BULLETS = 500;
	public static final int COUNTDOWN_FRAMES = 90;

	// === プレイヤー基本設定 ===
	public static final int PLAYER_MAX_HP = 100;
	public static final double PLAYER_SPEED = 3.0;
	public static final int PLAYER_SIZE = 15;
	public static final int PLAYER_POISON_DURATION = 120;
	public static final int PLAYER_COLD_DURATION = 180;
	public static final int PLAYER_THIRST_DURATION = 180;
	public static final int PLAYER_CONFIDENCE_DURATION = 180;

	// === ガード設定 ===
	public static final int GUARD_DURATION = 60;
	public static final int GUARD_COOLDOWN = 300;
	public static final double GUARD_DAMAGE_CUT_RATE = 0.1;

	// テレポート距離
	public static final double SKILL_TELEPORT_DISTANCE = 150.0;

	// === 武器・弾丸 基本設定 ===
	public static final int WEAPON_DEFAULT_AMMO = 5;
	public static final int WEAPON_DEFAULT_RELOAD = 60;
	public static final int WEAPON_DEFAULT_DAMAGE = 20;
	public static final double WEAPON_DEFAULT_SPEED = 10.0;
	public static final int WEAPON_DEFAULT_SIZE = 8;

	// 修正: ゴースト弾の貫通有効時間 (1秒 = 60フレーム)
	public static final int GHOST_VALID_TIME = 30;

	// バースト射撃設定
	public static final int WEAPON_BURST_INTERVAL = 5;

	// === マップ生成用 ===
	public static final int MAP_GEN_MARGIN = 100;
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
	public static final int FLAG_COLD = 1 << 3;
	public static final int FLAG_GHOST = 1 << 4;

	public static final int BULLET_SAFE_TIME = 10;

	// カラーパレット
	public static final Color COLOR_BG = new Color(20, 25, 35);
	public static final Color COLOR_GRID = new Color(40, 50, 70);
	public static final Color COLOR_WALL = new Color(0, 255, 255);
	public static final Color COLOR_PLAYER_ME = new Color(100, 150, 255);
	public static final Color COLOR_PLAYER_ENEMY = new Color(255, 100, 100);
	public static final Color COLOR_TEXT = new Color(220, 220, 220);
	public static final Color COLOR_GUARD_SHIELD = new Color(0, 255, 255, 100);
	public static final Color COLOR_GUARD_COOLDOWN = new Color(100, 100, 255);

	//パワーアップ・スキル用定数

	// EffectHill
	public static final double POWERUP_HILL_DAMAGE_MULT = 0.6;

	// EffectRising
	public static final double POWERUP_RISING_SPEED_MULT = 2.0;
	public static final double POWERUP_RISING_DAMAGE_MULT = 0.8;

	// EffectImpactShot
	public static final double POWERUP_IMPACT_DAMAGE_MULT = 2.0;
	public static final double POWERUP_IMPACT_SPEED_MULT = 2.0;
	public static final double POWERUP_IMPACT_RELOAD_MULT = 1.5;
	public static final int POWERUP_IMPACT_INTERVAL_ADD = 60;

	// EffectDanmaku
	public static final int POWERUP_DANMAKU_PELLETS_ADD = 4;
	public static final double POWERUP_DANMAKU_RELOAD_MULT = 1.3;

	// EffectReelGun
	public static final int POWERUP_REEL_BURST_ADD = 2;
	public static final double POWERUP_REEL_RELOAD_MULT = 1.3;

	// EffectShower
	public static final int POWERUP_SHOWER_PELLETS_ADD = 14;
	public static final double POWERUP_SHOWER_RELOAD_MULT = 1.5;
	public static final double POWERUP_SHOWER_DAMAGE_MULT = 0.3;

	// EffectReflection
	public static final int POWERUP_REFLECT_BOUNCE_ADD = 2;
	public static final double POWERUP_REFLECT_DAMAGE_MULT = 1.1;
	public static final double POWERUP_REFLECT_RELOAD_MULT = 1.2;

	// EffectOutOfControl
	public static final int POWERUP_CONTROL_BOUNCE_ADD = 5;
	public static final double POWERUP_CONTROL_SPEED_MULT = 1.2;
	public static final double POWERUP_CONTROL_RELOAD_MULT = 1.3;

	// EffectIdaten
	public static final double POWERUP_IDATEN_SPEED_MULT = 2.0;
	public static final double POWERUP_IDATEN_DAMAGE_MULT = 0.6;
	public static final double POWERUP_IDATEN_HP_MULT = 0.8;

	// EffectColdShot
	public static final double POWERUP_COLD_RELOAD_MULT = 1.5;

	// Effect3in1
	public static final double POWERUP_3IN1_DAMAGE_MULT = 2.0;
	public static final double POWERUP_3IN1_SPEED_MULT = 2.0;
	public static final int POWERUP_3IN1_AMMO_SUB = 2;
	public static final double POWERUP_3IN1_RELOAD_MULT = 1.5;

	// EffectPoison / Ghost
	public static final double POWERUP_POISON_RELOAD_MULT = 1.25;
	public static final double POWERUP_GHOST_RELOAD_MULT = 1.25;

	// EffectQuickReload
	public static final double POWERUP_QUICK_RELOAD_MULT = 0.5;

	// EffectExtendedMag
	public static final int EFFECT_EXTMAG_AMOUNT = 5;

	// Tank / BigBoy
	public static final int POWERUP_TANK_HP_BONUS = 50;
	public static final double POWERUP_TANK_SIZE_MULT = 2.0;

	// Rogue / SmallBoy
	public static final int POWERUP_ROGUE_FIXED_HP = 50;
	public static final double POWERUP_ROGUE_SIZE_MULT = 0.5;
	public static final double POWERUP_ROGUE_SPEED_MULT = 1.3;

	// Skills
	public static final double SKILL_EXC_DEFENSE_HP_MULT = 1.3;
}