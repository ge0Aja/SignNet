package com.sign.language.translator;

import android.speech.tts.TextToSpeech;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.util.Locale;

import org.json.JSONObject;

@SuppressLint("DefaultLocale")
public class MainActivity extends ActionBarActivity implements TaskListener {

	private final String TAG = "SIGNLANGTRANS";

	BluetoothAdapter btAdapter;
	BluetoothSocket btSocket;
	BluetoothDevice btDevice;
	InputStream inputStream;

	TextView lab;

	RadioButton radEnglish, radFrench, radArabic;

	SharedPreferences prefReader;

	boolean blnDisableBluetoothAdapterOnExit = false, blnThreadIsActive;

	boolean blnSpellWordOnce = false;

	Spinner cboUser;

	Thread workerThread;
	byte[] readBuffer;
	int readBufferPosition;
	int counter;
	volatile boolean stopWorker;
	EditText t;

	MediaPlayer mPlayer;
	boolean b = false;

	String getLanguage() {
		if (this.radEnglish.isChecked())
			return "en";
		else if (this.radFrench.isChecked())
			return "fr";
		// else if(this.radArabic.isChecked())
		// return "ar";
		else
			return "";
	}

	int getNumberFromString(String s) {
		String strResult = "";
		for (int i = 0; i < s.length(); i++)
			if (s.substring(i, i + 1).matches("-?\\d+(\\.\\d+)?"))
				strResult += s.substring(i, i + 1);
		return Integer.parseInt(strResult);
	}

	// input given as a code
	void PlaySound(int voiceID) {
		if (voiceID == 0)
			return;
		this.mPlayer = MediaPlayer.create(this, voiceID);
		if (this.mPlayer.isPlaying())
			this.mPlayer.stop();
		this.mPlayer.start();
	}

	// Async task to do the background network operations
	private class ANNTask extends AsyncTask<String, Void, Integer> {
		private TaskListener listener;
		private long startTime;

		StringBuilder sb = new StringBuilder();
		String tempOutput = "";

		public ANNTask(TaskListener listener) {
			this.listener = listener;
		}

		@Override
		protected Integer doInBackground(String... strings) {
			
			Log.d(TAG, "Start connecting server");
			Log.d(TAG, strings.toString());

			HttpURLConnection senddata_con = ServerLogic.setUpHttpConnection("http://192.168.137.1:8090/postjson", "POST",
					"192.168.137.1");

			if (senddata_con != null) {
				Log.d(TAG, "The connection is not null");
				try {
					OutputStream os = senddata_con.getOutputStream();
					OutputStreamWriter osw = new OutputStreamWriter(os, "UTF-8");

					JSONObject jsonObject = new JSONObject();

					jsonObject.put("x1", strings[0]);
					jsonObject.put("x2", strings[1]);
					jsonObject.put("x3", strings[2]);
					jsonObject.put("x4", strings[3]);
					jsonObject.put("x5", strings[4]);
					jsonObject.put("xacc1", strings[5]);
					jsonObject.put("xacc2", strings[6]);
					jsonObject.put("xacc3", strings[7]);

					jsonObject.put("y1", strings[8]);
					jsonObject.put("y2", strings[9]);
					jsonObject.put("y3", strings[10]);
					jsonObject.put("y4", strings[11]);
					jsonObject.put("y5", strings[12]);
					jsonObject.put("yacc1", strings[13]);
					jsonObject.put("yacc2", strings[14]);
					jsonObject.put("yacc3", strings[15]);

					senddata_con.connect();

					osw.write(jsonObject.toString());
					osw.flush();
					osw.close();

					BufferedReader br = new BufferedReader(new InputStreamReader(senddata_con.getInputStream()));
					while ((tempOutput = br.readLine()) != null) {
						sb.append(tempOutput);
					}
					if (sb.length() == 0 || sb.toString().equals("")) {
						Log.d(TAG, "The String builder is empty no response from server");
					}

					Log.d(TAG, sb.toString());

					if (sb.length() != 0 && !sb.toString().equals("")) {
						Log.d(TAG, "Server Response is " + sb.toString());
						JSONObject jsonObject1 = new JSONObject(sb.toString());
						if (jsonObject1.getString("status").equals("success")) {
							// send a jsonobject from the server (check)
							// dont send a word was found unless the accuracy is
							// high from the server side
							// call the method to play the sound and display the
							// text accordingly (check)
							// don't repeat calling the method if the last displayed
							// word is the same (check)
							int predictedWord = Integer.valueOf(jsonObject1.getString("word")) + 1;
							
							return predictedWord;

						} else {
							// the word was not found with high accuracy don't play
							// or display
							Log.d(TAG, "The word was not found with high accuracy");
							return 0;
						}
					} else {
						Log.d(TAG, "The connection is null");
					}

				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					if (senddata_con != null) {
						senddata_con.disconnect();
					}
				}
			}
			return 0;
		}

		@Override
		protected void onPostExecute(Integer integer) {
			Log.d(TAG, "The Background Network process finished working");
			listener.onTaskCompleted(integer);
			super.onPostExecute(integer);
		}
	}
	
	@Override
	public void onTaskCompleted(int predictedWord) {
		// TODO Auto-generated method stub

		switch (predictedWord) {
		case 1: // T
			this.SpeachSelectedText("T");
			break;
		case 2: // S
			this.SpeachSelectedText("S");
			break;
		case 3: // U
			this.SpeachSelectedText("U");
		case 4: // A
			this.SpeachSelectedText("A");
			break;
		case 5: // From
			this.SpeachSelectedText("From");
			break;
		case 6:
			this.SpeachSelectedText("We are engineers");
			break;
		case 7:
			this.SpeachSelectedText("Our senior project");
			break;
		case 8:
			this.SpeachSelectedText("This is");
			break;
		case 9:
			this.SpeachSelectedText("Fadi");
			break;
		case 10:
			this.SpeachSelectedText("My name is");
			break;
		case 11:
			this.SpeachSelectedText("Hi");
			break;
		default:
			t.setText("Unknown");
		}
	}


	// Added to forward input values to computer
	void forwardInputValues(String s) {

		String col[] = s.split("@");

		if (col.length != 2)
			return;
		String left[], right[];
		left = col[0].split(";");

		right = col[1].split(";");

		if (left.length != 8)
			return;
		if (right.length != 8)
			return;

		int l1, l2, l3, l4, l5, lx, ly, lz, r1, r2, r3, r4, r5, rx, ry, rz;
		
		l1 = this.getNumberFromString(left[0]);
		l2 = this.getNumberFromString(left[1]);
		l3 = this.getNumberFromString(left[2]);
		l4 = this.getNumberFromString(left[3]);
		l5 = this.getNumberFromString(left[4]);

		lx = this.getNumberFromString(left[5]);
		ly = this.getNumberFromString(left[6]);
		lz = this.getNumberFromString(left[7]);

		r1 = this.getNumberFromString(right[0]);
		r2 = this.getNumberFromString(right[1]);
		r3 = this.getNumberFromString(right[2]);
		r4 = this.getNumberFromString(right[3]);
		r5 = this.getNumberFromString(right[4]);

		rx = this.getNumberFromString(right[5]);
		ry = this.getNumberFromString(right[6]);
		rz = this.getNumberFromString(right[7]);

		this.lab.setText("");

		this.lab.append("\n\nRight hand:\n");
		for (int i = 0; i <= 4; i++)
			this.lab.append(right[i] + "   ");
		this.lab.append("\n\nacc: " + Integer.toString(rx) + "   " + Integer.toString(ry) + "   " + Integer.toString(rz)
				+ "\n");
		this.lab.append("\n\n-----------------------------------\n\nLeft hand:\n");

		for (int i = 0; i <= 4; i++)
			this.lab.append(left[i] + "   ");
		this.lab.append("\n\nacc: " + Integer.toString(lx) + "   " + Integer.toString(ly) + "   " + Integer.toString(lz)
				+ "\n");

		// start the logic of creating a connection and forwarding data to the
		// server
		StringBuilder sb = new StringBuilder();
		String tempOutput = "";

		Log.d(TAG, "Start Background task");
		
		// call the async task here
		  
		  ANNTask annTask = new ANNTask(MainActivity.this);
          annTask.execute(right[0],right[1],right[2],right[3],right[4],right[5],right[6],right[7],
        		  left[0],left[1],left[2],left[3],left[4],left[5],left[6],left[7]);
	}

