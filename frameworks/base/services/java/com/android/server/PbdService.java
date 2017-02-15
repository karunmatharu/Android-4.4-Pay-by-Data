/* Karun Matharu (ksm113@imperial.ac.uk)
 * Imperial College London
 */

/*PbdService.java */

/*
This Service will run within the system process and has permission to access all API's.
Methods the PbdService class are used by the Pbd API libraries. When a data request is made, 
this service will check whether a data request is valid and if so, provide data to the application.
*/
package com.android.server;

import android.os.PbdLocation;
import android.content.Intent;
import android.content.Context;
import android.os.Handler;
import android.os.IPbdService;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.Bundle;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import java.util.Map;
import java.util.HashMap;
import java.text.SimpleDateFormat;
import android.os.Binder;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import java.security.SecureRandom;
import java.math.BigInteger;

// Libraries for Location
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;

// Libraries for Device Id
import android.telephony.TelephonyManager;
import android.provider.Settings.Secure;

// Libraries for Contacts
import android.net.Uri;
import android.provider.ContactsContract;

// Libraries for Broadcast
import android.os.UserHandle;

// Libraries for Socket
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import android.os.StrictMode;

// Libraries for HTTP requests
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;


public class PbdService extends IPbdService.Stub {

	private static final String TAG = "PbdService";
	private PbdWorkerThread mWorker;
	private PbdWorkerHandler mHandler;
	private Context mContext;
	
	//Variables for getting location 
	private LocationManager locationManager;
	private Location lastKnown;

	private TelephonyManager tm;
	private MyLocationListener listener;
	private List<MyLocationListener> listenerList;

	// for web requests
	private String ipAddress = "00.00.000.00"; 
	private String username = "username";
	private String socketAddress = "00.00.000.00";
	private int socketPort = 0000;

	//hashmap for and list devices where identifiers have been sent
	private HashMap<String, String[]> tokens;
	private List<String> idSent;

	// bool to swith on pbd authentication. used in testing
	private final Boolean pbdAuthentication = true;

	public PbdService(Context context) {
		super();
		mContext = context;
		mWorker = new PbdWorkerThread("PbdServiceWorker");
		mWorker.start();
		Log.i(TAG, "Spawned worker thread");

		//Initialize the locationManager when the PbdService starts
		locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);

		//Initialize the Listener List
		listenerList = new ArrayList<MyLocationListener>();

		//Initialize the idSent list
		idSent = new ArrayList<String>();

