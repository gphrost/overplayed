/* Copyright (c) 2013 All Right Reserved Steven T. Ramzel
 *
 *	This file is part of Overplayed.
 *
 *	Overplayed is free software: you can redistribute it and/or modify
 *	it under the terms of the GNU Lesser General Public License as published by
 *	the Free Software Foundation, either version 3 of the License, or
 *	(at your option) any later version.
 *
 *	Overplayed is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *	GNU Lesser General Public License for more details.
 *
 *	You should have received a copy of the GNU Lesser General Public License
 *	along with Overplayed.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.gphrost.Overplayed;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

import org.gphrost.Overplayed.Controller.Controller;
import org.gphrost.Overplayed.Controller.ControlView;

/**
 * Thread controlling network interaction
 * 
 * @author Steven T. Ramzel
 */
public class NetworkThread extends Thread {
	private static final long MAX_REFRESH_WAIT = 500;

	private String address;
	ByteBuffer buffer;// Temp Buffer
	// Flag as to whether or not we need to send a packet
	public boolean changed = true;
	DatagramChannel channel = null;
	InetSocketAddress destination;
	private long nextSendTime;
	// Counter for localSequence. Used to test ping rate
	short localSequence;
	// Used to store when transmission where made for comparison when they are
	// echoed
	long[] packetTimes = new long[32];
	private int port;
	byte[] protocol = new byte[4];
	// Our send and receive buffers
	ByteBuffer receiveBuffer = ByteBuffer.allocate(6);
	int rtt = 0; // Last ping
	boolean running; // Status of the thread
	ByteBuffer sendBuffer = ByteBuffer.allocate(17);
	private long time;

	private Controller controller;

	{
		// Arbitrary protocol identifier attached to all network packets
		final int PROTOCOL_ID = 0x99887766;
		protocol[0] = (byte) (PROTOCOL_ID >> 24);
		protocol[1] = (byte) ((PROTOCOL_ID >> 16) & 0xFF);
		protocol[2] = (byte) ((PROTOCOL_ID >> 8) & 0xFF);
		protocol[3] = (byte) ((PROTOCOL_ID >> 0) & 0xFF);
	}

	NetworkThread(String address, int port, Controller controller) {
		super();
		this.address = address;
		this.port = port;
		this.controller = controller;
	}

	/**
	 * Preps the send buffer for next transmission
	 */
	public void prepSendBuffer() {
		// sendBuffer.rewind();
		// Put the protocol ID and the localSequence
		sendBuffer.put(protocol);
		sendBuffer.putShort(localSequence);
		// Put analog values
		sendBuffer.putShort(controller.analogState.get(0));
		sendBuffer.putShort(controller.analogState.get(1));
		sendBuffer.putShort(controller.analogState.get(2));
		sendBuffer.putShort(controller.analogState.get(3));

		// Build first byte and put
		byte firstByte = 0;
		for (int n = 0; n < 7; n++, firstByte <<= 1)
			if (controller.buttonState.get(n))
				firstByte |= 1;
		if (controller.buttonState.get(7))
			firstByte |= 1;
		sendBuffer.put(firstByte);

		// Build second byte and put
		byte secondByte = 0;
		for (int n = 8; n < 15; n++, secondByte <<= 1)
			if (controller.buttonState.get(n))
				secondByte |= 1;
		if (controller.buttonState.get(15))
			secondByte |= 1;
		sendBuffer.put(secondByte);

		// Put the extra button (not implemented in touch yet)
		//sendBuffer.put((byte) (controller.buttons.get(16) ? 1 : 0));
		sendBuffer.rewind();
	}

	@Override
	public void run() {
		try {
			destination = new InetSocketAddress(address, port);
			DatagramSocket socket = new DatagramSocket(port);
			socket.setSoTimeout(1);
			byte[] recieved = new byte[6];
			DatagramPacket recieved_pkt = new DatagramPacket(recieved,
					recieved.length);
			DatagramPacket sendPacket = new DatagramPacket(sendBuffer.array(),
					sendBuffer.capacity(), destination);

			while (running) {
				time = System.currentTimeMillis();
				socket.setSoTimeout(Math.max(rtt, 1));
				prepSendBuffer();
				socket.send(sendPacket);
				packetTimes[localSequence % 32] = time;
				localSequence++;
				changed = false;
				nextSendTime = time + MAX_REFRESH_WAIT;
				while (running && !(changed || time > nextSendTime))
					try {
						// Retrieve the echoed sequence number
						socket.receive(recieved_pkt);
						buffer = ByteBuffer.wrap(recieved_pkt.getData());
						buffer.getInt();
						int testShort = unsingnedShortToInt(buffer.getShort());
						rtt = (int) (time - packetTimes[testShort % 32]);
						ControlView.refresh = (ControlView.refresh + rtt) / 2;
						// Set time-out to the last ping
					} catch (IOException e) {
					}
			}
			// Killing thread/app
			// Send a zero sequence, so if we connect again to the server it has
			// a reset counter
			localSequence = 0;
			prepSendBuffer();
			socket.send(sendPacket);
			socket.close();
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Convert an unsigned short to an integer
	 * 
	 * @param value
	 *            Short to be converted
	 * @return
	 */
	public int unsingnedShortToInt(short value) {
		if (value < 0)
			return Short.MAX_VALUE + value - Short.MIN_VALUE;
		else
			return value;
	}
}
