import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

/** Parallel Download HTTP Client
 * 
 * @author David Pereira(52890) // Filipe Jose (53675)
 *
 */

public class GetFile{
	private static final int BUF_SIZE = 512;
	private static final String REQUEST_FORMAT = 
		"GET %s HTTP/1.0\r\n" + 
		"Host: %s\r\n" + 
		"Range: bytes=%d-%d\r\n" + 
		"User-Agent: X-RC2018\r\n\r\n";
	private static int BLOCK_SIZE = 500 * 1024;
	private static int nextByte = 0;//used by threads to ge a new block to work on
	private static Stats stats;

	//Synched because otherwise concurrency would make it go bananas
	private static synchronized int getNextByte(){
		int synchByte = nextByte;
		nextByte += BLOCK_SIZE;
		return synchByte;
	}

	//Each thread takes cares of one "block". When it's done with the block
	//it retreives a new assignment from getNextByte()
	static class TCPThread implements Runnable{
		private Socket sock;
		private int startByte, bytesRead, port;
		private String path, host;
		private RandomAccessFile fout;


		TCPThread(String host, int port, String path, String filename) throws Exception{
			this.startByte = nextByte;
			nextByte += BLOCK_SIZE;
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

			if (Http.parseHttpReply(answerLine = Http.readLine(in))[1].equals("416"))
				error = true;

			while ( !answerLine.equals("") )
				answerLine = Http.readLine(in);
			
			return error;
		}

		//writes the contents of in to the file.
		//@pre: in has already had its header consumed
		private void processWrite(InputStream in) throws Exception{
			int n;
			byte[] buffer = new byte[BUF_SIZE];
				while( (n = in.read(buffer) ) > 0 ) {
					this.bytesRead += n;
					fout.write(buffer, 0, n);
				}
		}

		public void run() {
			try{
				for(;;){
					int bytesMem = this.bytesRead;
					this.sock = new Socket(this.host, this.port);
					OutputStream out = sock.getOutputStream();
					InputStream in = sock.getInputStream();
					String request = String.format(REQUEST_FORMAT, this.path, this.host, this.startByte + this.bytesRead , this.startByte + BLOCK_SIZE - 1);
					out.write(request.getBytes());
					
					if(this.processHeader(in))
						break;
					
					this.processWrite(in);
					
					//Did we do well?
					stats.newRequest(this.bytesRead - bytesMem);
					
					//if we are finished with this block
					if(this.bytesRead >= BLOCK_SIZE){
						this.startByte = getNextByte();
						this.bytesRead = 0;
						this.fout.seek(this.startByte);
					}
					this.sock.close();
				}
				this.fout.close();
			}catch(Exception e){
				System.out.println("Uh oh, something went wrong...");	
			}
		}

	}

	public static void main(String[] args) throws Exception {
		if ( args.length < 2) {
			System.out.println("Usage: java GetFile block_size url_to_access1 url_to_access2 ... url_to_accessN output_filename");
			System.exit(0);
		}
		BLOCK_SIZE = Integer.parseInt(args[0]);
		String fileName = args[args.length - 1];
		URL u;
		List<Thread> threads = new LinkedList<>();
		stats = new Stats();

		//Throw the babies
		for(int i = 1; i < args.length - 1; i++){
			u = new URL(args[i]);
			threads.add(new Thread(new TCPThread (u.getHost(), u.getPort(), u.getPath(), fileName)));
			threads.get(threads.size() - 1).start();
		}
				
		//Wait for the babies
		for(Thread thread : threads)
			thread.join();

		stats.printReport();
	}
}