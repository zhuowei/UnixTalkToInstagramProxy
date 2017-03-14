import java.net.*;
import java.util.*;
import java.nio.charset.*;
/**
 Implements a stub version of the talkd protocol.
 See http://www.cs.columbia.edu/~hgs/internet/talk.html
 and https://github.com/att/uwin/blob/master/src/cmd/inetutils/libinetutils/talkd.h
*/
class InviteResponder implements Runnable {
	private OnConnectionListener listener;
	private byte[] addr;
	public InviteResponder(InetAddress ia) {
		addr = ia.getAddress();
	}
	public void run() {
		DatagramSocket socket;
		try {
			socket = new DatagramSocket(518);
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		byte[] buf = new byte[0x54];
		DatagramPacket packet = new DatagramPacket(buf, buf.length);
		byte[] outBuf = new byte[0x18];
		DatagramPacket outPacket = new DatagramPacket(outBuf, outBuf.length);
		Charset utf8Charset = Charset.forName("UTF-8");
		while (true) {
			try {
				socket.receive(packet);
				if (packet.getLength() != 0x54) {
					System.out.println("wrong length: " + packet.getLength());
					continue;
				}
				//System.out.println(Arrays.toString(buf));
				outBuf[0] = 1; // version
				outBuf[1] = buf[1]; // type;
				outBuf[2] = 0; // answer - success
				System.arraycopy(buf, 4, outBuf, 4, 4 + 16); // msg id and port
				int endi;
				for (endi = 0x38; endi < 0x38 + 12; endi++) {
					if (buf[endi] == 0) break;
				}
				String name = new String(buf, 0x38, endi - 0x38, utf8Charset);
				int port = listener.onConnectionRequested(name);
				outBuf[10] = (byte)((port >> 8) & 0xff);
				outBuf[11] = (byte)(port & 0xff);
				System.arraycopy(addr, 0, outBuf, 12, 4); // address
				outPacket.setSocketAddress(packet.getSocketAddress());
				//System.out.println(Arrays.toString(outBuf));
				socket.send(outPacket);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	public static interface OnConnectionListener {
		public int onConnectionRequested(String name);
	}
	public static void main(String[] args) throws Exception {
		InviteResponder responder = new InviteResponder(InetAddress.getByName(args[0]));
		responder.listener = new InstagramProxyListener();
		responder.run();
	}
}
