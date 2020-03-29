import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DartThrower {
	public static void main(String args[]) {
		Connection myConnection=null;
		String Database = "jdbc:mysql://localhost:3306/babyanthony?useLegacyDatetimeCode=false&serverTimezone=America/Los_Angeles";
		String user="root";
		String password="Password13";
		try {
		//	Class.forName("com.mysql.jdbc.Driver");
			Class.forName("com.mysql.cj.jdbc.Driver").newInstance();
			myConnection= DriverManager.getConnection(Database,user,password);
			System.out.println("Connection Established!");
		} catch (SQLException | InstantiationException | IllegalAccessException | ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		java.sql.Statement stmt = null;
		try {
			stmt = myConnection.createStatement();
			/*	for(int j=0;j<169;j++) {
					stmt.executeUpdate("INSERT INTO stockvalues (hour,symbol,stockvalue) VALUES ("+j+",'nvda',"+j*(Math.round(Math.random()*11)+1)+");");
				}*/
                while(true){
				DartThrower.scanInventory(myConnection);
				DartThrower.scanMarket(myConnection);
				DartThrower.getPERatio("NVDA", myConnection);
                Thread.sleep(3600);//bot sleeps for an hour
                
                }
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
	
	public static void updateStockValues(String symbol, Connection myConnection) {
		java.sql.Statement stmt = null;
		try {
			stmt=myConnection.createStatement();
			URL url=new URL("https://finance.yahoo.com/quote/"+symbol+"/history?p="+symbol);
			String lines[]=new String[100];
			URLConnection urlConn= url.openConnection();
			InputStreamReader InputReader= new InputStreamReader(urlConn.getInputStream());
			BufferedReader buff= new BufferedReader(InputReader);
			
			String regularex="\"adjclose\":(\\w|\\.)+";
			Pattern pattern= Pattern.compile(regularex);
			
			
			String line=buff.readLine();
			int i=0;
			while( line !=null) {
				lines[i]=line;
				line=buff.readLine();
				i++;
			}
			//String line56= lines[56];
			String Data[]= new String[30];
			Matcher matcher=null;
			int c=0;
			for(int j=0;j<lines.length;j++) {
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
			for(int l=0;l<Data.length;l++) {
				
				matcher2=pattern2.matcher(Data[l]);
				while(matcher2.find()==true) {
					Data[l]=matcher2.group(0);
				}
			}
			stmt.executeUpdate("DELETE FROM stockvalues WHERE symbol='"+symbol+"';");
			for(int p=0;p<Data.length;p++) {
				stmt.executeUpdate("INSERT INTO stockvalues (hour,symbol,stockvalue) VALUES ("+(p+1)+",'"+symbol+"',"+Double.parseDouble(Data[p])+");");
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
			String symbolcheck=null;
			int i=1;
			while(true) {
				ResultSet rs=stmt.executeQuery("SELECT symbol,shares FROM inventory");
				while(rs.next()) {
					symbol= rs.getString("symbol");
					shares= rs.getInt("shares");
				}
				//System.out.println(symbolcheck+".equals "+(symbol));
				try {
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
				ResultSet rs2=stmt.executeQuery("Select stockvalue FROM stockvalues WHERE symbol='"+symbol+"' AND hour=1;");
				while(rs2.next()) {
					stockvalue=rs2.getDouble("stockvalue");
				}
				String Decision= scanStock(myConnection,symbol);
				if(Decision=="Sell") {
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
				symbolcheck=symbol;
				}
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
		DartThrower.updateStockValues(symbol, myConnection);
		String buyorsell=null;
		double Data[]= {1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30};
		java.sql.Statement stmt=null;
		try {
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
				double PERatio=getPERatio(symbol,myConnection);
				
				if(zScores[1]<(-.674) && PERatio<16.5) {
					buyorsell="Buy";
				}
				else if(zScores[1]>.674 && PERatio>16.5) {
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
	public static void scanMarket(Connection myConnection){
		String inventory[]= new String[100];
		java.sql.Statement stmt=null;
		try {
			stmt=myConnection.createStatement();
			for(int i=0;i<100;i++) {
				ResultSet rs= stmt.executeQuery("SELECT symbol FROM babyanthony.inventory;");
				while(rs.next()) {
					inventory[i]= rs.getString("symbol");
				}
			}
			int Length=inventory.length;
			String symbol=null;
			String symbolcheck=null;
			int i=1;
			while(true) {
				ResultSet rs=stmt.executeQuery("SELECT symbol FROM babyanthony.stocks WHERE StockID="+i+";");
				while(rs.next()) {
					symbol=rs.getString("symbol");
				}
				try {
					System.out.println(symbolcheck+"=="+symbol);
				if(symbolcheck.contentEquals(symbol)) {
					System.out.println("Skrrrting to a halt");
					break;
				}
				}catch(NullPointerException e) {
				}
				boolean b=true;
				for(int j=1;j<Length;j++) {
					if(symbol.equals(inventory[j])) {
						b=false;
					}
				}
				if(b==true) {
					String buyorsell= scanStock(myConnection,symbol);
					if(buyorsell=="Buy") {
						double balance=0;
						double shareprice=0;
						ResultSet rs1= stmt.executeQuery("SELECT balance FROM balancesheet WHERE thing='balance';");
						while(rs1.next()) {
							balance=rs1.getDouble("balance");
						}
						if(balance>600) {
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
	
	public static double getPERatio(String symbol,Connection myConnection) {
		double PERatio=0;
		try {
		URL url=new URL("https://finance.yahoo.com/quote/"+symbol+"/");
		URLConnection urlConn= url.openConnection();
		InputStreamReader InputReader= new InputStreamReader(urlConn.getInputStream());
		BufferedReader buff= new BufferedReader(InputReader);
		
		String lines[]=new String[100];
		String line=buff.readLine();
		int i=0;
		while( line !=null) {
			lines[i]=line;
			line=buff.readLine();
			//System.out.println(line);
			i++;
		}
		
		String PERatio2="";
		String regularex="\"trailingPE\":.\"raw\":\\d+.\\d+";
		Pattern pattern=Pattern.compile(regularex);
		Matcher matcher=null;
		
		for(int i1=0; i1<lines.length;i1++) {
			try {
			matcher=pattern.matcher(lines[i1]);
			}catch(NullPointerException e) {
				return PERatio;
			}
			if(matcher.find()==true) {
				PERatio2=matcher.group(0);
				break;
			}
		}
		
		String regularex2="\\d+.\\d+";
		Pattern pattern2=Pattern.compile(regularex2);
		Matcher matcher2=pattern2.matcher(PERatio2);
		
		if(matcher2.find()==true) {
			PERatio2=matcher2.group(0);
		}
		
		PERatio=Double.parseDouble(PERatio2);
		//System.out.println("PERatio:"+PERatio);
		
		return PERatio;
		}catch(IOException e) {
			e.printStackTrace();
		}
		return PERatio;
		
	}
	
	public static double getMarketCap(String symbol,Connection myConnection){
		java.sql.Statement stmt=null;
		
		URL url;
		double marketcap=0;
		try {
			url = new URL("https://finance.yahoo.com/quote/"+symbol+"/history?p="+symbol);
		URLConnection urlConn= url.openConnection();
		InputStreamReader InputReader= new InputStreamReader(urlConn.getInputStream());
		BufferedReader buff= new BufferedReader(InputReader);
		
		String lines[]=new String[100];
		String line=buff.readLine();
		int i=0;
		while( line !=null) {
			lines[i]=line;
			line=buff.readLine();
			//System.out.println(line);
			i++;
		}
		
		String regularex="\"adjclose\":(\\w|\\.)+";
		Pattern pattern= Pattern.compile(regularex);
		
		String adjclose="";
		int i1=0;
		while(true) {
			Matcher matcher=pattern.matcher(lines[i1]);
			if(matcher.find()==true) {
				adjclose=matcher.group(0);
				break;
			}
			i1++;
		}
		
		String regularex2="(\\d|\\.)+";
		Pattern pattern2= Pattern.compile(regularex2);
		Matcher matcher2=null;
		
		matcher2=pattern2.matcher(adjclose);
		if(matcher2.find()==true) {
			adjclose=matcher2.group(0);
		}
		//System.out.println(adjclose);
		double adjustedclose= Double.parseDouble(adjclose);
		
		String Volume="";
		String regularex3="\"volume\":\\d+";
		Pattern pattern3=Pattern.compile(regularex3);
		Matcher matcher3=null;
		
		int i2=0;
		while(true) {
			 matcher3=pattern3.matcher(lines[i1]);
			if(matcher3.find()==true) {
				Volume=matcher3.group(0);
				break;
			}
			i2++;
		}

		String regularex4="\\d+";
		Pattern pattern4=Pattern.compile(regularex4);
		Matcher matcher4=pattern4.matcher(Volume);
		if(matcher4.find()==true) {
			Volume=matcher4.group(0);
		}
		double volume=Double.parseDouble(Volume);
		
		marketcap=volume*adjustedclose;
		return marketcap;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return marketcap;
	}
	
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
