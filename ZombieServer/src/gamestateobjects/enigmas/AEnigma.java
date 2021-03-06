package gamestateobjects.enigmas;

import gamestateobjects.MessageService;
import gamestateobjects.RoomList;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import server.Sender;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public abstract class AEnigma implements HttpHandler{
	
	private RoomList roomlist;
	private MessageService inbox;
	
	public AEnigma(RoomList roomlist, MessageService inbox) {
		this.roomlist = roomlist;
		this.inbox = inbox;
	}

	abstract boolean checkSolution(String s);

	public abstract String getRoomName();
	public abstract String getContext();
	
	abstract String getTip();
	
	abstract boolean hasNextRoom();
	
	abstract String getNextRoomName();
	
	abstract String getSMS();
	abstract String getSMSSender();
	
	@Override
	public void handle(HttpExchange t) throws IOException {
		String uri = t.getRequestURI().toString();
		if(uri.contains("/gettip"))
			Sender.sendData(t, "{\"data\": \""+getTip()+"\"}");
		else if(uri.contains("/checksolution")){
			BufferedReader input = new BufferedReader(new InputStreamReader(t.getRequestBody()));
			String sol = input.readLine();
			
			if(checkSolution(sol)) {
				roomlist.getRoom(getRoomName()).unlock();
				scheduleSMS();
				Sender.sendData(t, "{\"data\": \"true\"}");
			} else
				Sender.sendData(t, "{\"data\": \"false\"}");
		}
		else if(uri.contains("/getnextroom")){
			if(hasNextRoom())
				Sender.sendData(t, "{\"data\": \""+getNextRoomName()+"\"}");
			else
				Sender.sendData(t, "{\"data\": \"none\"}");
		}	
	}
	
	private void scheduleSMS(){
		Timer timer = new Timer();

		// scheduling the task at interval
		timer.schedule(new TimerTask() {

			@Override
			public void run() {
				inbox.publishMessage(getSMSSender(), getSMS());					
			}
		}, new Random().nextInt(9000)+5000); 
	}
}


