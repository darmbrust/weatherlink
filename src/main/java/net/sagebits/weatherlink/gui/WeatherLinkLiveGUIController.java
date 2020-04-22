package net.sagebits.weatherlink.gui;

import java.net.URL;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.util.Precision;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import eu.hansolo.medusa.Gauge;
import eu.hansolo.medusa.Gauge.KnobType;
import eu.hansolo.medusa.Gauge.NeedleBehavior;
import eu.hansolo.medusa.Gauge.NeedleShape;
import eu.hansolo.medusa.Gauge.NeedleSize;
import eu.hansolo.medusa.Gauge.NeedleType;
import eu.hansolo.medusa.Gauge.SkinType;
import eu.hansolo.medusa.GaugeBuilder;
import eu.hansolo.medusa.Marker;
import eu.hansolo.medusa.Marker.MarkerType;
import eu.hansolo.medusa.Section;
import eu.hansolo.medusa.TickLabelLocation;
import eu.hansolo.medusa.TickLabelOrientation;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.Chart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Pair;
import javafx.util.StringConverter;
import net.sagebits.weatherlink.data.DataCondenser;
import net.sagebits.weatherlink.data.DataFetcher;
import net.sagebits.weatherlink.data.DataReader;
import net.sagebits.weatherlink.data.StoredDataTypes;
import net.sagebits.weatherlink.data.WeatherProperty;
import net.sagebits.weatherlink.data.live.LiveData;
import net.sagebits.weatherlink.data.periodic.PeriodicData;
import net.sagebits.weatherlink.gui.gapchart.GapLineChart;
import net.sagebits.weatherlink.gui.gapchart.GapNumberAxis;

public class WeatherLinkLiveGUIController
{
	public static Logger log = LogManager.getLogger(WeatherLinkLiveGUIController.class);
	
	private DataReader dr;

	private Stage mainStage_;

	@FXML private ResourceBundle resources;

	@FXML private URL location;

	@FXML private BorderPane bp;

	FlowPane middleFlowPane;
	
	private ArrayList<Supplier<Void>> midnightTasks = new ArrayList<>();
	private ScheduledExecutorService periodicJobs = Executors.newScheduledThreadPool(2, r -> new Thread(r, "Periodic GUI Jobs"));
	
	protected static String ip = null;

	@FXML
	void initialize()
	{
		assert bp != null : "fx:id=\"bp\" was not injected: check your FXML file 'gui.fxml'.";

		log.debug("Weather Link Live Controller Initialized ");
	}

