package game;

import java.awt.Color;

/**
 * ゲーム全体で使用する定数を管理するクラス。
 * 通信設定、UIレイアウト、パラメータ調整値をここに集約。
 */
public class GameConstants {
	// ==========================================
	// サーバー・通信設定
	// ==========================================
	/** 接続先サーバーの IPアドレス */
	public static final String SERVER_IP = "127.0.0.1";
	/** 接続先サーバーのポート番号 */
	public static final int SERVER_PORT = 10000;
	/** 参加するゲームの ID */
	public static final int TARGET_GAME_ID = 1;

	// ==========================================
	// マップ・描画設定
	// ==========================================
	public static final int MAP_X = 50;
	public static final int MAP_Y = 130; // 上部余白を確保してスコアを見やすく
	public static final int MAP_WIDTH = 960;
	public static final int MAP_HEIGHT = 540;

	// グリッドサイズ
	public static final int GRID_SIZE = 60;

	/** ゲーム全体で使用するフォント名 (Monospaced, SansSerif, Dialog, DialogInput 等) */
	public static final String FONT_NAME = "Monospaced";

	/** 衝突判定のバッファ (壁にめり込まないようにするための余白距離) */
	public static final double MAP_COLLISION_BUFFER = 2.0;

	// --- UI レイアウト座標オフセット ---
	public static final int UI_AMMO_Y_OFFSET = 30;
	public static final int UI_BAR_HEIGHT = 5;
	public static final int UI_BAR_HP_Y_OFFSET = -35;
	public static final int UI_BAR_RELOAD_Y_OFFSET = -45;
	public static final int UI_BAR_GUARD_Y_OFFSET = -55;
	public static final int UI_TEXT_POISON_Y_OFFSET = -40;

	public static final int UI_CARD_START_X = 240;
	public static final int UI_CARD_Y = 280; // 位置調整
	public static final int UI_CARD_WIDTH = 180;
	public static final int UI_CARD_HEIGHT = 250;
	public static final int UI_CARD_GAP = 20;

	// ==========================================
	// ゲームルール設定
	// ==========================================
	public static final int FPS = 60;
	public static final int MAX_WINS = 5;
	public static final int MAX_BULLETS = 1000; // ショットガン用に最大数を増加
	public static final int COUNTDOWN_FRAMES = 90;

	// ==========================================
	// プレイヤー基本パラメータ
	// ==========================================
	public static final int PLAYER_MAX_HP = 100;
	public static final double PLAYER_SPEED = 4.0; // マップ拡大に合わせて速度アップ
	public static final int PLAYER_SIZE = 15;

	// 状態異常の効果時間 (フレーム数)
	public static final int PLAYER_POISON_DURATION = 120;
	public static final int PLAYER_COLD_DURATION = 180;
	public static final int PLAYER_THIRST_DURATION = 180;
	public static final int PLAYER_CONFIDENCE_DURATION = 180;

	// ==========================================
	// ガード・スキル設定
	// ==========================================
	public static final int GUARD_DURATION = 60;
	public static final int GUARD_COOLDOWN = 300;
	/** ガード時のダメージ軽減率 (0.1 = 10%のダメージを受ける = 90%カット) */
	public static final double GUARD_DAMAGE_CUT_RATE = 0.1;

	/** テレポートスキルの移動距離 */
	public static final double SKILL_TELEPORT_DISTANCE = 150.0;
	public static final double SKILL_EXC_DEFENSE_HP_MULT = 1.3;

	// ==========================================
	// 武器・弾丸 基本設定
	// ==========================================
	public static final int WEAPON_DEFAULT_AMMO = 5;
	public static final int WEAPON_DEFAULT_RELOAD = 60;
	public static final int WEAPON_DEFAULT_DAMAGE = 15;
	public static final double WEAPON_DEFAULT_SPEED = 10.0;
	public static final int WEAPON_DEFAULT_SIZE = 8;
	public static final int WEAPON_BURST_INTERVAL = 5;

