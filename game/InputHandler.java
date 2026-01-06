package game;

import java.awt.event.*;
import javax.swing.SwingUtilities;

/**
 * キーボードおよびマウスの入力を管理するハンドラクラス。
 * KeyListener, MouseListener, MouseMotionListenerを実装し、
 * 現在の入力状態をパブリックフィールドとして提供します。
 */
public class InputHandler implements KeyListener, MouseListener, MouseMotionListener {
	// --- キー入力状態 ---
	public boolean keyW, keyS, keyA, keyD;

	// --- マウス入力状態 ---
	public int mouseX, mouseY;
	public boolean isMousePressed = false;      // 左クリック
	public boolean isRightMousePressed = false; // 右クリック

	@Override
	public void keyPressed(KeyEvent e) {
		int k = e.getKeyCode();
		if (k == KeyEvent.VK_W) keyW = true;
		if (k == KeyEvent.VK_S) keyS = true;
		if (k == KeyEvent.VK_A) keyA = true;
		if (k == KeyEvent.VK_D) keyD = true;
	}

	@Override
	public void keyReleased(KeyEvent e) {
		int k = e.getKeyCode();
		if (k == KeyEvent.VK_W) keyW = false;
		if (k == KeyEvent.VK_S) keyS = false;
		if (k == KeyEvent.VK_A) keyA = false;
		if (k == KeyEvent.VK_D) keyD = false;
	}

	@Override public void mouseMoved(MouseEvent e) { mouseX = e.getX(); mouseY = e.getY(); }
	@Override public void mouseDragged(MouseEvent e) { mouseX = e.getX(); mouseY = e.getY(); }

	@Override public void mousePressed(MouseEvent e) {
		if (SwingUtilities.isLeftMouseButton(e)) isMousePressed = true;
		if (SwingUtilities.isRightMouseButton(e)) isRightMousePressed = true;
	}

	@Override public void mouseReleased(MouseEvent e) {
		if (SwingUtilities.isLeftMouseButton(e)) isMousePressed = false;
		if (SwingUtilities.isRightMouseButton(e)) isRightMousePressed = false;
	}

	// 未使用のインターフェースメソッド
	@Override public void keyTyped(KeyEvent e) {}
	@Override public void mouseClicked(MouseEvent e) {}
	@Override public void mouseEntered(MouseEvent e) {}
	@Override public void mouseExited(MouseEvent e) {}
}