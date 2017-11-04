package view;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

import amazon.Bestellabruf;
import amazon.Country;
import amazon.data.Artikel;
import amazon.data.Bestellung;
import evntHandler.KeyEventHandlerTableview;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.util.Callback;

public class MainDialogController {
	private Bestellabruf task = null;
	
    @FXML
    private ComboBox<String> country;
    
    @FXML
    private TextField user;
    
    @FXML
    private PasswordField pwd;
    
    @FXML
    private CheckBox details;
    
    @FXML
    private TableView<Artikel> tableview;
    
    @FXML
    private TreeTableView<Bestellung> shipments;
    
    @FXML
    private TreeTableView<AmazonTreeNode> orders;
    
    @FXML
    private Label statuslabel;
    
    private SimpleStringProperty status = new SimpleStringProperty("");

	private Properties properties;
    
    @FXML
    private void initialize() {
    	File propertiesFile = new File("amazon.properties");
    	properties = new Properties();
    	try (InputStream inputStream = new FileInputStream(propertiesFile)) {
			properties.load(inputStream);
		} catch (FileNotFoundException e) {
			// This is a normal situation when starting with no
			// properties file.
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

    	if (properties.containsKey("country")) {
    		country.setValue((String)properties.get("country"));
    	}
    	if (properties.containsKey("username")) {
        	user.setText((String)properties.get("username"));
    	}

    }
    
    @FXML
    private void runPressed(ActionEvent event) {

		properties.put("country", country.getValue());
    	properties.put("username", user.getText());

    	File propertiesFile = new File("amazon.properties");
    	try (OutputStream outputStream = new FileOutputStream(propertiesFile)) {
			properties.store(outputStream, "Properties for the amazon.de-Orders example application");
		} catch (IOException e) {
			// Don't worry if we can't save.
		}

    	
    	//    	if (task != null) {
//    		return;
//    	}
    	
    	// Temp
//    	task = new Bestellabruf(null, null, null, false, status);
    	
    	String countryDomain = country.getSelectionModel().getSelectedItem();
    	Country country = null;
    	switch (countryDomain) {
    	case "amazon.de":
    		country = Country.DE;
    		break;
    	case "amazon.co.uk":
    		country = Country.UK;
    		break;
    	}
    	
		task = new Bestellabruf(country, user.getText(), pwd.getText(), details.isSelected(), status);
    	tableview.setItems(task.getArtikelliste());
    	
    	TreeItem<AmazonTreeNode> root = new TreeItem<AmazonTreeNode>(new BestellabrufTreeNode(task));
    	root.setExpanded(true);
    	 
    	 
//     	Bestellung bestellung1 = new Bestellung();
//     	bestellung1.datum = "21 December 2016";
//     	bestellung1.wert = "£9.98";
// 		task.getBestellungliste().add(bestellung1);
//     	
//     	Bestellung bestellung2 = new Bestellung();
//     	bestellung2.datum = "22 December 2016";
//     	bestellung2.wert = "£92.98";
//     	bestellung2.cardNumber = "**** 3872";
//     	bestellung2.cardType = "Eurocard";
// 		task.getBestellungliste().add(bestellung2);
//     	
// 		Artikel artikel2 = new Artikel();
// 		artikel2.name = "Raspberry Pi";
// 		artikel2.preis = "£30,00";
//    	artikel2.bestellung = bestellung2;
//    	bestellung2.getArtikelliste().add(artikel2);

    	for (Bestellung bestellung : task.getBestellungliste()) {
    		TreeItem<AmazonTreeNode> bestellungTreeItem = new TreeItem<AmazonTreeNode>(new BestellungTreeNode(bestellung));

        	for (Artikel artikel : bestellung.getArtikelliste()) {
        		bestellungTreeItem.getChildren().add(
        				new TreeItem<AmazonTreeNode>(new ArtikelTreeNode(artikel)));
        	}

			root.getChildren().add(bestellungTreeItem);
    	}

    	task.getBestellungliste().addListener(new ListChangeListener<Bestellung>() {

    		@Override
    		public void onChanged(Change<? extends Bestellung> c) {
    			while (c.next()) {
    		        if (c.wasAdded()) {
    	    			for (Bestellung bestellung : c.getAddedSubList()) {
    	    				TreeItem<AmazonTreeNode> bestellungTreeItem2 = new TreeItem<AmazonTreeNode>(new BestellungTreeNode(bestellung));

    	    	        	for (Artikel artikel : bestellung.getArtikelliste()) {
    	    	        		bestellungTreeItem2.getChildren().add(
    	    	        				new TreeItem<AmazonTreeNode>(new ArtikelTreeNode(artikel)));
    	    	        	}

    						root.getChildren().add(bestellungTreeItem2);
    	    			}
    		        }
    		    }
    		}
    	});

    	orders.setRoot(root);
    	orders.setShowRoot(false);
    	
    	 (new Thread(task)).start();
    }
    
    public void set() {
    	statuslabel.textProperty().bind(status);
    	
    	tableview.getColumns().clear();
    	tableview.setOnKeyPressed(new KeyEventHandlerTableview());
    	tableview.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    	
    	TableColumn<Artikel, String> b1 = new TableColumn<>("Bestelldatum");
    	b1.setCellValueFactory(new Callback<CellDataFeatures<Artikel,String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<Artikel, String> param) {
    	        return new ReadOnlyStringWrapper(param.getValue().bestellung.datum);
    	    }
    	});
    	b1.setPrefWidth(100);

