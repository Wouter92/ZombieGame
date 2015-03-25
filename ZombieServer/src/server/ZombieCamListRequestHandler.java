package server;
import gamestateobjects.RoomList;

import java.io.IOException;
import java.io.OutputStream;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;


public class ZombieCamListRequestHandler implements HttpHandler {

	private final RoomList camlist;
	
	public ZombieCamListRequestHandler(RoomList camlist) {
		this.camlist = camlist;
	}
	
	@Override
	public void handle(HttpExchange t) throws IOException {
		String response = camlist.camListJSON();
		Headers responseHeaders= t.getResponseHeaders();
		responseHeaders.set("Content-Type","application/json");
        t.sendResponseHeaders(200, response.length());
        OutputStream os = t.getResponseBody();
        os.write(response.getBytes());
        os.close();
	}

}
