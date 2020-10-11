package me.alexisevelyn.crewmate.handlers.gamepacket;

import me.alexisevelyn.crewmate.PacketHelper;
import me.alexisevelyn.crewmate.enums.Language;
import me.alexisevelyn.crewmate.enums.Map;
import me.alexisevelyn.crewmate.handlers.GamePacketHandler;

import java.net.DatagramPacket;
import java.util.Arrays;

public class SearchGame {
	public static byte[] handleSearchPublicGame(DatagramPacket packet) {
		// Request Search
		// 0000   01 00 02 2c 00 10 00 2a 02 0a 00 01 00 00 01 00   ...,...*........
		// 0010   00 80 3f 00 00 80 3f 00 00 c0 3f 00 00 70 41 01   ..?...?...?..pA.
		// 0020   01 02 01 00 00 00 02 01 0f 00 00 00 78 00 00 00   ............x...
		// 0030   01 0f                                             ..

		if (packet.getLength() != 50)
			return new byte[0];

		byte[] buffer = packet.getData();

		// 0 Means Any
		int numberOfImposters = buffer[38];

		// List of Maps Being Included In Search
		Map[] maps = GamePacketHandler.parseMapsSearch(buffer[14]);

		// Language To Search By
		int language = Language.convertToInt(buffer[10], buffer[11]);

		System.out.println("Number of Imposters: " + ((numberOfImposters == 0) ? "Any" : numberOfImposters));
		System.out.println("Maps: " + Arrays.toString(maps));
		System.out.println("Language: " + GamePacketHandler.getLanguageName(language));

		return getFakeSearchBytes(numberOfImposters, maps, language);
	}

