// Server program  
// File name: "TCPServer.java"
// Dean Parrish
// COSC 439 Summer 2018
// Project 3
// ID: E01260622
import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class dp_TCPServer
{
	//Instance variables
	private static ConcurrentHashMap<Byte,Socket> socketMap;
	private static ServerSocket servSock;
	private static AtomicInteger count = new AtomicInteger();
	private static String fileName = null;
	private static int currentConnections;
	private static FileWriter fileWriter;
	private static File file = null;
	private static Integer g,n;


	public static void main(String[] args) throws IOException
	{
		//Instantiate ConcurrentHashMap
		socketMap = new ConcurrentHashMap<Byte,Socket>();
		//Hard coded port number
		Integer port = 22222;
		//Hard coded integers for key exchange
		g = 555;
		n = 933;
		System.out.println("Opening port...\n");
		// Create a server object
		try{
			//If no port given, plug in hard coded port
			servSock = new ServerSocket(port); 
			for(int i = 0; i < args.length; i++) {
				if(args[i].equals("-p")) {
					//Plug in port that was passed
					servSock = new ServerSocket(Integer.parseInt(args[i+1]));
				}
				if(args[i].equals("-n")) {
					//Plug in n value that was passed
					if(isPrime(Integer.parseInt(args[i+1])) == false) {
						//Check for prime
						System.out.println("n must be a number greater than 1, and prime.");
						System.exit(0);
					} else {
						n = Integer.parseInt(args[i+1]);
					}
				}
				if(args[i].equals("-g")) {
					//Plug in g value that was passed
					if(isPrime(Integer.parseInt(args[i+1])) == false) {
						//Check for prime
						System.out.println("g must be a number greater than 1, and prime.");
						System.exit(0);
					} else {
						g = Integer.parseInt(args[i+1]);
					}				
				}
			}
		} catch(IOException e){
			System.out.println("Unable to attach to port!");
			System.exit(1);
		} catch(NumberFormatException e) {
			System.out.println("Can only pass numbers as arguments for port, g, n");
			System.exit(1);
		}
		System.out.println("g value: " + g);
		System.out.println("n value: " + n);
		//Run program
		do {
			run();
		} while (true);

	}

	private static void run() throws IOException
	{
		//Method to run server, relaying information to and from client program
		Socket link = null;
		//Allows for new connections
		while(true) {
			// Put the server into a waiting state
			link = servSock.accept();
			//Create and start new thread
			new ServerThread(link).start();
		}
	}

	private static boolean isPrime(int num) {
		//Method to check if g and n values are prime numbers
		if(num == 2) {
			return true;
		}
		if(num % 2 == 0) {
			return false;
		} else {
			return true;
		}
	}

	private static class ServerThread extends Thread{

		//Instance variables
		private Socket socket;
		private String userName;
		private BigInteger secretKey;
		private byte oneBytePad;
		private int numMessages;

		//Constructor
		ServerThread(Socket socket){
			this.socket = socket;
		}

		//Run method writes messages to chat file, sends messages back to client
		public void run() {
			try {				
				//Once connection is made, increment counting variable, displaying number of current connections
				currentConnections = count.incrementAndGet();
				System.out.println("Active connections: " + currentConnections);
				//Execute Diffie-Hellman key exchange
				keyExchange(socket);
				//Get oneBytePad
				getOneBytePad();
				//Display connection host
				String host = InetAddress.getLocalHost().getHostName();
				System.out.println("Client has estabished a connection to " + host);
				//Start timer
				long startTime = System.currentTimeMillis();
				// Receive user name
				userName = receiveBytes();
				//Add user & corresponding port number to ConcurrentHashMap
				socketMap.put(oneBytePad, socket);
				//Broadcast joining of chat to other users
				broadcast("*** " + userName + " has joined the chat room ***");
				//Create chat file
				createFile();
				//Log and broadcast past chat
				sendChat();
				//Instantiate fileWriter
				fileWriter = new FileWriter(file.getAbsoluteFile(), true);
				//Write to file: user has joined chat room
				write("*** " + userName + " has joined the chat room ***");
				String message = receiveBytes();
				while (!message.equals("DONE"))
				{
					//While loop to read in messages, write to file, and broadcast to clients
					System.out.println(message);
					write(userName + ": " + message);
					broadcast(userName + ": " + message);
					numMessages++;
					message = receiveBytes();
				}
				//Stop timer
				long endTime = System.currentTimeMillis();
				//Variables to measure time elapsed
				long totalTime = endTime - startTime;
				long milliseconds = totalTime % 1000;
				long seconds = (totalTime / 1000) % 60;
				long minutes = (totalTime / 60000) % 60;
				long hours = (totalTime / 3600000) % 60;
				
				//Broadcast server info back to client
				try {
					DataOutputStream outStream = new DataOutputStream(socket.getOutputStream());
					//Encrypt and send message
					byte[] encryptedMessage = encryptMessage("\n*** Information received from the server ***" + "\nServer received " + (numMessages) + " messages" + "\nLength of session: " + hours + "::" + minutes + "::" + seconds + "::" + milliseconds);
					outStream.write(encryptedMessage.length);
					outStream.write(encryptedMessage);
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				
				//Writes when user has left chat room
				write("*** " + userName + " has left the chat room ***");
				//Broadcasts that user has left chat room
				broadcast("*** " + userName + " has left the chat room ***");
			}  catch(IOException e){
				e.printStackTrace();
			} finally {
				try {
					System.out.println("!!!!! Closing connection... !!!!!\n" + "!!! Waiting for the next connection... !!!");
					//Removes element from ConcurrentHashMap
					socketMap.remove(oneBytePad);
					//Close socket
					socket.close();
					//Decrement currentConnections
					currentConnections = count.decrementAndGet();
					System.out.println("Active connections: " + currentConnections);
					if(currentConnections == 0 && file != null) {
						//Delete file
						file.delete();
					}
				} catch(IOException e) {
					System.out.println("Unable to disconnect!");
					System.exit(1);
				}
			}
		}

		//Synchronized writing method
		private synchronized void write(String message) {
			try {
				fileWriter.write(message);
				fileWriter.write("\r\n");
				fileWriter.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		//Computes and returns secret key
		private BigInteger getSecretKey(BigInteger receivedValue, BigInteger randomValue) {
			//Convert g and n to BigInteger
			BigInteger newG = BigInteger.valueOf(g.intValue());
			BigInteger newN = BigInteger.valueOf(n.intValue());

			BigInteger temp = newG.modPow(receivedValue, newN);
			BigInteger key = temp.modPow(randomValue, newN);
			return key;
		}

		private void broadcast(String message) {
			//Broadcasts joining of chat to other users
			//Keep this threads oneBytePad
			byte temp = oneBytePad;
			for(Map.Entry<Byte,Socket> entry : socketMap.entrySet()) {
				//For loop allows for broadcast to other clients via their sockets
				if(!(entry.getValue().equals(this.socket))) {
					try {
						DataOutputStream outStream = new DataOutputStream(entry.getValue().getOutputStream());
						//Match oneBytePad of client with this server thread for proper encryption
						oneBytePad = entry.getKey();
						//Encrypt and send message
						byte[] encryptedMessage = encryptMessage(message);
						outStream.write(encryptedMessage.length);
						outStream.write(encryptedMessage);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			//Restore this threads oneBytePad
			oneBytePad = temp;
		}

		private void createFile() {
			//Creates chat file
			if(currentConnections == 1) {
				fileName = (userName.substring(0, 2) + "_chat.txt");
				file = new File(fileName);
				try {
					file.createNewFile();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		private void sendChat() {
			//Method to send past chat log to new client connection
			try {
				BufferedReader fileIn = new BufferedReader(new FileReader(fileName));
				DataOutputStream outStream = new DataOutputStream(socket.getOutputStream());
				//Log and broadcast past chat
				String line;
				while((line = fileIn.readLine()) != null && file != null) {
					//Encrypt and send each line from the file as a message
					byte[] encryptedMessage = encryptMessage(line);
					outStream.write(encryptedMessage.length);
					outStream.write(encryptedMessage);
				}
				fileIn.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		private byte[] encryptMessage(String message) {
			//Encryption method
			//Convert string to byte array
			byte[] messageBytes = message.getBytes();
			for(int i = 0; i < messageBytes.length; i++) {
				//XOR with oneBytePad
				messageBytes[i] = (byte) (messageBytes[i] ^ oneBytePad);
			}
			return messageBytes;
		}

		private String decryptMessage(byte[] messageBytes) {
			//Decryption method
			for(int i = 0; i < messageBytes.length; i++) {
				//XOR with oneBytePad
				messageBytes[i] = (byte) (messageBytes[i] ^ oneBytePad);
			}
			//Convert to string and return
			return new String(messageBytes, Charset.defaultCharset());
		}

		private String receiveBytes() {
			//Method to receive byte array from server
			String message = "";
			try {
				DataInputStream inStream = new DataInputStream(socket.getInputStream());
				//Read in length first to check for array size
				int length = inStream.read();
				if(length > 0) {
					byte[] messageBytes = new byte[length];
					//Fully reads in byte array
					inStream.readFully(messageBytes, 0, length);
					//Decrypt byte array
					message = decryptMessage(messageBytes);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			return message;
		}

		private void getOneBytePad() {
			//Method to cast and convert one byte pad from secret key
			oneBytePad = (byte)(secretKey.intValue() & 0xFF);
			String binaryPad = String.format("%8s", Integer.toBinaryString(oneBytePad & 0xFF)).replace(' ', '0');
			System.out.println("one byte pad: " + binaryPad);
		}

		private void keyExchange(Socket link) {
			try {
				//Buffered reader for reading in user name
				BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream())); 
				//Instantiate PrintWriter for sending messages to client
				PrintWriter out = new PrintWriter(socket.getOutputStream(),true);

				//Send values g & n
				out.println(g);
				out.println(n);

				//Produce and send random value
				BigInteger randomValue = new BigInteger(128, new Random());
				out.println(randomValue);

				//Receive random value
				BigInteger receivedValue = new BigInteger(in.readLine());

				//Compute secret key
				secretKey = getSecretKey(receivedValue, randomValue);
				System.out.println("Secret key: " + secretKey);
			} catch(IOException e) {
				e.printStackTrace();
			}
		}
	}
}
