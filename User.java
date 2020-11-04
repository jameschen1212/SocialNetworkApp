//package broadcast;

import java.io.DataInputStream;
import java.io.PrintStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;


public class User extends Thread {

	// The user socket
	private static Socket userSocket = null;
	// The output stream
	private static PrintStream output_stream = null;
	// The input stream
	private static BufferedReader input_stream = null;

	private static BufferedReader inputLine = null;
	private static boolean closed = false;
	private static String userName;
	private static ArrayList<String> friendReqs = new ArrayList<String>();

	public static void main(String[] args) {

		// The default port.
		int portNumber = 8000;
		// The default host.
		String host = "localhost";

		if (args.length < 2) {
			System.out
			.println("Usage: java User <host> <portNumber>\n"
					+ "Now using host=" + host + ", portNumber=" + portNumber);
		} else {
			host = args[0];
			portNumber = Integer.valueOf(args[1]).intValue();
		}

		/*
		 * Open a socket on a given host and port. Open input and output streams.
		 */
		try {
			userSocket = new Socket(host, portNumber);
			inputLine = new BufferedReader(new InputStreamReader(System.in));
			output_stream = new PrintStream(userSocket.getOutputStream());
			input_stream = new BufferedReader(new InputStreamReader(userSocket.getInputStream()));
		} catch (UnknownHostException e) {
			System.err.println("Don't know about host " + host);
		} catch (IOException e) {
			System.err.println("Couldn't get I/O for the connection to the host "
					+ host);
		}

		/*
		 * If everything has been initialized then we want to write some data to the
		 * socket we have opened a connection to on port portNumber.
		 */
		if (userSocket != null && output_stream != null && input_stream != null) {
			try {                
				/* Create a thread to read from the server. */
				new Thread(new User()).start();

				// Get user name and join the social net
				System.out.println("Enter UserName: ");
				userName = inputLine.readLine().trim();
				String join = "#join " + userName;
				output_stream.println(join);

				while (!closed) {
					String userMessage = new String();
					String userInput = inputLine.readLine().trim();
					if(userInput.equals("Exit")) {
						output_stream.println("#Bye");
					}
					else {
					// Read user input and send protocol message to server
						if(userInput.startsWith("@connect")) {
							String[] split = userInput.split("\\s",2);
							userMessage = "#friendme " + split[1];
							output_stream.println(userMessage);
						}
						else if(userInput.startsWith("@disconnect")) { //unfriends someone
							String[] split = userInput.split("\\s", 2);
							userMessage = "#unfriend " + split[1];
							output_stream.println(userMessage);
						}
						else if(userInput.startsWith("@friend")) { //If the person is on ur friend request list, u can add them
							String[] getUser = userInput.split("\\s", 2);
							if(friendReqs.contains(getUser[1])) {
							output_stream.println("#friends " + getUser[1]);
							friendReqs.remove(getUser[1]);
							}
						}
						else if(userInput.startsWith("@deny")) { //If the person is on ur friend request list, u can deny them
							String[] getUser = userInput.split("\\s",2);
							if(friendReqs.contains(getUser[1])) {
							output_stream.println("#DenyFriendRequest " + getUser[1]);
							friendReqs.remove(getUser[1]);
							}
						}
						
						else {
						userMessage = "#status " + userInput; //creates the message for the server
						output_stream.println(userMessage);
						}
					}
				}
				/*
				 * Close the output stream, close the input stream, close the socket.
				 */
			}catch (IOException e) {
				System.err.println("IOException:  " + e);
			}
		}
	}

	/*
	 * Create a thread to read from the server.
	 */
	public void run() {
		/*
		 * Keep on reading from the socket till we receive a Bye from the
		 * server. Once we received that then we want to break.
		 */
		String responseLine;
		
		try {
			while ((responseLine = input_stream.readLine()) != null) {

				// Display on console based on what protocol message we get from server.
				if(responseLine.startsWith("#welcome")) {
					//String[] words = responseLine.split("\\s", 2);
					System.out.println("Welcome " + userName +  " to our Social Media App. To leave enter Exit on a new line");
				}
				if(responseLine.startsWith("#busy")) {
					System.out.println("Server busy try again later");
				}
				//reads the response from the server
				if(responseLine.startsWith("#statusPosted")) { //If the server acknowledged the status post print success
					System.out.println("Your status was posted successfully");
				}
				if(responseLine.startsWith("#newuser")) {
					String[] words = responseLine.split("\\s", 2);
					System.out.println("New user " + words[1] + " has joined the network!");
					
				}
				if(responseLine.startsWith("#newStatus")) {
					String[] words = responseLine.split("\\s", 3);
					System.out.println("<" + words[1] +">" +  ": " + words[2]);
				}
				if(responseLine.startsWith("#Leave")) {
					String[] words = responseLine.split("\\s", 2);
					System.out.println(words[1] + " has left the network");
				}
				if(responseLine.startsWith("#friendme")) { // If they request to friend you, add their name to ur friend request list
					String[] words = responseLine.split("\\s",2);
					friendReqs.add(words[1]);
					System.out.println(words[1] + " has sent you a friend request");
				}
				if(responseLine.startsWith("#OKfriends")) {
					String[] words = responseLine.split("\\s",3);
					System.out.println(words[1] + " and " + words[2] + " are now friends");
				}
				if(responseLine.startsWith("#FriendRequestDenied")) {
					String[] words = responseLine.split("\\s",2);
					System.out.println(words[1] + " rejected your friend request");
				}
				
				if(responseLine.startsWith("#NotFriends")) {
					String[] words = responseLine.split("\\s", 3);
					System.out.println(words[1] + " and " + words[2] + " are no longer friends");
				}
				if(responseLine.startsWith("#Bye")) {
					System.out.println("Exiting...");
					break;
				}
				


			}
			closed = true;
			output_stream.close();
			input_stream.close();
			userSocket.close();
		} catch (IOException e) {
			System.err.println("IOException:  " + e);
		}
	}
}



