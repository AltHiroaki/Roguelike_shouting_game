import java.net.ServerSocket;
import java.net.Socket;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.OutputStreamWriter;

class ClientProcThread extends Thread {
	private int number;
	private Socket incoming;
	private InputStreamReader myIsr;
	private BufferedReader myIn;
	private PrintWriter myOut;
	private String myName;

	public ClientProcThread(int n, Socket i, InputStreamReader isr, BufferedReader in, PrintWriter out) {
		number = n;
		incoming = i;
		myIsr = isr;
		myIn = in;
		myOut = out;
	}

	public void run() {
		try {
			myOut.println("START " + number);
			myName = "Player" + number;

			while (true) {
				String str = myIn.readLine();
				if (str != null) {
					if (str.toUpperCase().equals("BYE")) {
						myOut.println("Good bye!");
						break;
					}
					MyServer2.SendAll(str + " " + number, myName);
				}
			}
		} catch (Exception e) {
			System.out.println("Disconnect from client No." + number + "(" + myName + ")");
			MyServer2.SetFlag(number, false);
			MyServer2.SendAll("LEAVE " + number + " " + number, myName);
		}
	}
}

class MyServer2 {
	private static final int PORT = 10000;
	private static final int MAX_CONNECTION = 100;

	private static Socket[] incoming;
	private static boolean[] flag;
	private static InputStreamReader[] isr;
	private static BufferedReader[] in;
	private static PrintWriter[] out;
	private static ClientProcThread[] myClientProcThread;
	private static int member;

	public static void SendAll(String str, String myName) {
		for (int i = 1; i <= member; i++) {
			if (flag[i] == true) {
				out[i].println(str);
				out[i].flush();
			}
		}
	}

	public static void SetFlag(int n, boolean value) {
		flag[n] = value;
	}

	public static void main(String[] args) {
		incoming = new Socket[MAX_CONNECTION];
		flag = new boolean[MAX_CONNECTION];
		isr = new InputStreamReader[MAX_CONNECTION];
		in = new BufferedReader[MAX_CONNECTION];
		out = new PrintWriter[MAX_CONNECTION];
		myClientProcThread = new ClientProcThread[MAX_CONNECTION];

		int n = 1;
		member = 0;

		try {
			System.out.println("=== Action Game Server (UTF-8) Started ===");
			ServerSocket server = new ServerSocket(PORT);
			while (true) {
				incoming[n] = server.accept();
				flag[n] = true;
				System.out.println("Accept client No." + n);

				isr[n] = new InputStreamReader(incoming[n].getInputStream(), "UTF-8");
				in[n] = new BufferedReader(isr[n]);
				out[n] = new PrintWriter(new OutputStreamWriter(incoming[n].getOutputStream(), "UTF-8"), true);

				myClientProcThread[n] = new ClientProcThread(n, incoming[n], isr[n], in[n], out[n]);
				myClientProcThread[n].start();
				member = n;
				n++;
			}
		} catch (Exception e) {
			System.err.println("Error: " + e);
		}
	}
}