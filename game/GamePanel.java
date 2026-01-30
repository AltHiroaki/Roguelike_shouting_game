package game;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import static game.GameConstants.*;

/**
 * ゲーム画面の描画とUIイベントを処理するパネルクラス。
 * タイトル、ゲームプレイ、リザルト画面など、すべての描画処理をここで行います。
 */
public class GamePanel extends JPanel {
	// --- 依存オブジェクト ---
	public InputHandler input;
	private GameLogic logic;
	private ActionClient client;

	// --- UIコンポーネントの矩形情報（クリック判定用） ---
	Rectangle startButtonRect;
	Rectangle abilityInfoBtnRect; // 能力紹介ボタン
	Rectangle controlsInfoBtnRect; // 操作説明ボタン
	Rectangle[] mapButtons = new Rectangle[3];
	Rectangle[] cardRects = new Rectangle[3];

	// 能力紹介・操作説明画面共通コンポーネント
	ArrayList<PowerUp> allPowerUps;
	Rectangle backButtonRect = new Rectangle(50, 50, 100, 40);
	int selectedAbilityIndex = 0;

	// スクロール用変数
	int scrollIndex = 0;
	final int MAX_VISIBLE_ITEMS = 15; // 一度に表示する最大数
	Rectangle[] visibleListRects;     // 画面上に表示される枠（固定位置）

