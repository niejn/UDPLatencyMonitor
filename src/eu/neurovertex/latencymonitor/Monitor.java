package eu.neurovertex.latencymonitor;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Observable;
import java.util.TimerTask;

/**
 * @author Neurovertex
 *         Date: 27/05/2014, 16:12
 */
public class Monitor extends Observable implements Runnable {
    private final DatagramSocket socket;
    private final InetAddress addr;
    private final int delay;
    private final LatencyRingBuffer buffer;
    private final int port;
    public long startTime;
    private Thread thread;
    private volatile long id = 0;
    private volatile boolean stop = false;
    private long lastUpdate = System.currentTimeMillis();

    public Monitor(InetAddress addr, int port, int delay) throws IOException {
        this.delay = delay;
        this.addr = addr;
        this.port = port;
        socket = new DatagramSocket();
        System.out.println("Receive buffer : " + socket.getReceiveBufferSize());
        buffer = new LatencyRingBuffer(1024);
    }

    public Monitor(byte[] addr, int port, int delay) throws IOException {
        this(InetAddress.getByAddress(addr), port, delay);
    }

    public Monitor(String hostname, int port, int delay) throws IOException {
        this(InetAddress.getByName(hostname), port, delay);
    }

    public long getPingTime(long id) {
        return startTime + id * delay;
    }

    public long getLatency(long id) {
        return Math.max(1L, System.currentTimeMillis() - getPingTime(id));
    }

    public synchronized void start() {
        thread = new Thread(this);
        thread.setDaemon(true);
        thread.start();
        new Thread(new PingThread()).start();
    }

    public synchronized void stopMonitor() {
        thread.interrupt();
    }

    @Override
    public void run() {
        try {
            ByteBuffer buff = ByteBuffer.allocate(256);
            DatagramPacket packet = new DatagramPacket(buff.array(), 256);
            while (!stop) {
                buff.rewind();
                socket.receive(packet);
                long i = buff.getLong(0);
                if (i <= id) { // True unless a reset just happened - see PingThread#run
                    long lat = getLatency(i);
                    buffer.set(i, lat);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public LatencyRingBuffer getBuffer() {
        return buffer;
    }

    @Override
    public synchronized void notifyObservers() {
        if (System.currentTimeMillis() - lastUpdate > 16) { // as to avoid spamming repaint()
            setChanged();
            super.notifyObservers();
            lastUpdate = System.currentTimeMillis();
        }
    }

    private class PingThread extends TimerTask {
        @Override
        public void run() {
            ByteBuffer buff = ByteBuffer.allocate(8).putLong(0L);
            DatagramPacket packet = new DatagramPacket(buff.array(), 8, addr, port);
            long time;
            time = startTime + id * delay;
            startTime = System.currentTimeMillis();
            while (!stop) {
                try {
                    buff.rewind();
                    buff.putLong(id++);
                    time = startTime + id * delay;
                    buffer.add();
                    try {
                        socket.send(packet);
                    } catch (SocketException ignore) {
                    }
                    notifyObservers();
                    if (System.currentTimeMillis() - time > 5 * delay) {
                        System.out.println("Huge lag detected - Assuming computer was put to sleep/hibernation");
                        buffer.reset();
                        time = System.currentTimeMillis() + delay;
                        startTime = time;
                        id = 0;
                    }
                } catch (IOException e) {
                    System.out.println("IOE : "+ e.getMessage());
                }
                try {
                    Thread.sleep(Math.max(10, time - System.currentTimeMillis()));
                } catch (InterruptedException ignore) {
                    return;
                }
            }
        }
    }
}
