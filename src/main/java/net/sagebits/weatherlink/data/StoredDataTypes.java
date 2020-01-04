package net.sagebits.weatherlink.data;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Hashtable;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Maps the json data from wll to an H2 sql database for storage.
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
public enum StoredDataTypes
{
	
	did("device id", "VARCHAR"),
	ts("BIGINT", "BIGINT"),
	lsid("logical sensor id", "VARCHAR"),
	txid("transmitter id", "TINYINT"),
	temp("temp", "REAL", "most recent valid temperature (°F)"),
	hum("humidity", "REAL", "most recent valid humidity (%RH)"),
	dew_point("dew point", "REAL", "most recent valid dew point (°F)"),
	wet_bulb("web bulb", "REAL", "most recent valid web bulb (°F)"),
	heat_index("heat index", "REAL", "most recent valid heat index (°F)"),
	wind_chill("wind chill", "REAL", "most recent valid wind chill (°F)"),
	thw_index("temp heat wind index", "REAL", "most recent temp heat wind index (°F)"),
	thsw_index("temp heat sun wind index", "REAL", "most recent valid temp heat sun wind index(°F)"),
	wind_speed_last("last wind speed", "REAL", "most recent valid wind speed (mph)"),
	wind_dir_last("last wind speed", "REAL", "most recent valid wind direction (°degree)"),
	wind_speed_avg_last_1_min("last wind speed", "REAL", "average wind speed over last 1 min (mph)"),
	wind_dir_scalar_avg_last_1_min("last wind speed", "REAL", "scalar average wind direction over last 1 min (°degree)"),
	wind_speed_avg_last_2_min("last wind speed", "REAL", "average wind speed over last 2 min (mph)"),
	wind_dir_scalar_avg_last_2_min("last wind speed", "REAL", "scalar average wind direction over last 2 min (°degree)"),
	wind_speed_hi_last_2_min("last wind speed", "REAL", "maximum wind speed over last 2 min (mph)"),
	wind_dir_at_hi_speed_last_2_min("last wind speed", "REAL", "gust wind direction over last 2 min (°degree)"),
	wind_speed_avg_last_10_min("last wind speed", "REAL", "average wind speed over last 10 min (mph)"),
	wind_dir_scalar_avg_last_10_min("last wind speed", "REAL", "scalar average wind direction over last 10 min (°degree)"),
	wind_speed_hi_last_10_min("last wind speed", "REAL", "maximum wind speed over last 10 min (mph)"),
	wind_dir_at_hi_speed_last_10_min("last wind speed", "REAL", "gust wind direction over last 10 min (°degree)"),
	rain_size("rain collector type / size", "TINYINT", "0: Reserved, 1: 0.01\", 2: 0.2 mm, 3:  0.1 mm, 4: 0.001\""),
	rain_rate_last("rain rate", "REAL", "most recent rain rate per hour"),
	rain_rate_hi("rain rate hi last minute", "REAL", "highest rain rate over last 1 minute"),
	rainfall_last_15_min("rain in last 15 min", "REAL", "total rain over last 15 min"),
	rain_rate_hi_last_15_min("rain rate hi last 15 minutes", "REAL", "highest rain rate over last 15 minute "),
	rainfall_last_60_min("rain in last hour", "REAL", "total rain for last 60 min"),
	rainfall_last_24_hr("rain in last 24 hours", "REAL", "total rain for last 24 hours"),
	rain_storm("rain storm total", "REAL", "total rain since last 24 hour break in rain"),  //TODO may need to re-document these, I think this is the current storm
	rain_storm_start_at("storm start time", "BIGINT", "current rainstorm start time"),
	solar_rad("solar radiation", "REAL", "most recent solar radiation (W/m²)"),
	uv_index("uv index", "REAL", "most recent UV index"),
	rx_state("receiver state", "TINYINT", "configured radio receiver state"),
	trans_battery_flag("transmitter battery status flag", "BOOLEAN", "transmitter battery status flag"),
	rainfall_daily("rain since midnight", "REAL", "total rain count since local midnight"),
	rainfall_monthly("rain this month", "REAL", "total rain count since first of month at local midnight"),
	rainfall_year("rain this year", "REAL", "total rain count since first of user-chosen month at local midnight"),
	rain_storm_last("previous rain storm", "REAL", "total rain count since last 24 hour long break in rain"),  //TODO and down here is the previous storm?
	rain_storm_last_start_at("previous storm start time", "BIGINT", "last rainstorm start time"),
	rain_storm_last_end_at("previous storm end time", "BIGINT", "last rainstorm end time"),
	