	void compaireInputValues(String s) {

		String col[] = s.split("@");

		if (col.length != 2)
			return;
		String left[], right[];
		left = col[0].split(";");

		right = col[1].split(";");

		if (left.length != 8)
			return;
		if (right.length != 8)
			return;

		int l1, l2, l3, l4, l5, lx, ly, lz, r1, r2, r3, r4, r5, rx, ry, rz;
		l1 = this.getNumberFromString(left[0]);
		l2 = this.getNumberFromString(left[1]);
		l3 = this.getNumberFromString(left[2]);
		l4 = this.getNumberFromString(left[3]);
		l5 = this.getNumberFromString(left[4]);

		lx = this.getNumberFromString(left[5]);
		ly = this.getNumberFromString(left[6]);
		lz = this.getNumberFromString(left[7]);

		r1 = this.getNumberFromString(right[0]);
		r2 = this.getNumberFromString(right[1]);
		r3 = this.getNumberFromString(right[2]);
		r4 = this.getNumberFromString(right[3]);
		r5 = this.getNumberFromString(right[4]);

		rx = this.getNumberFromString(right[5]);
		ry = this.getNumberFromString(right[6]);
		rz = this.getNumberFromString(right[7]);

		this.lab.setText("");

		this.lab.append("\n\nRight hand:\n");

		for (int i = 0; i <= 4; i++)
			this.lab.append(right[i] + "   ");

		this.lab.append("\n\nacc: " + Integer.toString(rx) + "   " + Integer.toString(ry) + "   " + Integer.toString(rz)
				+ "\n");

		this.lab.append("\n\n-----------------------------------\n\nLeft hand:\n");

		for (int i = 0; i <= 4; i++)
			this.lab.append(left[i] + "   ");

		this.lab.append("\n\nacc: " + Integer.toString(lx) + "   " + Integer.toString(ly) + "   " + Integer.toString(lz)
				+ "\n");

		// instead of calling the hard coded method we issue a call to the
		// server and send the data as a json string

		if (this.cboUser.getSelectedItemPosition() == 0) {
			// user 1
			if (!f1)
				if (this.check_Sunday_User1(r1, r2, r3, r4, r5)) {
					this.SpeachSelectedText("Sunday");
					resetAll_F_Users();
					f1 = true;
				}
			// else this.resetAll_F_Users();

			if (!f2)
				if (this.check_death_User1(r1, r2, r3, r4, r5, rx, ry, rz)) {
					this.SpeachSelectedText("death");
					resetAll_F_Users();
					f2 = true;
				} // else this.resetAll_F_Users();

			if (!f3)
				if (this.check_sickness_User1(r1, r2, r3, r4, r5, rx, ry, rz)) {
					this.SpeachSelectedText("illness");
					resetAll_F_Users();
					f3 = true;
				} // else this.resetAll_F_Users();

			if (!f4)
				if (this.check_time_User1(r1, r2, r3, r4, r5, rx, ry, rz, l1, l2, l3, l4, l5, lx, ly, lz)) {
					this.SpeachSelectedText("time");
					resetAll_F_Users();
					f4 = true;
				} // else this.resetAll_F_Users();

			if (!f5)
				if (this.check_Time_User1(r1, r2, r3, r4, r5, rx, ry, rz, l1, l2, l3, l4, l5, lx, ly, lz)) {
					this.SpeachSelectedText("Time");
					resetAll_F_Users();
					f5 = true;
				} // else this.resetAll_F_Users();

			if (!f6)
				if (this.check_cold_User1(r1, r2, r3, r4, r5, rx, ry, rz, l1, l2, l3, l4, l5, lx, ly, lz)) {
					this.SpeachSelectedText("cold");
					resetAll_F_Users();
					f6 = true;
				} // else this.resetAll_F_Users();

			if (!f7)
				if (this.check_health_User1(r1, r2, r3, r4, r5, rx, ry, rz, l1, l2, l3, l4, l5, lx, ly, lz)) {
					this.SpeachSelectedText("health");
					resetAll_F_Users();
					f7 = true;
				} // else this.resetAll_F_Users();

			if (!f8)
				if (this.check_fire_User1(r1, r2, r3, r4, r5, rx, ry, rz, l1, l2, l3, l4, l5, lx, ly, lz)) {
					this.SpeachSelectedText("fire");
					resetAll_F_Users();
					f8 = true;
				} // else this.resetAll_F_Users();

			if (!f9)
				if (this.check_medicine_User1(r1, r2, r3, r4, r5, rx, ry, rz)) {
					this.SpeachSelectedText("medicine");
					resetAll_F_Users();
					f9 = true;
				} // else this.resetAll_F_Users();

			if (!f10)
				if (this.check_today_User1(r1, r2, r3, r4, r5, rx, ry, rz)) {
					this.SpeachSelectedText("today");
					resetAll_F_Users();
					f10 = true;
				} // else this.resetAll_F_Users();

			if (!f11)
				if (this.check_water_User1(r1, r2, r3, r4, r5, rx, ry, rz)) {
					this.SpeachSelectedText("water");
					resetAll_F_Users();
					f11 = true;
				} // else this.resetAll_F_Users();

			if (!f12)
				if (this.check_pass_User1(r1, r2, r3, r4, r5, rx, ry, rz)) {
					this.SpeachSelectedText("pass");
					resetAll_F_Users();
					f12 = true;
				} // else this.resetAll_F_Users();

			if (!f13)
				if (this.check_hi_User1(r1, r2, r3, r4, r5, rx, ry, rz)) {
					this.SpeachSelectedText("hi");
					resetAll_F_Users();
					f13 = true;
				} // else this.resetAll_F_Users();

			if (!f14)
				if (this.check_what_User1(r1, r2, r3, r4, r5, rx, ry, rz)) {
					this.SpeachSelectedText("what");
					resetAll_F_Users();
					f14 = true;
				} // else this.resetAll_F_Users();

			if (!f15)
				if (this.check_Monday_User1(r1, r2, r3, r4, r5, rx, ry, rz)) {
					this.SpeachSelectedText("Monday");
					resetAll_F_Users();
					f15 = true;
				} // else this.resetAll_F_Users();

			if (!f16)
				if (this.check_drink_User1(r1, r2, r3, r4, r5, rx, ry, rz)) {
					this.SpeachSelectedText("drink");
					resetAll_F_Users();
					f16 = true;
				} // else this.resetAll_F_Users();

			if (!f17)
				if (this.check_Tuesday_User1(r1, r2, r3, r4, r5, l1, l2, l3, l4, l5)) {
					this.SpeachSelectedText("Tuesday");
					resetAll_F_Users();
					f17 = true;
				} // else this.resetAll_F_Users();

			if (!f18)
				if (this.check_Wednesday_User1(r1, r2, r3, r4, r5, rx, ry, rz, l1, l2, l3, l4, l5, lx, ly, lz)) {
					this.SpeachSelectedText("Wednesday");
					resetAll_F_Users();
					f18 = true;
				} // else this.resetAll_F_Users();

			if (!f19)
				if (this.check_Thursday_User1(r1, r2, r3, r4, r5, rx, ry, rz)) {
					this.SpeachSelectedText("Thursday");
					resetAll_F_Users();
					f19 = true;
				} // else this.resetAll_F_Users();

			if (!f20)
				if (this.check_Friday_User1(r1, r2, r3, r4, r5, rx, ry, rz)) {
					this.SpeachSelectedText("Friday");
					resetAll_F_Users();
					f20 = true;
				} // else this.resetAll_F_Users();

			if (!f21)
				if (this.check_Saturday_User1(r1, r2, r3, r4, r5, rx, ry, rz)) {
					this.SpeachSelectedText("Saturday");
					resetAll_F_Users();
					f21 = true;
				} // else this.resetAll_F_Users();

			if (!f22)
				if (this.check_Mother_User1(r1, r2, r3, r4, r5, rx, ry, rz)) {
					this.SpeachSelectedText("Mother");
					resetAll_F_Users();
					f22 = true;
				} // else this.resetAll_F_Users();

			if (!f23)
				if (this.check_Father_User1(r1, r2, r3, r4, r5, rx, ry, rz)) {
					this.SpeachSelectedText("Father");
					resetAll_F_Users();
					f23 = true;
				} // else this.resetAll_F_Users();

			if (!f24)
				if (this.check_Me_User1(r1, r2, r3, r4, r5, rx, ry, rz)) {
					this.SpeachSelectedText("Me");
					resetAll_F_Users();
					f24 = true;
				} // else this.resetAll_F_Users();

			if (!f25)
				if (this.check_You_User1(r1, r2, r3, r4, r5, rx, ry, rz)) {
					this.SpeachSelectedText("You");
					resetAll_F_Users();
					f25 = true;
				} // else this.resetAll_F_Users();

			if (!f26)
				if (this.check_Dream_User1(r1, r2, r3, r4, r5, rx, ry, rz)) {
					this.SpeachSelectedText("Dream");
					resetAll_F_Users();
					f26 = true;
				} // else this.resetAll_F_Users();

			if (!f27)
				if (this.check_Food_User1(r1, r2, r3, r4, r5, rx, ry, rz)) {
					this.SpeachSelectedText("Food");
					resetAll_F_Users();
					f27 = true;
				} // else this.resetAll_F_Users();

			if (!f28)
				if (this.check_Life_User1(r1, r2, r3, r4, r5, rx, ry, rz, l1, l2, l3, l4, l5, lx, ly, lz)) {
					this.SpeachSelectedText("Life");
					resetAll_F_Users();
					f28 = true;
				} // else this.resetAll_F_Users();

			if (!f29)
				if (this.check_Welcome_User1(r1, r2, r3, r4, r5, rx, ry, rz, l1, l2, l3, l4, l5, lx, ly, lz)) {
					this.SpeachSelectedText("Welcome");
					resetAll_F_Users();
					f29 = true;
				} // else this.resetAll_F_Users();

			if (!f30)
				if (this.check_Lebanon_User1(r1, r2, r3, r4, r5, rx, ry, rz, l1, l2, l3, l4, l5, lx, ly, lz)) {
					this.SpeachSelectedText("Lebanon");
					resetAll_F_Users();
					f30 = true;
				} // else this.resetAll_F_Users();

			if (!f31)
				if (this.check_Brother_User1(r1, r2, r3, r4, r5, rx, ry, rz, l1, l2, l3, l4, l5, lx, ly, lz)) {
					this.SpeachSelectedText("Brother");
					resetAll_F_Users();
					f31 = true;
				} // else this.resetAll_F_Users();

		} else if (this.cboUser.getSelectedItemPosition() == 1) {
			// user 2. 18 word

			if (!d1)
				if (this.check_Sunday_User2(r1, r2, r3, r4, r5)) {
					this.SpeachSelectedText("hi");
					resetAll_D_Users();
					d1 = true;
				}

			if (!d2)
				if (this.check_Monday_User2(r1, r2, r3, r4, r5, rx, ry, rz)) {
					this.SpeachSelectedText("my name is");
					resetAll_D_Users();
					d2 = true;
				}

			if (!d3)
				if (this.check_Tuesday_User2(l1, l2, l3, l4, l5, lx, ly, lz)) {
					this.SpeachSelectedText("dian");
					resetAll_D_Users();
					d3 = true;
				}

			if (!d4)
				if (this.check_Wednesday_User2(r1, r2, r3, r4, r5, rx, ry, rz)) {
					this.SpeachSelectedText("i am engineer");
					resetAll_D_Users();
					d4 = true;
				}

			if (!d5)
				if (this.check_Thursday_User2(r1, r2, r3, r4, r5, rx, ry, rz)) {
					this.SpeachSelectedText("Thursday");
					resetAll_D_Users();
					d5 = true;
				}

			if (!d6)
				if (this.check_Friday_User2(r1, r2, r3, r4, r5, rx, ry, rz)) {
					this.SpeachSelectedText("Friday");
					resetAll_D_Users();
					d6 = true;
				}

			if (!d7)
				if (this.check_Saturday_User2(r1, r2, r3, r4, r5, rx, ry, rz)) {
					this.SpeachSelectedText("Saturday");
					resetAll_D_Users();
					d7 = true;
				}

			if (!d8)
				if (this.check_Mother_User2(r1, r2, r3, r4, r5, rx, ry, rz)) {
					this.SpeachSelectedText("diana");
					resetAll_D_Users();
					d8 = true;
				}

			if (!d9)
				if (this.check_Father_User2(r1, r2, r3, r4, r5, rx, ry, rz)) {
					this.SpeachSelectedText("Father");
					resetAll_D_Users();
					d9 = true;
				}

			if (!d10)
				if (this.check_Me_User2(r1, r2, r3, r4, r5, rx, ry, rz)) {
					this.SpeachSelectedText("Me");
					resetAll_D_Users();
					d10 = true;
				}

			if (!d11)
				if (this.check_You_User2(r1, r2, r3, r4, r5, rx, ry, rz)) {
					this.SpeachSelectedText("You");
					resetAll_D_Users();
					d11 = true;
				}

			if (!d12)
				if (this.check_Dream_User2(r1, r2, r3, r4, r5, rx, ry, rz)) {
					this.SpeachSelectedText("Dream");
					resetAll_D_Users();
					d12 = true;
				}

			if (!d13)
				if (this.check_Food_User2(r1, r2, r3, r4, r5, rx, ry, rz)) {
					this.SpeachSelectedText("Food");
					resetAll_D_Users();
					d13 = true;
				}

			if (!d14)
				if (this.check_Life_User2(r1, r2, r3, r4, r5, rx, ry, rz, l1, l2, l3, l4, l5, lx, ly, lz)) {
					this.SpeachSelectedText("diana");
					resetAll_D_Users();
					d14 = true;
				}

			if (!d15)
				if (this.check_Welcome_User2(r1, r2, r3, r4, r5, rx, ry, rz, l1, l2, l3, l4, l5, lx, ly, lz)) {
					this.SpeachSelectedText("Welcome");
					resetAll_D_Users();
					d15 = true;
				}

			if (!d16)
				if (this.check_Lebanon_User2(r1, r2, r3, r4, r5, rx, ry, rz, l1, l2, l3, l4, l5, lx, ly, lz)) {
					this.SpeachSelectedText("Lebanon");
					resetAll_D_Users();
					d16 = true;
				}

			if (!d17)
				if (this.check_Brother_User2(r1, r2, r3, r4, r5, rx, ry, rz, l1, l2, l3, l4, l5, lx, ly, lz)) {
					this.SpeachSelectedText("Brother");
					resetAll_D_Users();
					d17 = true;
				}

			if (!d18)
				if (this.check_Time_User2(r1, r2, r3, r4, r5, rx, ry, rz, l1, l2, l3, l4, l5, lx, ly, lz)) {
					this.SpeachSelectedText("Time");
					resetAll_D_Users();
					d18 = true;
				}
		} else if (this.cboUser.getSelectedItemPosition() == 2) {
			// user 3
			if (this.check_User3_S1(r1, r2, r3, r4, r5, l1, l2, l3, l4, l5)) {
				if (!s1) {
					this.SpeachSelectedText("Hi");
					s1 = true;
				}
			}

			if (this.check_User3_S2(r1, r2, r3, r4, r5, rx, ry, rz)) {
				if (!s2) {
					this.SpeachSelectedText("my name is");
					s2 = true;
				}
			}

			if (this.check_User3_S3(l1, l2, l3, l4, l5, lx, ly, lz)) {
				if (!s3) {
					this.SpeachSelectedText("fadi");
					s3 = true;
				}
			}

			if (this.check_User3_S4(l1, l2, l3, l4, l5)) {
				if (!s4) {
					this.SpeachSelectedText("this is");
					s4 = true;
				}
			}

			if (this.check_User3_S5(r1, r2, r3, r4, r5, rx, ry, rz)) {
				if (!s5) {
					this.SpeachSelectedText("Our senior project");
					s5 = true;
				}
			}

			if (this.check_User3_S6(r1, r2, r3, r4, r5)) {
				if (!s6) {
					this.SpeachSelectedText("we are engineers");
					s6 = true;
				}
			}

			if (this.check_User3_S7(r1, r2, r3, r4, r5)) {
				if (!s7) {
					this.SpeachSelectedText("from");
					s7 = true;
				}

			}

			if (this.check_User3_S8(l1, l2, l3, l4, l5)) {
				if (!s8) {
					this.SpeachSelectedText("A");
					s8 = true;
				}
			}

			if (this.check_User3_S9(l1, l2, l3, l4, l5)) {
				if (!s9) {
					this.SpeachSelectedText("U");
					s9 = true;
				}
			}

			if (this.check_User3_S10(l1, l2, l3, l4, l5)) {
				if (!s10) {
					this.SpeachSelectedText("S");
					s10 = true;
				}
			}
			if (this.check_User3_S11(l1, l2, l3, l4, l5)) {
				if (!s11) {
					this.SpeachSelectedText("T");
					s11 = true;
				}
			}

			if (this.check_User3_S12(r1, r2, r3, r4, r5, rx, ry, rz)) {
				if (!s12) {
					this.SpeachSelectedText("thank you");
					s12 = true;
				}
			}

		}
	}

