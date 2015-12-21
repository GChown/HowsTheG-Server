import java.net.Socket;
import java.io.IOException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
public class Client{
	static DataInputStream din;
	static DataOutputStream dout;
	static Socket sock;
	public static void main(String[] args){
		try{
		sock = new Socket("localhost", 8000);
		din = new DataInputStream(sock.getInputStream());
		dout = new DataOutputStream(sock.getOutputStream());
		dout.writeInt(1);
		String text = din.readUTF();
		System.out.println(text);
		dout.writeInt(0);
		byte[] incoming = new byte[2];
		din.read(incoming);
		System.out.println("Reading bytes:");
		for(byte b : incoming){
			System.out.println(b);
		}
		}catch(IOException e){
			e.printStackTrace();
		}

	}
}