	temp_1("temp 1", "REAL", "most recent valid soil temp slot 1 (°F)"), 
	temp_2("temp 2", "REAL", "most recent valid soil temp slot 2 (°F)"),
	temp_3("temp 3", "REAL", "most recent valid soil temp slot 3 (°F)"),
	temp_4("temp 4", "REAL", "most recent valid soil temp slot 4 (°F)"),
	most_soil_1("soil moisture 1", "REAL", "most recent valid soil moisture slot 1 (cb)"),
	most_soil_2("soil moisture 2", "REAL", "most recent valid soil moisture slot 2 (cb)"),
	most_soil_3("soil moisture 3", "REAL", "most recent valid soil moisture slot 3 (cb)"),
	most_soil_4("soil moisture 4", "REAL", "most recent valid soil moisture slot 4 (cb)"),
	wet_leaf_1("leaf wetness 1", "TINYINT", "most recent valid leaf wetness slot 1"),
	wet_leaf_2("leaf wetness 2", "TINYINT", "most recent valid leaf wetness slot 2"),
	
	temp_in("temp", "REAL", "most recent valid temperature (°F)"),
	hum_in("humidity", "REAL", "most recent valid humidity (%RH)"),
	dew_point_in("dew point", "REAL", "most recent valid dew point (°F)"),
	heat_index_in("heat index", "REAL", "most recent valid heat index (°F)"),
	
	bar_sea_level("adjusted pressure", "REAL", "most recent bar sensor reading with elevation adjustment (inches)"),
	bar_trend("barometric trend", "REAL", "current 3 hour bar trent (inches)"),
	bar_absolute("absolute pressure", "REAL", "raw bar sensor reading (inches)"),
	;
	
	private String displayName;
	private String dataType;
	private static Hashtable<String, StoredDataTypes> lookupHash = new Hashtable<>();

	static {
		for (StoredDataTypes sdt : StoredDataTypes.values())
		{
			lookupHash.put(sdt.name(), sdt);
		}
	}
	
	private StoredDataTypes(String displayName, String dataType)
	{
		this(displayName, dataType, displayName);
	}
	
	private StoredDataTypes(String displayName, String dataType, String doc)
	{
		this.displayName = displayName;
		this.dataType = dataType;
	}
	
	public String colCreate(boolean noMore)
	{
		return colCreate(noMore, true);
	}
	
	public String colCreate(boolean noMore, boolean allowNull)
	{
		return name() + " " + dataType + (allowNull ? "" : " NOT NULL") + (noMore ? "" : ", " );
	}
	
	public String colCreate()
	{
		return colCreate(false);
	}
	
	public String getDisplayName()
	{
		return displayName;
	}
	
	public void intoPreparedStatement(JsonNode data, int parameterIndex, PreparedStatement ps) throws SQLException
	{
		switch (dataType)
		{
			case "VARCHAR" : ps.setString(parameterIndex, data.asText());
				break;
			case "BIGINT" : ps.setLong(parameterIndex, Long.parseLong(data.asText()) * 1000);
				break;
			case "TINYINT" : ps.setByte(parameterIndex, Byte.parseByte(data.asText()));
				break;
			case "REAL" : ps.setFloat(parameterIndex, Float.parseFloat(data.asText()));
				break;
			case "BOOLEAN" : ps.setBoolean(parameterIndex, data.asBoolean());
				break;
			
			default:
				throw new RuntimeException("Unhandled type " + dataType);
		}
	}
	
	public static StoredDataTypes match(String name)
	{
		return lookupHash.get(name);
	}
}