	boolean s1 = false, s2 = false, s3 = false, s4 = false, s5 = false, s6 = false, s7 = false, s8 = false, s9 = false,
			s10 = false, s11 = false, s12 = false, s13 = false, s14 = false;

	boolean f1 = false, f2 = false, f3 = false, f4 = false, f5 = false, f6 = false, f7 = false, f8 = false, f9 = false,
			f10 = false, f11 = false, f12 = false, f13 = false, f14 = false;

	boolean f15 = false, f16 = false, f17 = false, f18 = false, f19 = false, f20 = false, f21 = false, f22 = false,
			f23 = false, f24 = false, f25 = false, f26 = false, f27 = false, f28 = false, f29 = false, f30 = false,
			f31 = false;

	boolean d1 = false, d2 = false, d3 = false, d4 = false, d5 = false, d6 = false, d7 = false, d8 = false, d9 = false,
			d10 = false, d11 = false, d12 = false, d13 = false, d14 = false, d15 = false, d16 = false, d17 = false,
			d18 = false;

	void resetAll_F_Users() {
		f1 = false;
		f2 = false;
		f3 = false;
		f4 = false;
		f5 = false;
		f6 = false;
		f7 = false;
		f8 = false;
		f9 = false;
		f10 = false;
		f11 = false;
		f12 = false;
		f13 = false;
		f14 = false;
		f15 = false;
		f16 = false;
		f17 = false;
		f18 = false;
		f19 = false;
		f20 = false;
		f21 = false;
		f22 = false;
		f23 = false;
		f24 = false;
		f25 = false;
		f26 = false;
		f27 = false;
		f28 = false;
		f29 = false;
		f30 = false;
		f31 = false;
	}

