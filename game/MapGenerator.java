package game;

import java.awt.geom.Line2D;
import java.util.ArrayList;
import static game.GameConstants.*;

/**
 * ゲームマップの生成を行うユーティリティクラス。
 * 選択されたタイプに基づいて壁（障害物）のリストを生成して返します。
 */
public class MapGenerator {

	// --- マップタイプ定数 ---
	public static final int MAP_TYPE_C = 0; // 要塞
	public static final int MAP_TYPE_A = 1; // 平原
	public static final int MAP_TYPE_B = 2; // 通路

	/**
	 * 指定されたタイプのマップデータを生成します。
	 * @param type マップタイプ (0:要塞, 1:平原, 2:通路)
	 * @return 壁オブジェクト(Line2D.Double)のリスト
	 */
	public static ArrayList<Line2D.Double> generate(int type) {
		ArrayList<Line2D.Double> obstacles = new ArrayList<>();

		if (type == MAP_TYPE_A) {
			// === マップA: 平原 ===
			// 中央に小さな遮蔽物と、コーナー付近にL字壁を配置
			int cx = MAP_X + MAP_WIDTH / 2;
			int cy = MAP_Y + MAP_HEIGHT / 2;
			int len = 150;

			// 中央十字のような配置（隙間あり）
			obstacles.add(new Line2D.Double(cx - len, cy, cx - 30, cy));
			obstacles.add(new Line2D.Double(cx + 30, cy, cx + len, cy));
			obstacles.add(new Line2D.Double(cx, cy - len, cx, cy - 30));
			obstacles.add(new Line2D.Double(cx, cy + 30, cx, cy + len));

			// 左右の遮蔽物
			obstacles.add(new Line2D.Double(MAP_X + 100, MAP_Y + 100, MAP_X + 200, MAP_Y + 100));
			obstacles.add(new Line2D.Double(MAP_X + MAP_WIDTH - 200, MAP_Y + MAP_HEIGHT - 100, MAP_X + MAP_WIDTH - 100, MAP_Y + MAP_HEIGHT - 100));

		} else if (type == MAP_TYPE_B) {
			// === マップB: 通路 ===
			// 横長の通路を形成するような壁配置
			for (int i = 1; i <= 3; i++) {
				int y = MAP_Y + (MAP_HEIGHT / 4) * i;
				double gap = 100; // 中央を通れるように隙間を開ける
				obstacles.add(new Line2D.Double(MAP_X + 50, y, MAP_X + (double) MAP_WIDTH /2 - gap /2, y));
				obstacles.add(new Line2D.Double(MAP_X + (double) MAP_WIDTH /2 + gap/2, y, MAP_X + MAP_WIDTH - 50, y));
			}
			// 縦の遮蔽物
			obstacles.add(new Line2D.Double(MAP_X + (double) MAP_WIDTH /2, MAP_Y + 50, MAP_X + (double) MAP_WIDTH /2, MAP_Y + 150));
			obstacles.add(new Line2D.Double(MAP_X + (double) MAP_WIDTH /2, MAP_Y + MAP_HEIGHT - 150, MAP_X + (double) MAP_WIDTH /2, MAP_Y + MAP_HEIGHT - 50));

		} else {
			// === マップC: 要塞 (デフォルト) ===
			// 中央に大きな十字壁と、四隅にL字のコーナー壁
			int cx = MAP_X + MAP_WIDTH / 2;
			int cy = MAP_Y + MAP_HEIGHT / 2;

			// 中央の十字壁
			obstacles.add(new Line2D.Double(cx - MAP_C_CROSS_SIZE, cy, cx + MAP_C_CROSS_SIZE, cy));
			obstacles.add(new Line2D.Double(cx, cy - MAP_C_CROSS_SIZE, cx, cy + MAP_C_CROSS_SIZE));

			// 四隅のコーナー壁
			int margin = MAP_C_CORNER_MARGIN;
			int size = MAP_C_CORNER_SIZE;

			addCorner(obstacles, MAP_X + margin, MAP_Y + margin, size, 1, 1);
			addCorner(obstacles, MAP_X + MAP_WIDTH - margin, MAP_Y + margin, size, -1, 1);
			addCorner(obstacles, MAP_X + margin, MAP_Y + MAP_HEIGHT - margin, size, 1, -1);
			addCorner(obstacles, MAP_X + MAP_WIDTH - margin, MAP_Y + MAP_HEIGHT - margin, size, -1, -1);
		}
		return obstacles;
	}

	/**
	 * 四隅にL字型の壁を追加するためのヘルパーメソッド。
	 * @param list 追加先のリスト
	 * @param x 起点X座標
	 * @param y 起点Y座標
	 * @param size 壁の長さ
	 * @param dx X方向の向き (1 or -1)
	 * @param dy Y方向の向き (1 or -1)
	 */
	private static void addCorner(ArrayList<Line2D.Double> list, int x, int y, int size, int dx, int dy) {
		list.add(new Line2D.Double(x, y, x + size * dx, y));
		list.add(new Line2D.Double(x, y, x, y + size * dy));
	}
}