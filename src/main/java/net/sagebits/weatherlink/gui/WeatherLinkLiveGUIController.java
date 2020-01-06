package net.sagebits.weatherlink.gui;

import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;
import org.apache.commons.math3.util.Precision;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import eu.hansolo.medusa.Gauge;
import eu.hansolo.medusa.Gauge.NeedleSize;
import eu.hansolo.medusa.Gauge.NeedleType;
import eu.hansolo.medusa.Gauge.SkinType;
import eu.hansolo.medusa.GaugeBuilder;
import eu.hansolo.medusa.Marker;
import eu.hansolo.medusa.Marker.MarkerType;
import eu.hansolo.medusa.TickLabelLocation;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.MenuBar;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import net.sagebits.weatherlink.data.DataFetcher;
import net.sagebits.weatherlink.data.DataReader;
import net.sagebits.weatherlink.data.StoredDataTypes;
import net.sagebits.weatherlink.data.WeatherProperty;
import net.sagebits.weatherlink.data.periodic.PeriodicData;

public class WeatherLinkLiveGUIController
{
	public static Logger log = LogManager.getLogger();
	
	private DataReader dr;

	private Stage mainStage_;

	@FXML private ResourceBundle resources;

	@FXML private URL location;

	@FXML private BorderPane bp;

	@FXML private MenuBar menuBar;
	
	FlowPane middleFlowPane;
	
	ArrayList<Supplier<Void>> midnightTasks = new ArrayList<>();

	@FXML
	void initialize()
	{
		assert bp != null : "fx:id=\"bp\" was not injected: check your FXML file 'gui.fxml'.";
		assert menuBar != null : "fx:id=\"menuBar\" was not injected: check your FXML file 'gui.fxml'.";

		log.debug("Weather Link Live Controller Initialized ");
	}

