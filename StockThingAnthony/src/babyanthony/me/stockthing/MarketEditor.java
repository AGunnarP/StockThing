package babyanthony.me.stockthing;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public class MarketEditor {
	
	//Database instance, created during main class execution
	private static final Database database = DartThrower.getDatabaseInstance();
	private static ArrayList<Stock> stocks;
	
	//Prevent instantiation
	private MarketEditor() {}
	
	public static void loadAllStocks() throws SQLException {
	    
	    final Statement stmt = database.createStatement();
	    
	    stocks = new ArrayList<Stock>();
	    
	    database.executeState(stmt, 
		    "CREATE TEMPORARY TABLE babyanthony.tempinventory( select ROW_NUMBER() OVER( ORDER BY SYMBOL) "
			+ "AS rownum, symbol, shares FROM inventory);");
	    
	    boolean hasNextStock = true;
	    short count = 1;
	    
	    while(hasNextStock) {
		
		ResultSet results = database.getResultSet(stmt, 
			"SELECT symbol,shares FROM babyanthony.tempinventory WHERE rownum=" + count + ";");
		
		if(results.wasNull()) {
		    hasNextStock = false;
		    break;
		}
		
		do {
		    
		    String symbol = results.getString("symbol");
		    String url = "https://finance.yahoo.com/quote/" + symbol + "/history?p=" + symbol;
		    short shares = results.getShort("shares");
		    float price = results.getFloat("stockvalue");
		    
		    Stock stock = new Stock(symbol, url, shares, price);
		    stocks.add(stock);
		    
		}while(results.next());

		count++;
	    }
	    	
	    database.closeStatement(stmt);
	}
	
	public static void scanDatabaseInventory() {
	    
	}
	
	public static void scanMarketActivity() {
	    
	}
	
	public static void updateDatabaseValues() {
	
	    final Statement stmt = database.createStatement();
	    
	    try {
	    	loadAllStocks();
	    }catch(SQLException e) {
		e.printStackTrace();
	    }
	    
	    for(Stock stock : stocks) {
		stock.updatePrice();
	    }
	    
	    
	    
	    database.closeStatement(stmt);
	}
	
}
