import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URL;

/** A really simple HTTP Client
 * 
 * @author The RC - FCT/UNL class instructors
 *
 */

public class HttpClientDemo {
	private static final int BUF_SIZE = 512;
	
	public static void main(String[] args) throws Exception {
		if ( args.length != 1 ) {
			System.out.println("Usage: java HttpClientDemo url_to_access");
			System.exit(0);
		}
		String url = args[0];
		URL u = new URL(url);
		// Assuming URL of the form http://server-name:port/path ....
		int port = u.getPort() == -1 ? 80 : u.getPort();
		String path = u.getPath() == "" ? "/" : u.getPath();
		Socket sock = new Socket( u.getHost(), port );
		OutputStream out = sock.getOutputStream();
		InputStream in = sock.getInputStream();
		
		String request = String.format(
				"GET %s HTTP/1.0\r\n"+
				"Host: %s\r\n"+
				"User-Agent: X-Client-Demo-RC2019\r\n\r\n", path, u.getHost());
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
		
		System.out.println("\nPayload:\n");
		int n;
		byte[] buffer = new byte[BUF_SIZE];
		while( (n = in.read(buffer)) >= 0 ) {
			System.out.print(new String(buffer,0,n) );
		}
		sock.close();
	}

}
