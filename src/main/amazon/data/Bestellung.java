package amazon.data;

import java.util.ArrayList;
import java.util.List;

import amazon.AddressLinePair;
import de.open4me.amazonorders.AmazonOrder;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Bestellung, being an Amazon order.
 *
 */
public class Bestellung implements AmazonOrder {
	
	public String datum;
	public String wert;
	
	protected  ObservableList<Artikel> artikelliste;
	public List<AddressLinePair> allAddressLines = new ArrayList<>();
	public String cardType;
	public String cardNumber;
	public List<AddressLinePair> allSummaryLines = new ArrayList<>();
	public List<AddressLinePair> allTransactionLines = new ArrayList<>();
	
	public Bestellung() {
		artikelliste = FXCollections.observableArrayList();
		datum = "";
		wert = "";
	}

	public List<Artikel> getArtikelliste() {
		return artikelliste;
	}
	
	@Override
	public String toString() {
		return "Bestellung [datum=" + datum + ", wert=" + wert + "]";
	}
}