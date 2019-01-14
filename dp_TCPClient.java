// Client program
// File name: TCPClient.java
// Dean Parrish
// COSC 439 Summer 2018
// Project 3
// ID: E01260622

import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.nio.charset.Charset;
import java.util.Random;
import java.util.Scanner;
public class dp_TCPClient
{
	//Instance variables
	private static String userName = null;
	private static Scanner scan1;
	private static Scanner scan2;
	private static BigInteger g,n;
	private static BigInteger secretKey;
	private static byte oneBytePad;
	private static Socket link;
	private static boolean isFinished;

	public static void main(String[] args)
	{
		//Create variables for passed arguments
		int port = 22222;
		InetAddress host = null;
		try {
			// Get server IP-address
			host = InetAddress.getByName("localhost");
		} catch(UnknownHostException e) {
			System.out.println("Host ID not found!");
			System.exit(1);
		}
		scan1 = new Scanner(System.in);
		if(args.length == 0){
			//If no user name entered, prompt user to enter
			System.out.print("Please enter username: ");
			userName = scan1.nextLine();
		} else {
			for(int i = 0; i < args.length; i++){
				if(args[i].equals("-h")){
					try {
						// Get server IP-address
						host = InetAddress.getByName(args[i+1]);
					}
					catch(UnknownHostException e){
						System.out.println("Host ID not found!");
						System.exit(1);
					}
				}
				if(args[i].equals("-u")){
					//Get user name passed
					userName = args[i+1];
				}
				if(args[i].equals("-p")){
					//Get port number passed
					try {
						port = Integer.parseInt(args[i+1]);
					} catch(NumberFormatException e) {
						System.out.println("Port must be a 5 digit number, matching that of the server port");
						System.exit(1);
					}
				}
			}
		}
		scan2 = new Scanner(System.in);
		if(userName == (null)) {
			//If no user name entered (but other arguments were) prompt user to enter
			System.out.print("Please enter username: ");
			userName = scan1.nextLine();
		}
		//Run program
		run(host, port, userName);
	}

	private static void run(InetAddress host, int port, String userName)
	{
		link = null;
		try{
			// Establish a connection to the server
			link = new Socket(host,port);
			//Execute Diffie-Hellman key exchange
			keyExchange(link);
			//Get one byte pad
			getOneBytePad();
			//Pass user name to server
			sendUserName();
			//Create and start new threads
			new SendingThread(link).start();
			new ReceivingThread(link).start();
		} catch(IOException e) {
			e.printStackTrace();
		}
	}

	private static void getOneBytePad() {
		//Method to cast and convert one byte pad from secret key
		oneBytePad = (byte)(secretKey.intValue() & 0xFF);
		String binaryPad = String.format("%8s", Integer.toBinaryString(oneBytePad & 0xFF)).replace(' ', '0');
		System.out.println("One byte pad: " + binaryPad);
	}

	private static void keyExchange(Socket link) {
		//Method to compute Diffie-Hellman key exchange
		try {
			PrintWriter out = new PrintWriter(link.getOutputStream(),true); 
			BufferedReader in = new BufferedReader(new InputStreamReader(link.getInputStream())); 

			//Receive values g & n
			g = new BigInteger(in.readLine());
			n = new BigInteger(in.readLine());
			System.out.println("value g: " + g + ", value n: " + n);

			//Produce and send random value
			BigInteger randomValue = new BigInteger(128, new Random());
			out.println(randomValue);

			//Receive random value
			BigInteger receivedValue = new BigInteger(in.readLine());

			//Compute secret key
			secretKey = getSecretKey(receivedValue, randomValue);
			System.out.println("Secret key: " + secretKey.intValue());
		} catch(IOException e) {
			e.printStackTrace();
		}
	}

	private static void sendUserName() {
		//Method to send username to server
		try {
			DataOutputStream outStream = new DataOutputStream(link.getOutputStream());
			//Encrypt message
			byte[] encryptedMessage = encryptMessage(userName);
			//Write message
			outStream.write(encryptedMessage.length);
			outStream.write(encryptedMessage);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static byte[] encryptMessage(String message) {
		//Encryption method
		//Convert message to byte array
		byte[] messageBytes = message.getBytes();
		for(int i = 0; i < messageBytes.length; i++) {
			//XOR byte in byte array with oneBytePad
			messageBytes[i] = (byte) (messageBytes[i] ^ oneBytePad);
		}
		return messageBytes;
	}

	private static BigInteger getSecretKey(BigInteger receivedValue, BigInteger randomValue) {
		//Method to encrypt secret key
		BigInteger temp = g.modPow(receivedValue, n);
		BigInteger key = temp.modPow(randomValue, n);
		return key;
	}

	private static class ReceivingThread extends Thread{

		//Instance variables
		private Socket socket;

		//Constructor
		ReceivingThread(Socket socket){
			this.socket = socket;
		}

		public void run() {
			//Run method, printing received messages to client
			String message = receiveBytes();
			while (true)
			{
				//While loop to read in byte arrays sent from server
				System.out.print("\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b");
				System.out.println(message + "        ");
				if(isFinished == true) {
					break;
				}
				System.out.print("Enter message: ");
				//Restart read line
				message = receiveBytes();
			}
		}

		private String decryptMessage(byte[] messageBytes) {
			//Decryption method
			for(int i = 0; i < messageBytes.length; i++) {
				//XOR byte in byte array with oneBytePad
				messageBytes[i] = (byte) (messageBytes[i] ^ oneBytePad);
			}
			//Convert byte array to string
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
					//Decrypt bytes to String
					message = decryptMessage(messageBytes);
				} else {
					//Exit once connection is closed
					System.exit(0);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			return message;
		}
	}

	private static class SendingThread extends Thread{

		//Instance variables 
		private Socket socket;

		//Constructor
		SendingThread(Socket socket){
			this.socket = socket;
		}

		//Run method, taking input from client and sending to server
		public void run() {
			try {
				//Set up stream for keyboard entry
				BufferedReader userEntry = new BufferedReader(new InputStreamReader(System.in));

				//Get data from the user and send it to the server
				isFinished = false;
				String message;
				do{
					System.out.print("Enter message: ");
					message = userEntry.readLine();
					sendMessage(message);
				} while (!message.equals("DONE"));
				isFinished = true;
				
				//Receive final report and close connection
//				System.out.println("\n*** Information received from the server ***");
//				System.out.println("Server received " + (numMessages-1) + " messages");
//				System.out.println("Length of session: " + hours + "::" + minutes + "::" + seconds + "::" + milliseconds);
			} catch(IOException e) {
				e.printStackTrace();
			} finally {
				System.out.println("\n!!!!! Closing connection... !!!!!");
				scan1.close();
				scan2.close();
			}
		}
		private void sendMessage(String message) {
			//Method to send messages to server
			try {
				DataOutputStream out = new DataOutputStream(socket.getOutputStream());
				//Encrypt message
				byte[] encryptedMessage = encryptMessage(message);
				//Write message
				out.write(encryptedMessage.length);
				out.write(encryptedMessage);
			} catch(IOException e) {
				e.printStackTrace();
			}
		}
	}
}