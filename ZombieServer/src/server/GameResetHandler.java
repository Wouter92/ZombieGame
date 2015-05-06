package server;

import gamestateobjects.GameStatus;

import java.io.IOException;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class GameResetHandler implements HttpHandler{

	private GameStatus status = null;
	
	public GameResetHandler(GameStatus s) {
		this.status = s;
	}
	
	@Override
	public void handle(HttpExchange t) throws IOException {
		status.resetGame();
		Sender.sendData(t, "{\"data\": \"true\"}");
	}

}