	void resetAll_D_Users() {

		d1 = false;
		d2 = false;
		d3 = false;
		d4 = false;
		d5 = false;
		d6 = false;
		d7 = false;
		d8 = false;
		d9 = false;
		d10 = false;
		d11 = false;
		d12 = false;
		d13 = false;
		d14 = false;
		d15 = false;
		d16 = false;
		d17 = false;
		d18 = false;
	}
	// user
	// 3///////////////////////////////////////////////////////////////////////////////////////////

	boolean check_User3_S1(int r1, int r2, int r3, int r4, int r5, int l1, int l2, int l3, int l4, int l5) {
		if (c(r1, 440, 490) && c(r2, 260, 330) && c(r3, 270, 320) && c(r4, 250, 320) && c(r5, 180, 250))
			return true;
		else
			return false;
	}

	boolean check_User3_S2(int r1, int r2, int r3, int r4, int r5, int rx, int ry, int rz) {
		if (c(r1, 310, 370) && c(r2, 210, 270) && c(r3, 220, 370) && c(r4, 250, 300) && c(r5, 100, 500)

				&& c(rx, 380, 430) && c(ry, 350, 410) && c(rz, 410, 470))
			return true;
		else
			return false;
	}

	boolean check_User3_S3(int l1, int l2, int l3, int l4, int l5, int lx, int ly, int lz) {
		if (c(l1, 450, 500) && c(l2, 440, 510) && c(l3, 460, 530) && c(l4, 400, 470) && c(l5, 240, 300)

				&& c(lx, 350, 400) && c(ly, 310, 370) && c(lz, 260, 300))
			return true;
		else
			return false;
	}

	boolean check_User3_S4(int l1, int l2, int l3, int l4, int l5) {
		if (c(l1, 440, 500) && c(l2, 430, 510) && c(l3, 440, 510) && c(l4, 410, 470) && c(l5, 160, 240))
			return true;
		else
			return false;
	}

	boolean check_User3_S5(int r1, int r2, int r3, int r4, int r5, int rx, int ry, int rz) {
		if (c(r1, 320, 380) && c(r2, 220, 280) && c(r3, 220, 290) && c(r4, 240, 300) && c(r5, 100, 500)

				&& c(rx, 340, 400) && c(ry, 250, 310) && c(rz, 340, 400))
			return true;
		else
			return false;
	}

	boolean check_User3_S6(int r1, int r2, int r3, int r4, int r5) {
		if (c(r1, 330, 400) && c(r2, 220, 270) && c(r3, 210, 260) && c(r4, 170, 220) && c(r5, 100, 500))

			return true;
		else
			return false;
	}

	boolean check_User3_S7(int r1, int r2, int r3, int r4, int r5) {
		if (c(r1, 430, 480) && c(r2, 260, 310) && c(r3, 280, 330) && c(r4, 190, 240) && c(r5, 100, 500))
			return true;
		else
			return false;
	}

	boolean check_User3_S8(int l1, int l2, int l3, int l4, int l5) {
		if (c(l1, 280, 330) && c(l2, 310, 370) && c(l3, 380, 430) && c(l4, 340, 400) && c(l5, 220, 270))
			return true;
		else
			return false;
	}

	boolean check_User3_S9(int l1, int l2, int l3, int l4, int l5) {
		if (c(l1, 450, 500) && c(l2, 330, 390) && c(l3, 385, 440) && c(l4, 345, 400) && c(l5, 220, 280))
			return true;
		else
			return false;
	}

	boolean check_User3_S10(int l1, int l2, int l3, int l4, int l5) {
		if (c(l1, 460, 500) && c(l2, 440, 490) && c(l3, 380, 430) && c(l4, 370, 420) && c(l5, 220, 270))
			return true;
		else
			return false;
	}

	boolean check_User3_S11(int l1, int l2, int l3, int l4, int l5) {
		if (c(l1, 460, 500) && c(l2, 445, 500) && c(l3, 380, 430) && c(l4, 400, 450) && c(l5, 230, 290))
			return true;
		else
			return false;
	}

	boolean check_User3_S12(int r1, int r2, int r3, int r4, int r5, int rx, int ry, int rz) {
		if (c(r1, 430, 480) && c(r2, 275, 330) && c(r3, 270, 320) && c(r4, 260, 310) && c(r5, 195, 250)

				&& c(rx, 320, 370) && c(ry, 340, 390) && c(rz, 420, 480))
			return true;
		else
			return false;
	}

	/////////////////////////////////////////////////////////////////

	// user
	// 1///////////////////////////////////////////////////////////////////////////////////////////
	boolean check_Sunday_User1(int r1, int r2, int r3, int r4, int r5) {
		if (c(r1, 350, 420) && c(r2, 310, 370) && c(r3, 240, 330) && c(r4, 260, 325) && c(r5, 180, 250))
			return true;
		else
			return false;
	}

	boolean check_death_User1(int r1, int r2, int r3, int r4, int r5, int rx, int ry, int rz) {
		if (c(r1, 450, 500) && c(r2, 300, 350) && c(r3, 340, 400) && c(r4, 270, 320) && c(r5, 260, 410)

				&& c(rx, 420, 460) && c(ry, 310, 350) && c(rz, 340, 380))
			return true;
		else
			return false;
	}

	boolean check_sickness_User1(int r1, int r2, int r3, int r4, int r5, int rx, int ry, int rz) {
		if (c(r1, 450, 500) && c(r2, 300, 350) && c(r3, 340, 400) && c(r4, 270, 320) && c(r5, 270, 360)

				&& c(rx, 410, 450) && c(ry, 310, 350) && c(rz, 390, 430))
			return true;
		else
			return false;
	}

	boolean check_cold_User1(int r1, int r2, int r3, int r4, int r5, int rx, int ry, int rz, int l1, int l2, int l3,
			int l4, int l5, int lx, int ly, int lz) {
		if (c(r1, 310, 380) && c(r2, 240, 290) && c(r3, 230, 280) && c(r4, 180, 230) && c(r5, 180, 230)

				&& c(rx, 370, 410) && c(ry, 330, 370) && c(rz, 430, 470)

				&& c(l1, 360, 410) && c(l2, 380, 430) && c(l3, 400, 450) && c(l4, 320, 370) && c(l5, 160, 210)

				&& c(lx, 340, 380) && c(ly, 330, 380) && c(lz, 270, 310))
			return true;
		else
			return false;
	}

	boolean check_health_User1(int r1, int r2, int r3, int r4, int r5, int rx, int ry, int rz, int l1, int l2, int l3,
			int l4, int l5, int lx, int ly, int lz) {
		if (c(r1, 290, 360) && c(r2, 210, 270) && c(r3, 220, 300) && c(r4, 140, 200) && c(r5, 230, 300)

				&& c(rx, 400, 440) && c(ry, 400, 440) && c(rz, 370, 410)

				&& c(l1, 330, 400) && c(l2, 360, 420) && c(l3, 400, 450) && c(l4, 360, 420) && c(l5, 210, 270)

				&& c(lx, 390, 440) && c(ly, 400, 440) && c(lz, 320, 360))
			return true;
		else
			return false;
	}

	boolean check_fire_User1(int r1, int r2, int r3, int r4, int r5, int rx, int ry, int rz, int l1, int l2, int l3,
			int l4, int l5, int lx, int ly, int lz) {
		if (c(r1, 390, 440) && c(r2, 280, 330) && c(r3, 280, 330) && c(r4, 220, 270) && c(r5, 240, 290)

				&& c(rx, 300, 340) && c(ry, 340, 380) && c(rz, 400, 440)

				&& c(l1, 430, 500) && c(l2, 420, 480) && c(l3, 440, 490) && c(l4, 380, 430) && c(l5, 240, 300)

				&& c(lx, 280, 320) && c(ly, 330, 370) && c(lz, 310, 350))
			return true;
		else
			return false;
	}

	boolean check_medicine_User1(int r1, int r2, int r3, int r4, int r5, int rx, int ry, int rz) {
		if (c(r1, 450, 500) && c(r2, 300, 350) && c(r3, 340, 390) && c(r4, 200, 250) && c(r5, 250, 300)

				&& c(rx, 390, 430) && c(ry, 300, 340) && c(rz, 410, 460))
			return true;
		else
			return false;
	}

	boolean check_today_User1(int r1, int r2, int r3, int r4, int r5, int rx, int ry, int rz) {
		if (c(r1, 340, 400) && c(r2, 250, 300) && c(r3, 280, 320) && c(r4, 270, 320) && c(r5, 180, 230)

				&& c(rx, 400, 440) && c(ry, 410, 450) && c(rz, 350, 400))
			return true;
		else
			return false;
	}

	boolean check_water_User1(int r1, int r2, int r3, int r4, int r5, int rx, int ry, int rz) {
		if (c(r1, 300, 350) && c(r2, 220, 260) && c(r3, 220, 270) && c(r4, 150, 200) && c(r5, 220, 270)

				&& c(rx, 350, 400) && c(ry, 270, 310) && c(rz, 350, 400))
			return true;
		else
			return false;
	}