	public void finishInit(Stage mainStage)
	{
		mainStage_ = mainStage;
		
		startMidnightTaskRunner();
		
		Thread wllFinder = new Thread(() ->
		{
			log.debug("Data Reader init thread starts");
			try
			{
				dr = new DataReader(Optional.empty());
				dr.startReading(10, true);
			}
			catch (IOException e)
			{
				log.error(e);
				// TODO dialog
			}
			log.debug("Data Reader init thread ends");
		}, "data-reader-init");
		wllFinder.start();
		
		Thread guiInit = new Thread(() ->
		{
			log.debug("Gui init thread starts");
			String wllDeviceId = null;
			while (wllDeviceId == null)
			{
				HashSet<String> wllDeviceIds = DataFetcher.getInstance().getWeatherLinkDeviceIds();
				if (wllDeviceIds.size() == 0)
				{
					log.debug("Waiting for at least one device ID to be found");
					try
					{
						Thread.sleep(500);
					}
					catch (InterruptedException e)
					{
						// don't care
					}
				}
				else
				{
					wllDeviceId = wllDeviceIds.iterator().next();
				}
			}
			
			Set<String> outdoorSensors = DataFetcher.getInstance().getSensorsFor(wllDeviceId, StoredDataTypes.temp);
			String sensorOutdoor;
			if (outdoorSensors.size() > 0)
			{
				sensorOutdoor = outdoorSensors.iterator().next();
				try
				{
					Gauge wg = buildWindGauge(wllDeviceId, sensorOutdoor);
					Platform.runLater(() -> {
						if (middleFlowPane == null)
						{
							middleFlowPane = new FlowPane();
							bp.centerProperty().set(middleFlowPane);
						}
						wg.prefWidthProperty().bind(middleFlowPane.widthProperty().multiply(0.24));
						wg.prefHeightProperty().bind(wg.prefWidthProperty());
						middleFlowPane.getChildren().add(wg);
					});
				}
				catch (Exception e)
				{
					log.error("Problem building Gauge", e);
				}
				try
				{
					Gauge t1 = buildTempGauge(wllDeviceId, sensorOutdoor, "Outside", StoredDataTypes.temp, StoredDataTypes.heat_index, 
							StoredDataTypes.dew_point, StoredDataTypes.wind_chill, StoredDataTypes.thw_index);
					Platform.runLater(() -> {
						if (middleFlowPane == null)
						{
							middleFlowPane = new FlowPane();
							bp.centerProperty().set(middleFlowPane);
						}
						t1.prefWidthProperty().bind(middleFlowPane.widthProperty().multiply(0.25));
						t1.prefHeightProperty().bind(t1.prefWidthProperty());
						middleFlowPane.getChildren().add(t1);
					});
				}
				catch (Exception e)
				{
					log.error("Problem building Gauge", e);
				}
				try
				{
					Gauge humidityOut = buildHumidityGauge(wllDeviceId, sensorOutdoor, StoredDataTypes.hum, "Outside");
					Platform.runLater(() -> {
						if (middleFlowPane == null)
						{
							middleFlowPane = new FlowPane();
							bp.centerProperty().set(middleFlowPane);
						}
						humidityOut.prefWidthProperty().bind(middleFlowPane.widthProperty().multiply(0.24));
						humidityOut.prefHeightProperty().bind(humidityOut.prefWidthProperty());
						middleFlowPane.getChildren().add(humidityOut);
					});
				}
				catch (Exception e)
				{
					log.error("Problem building Gauge", e);
				}
			}
			else
			{
				log.error("No outdoor sensors found, skipping dependent gauges");
			}

			//This call should be safe, if we have a wllDeviceId
			String sensorGarageEnv = DataFetcher.getInstance().getSensorsFor(wllDeviceId, StoredDataTypes.temp_in).iterator().next();
			
			try
			{
				Gauge t2 = buildTempGauge(wllDeviceId, sensorGarageEnv, "Garage", StoredDataTypes.temp_in, StoredDataTypes.heat_index_in, 
						StoredDataTypes.dew_point_in, null, null);
				Platform.runLater(() -> {
					if (middleFlowPane == null)
					{
						middleFlowPane = new FlowPane();
						bp.centerProperty().set(middleFlowPane);
					}
					t2.prefWidthProperty().bind(middleFlowPane.widthProperty().multiply(0.25));
					t2.prefHeightProperty().bind(t2.prefWidthProperty());
					middleFlowPane.getChildren().add(t2);
				});
			}
			catch (Exception e)
			{
				log.error("Problem building Gauge", e);
			}
			
			try
			{
				Gauge humidityIn = buildHumidityGauge(wllDeviceId, sensorGarageEnv, StoredDataTypes.hum_in, "Garage");
				Platform.runLater(() -> {
					if (middleFlowPane == null)
					{
						middleFlowPane = new FlowPane();
						bp.centerProperty().set(middleFlowPane);
					}
					humidityIn.prefWidthProperty().bind(middleFlowPane.widthProperty().multiply(0.24));
					humidityIn.prefHeightProperty().bind(humidityIn.prefWidthProperty());
					middleFlowPane.getChildren().add(humidityIn);
				});
			}
			catch (Exception e)
			{
				log.error("Problem building Gauge", e);
			}	
			
			//This call should be safe, if we have a wllDeviceId
			String sensorGarageBar = DataFetcher.getInstance().getSensorsFor(wllDeviceId, StoredDataTypes.bar_absolute).iterator().next();
			log.debug("Gui init thread ends");
		}, "gui-init");
		guiInit.setDaemon(true);
		guiInit.start();

	}

	private Gauge buildWindGauge(String wllDeviceId, String sensorOutdoorId)
	{
		Gauge gauge = GaugeBuilder.create().unit("Wind").decimals(1).minValue(0).maxValue(75).tickLabelDecimals(0).majorTickMarksVisible(true)
				.tickLabelLocation(TickLabelLocation.INSIDE).areaTextVisible(true).thresholdVisible(true).threshold(0.0).animated(true).skinType(SkinType.DASHBOARD)
				.minSize(100, 100).build();
		Tooltip t = new Tooltip("Current Wind Speed");
		Tooltip.install(gauge, t);

		WeatherProperty currentWind = DataFetcher.getInstance().getDataFor(wllDeviceId, sensorOutdoorId, StoredDataTypes.wind_speed_last)
				.orElseThrow(() -> new RuntimeException("No Data Available for " + StoredDataTypes.wind_speed_last));
		WeatherProperty tenMinHi = DataFetcher.getInstance().getDataFor(wllDeviceId, sensorOutdoorId, StoredDataTypes.wind_speed_hi_last_10_min)
				.orElseThrow(() -> new RuntimeException("No Data Available for " + StoredDataTypes.wind_speed_hi_last_10_min));

		gauge.valueProperty().bind(currentWind.asDouble());
		currentWind.addListener(observable -> t.setText("Current Wind Speed " + currentWind.asDouble().get() + "\n" + new Date(currentWind.getTimeStamp()).toString()));

		tenMinHi.addListener(observable -> gauge.setThreshold(tenMinHi.asDouble().get()));

		return gauge;
	}
	
