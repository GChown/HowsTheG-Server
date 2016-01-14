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
		sqlCon = getConnection();
		createUsers();
		createVotes();
		createComments();
		System.out.println("Running on database " + tableName);
		rating = 0;
		numVotes = 0;
		totRating = 0;
		meal = 'b';
		//Every hour check if the meal has changed; 
		//if it has, update the variable
		Calendar timerSet = Calendar.getInstance();
		timerSet.set(Calendar.HOUR, 0);
		timerSet.set(Calendar.MINUTE, 0);
		timerSet.set(Calendar.SECOND, 1);
		Timer timer = new Timer();
		timer.schedule(new TimerTask(){
			public void run() {
				hour = calendar.get(Calendar.HOUR_OF_DAY);
				if(hour == 0)
					sqlCon = getConnection();
					createVotes();
				if(hour >= 8 && hour < 11)
					meal = 'b';
				if(hour >= 11 && hour < 14)
					meal = 'l';
				if(hour >= 14 && hour < 22)
					meal = 'd';
				if(hour >= 22)
					//Temporary, make it not calculate anything after 10:00.
					meal = 'd';
				System.out.println("Hour is " + hour + " so meal is " + meal);
			}
			//Start at 12:01AM every morning and run every hour after
		}, timerSet.getTime(), 60 * 60 * 1000);
	}
	/**
	 * Create the Users table(U_ID, device, usrname)
	 */
	private void createUsers(){
		try {
			PreparedStatement create = sqlCon.prepareStatement(
					"CREATE TABLE IF NOT EXISTS user(U_ID int NOT NULL AUTO_INCREMENT PRIMARY KEY, "
					+ "device varchar(255) NOT NULL UNIQUE, usrname varchar(20));");
			create.executeUpdate();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	/**
	 * Create the Comments table(C_ID, text, U_ID)
	 */
	private void createComments(){
		try {
			PreparedStatement create = sqlCon.prepareStatement(
					"CREATE TABLE IF NOT EXISTS comment(C_ID int NOT NULL AUTO_INCREMENT PRIMARY KEY, "
					+ "uid int NOT NULL, FOREIGN KEY (uid) REFERENCES user(U_ID), text varchar(255) NOT NULL, "
					+ "numvote int NOT NULL DEFAULT 1, timesent datetime NOT NULL DEFAULT ?);");
			create.setTimestamp(1, new java.sql.Timestamp(new java.util.Date().getTime()));
			create.executeUpdate();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	/**
	 * Create the votes table(V_ID, U_ID, vote_b, vote_l, vote_d)
	 */
	private void createVotes(){
		tableName = "_" + calendar.get(Calendar.YEAR) + "_" +  calendar.get(Calendar.MONTH) 
			+ "_" + calendar.get(Calendar.DAY_OF_MONTH);
		try {
			PreparedStatement create = sqlCon.prepareStatement(
					"CREATE TABLE IF NOT EXISTS vote" + tableName + "(V_ID int NOT NULL AUTO_INCREMENT PRIMARY KEY, "
					+ "uid int NOT NULL, FOREIGN KEY (uid) REFERENCES user(U_ID), vote_b INT, vote_l INT, vote_d INT);");
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
			System.out.println("Couldn't connect to database!");
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
					.prepareStatement("INSERT INTO user(device) VALUES (?) "
							+ "ON DUPLICATE KEY UPDATE device=device;");
				create.setString(1, "" + client.getDeviceId());
				create.executeUpdate();
				create = sqlCon
					.prepareStatement("INSERT INTO vote" + tableName + "(uid) VALUES (?) "
							+ "ON DUPLICATE KEY UPDATE V_ID=V_ID;");
				create.setString(1, "" + client.getDeviceId());
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
	public int getID(String devid){
		try{
			Statement stmt = sqlCon.createStatement();
			ResultSet results = stmt.executeQuery("SELECT U_ID FROM user WHERE device=\"" + devid + "\";");
			if(results.next())
				return results.getInt(1);
		}catch(SQLException e){
				e.printStackTrace();
			}
		return 0;
		}
	public void updateAverage() {
		try {
			Statement stmt = sqlCon.createStatement();
			ResultSet results = stmt.executeQuery("SELECT AVG(vote_" + meal + ") FROM vote" + tableName + ";");
			if(results.next())
				rating = results.getFloat(1);
			System.out.println("Updated rating " + rating);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void updateNumVotes() {
		try {
			Statement stmt = sqlCon.createStatement();
			ResultSet results = stmt.executeQuery("SELECT COUNT(*) FROM vote" + tableName 
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

	public void addScore(int score, int clientid) {
		numVotes++;
		totRating += score;
		try {
			//Update score in database
			PreparedStatement create = sqlCon
				.prepareStatement("UPDATE vote" + tableName + " SET vote_" + meal + "=? WHERE uid=?;");
			create.setString(1, "" + score);
			create.setString(2, "" + clientid);
			create.executeUpdate();
			updateAverage();
			updateNumVotes();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
