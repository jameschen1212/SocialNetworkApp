//package broadcast;

import java.io.DataInputStream;
import java.io.PrintStream;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.ServerSocket;
import java.util.*;



/*
 * A server that delivers status messages to other users.
 */
public class Server {

	// Create a socket for the server 
	private static ServerSocket serverSocket = null;
	// Create a socket for the server 
	private static Socket userSocket = null;
	// Maximum number of users 
	private static int maxUsersCount = 5;
	// An array of threads for users
	private static userThread[] threads = null;


	public static void main(String args[]) {

		// The default port number.
		int portNumber = 58531;
		if (args.length < 2) {
			System.out.println("Usage: java Server <portNumber>\n"
					+ "Now using port number=" + portNumber + "\n" +
					"Maximum user count=" + maxUsersCount);
		} else {
			portNumber = Integer.valueOf(args[0]).intValue();
			maxUsersCount = Integer.valueOf(args[1]).intValue();
		}

		System.out.println("Server now using port number=" + portNumber + "\n" + "Maximum user count=" + maxUsersCount);
		
		
		userThread[] threads = new userThread[maxUsersCount];


		/*
		 * Open a server socket on the portNumber (default 8000). 
		 */
		try {
			serverSocket = new ServerSocket(portNumber);
		} catch (IOException e) {
			System.out.println(e);
		}

		/*
		 * Create a user socket for each connection and pass it to a new user
		 * thread.
		 */
		while (true) {
			try {
				userSocket = serverSocket.accept();
				int i = 0;
				for (i = 0; i < maxUsersCount; i++) {
					if (threads[i] == null) {
						threads[i] = new userThread(userSocket, threads);
						threads[i].start();
						break;
					}
				}
				if (i == maxUsersCount) {
					PrintStream output_stream = new PrintStream(userSocket.getOutputStream());
					output_stream.println("#busy");
					output_stream.close();
					userSocket.close();
				}
			} catch (IOException e) {
				System.out.println(e);
			}
		}
	}
}

/*
 * Threads
 */
class userThread extends Thread {

	private String userName = null;
	private BufferedReader input_stream = null;
	private PrintStream output_stream = null;
	private Socket userSocket = null;
	private final userThread[] threads;
	private int maxUsersCount;
	private ArrayList<userThread> friendlist = new ArrayList<userThread>(); //Storing ur friends list

	public userThread(Socket userSocket, userThread[] threads) {
		this.userSocket = userSocket;
		this.threads = threads;
		maxUsersCount = threads.length;
	}

	public void run() {
		int maxUsersCount = this.maxUsersCount;
		userThread[] threads = this.threads;

		try {
			/*
			 * Create input and output streams for this client.
			 * Read user name.
			 */
			input_stream = new BufferedReader(new InputStreamReader(this.userSocket.getInputStream()));
			output_stream = new PrintStream(this.userSocket.getOutputStream());
			String line = input_stream.readLine();//stores the input from User

			/* Welcome the new user. */
			if(line.startsWith("#join")) {
				String[] username = line.split("\\s", 2);
				output_stream.println("#welcome " + username[1]);
				userName = username[1];
				synchronized (userThread.class) {
				for(int i = 0; i < maxUsersCount; i++) {
					if(threads[i] != this && threads[i] != null) {
						//PrintStream output_stream_user = new PrintStream(threads[i].userSocket.getOutputStream());
						threads[i].output_stream.println("#newuser " + userName);
						//threads[i].output_stream.close();
					}
				}
				}
			}
			
				

			/* Start the conversation. */
			while (true) {
				input_stream = new BufferedReader(new InputStreamReader(this.userSocket.getInputStream()));
				output_stream = new PrintStream(this.userSocket.getOutputStream());
				line = input_stream.readLine();//stores the input from User
				if(line.startsWith("#Bye")){ //If they want to leave, output it to everyone
					synchronized (userThread.class) {
					for(int i = 0; i < maxUsersCount; i++) {
						if(threads[i] != this && threads[i] != null) {
							//PrintStream output_stream_status = new PrintStream(threads[i].userSocket.getOutputStream());
							threads[i].output_stream.println("#Leave " + userName);
							//threads[i].output_stream.close();
							//threads[i].userSocket.close();
						}
					}
					}
					output_stream.println("#Bye");
					break;
					}
				if(line.startsWith("#status")) { //Post status
					output_stream.println("#statusPosted"); 
					String[] words = line.split("\\s", 2);
				/*splits the message into an array of 2 elements, the first being #status,
				 * the second being the actual message to print from User
				 */
					synchronized (userThread.class) {
					for(int i = 0; i < maxUsersCount; i++) { // Only post to the thread if its in ur friends list
						if(threads[i] != this && threads[i] != null && threads[i].friendlist.contains(this)) {
							//PrintStream output_stream_status = new PrintStream(threads[i].userSocket.getOutputStream());
							threads[i].output_stream.println("#newStatus " + userName + " " + words[1]);
							//threads[i].output_stream.close();
							//threads[i].userSocket.close();
						}
					}
					}
				}
				if(line.startsWith("#friendme")) {
					String[] words = line.split("\\s", 2);
					synchronized (userThread.class) {
						for(int i = 0; i < maxUsersCount; i++) {
							if(threads[i] != this && threads[i] != null && threads[i].userName.equals(words[1])){
								threads[i].output_stream.println("#friendme " + userName);
							}
						}
					}
				}
				if(line.startsWith("#friends")) {// If they accept, then add each other to friends list
					String[] words = line.split("\\s", 2);
					synchronized (userThread.class) {
						for(int i = 0; i < maxUsersCount; i++) {
							if(threads[i] != this && threads[i] != null && threads[i].userName.equals(words[1])){
								threads[i].friendlist.add(this);
								this.friendlist.add(threads[i]);
								output_stream.println("#OKfriends " + userName + " " + words[1]);
								threads[i].output_stream.println("#OKfriends " + words[1] + " " + userName);
							}
						}
					}
				}
				if(line.startsWith("#DenyFriendRequest")) { //Deny their friend request
					String[] words = line.split("\\s", 2);
					synchronized (userThread.class) {
						for(int i = 0; i < maxUsersCount; i++) {
							if(threads[i] != this && threads[i] != null && threads[i].userName.equals(words[1])){
								threads[i].output_stream.println("#FriendRequestDenied " + userName);
							}
						}
					}
				}
				if(line.startsWith("#unfriend")) { //Unfriend them and remove them from ur friends list
					String[] words = line.split("\\s", 2);
					synchronized (userThread.class) {
						for(int i = 0; i < maxUsersCount; i++) {
							if(threads[i] != this && threads[i] != null && threads[i].userName.equals(words[1])){
								threads[i].friendlist.remove(this);
								this.friendlist.remove(threads[i]);
								threads[i].output_stream.println("#NotFriends " + userName + " " + threads[i].userName);
								this.output_stream.println("#NotFriends " + userName+ " " + threads[i].userName);
							}
						}
					}
				}
			}

			// conversation ended.

			/*
			 * Clean up. Set the current thread variable to null so that a new user
			 * could be accepted by the server.
			 */
			synchronized (userThread.class) {
				for (int i = 0; i < maxUsersCount; i++) {
					if (threads[i] == this) {
						threads[i] = null;
					}
				}
			}
			/*
			 * Close the output stream, close the input stream, close the socket.
			 */
			input_stream.close();
			output_stream.close();
			userSocket.close();
		} catch (IOException e) {
			System.err.println("IOException:  " + e);
		}
	}
}




