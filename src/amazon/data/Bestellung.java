package amazon.data;

import java.util.ArrayList;
import java.util.List;

import amazon.AddressLinePair;

public class Bestellung {
	
	public Bestellung() {
		datum = "";
		wert = "";
	}
	@Override
	public String toString() {
		return "Bestellung [datum=" + datum + ", wert=" + wert + "]";
	}
	public String datum;
	public String wert;
	
	public List<AddressLinePair> allAddressLines = new ArrayList<>();
	public String cardType;
	public String cardNumber;
	public List<AddressLinePair> allSummaryLines = new ArrayList<>();
	public List<AddressLinePair> allTransactionLines = new ArrayList<>();
	
}