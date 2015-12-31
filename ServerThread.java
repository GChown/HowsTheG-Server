import java.io.EOFException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class ServerThread extends Thread {
	private ServerSocket socket;
	private Socket server;
	private Connection sqlCon;
	private float rating;
	private int numVotes;
	private float totRating;

	public static void main(String[] args) {
		try {
			new ServerThread().start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public ServerThread() throws IOException {
		socket = new ServerSocket(8000);
		System.out.println("Running on " + socket);
		rating = 0;
		numVotes = 0;
		totRating = 0;
		try {
			sqlCon = getConnection();
			PreparedStatement create = sqlCon.prepareStatement(
			"CREATE TABLE IF NOT EXISTS votes(ID int NOT NULL AUTO_INCREMENT PRIMARY KEY, name varchar(255) NOT NULL UNIQUE,"
			+ "vote INT);");
			create.executeUpdate();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static Connection getConnection() {
		try {
			String driver = "com.mysql.jdbc.Driver";
			String url = "jdbc:mysql://localhost:3306/testdb";
			String username = "root";
			String password = "IAintTellin";
			Class.forName(driver);
			Connection conn = DriverManager.getConnection(url, username, password);
			System.out.println("Connected to database");
			return conn;
		} catch (Exception e) {
			System.out.println(e);
		}
		return null;
	}

	public void run() {
		boolean listen = true;
		while (listen) {
			try {
				server = socket.accept();
				System.out.println("Connection from " + server.getInetAddress().getHostName() + "(" 
						+ server.getInetAddress().getHostAddress() + ")");
				ClientThread client = new ClientThread(server, this);
				// INSERT INTO 'votes' ('name') VALUES
				PreparedStatement create = sqlCon
                                	.prepareStatement("INSERT INTO votes (name) VALUES (?) ON DUPLICATE KEY UPDATE vote=vote;");
                                create.setString(1, client.getHostname());
				create.executeUpdate();
				new Thread(client).start();
			} catch (SQLException e) {
				e.printStackTrace();
			} catch (SocketTimeoutException k) {
				System.err.println("Socket timed out!");
			} catch (EOFException e) {
				System.out.println("Client disconnected");
				continue;
			} catch (IOException e) {
				e.printStackTrace();
				break;
			}
		}
	}

	public float getAverage() {
		try {
		Statement stmt = sqlCon.createStatement();
		ResultSet results = stmt.executeQuery("SELECT AVG(vote) FROM votes;");
		if(results.next())
			rating = results.getFloat(1);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return rating;
	}

	public int getNumVotes() {
		return numVotes;
	}

	public void addScore(int score, String hostname) {
		numVotes++;
		totRating += score;
		rating = totRating / numVotes;
		try {
			//Update score in database
			PreparedStatement create = sqlCon
	                        .prepareStatement("UPDATE votes SET vote=? WHERE name=?;");
			create.setString(1, "" + score);
                        create.setString(2, hostname);
			create.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		System.out.println("Incoming score " + score);
	}
}
