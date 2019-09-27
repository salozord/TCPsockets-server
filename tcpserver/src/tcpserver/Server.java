package tcpserver;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
	
	private static ServerSocket ss;
	
	private static final Integer NUM_THREADS = 25;
	
	public static void main(String[] args) {
		
		ExecutorService execuServ  = Executors.newFixedThreadPool(NUM_THREADS);
		
		try 
		{
			ss = new ServerSocket(7069);
			while(true) {
				try {
					Socket sc= ss.accept();
					execuServ.execute(new Task(sc.getLocalAddress().toString(), sc));
					
				}
				catch(Exception e) {
					
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
