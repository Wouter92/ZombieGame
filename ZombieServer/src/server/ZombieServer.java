package server;
import gamestateobjects.CamList;
import gamestateobjects.GameStatus;
import gamestateobjects.MessageService;
import gamestateobjects.RoomList;
import gamestateobjects.enigmas.AEnigma;
import gamestateobjects.enigmas.Enigma1;
import gamestateobjects.enigmas.Enigma2;
import gamestateobjects.enigmas.Enigma3;
import gamestateobjects.enigmas.Enigma4;

import java.io.IOException;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.*;


public class ZombieServer {

	private final HttpServer server;
	private final MessageService mservice = new MessageService();
	private final RoomList roomlist = new RoomList();
	private final CamList camlist = new CamList();
	private final GameStatus status;
	
	public ZombieServer(int port) throws IOException{
		
		//Thread t = new Thread(new LockThread(roomlist));
		//t.start();
		roomlist.addRoom("room1");
		roomlist.addRoom("room2");
		roomlist.addRoom("room3");
		roomlist.addRoom("room4");
		
		AEnigma[] enigmas = {new Enigma1(roomlist, mservice), new Enigma2(roomlist, mservice), new Enigma3(roomlist, mservice), new Enigma4(roomlist, mservice)};
		
		status = new GameStatus(roomlist);
		server = HttpServer.create(new InetSocketAddress(8082), 300);
		server.createContext("/", new ZombieFileHandler());
		server.createContext("/connectcam", new ZombieCamConnectHandler(camlist));
		server.createContext("/getcams", new ZombieCamListRequestHandler(camlist));
		server.createContext("/roomstatus", new RoomStatusUpdateHandler(roomlist));
		server.createContext("/roomcount", new RoomCountRequestHandler(roomlist));
		server.createContext("/getmessages", new MessageRequestHandler(mservice));
		server.createContext("/startgame", new GameStartHandler(status, mservice));
		server.createContext("/outrostarted", new OutroStartedHandler(status));
		server.createContext("/outroended", new OutroEndedHandler(status));
		server.createContext("/startoutro", new StartOutroHandler(status, mservice));
		server.createContext("/resetgame", new GameResetHandler(status, mservice));
		server.createContext("/maxtime", new MaxTimeHandler(status));
		server.createContext("/timerdone", new TimerDoneHandler(status));
		server.createContext("/inprogress", new InProgressHandler(status));
		server.createContext("/endgamestarted", new EndGameStartedHandler(status));
		server.createContext("/startendgame", new StartEndGameHandler(status));

		for (AEnigma ae : enigmas) { 
			server.createContext(ae.getContext(), ae);
			System.out.println("Context created: "+ae.getContext());
		}
		
		server.setExecutor(null);
		server.start();
		
		
		System.out.println("ZombieServer started.");
		
	}
	
	
	public static void main(String[] args) throws IOException {
		new ZombieServer(8082);
	}
}
