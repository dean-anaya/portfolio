Dean Parrish
COSC 439 Summer 2018
Project 3
ID: E01260622

1. Project Description
	-Data repository and echo service, relaying messages to and from client/server. The server takes an optional 5-digit port number, but otherwise will use a port number that is hard-coded into the code. In addition, the server takes two values "-g" and "-n" which must be prime numbers. Once the server is running it waits for a connection to the client. The client takes an optional 5-digit port number, an optional host location, and a required user name as parameters. These arguments are then passed to the server, where the port and host are verified to be matching with that of the server. Once connected, the server makes a new instance of the "ServerThread" class, hands the connection off to it, and then continues to be in a a 'waiting state', ready to accept new connections. With the new connection made, the server and client are connected, and any and all messages sent between the client and server will be encrypted using the one-time-pad method. Messages may be entered into the console of the client program. The user is then prompted for their user name (which is sent encrypted), which upon entering, a new text file "xy_chat.txt" is created (xy being the first two letters of the user name).

The values exchanged in order to perform the Diffie-Hellman key exchange, are sent from the client to the server using BufferedReader and PrintWriter, which are implemented in the "SendingThread" class. PrintWriter will send the information, while BufferedReader will read in the information. These messages get sent to the "ServerThread" class within the TCPServer program. The "ServerThread" class then performs it's own key exchange method. Once connection has been established, messages go through an encryption process that involves converting the message to an array of bytes, XORing each byte with the 1-Byte pad that has been calculated in the key exchange method, and then sends the messages via the DataOutputStream class. A clients 1-Byte pad and corresponding port number are added to a ConcurrentHashMap in the server class. The server thread then reads in the messages via a DataInputStream object (while the message is anything but "DONE"), decrypts them (reverse process of encryption, XORing bytes, and converting them to a string) and writes the messages to the aforementioned text file using a synchronized write() method (which implements a FileWriter object). The messages are then broadcasted to all other clients (after being encrypted once more), through a DataOutputStream object. The broadcasted messages are then received by the "ReceivingThread" class in the TCPClient program, and are once again decrypted. Once received, the messages are then printed to the client's screen.

Once the user is done entering messages, they can end the client program by entering "DONE"-- once entered, the program will return a status report, and terminate. The status report will show us the number of messages sent from the user, as well as the time the server was running (printed in hours, minutes, seconds, and milliseconds). Connections are then closed and the client program ends. The server program continues to wait for a connection until terminated manually.

2. How to compile and run
	1. Open terminal
	2. CD into dp_proj1
	3. Type "javac dp_TCPServer.java"
	4. Type "java dp_TCPServer" + "-p" followed by a number above 20000 (optional) + "-g" + "-n" both of which must be prime numbers (optional)
	5. Open new terminal window
	6. Type "javac dp_TCPClient.java"
	7. Type "java dp_TCPClient" + "-u" followed by a username (optional, but will be prompted for a username if not entered) + "-p" followed by an optional port number (above 20000) + "-h" followed by an optional host name (only localhost will work).
	8. (Optional) open up new terminal windows and repeat steps 2-7.
	9. Once connected, type a message and press the RETURN key
	10. Once finished, type "DONE" and press the RETURN key
	11. Client program has ended
	12. On server console, press CTRL + C to end server program

3. Conclusion
	-Because it was only one theme that I was implementing, the project went smoother than the ones before it, but I still found it challenging. I often struggled with the idea of converting bytes to String, reading in array of bytes, and XORing. I was able to show my messages encrypted and decrypted on both the client and server side, and that helped. I had to show that I was sending and receiving the right messages, and encrypting properly. I had more fun with this project than the previous ones, and I am happy with my work. I used some help from stackoverflow.com, but not much code itself, mainly information. The project took me a couple days in total. I took a night to implement the Diffie-Hellman key exchange method, and then it took me about two straight days and nights of coding to finish. The overall structure of the program stayed the same, however I broke the program up into as many methods as I could. It felt really good to organize it into methods, so I think I will try to do this as much as possible in the future.