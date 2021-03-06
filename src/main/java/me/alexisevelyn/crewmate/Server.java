package me.alexisevelyn.crewmate;

import me.alexisevelyn.crewmate.api.Plugin;
import me.alexisevelyn.crewmate.api.PluginLoader;
import me.alexisevelyn.crewmate.enums.TerminalColors;
import me.alexisevelyn.crewmate.enums.hazel.SendOption;
import me.alexisevelyn.crewmate.events.bus.EventBus;
import me.alexisevelyn.crewmate.handlers.FragmentPacketHandler;
import me.alexisevelyn.crewmate.handlers.GamePacketHandler;
import me.alexisevelyn.crewmate.handlers.HandshakeHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NoPermissionException;
import java.io.File;
import java.io.IOException;
import java.net.*;
import java.nio.file.AccessDeniedException;
import java.util.Arrays;
import java.util.ResourceBundle;

public class Server extends Thread {
	// Server Logger
	private static final Logger logger = LoggerFactory.getLogger(Server.class);

	// https://www.scadacore.com/tools/programming-calculators/online-hex-converter/

	private final DatagramSocket socket;
	private boolean running = false;
	private final byte[] buf = new byte[256];

	private final InetAddress boundIP;
	private final int port;
	private final int maxPlayers;

	private final EventBus eventBus = new EventBus();

	private final File root;
	private final File pluginsFolder;

	public Server(Config config) throws SocketException, AccessDeniedException {
		this.socket = new DatagramSocket(config.getServerPort(), config.getServerAddress());

		this.port = this.socket.getLocalPort();
		this.boundIP = this.socket.getLocalAddress();
		this.maxPlayers = config.getMaxPlayers();

		// Root Directory For Server Files
		root = config.getRootDir();

		// Create Root Folder If It Does Not Exist
		if (!root.exists() && !root.mkdirs()) {
			// https://docs.oracle.com/javase/7/docs/api/java/nio/file/AccessDeniedException.html
			throw new AccessDeniedException(String.format(Main.getTranslationBundle().getString("root_directory_failed_creation"), root.getAbsolutePath()));
		}

		// Plugins Directory For Server Plugins
		pluginsFolder = config.getPluginsDir();

		for (Plugin plugin : PluginLoader.loadPlugins(pluginsFolder, this)) {
			LogHelper.printLine(plugin.getID());
		}
	}

	@Override
	public void run() {
		ResourceBundle translation = Main.getTranslationBundle();
		
		// For Cleaning Up When Shutdown
		this.setupShutdownHook();

		running = true;
		boolean justStarted = false;

		while (this.isRunning()) {
			DatagramPacket packet = new DatagramPacket(this.buf, this.buf.length);

			if (!justStarted) {
				justStarted = true;

				LogHelper.printLine(translation.getString("server_started"));

				// For Title
				LogHelper.print(
						TerminalColors.getTitle(
								String.format(translation.getString("server_listening_title"), this.boundIP.getHostAddress(), this.port)
						)
				);
			}

			try {
				// This is useless as it doesn't stop the packet receiver
				if (this.isInterrupted())
					throw new InterruptedException();

				// Can't Receive Packets if Socket Is Closed
				if (this.socket.isClosed())
					this.exit();

				// Receive Packet From Client
				this.socket.receive(packet);

				// Parse Packet
				this.parsePacketAndReply(packet);

				// Clear Buffer
				this.clearBuffer();
			} catch (IOException e) {
				// This is the only way I know how to get rid of the exception output thrown when closing via normal means
				if (this.socket.isClosed())
					return;

				LogHelper.printLineErr("IOException: " + e.getMessage());
				e.printStackTrace();

				this.exit();
			} catch (InterruptedException e) {
				// This is due to recommendations to rethrow the interrupt after catching it
				// https://stackoverflow.com/a/1087504/6828099
				Thread.currentThread().interrupt();

				this.exit();
			}
		}
	}

	public EventBus getEventBus() {
		return eventBus;
	}

	public int getMaxPlayers() {
		return maxPlayers;
	}

