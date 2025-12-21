package game;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.io.PrintWriter;
import static game.GameConstants.*;

public class Player {
	public int id;
	public double x, y, angle;
	public int hp = PLAYER_MAX_HP;
	public int maxHp = PLAYER_MAX_HP;
	public Color color;
	public double speed = PLAYER_SPEED;
	public int size = PLAYER_SIZE;
	public int poisonTimer = 0;
	public Weapon weapon;

	public Player(int id, double x, double y, Color c) {
		this.id = id;
		this.x = x;
		this.y = y;
		this.color = c;
		this.weapon = new Weapon(this);
	}

	public void resetForRound() {
		hp = maxHp;
		poisonTimer = 0;
		weapon.reset();
	}

	public void sendStatus(PrintWriter out) {
		out.println("STATUS " + id + " " + maxHp + " " + size);
	}

	public void update(boolean keyW, boolean keyS, boolean keyA, boolean keyD, int mx, int my, ArrayList<Line2D.Double> obstacles, PrintWriter out) {
		double currentSpeed = speed;

		if (poisonTimer > 0) {
			poisonTimer--;
			currentSpeed *= PLAYER_POISON_SLOW_RATE; // 定数使用
		}

		double nextX = x, nextY = y;
		if (keyA) nextX -= currentSpeed;
		if (keyD) nextX += currentSpeed;
		if (!checkWall(nextX, y, obstacles)) x = nextX;

		if (keyW) nextY -= currentSpeed;
		if (keyS) nextY += currentSpeed;
		if (!checkWall(x, nextY, obstacles)) y = nextY;

		angle = Math.atan2(my - y, mx - x);

		out.println("MOVE " + (int)x + " " + (int)y + " " + angle + " " + hp
				+ " " + weapon.isReloading + " " + weapon.reloadTimer + " " + id);

		weapon.update(out, id);
	}

	private boolean checkWall(double tx, double ty, ArrayList<Line2D.Double> walls) {
		if (tx < MAP_X + size || tx > MAP_X + MAP_WIDTH - size) return true;
		if (ty < MAP_Y + size || ty > MAP_Y + MAP_HEIGHT - size) return true;
		for (Line2D.Double w : walls) if (w.ptSegDist(tx, ty) < size) return true;
		return false;
	}

	public Rectangle getBounds() {
		return new Rectangle((int)x - size, (int)y - size, size * 2, size * 2);
	}

	public void draw(Graphics2D g2d, BufferedImage imgMe, BufferedImage imgEnemy, int myId) {
		AffineTransform old = g2d.getTransform();
		g2d.translate(x, y);

		// HPバー (UI定数使用)
		g2d.setColor(Color.RED);
		g2d.fillRect(-UI_BAR_WIDTH/2, UI_BAR_HP_Y_OFFSET, UI_BAR_WIDTH, UI_BAR_HEIGHT);
		g2d.setColor(Color.GREEN);
		int barWidth = (int)(UI_BAR_WIDTH * (hp / (double)maxHp));
		if (barWidth > UI_BAR_WIDTH) barWidth = UI_BAR_WIDTH;
		g2d.fillRect(-UI_BAR_WIDTH/2, UI_BAR_HP_Y_OFFSET, barWidth, UI_BAR_HEIGHT);

		if (poisonTimer > 0) {
			g2d.setColor(Color.MAGENTA);
			g2d.drawString("POISON", -UI_BAR_WIDTH/2, UI_TEXT_POISON_Y_OFFSET);
		}

		// リロードバー
		if (weapon.isReloading) {
			g2d.setColor(Color.GRAY);
			g2d.fillRect(-UI_BAR_WIDTH/2, UI_BAR_RELOAD_Y_OFFSET, UI_BAR_WIDTH, UI_BAR_HEIGHT);
			g2d.setColor(Color.YELLOW);
			double progress = (double)weapon.reloadTimer / weapon.reloadDuration;
			g2d.fillRect(-UI_BAR_WIDTH/2, UI_BAR_RELOAD_Y_OFFSET, (int)(UI_BAR_WIDTH * progress), UI_BAR_HEIGHT);
		}

		g2d.rotate(angle);
		BufferedImage img = (id == myId) ? imgMe : imgEnemy;
		if (img != null) {
			g2d.drawImage(img, -size, -size, size * 2, size * 2, null);
		} else {
			g2d.setColor(color);
			g2d.fillOval(-size, -size, size * 2, size * 2);
			g2d.setColor(Color.BLACK);
			g2d.drawLine(0, 0, size + 10, 0);
		}
		g2d.setTransform(old);
	}
}