	private Gauge buildHumidityGauge(String wllDeviceId, String sensorId, StoredDataTypes humiditySensor, String title)
	{
		Gauge gauge = GaugeBuilder.create().unit(title).title("% Hum").decimals(0).minValue(0).maxValue(100)
				.thresholdVisible(false).animated(true).skinType(SkinType.SIMPLE_SECTION)
				.minSize(100, 100).build();

		WeatherProperty humidity = DataFetcher.getInstance().getDataFor(wllDeviceId, sensorId, humiditySensor)
				.orElseThrow(() -> new RuntimeException("No Data Available for " + humiditySensor));

		gauge.valueProperty().bind(humidity.asDouble());

		return gauge;
	}

	private Gauge buildTempGauge(String wllDeviceId, String sensorId, String title, StoredDataTypes sdt, StoredDataTypes heatIndex, 
			StoredDataTypes dewPoint, StoredDataTypes windChill, StoredDataTypes tempHeatWind)
	{
		final List<Marker> markers = new ArrayList<>();
		Marker mMin = new Marker(0.0, "Day Min", Color.BLUE, MarkerType.STANDARD);
		Marker mMax = new Marker(70.0, "Day Max", Color.RED, MarkerType.STANDARD);
		Marker heatIndexM = heatIndex == null ? null : new Marker(0.0, "Heat Index", Color.RED, MarkerType.TRIANGLE);
		Marker windChillM = windChill == null ? null : new Marker(0.0, "Wind Chill", Color.BLUE, MarkerType.TRIANGLE);
		Marker dewPointM = dewPoint == null ? null : new Marker(0.0, "Dew Point", Color.LIGHTBLUE, MarkerType.TRIANGLE);
		Marker tempHeatWindM = tempHeatWind == null ? null : new Marker(0.0, "Temp Heat Wind", Color.LAWNGREEN, MarkerType.TRIANGLE);
		
		markers.add(mMin);
		markers.add(mMax);
		
		if (heatIndex != null)
		{
			heatIndexM.valueProperty().bind(DataFetcher.getInstance().getDataFor(wllDeviceId, sensorId, heatIndex)
					.orElseThrow(() -> new RuntimeException("No Data Available for " + heatIndex)).asDouble());
			markers.add(heatIndexM);
		}
		if (dewPoint != null)
		{
			dewPointM.valueProperty().bind(DataFetcher.getInstance().getDataFor(wllDeviceId, sensorId, dewPoint)
					.orElseThrow(() -> new RuntimeException("No Data Available for " + dewPoint)).asDouble());
			markers.add(dewPointM);
		}
		if (windChill != null)
		{
			windChillM.valueProperty().bind(DataFetcher.getInstance().getDataFor(wllDeviceId, sensorId, windChill)
					.orElseThrow(() -> new RuntimeException("No Data Available for " + windChill)).asDouble());
			markers.add(windChillM);
		}
		if (tempHeatWind != null)
		{
			tempHeatWindM.valueProperty().bind(DataFetcher.getInstance().getDataFor(wllDeviceId, sensorId, tempHeatWind)
					.orElseThrow(() -> new RuntimeException("No Data Available for " + tempHeatWind)).asDouble());
			markers.add(tempHeatWindM);
		}

		@SuppressWarnings("unchecked") 
		Gauge gauge = GaugeBuilder.create().title(title).titleColor(Color.GRAY).subTitle("Temp")
				.decimals(1).minValue(20).maxValue(80).tickLabelDecimals(0).majorTickMarksVisible(true)
				.tickMarkRingVisible(true)
				.mediumTickMarksVisible(true)
				.mediumTickMarkLengthFactor(.6)
				.minorTickMarksVisible(true)
				.tickLabelLocation(TickLabelLocation.OUTSIDE)
				.needleSize(NeedleSize.THIN)
				.needleColor(Color.BLACK)
				.needleType(NeedleType.VARIOMETER)
				.areaTextVisible(true).thresholdVisible(false)
				.animated(true).markers(markers).markersVisible(true).skinType(SkinType.GAUGE).minSize(100, 100).build();

		WeatherProperty temp = DataFetcher.getInstance().getDataFor(wllDeviceId, sensorId, sdt)
				.orElseThrow(() -> new RuntimeException("No Data Available for " + sdt));
		
		//The highest the guage needs to go
		DoubleSupplier maxCalc = () ->
		{
			double current = temp.asDouble().get();
			return Math.round(Math.max(
					Math.max((heatIndexM == null ? current : heatIndexM.getValue()), (tempHeatWindM == null ? current : tempHeatWindM.getValue())), 
					Math.min(110.0, current + 20.0)));
		};
		
		//The lowest the guage needs to go
		DoubleSupplier minCalc = () ->
		{
			double current = temp.asDouble().get();
			return Math.round(Math.min(
					Math.min((dewPointM == null ? current : dewPointM.getValue()),(windChillM == null ? current : windChillM.getValue())), 
					Math.max(-40.0, current - 20.0)));
		};

		gauge.setMinValue(minCalc.getAsDouble());
		gauge.setMaxValue(maxCalc.getAsDouble());
		
		Date today = new Date();

		Optional<Float> maxValue = PeriodicData.getInstance().getMaxForDay(wllDeviceId, sensorId, sdt, today);
		Optional<Float> minValue = PeriodicData.getInstance().getMinForDay(wllDeviceId, sensorId, sdt, today);

		mMin.setValue(Precision.round(minValue.map(f -> (double) f).orElse(temp.asDouble().get()), 1));
		mMax.setValue(Precision.round(maxValue.map(f -> (double) f).orElse(temp.asDouble().get()), 1));
		
		addMidnightTask(() ->
		{
			Platform.runLater(() ->
			{
				mMin.setValue(temp.asDouble().get());
				mMax.setValue(temp.asDouble().get());
			});
			return null;
		});

		gauge.valueProperty().addListener(observable -> {
			double current = temp.asDouble().get();
			if (current > mMax.getValue())
			{
				mMax.setValue(current);
			}
			if (current < mMin.getValue())
			{
				mMin.setValue(current);
				
			}
			gauge.setMaxValue(maxCalc.getAsDouble());
			gauge.setMinValue(minCalc.getAsDouble());
		});
		
		gauge.valueProperty().bind(temp.asDouble());

		return gauge;

	}
	
