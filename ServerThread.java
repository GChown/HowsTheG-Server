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
import java.util.Date;

public class ServerThread extends Thread {
	private ServerSocket socket;
	private Socket server;
	private Connection sqlCon;
	private float rating;
	private int numVotes;
	private float totRating;
	private char meal;
	private int hour;
	private int numComment;

	public static void main(String[] args) {
		try {
			new ServerThread().start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public ServerThread() throws IOException {
		socket = new ServerSocket(8000);
		hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
		sqlCon = getConnection();
		
		runTimer();
		createUsers();
		createVotes();
		createComments();
		rating = 0;
		numVotes = 0;
		totRating = 0;
		numComment = 0;
		updateNumVotes();
		updateAverage();
	}

	private void runTimer(){
		//Every hour check if the meal has changed; 
		//if it has, update the variable meal.
		Calendar timerSet = Calendar.getInstance();
		timerSet.set(Calendar.HOUR, 0);
		timerSet.set(Calendar.MINUTE, 0);
		timerSet.set(Calendar.SECOND, 1);
		Timer timer = new Timer();
		timer.schedule(new TimerTask(){
			public void run() {
				Calendar rightNow = Calendar.getInstance();
				int hour = rightNow.get(Calendar.HOUR_OF_DAY);
				if(hour == 0)
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
					+ "numvote int NOT NULL DEFAULT 0, timesent datetime NOT NULL);");
			create.executeUpdate();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	/**
	 * Create the votes table(V_ID, U_ID, vote_b, vote_l, vote_d)
	 */
	private void createVotes(){
		try {
			PreparedStatement create = sqlCon.prepareStatement(
					"CREATE TABLE IF NOT EXISTS vote(V_ID int NOT NULL AUTO_INCREMENT PRIMARY KEY, "
					+ "uid int NOT NULL, FOREIGN KEY (uid) REFERENCES user(U_ID), vote_b INT, vote_l INT, vote_d INT,"
					+ "lastvote datetime NOT NULL);");
			create.executeUpdate();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static Connection getConnection(){
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
				PreparedStatement create = sqlCon
					.prepareStatement("INSERT INTO user(device) VALUES (?) "
							+ "ON DUPLICATE KEY UPDATE device=device;");
				create.setString(1, client.getDeviceName());
				create.executeUpdate();
				new Thread(client).start();
			} catch (SQLException e) {
				System.out.println("Database dead, restarting");
				sqlCon = getConnection();
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
			PreparedStatement stmt = sqlCon.prepareStatement("SELECT U_ID FROM user WHERE device=?;");
			stmt.setString(1, devid);
			ResultSet results = stmt.executeQuery();
			if(results.next()){
				return results.getInt(1);
			}
			
		}catch(SQLException e){
			e.printStackTrace();
		}
		return 0;
	}

	public void updateAverage() {
		try {
			PreparedStatement stmt = sqlCon.prepareStatement("SELECT AVG(v1.vote_" + meal + ") FROM vote "
				+ "v1 inner join (select V_ID, uid, max(lastvote) as MaxDate from vote group by uid) v2 "
				+ "on v1.uid = v2.uid and v1.lastvote = v2.MaxDate where lastvote between ? and ?");
			//Select avg of vote at current meal between midnight last night and right now.
			Date now = new Date();                      
			Calendar cal = Calendar.getInstance();      
			cal.setTime(now);                           
			cal.set(Calendar.HOUR_OF_DAY, 0);           
			cal.set(Calendar.MINUTE, 0);                
			cal.set(Calendar.SECOND, 0);                
			cal.set(Calendar.MILLISECOND, 0);           
			Date zeroedDate = cal.getTime();
			stmt.setTimestamp(1, new java.sql.Timestamp(zeroedDate.getTime()));
			stmt.setTimestamp(2, new java.sql.Timestamp(now.getTime()));
			ResultSet results = stmt.executeQuery();
			if(results.next())
				rating = results.getFloat(1);
			System.out.println("Updated rating " + rating);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void updateNumVotes() {
		try {
			PreparedStatement stmt = sqlCon.prepareStatement("SELECT COUNT(v1.vote_" + meal + ") FROM vote "
				+ "v1 inner join (select V_ID, uid, max(lastvote) as MaxDate from vote group by uid) v2 "
				+ "on v1.uid = v2.uid and v1.lastvote = v2.MaxDate where lastvote between ? and ?");
			Date now = new Date();                      
			Calendar cal = Calendar.getInstance();      
			cal.setTime(now);                           
			cal.set(Calendar.HOUR_OF_DAY, 0);           
			cal.set(Calendar.MINUTE, 0);                
			cal.set(Calendar.SECOND, 0);                
			cal.set(Calendar.MILLISECOND, 0);           
			Date zeroedDate = cal.getTime();
			stmt.setTimestamp(1, new java.sql.Timestamp(zeroedDate.getTime()));
			stmt.setTimestamp(2, new java.sql.Timestamp(now.getTime()));
			ResultSet results = stmt.executeQuery();
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

	public int getNumComments(){
		return numComment;
	}

	public void addScore(int score, int clientid) {
		try {
			//Insert score into database
			PreparedStatement create = sqlCon
				.prepareStatement("INSERT INTO vote (uid, vote_" + meal + ", lastvote) VALUES(?,?,?);");
			create.setInt(1, clientid);
			create.setInt(2, score);
			create.setTimestamp(3, new java.sql.Timestamp(new java.util.Date().getTime()));
			create.executeUpdate();
			updateAverage();
			updateNumVotes();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void updateNumComments(){
		//try {
			//Statement stmt = sqlCon.createStatement();
			//ResultSet results = stmt.executeQuery("SELECT COUNT(*) FROM comment WHERE ");
			//if(results.next())
				//numComment = results.getInt(1);
		//} catch (SQLException e) {
			//e.printStackTrace();
		//}
	}

	public void addComment(String comment, int clientid){
		if(comment.length() == 0) return;
		try{
			PreparedStatement create = sqlCon.prepareStatement("INSERT INTO comment(uid, text, timesent) VALUES"
					+ "(?, ?, ?);");
			create.setInt(1, clientid);
			create.setString(2, comment);
			create.setTimestamp(3, new java.sql.Timestamp(new java.util.Date().getTime()));
			create.executeUpdate();
			updateNumComments();
		}catch(SQLException e){
			e.printStackTrace();
		}
	}

	public String[] getComments(){
		String[] comments = new String[numComment];
		//Do something here. I'm not really sure yet...
		/*try{
			Statement stmt = sqlCon.createStatement();
			ResultSet results = stmt.executeQuery("");
			if(results.next())
				numComment = results.getInt(1);
		}catch(SQLException e){
			System.out.println("Error getting comment: " + e);
		}*/
		return comments;
	}
	public void setUsername(String uname, int uid){

	}
}