	boolean check_pass_User1(int r1, int r2, int r3, int r4, int r5, int rx, int ry, int rz) {
		if (c(r1, 320, 370) && c(r2, 230, 280) && c(r3, 230, 280) && c(r4, 160, 200) && c(r5, 250, 300)

				&& c(rx, 380, 420) && c(ry, 310, 350) && c(rz, 420, 460))
			return true;
		else
			return false;
	}

	boolean check_hi_User1(int r1, int r2, int r3, int r4, int r5, int rx, int ry, int rz) {
		if (c(r1, 430, 490) && c(r2, 260, 400) && c(r3, 260, 320) && c(r4, 240, 310) && c(r5, 160, 240)

				&& c(rx, 360, 420) && c(ry, 230, 400) && c(rz, 230, 470))
			return true;
		else
			return false;
	}

	boolean check_what_User1(int r1, int r2, int r3, int r4, int r5, int rx, int ry, int rz) {
		if (c(r1, 420, 470) && c(r2, 260, 320) && c(r3, 260, 320) && c(r4, 230, 380) && c(r5, 240, 300)

				&& c(rx, 290, 350) && c(ry, 340, 380) && c(rz, 400, 460))
			return true;
		else
			return false;
	}

	boolean check_Monday_User1(int r1, int r2, int r3, int r4, int r5, int rx, int ry, int rz) {
		if (c(r1, 350, 390) && c(r2, 250, 290) && c(r3, 260, 320) && c(r4, 170, 230) && c(r5, 190, 250)

				&& c(rx, 410, 440) && c(ry, 310, 350) && c(rz, 390, 430))
			return true;
		else
			return false;
	}

	boolean check_drink_User1(int r1, int r2, int r3, int r4, int r5, int rx, int ry, int rz) {
		if (c(r1, 410, 460) && c(r2, 260, 310) && c(r3, 250, 300) && c(r4, 200, 280) && c(r5, 230, 280)

				&& c(rx, 380, 440) && c(ry, 270, 320) && c(rz, 340, 380))
			return true;
		else
			return false;
	}

	boolean check_Tuesday_User1(int r1, int r2, int r3, int r4, int r5, int l1, int l2, int l3, int l4, int l5) {
		if (c(r1, 460, 500) && c(r2, 310, 360) && c(r3, 300, 350) && c(r4, 270, 310) && c(r5, 240, 300)

				&& c(l1, 465, 505) && c(l2, 460, 500) && c(l3, 470, 510) && c(l4, 420, 460) && c(l5, 200, 250))
			return true;
		else
			return false;
	}

	boolean check_Wednesday_User1(int r1, int r2, int r3, int r4, int r5, int rx, int ry, int rz, int l1, int l2,
			int l3, int l4, int l5, int lx, int ly, int lz) {
		if (c(r1, 450, 510) && c(r2, 310, 370) && c(r3, 310, 370) && c(r4, 260, 310) && c(r5, 250, 300)
				&& c(rx, 300, 340) && c(ry, 340, 380) && c(rz, 400, 440)

				&& c(l1, 450, 500) && c(l2, 450, 500) && c(l3, 470, 520) && c(l4, 420, 470) && c(l5, 200, 250)

				&& c(lx, 280, 320) && c(ly, 330, 370) && c(lz, 310, 350))
			return true;
		else
			return false;
	}

	boolean check_Thursday_User1(int r1, int r2, int r3, int r4, int r5, int rx, int ry, int rz) {
		if (c(r1, 460, 500) && c(r2, 300, 360) && c(r3, 320, 380) && c(r4, 260, 320) && c(r5, 250, 300)

				&& c(rx, 420, 470) && c(ry, 380, 420) && c(rz, 350, 400))
			return true;
		else
			return false;
	}

	boolean check_Friday_User1(int r1, int r2, int r3, int r4, int r5, int rx, int ry, int rz) {
		if (c(r1, 330, 390) && c(r2, 220, 300) && c(r3, 250, 310) && c(r4, 250, 300) && c(r5, 180, 260)

				&& c(rx, 350, 420) && c(ry, 260, 3200) && c(rz, 340, 400))
			return true;
		else
			return false;
	}

	boolean check_Saturday_User1(int r1, int r2, int r3, int r4, int r5, int rx, int ry, int rz) {
		if (c(r1, 330, 400) && c(r2, 220, 300) && c(r3, 250, 290) && c(r4, 160, 220) && c(r5, 180, 240)

				&& c(rx, 410, 460) && c(ry, 320, 380) && c(rz, 310, 370))
			return true;
		else
			return false;
	}

	boolean check_Mother_User1(int r1, int r2, int r3, int r4, int r5, int rx, int ry, int rz) {
		if (c(r1, 340, 400) && c(r2, 240, 290) && c(r3, 230, 280) && c(r4, 170, 230) && c(r5, 210, 270)

				&& c(rx, 380, 430) && c(ry, 270, 330) && c(rz, 360, 440))
			return true;
		else
			return false;
	}

	boolean check_Father_User1(int r1, int r2, int r3, int r4, int r5, int rx, int ry, int rz) {
		if (c(r1, 300, 380) && c(r2, 230, 300) && c(r3, 210, 280) && c(r4, 190, 240) && c(r5, 270, 320)

				&& c(rx, 420, 460) && c(ry, 310, 350) && c(rz, 340, 375))
			return true;
		else
			return false;
	}

	boolean check_Me_User1(int r1, int r2, int r3, int r4, int r5, int rx, int ry, int rz) {
		if (c(r1, 335, 390) && c(r2, 237, 290) && c(r3, 260, 310) && c(r4, 260, 320)

				&& c(rx, 385, 430) && c(ry, 360, 390) && c(rz, 410, 460))
			return true;
		else
			return false;
	}

	boolean check_You_User1(int r1, int r2, int r3, int r4, int r5, int rx, int ry, int rz) {
		if (c(r1, 330, 370) && c(r2, 250, 300) && c(r3, 220, 270) && c(r4, 250, 300) && c(r5, 210, 270)

				&& c(rx, 300, 340) && c(ry, 340, 380) && c(rz, 400, 440))
			return true;
		else
			return false;
	}

	boolean check_Dream_User1(int r1, int r2, int r3, int r4, int r5, int rx, int ry, int rz) {
		if (c(r1, 300, 360) && c(r2, 250, 300) && c(r3, 200, 260) && c(r4, 260, 310) && c(r5, 230, 380)

				&& c(rx, 400, 440) && c(ry, 280, 320) && c(rz, 320, 360))
			return true;
		else
			return false;
	}

	boolean check_Food_User1(int r1, int r2, int r3, int r4, int r5, int rx, int ry, int rz) {
		if (c(r1, 420, 460) && c(r2, 310, 360) && c(r3, 340, 390) && c(r4, 230, 280) && c(r5, 240, 300)

				&& c(rx, 340, 375) && c(ry, 277, 317) && c(rz, 370, 410))
			return true;
		else
			return false;
	}

	boolean check_Life_User1(int r1, int r2, int r3, int r4, int r5, int rx, int ry, int rz, int l1, int l2, int l3,
			int l4, int l5, int lx, int ly, int lz) {
		if (c(r1, 280, 340) && c(r2, 200, 260) && c(r3, 210, 270) && c(r4, 130, 200) && c(r5, 200, 280)

				&& c(rx, 350, 400) && c(ry, 300, 350) && c(rz, 400, 460)

				&& c(l1, 330, 390) && c(l2, 320, 390) && c(l3, 390, 430) && c(l4, 330, 390) && c(l5, 240, 300)

				&& c(lx, 330, 375) && c(ly, 290, 340) && c(lz, 280, 320))
			return true;
		else
			return false;
	}

	boolean check_Welcome_User1(int r1, int r2, int r3, int r4, int r5, int rx, int ry, int rz, int l1, int l2, int l3,
			int l4, int l5, int lx, int ly, int lz) {
		if (c(r1, 458, 498) && c(r2, 300, 350) && c(r3, 340, 390) && c(r4, 250, 300) && c(r5, 230, 280)

				&& c(rx, 288, 320) && c(ry, 340, 380) && c(rz, 390, 430)

				&& c(l1, 460, 510) && c(l2, 450, 500) && c(l3, 470, 510) && c(l4, 420, 472) && c(l5, 260, 315)

				&& c(lx, 280, 320) && c(ly, 310, 350) && c(lz, 310, 350))
			return true;
		else
			return false;
	}

	boolean check_Lebanon_User1(int r1, int r2, int r3, int r4, int r5, int rx, int ry, int rz, int l1, int l2, int l3,
			int l4, int l5, int lx, int ly, int lz) {
		if (c(r1, 448, 498) && c(r2, 300, 356) && c(r3, 320, 380) && c(r4, 240, 300) && c(r5, 250, 300)

				&& c(rx, 350, 390) && c(ry, 268, 310) && c(rz, 350, 400)

				&& c(l1, 466, 506) && c(l2, 457, 497) && c(l3, 472, 512) && c(l4, 420, 480) && c(l5, 240, 317)

				&& c(lx, 350, 400) && c(ly, 260, 300) && c(lz, 330, 382))
			return true;
		else
			return false;
	}

