
/*TestService.java */ 

package com.android.server;

//Import Libraries
import android.content.Context;
import android.os.Handler;
import android.os.ITestService;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Log;

//Import for device ID
import android.provider.Settings.Secure;

//imports for web
/*
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
*/

//imports for gcm
//import com.google.android.gcm.GCMRegistrar;


public class TestService extends ITestService.Stub { 

	private static final String TAG = "TestService";
   private TestWorkerThread mWorker;
   private TestWorkerHandler mHandler;
   private Context mContext;
   private String android_id;


   public TestService(Context context) {
       super();
       mContext = context;
       mWorker = new TestWorkerThread("TestServiceWorker");
       mWorker.start();
       Log.i(TAG, "Spawned worker thread");

   }

   public void setValue(int val) {
       Log.i(TAG, "setValue " + val);
       Message msg = Message.obtain();
       msg.what = TestWorkerHandler.MESSAGE_SET;
       msg.arg1 = val;
       mHandler.sendMessage(msg);
   }


   public String getDeviceId(){
    android_id = Secure.getString(mContext.getContentResolver(),
                                                    Secure.ANDROID_ID); 
    return android_id;

   }

   public String getString(){
      return "Alexis Sanchez";    
   }

   private class TestWorkerThread extends Thread {
       public TestWorkerThread(String name) {
           super(name);
       }
       public void run() {
           Looper.prepare();
           mHandler = new TestWorkerHandler();
           Looper.loop();
       }
   }

   private class TestWorkerHandler extends Handler {
       private static final int MESSAGE_SET = 0;
       @Override
       public void handleMessage(Message msg) {
           try {
               if (msg.what == MESSAGE_SET) {
                   Log.i(TAG, "set message received: " + msg.arg1);
               }
           } catch (Exception e) {
               // Log, don't crash!
               Log.e(TAG, "Exception in TestWorkerHandler.handleMessage:", e);
           }
       }
   }

}