	/** ゴースト弾の壁貫通有効時間 (フレーム数) */
	public static final int GHOST_VALID_TIME = 20;
	/** 弾生成直後の自分への当たり判定無効時間 */
	public static final int BULLET_SAFE_TIME = 10;
	/** 弾のデフォルト寿命 (フレーム数, 10秒) */
	public static final int BULLET_DEFAULT_LIFE = 600;

	// ==========================================
	// マップ生成定数
	// ==========================================
	public static final int MAP_GEN_MARGIN = 120;
	public static final int MAP_C_CROSS_SIZE = 120;
	public static final int MAP_C_CORNER_MARGIN = 60;
	public static final int MAP_C_CORNER_SIZE = 60;

	// スポーン位置のマージン (壁埋まり防止)
	public static final int SPAWN_MARGIN = 180;

	// ==========================================
	// リソース・フラグ
	// ==========================================
	public static final String IMAGE_PATH_PLAYER_ME = "player_me.png";
	public static final String IMAGE_PATH_PLAYER_ENEMY = "player_enemy.png";

	// 弾の特殊効果ビットフラグ
	public static final int FLAG_NONE = 0;
	public static final int FLAG_HILL = 1 << 0;
	public static final int FLAG_BOUNCE = 1 << 1;
	public static final int FLAG_POISON = 1 << 2;
	public static final int FLAG_COLD = 1 << 3;
	public static final int FLAG_GHOST = 1 << 4;

	// プレイヤーの状態フラグ (MOVEコマンド送信用)
	public static final int P_FLAG_RELOAD    = 1;      // 0001
	public static final int P_FLAG_GUARD     = 2;      // 0010
	public static final int P_FLAG_INVISIBLE = 4;      // 0100
	public static final int P_FLAG_THE_WORLD = 8;      // 1000 ("世界"発動通知用)
	public static final int P_FLAG_POISON    = 16;     // 10000 (毒状態通知用)

	// カラー定義
	public static final Color COLOR_BG = new Color(20, 25, 35);
	public static final Color COLOR_GRID = new Color(40, 50, 70);
	public static final Color COLOR_WALL = new Color(0, 255, 255);
	public static final Color COLOR_PLAYER_ME = new Color(100, 150, 255);
	public static final Color COLOR_PLAYER_ENEMY = new Color(255, 100, 100);
	public static final Color COLOR_TEXT = new Color(220, 220, 220);
	public static final Color COLOR_GUARD_SHIELD = new Color(0, 255, 255, 100);
	public static final Color COLOR_GUARD_COOLDOWN = new Color(100, 100, 255);

	// ==========================================
	// パワーアップ・スキル効果値
	// ==========================================

	// EffectHill (吸血)
	public static final double POWERUP_HILL_DAMAGE_MULT = 0.6;

	// EffectRising (高速弾)
	public static final double POWERUP_RISING_SPEED_MULT = 2.0;
	public static final double POWERUP_RISING_DAMAGE_MULT = 0.8;

	// EffectPoison(毒)
	public static final double POWERUP_POISON_DAMAGE_MULT = 0.6;
	public static final int POWERUP_POISON_COUNT_MULT = 3;

	// EffectImpactShot (高威力)
	public static final double POWERUP_IMPACT_DAMAGE_MULT = 2.0;
	public static final double POWERUP_IMPACT_SPEED_MULT = 2.0;
	public static final double POWERUP_IMPACT_RELOAD_MULT = 1.5;
	public static final int POWERUP_IMPACT_INTERVAL_ADD = 60;

	// EffectDanmaku (弾幕)
	public static final int POWERUP_DANMAKU_PELLETS_ADD = 4;
	public static final double POWERUP_DANMAKU_RELOAD_MULT = 1.3;

	// EffectReelGun (3点バースト)
	public static final int POWERUP_REEL_BURST_ADD = 2;
	public static final double POWERUP_REEL_RELOAD_MULT = 1.3;
	public static final int POWERUP_REEL_INTERVAL_ADD = 30;


