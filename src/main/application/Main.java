package application;
	
import java.util.ResourceBundle;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import view.MainDialogController;


public class Main extends Application {
	@Override
	public void start(Stage primaryStage) {
		try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(Main.class.getClassLoader().getResource("view/MainDialog.fxml"));
            loader.setResources(ResourceBundle.getBundle("view.MainDialog"));
            BorderPane mainLayout = (BorderPane) loader.load();
			Scene scene = new Scene(mainLayout,700,400);
			scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
			primaryStage.setScene(scene);
			primaryStage.show();
			((MainDialogController) loader.getController()).set();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		launch(args);
	}
}
