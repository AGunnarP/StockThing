package babyanthony.me.stockthing;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.sql.*;

public class DartThrower {
	
	//Driver and database strings
	static final String driver = "com.mysql.cj.jdbc.Driver";
	static final String database = "jdbc:mysql://localhost/babyanthony";
	
	//Database credentials
	static final String user = "root"; 
	static final String password = "Password13";
	
	public static void main(String args[]) {
		
		Connection myConnection = null;
			
		try {
			
			//Instantiate Driver Object
			Class.forName(driver).newInstance();
			
			//Create Connection Object using credentials
			myConnection = DriverManager.getConnection(database, user, password);
			
			System.out.println("Connection Established!");
			
		} catch (SQLException | InstantiationException | IllegalAccessException | ClassNotFoundException e) {
			e.printStackTrace();
			return;
		}
		
		//Scans inventory to sell stock.
		scanInventory(myConnection);
		
		//Scans website to buy stock.
		scanMarket(myConnection);
		
		//Attempt to close the connection
		try {
			myConnection.close();
		}catch(SQLException e) {
			e.printStackTrace();
		}
		
		return;
	}
	
	
	
	public static void updateStockValues(String symbol, Connection myConnection) {// Updates stock price on the database with info from yahoo finance.
		
		//Will be initialized
		Statement stmt = null;
		
		try {
			
			//Initialized Statement
			stmt = myConnection.createStatement();
			
			// Used to store html output to eventually obtain stock values from website.
			String lines[] = new String[100];
			
			URL url = new URL("https://finance.yahoo.com/quote/" + symbol + "/history?p=" + symbol);
			URLConnection urlConn= url.openConnection();
			
			
			//Input stream reader used to read html output from yahoo.
			InputStreamReader inputReader = new InputStreamReader(urlConn.getInputStream());
			BufferedReader buff = new BufferedReader(inputReader);
			
			
			//Regular expression applied to filter out the adjusted close value of stock from html output.
			String regex = "\"adjclose\":(\\w|\\.)+";
			Pattern pattern = Pattern.compile(regex);	
			
			String line = buff.readLine();
			int i = 0;
			
			//Uses buffered reader to read the html output from website and throws it into a String array object.
			while(line != null) {
				lines[i] = line;
				line = buff.readLine();
				i++;
			}
			
			//Array object for the adjusted close value to be stores once filtered from html output. 30 corresponds to the last 30 days of the month.
			String data[] = new String[30];
			Matcher matcher;
			
			int c = 0;
			
			//Loop applies regular ex to lines array to filter out adj close value and apply it to data array.
			for(int j = 0; j < lines.length; j++) {
				
				try {
					matcher = pattern.matcher(lines[j]);
				}catch(NullPointerException e) {
					break;
				}
				
				while(matcher.find()) {
					
					if(c == 30) {
						break;
					}
					else {
						data[c] = (matcher.group(0));
						c++;
					}
					
				}
				
			}
			
			String regex2 = "(\\d|\\.)+";
			Pattern pattern2 = Pattern.compile(regex2);
			Matcher matcher2;
			
			//Second regular expression loop to filter out the adjclose value into a specific integer.
			for(int l = 0; l < data.length; l++) {
				
				matcher2 = pattern2.matcher(data[l]);
				
				while(matcher2.find()) {
					data[l] = matcher2.group(0);
				}
			}
			
			//Deletes all previous rows pertaining to updated stock.
			stmt.executeUpdate("DELETE FROM stockvalues WHERE symbol='" + symbol + "';");
			
			for(int p = 0; p < data.length; p++) {
				//inserts updated stock values.
				stmt.executeUpdate("INSERT INTO stockvalues (hour,symbol,stockvalue) VALUES (" + (p + 1) + ",'" + symbol + "'," 
					+ Double.parseDouble(data[p]) + ");");
			}
			
		}catch(IOException | SQLException e) {
			e.printStackTrace();
		}
		
		return;
	}

	
	
