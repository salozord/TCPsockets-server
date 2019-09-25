package tcpserver;

public class Task implements Runnable{
	
	private static final Double MAX_TIME=20000.0;
	
	private String clientId;
	
	public Task(String clientId) {
		this.clientId = clientId;
		
	}
	public String getClientId() {
		return clientId;
	}
	
	public void realizarTarea() {
		while(true) {
			
		}
	}
	
	
	@Override
	public void run() {
		
		try {
			realizarTarea();
			System.out.println("Executing" + clientId);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		// TODO Auto-generated method stub
		
	}

}
