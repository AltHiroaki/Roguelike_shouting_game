package game; // <--- これを追加

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.io.PrintWriter;

public class Player {
	public int id;
	public double x, y, angle;
	public int hp = GameConstants.PLAYER_MAX_HP;
	public int maxHp = GameConstants.PLAYER_MAX_HP;
	public Color color;
	public double speed = GameConstants.PLAYER_SPEED;
	public int size = GameConstants.PLAYER_SIZE;
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

	// 引数を整理しました
	public void update(boolean keyW, boolean keyS, boolean keyA, boolean keyD, int mx, int my, ArrayList<Line2D.Double> obstacles, PrintWriter out) {
		double currentSpeed = speed;
		if (poisonTimer > 0) {
			poisonTimer--;
			currentSpeed *= 0.5;
		}

		double nextX = x, nextY = y;
		if (keyA) nextX -= currentSpeed;
		if (keyD) nextX += currentSpeed;
		if (!checkWall(nextX, y, obstacles)) x = nextX;

		if (keyW) nextY -= currentSpeed;
		if (keyS) nextY += currentSpeed;
		if (!checkWall(x, nextY, obstacles)) y = nextY;

		angle = Math.atan2(my - y, mx - x);

		// 動的情報のみ送信
		out.println("MOVE " + (int)x + " " + (int)y + " " + angle + " " + hp
				+ " " + weapon.isReloading + " " + weapon.reloadTimer + " " + id);

		// 武器の更新
		weapon.update(out, id);
	}

	private boolean checkWall(double tx, double ty, ArrayList<Line2D.Double> walls) {
		if (tx < GameConstants.MAP_X + size || tx > GameConstants.MAP_X + GameConstants.MAP_WIDTH - size) return true;
		if (ty < GameConstants.MAP_Y + size || ty > GameConstants.MAP_Y + GameConstants.MAP_HEIGHT - size) return true;
		for (Line2D.Double w : walls) if (w.ptSegDist(tx, ty) < size) return true;
		return false;
	}

	public Rectangle getBounds() {
		return new Rectangle((int)x - size, (int)y - size, size * 2, size * 2);
	}

	public void draw(Graphics2D g2d, BufferedImage imgMe, BufferedImage imgEnemy, int myId) {
		AffineTransform old = g2d.getTransform();
		g2d.translate(x, y);

		// HPバー
		g2d.setColor(Color.RED);
		g2d.fillRect(-20, -35, 40, 5);
		g2d.setColor(Color.GREEN);
		int barWidth = (int)(40 * (hp / (double)maxHp));
		if (barWidth > 40) barWidth = 40;
		g2d.fillRect(-20, -35, barWidth, 5);

		if (poisonTimer > 0) {
			g2d.setColor(Color.MAGENTA);
			g2d.drawString("POISON", -20, -40);
		}

		// リロードバー
		if (weapon.isReloading) {
			g2d.setColor(Color.GRAY);
			g2d.fillRect(-20, -45, 40, 5);
			g2d.setColor(Color.YELLOW);
			double progress = (double)weapon.reloadTimer / weapon.reloadDuration;
			g2d.fillRect(-20, -45, (int)(40 * progress), 5);
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