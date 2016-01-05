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
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

public class ServerThread extends Thread {
	private ServerSocket socket;
	private Socket server;
	private Connection sqlCon;
	private float rating;
	private int numVotes;
	private float totRating;
	private static String tableName;
	private char meal;
	private int hour;
	private Calendar calendar;


	public static void main(String[] args) {
		try {
			new ServerThread().start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public ServerThread() throws IOException {
		socket = new ServerSocket(8000);
		calendar = Calendar.getInstance();
		hour = calendar.get(Calendar.HOUR_OF_DAY);
		//Table name is _2016_01_31 for example
		tableName = "_" + calendar.get(Calendar.YEAR) + "_" +  calendar.get(Calendar.MONTH) 
			+ "_" + calendar.get(Calendar.DAY_OF_MONTH);
		System.out.println("Running on database " + tableName + " at " + socket);
		rating = 0;
		numVotes = 0;
		totRating = 0;
		meal = 'b';
		//Every hour check if the meal has changed; 
		//if it has, update the variable
		Timer timer = new Timer();
		timer.schedule(new TimerTask(){
				public void run() {
				if(hour > 8 && hour < 10){
				//It's breakfast
				meal = 'b';}
				if(hour >= 10 && hour < 14){
				//Lunch time
				meal = 'l';}
				if(hour >= 14){
				//Dinner time
				meal = 'd';}
				else{
				meal = 'o';}
				System.out.println("Meal is " + meal + " at " + hour);
				}
				//Run every hour - shouldn't take up too much processing power
				}, 0, 60 * 60 * 1000);

		try {
			sqlCon = getConnection();
			PreparedStatement create = sqlCon.prepareStatement(
					"CREATE TABLE IF NOT EXISTS votes" + tableName + "(ID int NOT NULL AUTO_INCREMENT PRIMARY KEY, "
					+ "name varchar(255) NOT NULL UNIQUE, vote_b INT, vote_l INT, vote_d INT);");
			create.executeUpdate();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static Connection getConnection() {
		try {
			String driver = "com.mysql.jdbc.Driver";
			String url = "jdbc:mysql://localhost:3306/HowsTheGdb";
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
					.prepareStatement("INSERT INTO votes" + tableName + " (name) VALUES (?) "
							+ "ON DUPLICATE KEY UPDATE vote_" + meal + "=vote_" + meal + ";");
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

	public void refreshAverage() {
		try {
			Statement stmt = sqlCon.createStatement();
			ResultSet results = stmt.executeQuery("SELECT AVG(vote_" + meal + ") FROM votes" + tableName + ";");
			if(results.next())
				rating = results.getFloat(1);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void refreshNumVotes() {
		try {
			Statement stmt = sqlCon.createStatement();
			ResultSet results = stmt.executeQuery("SELECT COUNT(*) FROM votes" + tableName 
					+ " WHERE vote_" + meal + " IS NOT NULL;");
			if(results.next())
				numVotes = results.getInt(1);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	public float getAverage(){
		return rating;
	}
	public int getNumVotes(){
		return numVotes;
	}	

	public void addScore(int score, String hostname) {
		numVotes++;
		totRating += score;
		rating = totRating / numVotes;
		try {
			//Update score in database
			PreparedStatement create = sqlCon
				.prepareStatement("UPDATE votes" + tableName + " SET vote_" + meal + " =? WHERE name=?;");
			create.setString(1, "" + score);
			create.setString(2, hostname);
			create.executeUpdate();
			refreshAverage();
			refreshNumVotes();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
