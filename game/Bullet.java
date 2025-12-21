package game;

import java.awt.*;

public class Bullet {
	public boolean isActive = false;
	public int id, ownerId, damage, size, typeFlag;
	public double x, y, angle, speed;
	public int bounceCount = 0, lifeTimer = 0;

	public Bullet() {}

	public void activate(int id, double x, double y, double a, double s, int d, int sz, int f, int o) {
		this.id = id;
		this.x = x;
		this.y = y;
		this.angle = a;
		this.speed = s;
		this.damage = d;
		this.size = sz;
		this.typeFlag = f;
		this.ownerId = o;
		this.isActive = true;
		this.bounceCount = 0;
		this.lifeTimer = 0;
	}

	public void deactivate() {
		isActive = false;
	}

	public void update() {
		x += Math.cos(angle) * speed;
		y += Math.sin(angle) * speed;
		lifeTimer++;
	}

	public void draw(Graphics2D g2d) {
		if (!isActive) return;
		g2d.setColor(Color.YELLOW);
		if ((typeFlag & GameConstants.FLAG_POISON) != 0) g2d.setColor(Color.MAGENTA);
		g2d.fillOval((int)x - size / 2, (int)y - size / 2, size, size);
	}
}