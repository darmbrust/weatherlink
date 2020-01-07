package net.sagebits.weatherlink.data;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Hashtable;
import com.fasterxml.jackson.databind.JsonNode;
import net.sagebits.weatherlink.data.live.LiveDataTypes;

/**
 * Maps the json data from wll to an H2 sql database for storage.
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
public enum StoredDataTypes
{
	did("device id", "VARCHAR", "iss"),
	lsid("logical sensor id", "VARCHAR", "iss"),
	ts("timestamp", "BIGINT", "iss"),
	txid("transmitter id", "TINYINT", "iss"),
	temp("temp", "REAL", "most recent valid temperature (°F)", "iss"),
	hum("humidity", "REAL", "most recent valid humidity (%RH)", "iss"),
	dew_point("dew point", "REAL", "most recent valid dew point (°F)", "iss"),
	wet_bulb("web bulb", "REAL", "most recent valid web bulb (°F)", "iss"),
	heat_index("heat index", "REAL", "most recent valid heat index (°F)", "iss"),
	wind_chill("wind chill", "REAL", "most recent valid wind chill (°F)", "iss"),
	thw_index("temp heat wind index", "REAL", "most recent temp heat wind index (°F)", "iss"),
	thsw_index("temp heat sun wind index", "REAL", "most recent valid temp heat sun wind index(°F)", "iss"),
	wind_speed_last("last wind speed", "REAL", "most recent valid wind speed (mph)", LiveDataTypes.wind_speed_last, "iss"),
	wind_dir_last("last wind speed", "REAL", "most recent valid wind direction (°degree)", LiveDataTypes.wind_dir_last,"iss"),
	wind_speed_avg_last_1_min("last wind speed", "REAL", "average wind speed over last 1 min (mph)", "iss"),
	wind_dir_scalar_avg_last_1_min("last wind speed", "REAL", "scalar average wind direction over last 1 min (°degree)", "iss"),
	wind_speed_avg_last_2_min("last wind speed", "REAL", "average wind speed over last 2 min (mph)", "iss"),
	wind_dir_scalar_avg_last_2_min("last wind speed", "REAL", "scalar average wind direction over last 2 min (°degree)", "iss"),
	wind_speed_hi_last_2_min("last wind speed", "REAL", "maximum wind speed over last 2 min (mph)", "iss"),
	wind_dir_at_hi_speed_last_2_min("last wind speed", "REAL", "gust wind direction over last 2 min (°degree)", "iss"),
	wind_speed_avg_last_10_min("last wind speed", "REAL", "average wind speed over last 10 min (mph)", "iss"),
	wind_dir_scalar_avg_last_10_min("last wind speed", "REAL", "scalar average wind direction over last 10 min (°degree)", "iss"),
	wind_speed_hi_last_10_min("last wind speed", "REAL", "maximum wind speed over last 10 min (mph)", LiveDataTypes.wind_speed_hi_last_10_min, "iss"),
	wind_dir_at_hi_speed_last_10_min("last wind speed", "REAL", "gust wind direction over last 10 min (°degree)", LiveDataTypes.wind_dir_at_hi_speed_last_10_min, "iss"),
	rain_size("rain collector type / size", "TINYINT", "0: Reserved, 1: 0.01\", 2: 0.2 mm, 3:  0.1 mm, 4: 0.001\"", LiveDataTypes.rain_size, "iss"),
	rain_rate_last("rain rate", "REAL", "most recent rain rate per hour", LiveDataTypes.rain_rate_last, "iss"),
	rain_rate_hi("rain rate hi last minute", "REAL", "highest rain rate over last 1 minute", "iss"),
	rainfall_last_15_min("rain in last 15 min", "REAL", "total rain over last 15 min", LiveDataTypes.rain_15_min, "iss"),
	rain_rate_hi_last_15_min("rain rate hi last 15 minutes", "REAL", "highest rain rate over last 15 minute ", "iss"),
	rainfall_last_60_min("rain in last hour", "REAL", "total rain for last 60 min", LiveDataTypes.rain_60_min, "iss"),
	rainfall_last_24_hr("rain in last 24 hours", "REAL", "total rain for last 24 hours", LiveDataTypes.rain_24_hr, "iss"),
	rain_storm("rain storm total", "REAL", "total rain since last 24 hour break in rain", LiveDataTypes.rain_storm, "iss"),  //TODO may need to re-document these, I think this is the current storm
	rain_storm_start_at("storm start time", "BIGINT", "current rainstorm start time", LiveDataTypes.rain_storm_start_at, "iss"),
	solar_rad("solar radiation", "REAL", "most recent solar radiation (W/m²)", "iss"),
	uv_index("uv index", "REAL", "most recent UV index", "iss"),
	rx_state("receiver state", "TINYINT", "configured radio receiver state", "iss"),
	trans_battery_flag("transmitter battery status flag", "BOOLEAN", "transmitter battery status flag", "iss"),
	rainfall_daily("rain since midnight", "REAL", "total rain count since local midnight", LiveDataTypes.rainfall_daily, "iss"),
	rainfall_monthly("rain this month", "REAL", "total rain count since first of month at local midnight", LiveDataTypes.rainfall_monthly, "iss"),
	rainfall_year("rain this year", "REAL", "total rain count since first of user-chosen month at local midnight", LiveDataTypes.rainfall_year, "iss"),
	rain_storm_last("previous rain storm", "REAL", "total rain count since last 24 hour long break in rain", "iss"),  //TODO and down here is the previous storm?
	rain_storm_last_start_at("previous storm start time", "BIGINT", "last rainstorm start time", "iss"),
	rain_storm_last_end_at("previous storm end time", "BIGINT", "last rainstorm end time", "iss"),
	
	temp_1("temp 1", "REAL", "most recent valid soil temp slot 1 (°F)", "soil"), 
	temp_2("temp 2", "REAL", "most recent valid soil temp slot 2 (°F)", "soil"),
	temp_3("temp 3", "REAL", "most recent valid soil temp slot 3 (°F)", "soil"),
	temp_4("temp 4", "REAL", "most recent valid soil temp slot 4 (°F)", "soil"),
	most_soil_1("soil moisture 1", "REAL", "most recent valid soil moisture slot 1 (cb)", "soil"),
	most_soil_2("soil moisture 2", "REAL", "most recent valid soil moisture slot 2 (cb)", "soil"),
	most_soil_3("soil moisture 3", "REAL", "most recent valid soil moisture slot 3 (cb)", "soil"),
	most_soil_4("soil moisture 4", "REAL", "most recent valid soil moisture slot 4 (cb)", "soil"),
	wet_leaf_1("leaf wetness 1", "TINYINT", "most recent valid leaf wetness slot 1", "soil"),
	wet_leaf_2("leaf wetness 2", "TINYINT", "most recent valid leaf wetness slot 2", "soil"),
	
	temp_in("temp", "REAL", "most recent valid temperature (°F)", "wll_env"),
	hum_in("humidity", "REAL", "most recent valid humidity (%RH)", "wll_env"),
	dew_point_in("dew point", "REAL", "most recent valid dew point (°F)", "wll_env"),
	heat_index_in("heat index", "REAL", "most recent valid heat index (°F)", "wll_env"),
	
	bar_sea_level("adjusted pressure", "REAL", "most recent bar sensor reading with elevation adjustment (inches)", "wll_bar"),
	bar_trend("barometric trend", "REAL", "current 3 hour bar trent (inches)", "wll_bar"),
	bar_absolute("absolute pressure", "REAL", "raw bar sensor reading (inches)", "wll_bar"),
	;
	
	//private static final Logger log = LogManager.getLogger(StoredDataTypes.class);
	private String displayName;
	private String dataType;
	private String doc;
	private String tableName;
	private LiveDataTypes ldt;

	private static Hashtable<String, StoredDataTypes> lookupHash = new Hashtable<>();

	static {
		for (StoredDataTypes sdt : StoredDataTypes.values())
		{
			lookupHash.put(sdt.name(), sdt);
		}
	}
	
	private StoredDataTypes(String displayName, String dataType, String tableName)
	{
		this(displayName, dataType, displayName, tableName);
	}
	private StoredDataTypes(String displayName, String dataType, String doc, String tableName)
	{
		this(displayName, dataType, doc, null, tableName);
	}
	
	private StoredDataTypes(String displayName, String dataType, String doc, LiveDataTypes ldt, String tableName)
	{
		this.displayName = displayName;
		this.dataType = dataType;
		this.doc = doc;
		this.ldt = ldt;
		this.tableName = tableName;
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
	
	public String getDoc()
	{
		return doc;
	}
	
	public String getTableName()
	{
		return tableName;
	}
	
	/**
	 * Returns null, if no live data type is applicable
	 * @return
	 */
	public LiveDataTypes getLiveDataType()
	{
		return ldt;
	}
	
	public Object intoPreparedStatement(JsonNode data, int parameterIndex, PreparedStatement ps) throws SQLException
	{
		switch (dataType)
		{
			case "VARCHAR" : 
				String s = data.asText();
				ps.setString(parameterIndex, s);
				return s;
			case "BIGINT" : 
				long l = Long.parseLong(data.asText()) * 1000; 
				ps.setLong(parameterIndex, l);
				return l;
			case "TINYINT" : 
				byte b = Byte.parseByte(data.asText());
				ps.setByte(parameterIndex, b);
				return b;
			case "REAL" : 
				float f = Float.parseFloat(data.asText());
				ps.setFloat(parameterIndex, f);
				return f;
			case "BOOLEAN" : 
				boolean bool = data.asBoolean(); 
				ps.setBoolean(parameterIndex, bool);
				return bool;
			
			default:
				throw new RuntimeException("Unhandled type " + dataType);
		}
	}
	
	public boolean isNumeric()
	{
		switch (dataType)
		{
			case "BIGINT" : 
			case "TINYINT" : 
			case "REAL" : 
				return true;
			case "VARCHAR" : 
			case "BOOLEAN" : 
				return false;
				
			default:
				throw new RuntimeException("Unhandled type " + dataType);
		}
	}
	
	
	public static StoredDataTypes match(String name)
	{
		return lookupHash.get(name);
	}
}
