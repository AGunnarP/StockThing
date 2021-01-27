package babyanthony.me.stockthing;

//Defines the Stock data structure
public class Stock {

	//Data points
	//The symbol of the company
	String symbol;
	//The URL of the stock
	String url;
	//The number of shares owned
	short shares;
	//The current price of each share
	float price;
	
	public Stock(String symbol, String url, short shares, float price) {
	    this.url = url;	
	    this.symbol = symbol;
	    this.shares = shares;
	    this.price = price;
	}
	
	//Getters
	public String getSymbol() {
	    return this.symbol;
	}
	
	public short getShares() {
	    return this.shares;
	}
	
	public float getPrice() {
	    return this.price;
	}
	
	public String getUrlString() {
	    return this.url;
	}
	
	//Setters
	public void setShares(short newShares) {
	    this.shares = newShares;
	}
	
	public void setPrice(float newPrice) {
	    this.price = newPrice;
	}
	
	//Methods
		//Unused
	public void updatePrice() {
	    
	}
	
}
