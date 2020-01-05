package net.sagebits.weatherlink.gui;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import net.sagebits.weatherlink.data.DataFetcher;
import net.sagebits.weatherlink.data.StoredDataTypes;

public class WeatherLinkLiveGUIController
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

		String wllDeviceId =  DataFetcher.getInstance().getAllWllDeviceIds().iterator().next();
		String sensorOutdoor =  DataFetcher.getInstance().getSensorsFor(wllDeviceId, StoredDataTypes.temp).iterator().next();
		String sensorGarageEnv = DataFetcher.getInstance().getSensorsFor(wllDeviceId, StoredDataTypes.temp_in).iterator().next();
		String sensorGarageBar = DataFetcher.getInstance().getSensorsFor(wllDeviceId, StoredDataTypes.bar_absolute).iterator().next();
		
		System.out.println(DataFetcher.getInstance().getDataFor(wllDeviceId, sensorGarageEnv, StoredDataTypes.temp_in).asString().get());
		System.out.println(DataFetcher.getInstance().getDataFor(wllDeviceId, sensorGarageBar, StoredDataTypes.bar_sea_level).asString().get());
		
		Thread t = new Thread(() -> 
		{
			try
			{
				Platform.runLater(() -> 
				{
					tempOut.textProperty().bind(DataFetcher.getInstance().getDataFor(wllDeviceId, sensorOutdoor, StoredDataTypes.temp).asString());
					windOut.textProperty().bind(DataFetcher.getInstance().getDataFor(wllDeviceId, sensorOutdoor, StoredDataTypes.wind_speed_last).asString());
				});
					
				Thread.sleep(5000);
				Platform.runLater(() -> 
				{
					
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
