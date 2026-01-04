/**
 * DOM要素を取得するためのID定義
 * @constant {Object}
 */
const DOM_IDS = {
	CONTAINER: 'js-scroll-container',
	TEXT: 'js-hero-text',
	GAME_WINDOW: 'js-game-window'
};

/**
 * アニメーションの設定値
 * @constant {Object}
 */
const CONFIG = {
	TEXT_FADE_END: 0.3,     // テキストが消えきる位置
	GAME_APPEAR_START: 0.2, // ゲーム画面が出現し始める位置
	EASE_FACTOR: 0.08       // 追従の滑らかさ (0.01〜0.1)。小さいほどヌルヌル動く。
};

// 現在の表示用進捗率（アニメーション用）
let currentProgress = 0;
// 実際のスクロール位置から計算された目標進捗率
let targetProgress = 0;

/**
 * 初期化関数
 */
function init() {
	const els = {
		container: document.getElementById(DOM_IDS.CONTAINER),
		text: document.getElementById(DOM_IDS.TEXT),
		gameWindow: document.getElementById(DOM_IDS.GAME_WINDOW)
	};

	if (!els.container || !els.text || !els.gameWindow) {
		console.error('要素が見つかりません');
		return;
	}

	// ブラウザに「これからアニメーションするよ」と伝えて最適化してもらう
	els.text.style.willChange = 'transform, opacity';
	els.gameWindow.style.willChange = 'transform, opacity';

	// ループ処理を開始
	tick(els);
}

/**
 * 毎フレーム実行されるループ処理 (Game Loopのようなもの)
 * スクロールイベントの中ではなく、常に回し続けることで滑らかさを出す
 * @param {Object} els - DOM要素
 */
function tick(els) {
	// 1. 目標値の計算 (ここは前の onScroll と同じロジック)
	const rect = els.container.getBoundingClientRect();
	const viewportHeight = window.innerHeight;
	const scrollableDist = rect.height - viewportHeight;

	// スクロール位置の取得
	// コンテナが画面上端からどれくらい上に過ぎ去ったか
	// window.scrollY を使うより、getBoundingClientRect().top を使うほうが
	// 慣性スクロール中でも正確な相対位置が取れる
	let scrolled = -rect.top;

	if (scrollableDist > 0) {
		let p = scrolled / scrollableDist;
		targetProgress = Math.min(Math.max(p, 0), 1);
	}

	// 2. 線形補間 (Lerp) で現在値を目標値に近づける
	// 公式: 現在値 += (目標値 - 現在値) * 係数
	// これにより、目標値に「ふんわり」着地する動きになる
	currentProgress += (targetProgress - currentProgress) * CONFIG.EASE_FACTOR;

	// 3. 計算結果がほとんど変わらなければ描画しない（省エネ）
	// 差分が 0.0001 以上あるときだけスタイル更新
	if (Math.abs(targetProgress - currentProgress) > 0.0001) {
		updateAnimation(els, currentProgress);
	}

	// 次のフレームを予約
	window.requestAnimationFrame(() => tick(els));
}

/**
 * DOMのスタイルを更新する関数
 * @param {Object} els
 * @param {number} progress - 補間された滑らかな進捗率
 */
function updateAnimation(els, progress) {
	// --- テキストのアニメーション ---
	let textOp = 1 - (progress / CONFIG.TEXT_FADE_END);
	textOp = Math.min(Math.max(textOp, 0), 1);

	let textScale = 1 + (progress * 0.5);
	let textY = progress * 100;

	els.text.style.opacity = textOp;
	els.text.style.transform = `scale(${textScale}) translateY(${textY}px)`;

	// 見えなくなったら非表示（レイアウト崩れ防止）
	// ただし慣性があるため、完全に0になるまで非表示にしない方が安全
	els.text.style.visibility = textOp <= 0 ? 'hidden' : 'visible';


	// --- ゲーム画面のアニメーション ---
	let gameProgress = (progress - CONFIG.GAME_APPEAR_START) / (1 - CONFIG.GAME_APPEAR_START);
	gameProgress = Math.min(Math.max(gameProgress, 0), 1);

	// スケール
	let scaleVal = 0.5 + (gameProgress * 0.5);

	// 透明度
	let opacityVal = gameProgress * 1.5;
	if (opacityVal > 1) opacityVal = 1;

	// 3D回転
	let rotateX = 60 - (gameProgress * 60);

	// Y位置
	let translateY = 200 - (gameProgress * 200);

	els.gameWindow.style.transform = `
        translate(-50%, -50%) 
        perspective(1000px) 
        rotateX(${rotateX}deg) 
        scale(${scaleVal})
        translateY(${translateY}px)
    `;
	els.gameWindow.style.opacity = opacityVal;
}

document.addEventListener('DOMContentLoaded', init);