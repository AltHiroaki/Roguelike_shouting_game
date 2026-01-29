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
	public static final int MAP_TYPE_C = 0; // 要塞 (Fortress)
	public static final int MAP_TYPE_A = 1; // 平原 (Plains / Tactical)
	public static final int MAP_TYPE_B = 2; // 通路 (Corridor / Maze)

	/**
	 * 指定されたタイプのマップデータを生成します。
	 * @param type マップタイプ (0:要塞, 1:平原, 2:通路)
	 * @return 壁オブジェクト(Line2D.Double)のリスト
	 */
	public static ArrayList<Line2D.Double> generate(int type) {
		ArrayList<Line2D.Double> obstacles = new ArrayList<>();

		int cx = MAP_X + MAP_WIDTH / 2;
		int cy = MAP_Y + MAP_HEIGHT / 2;

		// グリッドサイズ
		int g = GRID_SIZE;

		if (type == MAP_TYPE_A) {
			// === マップA (平原) ===

			// 中央左寄りの遮蔽壁 (縦)
			obstacles.add(new Line2D.Double(cx - 150, cy - 100, cx - 150, cy + 100));

			// 中央右寄りの遮蔽壁 (縦)
			obstacles.add(new Line2D.Double(cx + 150, cy - 100, cx + 150, cy + 100));

			// 中央上下の小さな遮蔽 (横)
			obstacles.add(new Line2D.Double(cx - 50, cy - 150, cx + 50, cy - 150));
			obstacles.add(new Line2D.Double(cx - 50, cy + 150, cx + 50, cy + 150));

			// 左右のボックス(グリッド合わせ) - 元のコードから維持
			// グリッドに合わせて配置: X軸は左端/右端から3グリッド分
			addBox(obstacles, MAP_X + g*3 + g/2, MAP_Y + g*3 + g/2, g/2);
			addBox(obstacles, MAP_X + MAP_WIDTH - (g*3 + g/2), MAP_Y + MAP_HEIGHT - (g*3 + g/2), g/2);

		} else if (type == MAP_TYPE_B) {
			// === マップB (通路) ===

			double gap = 160;

			// 中央通路の壁 (縦)
			obstacles.add(new Line2D.Double(cx - gap, MAP_Y, cx - gap, cy + 50));
			obstacles.add(new Line2D.Double(cx + gap, MAP_Y, cx + gap, cy + 50));

			// 中央の仕切り (縦)
			obstacles.add(new Line2D.Double(cx, cy - 50, cx, MAP_Y + MAP_HEIGHT));

			// 横方向の遮蔽 (横)
			obstacles.add(new Line2D.Double(MAP_X, cy, MAP_X + 150, cy));
			obstacles.add(new Line2D.Double(MAP_X + MAP_WIDTH - 150, cy, MAP_X + MAP_WIDTH, cy));

		} else {
			// === マップC (要塞) ===
			// addRectを使用しているため垂直・水平は保証されている

			addRect(obstacles, cx, cy, 120, 90);
			// 周囲の小さい柱 (1マス x 1マス)
			int offX = g * 4 + g/2;
			int offY = g * 2;
			// 半径30 (サイズ60x60 = 1マス)
			int r = 30;
			addRect(obstacles, cx - offX, cy - offY, r, r); // 左上
			addRect(obstacles, cx + offX, cy - offY, r, r); // 右上
			addRect(obstacles, cx - offX, cy + offY, r, r); // 左下
			addRect(obstacles, cx + offX, cy + offY, r, r); // 右下

			// コーナー付近の追加遮蔽 (グリッド線上に配置)
			obstacles.add(new Line2D.Double(MAP_X, MAP_Y + g*2, MAP_X + g, MAP_Y + g*2));
			obstacles.add(new Line2D.Double(MAP_X + MAP_WIDTH, MAP_Y + MAP_HEIGHT - g*2, MAP_X + MAP_WIDTH - g, MAP_Y + MAP_HEIGHT - g*2));
		}
		return obstacles;
	}

	/**
	 * 正方形を追加するヘルパーメソッド
	 */
	private static void addBox(ArrayList<Line2D.Double> list, int x, int y, int radius) {
		addRect(list, x, y, radius, radius);
	}

	/**
	 * 指定した中心座標に長方形の壁を追加するヘルパーメソッド。
	 * 上下左右の4本の直線（垂直・水平）を追加します。
	 * @param list 追加先のリスト
	 * @param x 中心のX座標
	 * @param y 中心のY座標
	 * @param rx X方向の半径（幅の半分）
	 * @param ry Y方向の半径（高さの半分）
	 */
	private static void addRect(ArrayList<Line2D.Double> list, int x, int y, int rx, int ry) {
		// 上 (水平)
		list.add(new Line2D.Double(x - rx, y - ry, x + rx, y - ry));
		// 右 (垂直)
		list.add(new Line2D.Double(x + rx, y - ry, x + rx, y + ry));
		// 下 (水平)
		list.add(new Line2D.Double(x + rx, y + ry, x - rx, y + ry));
		// 左 (垂直)
		list.add(new Line2D.Double(x - rx, y + ry, x - rx, y - ry));
	}
}