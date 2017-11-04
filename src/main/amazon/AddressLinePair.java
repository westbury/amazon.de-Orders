package amazon;

public class AddressLinePair {

	public final String addressLineDescription;
	
	public final String addressLineText;
	
	public AddressLinePair(String addressLineDescription, String addressLineText) {
		this.addressLineDescription = addressLineDescription;
		this.addressLineText = addressLineText;
	}

	public String toString() {
		return addressLineDescription + ": " + addressLineText;
	}
}
