package tcpserver;

import java.net.Socket;

import javax.xml.ws.handler.MessageContext.Scope;

public class Task implements Runnable{
	
	private static final Double MAX_TIME=20000.0;
	
	private Socket sc;
	private String clientId;
	
	public Task(String clientId, Socket socketCliente) {
		this.clientId = clientId;
		this.sc = socketCliente;
		
	}
	public String getClientId() {
		return clientId;
	}
	
	public void realizarTarea() {
	}
	
	
	@Override
	public void run() {
		
		try {
			System.out.println("Executing " + clientId);
			Protocol.procesar(sc.getInputStream(), sc.getOutputStream());
			
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		// TODO Auto-generated method stub
		
	}

}
