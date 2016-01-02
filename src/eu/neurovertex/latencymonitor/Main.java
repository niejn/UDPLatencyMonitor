package eu.neurovertex.latencymonitor;

import eu.neurovertex.latencymonitor.gui.GUI;
import eu.neurovertex.latencymonitor.gui.LatencyDisplayPanel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Observable;
import java.util.Observer;
import java.util.Optional;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * @author Neurovertex
 *         Date: 27/05/2014, 16:11
 */
public class Main {
	public static void main(String[] args) throws IOException, InterruptedException {
		Preferences prefs = Preferences.userNodeForPackage(Main.class);
		String host;
		int port, delay;
		if (args.length > 0) {
			host = args[0];
			prefs.put("hostname", host);
		} else {
			host = prefs.get("hostname", "localhost");
		}
		if (args.length > 1) {
			port = Integer.parseInt(args[1]);
			prefs.putInt("port", port);
		} else {
			port = prefs.getInt("port", 7);
		}
		if (args.length > 2) {
			delay = Integer.parseInt(args[2]);
			prefs.putInt("delay", delay);
		} else {
			delay = prefs.getInt("delay", 100);
		}
		try {
			prefs.flush();
		} catch (BackingStoreException e) {
			System.err.println("Could not save preferences");
			e.printStackTrace();
		}
		System.out.printf("Parameters : %s:%d, %dms delay%n", host, port, delay);
		Monitor monitor = new Monitor(host, port, delay);
		Optional<TelnetInterface> opTelnet;
		{
			TelnetInterface telnet = new TelnetInterface();
			final Observer waiter = new Observer(){
				@Override
				public synchronized void update(Observable o, Object arg) {
					notifyAll();
				}
			};
			telnet.addObserver(waiter);
			telnet.start();
			while (telnet.isTrying() && !telnet.isConnected())
				synchronized (waiter) {
					waiter.wait();
				}
			opTelnet = telnet.isConnected() ? Optional.of(telnet) : Optional.empty();
			System.out.println(opTelnet.isPresent() ? "Connected to Telnet" : "Couldn't connect to Telnet");

			monitor.start();
			new GUI(new LatencyDisplayPanel(monitor), opTelnet);
		}
		if (opTelnet.isPresent()) {
			BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
			String str;
			while ((str = in.readLine()) != null)
				opTelnet.get().inputLine(str);
		}
	}
}
