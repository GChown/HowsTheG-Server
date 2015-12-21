import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.EOFException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class ServerThread extends Thread {
	private ServerSocket socket;
	DataInputStream in;
	DataOutputStream out;
	private Socket[] clients;
	private int manyClients;
	private Socket server;
	public static void main(String[] args){
	try{
		new ServerThread().start();
	}catch(IOException e){
		e.printStackTrace();
	}
	}
	public ServerThread() throws IOException {
		clients = new Socket[10];
		manyClients = 0;
		socket = new ServerSocket(8000);
		System.out.println("Running on " + socket);
	}

	public void run() {
		while (true) {
			try {
				server = socket.accept();
				System.out.println("Incoming client " + manyClients + ": " + server.getRemoteSocketAddress());
				clients[manyClients] = server;
				manyClients++;
				for(Socket s : clients){
					in = new DataInputStream(server.getInputStream());
					out = new DataOutputStream(server.getOutputStream());
					int request = in.readInt();
					System.out.println("Recieved request " + request);
					if(request == 0){
						System.out.println("Sending bytes");
						byte[] sending = new byte[2];
						sending[0] = 5;
						sending[1] = 19;
						out.write(sending);
					}else if(request == 1){
						System.out.println("Sending string");
						out.writeUTF("This is the string that was sent");
					}	
				}
			} catch (SocketTimeoutException k) {
				System.err.println("Socket timed out!");
			} catch(EOFException e){
				System.out.println("Client " + manyClients + "disconnected");
				manyClients--;
				continue;
			} catch (IOException e) {
				e.printStackTrace();
				break;
			}
		}
	}
}
