package tcpserver;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PoolThreads 
{
	private static ServerSocket ss;
	private static int nThreads=2;
	private static Integer numeroSesiones=0;
	private static boolean primero;
	
	public void servidor() throws IOException, InterruptedException
	{
		InputStreamReader isr = new InputStreamReader(System.in);
		BufferedReader br = new BufferedReader(isr);
		int ip = 8080;
		System.out.println("Empezando servidor maestro en puerto " + ip);

		// Crea el socket que escucha en el puerto seleccionado.
		ss = new ServerSocket(ip);
		System.out.println("Socket creado.");
		
		ExecutorService executor= Executors.newFixedThreadPool(nThreads);
		System.out.println("Pool con "+nThreads+" threads ha sido creado.");
		System.out.println("Esperando solicitudes.");
		primero = false;

		while (true) {
			try 
			{ 
				primero = false;
				if(numeroSesiones <= 25)
				{
					Socket sc = ss.accept();
					if(numeroSesiones == 0)
					{
						primero = true;
					}
					numeroSesiones++;
					executor.execute(new Runnable() {
						
						@Override
						public void run() {
							long idThread = Thread.currentThread().getId();
							
							System.out.println("Cliente " + idThread + " aceptado.");
							Protocol d = new Protocol(sc,idThread, monitor, primero);
						}
					});
				}
				else
				{
					Thread.sleep(1000);
				}
			} catch (IOException e) {
				System.out.println("Error creando el socket cliente.");
				e.printStackTrace();
			}
		}
	}
	
	public void decision()
	{
		
	}
	
	public void finSesion()
	{
		synchronized (numeroSesiones) {
			numeroSesiones--;
		}
	}
	
	public static void main(String ... args) throws Exception{
		PoolThreads pool = new PoolThreads();
		pool.servidor();
	}
	

	public void setnThreads(int number) {
		nThreads = number;
	}
}