	// EffectShower (シャワー)
	public static final int POWERUP_SHOWER_PELLETS_ADD = 14;
	public static final double POWERUP_SHOWER_RELOAD_MULT = 1.5;
	public static final double POWERUP_SHOWER_DAMAGE_MULT = 0.3;

	// EffectReflection (反射)
	public static final int POWERUP_REFLECT_BOUNCE_ADD = 2;
	public static final double POWERUP_REFLECT_DAMAGE_MULT = 1.1;
	public static final double POWERUP_REFLECT_RELOAD_MULT = 1.2;

	// EffectOutOfControl (制御不能)
	public static final int POWERUP_CONTROL_BOUNCE_ADD = 5;
	public static final double POWERUP_CONTROL_SPEED_MULT = 1.2;
	public static final double POWERUP_CONTROL_RELOAD_MULT = 1.3;

	// EffectIdaten (韋駄天)
	public static final double POWERUP_IDATEN_SPEED_MULT = 1.5;
	public static final double POWERUP_IDATEN_DAMAGE_MULT = 0.6;
	public static final double POWERUP_IDATEN_HP_MULT = 0.5;

	// EffectColdShot (冷却)
	public static final double POWERUP_COLD_RELOAD_MULT = 1.5;

	// Effect3in1 (三位一体)
	public static final double POWERUP_3IN1_DAMAGE_MULT = 2.0;
	public static final double POWERUP_3IN1_SPEED_MULT = 2.0;
	public static final int POWERUP_3IN1_AMMO_SUB = 2;
	public static final double POWERUP_3IN1_RELOAD_MULT = 1.5;

	// EffectPoison / Ghost
	public static final double POWERUP_POISON_RELOAD_MULT = 1.25;
	public static final double POWERUP_GHOST_RELOAD_MULT = 1.25;

	// EffectQuickReload
	public static final double POWERUP_QUICK_RELOAD_MULT = 0.5;

	// Tank / BigBoy
	/** Big BoyのHP増加倍率 (1.5 = 150%) */
	public static final double POWERUP_TANK_HP_MULT = 1.5;
	public static final double POWERUP_TANK_SIZE_MULT = 2.0;

	// Rogue / SmallBoy (割合変更対応)
	/** Small BoyのHP減少倍率 (0.5 = 50%) */
	public static final double POWERUP_ROGUE_HP_MULT = 0.5;
	public static final double POWERUP_ROGUE_SIZE_MULT = 0.5;
	public static final double POWERUP_ROGUE_SPEED_MULT = 1.3;

	// 大容量 (Big Capacity)
	public static final int POWERUP_BIGCAP_AMMO_ADD = 3;
	public static final double POWERUP_BIGCAP_RELOAD_MULT = 0.9;

	// ショットガン (Shotgun)
	public static final int POWERUP_SHOTGUN_PELLETS_ADD = 20;
	public static final double POWERUP_SHOTGUN_DAMAGE_MULT = 1.1;
	public static final double POWERUP_SHOTGUN_RANGE = 150.0; // 射程距離
	public static final double POWERUP_SHOTGUN_SPREAD = Math.toRadians(45); // 拡散角度

	// 自己再生 (Self Regen)
	public static final double SKILL_REGEN_RATE = 0.3; // 30%
	public static final int SKILL_REGEN_CD_ADD = 120; // +2秒

	// ビルドアップ (Build Up)
	public static final double POWERUP_BUILDUP_DAMAGE_MULT = 1.3;
	public static final double POWERUP_BUILDUP_DEFENSE_RATE = 0.7; // 30% cut
	public static final double POWERUP_BUILDUP_SPEED_MULT = 0.7; // 30% down

	// "世界" (The World)
	public static final int SKILL_THE_WORLD_CD_ADD = 300; // +5秒
	public static final double SKILL_THE_WORLD_RANGE = 200.0;
}