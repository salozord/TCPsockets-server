package tcpserver;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
public class Protocol extends Thread{

	public static final String PREPARADO = "PREPARADO";
	public static File folder  = new File("../../data/");
	public static final String SEPARADOR = "$";
	public static final Integer TAMANIO_SEGMENTO = 1024;
	public static final String ERROR = "ERROR";
	public static final String ARCH = "ARCH";
	public static final String FINARCH = "FINARCH";
	public static final String RECIBIDO = "RECIBIDO";

	private Socket sc;
	private boolean aceptaArchs;
	private int tiempoMuerte;
	private static File archivo;
	private PoolThreads pool;

	public Protocol(Socket sc, boolean aceptaArchs, int tiempoMuerte, File archivo, PoolThreads pool) {
		this.sc = sc;
		this.aceptaArchs = aceptaArchs;
		this.tiempoMuerte = tiempoMuerte;
		Protocol.archivo = archivo;
		this.pool = pool;
	}

	public void run()
	{
		try 
		{
			sc.setSoTimeout(1000*tiempoMuerte);
			while(true)
			{
				if(aceptaArchs == true)
				{
					procesar(sc.getInputStream(), sc.getOutputStream());
					break;
				}
				else
				{
					//Duerme 5 segundos antes de validar si ya puede descargar el archivo
					Thread.sleep(5000);
					pool.seAceptan(aceptaArchs);
				}
			}
		} 
		catch (Exception e) 
		{
			// TODO: handle exception
		}
		finally
		{
			try 
			{
				if(aceptaArchs == true){
					pool.finSesionArchivos();
				}
				pool.finSesion();
				pool.notify();
				sc.close();
			} 
			catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	/**
	 * 
	 * @param leerDelCliente
	 * @param escribirleAlCliente
	 */
	public static void procesar(InputStream leerDelCliente , OutputStream escribirleAlCliente) {


		String preparado;
		try 
		{
			BufferedReader bf = new BufferedReader(new InputStreamReader(leerDelCliente));
			preparado = bf.readLine();
			if(preparado.equalsIgnoreCase(PREPARADO)) 
			{
				//				for ( File f: folder.listFiles()) 
				//				{
				//					archivosDisponibles += SEPARADOR;
				//					archivosDisponibles += f.getName();
				//				}
				//				escribirleAlCliente.write(archivosDisponibles.getBytes());
				//				String archivoSeleccionado = bf.readLine();
				//				String[] a = archivoSeleccionado.split(",");
				//				if(a.length == 1 ) {
				//					File archivoDeseado = null;
				//					if(archivosDisponibles.contains(archivoSeleccionado)) 
				//					{
				//						for(File f: folder.listFiles()) 
				//						{
				//							if(f.getName().equalsIgnoreCase(a[0].replace(SEPARADOR, "")))
				//							{
				//								archivoDeseado = f;
				//								break;
				//							}
				//						}
				File archivoDeseado = Protocol.archivo;
				if(archivoDeseado != null) 
				{
					//avisamos el nombre del archivo se mandara 
					escribirleAlCliente.write(archivoDeseado.getName().getBytes());

					byte[] mybytearray = new byte[TAMANIO_SEGMENTO];
					BufferedInputStream bis = new BufferedInputStream(new FileInputStream(archivoDeseado));

					//enviando archivo por trozos
					int bytesRead;
					while ((bytesRead = bis.read(mybytearray)) > 0) 
					{
						escribirleAlCliente.write(mybytearray,0, bytesRead);
					}
					escribirleAlCliente.write(FINARCH.getBytes());

					//hashing
					MessageDigest hash = MessageDigest.getInstance("SHA-256");
					hash.update(mybytearray);
					byte[] fileHashed = hash.digest();
					escribirleAlCliente.write(fileHashed);

					if(bf.readLine().equalsIgnoreCase(RECIBIDO)) {
						bf.close(); 
					}
					else {
						escribirleAlCliente.write(ERROR.getBytes());
					}
				}
				else {
					escribirleAlCliente.write(ERROR.getBytes());
				}

			}
			else {
				escribirleAlCliente.write(ERROR.getBytes());
				//esperar n clientes
			}
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


	}
}