	private void addMidnightTask(Supplier<Void> task)
	{
		synchronized (midnightTasks)
		{
			midnightTasks.add(task);
		}
	}
	
	private void startMidnightTaskRunner()
	{
		Thread t = new Thread(() ->
		{
			ZonedDateTime now = ZonedDateTime.of(LocalDateTime.now(), ZoneId.systemDefault());
			ZonedDateTime runAt = now.withHour(0).withMinute(0).withSecond(0).plusDays(1);
			
			while (true)
			{
				long sleep = Duration.between(now, runAt).toMillis();
				if (sleep > 0)
				{
					try
					{
						log.debug("Task runner sleeps for {}", sleep);
						Thread.sleep(sleep);
					}
					catch (InterruptedException e)
					{
						//don't care about spurious interrupts.  recalculate, and sleep again.
						
					}
					finally
					{
						//Update our 'now' to when we woke up
						log.debug("Task runner awakes");
						now = ZonedDateTime.of(LocalDateTime.now(), ZoneId.systemDefault());
					}
				}
				if (Duration.between(now,  runAt).toMillis() <= 0)
				{
					synchronized (midnightTasks)
					{
						log.debug("Task runner runs {} tasks", midnightTasks.size());
						//Time to run our tasks
						for (Supplier<Void> task : midnightTasks)
						{
							try
							{
								task.get();
							}
							catch (Exception e)
							{
								log.error("Midnight task failed", e);
							}
						}
					}
					
					//Advance the next run time
					runAt = runAt.plusDays(1);
				}
			}
		}, "Midnight Task Runner");
		t.setDaemon(true);
		t.start();
	}

	public void shutdown()
	{
		dr.stopReading();
		
	}
}
