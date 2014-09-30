package eu.neurovertex.latencymonitor;

import eu.neurovertex.latencymonitor.gui.GUI;
import eu.neurovertex.latencymonitor.gui.LatencyDisplayPanel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Optional;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * @author Neurovertex
 *         Date: 27/05/2014, 16:11
 */
public class Main {
	public static final boolean OPENVPN = true;
	public static void main(String[] args) throws IOException {
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
		TelnetInterface telnet = (OPENVPN) ? new TelnetInterface() : null;
		new GUI(new LatencyDisplayPanel(monitor), Optional.ofNullable(telnet));
		monitor.start();
		if (OPENVPN)
			telnet.start();
		if (!OPENVPN)
			return;
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		String str;
		while ((str = in.readLine()) != null) {
			telnet.inputLine(str);
		}
	}
}
