package net.sagebits.weatherlink.gui;

import java.net.URL;
import java.util.Date;
import java.util.ResourceBundle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import eu.hansolo.medusa.Gauge;
import eu.hansolo.medusa.Gauge.SkinType;
import eu.hansolo.medusa.GaugeBuilder;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;
import net.sagebits.weatherlink.data.DataFetcher;
import net.sagebits.weatherlink.data.StoredDataTypes;
import net.sagebits.weatherlink.data.WeatherProperty;

public class WeatherLinkLiveGUIController
{
	public static Logger logger = LogManager.getLogger();

	private Stage mainStage_;

	@FXML private ResourceBundle resources;

	@FXML private URL location;

	@FXML private BorderPane bp;

	@FXML private MenuBar menuBar;

	@FXML private FlowPane topFlowPane;

	@FXML private Label tempOut;

	@FXML private Label windOut;

	@FXML private FlowPane bottomFlowPane;

	@FXML
	void initialize()
	{
		assert bp != null : "fx:id=\"bp\" was not injected: check your FXML file 'gui.fxml'.";
		assert menuBar != null : "fx:id=\"menuBar\" was not injected: check your FXML file 'gui.fxml'.";
		assert topFlowPane != null : "fx:id=\"topFlowPane\" was not injected: check your FXML file 'gui.fxml'.";
		assert tempOut != null : "fx:id=\"tempOut\" was not injected: check your FXML file 'gui.fxml'.";
		assert windOut != null : "fx:id=\"windOut\" was not injected: check your FXML file 'gui.fxml'.";
		assert bottomFlowPane != null : "fx:id=\"bottomFlowPane\" was not injected: check your FXML file 'gui.fxml'.";

		logger.debug("Weather Link Live Controller Initialized ");
	}

	public void finishInit(Stage mainStage)
	{
		mainStage_ = mainStage;
		
		String wllDeviceId = DataFetcher.getInstance().getAllWllDeviceIds().iterator().next();
		String sensorOutdoor = DataFetcher.getInstance().getSensorsFor(wllDeviceId, StoredDataTypes.temp).iterator().next();
		String sensorGarageEnv = DataFetcher.getInstance().getSensorsFor(wllDeviceId, StoredDataTypes.temp_in).iterator().next();
		String sensorGarageBar = DataFetcher.getInstance().getSensorsFor(wllDeviceId, StoredDataTypes.bar_absolute).iterator().next();


		Gauge gauge = GaugeBuilder.create().unit("Wind").decimals(1).autoScale(true).minValue(0).maxValue(75).tickLabelDecimals(0)
				.majorTickMarksVisible(true).areaTextVisible(true).prefSize(200, 200).skinType(SkinType.DASHBOARD).build();
		Tooltip t = new Tooltip("Current Wind Speed");
		Tooltip.install(gauge, t);
//
//		FGauge fGauge = FGaugeBuilder.create().prefSize(250, 250).gauge(gauge).gaugeDesign(GaugeDesign.METAL).gaugeBackground(GaugeBackground.CARBON)
//				.foregroundVisible(true).build();
		
		
		topFlowPane.getChildren().add(gauge);
		Platform.runLater(() -> {
			tempOut.textProperty().bind(DataFetcher.getInstance().getDataFor(wllDeviceId, sensorOutdoor, StoredDataTypes.temp).asString());
			windOut.textProperty().bind(DataFetcher.getInstance().getDataFor(wllDeviceId, sensorOutdoor, StoredDataTypes.wind_speed_last).asString());
			WeatherProperty data = DataFetcher.getInstance().getDataFor(wllDeviceId, sensorOutdoor, StoredDataTypes.wind_speed_last);
			gauge.valueProperty().bind(data.asDouble());
			data.addListener(new InvalidationListener()
			{
				
				@Override
				public void invalidated(Observable observable)
				{
					t.setText("Current Wind Speed " + data.asDouble().get() + "\n" + new Date(data.getTimeStamp()).toString());
				}
			});
		});

		
	}
}
