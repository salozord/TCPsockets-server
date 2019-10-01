package tcpserver;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalTime;
import javax.xml.bind.DatatypeConverter;
public class Protocol implements Runnable{

	public static final String PREPARADO = "PREPARADO";
	public static File folder  = new File("./data/");
	public static final String SEPARADOR = "$";
	public static final Integer TAMANIO_SEGMENTO = 8192;
	public static final String ERROR = "ERROR";
	public static final String ARCH = "ARCH";
	public static final String FINARCH = "FINARCH";
	public static final String RECIBIDO = "RECIBIDO";
	public static final String NEW_LINE = "\n";
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
			//sc.setSoTimeout(1000*tiempoMuerte);
			while(true)
			{
				if(aceptaArchs == true)
				{

					procesar(sc.getInputStream(), sc.getOutputStream(), sc.hashCode());
					break;
				}
				else
				{
					//Duerme 5 segundos antes de validar si ya puede descargar el archivo
					Thread.sleep(5000);
//					this.aceptaArchs = pool.seAceptan(aceptaArchs);
					this.aceptaArchs = pool.seAceptan();
				}
			}
		} 
		catch (Exception e) 
		{
			// TODO: handle exception
			e.printStackTrace();
		}
		finally
		{
			try 
			{
				if(aceptaArchs == true){
					pool.finSesionArchivos();
				}
				pool.finSesion();
				sc.close();
				pool.notify();
			} 
			catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	/**
	 * Procesar el servicio
	 * @param leerDelCliente
	 * @param escribirleAlCliente
	 * @throws IOException 
	 */
	public static void procesar(InputStream leerDelCliente , OutputStream escribirleAlCliente, int codigoUnico) throws IOException 
	{
		FileWriter fw = new FileWriter(new File("./data/logs/"+codigoUnico+".log" ));
		try 
		{
			BufferedReader bf = new BufferedReader(new InputStreamReader(leerDelCliente));
			PrintWriter pw = new PrintWriter(new OutputStreamWriter(escribirleAlCliente), true);
			String preparado = bf.readLine();
			if(preparado.equalsIgnoreCase(PREPARADO)) 
			{
				LocalTime ld = LocalTime.now();
				fw.write(ld.toString()+"NUEVO CLIENTE " + codigoUnico + NEW_LINE );
				File archivoDeseado = Protocol.archivo;
				if(archivoDeseado != null) 
				{

					//avisamos el nombre del archivo se mandara 
					String header =  "NOMBRE" + SEPARADOR;
					ld = LocalTime.now();
					fw.write(ld.toString()+"CLIENTE " + codigoUnico + " ARCH " + archivoDeseado.getName() + NEW_LINE );
					header += archivoDeseado.getName();
					//String headerHex = DatatypeConverter.printHexBinary(header.getBytes());
					pw.println(header);

					byte[] mybytearray = new byte[TAMANIO_SEGMENTO];
					BufferedInputStream bis = new BufferedInputStream(new FileInputStream(archivoDeseado));
					BufferedInputStream bis2 = new BufferedInputStream(new FileInputStream(archivoDeseado));
					DataOutputStream dos =  new DataOutputStream(escribirleAlCliente);
					
					byte[] bytesEnteros = new byte[(int)archivoDeseado.length()];
					bis2.read(bytesEnteros, 0, (int)archivoDeseado.length());
					
					dos.writeLong(archivoDeseado.length());
					dos.flush();
					int n ;
					long sumaTam = 0;
					while (sumaTam < archivoDeseado.length() && ( n = bis.read(mybytearray)) != 1) 
					{
//						try
//						{
						dos.write(mybytearray,0, n);
						dos.flush();
						sumaTam += n;	
//							dos.flush();
//						}
//						catch(Exception e) {
//							break;
//						}
						//String hexy = DatatypeConverter.printHexBinary(mybytearray);
						//pw.println(hexy);
						//n++;
					}
					//dos.close();
					bis.close();
					bis2.close();
					ld = LocalTime.now();
					fw.write(ld.toString()+"CLIENTE " + codigoUnico + " ENVIADO ARCH " + archivoDeseado.getName() + NEW_LINE);
					//hashing
					MessageDigest hash = MessageDigest.getInstance("SHA-256");
					hash.update(bytesEnteros);
					byte[] fileHashed = hash.digest();
					//byte[] finA = (FINARCH + SEPARADOR).getBytes();

					String fin = (FINARCH + SEPARADOR)+ DatatypeConverter.printHexBinary(fileHashed);
					//					byte[] finArch = (FINARCH + SEPARADOR).getBytes() ;

					//
					//					ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
					//					outputStream.write(finArch);
					//					outputStream.write(fileHashed);


					ld = LocalTime.now();
					fw.write(ld.toString()+"CLIENTE " + codigoUnico + " ENVIADO HASH DEL ARCH " + archivoDeseado.getName() + NEW_LINE);


					//FINARCH$digest
					//escribirleAlCliente.write(outputStream.toByteArray());
					//					String finarch = DatatypeConverter.printHexBinary(outputStream.toByteArray());
					pw.println(fin);

					if(bf.readLine().equalsIgnoreCase(RECIBIDO)) {
						ld = LocalTime.now();
						fw.write(ld.toString()+"CLIENTE " + codigoUnico + " FIN CONEXION " + archivoDeseado.getName() + NEW_LINE );
						fw.close();

						bf.close(); 
					}
					else {
						ld = LocalTime.now();
						fw.write(ld.toString()+"CLIENTE " + codigoUnico + " NO LOGRO VERIFICAR INTEGRIDAD  " + archivoDeseado.getName() + NEW_LINE);
						fw.close();

						//escribirleAlCliente.write(ERROR.getBytes());
						//pw.println(ERROR);
						//String hexError = DatatypeConverter.printHexBinary(ERROR.getBytes());
						//pw.println(hexError);
					}
				}
				else {
					ld = LocalTime.now();
					fw.write(ld.toString()+"CLIENTE " + codigoUnico + " ARCH INEXISTENTE " + NEW_LINE );
					fw.close();

					//escribirleAlCliente.write(ERROR.getBytes());
					//pw.println(ERROR);
					//String hexError = DatatypeConverter.printHexBinary(ERROR.getBytes());
					//pw.println(hexError);
				}

			}
			else {
				LocalTime ld = LocalTime.now();
				fw.write(ld.toString()+"CLIENTE " + codigoUnico + " NO SIGUE EL PROTOCOLO " + NEW_LINE);
				fw.close();

				//escribirleAlCliente.write(ERROR.getBytes());
				//	pw.println(ERROR);
				//String hexError = DatatypeConverter.printHexBinary(ERROR.getBytes());
				//pw.println(hexError);
				//esperar n clientes
			}
		}
		catch (IOException e) {
			LocalTime ld = LocalTime.now();
			fw.write(ld.toString()+"CLIENTE " + codigoUnico + " ERROR " + e.getMessage() + NEW_LINE );
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			LocalTime ld = LocalTime.now();
			fw.write(ld.toString()+"CLIENTE " + codigoUnico + " ERROR " + e.getMessage() + NEW_LINE );
			e.printStackTrace();
		}

	}
}