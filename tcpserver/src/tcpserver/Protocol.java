package tcpserver;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
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
import java.time.LocalDate;

import javax.xml.bind.DatatypeConverter;
public class Protocol extends Thread{

	public static final String PREPARADO = "PREPARADO";
	public static File folder  = new File("./data/");
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
					procesar(sc.getInputStream(), sc.getOutputStream(), sc.hashCode());
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
	 * Procesar el servicio
	 * @param leerDelCliente
	 * @param escribirleAlCliente
	 * @throws IOException 
	 */
	public static void procesar(InputStream leerDelCliente , OutputStream escribirleAlCliente, int codigoUnico) throws IOException 
	{
		FileWriter fw = new FileWriter(new File("./data/"+codigoUnico+".txt" ));
		String preparado;
		try 
		{
			BufferedReader bf = new BufferedReader(new InputStreamReader(leerDelCliente));
			PrintWriter pw = new PrintWriter(new OutputStreamWriter(escribirleAlCliente));
			preparado = bf.readLine();
			if(preparado.equalsIgnoreCase(PREPARADO)) 
			{
				LocalDate ld = LocalDate.now();
				fw.write(ld.toString()+"NUEVO CLIENTE " + codigoUnico );
				File archivoDeseado = Protocol.archivo;
				if(archivoDeseado != null) 
				{

					//avisamos el nombre del archivo se mandara 
					String header =  "NOMBRE" + SEPARADOR;
					ld = LocalDate.now();
					fw.write(ld.toString()+"CLIENTE " + codigoUnico + " ARCH " + archivoDeseado.getName() );
					header += archivoDeseado.getName().getBytes();
					String headerHex = DatatypeConverter.printHexBinary(header.getBytes());
					pw.write(headerHex);

					byte[] mybytearray = new byte[TAMANIO_SEGMENTO];
					BufferedInputStream bis = new BufferedInputStream(new FileInputStream(archivoDeseado));
					byte[] bytesEnteros = new byte[(int)archivoDeseado.length()];
					bis.read(bytesEnteros);
					//enviando archivo por trozos
					int bytesRead;
					while ((bytesRead = bis.read(mybytearray)) > 0) 
					{
						//escribirleAlCliente.write(mybytearray,0, bytesRead);
						String hexy = DatatypeConverter.printHexBinary(mybytearray);
						pw.write(hexy);
					}
					
					bis.close();
					ld = LocalDate.now();
					fw.write(ld.toString()+"CLIENTE " + codigoUnico + " ENVIADO ARCH " + archivoDeseado.getName() );
					//hashing
					MessageDigest hash = MessageDigest.getInstance("SHA-256");
					hash.update(bytesEnteros);
					byte[] fileHashed = hash.digest();
					byte[] finArch = (FINARCH + SEPARADOR).getBytes() ;


					ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
					outputStream.write(finArch);
					outputStream.write(fileHashed);
					

					ld = LocalDate.now();
					fw.write(ld.toString()+"CLIENTE " + codigoUnico + " ENVIADO HASH DEL ARCH " + archivoDeseado.getName() );


					//FINARCH$digest
					//escribirleAlCliente.write(outputStream.toByteArray());
					String finarch = DatatypeConverter.printHexBinary(outputStream.toByteArray());
					pw.write(finarch);

					if(bf.readLine().equalsIgnoreCase(RECIBIDO)) {
						ld = LocalDate.now();
						fw.write(ld.toString()+"CLIENTE " + codigoUnico + " FIN CONEXION " + archivoDeseado.getName() );

						bf.close(); 
					}
					else {
						ld = LocalDate.now();
						fw.write(ld.toString()+"CLIENTE " + codigoUnico + " NO LOGRO VERIFICAR INTEGRIDAD  " + archivoDeseado.getName() );
						escribirleAlCliente.write(ERROR.getBytes());
						String hexError = DatatypeConverter.printHexBinary(ERROR.getBytes());
						pw.write(hexError);
					}
				}
				else {
					ld = LocalDate.now();
					fw.write(ld.toString()+"CLIENTE " + codigoUnico + " ARCH INEXISTENTE " );
					escribirleAlCliente.write(ERROR.getBytes());
					String hexError = DatatypeConverter.printHexBinary(ERROR.getBytes());
					pw.write(hexError);
				}

			}
			else {
				LocalDate ld = LocalDate.now();
				fw.write(ld.toString()+"CLIENTE " + codigoUnico + " NO SIGUE EL PROTOCOLO ");
				escribirleAlCliente.write(ERROR.getBytes());
				String hexError = DatatypeConverter.printHexBinary(ERROR.getBytes());
				pw.write(hexError);
				//esperar n clientes
			}
		}
		catch (IOException e) {
			LocalDate ld = LocalDate.now();
			fw.write(ld.toString()+"CLIENTE " + codigoUnico + " ERROR " + e.getMessage() );
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			LocalDate ld = LocalDate.now();
			fw.write(ld.toString()+"CLIENTE " + codigoUnico + " ERROR " + e.getMessage() );
			e.printStackTrace();
		}


	}
}