package com.sign.language.translator;


import java.util.Set;

import com.sign.language.translator.R;

import android.support.v7.app.ActionBarActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class BluetoothDiscovery extends ActionBarActivity {


	   private Button btnTurnBtOn;
	   private Button btnTurnBtOff;
	   private Button listBtn;
	   private Button btnSave;
	   private TextView labStatus, labSelectedDeviceMACAddress;
	   private BluetoothAdapter btAdapter;
	   private Set<BluetoothDevice> pairedDevices;
	   private Spinner lstPairedDevices;
	   private ArrayAdapter<String> arrBTArrayAdapter, arrBT_MAC_ADDRESS;

	   SharedPreferences prefReader;
	   SharedPreferences.Editor prefEditor;
	   
	   public void btnSave_Click(View v){
		   if(this.labSelectedDeviceMACAddress.getText().toString().equals("")){
			   Toast.makeText(getApplicationContext(), "Please select a device first", Toast.LENGTH_LONG).show();
			   return;
		   }
		   this.prefEditor.clear();
		   this.prefEditor.putString("BT_MAC_ADDRESS", this.labSelectedDeviceMACAddress.getText().toString());
		   if(this.prefEditor.commit()){
			   Toast.makeText(getApplicationContext(), "Parameters saved", Toast.LENGTH_LONG).show();
			   finish();
		   }
	   }
	  
	   @Override
	   protected void onCreate(Bundle savedInstanceState) {
	      super.onCreate(savedInstanceState);
	      setContentView(R.layout.activity_bluetooth_discovery);
	      
	      getActionBar().setDisplayHomeAsUpEnabled(true);
	      
	      this.labStatus = (TextView) findViewById(R.id.labStatus);
	      this.btnTurnBtOn = (Button)findViewById(R.id.btnTurnBtOn);
	      this.btnTurnBtOff = (Button)findViewById(R.id.btnTurnBtOff);
	      this.listBtn = (Button)findViewById(R.id.btnListPairedDevices);
	      this.lstPairedDevices = (Spinner)findViewById(R.id.lstPairedDevices);
	      this.btnSave = (Button)findViewById(R.id.btnSave);
	      this.labSelectedDeviceMACAddress = (TextView)findViewById(R.id.labSelectedDeviceMACAddress);
	      
	      this.prefReader = getSharedPreferences("BtPref", MODE_PRIVATE);
	      this.prefEditor = this.prefReader.edit();
	      this.ReadDefaultPreferences();
	      /*
1	MODE_APPEND

This will append the new preferences with the already existing preferences
2	MODE_ENABLE_WRITE_AHEAD_LOGGING

Database open flag. When it is set , it would enable write ahead logging by default
3	MODE_MULTI_PROCESS

This method will check for modification of preferences even if the sharedpreference instance has already been loaded
4	MODE_PRIVATE

By setting this mode , the file can only be accessed using calling application
5	MODE_WORLD_READABLE

This mode allow other application to read the preferences
6	MODE_WORLD_WRITEABLE

This mode allow other application to write the preferences
	       */

	      
	      // take an instance of BluetoothAdapter - Bluetooth radio
	      this.btAdapter = BluetoothAdapter.getDefaultAdapter();
		  
	      if(this.btAdapter == null) {
	    	  this.btnTurnBtOn.setEnabled(false);
	    	  this.btnTurnBtOff.setEnabled(false);
	    	  this.listBtn.setEnabled(false);
	    	  this.btnSave.setEnabled(false);
	    	  this.labStatus.setText("Status: not supported");
	    	  
	    	  Toast.makeText(getApplicationContext(),"Your device does not support Bluetooth",
	         		 Toast.LENGTH_LONG).show();
	      } else {
			   if(this.btAdapter.isEnabled()) {
				   this.labStatus.setText("Status: Enabled");
			   } else {   
				   this.labStatus.setText("Status: Disabled");
			   }
		      // create the arrayAdapter that contains the BTDevices, and set it to the ListView
		      this.arrBTArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
		      this.arrBT_MAC_ADDRESS = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
		      this.lstPairedDevices.setAdapter(arrBTArrayAdapter);
		      
		      this.btnListPairedDevices_Click(null);
		      this.lstPairedDevices.setOnItemSelectedListener(new OnItemSelectedListener() {

				@Override
				public void onItemSelected(AdapterView<?> parent, View view,
						int position, long id) {
					labSelectedDeviceMACAddress.setText(arrBT_MAC_ADDRESS.getItem(position));
				}

				@Override
				public void onNothingSelected(AdapterView<?> parent) {
					// TODO Auto-generated method stub
					
				}
			});
		      
	      }
	   }
	   
	   void ReadDefaultPreferences(){
		   this.labSelectedDeviceMACAddress.setText(this.prefReader.getString("BT_MAC_ADDRESS", ""));
	   }

	   public void btnTurnBtOn_Click(View view){
	      /*if (!btAdapter.isEnabled()) {
	         Intent turnOnIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
	         startActivityForResult(turnOnIntent, REQUEST_ENABLE_BT);

	         Toast.makeText(getApplicationContext(),"Bluetooth turned on" ,
	        		 Toast.LENGTH_LONG).show();
	      }
	      else{
	         Toast.makeText(getApplicationContext(),"Bluetooth is already on",
	        		 Toast.LENGTH_LONG).show();
	      }*/
		  //if(!this.btAdapter.isEnabled()){
			  if(this.btAdapter.enable())
				  this.labStatus.setText("Status: Enabled");
		  //}
	   }
	   
	   public void btnTurnBtOff_Click(View view){
		      /*if (!btAdapter.isEnabled()) {
		         Intent turnOnIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		         startActivityForResult(turnOnIntent, REQUEST_ENABLE_BT);

		         Toast.makeText(getApplicationContext(),"Bluetooth turned on" ,
		        		 Toast.LENGTH_LONG).show();
		      }
		      else{
		         Toast.makeText(getApplicationContext(),"Bluetooth is already on",
		        		 Toast.LENGTH_LONG).show();
		      }*/
				  if(this.btAdapter.disable())
					  this.labStatus.setText("Status: Disabled");
		   }
	   
	  /* @Override
	   protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		   // TODO Auto-generated method stub
		   if(requestCode == REQUEST_ENABLE_BT){
			   if(btAdapter.isEnabled()) {
				   this.labStatus.setText("Status: Enabled");
			   } else {   
				   this.labStatus.setText("Status: Disabled");
			   }
		   }
	   }*/
	   
	   public void btnListPairedDevices_Click(View view){
		   if(!this.btAdapter.isEnabled()){
			   Toast.makeText(getApplicationContext(), "Turn bluetooth ON first", Toast.LENGTH_LONG).show();
			   return;
		   }
		  // get paired devices
	      pairedDevices = btAdapter.getBondedDevices();
	      
	      // put it's one to the adapter
	      this.arrBT_MAC_ADDRESS.clear();
	      this.arrBTArrayAdapter.clear();
	      for(BluetoothDevice device : pairedDevices){
	    	  this.arrBTArrayAdapter.add(device.getName() + "\n" + device.getAddress());
	    	  this.arrBT_MAC_ADDRESS.add(device.getAddress());
	      }

	      Toast.makeText(getApplicationContext(),"Paired Devices Listed", Toast.LENGTH_SHORT).show();
	      
	   }
	   
	   final BroadcastReceiver btReceiver = new BroadcastReceiver() {
		    public void onReceive(Context context, Intent intent) {
		        String action = intent.getAction();
		        // When discovery finds a device
		        if (BluetoothDevice.ACTION_FOUND.equals(action)) {
		             // Get the BluetoothDevice object from the Intent
		        	 BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
		        	 // add the name and the MAC address of the object to the arrayAdapter
		             arrBTArrayAdapter.add(device.getName() + "\n" + device.getAddress());
		             arrBTArrayAdapter.notifyDataSetChanged();
		        }
		    }
		};
		
	   public void findDevices(View view) {
		   if (btAdapter.isDiscovering()) {
			   // the button is pressed when it discovers, so cancel the discovery
			   btAdapter.cancelDiscovery();
		   }
		   else {
				arrBTArrayAdapter.clear();
				btAdapter.startDiscovery();
				
				registerReceiver(btReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));	
			}    
	   }
	   
	   
	   @Override
	   protected void onDestroy() {
		   super.onDestroy();
		   if(btReceiver.isOrderedBroadcast())
			   unregisterReceiver(btReceiver);
	   }
	   
		@Override
		public boolean onOptionsItemSelected(MenuItem item) {
			finish();
			return super.onOptionsItemSelected(item);
		}
}
