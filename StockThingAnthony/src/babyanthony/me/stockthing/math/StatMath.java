package babyanthony.me.stockthing.math;

public class StatMath {

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
