package net.sagebits.weatherlink.data.live;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Listens to the Live Data updates broadcast over UDP
 * {@link LiveDataListener}
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
public class LiveDataListener
{
	final JsonFactory factory = new JsonFactory();
	final ObjectMapper mapper = new ObjectMapper(factory);
	private final Logger log = LogManager.getLogger();
	boolean enable = true;

	public LiveDataListener(int port) throws SocketException
	{
		final DatagramSocket datagramSocket = new DatagramSocket(port);
		final byte[] buffer = new byte[1024];
		final DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
		
		Thread t = new Thread(() -> {
			log.info("Live data listen thread starts");
			while (enable)
			{
				try
				{
					datagramSocket.receive(packet);
					String liveData = new String(buffer, 0, packet.getLength(), StandardCharsets.UTF_8);
					log.debug("Live Data: {}", liveData);
					JsonNode liveRootNode = mapper.readTree(liveData);
					LiveData.getInstance().update(liveRootNode);
				}
				catch (IOException e)
				{
					//IO probably means our socket it somehow messed up, and not recoverable. 
					//The hourly check thread will restart a new socket when it checks.
					log.error("Error reading live data", e);
					enable = false;
				}
				catch (Exception e)
				{
					log.error("Error parsing / reading live data", e);
				}
			}
			
			datagramSocket.close();
			log.info("Live data listen thread ends");
			
		}, "LiveDataListen");
		
		t.start();
		
	}

	public void stop()
	{
		enable = false;
	}
	
	public boolean running()
	{
		return enable;
	}

}