	private static byte[] getFakeSearchBytes(int numberOfImposters, Map[] maps, int language) {
		// TODO: Figure out what these mean!!!

		// Search Results
		// 0000   01 00 1c 02 01 10 ff 00 00 19 00 00 68 ed 80 75   ............h..u
		// 0010   07 56 e6 71 31 80 08 73 75 73 68 69 69 69 69 03   .V.q1..sushiiii.
		// 0020   b4 03 00 02 0a 14 00 00 c0 51 87 18 07 56 c5 94   .........Q...V..
		// 0030   13 80 03 59 6f 75 02 ed 01 00 02 0a 13 00 00 c0   ...You..........
		// 0040   51 82 3b 07 56 3b 4c 1d 80 02 42 61 06 ba 2b 00   Q.;.V;L...Ba..+.
		// 0050   02 0a 17 00 00 2d 4f 6e 6a 07 56 3a 3c 38 80 07   .....-Onj.V:<8..
		// 0060   4e 6f 74 4b 69 77 69 03 1d 00 02 07 14 00 00 2d   NotKiwi........-
		// 0070   4f 6e 19 07 56 a1 d9 07 80 03 79 75 68 09 ae 09   On..V.....yuh...
		// 0080   00 02 0a 15 00 00 2d 4f 6c 6b 07 56 29 d4 4c 80   ......-Olk.V).L.
		// 0090   04 59 6f 59 6f 03 c5 0b 00 02 0a 15 00 00 2d 4f   .YoYo.........-O
		// 00a0   14 15 07 56 48 8a 59 80 05 6a 65 6c 6c 79 02 40   ...VH.Y..jelly.@
		// 00b0   00 02 0a 1a 00 00 45 a4 c1 e9 07 56 a4 f9 48 80   ......E....V..H.
		// 00c0   09 42 75 62 62 61 62 75 74 65 05 84 0b 00 02 0a   .Bubbabute......
		// 00d0   18 00 00 2d 4f 61 87 07 56 4b 08 29 80 07 43 72   ...-Oa..VK.)..Cr
		// 00e0   69 73 74 61 6c 03 a4 02 00 02 0a 1a 00 00 ad e6   istal...........
		// 00f0   9e 08 07 56 a2 ec 2b 80 0a 41 6e 6e 65 20 46 72   ...V..+..Anne Fr
		// 0100   61 6e 6b 02 46 00 02 0a                           ank.F...

		// Search Results (One Result)
		// 0000   01 00 15 1e 00 10 1b 00 00 18 00 00 c6 3a 7b 62   .............:{b
		// 0010   07 56 1d d0 01 80 08 73 68 79 20 70 69 6e 6b 01   .V.....shy pink.
		// 0020   61 01 01 04                                       a...

		// Search Results (One Result)
		// 0000   01 00 0f 1c 00 10 19 00 00 16 00 00 ad ff ff d4   ................
		// 0010   07 56 4d e2 35 80 06 6a 61 79 64 65 6e 01 06 01   .VM.5..jayden...
		// 0020   01 0a                                             ..

		// Search Results (One Result)
		// 0000   01 00 2f 1c 00 10 19 00 00 16 00 00 ad ff ff d4   ../.............
		// 0010   07 56 4d e2 35 80 06 6a 61 79 64 65 6e 03 29 01   .VM.5..jayden.).
		// 0020   01 0a                                             ..

		// Search Results (No Results)
		// 0000   01 00 0f 03 00 10 00 00 00                        .........

		// Search Results (Two Results) - Game Code For Result 2 (EVAWJF - 10:1e:6c:8c)
		// 0000   01 00 36 36 00 10 33 00 00 16 00 00 ad ed 11 4c   ..66..3........L
		// 0010   cf 56 10 1e 6c 8c 06 4b 61 72 6f 6c 47 01 11 01   .V..l..KarolG...
		// 0020   02 0a 17 00 00 48 0e b5 a0 07 56 5f ad 25 80 06   .....H....V_.%..
		// 0030   50 6c 61 79 65 72 02 f6 0a 01 03 09               Player......

		// Search Results (One Result) - Game Code For Result (FCUBWQ - f9:51:13:80)
		// 0000   01 00 19 1c 00 10 19 00 00 16 00 00 32 74 07 64   ............2t.d
		// 0010   07 56 f9 51 13 80 05 61 66 66 61 6e 02 9b 02 01   .V.Q...affan....
		// 0020   02 08                                             ..

		// Search Results (One Result) - Game Code For Result (BGQFYQ - 4d:49:39:80)
		// Polus Only (0x02) - Arabic (0x20 0x00) - 2 Imposters - Name: "bunda eki" - 1 out of 7 max people
		// 0000   01 00 20 1f 00 10 1c 00 00 19 00 00 2d 38 52 d6   .. .........-8R.
		// 0010   07 56 4d 49 39 80 09 62 75 6e 64 61 20 65 6b 69   .VMI9..bunda eki
		// 0020   01 31 02 02 07                                    .1...

		// Search Results (One Result) - Game Code For Result (USCWTQ - f8:f4:2a:80)
		// Mira-HQ Only (0x01) - Korean (0x04 0x00) - 1 Imposter - Name: "freddy" - 1 out of 4 max people
		// 0000   01 00 01 1c 00 10 19 00 00 16 00 00 2d 21 22 14   ............-!".
		// 0010   07 56 f8 f4 2a 80 06 66 72 65 64 64 79 01 20 01   .V..*..freddy. .
		// 0020   01 04                                             ..

		// TODO: Broken Packet From Here
		// 0000   01 00 00 00 00 10 18 00 00 15 00 00 00 07 56 f9   ..............V.
		// 0010   51 13 80 09 46 61 6b 65 20 47 61 6d 65 03 01 02   Q...Fake Game...
		// 0020   08                                                .

		String name = "Fake Game";
		byte unknown = 0x00;
		int imposters = 3;

		byte[] gameCodeBytes = new byte[] {(byte) 0xf9, 0x51, 0x13, (byte) 0x80};

		byte[] messagePartTwo = PacketHelper.getCombinedReply(new byte[] {(byte) name.getBytes().length}, name.getBytes());
		byte[] messagePartThree = new byte[] {(byte) imposters, 0x01, 0x02, 0x08};

		byte[] combinedMessagePartOne = PacketHelper.getCombinedReply(messagePartTwo, messagePartThree);
		byte[] combinedMessagePartTwo = PacketHelper.getCombinedReply(gameCodeBytes, combinedMessagePartOne);

		byte[] messagePartOne = new byte[] {(byte) (3 + combinedMessagePartTwo.length), unknown, unknown, unknown, 0x07, 0x56};

		byte[] message = PacketHelper.getCombinedReply(messagePartOne, combinedMessagePartTwo);
		byte[] header = new byte[] {0x01, 0x00, unknown, unknown, 0x00, 0x10, (byte) (message.length), 0x00, 0x00};

		return PacketHelper.getCombinedReply(header, message);
	}
}
