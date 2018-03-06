package view;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import sun.dc.pr.PRError;

import java.lang.ref.PhantomReference;
import java.util.Timer;

public class Main extends Application {
    public static Stage mainStage;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/playerWindow.fxml"));
        primaryStage.setTitle("GoGame");
        primaryStage.setScene(new Scene(root));
        primaryStage.setResizable(false);
        primaryStage.setOnCloseRequest(we -> System.exit(0));
        mainStage = primaryStage;
    }
}
