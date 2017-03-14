import java.net.*;
import java.util.*;
class InstagramProxyListener implements InviteResponder.OnConnectionListener {
	public int onConnectionRequested(String name) {
		InstagramProxyHandler handler = null;
		try {
			handler = new InstagramProxyHandler(name);
			int port = handler.getPort();
			Thread t = new Thread(handler);
			t.start();
			return port;
		} catch (Exception e) {
			e.printStackTrace();
			if (handler != null) {
				handler.close();
			}
			return 0;
		}
	}
}
