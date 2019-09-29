package tcpserver;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PoolThreads 
{
	private static ServerSocket ss;
	private static int nThreads;
	private static Integer nThreadsActivos = 0;
	private static int tiempoMuerte = 0;
	private static File archivo;
	private static Integer numeroSesiones=0;
	private static Boolean iniciaConcurrencia;

	public String listarArchivos()
	{
		File directorio = new File("../../data");
		String retorno = "";
		String[] nombres = directorio.list();
		for(int i = 0; i < nombres.length; i++)
		{
			if(i == nombres.length-1)
			{
				retorno += (i+1) + ") " + nombres[i];
				break;
			}
			retorno += (i+1) + ") " + nombres[i] + "\n";
		}
		return retorno;
	}

	public void seleccion(BufferedReader br) throws IOException
	{
		while(true){
			System.out.println("Digite el número de un archivo");
			System.out.println(listarArchivos());
			File directorio = new File("../../data");
			int arch = Integer.parseInt(br.readLine());
			if(arch < 1 || arch >= directorio.listFiles().length)
			{
				System.out.println("Archivo incorrecto");
				continue;
			}
			archivo = directorio.listFiles()[arch-1];
			break;
		}
		while(true)
		{
			System.out.println("Ingrese el número de clientes concurrentes a los que se les enviará el archivo");
			int num = Integer.parseInt(br.readLine());
			if(num < 0 || num > 25)
			{
				System.out.println("Solo puede tener entre 1 y 25 clientes concurrentes");
				continue;
			}
			nThreads = num;
			break;
		}
		while(true)
		{
			System.out.println("Ingrese el número de timeout de las peticiones del cliente en segundos, MAX 10 SEGUNDOS");
			int muerte = Integer.parseInt(br.readLine());
			if(muerte < 0 || muerte > 10)
			{
				System.err.println("Ingrese un tiempo válido");
				continue;
			}
			tiempoMuerte = muerte;
			break;
		}
	}

	public void servidor() throws IOException, InterruptedException
	{
		InputStreamReader isr = new InputStreamReader(System.in);
		BufferedReader br = new BufferedReader(isr);
		int ip = 8080;
		System.out.println("Empezando servidor maestro en puerto " + ip);

		// Crea el socket que escucha en el puerto seleccionado.
		ss = new ServerSocket(ip);
		System.out.println("Socket creado.");

		seleccion(br);

		ExecutorService executor= Executors.newFixedThreadPool(nThreads);
		System.out.println("Pool con "+nThreads+" threads ha sido creado.");
		System.out.println("Esperando solicitudes.");
		iniciaConcurrencia = false;

		while (true) {
			try 
			{ 
				if(numeroSesiones <= 25)
				{
					Socket sc = ss.accept();
					numeroSesiones++;
					boolean aceptaArchs = false;
					seAceptan(aceptaArchs);
					PoolThreads yo = this;
					executor.execute(new Runnable() {

						@Override
						public void run() {
							
							Protocol d = new Protocol(sc, aceptaArchs, tiempoMuerte, archivo, yo);
							d.start();
						}
					});
				}
				else
				{
					wait();
				}
			} 
			catch (IOException e) {
				System.out.println("Error creando el socket cliente.");
				e.printStackTrace();
			}
		}
	}
	
	public void seAceptan(boolean aceptaArchs)
	{
		synchronized (nThreadsActivos) 
		{
			synchronized (iniciaConcurrencia) 
			{
				if(iniciaConcurrencia == false)
				{
					iniciaConcurrencia = true;
					nThreadsActivos++;
					aceptaArchs = true;
				}
				else
				{
					if(nThreadsActivos < nThreads)
					{
						aceptaArchs = true;
						nThreadsActivos++;
					}
					else
					{
						aceptaArchs = false;
					}
				}
			}
		}
	}
	
	public boolean siguenBloqueadosLosArchivos()
	{
		return iniciaConcurrencia;
	}

	public void finSesionArchivos()
	{
		synchronized(nThreadsActivos)
		{
			synchronized (iniciaConcurrencia) 
			{
				nThreadsActivos--;
				if(nThreadsActivos == 0)
				{
					iniciaConcurrencia = false;
				}
			}
		}
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
}
