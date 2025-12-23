package game;

import java.awt.geom.Line2D;
import java.util.ArrayList;
import static game.GameConstants.*;

public class MapGenerator {

	public static final int MAP_TYPE_C = 0;
	public static final int MAP_TYPE_A = 1;
	public static final int MAP_TYPE_B = 2;

	public static ArrayList<Line2D.Double> generate(int type) {
		ArrayList<Line2D.Double> obstacles = new ArrayList<>();

		if (type == MAP_TYPE_A) {
			int cx = MAP_X + MAP_WIDTH / 2;
			int cy = MAP_Y + MAP_HEIGHT / 2;
			int len = 150;
			obstacles.add(new Line2D.Double(cx - len, cy, cx - 30, cy));
			obstacles.add(new Line2D.Double(cx + 30, cy, cx + len, cy));
			obstacles.add(new Line2D.Double(cx, cy - len, cx, cy - 30));
			obstacles.add(new Line2D.Double(cx, cy + 30, cx, cy + len));
			obstacles.add(new Line2D.Double(MAP_X + 100, MAP_Y + 100, MAP_X + 200, MAP_Y + 100));
			obstacles.add(new Line2D.Double(MAP_X + MAP_WIDTH - 200, MAP_Y + MAP_HEIGHT - 100, MAP_X + MAP_WIDTH - 100, MAP_Y + MAP_HEIGHT - 100));
		} else if (type == MAP_TYPE_B) {
			for (int i = 1; i <= 3; i++) {
				int y = MAP_Y + (MAP_HEIGHT / 4) * i;
				int gap = 100;
				obstacles.add(new Line2D.Double(MAP_X + 50, y, MAP_X + MAP_WIDTH/2 - gap/2, y));
				obstacles.add(new Line2D.Double(MAP_X + MAP_WIDTH/2 + gap/2, y, MAP_X + MAP_WIDTH - 50, y));
			}
			obstacles.add(new Line2D.Double(MAP_X + MAP_WIDTH/2, MAP_Y + 50, MAP_X + MAP_WIDTH/2, MAP_Y + 150));
			obstacles.add(new Line2D.Double(MAP_X + MAP_WIDTH/2, MAP_Y + MAP_HEIGHT - 150, MAP_X + MAP_WIDTH/2, MAP_Y + MAP_HEIGHT - 50));
		} else {
			// MAP_TYPE_C
			int cx = MAP_X + MAP_WIDTH / 2;
			int cy = MAP_Y + MAP_HEIGHT / 2;
			obstacles.add(new Line2D.Double(cx - MAP_C_CROSS_SIZE, cy, cx + MAP_C_CROSS_SIZE, cy));
			obstacles.add(new Line2D.Double(cx, cy - MAP_C_CROSS_SIZE, cx, cy + MAP_C_CROSS_SIZE));

			int margin = MAP_C_CORNER_MARGIN;
			int size = MAP_C_CORNER_SIZE;

			addCorner(obstacles, MAP_X + margin, MAP_Y + margin, size, 1, 1);
			addCorner(obstacles, MAP_X + MAP_WIDTH - margin, MAP_Y + margin, size, -1, 1);
			addCorner(obstacles, MAP_X + margin, MAP_Y + MAP_HEIGHT - margin, size, 1, -1);
			addCorner(obstacles, MAP_X + MAP_WIDTH - margin, MAP_Y + MAP_HEIGHT - margin, size, -1, -1);
		}
		return obstacles;
	}

	private static void addCorner(ArrayList<Line2D.Double> list, int x, int y, int size, int dx, int dy) {
		list.add(new Line2D.Double(x, y, x + size * dx, y));
		list.add(new Line2D.Double(x, y, x, y + size * dy));
	}
}