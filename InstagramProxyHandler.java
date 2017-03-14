import java.net.*;
import java.util.*;
import java.io.*;
import java.nio.charset.*;
import org.json.*;

public class InstagramProxyHandler implements Runnable {
	private ServerSocket serverSocket;
	private String name;
	public InstagramProxyHandler(String name) throws IOException {
		this.name = name;
		serverSocket = new ServerSocket(0);
		serverSocket.setSoTimeout(5000); // 5 second timeout.
	}
	public void close() {
		if (serverSocket != null) {
			try {
				serverSocket.close();
			} catch (IOException ie) {
				ie.printStackTrace();
			}
			serverSocket = null;
		}
	}
	public int getPort() {
		return serverSocket.getLocalPort();
	}
	public void run() {
		Socket sock = null;
		try {
			sock = serverSocket.accept();
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			serverSocket.close();
			serverSocket = null;
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (sock == null) return;
		try {
			PrintWriter out = new PrintWriter(sock.getOutputStream());
			Scanner scan = new Scanner(sock.getInputStream());
			out.println("\025\027\r\nInstagram feed for " + name);
			out.flush();
			// todo: actually hook this up to the Instagram chat api.
			JSONArray pics = getFeedForUser(name, out);
			if (pics == null) return;
			for (int i = 0; i < pics.length(); i++) {
				out.println();
				JSONObject pic = pics.getJSONObject(i);
				printPic(pic, out);
				out.flush();
				if (scan.hasNextLine()) {
					String line = scan.nextLine();
				}
			}
				
			out.println("End of the most recent images.");
			out.println("Sorry, can't DM yet - anyone know how to access Instagram's private API?");
			out.println("https://github.com/zhuowei/UnixTalkToInstagramProxy");
			out.flush();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				sock.close();
			} catch (IOException ie) {
				ie.printStackTrace();
			}
		}
	}
	private static final String instagramJsonStart = "<script type=\"text/javascript\">window._sharedData = ";
	private static void printPic(JSONObject pic, PrintWriter out) throws Exception {
				out.println("https://instagram.com/p/" + pic.getString("code"));
				if (pic.optString("caption") != null) {
					out.println(pic.optString("caption"));
				}
				out.println(dateify(pic.getInt("date")));
				out.println("Press Enter to continue.");
	}
	private static JSONArray getFeedForUser(String username, PrintWriter out) throws Exception {
		URLConnection conn = null;
		DataInputStream stream = null;
		try {
			conn = new URL("https://www.instagram.com/" + username + "/").openConnection();
			stream = new DataInputStream(conn.getInputStream());
			byte[] buf = new byte[conn.getContentLength()];
			stream.readFully(buf);
			/*
			if (length != buf.length) {
				out.println("Server returned too small payload: " + length + ":" + buf.length);
				System.out.println(new String(buf, Charset.forName("UTF-8")));
				return;
			}
			*/
			String webpageSource = new String(buf, Charset.forName("UTF-8"));
			int index = webpageSource.indexOf(instagramJsonStart);
			if (index == -1) {
				out.println("Invalid username: " + username);
				return null;
			}
			String jsonStr = webpageSource.substring(index + instagramJsonStart.length());
			JSONObject jsonObj = new JSONObject(jsonStr);
			JSONArray pics = jsonObj.getJSONObject("entry_data").getJSONArray("ProfilePage").getJSONObject(0).
				getJSONObject("user").getJSONObject("media").getJSONArray("nodes");
			return pics;
		} finally {
			if (stream != null) {
				try {
					stream.close();
				} catch (IOException ie) {
					ie.printStackTrace();
				}
			}
		}
	}
	private static String dateify(int indate) {
		long targetTime = indate;
		long currentTime = new Date().getTime() / 1000L;
		long difference = targetTime - currentTime;
		long absDiff = Math.abs(difference);
		if (absDiff == 0) return "just now";
		StringBuilder b = new StringBuilder();
		long seconds = absDiff % 60L;
		absDiff /= 60L;
		long minutes = absDiff % 60L;
		absDiff /= 60L;
		long hours = absDiff % 24L;
		absDiff /= 24L;
		long days = absDiff;
		if (difference > 0) b.append("in ");
		if (days != 0) {
			b.append(days);
			b.append(" days ");
		}
		if (hours != 0) {
			b.append(hours);
			b.append(" hours ");
		}
		if (minutes != 0) {
			b.append(minutes);
			b.append(" minutes ");
		}
		if (seconds != 0) {
			b.append(seconds);
			b.append(" seconds ");
		}
		if (difference < 0) b.append("ago");
		return b.toString();
	}
}
