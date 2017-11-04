package amazon.data;

import de.open4me.amazonorders.AmazonShipment;

public class Zustellung implements AmazonShipment {
	public String status;
	
	public Zustellung() {
		status = "";
	}
}
