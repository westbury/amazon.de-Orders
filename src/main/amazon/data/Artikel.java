package amazon.data;

import de.open4me.amazonorders.AmazonItem;

/**
 * An Artikel in the Bestellabruf.  Each Bestellabruf may have one of more
 * Artikel in it.  
 *
 */
public class Artikel implements AmazonItem {
	public String preis;
	public String name;
	public String url;
	
	public Bestellung bestellung;
	public Zustellung zustellung;
	
	public Artikel() {
		zustellung = new Zustellung();
		bestellung = new Bestellung();
		preis = "";
		name = "";
		url = "";
	}
	
	@Override
	public String toString() {
		return "Artikel [preis=" + preis + ", name=" + name + ", url=" + url + ", bestellung=" + bestellung
				+ ", zustellung=" + zustellung + "]";
	}

	@Override
	public String getDescription() {
		return name;
	}

	@Override
	public String getPrice() {
		return preis;
	}

	@Override
	public String getAsin() {
		// TODO Auto-generated method stub
		return null;
	}

}
