package view;

import amazon.data.Bestellung;

public class BestellungTreeNode extends AmazonTreeNode {

	final Bestellung bestellung;
	
	public BestellungTreeNode(Bestellung bestellung) {
		this.bestellung = bestellung;
	}

	@Override
	public String getDescription() {
		return bestellung.datum;
	}

	@Override
	public String getWert() {
		return bestellung.wert;
	}

	@Override
	public String getCardNumber() {
		return bestellung.cardNumber;
	}

	@Override
	public String getCardType() {
		return bestellung.cardType;
	}

}
