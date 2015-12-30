import java.net.Socket;
import java.util.Scanner;
import java.io.IOException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
public class Client{
	static DataInputStream din;
	static DataOutputStream dout;
	static Socket sock;
	static Scanner keyb;
	public static void main(String[] args){
		keyb = new Scanner(System.in);
		int option = -1;
		if(args.length != 0) option = Integer.parseInt(args[0]);
		else{
			System.out.println("--Menu--\n1. Recieve Scores\n2. Send Score");
			option = keyb.nextInt();
		}
		try{
			sock = new Socket("IPGOESHERE.com", 8000);
			din = new DataInputStream(sock.getInputStream());
			dout = new DataOutputStream(sock.getOutputStream());
			if(option == 1){
				dout.writeInt(1);
				float rating = din.readFloat();
				int numVotes = din.readInt();
				System.out.println("Rating is " + rating + " (" + numVotes + ")");
			}else if(option == 2){
				//Send 0 then the rating
				System.out.println("Enter score to send:");
				int sending = keyb.nextInt();
				dout.writeInt(0);
				dout.writeInt(sending);
			}else{
				System.out.println("Should sent 1 or 2");
			}
		}catch(IOException e){
			e.printStackTrace();
		}

	}
}