	public static void scanInventory(Connection myConnection){
		
		//Will initialize later
		Statement stmt = null;
		
		try {
			
			//Initialized statement
			stmt = myConnection.createStatement();
			
			//Creates temporary table to create a table with a bunch of row numbers because I didn't know what AUTO_INCREMENT was at the time :embarassed:
			stmt.executeUpdate("CREATE TEMPORARY TABLE babyanthony.tempinventory( select ROW_NUMBER() OVER( ORDER BY SYMBOL) "
					+ "AS rownum, symbol, shares FROM inventory);");
			
			int x = 1;
			boolean runningScan = true;
			
			while(runningScan) {
				
				String symbol = "";
				int shares = 0;
				double stockValue = 0;
				
				//Result set methodically gets each stock from the inventory table to determine whether to sell it or not.
				ResultSet rs = stmt.executeQuery("SELECT symbol,shares FROM babyanthony.tempinventory WHERE rownum=" + x + ";");
				
				//Run first loop to get symbol and shares vars
				while(rs.next()) {
					symbol = rs.getString("symbol");
					shares = rs.getInt("shares");
				}
				
				//Check to see if the symbol does not exist, meaning the symbols have ran out
				if(symbol == null || symbol.isEmpty()) {
					runningScan = false;
					break;
				}
				
				//Hour 1 corresponds to current stock value in database.
				ResultSet rs2 = stmt.executeQuery("Select stockvalue FROM stockvalues WHERE symbol='" + symbol + "' AND hour=1;");
				
				//Run the second loop to get the stock's value
				while(rs2.next()) {
					stockValue = rs2.getDouble("stockvalue");
				}
				
				//Calls scanstock function which determines whether stock is worthy to buy or sell.
				Decision decision = scanStock(myConnection, symbol);
				
				//All this does is sell the stock and update balance.
				if(decision == Decision.SELL) {
					
					double balance = 0;
					double subtotal = (shares * stockValue);
					
					ResultSet rs1 = stmt.executeQuery("SELECT balance FROM balancesheet WHERE thing='balance';");
					
					while(rs1.next()) {
						balance = rs1.getDouble("balance");
					}
					
					double newBalance = balance + subtotal;
					
					stmt.executeUpdate("UPDATE balancesheet SET balance=" + newBalance + " WHERE thing='balance';");
					stmt.executeUpdate("DELETE FROM inventory WHERE symbol='" + symbol + "';");
					
					System.out.println("Sold " + shares + " shares of " + symbol + " for " + subtotal + "!");
				}
				x++;
			}
			
			stmt.executeUpdate("DROP TABLE babyanthony.tempinventory;");
		
		}catch(SQLException e) {
			e.printStackTrace();
		} 
		finally {
			if(stmt != null) {
				try {
					stmt.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
		return;
	}
		

	
	public static Decision scanStock(Connection myConnection, String symbol) {
		
		//Will be initialized
		Statement stmt = null;
		
		//This is where update stock values function is called to limit the amount of times it needs to be called.
		updateStockValues(symbol, myConnection);
		
		//Enum variable that will determine whether to buy or sell
		Decision buyOrSell = null;
		
		//Object that will contain the stockvalue data.
		double[] data = new double[30];
		
		
		//Gets the stockvalue from the last 30 days.
		try {
			
			//Initialized Statement
			stmt = myConnection.createStatement();
			
				for(int j = 0; j < data.length; j++) {
					
					ResultSet rs= stmt.executeQuery("SELECT stockvalue FROM stockvalues WHERE hour="+j+" AND symbol='"+symbol+"';");
					
					while(rs.next()) {
						data[j] = rs.getDouble("stockvalue");
					}
					
				}
				
				double mean = getMean(data);
				double Sx = getStandardDeviation(data, mean);
				double zScores[] = getZscores(data, mean, Sx);
				
				//zScores have to do with percentiles 
				if(zScores[1] < (-.674)) {
					
					//if the stock is in the 75th percentile, it will be set to sell.
					buyOrSell = Decision.BUY;
				}
				else if(zScores[1] > .674) {
					
					//If the stock is in the 25th percentile for the month, it will set to buy.
					buyOrSell = Decision.SELL;
				}

		}catch(SQLException e) {
			e.printStackTrace();
		}
		finally {
			if(stmt != null) {
				try {
					stmt.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
	    }
		
		return buyOrSell;
	}
	
	
	
	public static void scanMarket(Connection myConnection){//Same stuff as scan inventory but using stocks table to find the stocks on the market.
		
		//Will be initialized
		Statement stmt = null;
		
		String inventory[] = new String[100];
		
		try {
			
			//Initialized Statement
			stmt = myConnection.createStatement();
			
			stmt.executeUpdate("CREATE TEMPORARY TABLE babyanthony.tempinventory( select ROW_NUMBER() OVER( ORDER BY SYMBOL) "
					+ "AS rownum, symbol, shares FROM inventory);");
			stmt.executeUpdate("CREATE TEMPORARY TABLE babyanthony.tempstocks( select ROW_NUMBER() OVER( ORDER BY SYMBOL) "
					+ "AS rownum, symbol, stockindex FROM stocks);");
			
			
			//Gets symbol from the symbol table to check whether to buy or sell
			for(int i = 0; i < 100; i++) {
				
				ResultSet rs = stmt.executeQuery("SELECT symbol FROM babyanthony.tempinventory WHERE rownum=" + i + ";");
				
				while(rs.next()) {
					inventory[i]= rs.getString("symbol");
				}
				
			}
			
			
			int invLength = inventory.length;
			int x = 1;
			boolean runningScan = true;
			
			while(runningScan) {
				
				String symbol = "";
				
				ResultSet rs = stmt.executeQuery("SELECT symbol FROM babyanthony.tempstocks WHERE rownum="+ x +";");
				
				while(rs.next()) {
					symbol = rs.getString("symbol");
				}
				
				//Uses same concept that scanInventory() uses to make sure the table doesn't run out of stocks.
				if(symbol == null || symbol.isEmpty()) {
					runningScan = false;
					break;
				}
				
				boolean bool = true;
				
				//Checks to see if stocks that the machine is checking has not already been bought and in inventory!
				for(int j = 1; j < invLength; j++) {
					if(symbol.equals(inventory[j])) {
						bool = false;
					}
				}
				
				if(bool) {
					
					//Decision variable
					Decision buyOrSell = scanStock(myConnection, symbol);
					
					//All this does is buys the stock by subracting funds from balancesheet and updating the inventory
					if(buyOrSell == Decision.BUY) {
						
						double balance = 0;
						double shareprice = 0;
						
						ResultSet rs1= stmt.executeQuery("SELECT balance FROM balancesheet WHERE thing='balance';");
						
						while(rs1.next()) {
							balance=rs1.getDouble("balance");
						}
						
						//Puts a $600 limit on purchases so the machine doesn't bankrupt itself with Tesla
						if(balance > 600) {
							
							ResultSet rs2=stmt.executeQuery("SELECT stockvalue FROM stockvalues WHERE symbol='"+symbol+"' AND hour=1;");
							
							while(rs2.next()) {
								shareprice = rs2.getDouble("stockvalue");
							}
							
							double bonds = Math.floor(600 / shareprice);
							
							if(bonds > 0) {
								double newBalance = balance-(shareprice*bonds);
								
								stmt.executeUpdate("UPDATE balancesheet SET balance=" + newBalance + " WHERE thing='balance'");
								stmt.executeUpdate("INSERT INTO inventory (symbol,shares) VALUES('" + symbol + "', " + bonds + ");");
								
								System.out.println("Bought " + bonds + " shares of " + symbol + "!");
							}
							
						}
						
					}
				}

				x++;
			}
			
			stmt.executeUpdate("DROP TABLE babyanthony.tempstocks;");
			stmt.executeUpdate("DROP TABLE babyanthony.tempinventory;");
			
		}catch(SQLException e){
			e.printStackTrace();
		}
		finally {
	        if (stmt != null) {
	        	try {
	        		stmt.close();
	        	} catch (SQLException e) {
	        		e.printStackTrace();
	        	} 
	        }
	    }
		
		return;
	}
	
	
	//Code is not complete, never called. It is supposed to give an evaluation on your inventory.
	public static String getEvaluation(Connection myConnection){
		java.sql.Statement stmt=null;
		String symbolcheck=null;
		String total=null;
		try {
			stmt=myConnection.createStatement();
			stmt.executeUpdate("CREATE TEMPORARY TABLE babyanthony.tempinventory( select ROW_NUMBER() OVER( ORDER BY SYMBOL) AS rownum, symbol, shares FROM inventory);");
			String inventory[]= new String[100];
			int[] shares= new int[100];
			int i=1;
			while(true) {
				ResultSet rs=stmt.executeQuery("SELECT symbol,shares FROM babyanthony.tempinventory WHERE rownum="+i+";");
				while(rs.next()) {
					 inventory[i]=rs.getString("symbol");
					 shares[i]=rs.getInt("shares");
				}
				if(symbolcheck.equals(inventory[i])) {
					break;
				}
				symbolcheck=inventory[i];
				i++;
			}
			int Length=inventory.length;
			String symbol=null;
			double subtotal=0;
			for(int j=0;i<Length;i++) {
				symbol=inventory[j];
				ResultSet rs=stmt.executeQuery("SELECT stockvalue FROM stockvalues WHERE symbol='"+symbol+"' AND hour=168;");
				while(rs.next()) {
					subtotal=subtotal+((rs.getDouble("stockvalue"))*shares[j]);
				}
			}
			ResultSet rs=stmt.executeQuery("SELECT balance FROM balancesheet where thing='balance");
			while(rs.next()) {
				subtotal=subtotal+rs.getDouble("balance");
			}
			 total="Your total value is "+subtotal+"dollars!";
		}catch(SQLException e) {
			e.printStackTrace();
		}
		return total;
	}
	
	
	
	//Hella radical math functions.
	public static double getSum(double data[]) {
		
		int datLength = data.length;
		double sum = 0;
		
		for(int i = 0; i < datLength; i++) {
			sum = sum + data[i];
		}
		
		return sum;
	}
	
	public static double getMean(double data[]) {
		
		int datLength = data.length;
    	double sum = 0;
    	
    	for(int i = 0; i < datLength; i++) {
    		sum=sum + data[i];
    	}
    	
    	return sum / datLength;
    }
	
	public static double getMedian(double data[]) {
		
		int datLength = data.length;
    	double median = 0;
    	
    	//Calculates Median
        if((datLength % 2) == 0) {
            median = (data[datLength / 2] + data[datLength / 2 - 1]) / 2;
        }
        else {
            median = data[Math.round(datLength / 2)];
        }
        
        return median;
    }
	
	public static double getStandardDeviation(double data[], double mean) {
		
		int datLength = data.length;
    	double sd = 0;
    	
        for(int i = 0; i < datLength; i++) {//Gets Standard Deviation
            sd = sd + ((data[i] - mean) * (data[i]) - mean);
        }
        sd = sd / (datLength - 1);
        sd = Math.sqrt(sd);
        
        return sd;
    }
	
	//returns a double array where 0 = Q1, 1 = Q3, and 2 = IQR
	public static double[] getQuartileData(double data[]) {
		
		int datLength = data.length;
    	double[] quartileData = {1, 2, 3};
    	
    	double Q1 = 0, Q3 = 0, IQR = 0;
    		
    	
        if((datLength % 2) == 0 && !( ((datLength / 2) % 2) == 0)){//good
        		 
        	Q1 = data[Math.round(datLength / 4)];

        	Q3 = data[Math.round(datLength * (3/4))];
        	     
        	IQR = Q3 - Q1;
        	
        }
        else if((datLength % 2) == 0 && ((datLength / 2) % 2) == 0){//good
        	
        	Q1 = ((data[datLength / 4] + data[(datLength / 4) - 1]) / 2);

        	Q3 = ((data[datLength * (3/4)] + data[(datLength * (3/4)) - 1]) / 2);
        	     
        	IQR = Q3 - Q1;
        
        }
        else if(!((datLength % 2) == 0) && (((datLength - 1) / 2) % 2) == 0){//good
        	     
        	Q1 = (data[(datLength - 1) / 4] + data[(datLength - 1) / 4] - 1) / 2;
     
        	Q3 = (data[datLength * (3/4)] + data[datLength * (3/4) + 1]) / 2;
        	             	     
        	IQR = Q3 - Q1;
        	
        }
        else if(!((datLength % 2) == 0) && !((((datLength - 1) / 2) % 2) == 0)){//good
        	     
        	Q1 = data[Math.round((datLength - 1) / 4)];
        	     
        	Q3 = data[Math.round((datLength) * (3/4))];
        	             	     
        	IQR = Q3 - Q1;
        
        }
        
        quartileData[0] = Q1;
        quartileData[1] = Q3;
        quartileData[2] = IQR;
    	
        return quartileData;
    }
	
	public static double[] getZscores(double data[], double mean, double Sx) {
		
		int datLength = data.length;
		
    	double[] zscores = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10,
    						11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 
    						21, 22, 23, 24, 25, 26, 27, 28, 29, 30
    						};
        
    	for(int i = 0; i < datLength; i++) {
        	double zscore = (Math.round((data[i] - mean) / Sx * 100));
        	zscores[i] = (zscore / 100);
        }
    	
        return zscores;
    }
	
}