	/**
	 * コンストラクタ。
	 * 入力リスナーの設定とUIパーツの初期化を行います。
	 */
	public GamePanel(ActionClient client, GameLogic logic, InputHandler input) {
		this.client = client;
		this.logic = logic;
		this.input = input;

		// 全能力リストの取得
		allPowerUps = PowerUpFactory.getAllPowerUps();

		// 表示枠の初期化（画面上の固定位置）
		visibleListRects = new Rectangle[MAX_VISIBLE_ITEMS];
		for(int i=0; i<MAX_VISIBLE_ITEMS; i++) {
			visibleListRects[i] = new Rectangle(50, 120 + i * 30, 250, 25);
		}

		setFocusable(true);
		addKeyListener(input);
		addMouseListener(input);
		addMouseMotionListener(input);
		addMouseWheelListener(input);

		// UIクリック処理用のリスナー
		addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) { handleUIMouse(e.getX(), e.getY()); }
		});

		// UI配置の計算 (ウィンドウサイズ 1060x790 を前提に中央揃え)
		// 画面中央のX座標
		int centerX = 1060 / 2;

		// 1. マップ選択ボタン (中央少し上)
		int mapBtnW = 120; int mapBtnH = 40; int mapGap = 20;
		int mapTotalW = (mapBtnW * 3) + (mapGap * 2);
		int mapStartX = centerX - (mapTotalW / 2);
		int mapY = 400; // 高さ位置

		mapButtons[0] = new Rectangle(mapStartX, mapY, mapBtnW, mapBtnH);
		mapButtons[1] = new Rectangle(mapStartX + mapBtnW + mapGap, mapY, mapBtnW, mapBtnH);
		mapButtons[2] = new Rectangle(mapStartX + (mapBtnW + mapGap) * 2, mapY, mapBtnW, mapBtnH);

		// 2. スタートボタン (ど真ん中、大きく)
		int startW = 240; int startH = 80;
		startButtonRect = new Rectangle(centerX - startW/2, 500, startW, startH);

		// 3. 情報ボタン (スタートボタンの下に左右に配置)
		int infoW = 160; int infoH = 50; int infoGap = 40;
		// 操作説明(左)
		controlsInfoBtnRect = new Rectangle(centerX - infoW - infoGap/2, 630, infoW, infoH);
		// 能力紹介(右)
		abilityInfoBtnRect = new Rectangle(centerX + infoGap/2, 630, infoW, infoH);
	}

	/**
	 * マウスクリック時のUI判定処理。
	 * 現在のゲーム状態(State)に応じて処理を分岐します。
	 */
	private void handleUIMouse(int mx, int my) {
		if (client.currentState == ActionClient.GameState.TITLE) {
			// タイトル画面: マップ選択
			// 現在のホスト（最小ID）のみ選択可能にする
			if (client.myId == logic.getHostId()) {
				int[] types = {MapGenerator.MAP_TYPE_A, MapGenerator.MAP_TYPE_B, MapGenerator.MAP_TYPE_C};
				for (int i = 0; i < 3; i++) {
					if (mapButtons[i].contains(mx, my)) {
						client.selectedMapType = types[i];
						repaint();
						return;
					}
				}
			}

			if (startButtonRect.contains(mx, my)) client.joinGame();

			// 能力紹介ボタン
			if (abilityInfoBtnRect.contains(mx, my)) {
				client.currentState = ActionClient.GameState.ABILITY_INFO;
				selectedAbilityIndex = -1; // 初期状態（詳細なし）
				scrollIndex = 0;           // スクロールリセット
				repaint();
			}
			// 操作説明ボタン
			if (controlsInfoBtnRect.contains(mx, my)) {
				client.currentState = ActionClient.GameState.CONTROLS_INFO;
				repaint();
			}

		} else if (client.currentState == ActionClient.GameState.ABILITY_INFO) {
			// 能力紹介画面
			if (backButtonRect.contains(mx, my)) {
				client.currentState = ActionClient.GameState.TITLE;
				repaint();
			}
			// リスト選択（表示されている枠をクリックしたか判定）
			for(int i=0; i<MAX_VISIBLE_ITEMS; i++) {
				if(visibleListRects[i].contains(mx, my)) {
					int dataIndex = scrollIndex + i;
					if (dataIndex < allPowerUps.size()) {
						selectedAbilityIndex = dataIndex;
						repaint();
					}
				}
			}

		} else if (client.currentState == ActionClient.GameState.CONTROLS_INFO) {
			// 操作説明画面
			if (backButtonRect.contains(mx, my)) {
				client.currentState = ActionClient.GameState.TITLE;
				repaint();
			}

		} else if (client.currentState == ActionClient.GameState.ROUND_END_SELECT) {
			// ラウンド終了時: パワーアップカードの選択
			for(int i=0; i<logic.presentedPowerUps.size(); i++) {
				if(cardRects[i] != null && cardRects[i].contains(mx, my)) {
					PowerUp p = logic.presentedPowerUps.get(i);
					p.apply(logic.players.get(client.myId));
					client.onPowerUpSelected(p.name);
					break;
				}
			}

		} else if (client.currentState == ActionClient.GameState.GAME_OVER) {
			// ゲームオーバー画面: タイトルへ戻る
			client.backToTitle();
		}
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2d = (Graphics2D) g;
		// アンチエイリアス有効化
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		// 背景塗りつぶし
		g2d.setColor(COLOR_BG);
		g2d.fillRect(0, 0, getWidth(), getHeight());
		drawGrid(g2d);

		// 状態に応じた描画メソッドの呼び出し
		switch (client.currentState) {
			case TITLE:            drawTitleScreen(g2d); break;
			case ABILITY_INFO:     drawAbilityInfoScreen(g2d); break;
			case CONTROLS_INFO:    drawControlsScreen(g2d); break;
			case WAITING:          drawWaitingScreen(g2d); break;
			case PLAYING:          drawGameScreen(g2d); break;
			case ROUND_END_SELECT: drawGameScreen(g2d); drawPowerUpSelection(g2d); break;
			case ROUND_END_WAIT:   drawGameScreen(g2d); drawRoundEndWait(g2d); break;
			case COUNTDOWN:        drawGameScreen(g2d); drawCountdown(g2d); break;
			case GAME_OVER:        drawGameScreen(g2d); drawGameOver(g2d); break;
		}
	}

	/**
	 * 背景のグリッド線を描画します。
	 */
	private void drawGrid(Graphics2D g2d) {
		g2d.setColor(COLOR_GRID); g2d.setStroke(new BasicStroke(1));
		int gridSize = GRID_SIZE;
		for (int x = 0; x < getWidth(); x += gridSize) g2d.drawLine(x, 0, x, getHeight());
		for (int y = 0; y < getHeight(); y += gridSize) g2d.drawLine(0, y, getWidth(), y);
	}

	/**
	 * 星マークの文字列を生成するユーティリティ。勝利数表示用。
	 */
	private String getStars(int count) {
		StringBuilder sb = new StringBuilder();
		for(int i=0; i<count; i++) sb.append("*");
		return sb.toString();
	}

	/**
	 * タイトル画面の描画。マップ選択ボタンなどを表示します。
	 */
	private void drawTitleScreen(Graphics2D g2d) {
		g2d.setColor(Color.CYAN); g2d.setFont(new Font(FONT_NAME, Font.BOLD, 80)); // フォント大きく
		centerString(g2d, "VECTOR ARENA", 200);

		g2d.setFont(new Font(FONT_NAME, Font.BOLD, 16));

		// マップ選択ボタン: ABC順 (平原, 通路, 要塞)
		String[] labels = {"平原 (A)", "通路 (B)", "要塞 (C)"};
		int[] types = {MapGenerator.MAP_TYPE_A, MapGenerator.MAP_TYPE_B, MapGenerator.MAP_TYPE_C};

		int hostId = logic.getHostId();

		// 現在のホスト（最小ID）に応じて描画を変える
		if (client.myId == hostId) {
			// ホストの場合: 通常描画
			for (int i = 0; i < 3; i++) {
				Rectangle btn = mapButtons[i];
				if (client.selectedMapType == types[i]) g2d.setColor(Color.YELLOW); else g2d.setColor(Color.LIGHT_GRAY);
				g2d.fill(btn);
				g2d.setColor(Color.BLACK);
				String lb = labels[i]; int sw = g2d.getFontMetrics().stringWidth(lb);
				// ボタンの高さに対して、文字が垂直方向の中央に来るように調整 (+26px)
				g2d.drawString(lb, btn.x + (btn.width - sw)/2, btn.y + 26);
			}
			// ガイドメッセージ
			g2d.setColor(Color.WHITE); g2d.setFont(new Font(FONT_NAME, Font.PLAIN, 14));
			centerString(g2d, "マップを選択してください", 380);
		} else {
			// ゲストの場合: グレーアウトして無効化
			for (int i = 0; i < 3; i++) {
				Rectangle btn = mapButtons[i];
				g2d.setColor(Color.DARK_GRAY); // 暗い色
				g2d.fill(btn);
				g2d.setColor(Color.GRAY);
				String lb = labels[i]; int sw = g2d.getFontMetrics().stringWidth(lb);
				// ボタンの高さに対して、文字が垂直方向の中央に来るように調整 (+26px)
				g2d.drawString(lb, btn.x + (btn.width - sw)/2, btn.y + 26);
			}
			// ガイドメッセージ
			g2d.setColor(Color.LIGHT_GRAY); g2d.setFont(new Font(FONT_NAME, Font.PLAIN, 14));
			centerString(g2d, "※マップ選択権はホスト(Player" + hostId + ")にあります", 380);
		}

		// スタートボタン
		g2d.setColor(Color.GREEN); g2d.fill(startButtonRect);
		g2d.setColor(Color.BLACK); g2d.setFont(new Font(FONT_NAME, Font.BOLD, 40));
		String st = "START";
		int sw = g2d.getFontMetrics().stringWidth(st);
		g2d.drawString(st, startButtonRect.x + (startButtonRect.width - sw)/2, startButtonRect.y + 55);

		// 情報ボタン共通フォント
		g2d.setFont(new Font(FONT_NAME, Font.BOLD, 16));

		// 能力紹介ボタン
		g2d.setColor(Color.ORANGE); g2d.fill(abilityInfoBtnRect);
		g2d.setColor(Color.BLACK);
		String ab = "能力紹介";
		int aw = g2d.getFontMetrics().stringWidth(ab);
		g2d.drawString(ab, abilityInfoBtnRect.x + (abilityInfoBtnRect.width - aw)/2, abilityInfoBtnRect.y + 30);

		// 操作説明ボタン
		g2d.setColor(Color.ORANGE); g2d.fill(controlsInfoBtnRect);
		g2d.setColor(Color.BLACK);
		String ct = "操作説明";
		int cw = g2d.getFontMetrics().stringWidth(ct);
		g2d.drawString(ct, controlsInfoBtnRect.x + (controlsInfoBtnRect.width - cw)/2, controlsInfoBtnRect.y + 30);
	}

	/**
	 * 操作説明画面の描画
	 */
	private void drawControlsScreen(Graphics2D g2d) {
		// 半透明背景
		g2d.setColor(new Color(0, 0, 0, 200));
		g2d.fillRect(20, 20, getWidth()-40, getHeight()-40);

		// 戻るボタン
		g2d.setColor(Color.GRAY); g2d.fill(backButtonRect);
		g2d.setColor(Color.WHITE); g2d.setFont(new Font(FONT_NAME, Font.BOLD, 16));
		g2d.drawString("戻る", backButtonRect.x + 30, backButtonRect.y + 25);

		// タイトル
		g2d.setColor(Color.CYAN); g2d.setFont(new Font(FONT_NAME, Font.BOLD, 40));
		centerString(g2d, "操作説明", 80);

		int x = 100;
		int y = 150;
		int gap = 50;

		g2d.setFont(new Font(FONT_NAME, Font.BOLD, 24));
		g2d.setColor(Color.WHITE);

		g2d.drawString("【基本操作】", x, y);
		y += 40;
		g2d.setFont(new Font(FONT_NAME, Font.PLAIN, 20));
		g2d.drawString("移動: W, A, S, D", x + 30, y);
		y += gap;
		g2d.drawString("照準: マウスカーソル", x + 30, y);
		y += gap;
		g2d.drawString("射撃: 左クリック", x + 30, y);
		y += gap;
		g2d.drawString("ガード: 右クリック", x + 30, y);
		y += gap * 1;

		g2d.setFont(new Font(FONT_NAME, Font.BOLD, 24));
		g2d.drawString("【ゲームルール】", x, y);
		y += 40;
		g2d.setFont(new Font(FONT_NAME, Font.PLAIN, 20));
		g2d.drawString("・相手のHPを0にするとラウンド勝利。", x + 30, y);
		y += gap;
		g2d.drawString("・先に5勝したプレイヤーの完全勝利。", x + 30, y);
		y += gap;
		g2d.drawString("・ラウンド敗者は、次のラウンドで強力な能力を取得可能。", x + 30, y);
		y += gap;
		g2d.drawString("・一部の能力は「ガード」した瞬間に発動する。", x + 30, y);
	}

	/**
	 * 能力紹介画面の描画（スクロール対応）
	 */
	private void drawAbilityInfoScreen(Graphics2D g2d) {
		// スクロール処理
		if (input.scrollAmount != 0) {
			scrollIndex += input.scrollAmount;
			input.scrollAmount = 0; // 消費
		}
		// 範囲制限
		if (scrollIndex < 0) scrollIndex = 0;
		int maxScroll = Math.max(0, allPowerUps.size() - MAX_VISIBLE_ITEMS);
		if (scrollIndex > maxScroll) scrollIndex = maxScroll;

		// 半透明背景
		g2d.setColor(new Color(0, 0, 0, 200));
		g2d.fillRect(20, 20, getWidth()-40, getHeight()-40);

		// 戻るボタン
		g2d.setColor(Color.GRAY); g2d.fill(backButtonRect);
		g2d.setColor(Color.WHITE); g2d.setFont(new Font(FONT_NAME, Font.BOLD, 16));
		g2d.drawString("戻る", backButtonRect.x + 30, backButtonRect.y + 25);

		// タイトル
		g2d.setColor(Color.CYAN); g2d.setFont(new Font(FONT_NAME, Font.BOLD, 30));
		g2d.drawString("能力紹介", 300, 80);

		// 左側：リスト（スクロール範囲内のみ描画）
		g2d.setFont(new Font(FONT_NAME, Font.PLAIN, 14));

		for(int i=0; i<MAX_VISIBLE_ITEMS; i++) {
			int dataIndex = scrollIndex + i;
			if (dataIndex >= allPowerUps.size()) break;

			Rectangle rect = visibleListRects[i];
			if(dataIndex == selectedAbilityIndex) {
				g2d.setColor(Color.YELLOW);
				g2d.fill(rect);
				g2d.setColor(Color.BLACK);
			} else {
				g2d.setColor(Color.WHITE);
				g2d.draw(rect);
			}
			g2d.drawString(allPowerUps.get(dataIndex).name, rect.x + 5, rect.y + 18);
		}

		// スクロールバーの描画
		if (allPowerUps.size() > MAX_VISIBLE_ITEMS) {
			int barX = 310;
			int barY = 120;
			int barW = 10;
			int barH = MAX_VISIBLE_ITEMS * 30; // リスト全体の高さ

			// バー背景
			g2d.setColor(Color.DARK_GRAY);
			g2d.fillRect(barX, barY, barW, barH);

			// ハンドル（つまみ）
			double ratio = (double) MAX_VISIBLE_ITEMS / allPowerUps.size();
			int handleH = (int)(barH * ratio);
			if(handleH < 20) handleH = 20; // 最小サイズ

			double posRatio = (double) scrollIndex / (allPowerUps.size() - MAX_VISIBLE_ITEMS);
			int handleY = barY + (int)((barH - handleH) * posRatio);

			g2d.setColor(Color.LIGHT_GRAY);
			g2d.fillRect(barX, handleY, barW, handleH);
		}

		// 右側：詳細表示
		int detailsX = 350; int detailsY = 120;

		if (selectedAbilityIndex == -1) {
			// デフォルトメッセージ
			g2d.setColor(Color.WHITE);
			g2d.setFont(new Font(FONT_NAME, Font.PLAIN, 20));
			drawStringMultiLine(g2d, "このVECTOR ARENAでは様々な能力が存在している。\nこれらを知ることで、勝率を高めよう！", detailsX, detailsY);
		} else {
			PowerUp p = allPowerUps.get(selectedAbilityIndex);

			g2d.setColor(Color.WHITE);
			g2d.setFont(new Font(FONT_NAME, Font.BOLD, 40));
			g2d.drawString(p.name, detailsX, detailsY);

			g2d.setFont(new Font(FONT_NAME, Font.BOLD, 20));
			g2d.setColor(Color.CYAN);
			g2d.drawString("・" + p.merit, detailsX, detailsY + 50);
			g2d.setColor(Color.PINK);
			g2d.drawString("・" + p.demerit, detailsX, detailsY + 80);

			g2d.setColor(Color.WHITE);
			g2d.setFont(new Font(FONT_NAME, Font.PLAIN, 16));
			drawStringMultiLine(g2d, p.flavorText, detailsX, detailsY + 130);
		}
	}

	// 複数行テキスト描画ヘルパー
	private void drawStringMultiLine(Graphics2D g, String text, int x, int y) {
		for (String line : text.split("\n")) {
			g.drawString(line, x, y += g.getFontMetrics().getHeight());
		}
	}

	/**
	 * 対戦相手待機画面の描画。
	 */
	private void drawWaitingScreen(Graphics2D g2d) {
		g2d.setColor(COLOR_TEXT); g2d.setFont(new Font(FONT_NAME, Font.BOLD, 30));
		centerString(g2d, "対戦相手を待っています...", 300);
		g2d.setFont(new Font(FONT_NAME, Font.PLAIN, 20));
		centerString(g2d, "現在の参加人数: " + logic.joinedPlayers.size(), 350);
	}

	/**
	 * ゲームプレイ画面の描画。
	 * プレイヤー、障害物、弾丸、UIなどを描画します。
	 */
	private void drawGameScreen(Graphics2D g2d) {
		// スコア表示 (MAP_Y = 130なので、少し下にずらして見やすく)
		g2d.setFont(new Font(FONT_NAME, Font.BOLD, 30));
		g2d.setColor(COLOR_PLAYER_ME); g2d.drawString("自分: " + getStars(logic.myWinCount), 50, 60);
		g2d.setColor(COLOR_PLAYER_ENEMY);  g2d.drawString("相手: " + getStars(logic.enemyWinCount), 500, 60);

		// 能力の頭文字を表示
		g2d.setFont(new Font(FONT_NAME, Font.BOLD, 20));
		int iconGap = 25; // 文字の間隔

		// 自分の能力（左側：スコアの下あたり）
		if (logic.players.containsKey(client.myId)) {
			ArrayList<String> myAbs = logic.players.get(client.myId).abilityNames;
			g2d.setColor(Color.CYAN);
			for(int i=0; i<myAbs.size(); i++) {
				String name = myAbs.get(i);
				String initial = (name.length() > 0) ? name.substring(0, 1) : "?";
				// Y座標調整
				g2d.drawString(initial, 50 + (i * iconGap), 100);
			}
		}

		// 相手の能力（右側）
		int enemyId = -1;
		for(int id : logic.players.keySet()) {
			if(id != client.myId) { enemyId = id; break; }
		}

		if (enemyId != -1 && logic.players.containsKey(enemyId)) {
			ArrayList<String> enAbs = logic.players.get(enemyId).abilityNames;
			g2d.setColor(Color.PINK);
			for(int i=0; i<enAbs.size(); i++) {
				String name = enAbs.get(i);
				String initial = (name.length() > 0) ? name.substring(0, 1) : "?";
				// Y座標調整
				g2d.drawString(initial, 500 + (i * iconGap), 100);
			}
		}

		// マップ枠線
		g2d.setColor(COLOR_TEXT); g2d.setStroke(new BasicStroke(3));
		g2d.drawRect(MAP_X, MAP_Y, MAP_WIDTH, MAP_HEIGHT);

		// 障害物（壁）
		g2d.setColor(COLOR_WALL);
		g2d.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		for (java.awt.geom.Line2D.Double wall : logic.obstacles) g2d.draw(wall);

		// プレイヤーと弾丸
		g2d.setStroke(new BasicStroke(1));
		for (Player p : logic.players.values()) p.draw(g2d, client.imgPlayerMe, client.imgPlayerEnemy, client.myId);
		for (Bullet b : logic.bulletPool) b.draw(g2d);

		// 自分の残弾数表示
		if (logic.players.containsKey(client.myId)) {
			Player me = logic.players.get(client.myId);
			g2d.setFont(new Font(FONT_NAME, Font.BOLD, 18));
			if (me.weapon.currentAmmo == 0) g2d.setColor(Color.RED); else g2d.setColor(Color.CYAN);
			String ammoText = me.weapon.isReloading ? "リロード中..." : "残弾: " + me.weapon.currentAmmo + "/" + me.weapon.maxAmmo;
			g2d.drawString(ammoText, MAP_X, MAP_Y + MAP_HEIGHT + UI_AMMO_Y_OFFSET);
		}
	}

	/**
	 * ラウンド敗北時のパワーアップ選択画面の描画。
	 * カード状のUIを表示します。
	 */
	private void drawPowerUpSelection(Graphics2D g2d) {
		// 背景を暗くする
		g2d.setColor(new Color(0,0,0,200)); g2d.fillRect(0,0,getWidth(),getHeight());

		g2d.setColor(Color.WHITE); g2d.setFont(new Font(FONT_NAME, Font.BOLD, 40));
		centerString(g2d, "強化を選択してください", 150);

		int startX = UI_CARD_START_X; int y = UI_CARD_Y; int w = UI_CARD_WIDTH; int h = UI_CARD_HEIGHT; int gap = UI_CARD_GAP;

		for(int i=0; i<logic.presentedPowerUps.size(); i++) {
			PowerUp p = logic.presentedPowerUps.get(i);
			Rectangle rect = new Rectangle(startX + (w+gap)*i, y, w, h);
			cardRects[i] = rect;

			// ホバー判定
			if(input.mouseX >= rect.x && input.mouseX <= rect.x+rect.width && input.mouseY >= rect.y && input.mouseY <= rect.y+rect.height) {
				g2d.setColor(Color.YELLOW); g2d.setStroke(new BasicStroke(3));
			} else {
				g2d.setColor(Color.WHITE); g2d.setStroke(new BasicStroke(1));
			}

			// カード描画
			g2d.draw(rect); g2d.setColor(new Color(50,50,50)); g2d.fill(rect);

			// テキスト描画
			g2d.setColor(Color.WHITE); g2d.setFont(new Font(FONT_NAME, Font.BOLD, 20));
			g2d.drawString(p.name, rect.x+10, rect.y+40);
			g2d.setFont(new Font(FONT_NAME, Font.PLAIN, 12));
			g2d.drawString(p.desc, rect.x+10, rect.y+70);
			g2d.setColor(Color.CYAN); g2d.drawString("長所: "+p.merit, rect.x+10, rect.y+120);
			g2d.setColor(Color.PINK); g2d.drawString("短所: "+p.demerit, rect.x+10, rect.y+150);
		}
	}

	/**
	 * ラウンド勝利時の待機画面描画。
	 */
	private void drawRoundEndWait(Graphics2D g2d) {
		g2d.setColor(new Color(0,0,0,150)); g2d.fillRect(0,0,getWidth(),getHeight());
		g2d.setColor(Color.WHITE); g2d.setFont(new Font(FONT_NAME, Font.BOLD, 40));
		centerString(g2d, logic.resultMessage, 200);
		centerString(g2d, "相手の準備を待っています...", 300);
	}

	/**
	 * ラウンド開始前のカウントダウン描画。
	 */
	private void drawCountdown(Graphics2D g2d) {
		g2d.setColor(Color.YELLOW); g2d.setFont(new Font(FONT_NAME, Font.BOLD, 100));
		centerString(g2d, String.valueOf(client.countdownTimer/FPS + 1), 300);
	}

	/**
	 * 最終的なゲームオーバー画面の描画。
	 */
	private void drawGameOver(Graphics2D g2d) {
		g2d.setColor(new Color(0,0,0,200)); g2d.fillRect(0,0,getWidth(),getHeight());

		g2d.setColor(logic.resultMessage.contains("勝利") ? Color.YELLOW : Color.GRAY);
		g2d.setFont(new Font(FONT_NAME, Font.BOLD, 50));
		centerString(g2d, logic.resultMessage, 300);

		g2d.setColor(Color.WHITE); g2d.setFont(new Font(FONT_NAME, Font.PLAIN, 20));
		centerString(g2d, "クリックしてタイトルへ戻る", 400);
	}

	/**
	 * 文字列を画面中央（X軸）に描画するためのヘルパーメソッド。
	 */
	private void centerString(Graphics2D g, String s, int y) {
		int w = g.getFontMetrics().stringWidth(s);
		g.drawString(s, (getWidth()-w)/2, y);
	}
}