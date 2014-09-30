package eu.neurovertex.latencymonitor;

import java.util.AbstractList;
import java.util.List;

/**
 * @author Neurovertex
 *         Date: 27/05/2014, 17:39
 */
public class LatencyRingBuffer extends AbstractList<Long> {
	private int startIndex = 0;
	private int offset = 0;
	private int size = 0;
	private long[] buffer;

	public LatencyRingBuffer(int size) {
		buffer = new long[size];
	}


	@Override
	public Long get(int index) {
		if (index < 0 || index - offset >= size)
			throw new ArrayIndexOutOfBoundsException(index);
		if (index < offset)
			return 0L;
		return buffer[(index - offset + startIndex) % buffer.length];
	}

	@Override
	public synchronized boolean add(Long aLong) {
		if (size < buffer.length)
			size ++;
		else {
			startIndex = (startIndex + 1) % buffer.length;
			offset ++;
		}
		buffer[(startIndex + size)%buffer.length] = aLong;
		return true;
	}

	@Override
	public synchronized Long set(int index, Long element) {
		if (index < 0 || index - offset >= size)
			throw new ArrayIndexOutOfBoundsException(index);
		if (index < offset)
			return 0L;
		Long val = buffer[(index - offset + startIndex) % buffer.length];
		buffer[(index - offset + startIndex) % buffer.length] = element;
		return val;
	}

	public synchronized boolean add() {
		return add(0l);
	}

	@Override
	public int size() {
		return size;
	}

	public long[] getBuffer(long[] buff) {
		if (buff != null && buff.length != buffer.length)
			throw new IllegalArgumentException("array size must match internal buffer size");
		if (buff == null)
			buff = new long[buffer.length];
		System.arraycopy(buffer, startIndex, buff, 0, buffer.length-startIndex);
		if (startIndex > 0)
			System.arraycopy(buffer, 0, buff, buffer.length-startIndex, startIndex);
		return buff;
	}

	long[] getInternalBuffer() {
		return buffer.clone();
	}}
