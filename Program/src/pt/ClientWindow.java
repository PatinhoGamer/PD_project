package pt;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class ClientWindow extends Application {
	
	private Scene scene;
	private Stage stage;
	

	
	@Override
	public void start(Stage primaryStage) throws Exception {
		stage = primaryStage;
		primaryStage.setTitle("");
		Parent root = loadParent("sample.fxml");
		scene = new Scene(root, 300, 275);
		primaryStage.setScene(scene);
		primaryStage.show();
	}
	
	
	public void setWindowRoot(String fileName) throws IOException {
		scene.setRoot(loadParent(fileName));
	}
	
	private Parent loadParent(String fileName) throws IOException {
		return FXMLLoader.load(getClass().getResource(fileName));
	}
	
	public static void main(String[] args) {
		String serverAddress = "rodrigohost.ddns.net";
		if (args.length == 0) {
			System.out.println("No server address on arguments\nUsing default");
		} else {
			serverAddress = args[0];
		}
		
		ClientMain client = new ClientMain(serverAddress, ServerMain.SERVER_PORT);
		try {
			if (client.run() > 0) {
				System.out.println("gtnreiiger");
				launch(args);
			} else {
				System.out.println("CHOULD NOT ACCESS THE SERVER. TRY AGAIN LATER");
				return;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
