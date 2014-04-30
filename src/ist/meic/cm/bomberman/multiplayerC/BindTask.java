package ist.meic.cm.bomberman.multiplayerC;

import ist.meic.cm.bomberman.AbsMainGamePanel;
import ist.meic.cm.bomberman.controller.MapController;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;

import android.os.AsyncTask;
import android.widget.Toast;

public class BindTask extends AsyncTask<Object, Void, Void> {
	private Socket client;
	private ObjectOutputStream output;
	private ObjectInputStream input;
	private static final int port = 4444;
	private Message toSend, received;
	private AbsMainGamePanel gamePanel;
	private MapController mapController;
	private ArrayList<String> players;
	private GameLobby gameLobby;

	@Override
	protected Void doInBackground(Object... objects) {
		try {
			gameLobby=(GameLobby)objects[5];
			client = new Socket((String) objects[3], port);
			gamePanel = (AbsMainGamePanel) objects[0];
			output = new ObjectOutputStream(client.getOutputStream());
			input = new ObjectInputStream(client.getInputStream());

			toSend = new Message(Message.JOIN, (String) objects[2] + " "
					+ (String) objects[4]);

			output.writeObject(toSend);
			output.reset();

			received = (Message) input.readObject();

			if (received.getCode() == Message.SUCCESS) {
				objects[1] = received.getPlayerID();
				mapController = received.getGameMap();
				gamePanel.setMapController(mapController);
				gamePanel.setSocket(client);
				gamePanel.setOutput(output);
				gamePanel.setInput(input);
				GameLobby.setConnected(true);
				players = received.getPlayers();
				System.out.println(players.size());
			}

		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	protected void onPostExecute(Void v) {
		gameLobby.setPlayers(players);
		gameLobby.setOutput(output);
		gameLobby.setInput(input);
	}

}