	public void finishInit(Stage mainStage)
	{
		mainStage_ = mainStage;
		mainStage_.getIcons().add(new Image(WeatherLinkLiveGUIController.class.getResourceAsStream("/appIcon.png")));
		
		startMidnightTaskRunner();
		
		Thread wllFinder = new Thread(() ->
		{
			log.debug("Data Reader init thread starts");
			try
			{
				dr = new DataReader(Optional.ofNullable(StringUtils.isBlank(ip) ? null : ip));
				dr.startReading(10, true);
			}
			catch (Exception e)
			{
				log.error("Error starting data reading", e);
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
			String sensorOutdoor = null;
			if (outdoorSensors.size() > 0)
			{
				sensorOutdoor = outdoorSensors.iterator().next();
//				try
//				{
//					Gauge wg = buildWindGauge(wllDeviceId, sensorOutdoor);
//					Platform.runLater(() -> {
//						if (middleFlowPane == null)
//						{
//							middleFlowPane = new FlowPane();
//							bp.centerProperty().set(middleFlowPane);
//						}
//						wg.prefWidthProperty().bind(middleFlowPane.widthProperty().multiply(0.24));
//						wg.prefHeightProperty().bind(wg.prefWidthProperty());
//						middleFlowPane.getChildren().add(wg);
//					});
//				}
//				catch (Exception e)
//				{
//					log.error("Problem building Gauge", e);
//				}
				
//				try
//				{
//					Gauge wg = buildQuarterWindGauge(wllDeviceId, sensorOutdoor);
//					Platform.runLater(() -> {
//						if (middleFlowPane == null)
//						{
//							middleFlowPane = new FlowPane();
//							bp.centerProperty().set(middleFlowPane);
//						}
//						wg.prefWidthProperty().bind(middleFlowPane.widthProperty().multiply(0.22));
//						wg.prefHeightProperty().bind(wg.prefWidthProperty());
//						middleFlowPane.getChildren().add(wg);
//					});
//				}
//				catch (Exception e)
//				{
//					log.error("Problem building Gauge", e);
//				}
//				
//				try
//				{
//					Gauge wg = buildWindDirectionGauge(wllDeviceId, sensorOutdoor);
//					Platform.runLater(() -> {
//						if (middleFlowPane == null)
//						{
//							middleFlowPane = new FlowPane();
//							bp.centerProperty().set(middleFlowPane);
//						}
//						//wg.setPadding(new Insets(20));
//						wg.prefWidthProperty().bind(middleFlowPane.widthProperty().multiply(0.22));
//						wg.prefHeightProperty().bind(wg.prefWidthProperty());
//						middleFlowPane.getChildren().add(wg);
//					});
//				}
//				catch (Exception e)
//				{
//					log.error("Problem building Gauge", e);
//				}
				
				try
				{
					Gauge wsg = buildQuarterWindGauge(wllDeviceId, sensorOutdoor);
					Gauge wdg = buildWindDirectionGauge(wllDeviceId, sensorOutdoor);
					
					StackPane sp = new StackPane();
					sp.getChildren().add(wdg);
					sp.getChildren().add(wsg);
					StackPane.setAlignment(wdg, Pos.CENTER);
					wsg.widthProperty().addListener(change -> StackPane.setMargin(wdg, new Insets(wsg.getWidth() / 3.0, 0, 0, wsg.getWidth() / 6.5)));
					
					Platform.runLater(() -> {
						if (middleFlowPane == null)
						{
							middleFlowPane = new FlowPane();
							bp.centerProperty().set(middleFlowPane);
						}
						sp.prefWidthProperty().bind(middleFlowPane.widthProperty().multiply(0.24));
						sp.prefHeightProperty().bind(sp.prefWidthProperty());
						wdg.prefWidthProperty().bind(wsg.widthProperty().multiply(0.40));
						wdg.prefHeightProperty().bind(wdg.widthProperty());
						wdg.maxWidthProperty().bind(wdg.prefWidthProperty());
						wdg.maxHeightProperty().bind(wdg.prefHeightProperty());
						middleFlowPane.getChildren().add(sp);
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
					Chart chart = createTempChart(wllDeviceId, sensorOutdoor);
					Platform.runLater(() -> {
						if (middleFlowPane == null)
						{
							middleFlowPane = new FlowPane();
							bp.centerProperty().set(middleFlowPane);
						}
						chart.prefWidthProperty().bind(middleFlowPane.widthProperty().multiply(0.48));
						chart.prefHeightProperty().bind(chart.prefWidthProperty().divide(2.0));
						middleFlowPane.getChildren().add(chart);
					});
				}
				catch (Exception e)
				{
					log.error("Problem building temp Chart", e);
				}
				
				try
				{
					Chart chart = createWindChart(wllDeviceId, sensorOutdoor);
					Platform.runLater(() -> {
						if (middleFlowPane == null)
						{
							middleFlowPane = new FlowPane();
							bp.centerProperty().set(middleFlowPane);
						}
						chart.prefWidthProperty().bind(middleFlowPane.widthProperty().multiply(0.48));
						chart.prefHeightProperty().bind(chart.prefWidthProperty().divide(2.0));
						middleFlowPane.getChildren().add(chart);
					});
				}
				catch (Exception e)
				{
					log.error("Problem building wind Chart", e);
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
					humidityIn.prefWidthProperty().bind(middleFlowPane.widthProperty().multiply(0.23));
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
			try
			{
				Chart chart = createBarometerChart(wllDeviceId, sensorGarageBar);
				Platform.runLater(() -> {
					if (middleFlowPane == null)
					{
						middleFlowPane = new FlowPane();
						bp.centerProperty().set(middleFlowPane);
					}
					chart.prefWidthProperty().bind(middleFlowPane.widthProperty().multiply(0.24));
					chart.prefHeightProperty().bind(chart.prefWidthProperty());
					middleFlowPane.getChildren().add(chart);
				});
			}
			catch (Exception e)
			{
				log.error("Problem building barometric Chart", e);
			}
			
			if (outdoorSensors.size() > 0)
			{
				try
				{
					Chart chart = createRainTotalsChart(wllDeviceId, sensorOutdoor);
					Platform.runLater(() -> {
						if (middleFlowPane == null)
						{
							middleFlowPane = new FlowPane();
							bp.centerProperty().set(middleFlowPane);
						}
						chart.prefWidthProperty().bind(middleFlowPane.widthProperty().multiply(0.23));
						chart.prefHeightProperty().bind(chart.prefWidthProperty());
						middleFlowPane.getChildren().add(chart);
					});
				}
				catch (Exception e)
				{
					log.error("Problem building rain total Chart", e);
				}
				try
				{
					Chart chart = createRainCurrentChart(wllDeviceId, sensorOutdoor);
					Platform.runLater(() -> {
						if (middleFlowPane == null)
						{
							middleFlowPane = new FlowPane();
							bp.centerProperty().set(middleFlowPane);
						}
						chart.prefWidthProperty().bind(middleFlowPane.widthProperty().multiply(0.24));
						chart.prefHeightProperty().bind(chart.prefWidthProperty());
						middleFlowPane.getChildren().add(chart);
					});
				}
				catch (Exception e)
				{
					log.error("Problem building wind Chart", e);
				}
			}
			log.debug("Gui init thread ends");
		}, "gui-init");
		guiInit.setDaemon(true);
		guiInit.start();

	}

	private Gauge buildWindGauge(String wllDeviceId, String sensorOutdoorId)
	{
		Gauge gauge = GaugeBuilder.create().unit("Wind")
				.decimals(1).minValue(0).maxValue(75).tickLabelDecimals(0).majorTickMarksVisible(true)
				.tickLabelLocation(TickLabelLocation.INSIDE)
				.areaTextVisible(true).thresholdVisible(true).threshold(0.0)
				.animated(true).skinType(SkinType.DASHBOARD)
				.minSize(75, 75).build();
		Tooltip t = new Tooltip("Current Wind Speed");
		Tooltip.install(gauge, t);

		WeatherProperty currentWind = DataFetcher.getInstance().getDataFor(wllDeviceId, sensorOutdoorId, StoredDataTypes.wind_speed_last);
		WeatherProperty tenMinHi = DataFetcher.getInstance().getDataFor(wllDeviceId, sensorOutdoorId, StoredDataTypes.wind_speed_hi_last_10_min);

		gauge.valueProperty().bind(currentWind.asDouble());
		gauge.valueVisibleProperty().bind(currentWind.isValid());
		currentWind.addListener(observable -> t.setText("Current Wind Speed " + currentWind.asDouble().get() + "\n" + new Date(currentWind.getTimeStamp()).toString()));

		gauge.thresholdVisibleProperty().bind(tenMinHi.isValid());
		tenMinHi.addListener(observable -> gauge.setThreshold(tenMinHi.asDouble().get()));

		return gauge;
	}
	
	private Gauge buildQuarterWindGauge(String wllDeviceId, String sensorOutdoorId)
	{
		Marker dayMax = new Marker(0.0, "Day Max", Color.RED, MarkerType.STANDARD);
		Marker tenMH = new Marker(0.0, "Ten Minute High", Color.CADETBLUE, MarkerType.TRIANGLE);
		Marker twoMH = new Marker(0.0, "Two Minute High", Color.DARKVIOLET, MarkerType.TRIANGLE);
		Marker tenMA = new Marker(0.0, "Ten Minute Average", Color.CADETBLUE, MarkerType.DOT);
		Marker twoMA = new Marker(0.0, "Two Minute Average", Color.DARKVIOLET, MarkerType.DOT);
		Marker oneMA = new Marker(0.0, "One Minute Average", Color.CORNFLOWERBLUE, MarkerType.DOT);
		
		Gauge gauge = GaugeBuilder.create()
				.unit("mph    ").title("Wind  ")
				.animated(true)
				.autoScale(true)
				.minValue(0).maxValue(50.0)
				.skinType(SkinType.QUARTER)
				.decimals(1)
				.markers(dayMax, tenMH, twoMH, tenMA, twoMA, oneMA)
				.markersVisible(true)
				.knobType(KnobType.STANDARD)
				.knobColor(Color.RED)
				.needleShape(NeedleShape.FLAT)
				.needleType(NeedleType.VARIOMETER)
				.needleSize(NeedleSize.THIN)
				.minSize(75, 75).build();

		WeatherProperty currentWind = DataFetcher.getInstance().getDataFor(wllDeviceId, sensorOutdoorId, StoredDataTypes.wind_speed_last);
		gauge.valueProperty().bind(currentWind.asDouble());
		gauge.valueVisibleProperty().bind(currentWind.isValid());
		
		//Hackish way to move the speed output
		Pane p = (Pane)gauge.getChildrenUnmodifiable().get(0);
		for (Node n : p.getChildren())
		{
			if (n instanceof Text && ((Text)n).getText().length() > 0)
			{
				gauge.widthProperty().addListener(change -> 
					((Text)n).relocate(gauge.widthProperty().divide(2).multiply(-1).get(), gauge.widthProperty().divide(1.35).multiply(-1).get()));
			}
		}
		
		Optional<Float> maxValue = PeriodicData.getInstance().getMaxForDay(wllDeviceId, sensorOutdoorId, StoredDataTypes.wind_speed_hi_last_2_min, new Date());

		dayMax.setValue(Precision.round(maxValue.map(f -> (double) f).orElse(currentWind.asDouble().get()), 1));
		
		gauge.valueProperty().addListener(observable -> {
			double current = currentWind.asDouble().get();
			if (current > dayMax.getValue())
			{
				dayMax.setValue(current);
			}
			
			gauge.setMaxValue(Math.max((current + 5), 50));
		});
		
		addMidnightTask(() ->
		{
			Platform.runLater(() ->
			{
				double current = currentWind.asDouble().get();
				dayMax.setValue(current);
				gauge.setMaxValue(Math.max((current + 5), 50));
			});
			return null;
		});

		//TODO bind something to the invalid state on all these markers
		tenMH.valueProperty().bind(DataFetcher.getInstance().getDataFor(wllDeviceId, sensorOutdoorId, StoredDataTypes.wind_speed_hi_last_10_min).asDouble());
		twoMH.valueProperty().bind(DataFetcher.getInstance().getDataFor(wllDeviceId, sensorOutdoorId, StoredDataTypes.wind_speed_hi_last_2_min).asDouble());
		
		twoMH.valueProperty().addListener(observable -> {
			double twoMinMax = twoMH.valueProperty().get();
			if (twoMinMax > dayMax.getValue())
			{
				dayMax.setValue(twoMinMax);
			}
			
			gauge.setMaxValue(Math.max((twoMinMax + 5), 50));
		});
		
		tenMA.valueProperty().bind(DataFetcher.getInstance().getDataFor(wllDeviceId, sensorOutdoorId, StoredDataTypes.wind_speed_avg_last_10_min).asDouble());
		twoMA.valueProperty().bind(DataFetcher.getInstance().getDataFor(wllDeviceId, sensorOutdoorId, StoredDataTypes.wind_speed_avg_last_2_min).asDouble());
		oneMA.valueProperty().bind(DataFetcher.getInstance().getDataFor(wllDeviceId, sensorOutdoorId, StoredDataTypes.wind_speed_avg_last_1_min).asDouble());
		
		Tooltip tt = new Tooltip("Mode Pending");
		Tooltip.install(gauge, tt);
		final SimpleDateFormat sdf = new SimpleDateFormat("h:mm:ss");
		
		final Consumer<Void> updateTooltip = input -> 
		{
			//This will give us a pulse, every read attempt.  Don't actually care about the value.
			if (currentWind.getTimeStamp() < (currentWind.getLocalTimeStamp() - 6000))
			{
				//More than 6 seconds out of date, missed at least 2 live data pulses.
				gauge.setKnobColor(Color.BLACK);
				tt.setText("Poll Last Update " + sdf.format(new Date(currentWind.getTimeStamp())));
			}
			else if (currentWind.getTimeStamp() < (System.currentTimeMillis() - 30000))
			{
				//More than 30 seconds for any data.
				gauge.setKnobColor(Color.RED);
				tt.setText("Last Update " + sdf.format(new Date(currentWind.getTimeStamp())));
			}
			else
			{
				gauge.setKnobColor(Color.GREEN);
				tt.setText("Live Last Update " + sdf.format(new Date(currentWind.getTimeStamp())));
			}
		};

		if (dr != null)
		{
			dr.getLastReadAttemptTime().addListener((value, old, newv) -> 
			{
				updateTooltip.accept(null);
			});
		}
		
		LiveData.getInstance().getLastDataTime().addListener((value, old, newv) -> 
		{
			updateTooltip.accept(null);
		});
		
		return gauge;
	}
	
	private Gauge buildWindDirectionGauge(String wllDeviceId, String sensorId)
	{
		Section avgTen1 = new Section(0, 0, Color.LIGHTGRAY);
		Section avgTen2 = new Section(0, 0, Color.LIGHTGRAY);
		Section avgTwo1 = new Section(0, 0, Color.LIGHTCYAN);
		Section avgTwo2 = new Section(0, 0, Color.LIGHTCYAN);
		Section avgOne1 = new Section(0, 0, Color.LIGHTSLATEGREY);
		Section avgOne2 = new Section(0, 0, Color.LIGHTSLATEGREY);
		Gauge gauge = GaugeBuilder.create()
				.skinType(SkinType.GAUGE)
				.minSize(50, 50)
				.borderPaint(Gauge.DARK_COLOR)
				.minValue(0)
				.maxValue(360)
				.startAngle(180)
				.angleRange(360)
				.autoScale(false)
				.decimals(0)
				.minorTickMarksVisible(false)
				.mediumTickMarksVisible(false)
				.majorTickMarksVisible(true)
				.valueVisible(false)
				.customTickLabelsEnabled(true)
				.tickLabelLocation(TickLabelLocation.INSIDE)
				.tickLabelOrientation(TickLabelOrientation.ORTHOGONAL)
				.minorTickSpace(1.5)
				.majorTickSpace(22.5)
				.customTickLabels("N", "", "NE", "", "E", "", "SE", "", "S",
								  "", "SW", "", "W", "", "NW", "")
				.customTickLabelFontSize(32)
				.knobType(KnobType.STANDARD)
				.knobColor(Gauge.DARK_COLOR)
				.needleShape(NeedleShape.FLAT)
				.needleType(NeedleType.BIG)
				.needleBehavior(NeedleBehavior.OPTIMIZED)
				.tickLabelColor(Gauge.DARK_COLOR)
				.animated(true)
				.animationDuration(800)
				.sections(avgTen1, avgTen2, avgTwo1, avgTwo2, avgOne1, avgOne2)
				.sectionsVisible(true)
				.ledColor(Color.RED)
				.ledVisible(false)
				.build();

		WeatherProperty windDirP = DataFetcher.getInstance().getDataFor(wllDeviceId, sensorId, StoredDataTypes.wind_dir_last);
		gauge.ledVisibleProperty().bind(windDirP.isValid().not());
		
		//TODO figure out how to make individual sections visible / invisible
		WeatherProperty windDirAvgTen = DataFetcher.getInstance().getDataFor(wllDeviceId, sensorId, StoredDataTypes.wind_dir_scalar_avg_last_10_min);
		WeatherProperty windDirAvgTwo = DataFetcher.getInstance().getDataFor(wllDeviceId, sensorId, StoredDataTypes.wind_dir_scalar_avg_last_2_min);
		WeatherProperty windDirAvgOne = DataFetcher.getInstance().getDataFor(wllDeviceId, sensorId, StoredDataTypes.wind_dir_scalar_avg_last_1_min);

		gauge.valueProperty().bind(windDirP.asDouble());
		
		windDirAvgTen.addListener(change -> {
				updateWindSection(windDirAvgTen, 20, avgTen1, avgTen2);
		});
		windDirAvgTwo.addListener(change -> {
			updateWindSection(windDirAvgTwo, 14, avgTwo1, avgTwo2);
		});
		windDirAvgOne.addListener(change -> {
			updateWindSection(windDirAvgOne, 8, avgOne1, avgOne2);
		});
		
		updateWindSection(windDirAvgTen, 20, avgTen1, avgTen2);
		updateWindSection(windDirAvgTwo, 14, avgTwo1, avgTwo2);
		updateWindSection(windDirAvgOne, 8, avgOne1, avgOne2);
		return gauge;
	}
	
	private void updateWindSection (WeatherProperty wp, int halfSize, Section lowerHalf, Section upperHalf)
	{
		//We use 2 sections, so we can deal with the issues that occur near 360 and 0....
		double start1 = wp.asDouble().get() - halfSize;
		double end1 = start1 + halfSize;
		double start2 = end1;
		double end2 = start2 + halfSize;
		
		if (start1 < 0)
		{
			//plus, not minus, due to double negative
			start1 = 360 + start1;
			end1 = 360;
			start2 = 0;
		}
		else if (end2 > 360)
		{
			end2 = end2 - 360;
			start2 = 0;
			end1 = 360;
		}
		
		lowerHalf.setStart(start1);
		lowerHalf.setStop(end1);
		upperHalf.setStart(start2);
		upperHalf.setStop(end2);
	};
	
	private Gauge buildHumidityGauge(String wllDeviceId, String sensorId, StoredDataTypes humiditySensor, String title)
	{
		Gauge gauge = GaugeBuilder.create().unit(title).title("% Hum").decimals(0).minValue(0).maxValue(100)
				.thresholdVisible(false).animated(true).skinType(SkinType.SIMPLE_SECTION)
				.minSize(75, 75).build();

		WeatherProperty humidity = DataFetcher.getInstance().getDataFor(wllDeviceId, sensorId, humiditySensor);

		gauge.valueProperty().bind(humidity.asDouble());
		gauge.valueVisibleProperty().bind(humidity.isValid());

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
		
		//TODO hide markers when invalid
		if (heatIndex != null)
		{
			heatIndexM.valueProperty().bind(DataFetcher.getInstance().getDataFor(wllDeviceId, sensorId, heatIndex).asDouble());
			markers.add(heatIndexM);
		}
		if (dewPoint != null)
		{
			dewPointM.valueProperty().bind(DataFetcher.getInstance().getDataFor(wllDeviceId, sensorId, dewPoint).asDouble());
			markers.add(dewPointM);
		}
		if (windChill != null)
		{
			windChillM.valueProperty().bind(DataFetcher.getInstance().getDataFor(wllDeviceId, sensorId, windChill).asDouble());
			markers.add(windChillM);
		}
		if (tempHeatWind != null)
		{
			tempHeatWindM.valueProperty().bind(DataFetcher.getInstance().getDataFor(wllDeviceId, sensorId, tempHeatWind).asDouble());
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
				.animated(true).markers(markers).markersVisible(true).skinType(SkinType.GAUGE).minSize(75, 75).build();

		WeatherProperty temp = DataFetcher.getInstance().getDataFor(wllDeviceId, sensorId, sdt);
		
		//The highest the guage needs to go
		DoubleSupplier maxCalc = () ->
		{
			double current = temp.asDouble().get();
			return Math.round(Math.max(
					Math.max((heatIndexM == null ? current : heatIndexM.getValue()), (tempHeatWindM == null ? current : tempHeatWindM.getValue())), 
					Math.min(110.0, Math.max(current, mMax.getValue()) + 20.0)));
		};
		
		//The lowest the guage needs to go
		DoubleSupplier minCalc = () ->
		{
			double current = temp.asDouble().get();
			return Math.round(Math.min(
					Math.min((dewPointM == null ? current : dewPointM.getValue()),(windChillM == null ? current : windChillM.getValue())), 
					Math.max(-40.0, Math.min(current, mMin.getValue()) - 20.0)));
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
		gauge.valueVisibleProperty().bind(temp.isValid());

		return gauge;

	}
	
	private XYChart<GapNumberAxis, GapNumberAxis> createWindChart(String wllDeviceId, String sensorId) throws SQLException
	{
		Series<Long, Double> series1 = new Series<>();
		series1.setName("Average Wind Speed");
		Series<Long, Double> series2 = new Series<>();
		series2.setName("Peak Gust");
		XYChart<GapNumberAxis, GapNumberAxis> chart = createHourChart(24, false, series1, series2); 
		
		Runnable updateData = () ->
		{
			try
			{
				log.debug("Updating wind chart");
				long startTime = System.currentTimeMillis() - (24*60*60*1000);
				
				List<Object[]> windData = PeriodicData.getInstance().getDataForRange(wllDeviceId, sensorId, startTime, 
						Optional.empty(), StoredDataTypes.wind_speed_avg_last_1_min, StoredDataTypes.wind_speed_hi_last_2_min);
				
				//If doing this as a stacked area chart, need to subtract the series 1 data from the series 2 data
				ArrayList<Pair<Long, Double>> avgWindSpeeds = DataCondenser.averageEvery(15,  windData.stream().map(in -> new Pair<Long, Number>((Long)in[0], (Number)in[1])), 
						DataCondenser.MISSING_BEHAVIOR.ZERO);
				ArrayList<Pair<Long, Double>> highWindSpeeds = DataCondenser.maxEvery(15,  windData.stream().map(in -> new Pair<Long, Number>((Long)in[0], (Number)in[2])));

				
				Platform.runLater(() ->
				{
					series1.getData().clear();
					for (Pair<Long, Double> point : avgWindSpeeds)
					{
						series1.getData().add(new XYChart.Data<>(point.getKey(), point.getValue()));
					}
					series2.getData().clear();
					for (Pair<Long, Double> point : highWindSpeeds)
					{
						series2.getData().add(new XYChart.Data<>(point.getKey(), point.getValue()));
					}
					GapNumberAxis.class.cast(chart.getXAxis()).setLowerBound(startTime);
					GapNumberAxis.class.cast(chart.getXAxis()).setUpperBound(System.currentTimeMillis());
				});
			}
			catch (Exception e)
			{
				log.error("Unexpected error updating chart series data", e);
			}
		};

		periodicJobs.scheduleAtFixedRate(updateData, 0, 5, TimeUnit.MINUTES);
		chart.setTitle("Wind Speed Avg and Gust");
		chart.setLegendVisible(false);
		return chart;
	}
	
	private XYChart<GapNumberAxis, GapNumberAxis> createTempChart(String wllDeviceId, String sensorId) throws SQLException
	{
		Series<Long, Double> series1 = new Series<>();
		series1.setName("Outdoor Temp");
		
		XYChart<GapNumberAxis, GapNumberAxis> chart = createHourChart(24, true, series1); 
		
		Runnable updateData = () ->
		{
			try
			{
				log.debug("Updating outdoor temp chart");
				long startTime =  System.currentTimeMillis() - (24*60*60*1000);
				List<Object[]> data = PeriodicData.getInstance().getDataForRange(wllDeviceId, sensorId, startTime, 
						Optional.empty(), StoredDataTypes.temp);
				
				ArrayList<Pair<Long, Double>> averaged = DataCondenser.averageEvery(5,  data.stream().map(in -> new Pair<Long, Number>((Long)in[0], (Number)in[1])), 
						DataCondenser.MISSING_BEHAVIOR.NAN);
				
				Platform.runLater(() ->
				{
					series1.getData().clear();
					for (Pair<Long, Double> point : averaged)
					{
						series1.getData().add(new XYChart.Data<>(point.getKey(), point.getValue()));
					}
					GapNumberAxis.class.cast(chart.getXAxis()).setLowerBound(startTime);
					GapNumberAxis.class.cast(chart.getXAxis()).setUpperBound(System.currentTimeMillis());
				});
			}
			catch (Exception e)
			{
				log.error("Unexpected error updating chart series data", e);
			}
		};

		periodicJobs.scheduleAtFixedRate(updateData, 0, 5, TimeUnit.MINUTES);
		chart.setTitle("Outdoor Temp");
		chart.setLegendVisible(false);
		return chart;
	}
	
	private XYChart<GapNumberAxis, GapNumberAxis> createBarometerChart(String wllDeviceId, String sensorId) throws SQLException
	{
		Series<Long, Double> series1 = new Series<>();
		series1.setName("Barometric Pressure");
		XYChart<GapNumberAxis, GapNumberAxis> chart = createHourChart(12, true, series1); 
		GapNumberAxis.class.cast(chart.getYAxis()).setForceZeroInRange(false);
		GapNumberAxis.class.cast(chart.getYAxis()).setTickLabelFormatter(new StringConverter<Number>()
		{
			DecimalFormat df = new DecimalFormat("00.00");
			@Override
			public String toString(Number object)
			{
				
				return df.format(object.doubleValue());
			}

			@Override
			public Number fromString(String string)
			{
				throw new UnsupportedOperationException();
			}
		});;
		
		Runnable updateData = () ->
		{
			try
			{
				log.debug("Updating bar chart");
				long startTime = System.currentTimeMillis() - (12*60*60*1000);
				
				List<Object[]> data = PeriodicData.getInstance().getDataForRange(wllDeviceId, sensorId, startTime, 
						Optional.empty(), StoredDataTypes.bar_sea_level);
				
				ArrayList<Pair<Long, Double>> averaged = DataCondenser.averageEvery(5,  data.stream().map(in -> new Pair<Long, Number>((Long)in[0], (Number)in[1])), 
						DataCondenser.MISSING_BEHAVIOR.NAN);
				
				Platform.runLater(() ->
				{
					series1.getData().clear();
					
					for (Pair<Long, Double> point : averaged)
					{
						series1.getData().add(new XYChart.Data<>(point.getKey(), point.getValue()));
					}
					GapNumberAxis.class.cast(chart.getXAxis()).setLowerBound(startTime);
					GapNumberAxis.class.cast(chart.getXAxis()).setUpperBound(System.currentTimeMillis());
				});
			}
			catch (Exception e)
			{
				log.error("Unexpected error updating chart series data", e);
			}
		};
		
		
		
		periodicJobs.scheduleAtFixedRate(updateData, 0, 5, TimeUnit.MINUTES);
		chart.setTitle("Barometric Pressure");
		chart.setLegendVisible(false);
		chart.setMinSize(100,  100);
		return chart;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@SafeVarargs
	private XYChart<GapNumberAxis, GapNumberAxis> createHourChart(int hourCount, boolean line, Series<Long, Double> ... seriesData)
	{
		GapNumberAxis xAxis = new GapNumberAxis();
		xAxis.setTickLabelFormatter(new StringConverter<Number>()
		{
			SimpleDateFormat sdf = new SimpleDateFormat("h a");
			
			@Override
			public String toString(Number object)
			{
				return sdf.format(new Date(object.longValue())).toLowerCase();
			}

			@Override
			public Number fromString(String string)
			{
				throw new UnsupportedOperationException();
			}
		});
		//One Hour
		xAxis.setTickUnit(1*60*60*1000);
		xAxis.setMinorTickCount(2);
		xAxis.setAutoRanging(false);
		xAxis.setUpperBound(System.currentTimeMillis());
		xAxis.setLowerBound(System.currentTimeMillis() - (hourCount*60*60*1000));
		
		GapNumberAxis yAxis = new GapNumberAxis();
		//Animation is buggy on data change
		yAxis.setAnimated(false);
		
		ObservableList<XYChart.Series<Long, Double>> chartData = FXCollections.observableArrayList();
		chartData.addAll(seriesData);
		
		if (line)
		{
			GapLineChart lc = new GapLineChart<>(xAxis, yAxis);
			lc.setCreateSymbols(false);
			lc.setData(chartData);
			lc.lookup(".chart-title").setStyle("-fx-font-size: 1.2em");
			return lc;
		}
		else
		{
			AreaChart sac = new AreaChart<>(xAxis, yAxis);
			sac.setCreateSymbols(false);
			sac.setData(chartData);
			sac.lookup(".chart-title").setStyle("-fx-font-size: 1.2em");
			return sac;
		}
	}
	
	private BarChart<String, Double> createRainTotalsChart(String wllDeviceId, String sensorId) throws SQLException
	{
		BarChart<String, Double> chart = createBarChart();
		WeatherProperty daily = DataFetcher.getInstance().getDataFor(wllDeviceId, sensorId, StoredDataTypes.rainfall_daily);
		WeatherProperty monthly = DataFetcher.getInstance().getDataFor(wllDeviceId, sensorId, StoredDataTypes.rainfall_monthly);
		WeatherProperty yearly = DataFetcher.getInstance().getDataFor(wllDeviceId, sensorId, StoredDataTypes.rainfall_year);
		WeatherProperty sizeAdjust = DataFetcher.getInstance().getDataFor(wllDeviceId, sensorId, StoredDataTypes.rain_size);
		
		if (!sizeAdjust.asString().get().equals("1"))
		{
			log.error("Unsupported rain transformation for {}", sizeAdjust.asString().get());
		}
		
		@SuppressWarnings("unchecked") Runnable updateData = () ->
		{
			try
			{
				log.debug("Updating rain totals chart");
				Series<String, Double> dayRain = new Series<>();
				Series<String, Double> monthRain = new Series<>();
				Series<String, Double> yearRain = new Series<>();
				dayRain.getData().add(new XYChart.Data<>("Day", daily.asDouble().divide(100.0).get()));
				monthRain.getData().add(new XYChart.Data<>("Month", monthly.asDouble().divide(100.0).get()));
				yearRain.getData().add(new XYChart.Data<>("Year", yearly.asDouble().divide(100.0).get()));
				ObservableList<XYChart.Series<String, Double>> chartData = FXCollections.observableArrayList();
				chartData.addAll(dayRain, monthRain, yearRain);
				chart.setData(chartData);
				Tooltip.install(dayRain.getData().get(0).getNode(), 
						new Tooltip(dayRain.getData().get(0).getXValue() + ": " + dayRain.getData().get(0).getYValue().toString() + " in"));
				Tooltip.install(monthRain.getData().get(0).getNode(), 
						new Tooltip(monthRain.getData().get(0).getXValue() + ": " + monthRain.getData().get(0).getYValue().toString() + " in"));
				Tooltip.install(yearRain.getData().get(0).getNode(), 
						new Tooltip(yearRain.getData().get(0).getXValue() + ": " + yearRain.getData().get(0).getYValue().toString() + " in"));
			}
			catch (Exception e)
			{
				log.error("Unexpected error updating chart series data", e);
			}
		};

		updateData.run();
		daily.addListener(change -> updateData.run());
		monthly.addListener(change -> updateData.run());
		yearly.addListener(change -> updateData.run());
		chart.setTitle("Rain Totals");
		chart.lookup(".chart-title").setStyle("-fx-font-size: 1.2em");
		chart.setMinSize(100,  100);
		return chart;
	}
	
	private BarChart<String, Double> createRainCurrentChart(String wllDeviceId, String sensorId) throws SQLException
	{
		BarChart<String, Double> chart = createBarChart();
		
		WeatherProperty fifteenMinData = DataFetcher.getInstance().getDataFor(wllDeviceId, sensorId, StoredDataTypes.rainfall_last_15_min);
		WeatherProperty sixtyMinData = DataFetcher.getInstance().getDataFor(wllDeviceId, sensorId, StoredDataTypes.rainfall_last_60_min);
		WeatherProperty twentyFourHourData = DataFetcher.getInstance().getDataFor(wllDeviceId, sensorId, StoredDataTypes.rainfall_last_24_hr);
		WeatherProperty stormData = DataFetcher.getInstance().getDataFor(wllDeviceId, sensorId, StoredDataTypes.rain_storm);
		WeatherProperty stormStartAt = DataFetcher.getInstance().getDataFor(wllDeviceId, sensorId, StoredDataTypes.rain_storm_start_at);
		WeatherProperty rateData = DataFetcher.getInstance().getDataFor(wllDeviceId, sensorId, StoredDataTypes.rain_rate_last);
		WeatherProperty sizeAdjust = DataFetcher.getInstance().getDataFor(wllDeviceId, sensorId, StoredDataTypes.rain_size);
		if (!sizeAdjust.asString().get().equals("1"))
		{
			log.error("Unsupported rain transformation for {}", sizeAdjust.asString().get());
		}
		
		@SuppressWarnings("unchecked") Runnable updateData = () ->
		{
			try
			{
				log.debug("Updating current rain chart");
				Series<String, Double> fifteenMin = new Series<>();
				Series<String, Double> sixtyMin = new Series<>();
				Series<String, Double> twentyFourHours = new Series<>();
				Series<String, Double> storm = new Series<>();
				Series<String, Double> rate = new Series<>();
				//sets and adds on bar charts are all broken, need to just set the list entirely.
				fifteenMin.getData().add(new XYChart.Data<>("15 Min", fifteenMinData.asDouble().divide(100.0).get()));
				sixtyMin.getData().add(new XYChart.Data<>("60 Min", sixtyMinData.asDouble().divide(100.0).get()));
				twentyFourHours.getData().add(new XYChart.Data<>("24 Hrs", twentyFourHourData.asDouble().divide(100.0).get()));
				storm.getData().add(new XYChart.Data<>("Storm", stormData.asDouble().divide(100.0).get()));
				rate.getData().add(new XYChart.Data<>("Rate", rateData.asDouble().divide(100.0).get()));
				ObservableList<XYChart.Series<String, Double>> chartData = FXCollections.observableArrayList();
				chartData.addAll(fifteenMin, sixtyMin, twentyFourHours, storm, rate);
				chart.setData(chartData);
				Tooltip.install(fifteenMin.getData().get(0).getNode(), 
						new Tooltip("Rain in last " + fifteenMin.getData().get(0).getXValue() + ": " + fifteenMin.getData().get(0).getYValue().toString() + " in"));
				Tooltip.install(sixtyMin.getData().get(0).getNode(), 
						new Tooltip("Rain in last " + sixtyMin.getData().get(0).getXValue() + ": " + sixtyMin.getData().get(0).getYValue().toString() + " in"));
				Tooltip.install(twentyFourHours.getData().get(0).getNode(), 
						new Tooltip("Rain in last " + twentyFourHours.getData().get(0).getXValue() + ": " + twentyFourHours.getData().get(0).getYValue().toString() 
								+ " in"));
				Tooltip.install(storm.getData().get(0).getNode(), 
						new Tooltip("Rain since " + (stormStartAt.isValid().get() ? new Date((long)stormStartAt.asDouble().doubleValue()).toString() :"?") + ": "
								+ storm.getData().get(0).getYValue().toString() + " in"));
				Tooltip.install(rate.getData().get(0).getNode(), 
						new Tooltip("Rain rate: " + rate.getData().get(0).getYValue().toString() + " in per hour"));
			}
			catch (Exception e)
			{
				log.error("Unexpected error updating chart series data", e);
			}
		};
		
		//TODO maybe make each of these just update its series, instead of all, have to see if its a problem
		updateData.run();
		fifteenMinData.addListener(change -> updateData.run());
		sixtyMinData.addListener(change -> updateData.run());
		twentyFourHourData.addListener(change -> updateData.run());
		stormData.addListener(change -> updateData.run());
		rateData.addListener(change -> updateData.run());
		
		chart.setTitle("Current Rain");
		chart.lookup(".chart-title").setStyle("-fx-font-size: 1.2em");
		chart.setMinSize(100,  100);
		return chart;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@SafeVarargs
	private BarChart<String, Double> createBarChart(Series<String, Double> ... seriesData)
	{
		CategoryAxis xAxis = new CategoryAxis();
		NumberAxis yAxis = new NumberAxis();
		//Animation is buggy on data change
		yAxis.setAnimated(false);
		xAxis.setAnimated(false);
		yAxis.setTickLabelFormatter(new StringConverter<Number>()
		{
			DecimalFormat df = new DecimalFormat("#0.00");
			
			@Override
			public String toString(Number object)
			{
				return df.format(object.doubleValue());
			}

			@Override
			public Number fromString(String string)
			{
				throw new UnsupportedOperationException();
			}
		});
		
		ObservableList<XYChart.Series<String, Double>> chartData = FXCollections.observableArrayList();
		chartData.addAll(seriesData);

		BarChart bc = new BarChart<>(xAxis, yAxis);
		bc.setData(chartData);
		bc.setLegendVisible(false);
		bc.setVerticalGridLinesVisible(false);
		bc.lookup(".chart-title").setStyle("-fx-font-size: 1.2em");
		bc.widthProperty().addListener(change -> bc.setBarGap(bc.widthProperty().divide(11).multiply(-1).get()));
		bc.setCategoryGap(0);
		return bc;
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
		try
		{
			periodicJobs.shutdownNow();
			if (dr != null)
			{
				dr.stopReading();
			}
		}
		catch (Exception e)
		{
			log.error("Error during shutdown", e);
		}
	}
}
