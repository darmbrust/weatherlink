package net.sagebits.weatherlink.data.live;

import java.util.Optional;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.ReadOnlyFloatProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyLongProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;

/**
 * All attributes which are sent "Live" (every 2.5 seconds) by a WeatherLinkLive.  Contains observables for all values.
 * The observables will update as data is available.
 * 
 * {@link ConditionsLive}
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
public class ConditionsLive
{
	private final String lsid;
	private final char data_structure_type;
	private final char txid;
	
	private LongProperty ts = new SimpleLongProperty(0);
	private FloatProperty wind_speed_last = new SimpleFloatProperty(0);
	private FloatProperty wind_dir_last = new SimpleFloatProperty(0);
	private IntegerProperty rain_size = new SimpleIntegerProperty(0);
	private FloatProperty rain_rate_last = new SimpleFloatProperty(0);
	private FloatProperty rain_15_min = new SimpleFloatProperty(0);
	private FloatProperty rain_60_min = new SimpleFloatProperty(0);
	private FloatProperty rain_24_hr = new SimpleFloatProperty(0);
	private FloatProperty rain_storm = new SimpleFloatProperty(0);
	private LongProperty rain_storm_start_at = new SimpleLongProperty(0);
	private FloatProperty rainfall_daily = new SimpleFloatProperty(0);
	private FloatProperty rainfall_monthly = new SimpleFloatProperty(0);
	private FloatProperty rainfall_year = new SimpleFloatProperty(0);
	private FloatProperty wind_speed_hi_last_10_min = new SimpleFloatProperty(0);
	private FloatProperty wind_dir_at_hi_speed_last_10_min = new SimpleFloatProperty(0);
	
	protected ConditionsLive(String lsid, ObjectNode conditionData)
	{
		this.lsid = lsid;
		this.data_structure_type = Optional.ofNullable(conditionData.get("data_structure_type")).orElseThrow().asText().charAt(0);
		this.txid = Optional.ofNullable(conditionData.get("txid")).orElseThrow().asText().charAt(0);
	}
	
	public void update(long timeStamp, ObjectNode conditionData)
	{
		String localLsid = Optional.ofNullable(conditionData.get("lsid")).orElseThrow().asText();
		if (!localLsid.equals(lsid))
		{
			throw new IllegalArgumentException("Wrong lsid value!");
		}
		char localdst = Optional.ofNullable(conditionData.get("data_structure_type")).orElseThrow().asText().charAt(0);
		if (localdst != data_structure_type)
		{
			throw new IllegalArgumentException("Unexpected data_structure_type!");
		}
		char localtxid = Optional.ofNullable(conditionData.get("txid")).orElseThrow().asText().charAt(0);
		if (localtxid != txid)
		{
			throw new IllegalArgumentException("Unexpected txid!");
		}
		
		ts.set(timeStamp);
		wind_speed_last.set(Float.parseFloat(Optional.ofNullable(conditionData.get("wind_speed_last").asText())
				.map(text -> text.equals("null") ? "0" : text).orElse("0")));
		wind_dir_last.set(Float.parseFloat(Optional.ofNullable(conditionData.get("wind_dir_last").asText())
				.map(text -> text.equals("null") ? "-1" : text).orElse("-1")));
		rain_size.set(Integer.parseInt(Optional.ofNullable(conditionData.get("rain_size").asText())
				.map(text -> text.equals("null") ? "0" : text).orElse("0")));
		rain_rate_last.set(Float.parseFloat(Optional.ofNullable(conditionData.get("rain_rate_last").asText())
				.map(text -> text.equals("null") ? "0" : text).orElse("0")));
		rain_15_min.set(Float.parseFloat(Optional.ofNullable(conditionData.get("rain_15_min").asText())
				.map(text -> text.equals("null") ? "0" : text).orElse("0")));
		rain_60_min.set(Float.parseFloat(Optional.ofNullable(conditionData.get("rain_60_min").asText())
				.map(text -> text.equals("null") ? "0" : text).orElse("0")));
		rain_24_hr.set(Float.parseFloat(Optional.ofNullable(conditionData.get("rain_24_hr").asText())
				.map(text -> text.equals("null") ? "0" : text).orElse("0")));
		rain_storm.set(Float.parseFloat(Optional.ofNullable(conditionData.get("rain_storm").asText())
				.map(text -> text.equals("null") ? "0" : text).orElse("0")));
		rain_storm_start_at.set(Long.parseLong(Optional.ofNullable(conditionData.get("rain_storm_start_at").asText())
				.map(text -> text.equals("null") ? "0" : text).orElse("0")) * 1000);
		rainfall_daily.set(Float.parseFloat(Optional.ofNullable(conditionData.get("rainfall_daily").asText())
				.map(text -> text.equals("null") ? "0" : text).orElse("0")));
		rainfall_monthly.set(Float.parseFloat(Optional.ofNullable(conditionData.get("rainfall_monthly").asText())
				.map(text -> text.equals("null") ? "0" : text).orElse("0")));
		rainfall_year.set(Float.parseFloat(Optional.ofNullable(conditionData.get("rainfall_year").asText())
				.map(text -> text.equals("null") ? "0" : text).orElse("0")));
		wind_speed_hi_last_10_min.set(Float.parseFloat(Optional.ofNullable(conditionData.get("wind_speed_hi_last_10_min").asText())
				.map(text -> text.equals("null") ? "0" : text).orElse("0")));
		wind_dir_at_hi_speed_last_10_min.set(Float.parseFloat(Optional.ofNullable(conditionData.get("wind_dir_at_hi_speed_last_10_min").asText())
				.map(text -> text.equals("null") ? "-1" : text).orElse("-1")));
	}

	public String getLsid()
	{
		return lsid;
	}

	public char getData_structure_type()
	{
		return data_structure_type;
	}

	public char getTxid()
	{
		return txid;
	}

	public ReadOnlyLongProperty getTs()
	{
		return ts;
	}

	public ReadOnlyFloatProperty getWind_speed_last()
	{
		return wind_speed_last;
	}

	public ReadOnlyFloatProperty getWind_dir_last()
	{
		return wind_dir_last;
	}

	public ReadOnlyIntegerProperty getRain_size()
	{
		return rain_size;
	}

	public ReadOnlyFloatProperty getRain_rate_last()
	{
		return rain_rate_last;
	}

	public ReadOnlyFloatProperty getRain_15_min()
	{
		return rain_15_min;
	}

	public ReadOnlyFloatProperty getRain_60_min()
	{
		return rain_60_min;
	}

	public ReadOnlyFloatProperty getRain_24_hr()
	{
		return rain_24_hr;
	}

	public ReadOnlyFloatProperty getRain_storm()
	{
		return rain_storm;
	}

	public ReadOnlyLongProperty getRain_storm_start_at()
	{
		return rain_storm_start_at;
	}

	public ReadOnlyFloatProperty getRainfall_daily()
	{
		return rainfall_daily;
	}

	public ReadOnlyFloatProperty getRainfall_monthly()
	{
		return rainfall_monthly;
	}

	public ReadOnlyFloatProperty getRainfall_year()
	{
		return rainfall_year;
	}

	public ReadOnlyFloatProperty getWind_speed_hi_last_10_min()
	{
		return wind_speed_hi_last_10_min;
	}

	public ReadOnlyFloatProperty getWind_dir_at_hi_speed_last_10_min()
	{
		return wind_dir_at_hi_speed_last_10_min;
	}
}