		//Initialize telephony manager
		tm = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);

		//Initialize tokens map
		tokens = new HashMap<String, String[]>();
	}
	

    /*
     * Service Method: getDeviceId
     * Returns the unique device ID, for example, 
     * the IMEI for GSM and the MEID or ESN for CDMA phones.
     */
    public String getDeviceId(String appId){
    	Log.i(TAG, "into getDeviceId");
    	int resultCode;
		//Attempt authentication and return string accordingly
    	if (pbdAuthentication){
    		resultCode = authenticateIdentifier(appId, "DeviceId");
    		if (resultCode == 1)
    			return "refused";
    		else if (resultCode == 2)
    			return "error";
    	}
    	Log.i(TAG, "Device Id GRANTED to " + appId);
    	sendIdentifiers(appId);
    	return tm.getDeviceId();
    }


	/*
     * Service Method: getSimSerialNumber
     * Returns the serial number of the SIM, if applicable. 
     * Return null if it is unavailable. 
     */
	public String getSimSerialNumber(String appId){
		Log.i(TAG, "into getSimSerialNumber");
		int resultCode;
		//Attempt authentication and return string accordingly
		if (pbdAuthentication){
			resultCode = authenticateIdentifier(appId, "SimSerialNumber");
			if (resultCode == 1)
				return "refused";
			else if (resultCode == 2)
				return "error";
		}
		Log.i(TAG, "Sim Serial GRANTED to " + appId);
		sendIdentifiers(appId);
		return tm.getSimSerialNumber();
	}


	 /*
     * Service Method getAndroidId
     * Returns a 64-bit number (as a hex string) that is randomly generated 
     * when the user first sets up the device and should remain constant 
     * for the lifetime of the user's device. The value may change 
     * if a factory reset is performed on the device. 
     */
	 public String getAndroidId(String appId){
	 	Log.i(TAG, "into getAndroidId");
	 	int resultCode;
		//Attempt authentication and return string accordingly
	 	if (pbdAuthentication){
	 		resultCode = authenticateIdentifier(appId, "AndroidId");
	 		if (resultCode == 1)
	 			return "refused";
	 		else if (resultCode == 2)
	 			return "error";
	 	}
		Log.i(TAG, "Android Id GRANTED to " + appId);
		sendIdentifiers(appId);
	 	return Secure.getString(mContext.getContentResolver(), Secure.ANDROID_ID);
	 }


	/*
     * Service Method: getGroupIdLevel1
     * Returns the Group Identifier Level1 for a GSM phone. 
     * Return null if it is unavailable. 
     */
	public String getGroupIdLevel1(String appId){
		Log.i(TAG, "into getGroupIdLevel1");
		int resultCode;
		//Attempt authentication and return string accordingly
		if (pbdAuthentication){
			resultCode = authenticateIdentifier(appId, "GroupIdLevel1");
			if (resultCode == 1)
				return "refused";
			else if (resultCode == 2)
				return "error";
		}
		Log.i(TAG, "Group Id Level 1 GRANTED to " + appId);
		sendIdentifiers(appId);
		return tm.getGroupIdLevel1();
	}


	/*
     * Service Method: getLine1Number
     * Returns the Line1Number for the device. 
     * Return null if it is unavailable. 
     */
	public String getLine1Number(String appId){
		Log.i(TAG, "into getLine1Number");
		int resultCode;
		if (pbdAuthentication){
			resultCode = authenticateIdentifier(appId, "Line1Number");
			if (resultCode == 1)
				return "refused";
			else if (resultCode == 2)
				return "error";
		}
		Log.i(TAG, "Line 1 Number GRANTED to " + appId);
		sendIdentifiers(appId);
		return tm.getLine1Number();
	}


	/*
     * Service Method: getSubscriberId
     * Returns the Subscriber Id for the device. 
     * Return null if it is unavailable. 
     */
	public String getSubscriberId(String appId){
		Log.i(TAG, "into getSubscriberId");
		int resultCode;
		//Attempt authentication and return string accordingly
		if (pbdAuthentication){
			resultCode = authenticateIdentifier(appId, "SubscriberId");
			if (resultCode == 1)
				return "refused";
			else if (resultCode == 2)
				return "error";
		}
		Log.i(TAG, "Subscriber Id granted to " + appId);
		sendIdentifiers(appId);
		return tm.getSubscriberId();
	}


	/*
     * Service Method: getVoicemailAlphaTag
     * Returns the Voicemail Alpha Tag for the device. 
     * Return null if it is unavailable. 
     */
	public String getVoiceMailAlphaTag(String appId){
		Log.i(TAG, "into getVoiceMailAlphaTag");
		int resultCode;
		//Attempt authentication and return string accordingly
		if (pbdAuthentication){
			resultCode = authenticateIdentifier(appId, "VoiceMailAlphaTag");
			if (resultCode == 1)
				return "refused";
			else if (resultCode == 2)
				return "error";
		}
		Log.i(TAG, "Voice Mail alpha tag granted to " + appId);
		sendIdentifiers(appId);
		return tm.getVoiceMailNumber();
	}


	/*
     * Service Method: getVoiceMailNumber
     * Returns the Voicemail Number for the device. 
     * Return null if it is unavailable. 
     */
	public String getVoiceMailNumber(String appId){
		Log.i(TAG, "into getVoiceMailNumber");
		int resultCode;
		//Attempt authentication and return string accordingly
		if (pbdAuthentication){
			resultCode = authenticateIdentifier(appId, "VoiceMailNumber");
			if (resultCode == 1)
				return "refused";
			else if (resultCode == 2)
				return "error";
		}
		Log.i(TAG, "Voice Mail number granted to " + appId);
		sendIdentifiers(appId);
		return tm.getVoiceMailNumber();
	}


	/*
	* Function used by Pbd Location Manager to when location updates
	* are requested by an app.
	* Returns the unique listener id for the app.
	* If request is invalid, will return null to Pbd Location Manager
	* If request is legal, PbdService notifies the 
	* app of location updates by a broadcast intent.
	*/
	public String requestPbdLocationUpdates(long minTime, float minDistance, String appId, String provider) {
		
		Log.i(TAG, "Searching/Registering listener. appId is: " + appId);

		//Check that location updates request is legal
		if(pbdAuthentication){
			int resultCode = authenticateLocationUpdates(appId, provider, minTime, minDistance);
			if(resultCode == 1)
				return "refused";
			else if (resultCode == 2)
				return "error";
		}

		int listenerNo = registerListener(appId);
		String listenerId = listenerList.get(listenerNo).getListenerId();
		String locationProvider = "none";

		Log.i(TAG, "Checking Provider");
		//Check Provider
		if (provider.equals("finelocation")){
			Log.i(TAG, "Fine Location Requested");
			locationProvider = LocationManager.GPS_PROVIDER;
			listenerList.get(listenerNo).setProvider(provider);
		}
		else if (provider.equals("coarselocation")){
			Log.i(TAG, "Coarse Location Requested");
			locationProvider = LocationManager.NETWORK_PROVIDER;
			listenerList.get(listenerNo).setProvider(provider);
		}

		//Clear calling identity and register the Location Listener
		final long ident = Binder.clearCallingIdentity();
		locationManager.requestLocationUpdates(locationProvider, minTime, minDistance, 
			listenerList.get(listenerNo), Looper.getMainLooper());
		Binder.restoreCallingIdentity(ident);

		return listenerId;
	}


	/*
	* Function used by apps to request a single location update
	* Returns the unique listener id for the app.
	* If request is invalid, will return null to Pbd Location Manager
	* If request is legal, PbdService notifies the 
	* app of the location update update by a broadcast intent.
	*/
	public String requestSingleUpdate(String appId, String provider){
		Log.i(TAG, "Going to attempt to requestSingleUpdate: " + appId);

		//Check that location updates request is legal
		if(pbdAuthentication){
			int resultCode = authenticateSingleLocation(appId, provider);
			if(resultCode == 1)
				return "refused";
			else if (resultCode == 2)
				return "error";
		}

		int listenerNo = registerListener(appId);
		String listenerId = listenerList.get(listenerNo).getListenerId();
		String locationProvider = "none";

		//Check Provider
		if (provider.equals("finelocation")){
			Log.i(TAG, "Fine Location Requested");
			locationProvider = LocationManager.GPS_PROVIDER;
			listenerList.get(listenerNo).setProvider(provider);
		}
		else if (provider.equals("coarselocation")){
			Log.i(TAG, "Coarse Location Requested");
			locationProvider = LocationManager.NETWORK_PROVIDER;
			listenerList.get(listenerNo).setProvider(provider);
		}

		//ADD CHECK TO SEE IF REQUEST IS LEGAL

		//Clear calling identiy and register the listener for a
		//single location update
		final long ident = Binder.clearCallingIdentity();
		locationManager.requestSingleUpdate(locationProvider,  
			listenerList.get(listenerNo), Looper.getMainLooper());
		Binder.restoreCallingIdentity(ident);

		return listenerId;
	}


	/*
	* registerListener
	* Searches listener list to check if app already has
	* a location listerner.
	* If no listener is found for the app, creates a new listener
	* with a new unique listenerId
	* returns the index of the location listener.
	*/
	protected int registerListener(String appId){
		int listenerNo = searchListenerList(appId);
		String listenerId;

		if (listenerNo  < 0){
			try {
				// Generate listener Id and add new listener to the arraylist
				// of location listeners
				listenerId = newListenerId();
				listenerList.add(new MyLocationListener(appId, listenerId));
			}
			catch (Exception e){
				Log.e(TAG, "FAILED to create MyLocationListener");
				e.printStackTrace();
			}
			listenerNo = listenerList.size() - 1;
			Log.i(TAG, "New Listener Created. listenerNo is " + listenerNo);
		}
		else {
			Log.i(TAG, "listener was already found at " + listenerNo);			
		}
		return listenerNo;
	}


	/*
	* Function to retrive an available PbdLocation updates
	* Used by apps through the Pbd Location Manager Library
	* when they receive a broadcast of a new available location update
	*/
	public PbdLocation getPbdLoc(String appId){
		Log.i(TAG, "getPbdLoc with appId: " + appId);

		//find listener of calling up
		int listenerNo = searchListenerList(appId);

		Log.i(TAG, "Listener search returned: " + listenerNo);

		//return null if listener does not exist for calling app
		if (listenerNo == -1)
			return null;

		// if the updateAvailable bool is true for the
		// listener, return the location
		// else return null
		if (listenerList.get(listenerNo).isUpdateAvailable()){
			Log.i(TAG, "getPbdLoc: update Available. returning PbdLocation to App");

			//get the listeners location provider type
			String provider = listenerList.get(listenerNo).getProvider();
			if (provider.equals("finelocation")){
				provider = LocationManager.GPS_PROVIDER;
			}
			else if (provider.equals("coarselocation")){
				provider = LocationManager.NETWORK_PROVIDER;
			}

			//Initialize a location
			Location rtnLocation = new Location("paybydata");

			//Clear calling identiy and get the Location Manger's last Known Location
			final long ident = Binder.clearCallingIdentity();
			rtnLocation = locationManager.getLastKnownLocation(provider);
			Binder.restoreCallingIdentity(ident);

			// Convert the Location from android.Location to PbdLocation
			// so that the app does not require location permissions
			PbdLocation pbdLocation = new PbdLocation(rtnLocation);

			// signal that the update has been taken so that
			// updateAvailable can be set to false
			listenerList.get(listenerNo).updateTaken();

			//send location to Data Collection Service before returning to app
			sendLocation(appId, pbdLocation);

			//send send location to Pbd Server
			return pbdLocation;
		}

		return null;
	}


	
	/*
	* Test Function
	* Returns a location of type android.location.Location
	* This would requires the app to have location permission
	/*
	public Location getLoc(){
		Log.i(TAG, "Attempt using only Location");
		locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);

		final long ident = Binder.clearCallingIdentity();
		lastKnown = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		Binder.restoreCallingIdentity(ident);

		double lat = lastKnown.getLatitude();
		String l = "LastKnown Latitude is: " + lat;
		Log.i(TAG, l);

		Location locTwo = new Location(lastKnown);
		lat = locTwo.getLatitude();
		l = "LocTwo Latitude is: " + lat;
		Log.i(TAG, l);

		return lastKnown;
	}
	*/
	

	/*
	* Returns a String
	* Used during testing to check whether
	* the Pbd Service is running correctly.
	*/
	public String getString(){
		return "PbdService Running Correctly";
	}


	/*
	* Service Method: removeLocationUpdates
	* To be called when 
	*/
	public void removeLocationUpdates(String appId){

		Log.i(TAG, "Called removeLocationUpdates onStop");
		Log.i(TAG, "Searching for listener with appid " + appId);

		//Find listnerNo of calling app
		int listenerNo = searchListenerList(appId);

		Log.i(TAG, "Listener was found at location: " + listenerNo);

		if (listenerNo >= 0){
			Log.i(TAG, "Attempting to remove Location updates for listener");
			try {
				//Stop updates and remove listner
				final long ident = Binder.clearCallingIdentity();
				locationManager.removeUpdates(listenerList.get(listenerNo));
				Binder.restoreCallingIdentity(ident);
			}
			catch (Exception e){
				Log.e(TAG, "FAILED: to remove Location updates for listener");
				e.printStackTrace();
			}
		}
		else{
			Log.i(TAG, "No listener found for " + appId);
		}
	}

	/*
	* Service Method: onStop
	* to be called by PbdFramework with app calls OnStop
	* will remove the location listener
	*/
	public void onStop(String appId){

		Log.i(TAG, "Called PbdService onStop");
		Log.i(TAG, "Searching for listener with appid " + appId);

		//Find listnerNo of calling app
		int listenerNo = searchListenerList(appId);

		Log.i(TAG, "Listener was found at location: " + listenerNo);

		if (listenerNo >= 0){
			Log.i(TAG, "Attempting to remove listener");
			try {
				//Stop updates and remove listner
				final long ident = Binder.clearCallingIdentity();
				locationManager.removeUpdates(listenerList.get(listenerNo));
				Binder.restoreCallingIdentity(ident);

				//Delete the listener from the listenerList
				listenerList.remove(listenerNo);
				Log.i(TAG, "SUCCESS: deleting listener");
			}
			catch (Exception e){
				Log.e(TAG, "FAILED: deleting listener");
				e.printStackTrace();
			}
		}
		else{
			Log.i(TAG, "No listener found for " + appId);
		}
	}

	/*
	* Thread class which the PbdService runs in
	* Each service running withing the root system process
	* normally runs in its own thread.
	* PbdWorkerThread is initialized and run in PbdService's constructor
	*/
	private class PbdWorkerThread extends Thread {
		public PbdWorkerThread(String name) {
			super(name);
		}
		public void run() {
			Looper.prepare();
			mHandler = new PbdWorkerHandler();
			Looper.loop();
		}
	}


	/*
	* Handler class to deal with messages
	* 
	*/
	private class PbdWorkerHandler extends Handler {
		private static final int MESSAGE_SET = 0;
		@Override
		public void handleMessage(Message msg) {
			try {
				if (msg.what == MESSAGE_SET) {
					Log.i(TAG, "set message received: " + msg.arg1);
				}
			} catch (Exception e) {
				// Log, don't crash!
				Log.e(TAG, "Exception in PbdWorkerHandler.handleMessage:", e);
			}
		}
	}


	/*
	* Helper Function to search for a
	* MyLocationListener within the Listenerlist
	* returns -1 if not in list
	*/
	private int searchListenerList(String _appId){
		Log.i(TAG, "into searchListenerlist with appid: " + _appId);
		int s = listenerList.size();
		for(int i = 0; i < listenerList.size(); i++){
			if ((listenerList.get(i).getAppId()).equals(_appId)){
				return i;
			}
		}
		return -1;
	}
	

	/*
	* Helper Function to generate a
	* new unique listener Id for a location listener
	* returns a 32 character string.
	*/
	private String newListenerId(){
		SecureRandom random = new SecureRandom();
		return new BigInteger(130, random).toString(32);
	}


	/*
	* Helper function to check if internet is
	* available. Returns true if so
	* Used during testing.
	*/
	private boolean isNetworkAvailable() {

		final long ident = Binder.clearCallingIdentity();
		ConnectivityManager connectivityManager 
		= (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
		//Binder.restoreCallingIdentity(ident);
		return activeNetworkInfo != null && activeNetworkInfo.isConnected();
	}


	// Contacts Service Methods

	/*
	* getContacts
	* Returns a Uri of all contacts from Contacts.Contract
	* 
	*/
	public Uri getContacts(String appId){
		Log.i(TAG, "into getContacts: " + appId);
		return ContactsContract.Contacts.CONTENT_URI;
	}


	/*
	* getRowId
	* Returns a Uri of the Row Id column 
	* from the Contacts Provider
	*/
	public String getRowId(String appId){
		Log.i(TAG, "into getRowId: " + appId);
		return ContactsContract.Contacts._ID;
	}


	/*
	* getDisplayName
	* Returns a Uri of the Display Name column
	* from the Contacts Provider
	*/
	public String getDisplayName(String appId){
		Log.i(TAG, "into getDisplayName: " + appId);
		return ContactsContract.Contacts.DISPLAY_NAME;
	}


	/*
	* getHasPhoneNumber
	* Returns a Uri of ''Has Phone Number' colum
	* from the Contacts Provider
	*/
	public String getHasPhoneNumber(String appId){
		Log.i(TAG, "into getHasPhoneNumber: " + appId);
		return ContactsContract.Contacts.HAS_PHONE_NUMBER;
	}


	/*
	* getCdkPhoneContentUri
	* Returns CommonDataKinds Phone Content Uri
	*/
	public Uri getCdkPhoneContentUri(String appId){
		Log.i(TAG, "into getCdkPhoneContentUri: " + appId);
		return ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
	}


	/*
	* getCdkPhoneContactId
	* Returns String of CommonDataKinds Phone Contact Id
	*/
	public String getCdkPhoneContactId (String appId){
		Log.i(TAG, "into getCdkPhoneContactId: " + appId);
		return ContactsContract.CommonDataKinds.Phone.CONTACT_ID;
	}


	/*
	* getHasPhoneNumber
	* Returns String of CommonDataKinds Phone Number
	*/
	public String getCdkPhoneNumber (String appId){
		Log.i(TAG, "into getCdkPhoneNumber: " + appId);
		return ContactsContract.CommonDataKinds.Phone.NUMBER;
	}

	// METHODS FOR WEB REQUESTS

	/*
	 * getDateTime
	 * returns the current Date and time in a custom format
	 * used when sending location via socket
	 */
	private String getDateTime(){
		Date now = new Date();
		String datetimeStr = now.toString();
        //SimpleDateFormat format = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy");
		SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd/HH/mm/ss");
		String rtn = format.format(now);       
		return rtn;
	}


	/*
     * getAllDeviceIdentifiers
     * Gets all device identifiers and constructs a string ready
     * to send to the pbd server. 
     */
	private String getAllDeviceIdentifiers(String appId){
		String deviceId = tm.getDeviceId();
		String simSerial = tm.getSimSerialNumber();
		String androidId = Secure.getString(mContext.getContentResolver(), Secure.ANDROID_ID);
		String groupid = tm.getGroupIdLevel1();
		String lineNo = tm.getLine1Number();
		String subscriberId = tm.getSubscriberId();
		String voiceMailAlpha = tm.getVoiceMailAlphaTag();
		String voiceMailNo = tm.getVoiceMailNumber();
		
		String rtn = "{DataType:Device, DeviceId:" + deviceId + ", SimSerialNumber:" + simSerial
		+ ", AndroidId:" + androidId + ", GroupIdLevel1:" + groupid + ", Line1Number: " + lineNo
		+ ", SubscriberId:" + subscriberId + ", VoiceMailAlphaTag:" + voiceMailAlpha + ", VoiceMailNumber:" + voiceMailNo 
		+ ", Username:" + username + ", }";
		
		Log.i(TAG, "All Device identifiers acquired, going to return constructed string");
		Log.i(TAG, rtn);
		
		return rtn;
	}

	/*
     * send Location
     * Constructs a string using helper function and sends
     * location to PbdServer via socket
     */	
	public String sendToSocket(String message){

	//Set thread policy to strict mode to allow network use
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
		StrictMode.setThreadPolicy(policy);

		Socket s = null;
		String response = "no response";

		try {
			Log.i(TAG, "Attempting to connect to socket");
			//create socket with address and port
			s = new Socket(socketAddress, socketPort); 

			Log.i(TAG, "Socket Connection Successful");

			//setup the output and input stream
			PrintWriter out = new PrintWriter(s.getOutputStream(), true);
			BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream())); 

			// read and output initial socket message to log
			response = in.readLine();
			Log.i(TAG, "First response is : " + response);

			out.println("trigger");
			out.flush();
			
			response = in.readLine();
			Log.i(TAG, "Second response is : " + response);
			//send message to socket and then read response
			Log.i(TAG, "Sending message: " + message);
			out.println(message);
			out.flush();

			response = in.readLine();
			Log.i(TAG, "Third response is : " + response);

		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally{
			try{
				// close the socket and connection
				Log.i(TAG, "Closing the socket");
				s.close();
				return response;
			}catch(Exception e){
				Log.e(TAG, "Error closing the socket");
				e.printStackTrace();				
			}
		}
		
		return "unsuccesful";
	}


	/*
     * send Location
     * Checks whether identifiers have already been sent by
     * searching through idSent list. If not, 
     * Constructs a string of device identifiers
     * ans sends to service via socket
     */	
	private void sendIdentifiers(String appId){
		Log.i(TAG, "sendIdentifiers");
		if(idSent.contains(appId)){
			Log.i(TAG, "Identifiers have already been sent");
			return;
		}
		Log.i(TAG, "Sending device Identifiers");

		final String devIds = getAllDeviceIdentifiers(appId);

		Thread thread = new Thread()
		{
    	@Override
    	public void run() {
        	try {
				sendToSocket(devIds);            	
        		}catch (Exception e) {
            	e.printStackTrace();
        		}
    		}
		};
		thread.start();
		//add app to id sent list
		idSent.add(appId);
		return;
	}


	/*
     * send Location
     * Constructs a string using helper function and sends
     * location to PbdServer via socket
     */	
	private void sendLocation(String appId, PbdLocation loc){
		Log.i(TAG, "Sending Location. AppId: " + appId);
		int listenerNo = searchListenerList(appId);
		String provider = listenerList.get(listenerNo).getProvider();

		final String toSend = getLocationString(loc, provider);


		Thread thread = new Thread()
		{
    	@Override
    	public void run() {
        	try {
				sendToSocket(toSend);            	
        		}catch (Exception e) {
            	e.printStackTrace();
        		}
    		}
		};
		thread.start();
		//sendToSocket(toSend);
	}


	/*
     * getLocationString
     * Returns the last known location from the GPS location provider
     * and formats it as a string ready
     * to send to the pbd server. 
     */	
	private String getLocationString(PbdLocation loc, String provider){
		//get the last known location from the fine location provider
		String prov = null;

		if (provider.equals("finelocation")){
			prov = "Fine";
		}
		else if (provider.equals("coarselocation")){
			prov = "Coarse";
		}
		else {
			Log.i(TAG, "Incompatible provider when construction Location string");
		}
		
		//extract longitude and latitude from the returned location
		String lon = Double.toString(loc.getLongitude());
		String lat = Double.toString(loc.getLatitude());
		
		String dateTime = getDateTime();
		
		String rtn = "{DataType:Location_Update, Username:" + username  + ", Longitude:" 
		+ lon +", " + "Latitude:" + lat +", " + "Location_type:" 
		+ prov + ", " + "Location_time:" + dateTime + ", }";
		
		Log.i (TAG, "Location String constructed with Longitude: " + lon + " and Latitude: " + lat);
		Log.i(TAG, "Location String: " + rtn);
		return rtn;
	}


	/*
     * getTokens
     * Used to get or update http access tokens for an app
     * tokens are stored in the tokens Map
     * returns true of obtaining tokens is succesful
     * otherwise returns false
     */	
	private Boolean getTokens(String appId){
		Log.i(TAG, "into getTokens with appId " + appId);
		HttpRequest httpRequest = new HttpRequest();

		//Check if app id exists in map, update if so
		if (tokens.containsKey(appId)){
			//update tokens
			Log.i(TAG, "tokens map contains keys for " + appId);

			String[] toks = tokens.get(appId).clone();
			Log.i(TAG, "length of toks array is " + toks.length);

			Log.i(TAG, "toks[0] is :" + toks[0]);
			Log.i(TAG, "toks[1] is :" + toks[1]);
			Log.i(TAG, "toks[2] is :" + toks[2]);
			Log.i(TAG, "toks[3] is :" + toks[3]);
			Log.i(TAG, "toks[4] is :" + toks[4]);



			String refreshToken = toks[1];
			String clientId = toks[2];
			String clientSecret = toks[3];

			String [] newToks = httpRequest.getNewTokens(refreshToken, clientSecret, clientId).clone();

			//getNewTokens returns {"invalid", "invalid"} if error
			//return false if error in obtaining tokens
			if (newToks[0].equals("invalid")){
				Log.i(TAG, "getTokens: error obtaining tokens");
				return false;
			}

			//update the new access token and refresh token
			toks[0] = newToks[0];  //access token
			toks[1] = newToks[1];  //refresh token

			//toks = httpRequest.getNewTokens(refreshToken, clientSecret, clientId).clone();

			Log.i(TAG, "after getNewTokens, toks length is " + toks.length);

			Log.i(TAG, "new toks[0] is :" + toks[0]);
			Log.i(TAG, "new toks[1] is :" + toks[1]);
			Log.i(TAG, "new toks[2] is :" + toks[2]);
			Log.i(TAG, "new toks[3] is :" + toks[3]);
			Log.i(TAG, "new toks[4] is :" + toks[4]);


			//add new tokens to the tokens Map
			tokens.put(appId, toks);
		}
		else{
			//get current tokens
			Log.i(TAG, "going to call get current tokens");
			String[] toks = httpRequest.getCurrentTokens().clone();

			//return false if error in obtaining tokens
			if (toks.length == 0){
				Log.i(TAG, "getTokens: error obtaining tokens");
				return false;
			}
			tokens.put(appId, toks);
		}
		return true;
	}


	/*
     * authenticateIdentifier
     * Used by device identifier methods to check
     * whether request is legal
     * returns 0 if request granted
     * returns 1 if request refused
     * returns 2 if there is a error in request process
     */	
	private int authenticateIdentifier(String appId, String scope){
		Log.i(TAG, "authenticate Identifier with appId: " + appId + " scope: " + scope);		
		HttpRequest httpRequest = new HttpRequest();

		//get up to date OAuth2 tokens
		//return 2 if error in request
		if(getTokens(appId) == false){
			Log.i(TAG, "authenticateIdentifier: error in getTokens call");
			return 2;			
		}
		String[] toks = tokens.get(appId).clone();
		String accessToken = toks[0];
		String apiKey = toks[4];

		if (httpRequest.getDeviceFlag(apiKey, accessToken, scope)){
			Log.i(TAG, "Device flag for: " + scope + "GRANTED for package: " + appId);
			return 0;
		}
		else{
			Log.i(TAG, "Device flag for: " + scope + "REFUSED for package: " + appId);
			return 1;
		}
	}

	/*
     * authenticateLocationUpdates
     * Used by requestLocationUpdate method 
     * to authenticate request 
     * returns 0 if request granted
     * returns 1 if request refused
     * returns 2 if there is a error in request process
     */	
	private int authenticateLocationUpdates(String appId, String provider, long minTime, float minDistance ){
		Log.i(TAG, "Called authenticateLocationUpdates. appId: " + appId);
		HttpRequest httpRequest = new HttpRequest();

			//get up to date OAuth2 tokens
			//return 2 if error in request
		if(getTokens(appId) == false){
			Log.i(TAG, "authenticateLocationUpdates: error in getTokens call");
			return 2;
		}
		String[] toks = tokens.get(appId).clone();
		String accessToken = toks[0];

		if (httpRequest.getLocationFlag(accessToken, minTime, minDistance, provider)){
			Log.i(TAG, "Location Updates GRANTED for package: " + appId);
			return 0;
		}
		else{
			Log.i(TAG, "Location Updates REFUSED for package: " + appId);
			return 1;
		}

	}


	/*
     * authenticateLocationUpdates
     * Used by requestSingleUpdate method 
     * to authenticate request 
     * returns 0 if request granted
     * returns 1 if request refused
     * returns 2 if there is a error in request process
     */	
	private int authenticateSingleLocation(String appId, String provider){
		Log.i(TAG, "Called authenticateSingleLocation. appId: " + appId);
		HttpRequest httpRequest = new HttpRequest();

		//get up to date OAuth2 tokens
		//return 2 if error in request
		if(getTokens(appId) == false){
			Log.i(TAG, "authenticateSingleLocation: error in getTokens call");
			return 2;
		}
		String[] toks = tokens.get(appId).clone();
		String accessToken = toks[0];

		if (httpRequest.getSingleLocationFlag(accessToken, provider)){
			Log.i(TAG, "Single Location Update GRANTED for package: " + appId);
			return 0;
		}
		else{
			Log.i(TAG, "Single Location Update REFUSED for package: " + appId);
			return 1;
		}
	}


	/*
	* My Location Listener is uses to trigger methods when a location 
	* update is received. The OnLocationChanged sends a intent Broadcast
	* notifying an app of available location update
	*/
	public class MyLocationListener implements LocationListener {

		//The id of the calling app is stored as a string
		// and initialized by the MyLocationListener constuctor
		private String appId;
		private String listenerId;
		private String provider;
		private Boolean updateAvailable;


		public MyLocationListener(String _appId, String _listenerId){
			appId = _appId;
			listenerId = _listenerId;
			provider = null;
			updateAvailable = false;
		}

		/*
		* getAppId
		* Accessor function for the AppId
		*/
		public String getAppId(){
			return this.appId;
		}

		/*
		* getListenerId
		* Accessor function for the ListenerId
		*/
		public String getListenerId(){
			return this.listenerId;
		}

		/*
		* getProvider
		* Accessor function for the provider
		*/
		public String getProvider(){
			return this.provider;
		}


		/*
		* getProvider
		* Sets the provider to the argument passed
		* will either be set to "coarselocation" or "finelocation"
		*/
		public void setProvider(String _provider){
			provider = _provider;			
		}

		/*
		* isUpdateAvailable 
		* Returns the status of updateAvailable
		*/
		public Boolean isUpdateAvailable(){
			return updateAvailable;
		}

		/*
		* updateTaken 
		* used when an app takes the location update
		* sets updateAvailable to false;
		*/
		public void updateTaken(){
			updateAvailable = false;
		}

		/*
		* onLocationChanged
		* Function invoked when the MyLocationListener receives
		* a location update.
		* Broadcasts an intent with the MyLocationListeners unique
		* listenerId. The broadcast is intended to be received by
		* the app as a notification of a new location update.
		*/
		public void onLocationChanged(final Location loc)
		{
        //Broadcast a notification when the location has changed
			Log.i(TAG, "Broadcasting Intent with action: " + listenerId);
			//Set updateAvailable to true so location can be accessed.
			updateAvailable = true;
			Intent intent = new Intent();
			intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
			intent.setAction(listenerId);
			mContext.sendBroadcast(intent);
		}

		public void onProviderDisabled(String provider)
		{
	    	//Method Unused
		}


		public void onProviderEnabled(String provider)
		{
	    	//Method Unused	    }
		}


		public void onStatusChanged(String provider, int status, Bundle extras)
		{
	    	//Method Unused
		}
	}


		
	/*
	* HttpRequest
	* Nested Class for Oauth2 http requests
	* methods are called when validating
	* a sensitive data access method
	* All methods query the Pbd Web Server
	* and look for response in return JSON objects
	*/
	public class HttpRequest {
		InputStream is = null;
		JSONObject jObj = null;
		String json = "";

		public HttpRequest() {
		}
		
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		Map<String, String> mapn;
		DefaultHttpClient httpClient;
		HttpGet httpGet;

		
		/*
		 * getCurrentTokens
		 * Returns tokens as an array of string
		 * Index 0: Access Token
		 * Index 1: Refresh Token
		 * Index 2: Client Id
		 * Index 3: Client Secret
		 * Index 4: Api Key
		 */
		public String[]  getCurrentTokens(){
			Log.i(TAG, "into getCurrentTokens");

			String address = "http://" + ipAddress + ":8000/get_current_tokens";
			JSONObject j = getQuery(address); 
			String tokens[] = {"invalid", "invalid", "invalid", "invalid", "invalid"};
			try{
				tokens[0] = j.getString("access_token");
				tokens[1] = j.getString("refresh_token");
				tokens[2] = j.getString("client_id");
				tokens[3] = j.getString("client_secret");
				tokens[4] = j.getString("api_key");
				
			} catch (Exception e){
				Log.e("AuthTest", "FAILED: to get current tokens");
				e.printStackTrace();
				return null;		
			}
			return tokens;				
		}
		


		
		/*
		 * getNewTokens
		 * public method to get a new access and refresh tokens
		 * Returns an array of Strings
		 * Index 0: New Access Token
		 * Index 1: New Refresh Token
		 */
		public String[] getNewTokens(String refreshToken, String clientSecret, String clientId) {
			
			String address = "http://" + ipAddress + ":5000/oauth/token";
			
			String redirectUri = "http://" + ipAddress + ":8000/authorized";
			Log.i(TAG, "test the redirect uri is: " + redirectUri);

			List<NameValuePair> params = new ArrayList<NameValuePair>();
			params.add(new BasicNameValuePair("grant_type", "refresh_token"));
			params.add(new BasicNameValuePair("refresh_token", refreshToken));
			params.add(new BasicNameValuePair("client_secret", clientSecret));
			params.add(new BasicNameValuePair("redirect_uri", redirectUri));
			params.add(new BasicNameValuePair("client_id", clientId));

			String paramString = URLEncodedUtils.format(params, "utf-8");

			address += ("?" + paramString);

			Log.i(TAG, "getNewTokens. Address before sending to getquery is: " + address);

			JSONObject j = getQuery(address); 
			
			String tokens[] = {"invalid", "invalid"};
			
			try{
				tokens[0] = j.getString("access_token");
				tokens[1] = j.getString("refresh_token");
			} catch (Exception e){
				Log.e("AuthTest", "FAILED: to get new tokens");
				e.printStackTrace();			
			}
			return tokens;			
		}

		
		/*
		 * retrieveDeviceId
		 * A query to retrieve Location
		 */		
		public String retrieveLocation(String accessToken){

			String address = "http://" + ipAddress + ":5000/api/current_Location";
			
			List<NameValuePair> params = new ArrayList<NameValuePair>();
			params.add(new BasicNameValuePair("access_token", accessToken));
			params.add(new BasicNameValuePair("scope", "Location"));
			params.add(new BasicNameValuePair("username", username));

			String paramString = URLEncodedUtils.format(params, "utf-8");
			
			address += ("?" + paramString);
			
			//Log.i("AuthTest", "The address is " + address);
			JSONObject j = getQuery(address); 
			//Log.i("JSONStr", json);
			String token = "invalid";
			
			try{
				token = j.getString("Latitude");
			} catch (Exception e){
				Log.e("AuthTest", "FAILED: to get key value pair");
				e.printStackTrace();			
			}
			return token;		
		}
		

		
		public Boolean getDeviceFlag(String apiKey, String accessToken, String scope){
			String address = "http://" + ipAddress + ":5000/api/device";

			List<NameValuePair> params = new ArrayList<NameValuePair>();
			params.add(new BasicNameValuePair("access_token", accessToken));
			params.add(new BasicNameValuePair("scope", scope));
			params.add(new BasicNameValuePair("username", username));
			params.add(new BasicNameValuePair("api_key", apiKey)); 

			String paramString = URLEncodedUtils.format(params, "utf-8");
			
			address += ("?" + paramString);
			//Log.i("AuthTest", "The address is " + address);
			JSONObject j = getQuery(address);

			String token = "invalid";
			
			try{
				token = j.getString("Device_access");
			} catch (Exception e){
				Log.e("AuthTest", "FAILED: to get key value pair");
				e.printStackTrace();			
			}
			
			Log.i(TAG, "String value of returned is key pair is:" + token);
			
			//Check if return is permitted or denied
			if (token.equals("Permitted")){
				Log.i(TAG, "Returning True");
				return true;
			}
			Log.i(TAG, "Returning False");
			return false;
		}
		

		/*
		 * getDeviceAccessFlag
		 * Returns a Flag for whether access to device identifiers
		 * is permitted
		 */			
		public Boolean getDeviceAccessFlag(String apiKey, String accessToken, String scope){
			String address = "http://" + ipAddress + ":5000/api/device";
			String locProvider = null;

			List<NameValuePair> params = new ArrayList<NameValuePair>();
			params.add(new BasicNameValuePair("access_token", accessToken));
			params.add(new BasicNameValuePair("scope", scope));
			params.add(new BasicNameValuePair("username", username));
			params.add(new BasicNameValuePair("api_key", apiKey)); 

			String paramString = URLEncodedUtils.format(params, "utf-8");
			
			address += ("?" + paramString);
			//Log.i("AuthTest", "The address is " + address);
			JSONObject j = getQuery(address);

			String token = "invalid";
			
			try{
				token = j.getString("Device_access");
			} catch (Exception e){
				Log.e("AuthTest", "FAILED: to get key value pair");
				e.printStackTrace();			
			}
			
			Log.i(TAG, "String value of returned is key pair is:" + token);
			
			//Check if return is permitted or denied
			if (token.equals("Permitted")){
				Log.i(TAG, "Returning True");
				return true;
			}
			
			Log.i(TAG, "Returning False");
			return false;
		}
		
		

		/*
		 * getSingleLocationFlag
		 * Checks whether a request for a single location update is valid.
		 * 
		 */		
		public Boolean getSingleLocationFlag(String accessToken, String provider){

			String address = "http://" + ipAddress + ":5000/api/single_Location";
			String locProvider = null;
			
			if (provider.equals("finelocation"))
				locProvider = "Fine";
			else if (provider.equals("coarselocation"))
				locProvider = "Coarse";

			
			List<NameValuePair> params = new ArrayList<NameValuePair>();
			params.add(new BasicNameValuePair("access_token", accessToken));
			params.add(new BasicNameValuePair("scope", locProvider));
			params.add(new BasicNameValuePair("username", username));
			params.add(new BasicNameValuePair("type", "Single"));

			String paramString = URLEncodedUtils.format(params, "utf-8");
			
			address += ("?" + paramString);
			//Log.i("AuthTest", "The address is " + address);
			JSONObject j = getQuery(address);

			String token = "invalid";
			
			try{
				token = j.getString("Location_access");
			} catch (Exception e){
				Log.e("AuthTest", "FAILED: to get key value pair");
				e.printStackTrace();			
			}
			Log.i(TAG, "String value of returned is key pair is:" + token);
			//Check if return is permitted or denied
			if (token.equals("Permitted")){
				Log.i(TAG, "Returning True");
				return true;
			}
			
			Log.i(TAG, "Returning False");
			return false;
		}
		

		/*
		 * getLocationFlag
		 * A function to query server to check if location request
		 * is legal.
		 */			
		public Boolean getLocationFlag(String accessToken, long minTime, float minDistance, String provider){
			
			String address = "http://" + ipAddress + ":5000/api/current_Location";
			String locProvider = null;
			
			if (provider.equals("finelocation"))
				locProvider = "Fine";
			else if (provider.equals("coarselocation"))
				locProvider = "Coarse";
			
			//Convert minTime from milliseconds to seconds
			minTime = minTime / 1000;
			
			//Convert minTime and minDistance to Strings
			String mTime = 	String.valueOf(minTime);
			String mDistance = String.valueOf(minDistance);
			
			List<NameValuePair> params = new ArrayList<NameValuePair>();
			params.add(new BasicNameValuePair("access_token", accessToken));
			params.add(new BasicNameValuePair("scope", locProvider));
			params.add(new BasicNameValuePair("username", username));
			params.add(new BasicNameValuePair("duration", mTime)); 
			params.add(new BasicNameValuePair("distance", mDistance));

			String paramString = URLEncodedUtils.format(params, "utf-8");
			
			address += ("?" + paramString);
			JSONObject j = getQuery(address);

			String token = "invalid";
			
			try{
				token = j.getString("Location_access");
			} catch (Exception e){
				Log.e("AuthTest", "FAILED: to get key value pair");
				e.printStackTrace();			
			}
			
			Log.i(TAG, "String value of returned is key pair is:" + token);
			
			//Check if return is permitted or denied
			if (token.equals("Permitted")){
				Log.i(TAG, "Returning True");
				return true;
			}
			
			Log.i(TAG, "Returning False");
			return false;
		}


		/*
		 * retrieveDeviceId
		 * A query to retrieve DeviceId
		 */		
		public String retrieveDeviceId(String accessToken){

			String address = "http://" + ipAddress + ":5000/api/get_device";
			
			List<NameValuePair> params = new ArrayList<NameValuePair>();
			params.add(new BasicNameValuePair("access_token", accessToken));
			params.add(new BasicNameValuePair("scope", "Location"));
			params.add(new BasicNameValuePair("username", username));

			String paramString = URLEncodedUtils.format(params, "utf-8");
			
			address += ("?" + paramString);
			
			//Log.i("AuthTest", "The address is " + address);
			JSONObject j = getQuery(address); 
			//Log.i("JSONStr", json);
			String token = "invalid";
			
			try{
				token = j.getString("DeviceId");
			} catch (Exception e){
				Log.e("AuthTest", "FAILED: to get key value pair");
				e.printStackTrace();			
			}
			return token;		
		}

		
		/*
		 * getQuery
		 * A complete get query where the full address is provided
		 *
		 */
		public JSONObject getQuery(String address){

		//Set thread policy to strict mode to allow network use
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
		StrictMode.setThreadPolicy(policy);

		//check if network is available - for testing
		if (isNetworkAvailable())
			Log.i(TAG, "Network is available");
		else
			Log.i(TAG, "Network is not available");


			try{
				httpClient = new DefaultHttpClient();
				Log.i(TAG, "The address is " + address);
				httpGet = new HttpGet(address);
				httpGet.setHeader("Content-Type", "application/x-www-form-urlencoded");

				HttpResponse httpResponse = httpClient.execute(httpGet);
				HttpEntity httpEntity = httpResponse.getEntity();
				is = httpEntity.getContent();
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				BufferedReader reader = new BufferedReader(new InputStreamReader(
					is, "iso-8859-1"), 8);
				StringBuilder sb = new StringBuilder();
				String line = null;
				while ((line = reader.readLine()) != null) {
					sb.append(line + " ");
				}
				is.close();
				json = sb.toString();
				Log.i("JSONStr", json);
			} catch (Exception e) {
				e.getMessage();
				Log.e("Buffer Error", "Error converting result " + e.toString());
			}
			// Parse the String to a JSON Object
			try {
				jObj = new JSONObject(json);
			} catch (JSONException e) {
				Log.e("JSON Parser", "Error parsing data " + e.toString());
			}
			// Return JSON Object
			return jObj;
		}

	}

}