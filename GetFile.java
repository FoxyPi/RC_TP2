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
	public static void main(String[] args) throws Exception {
		if ( args.length != 2 ) {
			System.out.println("Usage: java HttpClientDemo url_to_access output_filename");
			System.exit(0);
		}
        String url = args[0];
        String fileName = args[1];
		URL u = new URL(url);
		// Assuming URL of the form http://server-name/path ....
		int port = u.getPort() == -1 ? 80 : u.getPort();
		String path = u.getPath() == "" ? "/" : u.getPath();
        Socket sock = new Socket( u.getHost(), port );
        OutputStream out = sock.getOutputStream();
        FileOutputStream fout = new FileOutputStream(fileName);
		InputStream in = sock.getInputStream();
		
		String request = String.format(
				"GET %s HTTP/1.0\r\n"+
				"Host: %s\r\n"+
				"User-Agent: X-RC2018\r\n\r\n", path, u.getHost());
		out.write(request.getBytes());
		
		System.out.println("\nSent:\n\n"+request);
		System.out.println("Got:\n");
		
		String answerLine = Http.readLine(in);  // first line is always present
		System.out.println(answerLine);
		answerLine = Http.readLine(in);
		while ( !answerLine.equals("") ) {
			System.out.println(answerLine);
			answerLine = Http.readLine(in);
		}
		
		System.out.println("\nPayload:\n See file teste.HTML");
		int n;
		byte[] buffer = new byte[BUF_SIZE];
		while( (n = in.read(buffer) ) > 0 ) {
            fout.write(buffer, 0, n);
        }
        fout.close();
		sock.close();
	}

}
