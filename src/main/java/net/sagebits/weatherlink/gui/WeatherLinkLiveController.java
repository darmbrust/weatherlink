package net.sagebits.weatherlink.gui;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import net.sagebits.weatherlink.data.StoredDataTypes;
import net.sagebits.weatherlink.data.live.LiveData;
import net.sagebits.weatherlink.data.periodic.PeriodicData;

public class WeatherLinkLiveController
{
	public static Logger logger = LogManager.getLogger();

	private Stage mainStage_;
	
	@FXML 
	private Label tempOut;
	
	@FXML 
	private Label windOut;

	@FXML
	void initialize()
	{
		assert tempOut != null : "fx:id=\"tempOut\" was not injected: check your FXML file 'gui.fxml'.";
		
		Thread t = new Thread(() -> 
		{
			try
			{
				PeriodicData.getInstance().registerConsumer(v ->
				{
					Platform.runLater(() -> tempOut.textProperty().set(PeriodicData.getInstance().getLatestData(StoredDataTypes.temp) + ""));
				});
				Thread.sleep(5000);
				Platform.runLater(() -> 
				{
					windOut.textProperty().bind(LiveData.getInstance().getLiveData("").getLiveData("").getWind_speed_last().asString())	;
				});
			}
			catch (InterruptedException e)
			{
				System.out.println("oops");
			}
		});
		t.start();

		logger.debug("Weather Link Live Controller Initialized ");
	}

	public void finishInit(Stage mainStage)
	{
		mainStage_ = mainStage;
	}
}