	boolean check_Brother_User1(int r1, int r2, int r3, int r4, int r5, int rx, int ry, int rz, int l1, int l2, int l3,
			int l4, int l5, int lx, int ly, int lz) {
		if (c(r1, 420, 470) && c(r2, 280, 330) && c(r3, 310, 360) && c(r4, 250, 300) && c(r5, 240, 290)

				&& c(rx, 430, 460) && c(ry, 340, 380) && c(rz, 330, 380)

				&& c(l1, 450, 500) && c(l2, 420, 470) && c(l3, 450, 500) && c(l4, 400, 450) && c(l5, 220, 280)

				&& c(lx, 420, 460) && c(ly, 350, 390) && c(lz, 360, 400))
			return true;
		else
			return false;
	}
	// timeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee

	boolean check_Time_User1(int r1, int r2, int r3, int r4, int r5, int rx, int ry, int rz, int l1, int l2, int l3,
			int l4, int l5, int lx, int ly, int lz) {
		if (c(r1, 300, 350) && c(r2, 220, 290) && c(r3, 210, 260) && c(r4, 170, 220) && c(r5, 210, 270)

				&& c(rx, 430, 470) && c(ry, 360, 396) && c(rz, 350, 490)

				&& c(l1, 460, 510) && c(l2, 380, 430) && c(l3, 410, 460) && c(l4, 340, 420) && c(l5, 190, 240)

				&& c(lx, 410, 450) && c(ly, 380, 420) && c(lz, 340, 380))
			return true;
		else
			return false;
	}

	boolean check_time_User1(int r1, int r2, int r3, int r4, int r5, int rx, int ry, int rz, int l1, int l2, int l3,
			int l4, int l5, int lx, int ly, int lz) {
		if (c(r1, 340, 400) && c(r2, 240, 290) && c(r3, 250, 310) && c(r4, 260, 310) && c(r5, 200, 250)

				&& c(rx, 400, 440) && c(ry, 400, 450) && c(rz, 360, 400)

				&& c(l1, 370, 420) && c(l2, 380, 430) && c(l3, 410, 460) && c(l4, 350, 400) && c(l5, 180, 240)

				&& c(lx, 410, 460) && c(ly, 340, 380) && c(lz, 340, 400))
			return true;
		else
			return false;
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	// user
	// 2/////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	boolean check_Sunday_User2(int r1, int r2, int r3, int r4, int r5) {
		if (c(r1, 440, 500) && c(r2, 250, 320) && c(r3, 290, 370) && c(r4, 270, 340) && c(r5, 190, 260))
			return true;
		else
			return false;
	}

	boolean check_Monday_User2(int r1, int r2, int r3, int r4, int r5, int rx, int ry, int rz) {
		if (c(r1, 340, 400) && c(r2, 200, 290) && c(r3, 240, 300) && c(r4, 280, 340) && c(r5, 190, 250)
				&& c(rx, 360, 410) && c(ry, 350, 400) && c(rz, 430, 500))

			return true;
		else
			return false;
	}

	boolean check_Tuesday_User2(int l1, int l2, int l3, int l4, int l5, int lx, int ly, int lz) {
		if (c(l1, 450, 510) && c(l2, 450, 500) && c(l3, 450, 510) && c(l4, 410, 470) && c(l5, 200, 300)
				&& c(lx, 340, 400) && c(ly, 290, 350) && c(lz, 270, 320))
			return true;
		else
			return false;
	}

	boolean check_Wednesday_User2(int r1, int r2, int r3, int r4, int r5, int rx, int ry, int rz) {
		if (c(r1, 330, 420) && c(r2, 230, 300) && c(r3, 230, 300) && c(r4, 255, 300) && c(r5, 170, 250)
				&& c(rx, 340, 390) && c(ry, 260, 310) && c(rz, 350, 420))
			return true;
		else
			return false;
	}

	boolean check_Thursday_User2(int r1, int r2, int r3, int r4, int r5, int rx, int ry, int rz) {
		if (c(r1, 460, 500) && c(r2, 320, 360) && c(r3, 340, 380) && c(r4, 270, 330) && c(r5, 300, 360)

				&& c(rx, 20, 70) && c(ry, 30, 80) && c(rz, 40, 390))
			return true;
		else
			return false;
	}

	boolean check_Friday_User2(int l1, int l2, int l3, int l4, int l5, int lx, int ly, int lz) {
		if (c(l1, 460, 510) && c(l2, 450, 500) && c(l3, 470, 520) && c(l4, 420, 460) && c(l5, 240, 290)

				&& c(lx, 270, 300) && c(ly, 320, 350) && c(lz, 330, 360))
			return true;
		else
			return false;
	}

	boolean check_Saturday_User2(int r1, int r2, int r3, int r4, int r5, int rx, int ry, int rz) {
		if (c(r1, 330, 400) && c(r2, 220, 300) && c(r3, 250, 290) && c(r4, 160, 220) && c(r5, 180, 240)

				&& c(rx, 410, 460) && c(ry, 320, 380) && c(rz, 310, 370))
			return true;
		else
			return false;
	}

	boolean check_Mother_User2(int r1, int r2, int r3, int r4, int r5, int rx, int ry, int rz) {
		if (c(r1, 330, 380) && c(r2, 430, 480) && c(r3, 460, 510) && c(r4, 390, 440) && c(r5, 220, 280)

				&& c(rx, 340, 400) && c(ry, 300, 350) && c(rz, 220, 270))
			return true;
		else
			return false;
	}

	boolean check_Father_User2(int r1, int r2, int r3, int r4, int r5, int rx, int ry, int rz) {
		if (c(r1, 300, 380) && c(r2, 230, 300) && c(r3, 210, 280) && c(r4, 190, 240) && c(r5, 270, 320)

				&& c(rx, 420, 460) && c(ry, 310, 350) && c(rz, 340, 375))
			return true;
		else
			return false;
	}

	boolean check_Me_User2(int r1, int r2, int r3, int r4, int r5, int rx, int ry, int rz) {
		if (c(r1, 335, 390) && c(r2, 237, 290) && c(r3, 260, 310) && c(r4, 260, 320)

				&& c(rx, 385, 430) && c(ry, 360, 390) && c(rz, 410, 460))
			return true;
		else
			return false;
	}

	boolean check_You_User2(int r1, int r2, int r3, int r4, int r5, int rx, int ry, int rz) {
		if (c(r1, 330, 370) && c(r2, 300, 370) && c(r3, 220, 270) && c(r4, 250, 300) && c(r5, 200, 280)

				&& c(rx, 420, 470) && c(ry, 329, 369) && c(rz, 340, 390))
			return true;
		else
			return false;
	}

	boolean check_Dream_User2(int r1, int r2, int r3, int r4, int r5, int rx, int ry, int rz) {
		if (c(r1, 300, 360) && c(r2, 260, 330) && c(r3, 210, 260) && c(r4, 280, 326) && c(r5, 270, 316)

				&& c(rx, 410, 450) && c(ry, 300, 340) && c(rz, 340, 400))
			return true;
		else
			return false;
	}

	boolean check_Food_User2(int r1, int r2, int r3, int r4, int r5, int rx, int ry, int rz) {
		if (c(r1, 442, 492) && c(r2, 320, 376) && c(r3, 316, 376) && c(r4, 250, 289) && c(r5, 260, 328)

				&& c(rx, 340, 375) && c(ry, 277, 317) && c(rz, 390, 410))
			return true;
		else
			return false;
	}

	boolean check_Life_User2(int r1, int r2, int r3, int r4, int r5, int rx, int ry, int rz, int l1, int l2, int l3,
			int l4, int l5, int lx, int ly, int lz) {
		if (c(r1, 450, 500) && c(r2, 290, 350) && c(r3, 310, 360) && c(r4, 260, 330) && c(r5, 240, 300)

				&& c(rx, 350, 400) && c(ry, 310, 370) && c(rz, 280, 320)

				&& c(l1, 460, 510) && c(l2, 460, 510) && c(l3, 470, 520) && c(l4, 410, 460) && c(l5, 240, 340)

				&& c(lx, 360, 400) && c(ly, 310, 370) && c(lz, 400, 440))
			return true;
		else
			return false;
	}

	boolean check_Welcome_User2(int r1, int r2, int r3, int r4, int r5, int rx, int ry, int rz, int l1, int l2, int l3,
			int l4, int l5, int lx, int ly, int lz) {
		if (c(r1, 458, 498) && c(r2, 308, 348) && c(r3, 330, 394) && c(r4, 240, 300) && c(r5, 300, 360)

				&& c(rx, 288, 318) && c(ry, 340, 380) && c(rz, 390, 430)

				&& c(l1, 464, 504) && c(l2, 453, 493) && c(l3, 470, 510) && c(l4, 420, 462) && c(l5, 260, 315)

				&& c(lx, 288, 308) && c(ly, 320, 369) && c(lz, 310, 340))
			return true;
		else
			return false;
	}

