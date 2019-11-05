import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URL;

/** A really simple HTTP Client
 * 
 * @author Eu
 *
 */

public class GetFile{
	private static final int BUF_SIZE = 512;
	private static final String REQUEST_FORMAT = "GET %s HTTP/1.0\r\n" + "Host: %s\r\n" + "Range: bytes=%d-%d\r\n" + "User-Agent: X-RC2018\r\n\r\n";
	private static final int BLOCK_SIZE = 1048576; //1Mbyte
	public static void main(String[] args) throws Exception {
		if ( args.length != 2 ) {
			System.out.println("Usage: java HttpClientDemo url_to_access output_filename");
			System.exit(0);
		}
        String url = args[0];
		String fileName = args[1];
		URL u = new URL(url);
		int offset = 0;
		Stats stats = new Stats();
		// Assuming URL of the form http://server-name/path ....
		int port = u.getPort() == -1 ? 80 : u.getPort();
		String path = u.getPath() == "" ? "/" : u.getPath();
        FileOutputStream fout = new FileOutputStream(fileName);
		
		for(;;){
			int bytesRead = offset;
			String answerLine;
			Socket sock = new Socket( u.getHost(), port );
			OutputStream out = sock.getOutputStream();
			InputStream in = sock.getInputStream();

			String request = String.format(REQUEST_FORMAT, path, u.getHost(),offset, offset + BLOCK_SIZE);
			out.write(request.getBytes());
			
			System.out.println("\nSent:\n\n"+request);
			System.out.println("Got:\n");
			
			//Se tiver o codigo 416, entao pedimos um bloco que nao existe
			if (Http.parseHttpReply(answerLine = Http.readLine(in))[1].equals("416"))
				break;

			//papa o resto do cabecalho
			while ( !answerLine.equals("") ) {
				System.out.println(answerLine);
				answerLine = Http.readLine(in);
			}
			
			int n;
			byte[] buffer = new byte[BUF_SIZE];
			while( (n = in.read(buffer) ) > 0 ) {
				offset += n;
				fout.write(buffer, 0, n);
			}
			stats.newRequest(offset - bytesRead);

			sock.close();
		}
		fout.close();
		stats.printReport();
	}

}
