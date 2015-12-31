import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;

class ClientThread implements Runnable{
		private Socket socket;
		private int request;
		private String hostname;
		private DataInputStream in;
		private DataOutputStream out;
		private ServerThread host;
		
		ClientThread(Socket sock, ServerThread host){
			this.socket = sock;
			this.host = host;
			try{
				in = new DataInputStream(socket.getInputStream());
				out = new DataOutputStream(socket.getOutputStream());
				request = -1;
				this.hostname = in.readUTF();
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
				System.out.println("Recieved request " + request + " from client " + hostname + "; ");
				
				
				//Incoming rating
				if(request == 0){
					int usrRating = in.readInt();
					if(usrRating > 6 || usrRating < 1){
						out.writeUTF("Error: Must be between 1-5");
						break;
					}
					host.addScore(usrRating, hostname);
					
				//Output rating and numVotes
				}else if(request == 1){
					float avg = host.getAverage();
					int num = host.getNumVotes();
					System.out.println("Sending score " + avg + ", numVotes " + num);
					out.writeFloat(avg);
					out.writeInt(num);
					
					
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
		
		public String getHostname(){
			return hostname;
		}
	}
