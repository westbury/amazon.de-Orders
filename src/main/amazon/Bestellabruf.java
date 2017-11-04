package amazon;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.ui4j.api.browser.BrowserEngine;
import com.ui4j.api.browser.BrowserFactory;
import com.ui4j.api.browser.Page;
import com.ui4j.api.dom.Element;
import com.ui4j.api.dom.Option;
import com.ui4j.api.dom.Select;

import amazon.data.Artikel;
import amazon.data.Bestellung;
import amazon.data.Zustellung;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Bestellabruf, being the class that retrieves Amazon orders.
 *
 */
public class Bestellabruf implements Runnable{

	protected  MyPageConfiguration cfg = MyPageConfiguration.instance();
	protected Country country;
	protected  String user;
	protected  String passwort;
	protected boolean isDetailRequired;
	
	protected ObservableList<Bestellung> bestellungliste;
	protected  ObservableList<Artikel> artikelliste;
	protected  SimpleStringProperty status;

	public Bestellabruf(Country country, String user, String passwort, boolean isDetailRequired, SimpleStringProperty status) {
		bestellungliste = FXCollections.observableArrayList();
		artikelliste = FXCollections.observableArrayList();
		this.country = country;
		this.user = user;
		this.passwort = passwort;
		this.isDetailRequired = isDetailRequired;
		this.status = status;
	}

	public ObservableList<Bestellung> getBestellungliste() {
		return bestellungliste;
	}

	public ObservableList<Artikel> getArtikelliste() {
		return artikelliste;
	}

	@Override
	public void run() {
		setStatus("Bitte warten...Web-Komponente wird gestartet");
		webKit = BrowserFactory.getWebKit();

		Page login;
		login = login(webKit);
		analyse(login);
		setStatus("Fertig");
		//writeToOutput(getListe());
	}


