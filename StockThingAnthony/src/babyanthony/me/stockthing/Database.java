package babyanthony.me.stockthing;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {

	public Database() {}
	
	//Driver and database strings
	private final String driver = "com.mysql.cj.jdbc.Driver";
	private final String databasePath = "jdbc:mysql://localhost/babyanthony";
	
	//Database credentials
	private final String userCred = "root"; 
	private final String passwordCred = "Password13";
	
	//Database connection and statement
	private Connection sqlConnection;
	
	//Getters
	public String getDriver() {
	    return this.driver;
	}
	
	public String getDatabasePath() {
	    return this.databasePath;
	}
	
	public String getUserCred() {
	    return this.userCred;
	}
	
	public String getPasswordCred() {
	    return this.passwordCred;
	}
	
	public Connection getConnection() {
	    return this.sqlConnection;
	}
	
	//Creates the connection object
	public void createConnection() {
	    try {
		//Instantiate Driver Object
		Class.forName(this.driver).newInstance();
			
		//Create Connection Object using credentials
		this.sqlConnection = DriverManager.getConnection(this.databasePath, this.userCred, this.passwordCred);
		
		System.out.println("Connection Established!");
	    }catch(SQLException | InstantiationException | IllegalAccessException | ClassNotFoundException e) {
		e.printStackTrace();
	    }
	}
	
	//Closes the current connection
	public void closeConnection() {
	    try {
		this.sqlConnection.close();
	    }catch(SQLException e) {
		e.printStackTrace();
	    }
	}
	
	//Returns a new statement object
	public Statement createStatement() {
	    try {
		return this.sqlConnection.createStatement();
	    }catch(SQLException e) {
		e.printStackTrace();
		return null;
	    }
	}
	
	//Closes the given statement object, returns if null
	public void closeStatement(Statement stmt) {
	    if(stmt == null) {
		return;
	    }
	    try {
		stmt.close();
	    } catch (SQLException e) {
		e.printStackTrace();
	    }
	}
	
	//Statement execution wrapper
	public void executeState(Statement stmt, String cmd) {
	    try {
		stmt.executeUpdate(cmd);
	    }catch(SQLException e) {
		e.printStackTrace();
	    }
	}
	
	//Statement query wrapper
	public ResultSet getResultSet(Statement stmt, String cmd) {
	    try {
		return stmt.executeQuery(cmd);
	    }catch(SQLException e) {
		e.printStackTrace();
	    }
	    return null;
	}
}
