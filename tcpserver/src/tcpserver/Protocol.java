package tcpserver;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
public class Protocol {

	public static final String PREPARADO = "PREPARADO";
	public static File folder  = new File("../files/");
	public static final String SEPARADOR = "$";
	public static final Integer TAMANIO_SEGMENTO = 1024;
	public static final String ERROR = "ERROR";
	public static final String ARCH = "ARCH";
	public static final String FINARCH = "FINARCH";
	public static final String RECIBIDO = "RECIBIDO";



	public Protocol() {
	}
	public static void procesar(BufferedReader leerDelCliente , OutputStream escribirleAlCliente) {


		String preparado;
		try {
			preparado = leerDelCliente.readLine();
			String archivosDisponibles = "";
			if(preparado.equalsIgnoreCase(PREPARADO)) {
				for ( File f: folder.listFiles()) {
					archivosDisponibles += SEPARADOR;
					archivosDisponibles += f.getName();
				}
				escribirleAlCliente.write(archivosDisponibles.getBytes());
				String archivoSeleccionado = leerDelCliente.readLine();
				String[] a = archivoSeleccionado.split(",");
				if(a.length == 1 ) {
					File archivoDeseado = null;
					if(archivosDisponibles.contains(archivoSeleccionado)) {
						for(File f: folder.listFiles()) {
							if(f.getName().equalsIgnoreCase(a[0].replace(SEPARADOR, ""))) {
								archivoDeseado = f;
								break;
							}
						}
						if(archivoDeseado != null) 
						{
							//avisamos que el archivo se mandara 
							escribirleAlCliente.write(ARCH.getBytes());
							
							byte[] mybytearray = new byte[TAMANIO_SEGMENTO];
							BufferedInputStream bis = new BufferedInputStream(new FileInputStream(archivoDeseado));

							//enviando archivo por trozos
							int bytesRead;
							while ((bytesRead = bis.read(mybytearray)) > 0) {
								escribirleAlCliente.write(mybytearray,0, bytesRead);
							}
							escribirleAlCliente.write(FINARCH.getBytes());
							
							//hashing
							MessageDigest hash = MessageDigest.getInstance("SHA-256");
							hash.update(mybytearray);
							byte[] fileHashed = hash.digest();
							escribirleAlCliente.write(fileHashed);
							
							if(leerDelCliente.readLine().equalsIgnoreCase(RECIBIDO)) {
								leerDelCliente.close(); 
							}
							else {
								escribirleAlCliente.write(ERROR.getBytes());
							}
						}
						else {
							escribirleAlCliente.write(ERROR.getBytes());
						}

					}
				}
				else {
					//esperar n clientes
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


	}
}
