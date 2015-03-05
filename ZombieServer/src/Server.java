import java.awt.Dimension;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamStreamer;


public class Server {

	public static void main(String[] args) {
		Webcam webcam = Webcam.getWebcams().get(0);
		ServerSocket sock;
		webcam.setViewSize(new Dimension(640, 480));
		new WebcamStreamer(8081, webcam, 100, true);
		  do {
		    try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		  } while (true);
	}
	
}