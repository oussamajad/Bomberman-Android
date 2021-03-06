package ist.meic.cm.bomberman.multiplayerC;

import ist.meic.cm.bomberman.InGame;
import ist.meic.cm.bomberman.controller.MapController;
import ist.meic.cm.bomberman.controller.OperationCodes;
import ist.meic.cm.bomberman.p2p.manager.WiFiGlobal;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.net.Socket;

import android.app.Service;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

public class SyncMap extends Service {
	private MPMainGamePanel gamePanel;
	private boolean end;
	private OperationCodes option;
	private boolean running;
	private MapController mapController;

	@Override
	public IBinder onBind(Intent intent) {

		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		gamePanel = (MPMainGamePanel) InGame.getGamePanel();
		end = intent.getBooleanExtra("end", false);
		option = (OperationCodes) intent.getSerializableExtra("option");

		running = true;

		ThreadRefresh td = new ThreadRefresh();
		td.start();
		return super.onStartCommand(intent, flags, startId);
	}

	private class ThreadRefresh extends Thread {

		private Socket client;
		private ObjectInputStream input;
		private ObjectOutputStream output;
		private Message toSend;
		private Message received;
		private static final long REFRESH = 400;

		@Override
		public void run() {
			super.run();

			client = gamePanel.getClient();

			input = gamePanel.getInput();

			output = gamePanel.getOutput();

			try {

				if (end) {
					toSend = new Message(Message.END);
					sendToServer();
					running = false;

					if (InGame.isClient()) {
						WiFiGlobal global = WiFiGlobal.getInstance();
						Channel channel = global.getChannel();
						WifiP2pManager manager = global.getManager();

						if (manager != null && channel != null) {
							manager.removeGroup(channel, new ActionListener() {

								@Override
								public void onFailure(int reasonCode) {
									Log.d("QUIT", "Disconnect failed. Reason :"
											+ reasonCode);
								}

								@Override
								public void onSuccess() {
									try {
										client.close();
									} catch (IOException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
								}

							});
						}
						WiFiGlobal.clear();
					}
				} else
					while (running) {
						toSend = new Message(Message.REQUEST, option);

						sendToServer();

						received = (Message) input.readObject();

						if (received.getCode() == Message.SUCCESS) {

							mapController = received.getGameMap();

							handler.sendEmptyMessage(0);
						} else if (received.getCode() == Message.END) {
							running = false;
							handler.sendEmptyMessage(1);
						}

						sleep(REFRESH);
					}

			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (OptionalDataException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		private void sendToServer() throws IOException {
			synchronized (output) {
				output.writeObject(toSend);
				output.reset();
			}

		}
	}

	private Handler handler = new Handler() {
		@Override
		public void handleMessage(android.os.Message msg) {
			Intent intent = new Intent();
			intent.setAction("your.custom.BROADCAST");
			intent.setPackage("ist.meic.cm.bomberman");
			if (msg.what == 1) {
				intent.putExtra("mode", 1);

			} else {

				intent.putExtra("mode", 0);
				intent.putExtra("mapController", mapController);

			}
			sendBroadcast(intent);
		}
	};

}