	boolean check_Lebanon_User2(int r1, int r2, int r3, int r4, int r5, int rx, int ry, int rz, int l1, int l2, int l3,
			int l4, int l5, int lx, int ly, int lz) {
		if (c(r1, 448, 498) && c(r2, 310, 356) && c(r3, 320, 380) && c(r4, 240, 300) && c(r5, 250, 300)

				&& c(rx, 350, 390) && c(ry, 268, 300) && c(rz, 360, 400)

				&& c(l1, 466, 506) && c(l2, 457, 497) && c(l3, 472, 512) && c(l4, 410, 463) && c(l5, 240, 317)

				&& c(lx, 340, 380) && c(ly, 260, 300) && c(lz, 330, 382))
			return true;
		else
			return false;
	}

	boolean check_Brother_User2(int r1, int r2, int r3, int r4, int r5, int rx, int ry, int rz, int l1, int l2, int l3,
			int l4, int l5, int lx, int ly, int lz) {
		if (c(r1, 450, 500) && c(r2, 300, 350) && c(r3, 320, 370) && c(r4, 270, 320) && c(r5, 200, 260)

				&& c(rx, 280, 310) && c(ry, 340, 370) && c(rz, 370, 410)

				&& c(l1, 460, 510) && c(l2, 450, 510) && c(l3, 470, 520) && c(l4, 410, 460) && c(l5, 200, 250)

				&& c(lx, 280, 310) && c(ly, 290, 330) && c(lz, 340, 380))
			return true;
		else
			return false;
	}

	boolean check_Time_User2(int r1, int r2, int r3, int r4, int r5, int rx, int ry, int rz, int l1, int l2, int l3,
			int l4, int l5, int lx, int ly, int lz) {
		if (c(r1, 350, 400) && c(r2, 240, 300) && c(r3, 280, 330) && c(r4, 290, 350) && c(r5, 260, 310)

				&& c(rx, 410, 450) && c(ry, 400, 440) && c(rz, 380, 420)

				&& c(l1, 460, 510) && c(l2, 450, 510) && c(l3, 470, 520) && c(l4, 410, 460) && c(l5, 200, 250)

				&& c(lx, 280, 310) && c(ly, 300, 340) && c(lz, 320, 360))
			return true;
		else
			return false;
	}
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	boolean bHi = false, bMr = false, bHappy = false, bEaster = false, bCong = false, blnSunday = false;

	boolean c(int n, int f, int t) {
		if (n <= t && n >= f)
			return true;
		else
			return false;
	}

	String getFrenchText(String s) {
		if (s.toLowerCase().equals("death"))
			return "mourir";
		else if (s.toLowerCase().equals("mother"))
			return "mère";
		else if (s.toLowerCase().equals("father"))
			return "père";
		else if (s.toLowerCase().equals("me"))
			return "moi";
		else if (s.toLowerCase().equals("you"))
			return "vous";
		else if (s.toLowerCase().equals("dream"))
			return "rêve";
		else if (s.toLowerCase().equals("food"))
			return "nourriture";
		else if (s.toLowerCase().equals("life"))
			return "la vie";
		else if (s.toLowerCase().equals("welcome"))
			return "bienvenue";
		else if (s.toLowerCase().equals("lebanon"))
			return "liban";
		else if (s.toLowerCase().equals("brother"))
			return "frère";
		else if (s.toLowerCase().equals("monday"))
			return "lundi";
		else if (s.toLowerCase().equals("tuesday"))
			return "mardi";
		else if (s.toLowerCase().equals("wednesday"))
			return "mercredi";
		else if (s.toLowerCase().equals("thursday"))
			return "jeudi";
		else if (s.toLowerCase().equals("friday"))
			return "vendredi";
		else if (s.toLowerCase().equals("saturday"))
			return "samedi";
		else if (s.toLowerCase().equals("sunday"))
			return "dimanche";
		else if (s.toLowerCase().equals("time"))
			return "temps";
		else if (s.toLowerCase().equals("medicine"))
			return "médicament";
		else if (s.toLowerCase().equals("cold"))
			return "froid";
		else if (s.toLowerCase().equals("drink"))
			return "boire";
		else if (s.toLowerCase().equals("today"))
			return "aujourd'hui";
		else if (s.toLowerCase().equals("water"))
			return "eau";
		else if (s.toLowerCase().equals("pass"))
			return "réussir";
		else if (s.toLowerCase().equals("what"))
			return "quel";
		else if (s.toLowerCase().equals("health"))
			return "santé";
		else if (s.toLowerCase().equals("fire"))
			return "feu";
		else if (s.toLowerCase().equals("sickness"))
			return "maladie";
		else if (s.toLowerCase().equals("hi"))
			return "salutation";
		else if (s.toLowerCase().equals("my name is"))
			return "je suis";
		else if (s.toLowerCase().equals("we are engineers"))
			return "nous sommes des ingénieurs";
		else if (s.toLowerCase().equals("this is"))
			return "c'est";
		else if (s.toLowerCase().equals("our senior project"))
			return "notre projet senior";
		else if (s.toLowerCase().equals("from"))
			return "de l'université de";

		return s;
	}

	String getArabicText(String s) {
		if (s.toLowerCase().equals("death"))
			return "ãÇÊ";
		else if (s.toLowerCase().equals("mother"))
			return "Çã";
		else if (s.toLowerCase().equals("father"))
			return "ÃÈ";
		else if (s.toLowerCase().equals("me"))
			return "ÃäÇ";
		else if (s.toLowerCase().equals("you"))
			return "ÃäÊã";
		else if (s.toLowerCase().equals("dream"))
			return "Íáã";
		else if (s.toLowerCase().equals("food"))
			return "ØÚÇã";
		else if (s.toLowerCase().equals("life"))
			return "ÍíÇÉ";
		else if (s.toLowerCase().equals("welcome"))
			return "ÃåáÇ æ ÓåáÇ";
		else if (s.toLowerCase().equals("lebanon"))
			return "áÈäÇä";
		else if (s.toLowerCase().equals("brother"))
			return "ÃÎ";
		else if (s.toLowerCase().equals("monday"))
			return "ÇáÅËäíä";
		else if (s.toLowerCase().equals("tuesday"))
			return "ÇáËáÇËÇÁ";
		else if (s.toLowerCase().equals("wednesday"))
			return "ÇáÇÑÈÚÇÁ";
		else if (s.toLowerCase().equals("thursday"))
			return "ÇáÎãíÓ";
		else if (s.toLowerCase().equals("friday"))
			return "ÇáÌãÚÉ";
		else if (s.toLowerCase().equals("saturday"))
			return "ÇáÓÈÊ";
		else if (s.toLowerCase().equals("sunday"))
			return "ÇáÇÍÏ";
		else if (s.toLowerCase().equals("time"))
			return "æÞÊ";
		else if (s.toLowerCase().equals("medicine"))
			return "ÏæÇÁ";
		else if (s.toLowerCase().equals("cold"))
			return "ÈÑÏ";
		else if (s.toLowerCase().equals("drink"))
			return "ÔÑÈ";
		else if (s.toLowerCase().equals("today"))
			return "Çáíæã";
		else if (s.toLowerCase().equals("water"))
			return "ãÇÁ";
		else if (s.toLowerCase().equals("pass"))
			return "äÌÍ";
		else if (s.toLowerCase().equals("what"))
			return "ãÇÐÇ";
		else if (s.toLowerCase().equals("health"))
			return "ÕÍÉ";
		else if (s.toLowerCase().equals("fire"))
			return "äÇÑ";
		else if (s.toLowerCase().equals("sickness"))
			return "ãÑÖ";
		else if (s.toLowerCase().equals("hi"))
			return "ÓáÇã";
		return "";
	}

	public void SpeachSelectedText(String text) {
		String strLangaue = this.getLanguage();

		// edit the SpeachSelectedText not to repeat the same word

		if (strLangaue.equals("en")) {
			Log.d(TAG, "Current word is " + t.getText().toString());
			if (!t.getText().toString().equals(text)) {
				t.setText(text);
				this.SpeachText_English();
			}

		} else if (strLangaue.equals("fr")) {
			if (!t.getText().toString().equals(this.getFrenchText(text))) {
				t.setText(this.getFrenchText(text));
				this.SpeachText_French();
			}

		}

		// else if(strLangaue.equals("ar")){
		// t.setText(this.getArabicText(text));
		// this.PlaySound(this.getAudioText_Arabic(text));
		// }
	}

	/*
	 * public void SpeachSelectedText(String text, boolean
	 * blnSpeakAllLanguages){ if(!blnSpeakAllLanguages)
	 * this.SpeachSelectedText(text); else{
	 * 
	 * } }
	 */

