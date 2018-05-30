package com.justcoders.fis;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Scanner;

public class FISClient {

	private InetAddress host = InetAddress.getLocalHost();
	private int port = 6655;
	private static Scanner scanner = null;

	public FISClient(InetAddress serverAddress, int serverPort) throws IOException {
		host = serverAddress;
		port = serverPort;
	}

	private void start() throws IOException, ClassNotFoundException, InterruptedException {
		Socket socket = null;
		ObjectOutputStream oos = null;
		ObjectInputStream ois = null;

		System.out.println("Enter command to send to server:");
		
		String input = scanner.nextLine();
		String commandOutput = "";
		while (!input.equalsIgnoreCase("exit")) {
			// establish socket connection to server
			socket = new Socket(host.getHostName(), port);
			// write to socket using ObjectOutputStream
			oos = new ObjectOutputStream(socket.getOutputStream());
			System.out.println("Sending request to Socket Server");

			oos.writeObject(input);

			// read the server response message
			ois = new ObjectInputStream(socket.getInputStream());
			commandOutput = (String) ois.readObject();
			System.out.println(commandOutput);

			if (commandOutput.equalsIgnoreCase("200 Goodbye!")) {
				break;
			}
			// close resources
			ois.close();
			oos.close();
			Thread.sleep(100);
			System.out.println();
			System.out.println("Enter command to send to server:");
			input = scanner.nextLine();
		}
		scanner.close();
	}

	public static void main(String[] args) throws Exception {
		String serverIpAddress = null;
		int port = 0;
		scanner = new Scanner(System.in);
		if (args == null || !(args.length >= 1)) {
			System.out.println("Enter the server ip address:");
			serverIpAddress = scanner.nextLine();
		} else {
			serverIpAddress = args[0];
		}

		if (args == null || !(args.length >= 2)) {
			System.out.println("Enter the server port:");
			port = Integer.parseInt(scanner.nextLine());
		} else {
			port = Integer.parseInt(args[1]);
		}

		FISClient client = new FISClient(InetAddress.getByName(serverIpAddress), port);
		client.start();
	}

}
