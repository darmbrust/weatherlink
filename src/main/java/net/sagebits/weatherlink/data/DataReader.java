package net.sagebits.weatherlink.data;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javafx.application.Platform;
import javafx.beans.property.LongProperty;
import javafx.beans.property.ReadOnlyLongProperty;
import javafx.beans.property.SimpleLongProperty;
import net.sagebits.weatherlink.data.live.LiveDataListener;
import net.sagebits.weatherlink.data.periodic.PeriodicData;
import net.straylightlabs.hola.dns.Domain;
import net.straylightlabs.hola.sd.Instance;
import net.straylightlabs.hola.sd.Query;
import net.straylightlabs.hola.sd.Service;

/**
 * The class that manages the background threads for doing data reading, both periodic full reads, and live reads
 * over UDP.
 * 
 * {@link DataReader}
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
public class DataReader
{
	private final Logger log = LogManager.getLogger(DataReader.class);
	private final String address;
	private final int port;
	
	private ScheduledExecutorService timed;
	private ScheduledFuture<?> periodicTask;
	private LiveDataListener ldl;
	private int pollInterval;
	
	private final JsonFactory factory = new JsonFactory();
	private final ObjectMapper mapper = new ObjectMapper(factory);
	
	private final LongProperty lastReadAttemptTime = new SimpleLongProperty(System.currentTimeMillis());

	/**
	 * @param wllAddress The IP address of the WeatherLinkLive. If not provided, attempts to auto-discover. This constructor will fail if it cannot be
	 * auto-discovered. Will also fail if the provided address doesn't respond.
	 * @throws IOException, IllegalArgumentException 
	 */
	public DataReader(Optional<String> wllAddress) throws IOException
	{
		if (wllAddress.isEmpty())
		{
			try
			{
				log.info("Autodiscovering WeatherLinkLive");
				Service service = Service.fromName("_weatherlinklive._tcp");
				Query query = Query.createFor(service, Domain.LOCAL);
				Set<Instance> instances = query.runOnce();
				if (instances.size() == 0)
				{
					throw new IOException("Couldn't locate any instance of a WeatherLinkLive.  Please set the IP address manually.");
				}
				else if (instances.size() > 1)
				{
					throw new IOException("Autodetect found more than one WeatherLinkLive.  Please set the IP address manually.");
				}
				else
				{
					Instance instance = instances.iterator().next(); 
					address = instance.getAddresses().iterator().next().getHostAddress();
					port = instance.getPort();
				}
			}
			catch (IOException  e)
			{
				log.error("Error during autodiscover", e);
				throw new IOException("Autodiscover failed.  Plese set the IP address manually");
			}
		}
		else
		{
			address = wllAddress.get();
			port = 80;
		}
		
		log.info("testing WLL address: {}:{}", address, port);
		log.debug("test success - received: {}", readBytes(new URL("http://" + address + ":" + port + "/v1/current_conditions")));
	}

	/**
	 * @param pollInterval - time (in seconds) in between polls for full data sets. Docs recommend no more often than once every 10 seconds.
	 * @param readLive - if true, enable the 'live data' function of WLL, which sends certain data every 2.5 seconds.
	 */
	public void startReading(int pollInterval, boolean readLive)
	{
		this.pollInterval = pollInterval;
		timed = Executors.newSingleThreadScheduledExecutor(new ThreadFactory()
		{
			@Override
			public Thread newThread(Runnable r)
			{
				return new Thread(r, "Scheduled Data Requests");
			}
		});
		
		//Poke WLL once and hour, and ask it to continue the "live" data for at least the next 3 hours.
		if (readLive)
		{
			try
			{
				ldl = new LiveDataListener(22222);
				timed.scheduleAtFixedRate(() -> 
				{
					try
					{
						log.debug("Requesting Live Data Stream");
						String response = readBytes(new URL("http://" + address + ":" + port + "/v1/real_time?duration=10800"));
						log.trace("Live request response: {}", response);
						
						JsonNode rootNode = mapper.readTree(response);
						if (!"null".equals(rootNode.get("error").asText()))
						{
							log.error("Error reported in broadcast request {}", rootNode.get("error").asText());
						}
						ObjectNode on = (ObjectNode)rootNode.get("data");
						if (!on.get("broadcast_port").asText().equals("22222"))
						{
							log.error("WLL reports unexpected broadcast port!");
						}
						if (on.get("duration").asInt() < 10800)
						{
							log.error("WLL reports smaller than expected duration");
						}
						
						if (!ldl.running())
						{
							log.info("Live Data Reader appears to have failed.  Restarting");
							ldl = new LiveDataListener(22222);
						}
					}
					catch (Exception e)
					{
						log.error("Request for live data failed:" + e);
					}
					
				}, 0, 1 ,TimeUnit.HOURS);
			}
			catch (SocketException e)
			{
				log.error("Problem setting up for live data", e);
			}
		}
		periodicTask = timed.scheduleAtFixedRate(() -> readData(), 1, this.pollInterval,TimeUnit.SECONDS);
	}
	
	
	private void readData()
	{
		try
		{
			Platform.runLater(() -> lastReadAttemptTime.set(System.currentTimeMillis()));
			String data = readBytes(new URL("http://" + address + ":" + port + "/v1/current_conditions"));
			log.trace("Periodic Data: {}", data);
			
			JsonNode rootNode = mapper.readTree(data);
			if (!"null".equals(rootNode.get("error").asText()))
			{
				log.error("Error reported in periodic request {}", rootNode.get("error").asText());
			}
			
			PeriodicData.getInstance().append(rootNode.get("data"));
			log.debug("Periodic data updated");
		}
		catch (Exception e)
		{
			//Don't need a stack trace here
			log.warn("Error during periodic data read, delaying and rescheduling: {}", e.toString());
			//Its probably busy.  Lets sleep for a bit, and give it time to recover.
			//Will do this by canceling our current task, and rescheduling after a delay.
			ScheduledExecutorService localRef = timed;
			if (localRef != null)  //If null, shutdown requested
			{
				periodicTask.cancel(false);
				periodicTask = localRef.scheduleAtFixedRate(() -> readData(), 11, pollInterval, TimeUnit.SECONDS);
			}
		}
	}

	public void stopReading()
	{
		if (ldl != null)
		{
			ldl.stop();
			ldl = null;
		}
		ScheduledExecutorService localRef = timed;
		if (localRef != null)
		{
			timed = null;
			localRef.shutdownNow();
			try
			{
				localRef.awaitTermination(1, TimeUnit.SECONDS);
			}
			catch (InterruptedException e)
			{
				// don't care
			}
		}
		try
		{
			PeriodicData.getInstance().shutDown();
		}
		catch (SQLException e)
		{
			log.error("Error closing DB", e);
		}
	}
	
	public ReadOnlyLongProperty getLastReadAttemptTime()
	{
		return lastReadAttemptTime;
	}
	
	public static String readBytes(URL url) throws IOException
	{
		InputStream is = null;
		try
		{
			URLConnection con = url.openConnection();
			con.setReadTimeout(2000);
			con.setConnectTimeout(1000);
			con.connect();
			is = con.getInputStream(); 
			return new String(is.readAllBytes(), StandardCharsets.UTF_8);
		}
		finally
		{
			if (is != null)
			{
				is.close();
			}
		}
	}
	
	public static void main(String[] args) throws IOException, InterruptedException, SQLException
	{
		DataReader dr = new DataReader(Optional.empty());
		dr.startReading(10, true);
		Thread.sleep(8 * 60 * 60 * 1000);
		dr.stopReading();
		PeriodicData.getInstance().shutDown();
	}
}
