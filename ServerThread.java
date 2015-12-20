import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class ServerThread extends Thread {
	private ServerSocket socket;
	DataInputStream in;
	DataOutputStream out;

	public static void main(String[] args){
	try{
		new ServerThread().start();
	}catch(IOException e){
		e.printStackTrace();
	}
	}
	public ServerThread() throws IOException {
		socket = new ServerSocket(8000);
		System.out.println("Running on " + socket);
	}

	public void run() {
		while (true) {
			try {
				Socket server = socket.accept();
				System.out.println("Incoming request: " + server.getRemoteSocketAddress());
				in = new DataInputStream(server.getInputStream());
				out = new DataOutputStream(server.getOutputStream());
				byte request = in.readByte();
				System.out.println("Recieved request " + request);
				if(request == 0){
					byte[] sending = new byte[4];
					sending[0] = 4;
					sending[1] = 19;
					sending[2] = 38;
					sending[3] = -19;
					out.write(sending);
				}else if(request == 1){
					out.writeUTF("You entered 1");
				}	
				server.close();
			} catch (SocketTimeoutException k) {
				System.err.println("Socket timed out!");
			} catch (IOException e) {
				e.printStackTrace();
				break;
			}
		}
	}
}
