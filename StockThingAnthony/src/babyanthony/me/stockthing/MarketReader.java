package babyanthony.me.stockthing;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MarketReader {
	
	private URL url;
	private URLConnection urlConnection;
	
	private BufferedReader streamReader;
	
	//Creates this reader object for a specific url
	public MarketReader(String url) {
	    this.connectToUrl(url);
	    this.createReader();
	}
	
	//Getters
	public URLConnection getUrlConnection() {
	    return this.urlConnection;
	}
	
	//Initialization methods
	private void connectToUrl(String urlString) {
	    try {
		this.url = new URL(urlString);
		urlConnection = url.openConnection();
	    }catch(IOException e) {
		e.printStackTrace();
	    }
	}
	
	private void createReader() {
	    try {
		InputStreamReader inputReader = new InputStreamReader(urlConnection.getInputStream());
		this.streamReader = new BufferedReader(inputReader);
	    }catch(IOException e) {
		e.printStackTrace();
	    }
	}
	
	//Class Methods
	public String[] getLines() {
	    
	    String[] lines = new String[100];
	    
	    try {
		String currentLine = streamReader.readLine();
		int count = 0;
		
		while(currentLine != null) {
		    lines[count] = currentLine;
		    currentLine = streamReader.readLine();
		    count++;
		}
	    }catch(IOException e) {
		e.printStackTrace();
	    }
	    return lines;
	}
	
	public String[] matchAdjCloseData() {
	    
	    String[] data = new String[30];
	    String[] currLines = this.getLines();
	    
	    Pattern pattern = Pattern.compile("\"adjclose\":(\\w|\\.)+");
	    Matcher matcher = null;
	    
	    int count = 0;
	    
	    //First loop filters out adjclose value and assigns it to the data[] array
	    for(int x = 0; x < currLines.length; x++) {
		
		matcher = pattern.matcher(currLines[x]);
		
		while(matcher.find()) {
		    if(count == 30) {
			break;
		    }else {
			data[count] = matcher.group(0);
			count++;
		    }
		}
		
	    }
	    
	    return data;
	}
	
	public String[] filterMatchedData() {
	    
	    String[] newData = new String[30];
	    String[] oldData = this.matchAdjCloseData();
	    
	    Pattern pattern = Pattern.compile("(\\d|\\.)+");
	    Matcher matcher = null;
	    
	    //Second loop filters AdjClose Data to specific integers
	    for(int x = 0; x < oldData.length; x++) {
		
		matcher = pattern.matcher(oldData[x]);
		
		while(matcher.find()) {
		    newData[x] = matcher.group(0);
		}
		
	    }
	    
	    return newData;
	}

}
