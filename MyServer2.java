import java.net.ServerSocket;
import java.net.Socket;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.PrintWriter;

// スレッド部（各クライアントの通信処理）
class ClientProcThread extends Thread {
	private int number; // 自分の番号
	private Socket incoming;
	private InputStreamReader myIsr;
	private BufferedReader myIn;
	private PrintWriter myOut;
	private String myName; // 接続者の名前

	public ClientProcThread(int n, Socket i, InputStreamReader isr, BufferedReader in, PrintWriter out) {
		number = n;
		incoming = i;
		myIsr = isr;
		myIn = in;
		myOut = out;
	}

	public void run() {
		try {
			// アクションゲーム用に "START 番号" という形式で送る
			myOut.println("START " + number);

			// 名前入力待ちは削除（アクションゲームでは即開始するため）
			// myName = myIn.readLine();
			myName = "Player" + number; // 自動で仮の名前をつける

			while (true) { // 無限ループで、ソケットへの入力を監視する
				String str = myIn.readLine();

				// 高速化のためログ出力はコメントアウト
				// System.out.println("Received from client No."+number+"("+myName+"), Messages: "+str);

				if (str != null) {
					if (str.toUpperCase().equals("BYE")) {
						myOut.println("Good bye!");
						break;
					}

					// メッセージの末尾に「送信者のID」を付け足して全員に送る
					// これにより、受信側は「誰が動いたか」がわかるようになる
					MyServer2.SendAll(str + " " + number, myName);
				}
			}
		} catch (Exception e) {
			// ここにプログラムが到達するときは、接続が切れたとき
			System.out.println("Disconnect from client No." + number + "(" + myName + ")");
			MyServer2.SetFlag(number, false); // 接続が切れたのでフラグを下げる

			// 切断情報を全員に通知（LEAVEコマンド）
			MyServer2.SendAll("LEAVE " + number + " " + number, myName);
		}
	}
}

class MyServer2 {

	private static int maxConnection = 100; // 最大接続数
	private static Socket[] incoming; // 受付用のソケット
	private static boolean[] flag; // 接続中かどうかのフラグ
	private static InputStreamReader[] isr; // 入力ストリーム用の配列
	private static BufferedReader[] in; // バッファリングをによりテキスト読み込み用の配列
	private static PrintWriter[] out; // 出力ストリーム用の配列
	private static ClientProcThread[] myClientProcThread; // スレッド用の配列
	private static int member; // 接続しているメンバーの数

	// 全員にメッセージを送る
	public static void SendAll(String str, String myName) {
		// 送られた来たメッセージを接続している全員に配る
		for (int i = 1; i <= member; i++) {
			if (flag[i] == true) {
				out[i].println(str);
				out[i].flush(); // バッファをはき出す＝＞バッファにある全てのデータをすぐに送信する

				// 高速化のためログ出力はコメントアウト
				// System.out.println("Send messages to client No."+i);
			}
		}
	}

	// フラグの設定を行う
	public static void SetFlag(int n, boolean value) {
		flag[n] = value;
	}

	// main プログラム
	public static void main(String[] args) {
		// 必要な配列を確保する
		incoming = new Socket[maxConnection];
		flag = new boolean[maxConnection];
		isr = new InputStreamReader[maxConnection];
		in = new BufferedReader[maxConnection];
		out = new PrintWriter[maxConnection];
		myClientProcThread = new ClientProcThread[maxConnection];

		int n = 1;
		member = 0; // 誰も接続していないのでメンバー数は０

		try {
			System.out.println("=== Action Game Server (MyServer2 Mod) Started ===");
			ServerSocket server = new ServerSocket(10000); // 10000番ポートを利用する
			while (true) {
				incoming[n] = server.accept();
				flag[n] = true;
				System.out.println("Accept client No." + n);

				// 必要な入出力ストリームを作成する
				isr[n] = new InputStreamReader(incoming[n].getInputStream());
				in[n] = new BufferedReader(isr[n]);
				out[n] = new PrintWriter(incoming[n].getOutputStream(), true);

				myClientProcThread[n] = new ClientProcThread(n, incoming[n], isr[n], in[n], out[n]); // 必要なパラメータを渡しスレッドを作成
				myClientProcThread[n].start(); // スレッドを開始する
				member = n; // メンバーの数を更新する
				n++;
			}
		} catch (Exception e) {
			System.err.println("ソケット作成時にエラーが発生しました: " + e);
		}
	}
}