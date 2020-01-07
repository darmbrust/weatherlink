package net.sagebits.weatherlink.gui;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class WeatherLinkLiveGUI extends Application
{
	private static Stage mainStage_;
	private WeatherLinkLiveGUIController wllc_;
	

	public static Logger logger = LogManager.getLogger(WeatherLinkLiveGUI.class);

	@Override
	public void start(Stage primaryStage) throws Exception
	{
		logger.info("Startup of Weather Link GUI");
		mainStage_ = primaryStage;
		finishSetUp();
	}
	
	public void finishSetUp()
	{
			Platform.runLater(() ->
			{
				try
				{
					FXMLLoader loader = new FXMLLoader();
					Scene scene = new Scene((Parent) loader.load(WeatherLinkLiveGUI.class.getResourceAsStream("/fxml/gui.fxml")));
					mainStage_.setScene(scene);
					mainStage_.sizeToScene();
					mainStage_.setMinHeight(210);
					mainStage_.setMinWidth(210);
					wllc_ = loader.getController();
					wllc_.finishInit(mainStage_);
					//		mainStage_.getIcons().add(Images.APPLICATION.getImage());
					mainStage_.setTitle("Weather Link Live GUI");
					mainStage_.setOnCloseRequest(event -> 
					{
						wllc_.shutdown();
						
						System.exit(0);
					});
					//		ltc_.finishInit();

					Screen screen = Screen.getPrimary();

					mainStage_.show();
				}
				catch (IOException e)
				{
					logger.error("Unexpected error starting up", e);
					System.exit(1);
				}
			});
//		else
//		{
//			showErrorDialog("Error", "Startup failed", null);
//			System.exit(1);
//		}
	}

	public static void showErrorDialog(String title, String headerText, String contentText)
	{
		
		if (Platform.isFxApplicationThread())
		{
			Alert alert = new Alert(AlertType.ERROR);
			if (mainStage_ != null && mainStage_.isShowing())
			{
				alert.initOwner(mainStage_);
			}
			alert.setTitle(title);
			alert.setHeaderText(headerText);
			if (StringUtils.isNotBlank(contentText))
			{
				alert.setContentText(contentText);
			}
			alert.showAndWait();
		}
		else
		{
			CountDownLatch cdl = new CountDownLatch(1);
			Platform.runLater(() -> 
			{
				Alert alert = new Alert(AlertType.ERROR);
				if (mainStage_ != null && mainStage_.isShowing())
				{
					alert.initOwner(mainStage_);
				}
				alert.setTitle(title);
				alert.setHeaderText(headerText);
				if (StringUtils.isNotBlank(contentText))
				{
					alert.setContentText(contentText);
				}
				alert.showAndWait();
				cdl.countDown();
			});
			try
			{
				cdl.await();
			}
			catch (InterruptedException e)
			{
				logger.error("Unexpected", e);
			}
		}
	}
}
