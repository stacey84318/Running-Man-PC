package sunsonfinalproject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import javafx.embed.swing.JFXPanel;

public class Server {

	private ServerSocket serverSocket;
	private ConnectionThread connection;
	private List<ConnectionThread> connections = new ArrayList<ConnectionThread>();
	private MainApplet applet;
	private int appletWidth = 1200, appletHeight = 820;
	private static int portNum;
	private int player=0, selectCnt=0;
	
	public Server() {
		final CountDownLatch latch = new CountDownLatch(1);
		SwingUtilities.invokeLater(new Runnable() {
		    public void run() {
		        new JFXPanel(); // initializes JavaFX environment
		        latch.countDown();
		    }
		});
		try {
			latch.await();
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		GUI();
		
		try {
			this.serverSocket = new ServerSocket(portNum);
			System.out.printf("Server IP:   %s\nServer port: %d.\n", this.getAddress(), portNum);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void GUI(){
		applet = new MainApplet(this.getAddress() ,Integer.toString(portNum));
		applet.init();
		applet.setSize(appletWidth, appletHeight);
		applet.start();
		applet.setFocusable(true);
		
		JFrame window = new JFrame("Running man");
		window.setContentPane(applet);
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		window.setSize(appletWidth, appletHeight);      
		window.setVisible(true);
	}
	
	public void runForever() {
		System.out.println("Server starts waiting for client.");
		while(true){
			try{
				if(this.connections.size() < 4){
					Socket ToClient = this.serverSocket.accept();
					System.out.println("Get connection from client"
										+ ToClient.getInetAddress()+":"
										+ ToClient.getPort());
					
					connection = new ConnectionThread(ToClient, this.player);
					this.player++;
					connection.start();
					this.connections.add(connection);
					applet.waitConnectPage.sendClientIP(ToClient.getInetAddress().toString());
					this.broadcast("start");
				}
				
				if(this.connections.size() == 2){
					applet.currentGameState = gameState.CHOOSECHAR;
				}
						
				
			}catch(BindException e){
				e.printStackTrace();
			}catch(IOException e){
				e.printStackTrace();
			}
		}
	}
	
	private void broadcast(String message) {
		for (ConnectionThread connection: connections) {
			connection.sendMessage(message);
		}
	}
	
	// Define an inner class (class name should be ConnectionThread)
	class ConnectionThread extends Thread{
		private PrintWriter writer;
		private BufferedReader reader;
		private Socket socket;
		private Character character;
		public ChooseCharacter rect;
		private int lastShake=0, playerIndex;
		private gameState currentGameState = gameState.WAITCONNECT;
		
		public ConnectionThread(Socket socket, int player){
			this.socket = socket;
			this.playerIndex = player;
			try{
				this.reader = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
				this.writer = new PrintWriter(new OutputStreamWriter(this.socket.getOutputStream()));
			}catch(IOException e){
				e.printStackTrace();
			}
		}
		
		public void run(){
			while(true){
				try{
					String line = this.reader.readLine();
					System.out.println("server:"+this.playerIndex+" "+ line);
					//Wait
					if(this.currentGameState == gameState.WAITCONNECT){
						if(line.equals("enter")){
							this.currentGameState = gameState.CHOOSECHAR;
							rect = applet.newRect();
						}
					}
					//Choose characters
					else if(this.currentGameState == gameState.CHOOSECHAR){
						rect.choose(line);
						if(rect.getSelect() == true){
							character = applet.newCharacter(this.playerIndex);
							this.currentGameState = gameState.PLAY;
							selectCnt++;
							
							//PLAY start, the fastest client control
							if(selectCnt == connections.size() && applet.currentGameState != gameState.PLAY){
								applet.currentGameState = gameState.PLAY;
							}
						}
					}
					//Play
					else if(this.currentGameState == gameState.PLAY){
						character.diff = Integer.parseInt(line) - lastShake;
						lastShake = Integer.parseInt(line);
					}
				}
				catch(IOException e){
					e.printStackTrace();
				}
			}
		}
		
		public void sendMessage(String msg){
			this.writer.println(msg);
			this.writer.flush();
		}
	}
	
	public String getAddress(){
		InetAddress localIp;
	    try{
	    	localIp=InetAddress.getLocalHost();
	    	String ip=localIp.getHostAddress();
	    	return ip;
	    }catch (java.net.UnknownHostException e) {
			e.printStackTrace();
		}
	    return null;
	}
	
	public static void main(String[] args) {
		portNum = 8000;
		Server server = new Server();
		server.runForever();
	}

}
