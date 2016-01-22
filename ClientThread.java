import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;

class ClientThread implements Runnable{
		private Socket socket;
		private int request;
		private String deviceid;
		private DataInputStream in;
		private DataOutputStream out;
		private ServerThread host;
		private int id;		
		ClientThread(Socket sock, ServerThread host){
			this.socket = sock;
			this.host = host;
			try{
				in = new DataInputStream(socket.getInputStream());
				out = new DataOutputStream(socket.getOutputStream());
				request = -1;
				this.deviceid = in.readUTF();
				this.id = host.getID(deviceid);
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

				//Incoming rating
				if(request == 0){
					int usrRating = in.readInt();
					System.out.print("Got score from " + id + ": " + vote);
					if(usrRating > 5 || usrRating < 1){
						System.out.println("Recieved bad score: " + usrRating);
						//Entered bad score!
						break;
					}
					host.addScore(usrRating, id);
					
				//Output rating and numVotes
				}else if(request == 1){
					float avg = host.getAverage();
					int num = host.getNumVotes();
					out.writeFloat(avg);
					out.writeInt(num);
				}else if(request == 2){
					/*Incoming comment
					String comment = in.readUTF();
					System.out.println("comment \"" + comment + "\"");
					host.addComment(comment, id);*/
				}else if(request == 3){
					/*Send comments
					int numComments = host.getNumComments();*/
				}else{
					System.out.println("Bad request recieved");
				}
			}catch(EOFException e){
				System.out.println("Client disconnected");
				runIt = false;
			}catch(java.net.SocketException e){
				System.out.println("Client disconnected");
				runIt = false;
			}catch(IOException e){
				runIt = false;
				e.printStackTrace();
			}
			}
		}
		
		public int getDeviceId(){
			return id;
		}
		public String getDeviceName(){
			return deviceid;
		}
	}
