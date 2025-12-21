package game;

public class GameConstants {
	// サーバー設定
	public static final String SERVER_IP = "127.0.0.1";
	public static final int SERVER_PORT = 10000;
	public static final int TARGET_GAME_ID = 1;

	// マップ設定
	public static final int MAP_X = 50;
	public static final int MAP_Y = 80;
	public static final int MAP_WIDTH = 700;
	public static final int MAP_HEIGHT = 450;

	// ゲーム設定
	public static final int FPS = 60;
	public static final int MAX_WINS = 3;
	public static final int MAX_BULLETS = 500;

	// プレイヤー設定
	public static final int PLAYER_MAX_HP = 100;
	public static final double PLAYER_SPEED = 3.0;
	public static final int PLAYER_SIZE = 15;

	// 画像パス
	public static final String IMAGE_PATH_PLAYER_ME = "player_me.png";
	public static final String IMAGE_PATH_PLAYER_ENEMY = "player_enemy.png";

	// フラグ（ビット演算用）
	public static final int FLAG_NONE = 0;
	public static final int FLAG_HILL = 1 << 0;
	public static final int FLAG_BOUNCE = 1 << 1;
	public static final int FLAG_POISON = 1 << 2;
}