	int getAudioText_Arabic(String s) {
		if (s.toLowerCase().equals("death"))
			return R.raw.die;
		else if (s.toLowerCase().equals("mother"))
			return R.raw.mother;
		else if (s.toLowerCase().equals("father"))
			return R.raw.father;
		else if (s.toLowerCase().equals("me"))
			return R.raw.me;
		else if (s.toLowerCase().equals("you"))
			return R.raw.you;
		else if (s.toLowerCase().equals("dream"))
			return R.raw.dream;
		else if (s.toLowerCase().equals("food"))
			return R.raw.food;
		else if (s.toLowerCase().equals("life"))
			return R.raw.life;
		else if (s.toLowerCase().equals("welcome"))
			return R.raw.welcome;
		else if (s.toLowerCase().equals("lebanon"))
			return R.raw.lebanon;
		else if (s.toLowerCase().equals("brother"))
			return R.raw.brother;
		else if (s.toLowerCase().equals("monday"))
			return R.raw.monday;
		else if (s.toLowerCase().equals("tuesday"))
			return R.raw.tuesday;
		else if (s.toLowerCase().equals("wednesday"))
			return R.raw.wednesday;
		else if (s.toLowerCase().equals("thursday"))
			return R.raw.thursday;
		else if (s.toLowerCase().equals("friday"))
			return R.raw.friday;
		else if (s.toLowerCase().equals("saturday"))
			return R.raw.saturday;
		else if (s.toLowerCase().equals("sunday"))
			return R.raw.sunday;
		else if (s.toLowerCase().equals("time"))
			return R.raw.time;
		else if (s.toLowerCase().equals("medicine"))
			return R.raw.medicine;
		else if (s.toLowerCase().equals("cold"))
			return R.raw.cold;
		else if (s.toLowerCase().equals("drink"))
			return R.raw.drink;
		else if (s.toLowerCase().equals("today"))
			return R.raw.today;
		else if (s.toLowerCase().equals("water"))
			return R.raw.water;
		else if (s.toLowerCase().equals("pass"))
			return R.raw.pass;
		else if (s.toLowerCase().equals("what"))
			return R.raw.what;
		else if (s.toLowerCase().equals("health"))
			return R.raw.health;
		else if (s.toLowerCase().equals("fire"))
			return R.raw.fire;
		else if (s.toLowerCase().equals("sickness"))
			return R.raw.illness;
		else if (s.toLowerCase().equals("hi"))
			return R.raw.hi;
		return 0;
	}

	public void imgBtSettings_Click(View v) {
		Intent I = new Intent(this, BluetoothDiscovery.class);
		startActivity(I);
	}

	public void imgConnect_Click(View v) {
		this.ConnectTobluetooth();
	}

	TextToSpeech ttsEnglish, ttsFrench;

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 111) {
			Intent installTTSIntent = new Intent();
			installTTSIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
			startActivity(installTTSIntent);
		}
	}

	void SpeachText_English() {
		if (t.getText().toString().trim().equals(""))
			return;
		this.ttsEnglish.speak(t.getText().toString(), TextToSpeech.QUEUE_FLUSH, null);
	}

	void SpeachText_French() {
		if (t.getText().toString().trim().equals(""))
			return;
		this.ttsFrench.speak(t.getText().toString(), TextToSpeech.QUEUE_FLUSH, null);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		this.btAdapter = BluetoothAdapter.getDefaultAdapter();
		this.cboUser = (Spinner) findViewById(R.id.cboUser);

		this.prefReader = getSharedPreferences("BtPref", MODE_PRIVATE);

		this.lab = (TextView) findViewById(R.id.lab1);

		this.t = (EditText) findViewById(R.id.editText1);

		this.lab.setText("");

		this.radEnglish = (RadioButton) findViewById(R.id.radEnglish);
		this.radFrench = (RadioButton) findViewById(R.id.radFrench);
		this.radArabic = (RadioButton) findViewById(R.id.radArabic);

		this.radEnglish.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
				resetAll_F_Users();
			}
		});

		this.radFrench.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
				resetAll_F_Users();
			}
		});

		this.radArabic.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
				resetAll_F_Users();
			}
		});

		this.ConnectTobluetooth();

		// while(!blnResult);
		this.listenForBluetoothData();

		this.ttsEnglish = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {

			@Override
			public void onInit(int status) {
				if (status != TextToSpeech.ERROR) {
					Locale locale = new Locale("en", "EN");
					// tts.setLanguage(Locale.getDefault());
					ttsEnglish.setLanguage(locale);
				}
			}
		});

		this.ttsFrench = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {

			@Override
			public void onInit(int status) {
				if (status != TextToSpeech.ERROR) {
					Locale locale = new Locale("fr", "FR");
					// tts.setLanguage(Locale.getDefault());
					ttsFrench.setLanguage(locale);
				}
			}
		});

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	void listenForBluetoothData() {

		final Handler handler = new Handler();
		final byte delimiter = 10; // This is the ASCII code for a newline
									// character
		this.blnThreadIsActive = true;
		readBufferPosition = 0;
		readBuffer = new byte[1024];

		workerThread = new Thread(new Runnable() {
			public void run() {
				try {
					while (btSocket == null)
						;
					inputStream = btSocket.getInputStream();
					while (blnThreadIsActive) {

						// lab.setText("listening");
						try {
							int bytesAvailable = inputStream.available();
							if (bytesAvailable > 0) {
								byte[] packetBytes = new byte[bytesAvailable];
								inputStream.read(packetBytes);
								for (int i = 0; i < bytesAvailable; i++) {
									byte b = packetBytes[i];
									if (b == delimiter) {
										byte[] encodedBytes = new byte[readBufferPosition];
										System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
										final String strBluetoothData = new String(encodedBytes, "US-ASCII").trim();

										// TODO append the label of the move
										// here

										// here we log the data received via
										// bluetooth and skip calling the
										// function

										// try{
										// if (!dir.exists()) {
										// dir.mkdirs();
										// }
										//
										// if (!myFile.exists()){
										// out = new
										// OutputStreamWriter(openFileOutput(myFile.getAbsolutePath(),
										// MODE_PRIVATE));
										// }else{
										// out = new
										// OutputStreamWriter(openFileOutput(myFile.getAbsolutePath(),
										// MODE_APPEND));
										// }
										// out.write(strBluetoothData);
										// out.write('\n');
										// out.close();
										//
										// Log.d("SignApp", "A new received line
										// has been appended");
										//
										// }catch(java.io.IOException e){
										// Log.e("SignApp", e.getMessage());
										// }

										// end of edit
										readBufferPosition = 0;

										runOnUiThread(new Runnable() {

											@Override
											public void run() {
												// Toast.makeText(getApplicationContext(),
												// strBluetoothData,
												// Toast.LENGTH_SHORT).show();
												// t.setText(strBluetoothData);
												// ttt(null);

												// TODO uncommenet the following
												// line for processing
												// compaireInputValues(strBluetoothData);

												forwardInputValues(strBluetoothData);

												// lab.setText(String.format("Left
												// data:\n%s\nRight data:\n%s",
												// col[0], col[1]));
												// lab.setText("bbb");

											}
										});

										handler.post(new Runnable() {
											public void run() {

											}
										});
									} else {
										readBuffer[readBufferPosition++] = b;
									}
								}
							}
						} catch (Exception ex) {
							stopWorker = true;
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});

		workerThread.start();
	}

	boolean blnResult;

	boolean ConnectTobluetooth() {

		blnResult = false;
		if (!this.btAdapter.isEnabled()) {
			this.blnDisableBluetoothAdapterOnExit = true;
			this.btAdapter.enable();
			while (!this.btAdapter.isEnabled())
				;
		}
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					String strMACAddress = prefReader.getString("BT_MAC_ADDRESS", "");
					if (strMACAddress.equals("")) {
						runOnUiThread(new Runnable() {

							@Override
							public void run() {
								Toast.makeText(getApplicationContext(),
										"Paired device is not set. Please select one of the paired devices, or pair a new device",
										Toast.LENGTH_LONG).show();
							}
						});
						imgBtSettings_Click(null);
						return;
					}
					runOnUiThread(new Runnable() {

						@Override
						public void run() {
							Toast.makeText(getApplicationContext(), "Connecting...", Toast.LENGTH_SHORT).show();
						}
					});
					btDevice = btAdapter.getRemoteDevice(strMACAddress);
					Method m = btDevice.getClass().getMethod("createRfcommSocket", new Class[] { int.class });
					btSocket = (BluetoothSocket) m.invoke(btDevice, 1);
					btSocket.connect();
					runOnUiThread(new Runnable() {

						@Override
						public void run() {
							Toast.makeText(getApplicationContext(), "Connected", Toast.LENGTH_LONG).show();
							blnResult = true;
						}
					});
					Thread.sleep(1000);
				} catch (final Exception e) {
					e.printStackTrace();
					runOnUiThread(new Runnable() {

						@Override
						public void run() {
							if (e.getMessage().equals("read failed, socket might closed or timeout, read ret: -1")) {
								Toast.makeText(getApplicationContext(),
										"Not connected. Check that the device is turned on and not connected to another mobile.",
										Toast.LENGTH_LONG).show();
							} else
								Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
						}
					});
				}
			}
		}).start();
		return blnResult;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) // Override Keyback to do nothing
												// in this case.
		{
			try {
				this.blnThreadIsActive = false;
				if (this.btSocket != null)
					this.btSocket.close();
				if (this.blnDisableBluetoothAdapterOnExit)
					this.btAdapter.disable();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return super.onKeyDown(keyCode, event); // -->All others key will work
												// as usual
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			imgBtSettings_Click(null);
			return true;
		} else if (id == R.id.action_connect) {
			imgConnect_Click(null);
			return true;
		} else if (id == R.id.action_test) {
			this.SpeachSelectedText(t.getText().toString());
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

}
