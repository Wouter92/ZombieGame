import java.io.IOException;
import java.net.InetSocketAddress;
import com.sun.net.httpserver.*;


public class ZombieServer {

	private final HttpServer server;
	private final ZombieCamList camlist = new ZombieCamList();
	
	public ZombieServer(int port) throws IOException{
		server = HttpServer.create(new InetSocketAddress(8082), 300);
		server.createContext("/", new ZombieFileHandler());
		server.createContext("/connectcam", new ZombieCamConnectHandler(camlist));
		server.createContext("/getcams", new ZombieCamListRequestHandler(camlist));

		server.setExecutor(null);
		server.start();
	}
	
	
	public static void main(String[] args) throws IOException {
		new ZombieServer(8082);
	}
}
