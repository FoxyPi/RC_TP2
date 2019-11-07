import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

/** A really simple HTTP Client
 * 
 * @author Eu
 *
 */

public class GetFile{
	private static final int BUF_SIZE = 512;
	private static final String REQUEST_FORMAT = "GET %s HTTP/1.0\r\n" + "Host: %s\r\n" + "Range: bytes=%d-%d\r\n" + "User-Agent: X-RC2018\r\n\r\n";
	private static final int BLOCK_SIZE = 1048576; //1Mbyte
	private static int nextByte = 0;
	private static boolean done = false;
	private static Stats stats;

	static class TCPThread implements Runnable{
		private Socket sock;
		private int startByte, bytesRead;
		private String path, host;
		private RandomAccessFile fout;


		TCPThread(String host, int port, String path, RandomAccessFile fout) throws Exception{
			this.startByte = nextByte;
			nextByte += BLOCK_SIZE;
			this.sock = new Socket(host, port);
			this.path =  path == "" ? "/" : path;
			this.host = host;
			this.fout = fout;
			this.fout.seek(startByte);
			this.bytesRead = 0;
		}

		public void run() {
			try{
				for(;;){
					String answerLine;
					OutputStream out = sock.getOutputStream();
					InputStream in = sock.getInputStream();
		
					String request = String.format(REQUEST_FORMAT, this.path, this.host, this.startByte + this.bytesRead , this.startByte + BLOCK_SIZE);
					out.write(request.getBytes());
					
					System.out.println("\nSent:\n\n"+request);
					System.out.println("Got:\n");
					
					//Se tiver o codigo 416, entao pedimos um bloco que nao existe
					if (Http.parseHttpReply(answerLine = Http.readLine(in))[1].equals("416")){
						done = true;
						break;
					}
		
					//papa o resto do cabecalho
					while ( !answerLine.equals("") ) {
						System.out.println(answerLine);
						answerLine = Http.readLine(in);
					}
					
					int n;
					byte[] buffer = new byte[BUF_SIZE];
						while( (n = in.read(buffer) ) > 0 ) {
							this.bytesRead += n;
							fout.write(buffer, 0, n);
						}
					if(this.bytesRead == BLOCK_SIZE){
						this.startByte = nextByte;
						nextByte += BLOCK_SIZE;
						this.bytesRead = 0;
						this.fout.seek(this.startByte);
					}else

					stats.newRequest(bytesRead);
					
				}
			
				this.sock.close();
			
			}catch(IOException e){
				System.out.println("IOException occured");	
			}
		}

	}

	public static void main(String[] args) throws Exception {
		if ( args.length < 2) {
			System.out.println("Usage: java HttpClientDemo url_to_access1 url_to_access2 ... url_to_accessN output_filename");
			System.exit(0);
		}
		String fileName = args[args.length - 1];
		RandomAccessFile  fout = new RandomAccessFile(fileName, "rw");
		URL u;
		List<Thread> threads = new LinkedList<>();
		stats = new Stats();
		// Assuming URL of the form http://server-name/path ....
		for(int i = 0; i < args.length - 1; i++){
			u = new URL(args[i]);
			threads.add(new Thread(new TCPThread (u.getHost(), u.getPort(), u.getPath(), fout)));
			threads.get(threads.size() - 1).start();
		}

		while(!done);
		
		for(Thread thread : threads)
			thread.join();

		fout.close();
		stats.printReport();
	}
}
