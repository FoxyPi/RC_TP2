import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

/** Parallel Download HTTP Client
 * 
 * @author David Pereira(52890) // Filipe Jose (53277)
 *
 */

public class GetFile{
	private static final int BUF_SIZE = 512;
	private static final String REQUEST_FORMAT = 
		"GET %s HTTP/1.0\r\n" + 
		"Host: %s\r\n" + 
		"Range: bytes=%d-%d\r\n" + 
		"User-Agent: X-RC2018\r\n\r\n";
	private static int BLOCK_SIZE = 8 * 1024;//8Kbytes is the optimal size found by our python script
	private static int nextByte = 0;//used by threads to ge a new block to work on
	private static Stats stats;

	//Synched because otherwise concurrency would make it go bananas
	private static synchronized int getNextByte(){
		int synchByte = nextByte;
		nextByte += BLOCK_SIZE;
		return synchByte;
	}

	//Synched because otherwise concurrency would make the stats go bananas
	private static synchronized void processStats(int bytes){
		stats.newRequest(bytes);
	}

	//Each thread takes cares of one "block". When it's done with the block
	//it retreives a new assignment from getNextByte()
	static class TCPThread implements Runnable{
		private Socket sock;
		private int startByte;//Representes the starting byte of the current range
		private int bytesRead;//Number of bytes read in the current range. 
							  //Resets to 0 when acquiring a new range
		private int port;
		private String path, host;
		private RandomAccessFile fout;


		TCPThread(String host, int port, String path, String filename) throws Exception{
			this.startByte = getNextByte();
			this.path =  path == "" ? "/" : path;
			this.port = port;
			this.host = host;
			this.fout = new RandomAccessFile(filename, "rw");
			this.fout.seek(startByte);
			this.bytesRead = 0;
		}

		// Returns true if its time to stop. Also consumes rest of header
		private boolean processHeader(InputStream in) throws Exception{
			String answerLine;
			boolean error = false;

			if (!Http.parseHttpReply(answerLine = Http.readLine(in))[1].equals("206"))
				error = true;

			while ( !answerLine.equals("") )
				answerLine = Http.readLine(in);
			
			return error;
		}

		/**
		 * Writes the reply's payload to the file
		 * Returns the number of bytes received in the reply
		 */
		private int processWrite(InputStream in) throws Exception{
			int n;
			int startingBytes = this.bytesRead;
			byte[] buffer = new byte[BUF_SIZE];
				while( (n = in.read(buffer) ) > 0 ) {
					this.bytesRead += n;
					fout.write(buffer, 0, n);
				}
			
			return this.bytesRead - startingBytes;
		}

		public void run() {
			try{
				for(;;){

					//----------------SOCKET-------------------------
					this.sock = new Socket(this.host, this.port);
					OutputStream out = sock.getOutputStream();
					InputStream in = sock.getInputStream();

					//---------------Send Request--------------------
					String request = String.format(REQUEST_FORMAT, this.path, this.host, this.startByte + this.bytesRead , this.startByte + BLOCK_SIZE - 1);
					out.write(request.getBytes());
					
					//---------------Process Reply-------------------
					if(this.processHeader(in))
						break;
					
					processStats(this.processWrite(in));
										
					//if we are finished with this block
					if(this.bytesRead >= BLOCK_SIZE){
						this.startByte = getNextByte();
						this.bytesRead = 0;
						this.fout.seek(this.startByte);
					}

					this.sock.close();
				}
				this.sock.close();
				this.fout.close();
			}catch(Exception e){
				System.out.println("Uh oh, something went wrong...");	
			}
		}

	}

	public static void main(String[] args) throws Exception {
		if ( args.length < 1) {
			System.out.println("Usage: java GetFile url_to_access1 url_to_access2 ... url_to_accessN");
			System.exit(0);
		}
		String fileName = ("copy");
		String path;
		URL u;
		int port;
		List<Thread> threads = new LinkedList<>();
		stats = new Stats();

		//Throw the workers
		for(String arg : args) {
			u = new URL(arg);
			port = u.getPort() == -1 ? 80 : u.getPort();
			path = u.getPath() == "" ? "/" : u.getPath();
			threads.add(new Thread(new TCPThread (u.getHost() ,port ,path , fileName)));
			threads.get(threads.size() - 1).start();
		}
				
		//Wait for the workers
		for(Thread thread : threads)
			thread.join();

		stats.printReport();
	}
}