	protected  void writeToOutput(List<Artikel> list) {
		try {
			PrintWriter output = new PrintWriter(new File("/tmp/output.csv"), "UTF-8");
			output.println("Bestlldatum|Bestellwert|Status|Artikel|Artikelpreis|Artikelurl");
			for (Artikel x:list) {
				output.println(x.bestellung.datum + "|" + x.bestellung.wert + "|" + x.zustellung.status + "|"  + x.name + "|" + x.preis + "|" + x.url);
			}
			output.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	protected  void extract2(Page login) {
		System.out.println(login.getDocument().getBody());
		try {
			List<Element> order = login.getDocument().queryAll("div[class=\"a-box-group a-spacing-base order\"]");
			System.out.println("Found " + order.size() + " Orders on this page.");
			for (Element x : order) {
				analyseOrder(login, x);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected  void analyseOrder(Page login, Element order) {
		System.out.println("===========================");
		System.out.println("Found " + order.getChildren().size() + " Children.");
		Bestellung bestellung = extractOrderHeader(order.getChildren().get(0));
		System.out.println(bestellung);
		for (int i = 1; i < order.getChildren().size(); i++) {
			System.out.println("-------------------------------------------------------------------------------------------------------------------" + "Part" + i);
			analysePart(login, bestellung, order.getChildren().get(i));
		}
		
		// Now go into the detail
		
		if (isDetailRequired) {
			List<Element> selectEleOpt = order.queryAll("a[class=\"a-link-normal\"]");
			for (Element e : selectEleOpt) {
				String description = e.getText().get().trim();
				if (description.equals("Bestelldetails") || description.equals("Order Details")) {
					Optional<String> link = e.getAttribute("href");

					Page detailsPage = webKit.navigate("https://" + country.getDomain() + link.get(), cfg);
					cfg.waitFinish();
					extractDetailsPage(bestellung, detailsPage);
					break;
				}
			}
		}
		
		/*
		 * Add to the list of all Bestellung. A Bestellung is the top level object.
		 * Artikel are just child objects of a Bestellung object.
		 */		
		bestellungliste.add(bestellung);
	}

	private void extractDetailsPage(Bestellung rechnung, Page detailsPage) {
		Element detailsBody = detailsPage.getDocument().getBody();
		System.out.println(detailsBody.getOuterHTML());

		// Get the delivery address lines
		Optional<Element> divShipping = detailsBody.query("div[class=\"a-section a-spacing-none od-shipping-address-container\"]");
		if (divShipping.isPresent()) {
			List<Element> addressLines = divShipping.get().queryAll("li");
			for (Element addressLine : addressLines) {
				List<String> allClasses = addressLine.getClasses();
				for (String eachClass : allClasses) {
					if (eachClass.startsWith("displayAddress") && !eachClass.equals("displayAddressLI")) {
						String addressLineDescription = eachClass.substring("displayAddress".length());
						String addressLineText = addressLine.getText().get();
						rechnung.allAddressLines.add(new AddressLinePair(addressLineDescription, addressLineText));
					}
				}
			}
		}
		
		// Get all the h5 elements, first address, second payment, third summary
		List<Element> headerFives = detailsBody.queryAll("h5");
		
		// Get the payment method
		Element paymentDiv = headerFives.get(1).getParent().get();

		Optional<Element> cardTypeElement = paymentDiv.query("img");
		rechnung.cardType = cardTypeElement.get().getAttribute("alt").get();

		Optional<Element> cardNumberElement = paymentDiv.query("span");
		rechnung.cardNumber = cardNumberElement.get().getText().get().trim();

		// Get the order summary
		Element summaryDiv = headerFives.get(2).getParent().get();
		List<Element> summaryRows = summaryDiv.queryAll("div[class=\"a-row\"]");
		for (Element summaryRow : summaryRows) {
			List<Element> spans = summaryRow.queryAll("span");
			String summaryDescription = spans.get(0).getText().get().trim();
			String summaryAmount = spans.get(1).getText().get().trim();

			if (summaryDescription.endsWith(":")) {
				summaryDescription = summaryDescription.substring(0, summaryDescription.length() - 1);
			}
			rechnung.allSummaryLines.add(new AddressLinePair(summaryDescription, summaryAmount));
		}

		// Get the transactions in the expander area (shown only when expanded)
		// This gives the actual charge amounts which is important when the order was split into
		// multiple shipments charged separately.
		
		Optional<Element> transactionsContent = detailsBody.query("div[class=\"a-expander-content a-expander-inline-content a-expander-inner aok-hidden\"]");
		if (transactionsContent.isPresent()) {
			List<Element> transactionRows = transactionsContent.get().queryAll("div[class=\"a-row\"]");
			for (Element transactionRow : transactionRows) {
				List<Element> spans = transactionRow.queryAll("span");
				String tranactionInfo = spans.get(1).getText().get().trim();
				String transactionAmount = spans.get(1).query("nobr").get().getText().get();

				rechnung.allTransactionLines.add(new AddressLinePair(tranactionInfo, transactionAmount));
			}
		}
	}

	protected  Artikel handleArticel(Element articel) {
		String preis = get(articel, "span[class=\"a-size-small a-color-price\"]", "not found", true, 0, "text");
		String text = get(articel, "a[class=\"a-link-normal\"]", "not found", true, 1, "text");
		if ("not found".equals(text)) {
			Optional<Element> subelement = articel.query("div[class=\"a-fixed-left-grid-col a-col-right\"]");
			if (subelement.isPresent()) {
				print(articel, 0);
				text = get(articel, "div[class=\"a-row\"]", "not found", true, 0, "text");
			}
		}
			
		String url = get(articel, "a[class=\"a-link-normal\"]", "not found", true, 1, "href");
		if (!"not found".equals(url)) {
			url = country.getDomain() + url;
		}
		Artikel artikel = new Artikel();
		artikel.preis = preis;
		artikel.name = text;
		artikel.url = url;
//		if ("not found".equals(preis) || "not found".equals("text")) {
//			print(articel,5);
//		}
		return artikel;

	}

	protected  void analysePart(Page login, Bestellung bestellung, Element element) {
		Zustellung zustellung;
		for (Element c : element.getChildren()) {
			zustellung = new Zustellung();
			for (Element cc : unroll(c)) {
				switch (cc.getAttribute("class").get()) {
				case "a-row shipment-top-row":
					zustellung = handleZustellung(cc);
					break;
				case "a-fixed-left-grid-inner": 
					System.out.println(cc.getTagName() + " | " + cc.getAttribute("class") + " | " + trim(cc.getText().get()));
					//					print(cc, 0);
					Artikel artikel = handleArticel(cc);
					artikel.bestellung = bestellung;
					artikel.zustellung = zustellung;
					getArtikelliste().add(artikel);
					bestellung.getArtikelliste().add(artikel);
					break;
				default:
					System.out.println(cc.getTagName() + " | " + cc.getAttribute("class") + " | " + trim(cc.getText().get()));
					print(cc, 0);
				}
			}
		}
	}

	protected  Zustellung handleZustellung(Element cc) {
		String status = get(cc, "span[class=\"a-size-medium a-text-bold\"]", "", true, 0, "text");
		Zustellung zustellung = new Zustellung();
		zustellung.status = status;
		return zustellung;
	}

	protected  List<String> unrollDivList = Arrays.asList(new String[] {"a-box-inner", "a-fixed-right-grid-inner", "a-fixed-right-grid-inner", "a-row"});
	protected  List<String> ignoreDivList = Arrays.asList(new String[] {"a-fixed-right-grid-col a-col-right"});
	private BrowserEngine webKit;

	protected  List<Element> unroll(Element c) {
		ArrayList<Element> l = new ArrayList<Element>();
		if (c.getTagName().equals("div")) {
			// Ignore elements empty class attributes
			if (!c.getAttribute("class").isPresent()) {
				return l;
			}
			String attr = c.getAttribute("class").get();
			if (ignoreDivList.contains(attr)) {
				return l;
			}
			if (unrollDivList.contains(attr)) {
				for (Element cc : c.getChildren()) {
					l.addAll(unroll(cc));
				}
				return l;
			}
			// Remove nested divs 
			if (c.getChildren().size() == 1) {
				return unroll(c.getChildren().get(0));
			}
		}
		l.add(c);
		return l;
	}

	protected  void print(Element element, int einruecken) {
		String indent = "";
		for (int i = 0; i < einruecken; i++ ) {
			indent += "  ";
		}
		for (Element c : element.getChildren()) {
			Optional<String> x = c.getText();
			if (x != null && x.isPresent()) {
				String trimed = trim(x.get());
				if (!trimed.isEmpty()) {
					System.out.println(indent + trimed + " [" +  c.getTagName()  +"|" + c.getAttribute("class") + "]");
				}
			}
			for (Element cc : c.getChildren()) {
				print(cc, einruecken + 1);
			}
		}	
	}

	protected  Bestellung extractOrderHeader(Element order) {
		Bestellung rechnung = new Bestellung();
		String last = "";
		for (Element y : order.queryAll("span")) {
			String newString = y.getText().get().replace("\r"," ").replace("\n", "").trim();
			if (last.equals("Bestellung aufgegeben") || last.equals("Order placed")) {
				rechnung.datum = newString;
			}
			if (last.equals("Summe") || last.equals("Total")) {
				rechnung.wert = newString;
			}
			last = newString;
		}
		return rechnung;
	}

	protected  String get(Element y, String xpath, String errorMessage, boolean trim, int idx, String attr) {
		if (y == null) {
			System.err.println("Y is null");
			return errorMessage;
		}
		Element element;
		if (idx == 0) {
			Optional<Element> optelement = y.query(xpath);
			if (optelement == null) {
				System.err.println("Elemnet with xpath '" + xpath + "' is null! Xpath wrong!?");
				return errorMessage;
			}
			if (!optelement.isPresent()) {
				System.err.println("Elemnet with xpath '" + xpath + "' not found!");
				return errorMessage;
			}
			element = optelement.get();
		} else {
			List<Element> elements = y.queryAll(xpath);
			if (idx >= elements.size()) {
				System.err.println(idx + ". Element with xpath '" + xpath + "' not found!");
				return errorMessage;
			}
			element = elements.get(idx);
		}
		Optional<String> text;
		if (attr.equals("text")) {
			text = element.getText();
		} else {
			text = element.getAttribute(attr);
		}
		if (text == null) {
			System.err.println("No Text (" + attr + ") found"); 
			return errorMessage;
		}
		String out = text.get();
		if (trim) {
			out = trim(out);
		}
		return out;
	}


	public String trim(String out) {
		return out.replace("\t", "").replace("\n", "").replace("\r", "").trim();
	}
	protected  void analyse(Page login) {
		System.out.println(login.getDocument().getBody().getOuterHTML());
		System.out.println(login.getDocument().getBody().getInnerHTML());
		Element selectEle = login.getDocument().query("select[name=\"orderFilter\"]").get();
		Select select = new Select(selectEle);
		for (int i = 0; i < select.getOptions().size(); i++) {
			// set selectEle and Selct new because we might be on an new page
			Optional<Element> selectEleOpt = login.getDocument().query("select[name=\"orderFilter\"]");
			if (selectEleOpt.isPresent()) {
				selectEle = selectEleOpt.get();
				select = new Select(selectEle);
				Option opt = select.getOption(i);
				if (opt.getElement().getAttribute("value").get().startsWith("year-")) {
					System.out.println("Setting to " + opt.getElement().getAttribute("value"));
					setStatus("Jahr: " + opt.getElement().getText().get());
					select.clearSelection();
					select.setSelectedIndex(i);
					select.change();
					cfg.waitFinish();
					int page = 1;
					while (true) {
						setStatus("Jahr: " + opt.getElement().getText().get() + " Seite "+ page);
						System.out.println(opt.getElement().getText() + "/" + page);
						extract2(login);
						page++;
						System.out.println(login.getDocument().getBody().getInnerHTML());
						Optional<Element> next = login.getDocument().query("li[class=\"a-last\"]");
						if (next.isPresent()) {
							Optional<Element> ahref = next.get().query("a");
							if (ahref.isPresent()) {
								System.out.println("Klick auf Next");
								ahref.get().click();
								cfg.waitFinish();
								continue;
							} else {
								break;
							}
						} else {
							break;
						}
					}
				}
			} else {
				// orderFilter not present
			}
		}
	}

	protected  Page login(BrowserEngine webKit) {
		setStatus("Loginversuch...");
		String url = String.format("https://%s/gp/css/order-history/ref=nav__gno_yam_yrdrs", country.getDomain());
		Page login = webKit.navigate(url, cfg);
		cfg.waitFinish();
		login.show();
		login.getDocument().query("input[id=\"ap_email\"]").get().setValue(user);
		login.getDocument().query("input[id=\"ap_password\"]").get().setValue(passwort);
		login.getDocument().query("input[id=\"signInSubmit\"]").get().click();
		cfg.waitFinish();
		if (login.getDocument().query("input[id=\"ap_email\"]").isPresent()) {
			setStatus("Login wegen Captcha fehlgeschlagen. Bitte Captcha lösen und auf Anmelden klicken.");
			login.getDocument().query("input[id=\"ap_email\"]").get().setValue(user);
			login.getDocument().query("input[id=\"ap_password\"]").get().setValue(passwort);
			cfg.waitFinish();
		}
		return login;
	}

	protected  void setStatus(String string) {
		Platform.runLater(() -> status.set(string));
	}
	
}
