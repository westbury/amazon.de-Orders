package amazon;

public enum Country {
	
	DE("www.amazon.de"),
	UK("www.amazon.co.uk");
	
	private String domain;
	
	Country(String domain) {
		this.domain = domain;
	}
	
	String getDomain() {
		return domain;
	}
}
