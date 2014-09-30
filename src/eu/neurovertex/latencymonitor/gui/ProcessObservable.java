package eu.neurovertex.latencymonitor.gui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Observable;

/**
 * @author Neurovertex
 *         Date: 30/09/2014, 01:58
 */
public class ProcessObservable extends Observable {
	private String[] command;
	private Process process;

	public ProcessObservable(String...cmd) {
		command = cmd;
	}

	public void start() {
		try {
			process = Runtime.getRuntime().exec(command);
			Thread tout = new Thread(streamReader(process.getInputStream())), terr = new Thread(streamReader(process.getErrorStream()));
			tout.setDaemon(true);
			terr.setDaemon(true);
			tout.start();
			terr.start();
		} catch (IOException ignore) {}
	}

	public void waitProcess() {
		try {
			process.waitFor();
		} catch (InterruptedException ignore) {}
	}

	private Runnable streamReader(InputStream input) {
		return () -> {
			try {
				try (BufferedReader in = new BufferedReader(new InputStreamReader(input))) {
					String line;
					while ((line = in.readLine()) != null) {
						setChanged();
						notifyObservers(line);
					}
				}
			} catch (IOException ignore) {}
		};
	}
}