    	TableColumn<Artikel, String> b2 = new TableColumn<>("Bestellwert");
    	b2.setCellValueFactory(new Callback<CellDataFeatures<Artikel,String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<Artikel, String> param) {
    	        return new ReadOnlyStringWrapper(param.getValue().bestellung.wert);
    	    }
    	});
    	b2.setPrefWidth(100);
    	
    	TableColumn<Artikel, String> z1 = new TableColumn<>("Status");
    	z1.setCellValueFactory(new Callback<CellDataFeatures<Artikel,String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<Artikel, String> param) {
    	        return new ReadOnlyStringWrapper(param.getValue().zustellung.status);
    	    }
    	});
    	z1.setPrefWidth(100);

    	
    	TableColumn<Artikel, String> a1 = new TableColumn<>("Name");
    	a1.setCellValueFactory(new Callback<CellDataFeatures<Artikel,String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<Artikel, String> param) {
    	        return new ReadOnlyStringWrapper(param.getValue().name);
    	    }
    	});
    	a1.setPrefWidth(200);
    	
    	TableColumn<Artikel, String> a2 = new TableColumn<>("Preis");
    	a2.setCellValueFactory(new Callback<CellDataFeatures<Artikel,String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<Artikel, String> param) {
    	        return new ReadOnlyStringWrapper(param.getValue().preis);
    	    }
    	});
    	a1.setPrefWidth(100);
    	
    	TableColumn<Artikel, String> a3 = new TableColumn<>("Url");
    	a3.setCellValueFactory(new Callback<CellDataFeatures<Artikel,String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<Artikel, String> param) {
    	        return new ReadOnlyStringWrapper(param.getValue().url);
    	    }
    	});
    	a3.setPrefWidth(100);
    	tableview.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);    	
    	tableview.getColumns().addAll(b1,b2, z1, a1, a2, a3);
    	
    	ordersTable();
    }

	private void ordersTable() {
    	orders.getColumns().clear();
    	orders.setOnKeyPressed(new KeyEventHandlerTableview());
    	orders.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    	
    	TreeTableColumn<AmazonTreeNode, String> orderDateColumn = new TreeTableColumn<>("Bestelldatum/Artikel Name");
    	orderDateColumn.setCellValueFactory(new Callback<TreeTableColumn.CellDataFeatures<AmazonTreeNode,String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(TreeTableColumn.CellDataFeatures<AmazonTreeNode, String> param) {
    	        AmazonTreeNode value = param.getValue().getValue();
				return new ReadOnlyStringWrapper(value.getDescription());
    	    }
    	});
    	orderDateColumn.setPrefWidth(100);

    	TreeTableColumn<AmazonTreeNode, String> orderTotalColumn = new TreeTableColumn<>("Bestellwert"); // Order Total
    	orderTotalColumn.setCellValueFactory(new Callback<TreeTableColumn.CellDataFeatures<AmazonTreeNode, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(TreeTableColumn.CellDataFeatures<AmazonTreeNode, String> param) {
    	        AmazonTreeNode value = param.getValue().getValue();
    	        return new ReadOnlyStringWrapper(value.getWert());
    	    }
    	});
    	orderTotalColumn.setPrefWidth(100);
    	
    	TreeTableColumn<AmazonTreeNode, String> deliveryDateColumn = new TreeTableColumn<>("Delivery Date");
    	deliveryDateColumn.setCellValueFactory(new Callback<TreeTableColumn.CellDataFeatures<AmazonTreeNode, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(TreeTableColumn.CellDataFeatures<AmazonTreeNode, String> param) {
    	        AmazonTreeNode value = param.getValue().getValue();
    	        return new ReadOnlyStringWrapper(value.getDeliveryDate());
    	    }
    	});
    	deliveryDateColumn.setPrefWidth(200);
    	
    	TreeTableColumn<AmazonTreeNode, String> cardNumberColumn = new TreeTableColumn<>("Card Number");
    	cardNumberColumn.setCellValueFactory(new Callback<TreeTableColumn.CellDataFeatures<AmazonTreeNode, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(TreeTableColumn.CellDataFeatures<AmazonTreeNode, String> param) {
    	        AmazonTreeNode value = param.getValue().getValue();
    	        return new ReadOnlyStringWrapper(value.getCardNumber());
    	    }
    	});
    	deliveryDateColumn.setPrefWidth(100);
    	
    	TreeTableColumn<AmazonTreeNode, String> cardTypeColumn = new TreeTableColumn<>("Card Type");
    	cardTypeColumn.setCellValueFactory(new Callback<TreeTableColumn.CellDataFeatures<AmazonTreeNode, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(TreeTableColumn.CellDataFeatures<AmazonTreeNode, String> param) {
    	        AmazonTreeNode value = param.getValue().getValue();
    	        return new ReadOnlyStringWrapper(value.getCardType());
    	    }
    	});
    	cardTypeColumn.setPrefWidth(100);
    	orders.setColumnResizePolicy(TreeTableView.CONSTRAINED_RESIZE_POLICY);    	
    	orders.getColumns().addAll(orderDateColumn ,orderTotalColumn, deliveryDateColumn, cardNumberColumn, cardTypeColumn);
	}
    

}
