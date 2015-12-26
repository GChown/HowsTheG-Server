import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.EOFException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class ServerThread extends Thread {
	private ServerSocket socket;
	private Socket server;
	private DataInputStream in;
	private DataOutputStream out;
	private float rating;
	private int numVotes;
	private float totRating;

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
		rating = 0;
		numVotes = 0;
		totRating = 0;
	}

	public void run() {
		boolean listen = true;
		while (listen) {
			try {
				server = socket.accept();
				System.out.println("Connection from " + server);
				new Thread(new ClientThread(server)).start();
			} catch (SocketTimeoutException k) {
				System.err.println("Socket timed out!");
			} catch(EOFException e){
				System.out.println("Client disconnected");
				continue;
			} catch (IOException e) {
				e.printStackTrace();
				break;
			}
		}
	}
	class ClientThread implements Runnable{
		Socket socket;
		int request;
		ClientThread(Socket sock){
			this.socket = sock;
			try{
				in = new DataInputStream(socket.getInputStream());
				out = new DataOutputStream(socket.getOutputStream());
				request = -1;
			}catch(EOFException e){
				System.out.println("Client disconnected");
			}catch(IOException e){
				e.printStackTrace();
			}
		}
		public void run(){
			boolean runIt = true;
			while(runIt){
			try{
				request = in.readInt();
				//Request 0 is send Rating (incoming), request 1 is get rating and numVotes
				System.out.println("Recieved request " + request + " from client " + socket.getInetAddress().getHostName() + "; ");
				if(request == 0){
					System.out.print("Incoming score: ");
					int usrRating = in.readInt();
					if(usrRating > 6 || usrRating < 1){
						out.writeUTF("Error: Must be between 1-5");
						break;
					}
					System.out.print(usrRating);
					numVotes++;
					totRating += usrRating;
					rating = totRating / numVotes;
					rating = ((float) Math.floor(rating * 10)) / 10;
					System.out.println(", number of votes: " + numVotes + ", total rating: " + totRating
							+ ", average rating: " + rating);
				}else if(request == 1){
					System.out.println("Sending score " + rating + ", numVotes " + numVotes);
					out.writeFloat(rating);
					out.writeInt(numVotes);
				}else{
					System.out.println("Bad request recieved");
				}
			}catch(EOFException e){
				System.out.println("Client disconnected");
				runIt = false;
			}catch(java.net.SocketException e){
				System.out.println("Error with client " + socket); 
				e.printStackTrace();
			}catch(IOException e){
				e.printStackTrace();
			}
			}
		}
	}
}
