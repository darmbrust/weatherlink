package net.sagebits.weatherlink;

import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Iterator;
import java.util.Map.Entry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class Testing
{
	private static Logger log = LogManager.getLogger();

	public static void main(String[] args) throws IOException
	{
		System.out.println("Connecting...");

		InputStream is = new URL("http://192.168.10.32:80/v1/current_conditions").openStream();

		JsonFactory factory = new JsonFactory();

		ObjectMapper mapper = new ObjectMapper(factory);
		JsonNode rootNode = mapper.readTree(is);

		//Doc per https://weatherlink.github.io/weatherlink-live-local-api/
		//		"data":
		//		{
		//		    "did":"001D0A700002",
		//		    "ts":1531754005,
		//		    "conditions": [
		//		    {
		//		            "lsid":48308,                                  // logical sensor ID **(no unit)**
		//		            "data_structure_type":1,                       // data structure type **(no unit)**
		//		            "txid":1,                                      // transmitter ID **(no unit)**
		//		            "temp": 62.7,                                  // most recent valid temperature **(°F)**
		//		            "hum":1.1,                                     // most recent valid humidity **(%RH)**
		//		            "dew_point": -0.3,                             // **(°F)**
		//		            "wet_bulb":null,                               // **(°F)**
		//		            "heat_index": 5.5,                             // **(°F)**
		//		            "wind_chill": 6.0,                             // **(°F)**
		//		            "thw_index": 5.5,                              // **(°F)**
		//		            "thsw_index": 5.5,                             // **(°F)**
		//		            "wind_speed_last":2,                           // most recent valid wind speed **(mph)**
		//		            "wind_dir_last":null,                          // most recent valid wind direction **(°degree)**
		//		            "wind_speed_avg_last_1_min":4                  // average wind speed over last 1 min **(mph)**
		//		            "wind_dir_scalar_avg_last_1_min":15            // scalar average wind direction over last 1 min **(°degree)**
		//		            "wind_speed_avg_last_2_min":42606,             // average wind speed over last 2 min **(mph)**
		//		            "wind_dir_scalar_avg_last_2_min": 170.7,       // scalar average wind direction over last 2 min **(°degree)**
		//		            "wind_speed_hi_last_2_min":8,                  // maximum wind speed over last 2 min **(mph)**
		//		            "wind_dir_at_hi_speed_last_2_min":0.0,         // gust wind direction over last 2 min **(°degree)**
		//		            "wind_speed_avg_last_10_min":42606,            // average wind speed over last 10 min **(mph)**
		//		            "wind_dir_scalar_avg_last_10_min": 4822.5,     // scalar average wind direction over last 10 min **(°degree)**
		//		            "wind_speed_hi_last_10_min":8,                 // maximum wind speed over last 10 min **(mph)**
		//		            "wind_dir_at_hi_speed_last_10_min":0.0,        // gust wind direction over last 10 min **(°degree)**
		//		            "rain_size":2,                                 // rain collector type/size **(0: Reserved, 1: 0.01", 2: 0.2 mm, 3:  0.1 mm, 4: 0.001")**
		//		            "rain_rate_last":0,                            // most recent valid rain rate **(counts/hour)**
		//		            "rain_rate_hi":null,                           // highest rain rate over last 1 min **(counts/hour)**
		//		            "rainfall_last_15_min":null,                   // total rain count over last 15 min **(counts)**
		//		            "rain_rate_hi_last_15_min":0,                  // highest rain rate over last 15 min **(counts/hour)**
		//		            "rainfall_last_60_min":null,                   // total rain count for last 60 min **(counts)**
		//		            "rainfall_last_24_hr":null,                    // total rain count for last 24 hours **(counts)**
		//		            "rain_storm":null,                             // total rain count since last 24 hour long break in rain **(counts)**
		//		            "rain_storm_start_at":null,                    // UNIX timestamp of current rain storm start **(seconds)**
		//		            "solar_rad":747,                               // most recent solar radiation **(W/m²)**
		//		            "uv_index":5.5,                                // most recent UV index **(Index)**
		//		            "rx_state":2,                                  // configured radio receiver state **(no unit)**
		//		            "trans_battery_flag":0,                        // transmitter battery status flag **(no unit)**
		//		            "rainfall_daily":63,                           // total rain count since local midnight **(counts)**
		//		            "rainfall_monthly":63,                         // total rain count since first of month at local midnight **(counts)**
		//		            "rainfall_year":63,                            // total rain count since first of user-chosen month at local midnight **(counts)**
		//		            "rain_storm_last":null,                        // total rain count since last 24 hour long break in rain **(counts)**
		//		            "rain_storm_last_start_at":null,               // UNIX timestamp of last rain storm start **(sec)**
		//		            "rain_storm_last_end_at":null                  // UNIX timestamp of last rain storm end **(sec)**
		//		    },
		//		    {
		//		            "lsid":3187671188,
		//		            "data_structure_type":2,
		//		            "txid":3,
		//		            "temp_1":null,                                 // most recent valid soil temp slot 1 **(°F)**
		//		            "temp_2":null,                                 // most recent valid soil temp slot 2 **(°F)**
		//		            "temp_3":null,                                 // most recent valid soil temp slot 3 **(°F)**
		//		            "temp_4":null,                                 // most recent valid soil temp slot 4 **(°F)**
		//		            "moist_soil_1":null,                           // most recent valid soil moisture slot 1 **(|cb|)**
		//		            "moist_soil_2":null,                           // most recent valid soil moisture slot 2 **(|cb|)**
		//		            "moist_soil_3":null,                           // most recent valid soil moisture slot 3 **(|cb|)**
		//		            "moist_soil_4":null,                           // most recent valid soil moisture slot 4 **(|cb|)**
		//		            "wet_leaf_1":null,                             // most recent valid leaf wetness slot 1 **(no unit)**
		//		            "wet_leaf_2":null,                             // most recent valid leaf wetness slot 2 **(no unit)**
		//		            "rx_state":null,                               // configured radio receiver state **(no unit)**
		//		            "trans_battery_flag":null                      // transmitter battery status flag **(no unit)**
		//		    },
		//		    {
		//		            "lsid":48307,
		//		            "data_structure_type":4,
		//		            "temp_in":78.0,                                // most recent valid inside temp **(°F)**
		//		            "hum_in":41.1,                                 // most recent valid inside humidity **(%RH)**
		//		            "dew_point_in":7.8,                            // **(°F)**
		//		            "heat_index_in":8.4                            // **(°F)**
		//		    },
		//		    {
		//		            "lsid":48306,
		//		            "data_structure_type":3,
		//		            "bar_sea_level":30.008,                       // most recent bar sensor reading with elevation adjustment **(inches)**
		//		            "bar_trend": null,                            // current 3 hour bar trend **(inches)**
		//		            "bar_absolute":30.008                         // raw bar sensor reading **(inches)**
		//		    }]
		//		},
		//		"error":null }
		
		
		//Live Data:
		//		 "did":"001D0A700002",
		//		    "ts":1532031640,
		//		    "conditions": [
		//		    {
		//		            "lsid":3187671188,                           // logical sensor ID **(no unit)**
		//		            "data_structure_type":1,                     // data structure type **(no unit)**
		//		            "txid":1,                                    // transmitter ID **(no unit)**
		//		            "wind_speed_last":0.08,                      // most recent wind speed **(mph)**
		//		            "wind_dir_last":26.7,                        // most recent wind direction **(°degree)**
		//		            "rain_size":2,                               // rain collector size/type **(0: Reserved, 1: 0.01", 2: 0.2 mm, 3: 0.1 mm, 4: 0.001")**
		//		            "rain_rate_last":0,                          // most recent rain rate **(count/hour)**
		//		            "rain_15_min":0,                             // total rain count over last 15 min **(counts)**
		//		            "rain_60_min":0,                             // total rain count over last 60 min **(counts)**
		//		            "rain_24_hr":0,                              // total rain count over last 24 hours **(counts)**
		//		            "rain_storm":0,                              // total rain count since last 24 hour long break in rain **(counts)**
		//		            "rain_storm_start_at":1553187540,            // UNIX timestamp of current rain storm start **(seconds)**
		//		            "rainfall_daily":63,                         // total rain count since local midnight **(counts)**
		//		            "rainfall_monthly":63,                       // total rain count since first of the month at local midnight **(counts)**
		//		            "rainfall_year":63,                          // total rain count since first of the user chosen month at local midnight **(counts)** 
		//		            "wind_speed_hi_last_10_min":null,            // maximum wind speed over last 10 min **(mph)**
		//		            "wind_dir_at_hi_speed_last_10_min":null      // gust wind direction over last 10 min **(°degree)**
		//		    },

		//root Node should contain "data" and "error"
		JsonNode jn = rootNode.get("error");
		if (!jn.isNull())
		{
			log.error("Error reported in API call: {}", jn.toString());
		}

		handleData(rootNode.get("data"));

		System.out.println("--------------");
		System.out.println("Poke for realtime");

		//ask it to continue for the next 20 minutes
		System.out.println(new String(new URL("http://192.168.10.32:80/v1/real_time?duration=1200").openStream().readAllBytes(), StandardCharsets.UTF_8));

		DatagramSocket datagramSocket = new DatagramSocket(22222);

		byte[] buffer = new byte[1024];
		DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
		
		for (int i = 0; i < 3; i++)
		{
			datagramSocket.receive(packet);
			System.out.println("receive:" + new String(buffer, StandardCharsets.UTF_8));
			JsonNode liveRootNode = mapper.readTree(new String(buffer, StandardCharsets.UTF_8));
			handleData(liveRootNode);
			
		}
		
		
		
//		DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length, InetAddress.getByName("0.0.0.0"), 22222);
//		socket.receive(receivePacket);
//		System.out.println("receive:" + receivePacket.toString());
//		socket.close();
	}
	
	

	private static void handleData(JsonNode jsonNode)
	{
		//contains did (device id), ts (timestamp) conditions.  Timestamp appears to be number of seconds from epoc in UTC
		System.out.println("did: " + jsonNode.get("did"));
		System.out.println("ts: " + jsonNode.get("ts") + " " + new Date(Long.parseLong(jsonNode.get("ts").asText()) * 1000).toString());
		handleConditions((ArrayNode) jsonNode.get("conditions"));

	}

	private static void handleConditions(ArrayNode an)
	{
		Iterator<JsonNode> it = an.elements();
		while (it.hasNext())
		{
			ObjectNode on = (ObjectNode) it.next();
			String dst = on.get("data_structure_type").asText();
			if (dst.equals("1"))
			{
				handleISS(on);
			}
			else if (dst.equals("2"))
			{
				handleSoil(on);
			}
			else if (dst.equals("3"))
			{
				handleWLBar(on);
			}
			else if (dst.equals("4"))
			{
				handleWlTemp(on);
			}
			else
			{
				log.error("Unexpected data_structure_type {}", dst);
			}
		}
	}

	private static void handleWlTemp(ObjectNode on)
	{
		System.out.println("indoor temp");
		Iterator<Entry<String, JsonNode>> f = on.fields();
		while (f.hasNext())
		{
			Entry<String, JsonNode> e = f.next();
			System.out.println(e.getKey() + " " + e.getValue());
		}
	}

	private static void handleWLBar(ObjectNode on)
	{
		System.out.println("indoor bar");
		Iterator<Entry<String, JsonNode>> f = on.fields();
		while (f.hasNext())
		{
			Entry<String, JsonNode> e = f.next();
			System.out.println(e.getKey() + " " + e.getValue());
		}

	}

	private static void handleSoil(ObjectNode on)
	{
		System.out.println("soil");
		Iterator<Entry<String, JsonNode>> f = on.fields();
		while (f.hasNext())
		{
			Entry<String, JsonNode> e = f.next();
			System.out.println(e.getKey() + " " + e.getValue());
		}

	}

	private static void handleISS(ObjectNode on)
	{
		System.out.println("outdoor info");
		Iterator<Entry<String, JsonNode>> f = on.fields();
		while (f.hasNext())
		{
			Entry<String, JsonNode> e = f.next();
			System.out.println(e.getKey() + " " + e.getValue());
		}

	}
}