	// TODO: TODO TODO - https://discord.com/channels/757425025379729459/759066383090188308/765419168466993162
	// "Yeah, lengths for Hazel messages are always 2 bytes little-endian" - codyphobe from Imposter Discord
	private void parsePacketAndReply(DatagramPacket packet) throws IOException {
		if (packet.getData().length < 1)
			return;

		SendOption sendOption = SendOption.getSendOption(packet.getData()[0]);

		// Throw Out Any Unknown Packets
		// Sanitization Check
		if (sendOption == null)
			return;

		byte[] replyBuffer;
		switch (sendOption) {
			case HELLO: // Initial Connection (Handshake)
				replyBuffer = HandshakeHandler.handleHandshake(packet, this);
				break;
			case ACKNOWLEDGEMENT: // Unhandled
			case PING: // Ping
				sendReliablePacketAcknowledgement(packet);
				return;
			case RELIABLE: // Reliable Packet (UDP Doesn't Have Reliability Builtin Like TCP Does)
				replyBuffer = GamePacketHandler.handleReliablePacket(packet, this);
				this.sendReliablePacketAcknowledgement(packet);
				break;
			case NONE: // Generic Unreliable Packet - Used For Movement (Unknown If Used For Anything Else)
				replyBuffer = GamePacketHandler.handleUnreliablePacket(packet);
				break;
			case FRAGMENT: // Fragmented Packet (For Data Bigger Than One Packet Can Hold) - Unknown If Used in Among Us
				replyBuffer = FragmentPacketHandler.handleFragmentPacket(packet);
				break;
			default:
				return;
		}

		// Don't Send Packet if No Data To Send
		if (replyBuffer.length == 0)
			return;

		// Received Packet Port and Address
		InetAddress address = packet.getAddress();
		int port = packet.getPort();

		// Packet to Send Back to Client
		packet = this.createSendPacket(replyBuffer, replyBuffer.length, address, port);

		// Send Reply Back
		this.socket.send(packet);

		// Check If Disconnect and Disconnect From Our End
		SendOption replyOption = SendOption.getSendOption(replyBuffer[0]);

		// Sanitization
		if (replyOption == null)
			return;

		// Disconnect Client From Server End
		if (replyOption.equals(SendOption.DISCONNECT)) {
			// TODO: Figure out how to close one client's connection
			// LogHelper.printLine("Closing Connection!!!");
			// this.socket.disconnect();
		}
	}

	private void sendReliablePacketAcknowledgement(DatagramPacket packet) {
		// Received Packet Port and Address
		InetAddress address = packet.getAddress();
		int port = packet.getPort();

		// Get Packet Info
		int length = packet.getLength();
		byte[] buffer = packet.getData();

		// Verify Packet Length
		if (length < 3)
			return;

		// Get Nonce
		byte[] nonce = new byte[] {buffer[1], buffer[2]};

		// Get Acknowledgement
		byte[] acknowledgement = PacketHelper.getAcknowledgement(nonce);

		// Packet to Send Back to Client
		packet = this.createSendPacket(acknowledgement, acknowledgement.length, address, port);

		// Send Reply Back
		try {
			this.socket.send(packet);
		} catch (IOException exception) {
			exception.printStackTrace();
		}
	}

	public void sendPacket(DatagramPacket packet) {
		try {
			this.socket.send(packet);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public DatagramPacket createSendPacket(byte[] buffer, int length, InetAddress address, int port) {
		byte[] sendBuffer = new byte[length];

		if (length >= 0)
			System.arraycopy(buffer, 0, sendBuffer, 0, length);

		return new DatagramPacket(sendBuffer, sendBuffer.length, address, port);
	}

	// To prevent leaking data
	private void clearBuffer() {
		Arrays.fill(this.buf, (byte) 0x0);
	}

	// Isn't this supposed to be overridable from Thread?
	public void exit() {
		if (this.running)
			this.shutdown();
	}

	private void shutdown() {
		this.running = false;

		LogHelper.printLine(Main.getTranslationBundle().getString("server_shutdown"));

		// This never runs.
		this.socket.close();
	}

	public boolean isRunning() {
		return this.running;
	}

	private void setupShutdownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> Main.getServer().exit()));
	}

	public InetAddress getBoundIP() {
		return this.boundIP;
	}

	public int getPort() {
		return this.port;
	}
}
