import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DartThrower {
	public static void main(String args[]) {
		Connection myConnection=null;
		String Database = "jdbc:mysql://localhost/babyanthony"; 
		String user="root"; //creates connection object with given database information to cast connection to local database.
		String password="Password13";
		try {
			Class.forName("com.mysql.cj.jdbc.Driver").newInstance();//Driver contains API's to use SQL databases in java! Need to set up in classpath.
			myConnection= DriverManager.getConnection(Database,user,password);
			System.out.println("Connection Established!");
		} catch (SQLException | InstantiationException | IllegalAccessException | ClassNotFoundException e) {
			e.printStackTrace();
		}
		java.sql.Statement stmt = null;
		try {
			stmt = myConnection.createStatement();

				//Calls for main functions of the project in this part of the instance.
				DartThrower.scanInventory(myConnection);//Scans inventory to sell stock.
				DartThrower.scanMarket(myConnection);//Scans website to buy stock.
		} catch(SQLException e) {
			e.printStackTrace();
		} finally {
	        if (stmt != null) { try {
				stmt.close();
			} catch (SQLException e) {
				e.printStackTrace();
			} }
	    }
	}
	
	public static void updateStockValues(String symbol, Connection myConnection) {// Updates stock price on the database with info from yahoo finance.
		java.sql.Statement stmt = null;
		try {
			stmt=myConnection.createStatement();
			URL url=new URL("https://finance.yahoo.com/quote/"+symbol+"/history?p="+symbol);
			String lines[]=new String[100];// Used to store html output to eventually obtain stock values from website.
			URLConnection urlConn= url.openConnection();
			InputStreamReader InputReader= new InputStreamReader(urlConn.getInputStream());//Input stream reader used to read html output from yahoo.
			BufferedReader buff= new BufferedReader(InputReader);
			
			String regularex="\"adjclose\":(\\w|\\.)+";//Regular expression applied to filter out the adjusted close value of stock from html output.
			Pattern pattern= Pattern.compile(regularex);
			
			
			String line=buff.readLine();
			int i=0;
			while( line !=null) {//Uses buffered reader to read the html output from website and throws it into a String array object.
				lines[i]=line;
				//System.out.println(line); //For giggles
				line=buff.readLine();
				i++;
			}
			String Data[]= new String[30];//Array object for the adjusted close value to be stores once filtered from html output. 30 corresponds to the last 30 days of the month.
			Matcher matcher=null;
			int c=0;
			for(int j=0;j<lines.length;j++) {//Loop applies regular ex to lines array to filter out adj close value and apply it to data array.
				try {
				 matcher = pattern.matcher(lines[j]);
				}catch(NullPointerException e) {
					break;
				}
				while(matcher.find()==true) {
					if(c==30) {
						break;
					}
					Data[c]=(matcher.group(0));
					c++;
				}
			}
			String regularex2="(\\d|\\.)+";
			Pattern pattern2= Pattern.compile(regularex2);
			Matcher matcher2=null;
			for(int l=0;l<Data.length;l++) {//Second regular expression loop to filter out the adjclose value into a specific integer.
				
				matcher2=pattern2.matcher(Data[l]);
				while(matcher2.find()==true) {
					Data[l]=matcher2.group(0);
				}
			}
			stmt.executeUpdate("DELETE FROM stockvalues WHERE symbol='"+symbol+"';");//Deletes all previous rows pertaining to updated stock.
			for(int p=0;p<Data.length;p++) {
				stmt.executeUpdate("INSERT INTO stockvalues (hour,symbol,stockvalue) VALUES ("+(p+1)+",'"+symbol+"',"+Double.parseDouble(Data[p])+");");
				//inserts updated stock values.
			}
			
	
			//System.out.println(line56);
		}catch(IOException | SQLException e) {
			e.printStackTrace();
		}
	}

	public static void scanInventory(Connection myConnection){
		java.sql.Statement stmt=null;
		String symbol=null;
		int shares=0;
		double stockvalue=0;
		try {
			stmt=myConnection.createStatement();
			stmt.executeUpdate("CREATE TEMPORARY TABLE babyanthony.tempinventory( select ROW_NUMBER() OVER( ORDER BY SYMBOL) AS rownum, symbol, shares FROM inventory);");//Creates temporary table to create a table with a bunch of row numbers because I didn't know what AUTO_INCREMENT was at the time :embarassed:
			String symbolcheck=null;
			int i=1;
			while(true) {//while(true) muahahahaha *The while loop only stops if the data in the middle of the loop changes so it is set to true*.
				ResultSet rs=stmt.executeQuery("SELECT symbol,shares FROM babyanthony.tempinventory WHERE rownum="+i+";");//Result set methodically gets each stock from the inventory table to determine whether to sell it or not.
				while(rs.next()) {
					symbol= rs.getString("symbol");
					shares= rs.getInt("shares");
				}
				//System.out.println(symbolcheck+".equals "+(symbol));
				try {//Checks to see if the query ran out of stocks in the inventor by breaking loop if it is using the same stock from the last incursion of the query.
					System.out.println(symbol+"=="+symbolcheck);
				if(symbolcheck.equals(symbol)) {
					System.out.println("Skrrrting to a halt");
					break;
				}
				}catch(NullPointerException e){
					if (symbol==null) {
						break;
					}
				}
				ResultSet rs2=stmt.executeQuery("Select stockvalue FROM stockvalues WHERE symbol='"+symbol+"' AND hour=1;");//hour 1 corresponds to current stock value in database.
				while(rs2.next()) {
					stockvalue=rs2.getDouble("stockvalue");
				}
				String Decision= scanStock(myConnection,symbol);//Calls scanstock function which determines whether stock is worthy to buy or sell.
				if(Decision=="Sell") {//All this does is sell the stock and update balance.
					double balance=0;
					double subtotal= (shares*stockvalue);
					ResultSet rs1= stmt.executeQuery("SELECT balance FROM balancesheet WHERE thing='balance';");
					while(rs1.next()) {
						balance= rs1.getDouble("balance");
					}
					double newbalance= balance+subtotal;
					stmt.executeUpdate("UPDATE balancesheet SET balance="+newbalance+" WHERE thing='balance';");
					stmt.executeUpdate("DELETE FROM inventory WHERE symbol='"+symbol+"';");
					System.out.println("Sold "+shares+" shares of "+symbol+" for "+subtotal+"!");
				}
				i++;
				symbolcheck=symbol;//Setting the symbolcheck to be the last incursion of the stock.
				}
			stmt.executeUpdate("DROP TABLE babyanthony.tempinventory;");
		}catch(SQLException e) {
			e.printStackTrace();
		}finally {
	        if (stmt != null) { try {
				stmt.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} }
	    }
	}
		
	public static String scanStock(Connection myConnection,String symbol) {
		DartThrower.updateStockValues(symbol, myConnection);//This is where update stock values function is called to limit the amount of times it needs to be called.
		String buyorsell=null;//return string to determine whether to buy or sell.
		double Data[]= {1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30};//Object that will contain the stockvalue data.
		java.sql.Statement stmt=null;
		try {//Gets the stockvalue from the last 30 days.
			stmt=myConnection.createStatement();
				for(int j=0;j<30;j++) {
					ResultSet rs= stmt.executeQuery("SELECT stockvalue FROM stockvalues WHERE hour="+j+" AND symbol='"+symbol+"';");
					while(rs.next()) {
						Data[j]=rs.getDouble("stockvalue");
					}
				}
				double mean=getMean(Data);
				double Sx=getStandardDeviation(Data,mean);
				double zScores[]=getZscores(Data,mean,Sx);
				if(zScores[1]<(-.674)) {//zscores have to do with perentiles. Basically, if the stock is in the 75th percentile, it will be set to sell.
					buyorsell="Buy";
				}
				else if(zScores[1]>.674) {//If the stock is in the 25th percentile for the month, it will set to buy.
					buyorsell="Sell";
				}
			//}
		}catch(SQLException e) {
			e.printStackTrace();
		}finally {
	        if (stmt != null) { try {
				stmt.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} }
	    }
		return buyorsell;
	}
	public static void scanMarket(Connection myConnection){//Same stuff as scan inventory but using stocks table to find the stocks on the market.
		String inventory[]= new String[100];
		java.sql.Statement stmt=null;
		try {
			stmt=myConnection.createStatement();
			stmt.executeUpdate("CREATE TEMPORARY TABLE babyanthony.tempinventory( select ROW_NUMBER() OVER( ORDER BY SYMBOL) AS rownum, symbol, shares FROM inventory);");
			stmt.executeUpdate("CREATE TEMPORARY TABLE babyanthony.tempstocks( select ROW_NUMBER() OVER( ORDER BY SYMBOL) AS rownum, symbol, stockindex FROM stocks);");
			for(int i=0;i<100;i++) {//Gets symbol from the symbol table to check whether to buy or sell
				ResultSet rs= stmt.executeQuery("SELECT symbol FROM babyanthony.tempinventory WHERE rownum="+i+";");
				while(rs.next()) {
					inventory[i]= rs.getString("symbol");
				}
			}
			int Length=inventory.length;
			String symbol=null;
			String symbolcheck=null;
			int i=1;
			while(true) {//Laughs in "special"
				ResultSet rs=stmt.executeQuery("SELECT symbol FROM babyanthony.tempstocks WHERE rownum="+i+";");
				while(rs.next()) {
					symbol=rs.getString("symbol");
				}
				try {//Uses same concept that the scaninventory uses to make sure the table doesn't run out of stocks.
					System.out.println(symbolcheck+"=="+symbol);
				if(symbolcheck.contentEquals(symbol)) {
					System.out.println("Skrrrting to a halt");
					break;
				}
				}catch(NullPointerException e) {
				}
				boolean b=true;
				for(int j=1;j<Length;j++) {//Checks to see if stocks that the machine is checking has not already been bought and in inventory!
					if(symbol.equals(inventory[j])) {
						b=false;
					}
				}
				if(b==true) {
					String buyorsell= scanStock(myConnection,symbol);
					if(buyorsell=="Buy") {//All this does is buys the stock by subracting funds from balancesheet and updating the inventory
						double balance=0;
						double shareprice=0;
						ResultSet rs1= stmt.executeQuery("SELECT balance FROM balancesheet WHERE thing='balance';");
						while(rs1.next()) {
							balance=rs1.getDouble("balance");
						}
						if(balance>600) {//Puts a $600 limit on purchases so the machine doesn't bankrupt itself with Tesla
						ResultSet rs2=stmt.executeQuery("SELECT stockvalue FROM stockvalues WHERE symbol='"+symbol+"' AND hour=1;");
						while(rs2.next()) {
							shareprice=rs2.getDouble("stockvalue");
						}
						double bonds= Math.floor(600/shareprice);
						if(bonds>0) {
						double newbalance=balance-(shareprice*bonds);
						stmt.executeUpdate("UPDATE balancesheet SET balance="+newbalance+" WHERE thing='balance'");
						stmt.executeUpdate("INSERT INTO inventory (symbol,shares) VALUES('"+symbol+"', "+bonds+");");
						System.out.println("Bought "+bonds+" shares of "+ symbol+"!");
						}
						}
					}
				}
				symbolcheck=symbol;
				i++;
			}
			stmt.executeUpdate("DROP TABLE babyanthony.tempstocks;");
			stmt.executeUpdate("DROP TABLE babyanthony.tempinventory;");
		}catch(SQLException e){
			e.printStackTrace();
		}finally {
	        if (stmt != null) { try {
				stmt.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} }
	    }	
	}
	public static String getEvaluation(Connection myConnection){//Code is not complete, never called. It is supposed to give an evaluation on your inventory.
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
	public static double getSum(double Data[]) {
		int Length=Data.length;
		double Sum=0;
		for(int i=0;i<Length;i++) {
			Sum=Sum+Data[i];
		}
		return Sum;
	}
	public static double getMean(double Data[]) {
		int Length=Data.length;
    	double sum=0;
    	for(int i=0;i<Length;i++) {
    		sum=sum+Data[i];
    	}
    	double mean=sum/Length;
    	return mean;
    }
	public static double getMedian(double Data[]) {
		int Length=Data.length;
    	double median=0;
        if(Length % 2==0) {//Gets Median
            median= (Data[Length/2]+Data[Length/2-1])/2;
        }
        else {
            median=Data[Math.round(Length/2)];
        }
        return median;
    }
	public static double getStandardDeviation(double Data[],double mean) {
		int Length=Data.length;
    	double SD=0;
        for(int i=0;i<Length;i++) {//Gets Standard Deviation
            SD=SD+((Data[i]-mean)*(Data[i]-mean));
        }
        SD=SD/(Length-1);
        SD=Math.sqrt(SD);
        return SD;
    }
	public static double[] getQuartileData(double Data[]) {//returns a double array where 0=Q1, 1=Q3, and 2=IQR
		int Length=Data.length;
    	double[] QuartileData= {1,2,3};
    	double Q1=0;
        double Q3=0;
        double IQR=0;
        if(Length % 2 == 0 && !((Length/2) % 2==0)){//good
        		 Q1=Data[Math.round(Length/4)];

        	     Q3=Data[Math.round(Length*3/4)];
        	     
        	     IQR=Q3-Q1;
        	}

        	else if(Length % 2==0 && (Length/2) % 2 ==0){//good
        	     Q1=((Data[Length/4]+Data[(Length/4)-1])/2);

        	     Q3=((Data[Length*3/4]+Data[(Length*3/4)-1])/2);
        	     
        	     IQR=Q3-Q1;
        	}
        	else if(!(Length % 2 == 0) && ((Length-1)/2) % 2 == 0){//good
        	     Q1=(Data[(Length-1)/4]+Data[(Length-1)/4]-1)/2;

        	     Q3=(Data[Length*3/4]+Data[Length*3/4+1])/2;
        	     
        	     IQR=Q3-Q1;
        	}
        	else if(!(Length % 2 == 0) && !((Length-1/2) % 2 == 0)){//good
        	     Q1=Data[Math.round((Length-1)/4)];

        	     Q3=Data[Math.round((Length)*3/4)];
        	     
        	     IQR=Q3-Q1;
        	}
        QuartileData[0]=Q1;
        QuartileData[1]=Q3;
        QuartileData[2]=IQR;
    	return QuartileData;
    }
	public static double[] getZscores(double Data[],double mean, double Sx) {
		int Length=Data.length;
    	double[] zscores= {1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30};
        for(int i=0;i<Length;i++) {
        	double zscore=(Math.round((Data[i]-mean)/Sx*100));
        	zscores[i]=(zscore/100);
        }
        return zscores;
    }
}