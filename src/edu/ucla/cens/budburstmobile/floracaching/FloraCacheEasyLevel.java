package edu.ucla.cens.budburstmobile.floracaching;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;


import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;

import edu.ucla.cens.budburstmobile.R;
import edu.ucla.cens.budburstmobile.adapter.MyListAdapterMainPage;
import edu.ucla.cens.budburstmobile.database.OneTimeDBHelper;
import edu.ucla.cens.budburstmobile.helper.HelperGpsHandler;
import edu.ucla.cens.budburstmobile.helper.HelperListItem;
import edu.ucla.cens.budburstmobile.helper.HelperPlantItem;
import edu.ucla.cens.budburstmobile.helper.HelperSharedPreference;
import edu.ucla.cens.budburstmobile.helper.HelperValues;
import edu.ucla.cens.budburstmobile.lists.ListMain;
import edu.ucla.cens.budburstmobile.mapview.MapViewMain;
import edu.ucla.cens.budburstmobile.mapview.MyLocOverlay;
import edu.ucla.cens.budburstmobile.mapview.SpeciesMapOverlay;
import edu.ucla.cens.budburstmobile.myplants.PBBPlantList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class FloraCacheEasyLevel extends MapActivity {

	private boolean mIsBound;
	
	private HelperGpsHandler gpsHandler;
	private boolean mFirstGps;	
	
	// Map related variables
	private LocationManager mLocManager = null;
	private MapView mMapView = null;
	private MyLocOverlay mMyOverLay = null;
	private MapController mMapController = null;
	
	private double mLatitude 		= 0.0;
	private double mLongitude 		= 0.0;
	
	private ArrayList<FloracacheItem> mPlantList;
	private HelperPlantItem pItem;
	private Drawable mMarker;
	private int mGroupID;
	
	private ServiceConnection mConnection = new ServiceConnection() {

		public void onServiceConnected(ComponentName className, IBinder binder) {
			gpsHandler = ((HelperGpsHandler.GpsBinder) binder).getService();
			//Toast.makeText(PBBMapMain.this, "Connected", Toast.LENGTH_SHORT).show();
		}

		public void onServiceDisconnected(ComponentName className) {
			gpsHandler = null;
		}
	};
	
	private void doBindService() {
		
		Log.i("K", "BindService");
		
		bindService(new Intent(FloraCacheEasyLevel.this, HelperGpsHandler.class), mConnection,
				Context.BIND_AUTO_CREATE);
		mIsBound = true;
		//Toast.makeText(PBBMapMain.this, "bindService", Toast.LENGTH_SHORT).show();
	
	}
	
	private void doUnbindService() {
		
		Log.i("K", "UnBindService");
		
		if(mIsBound) {
			if(mConnection != null) {
				
			}
			
			//Toast.makeText(PBBMapMain.this, "UnbindService", Toast.LENGTH_SHORT).show();
			unbindService(mConnection);
			mIsBound = false;
		}
	}
	
	@Override
	public void onDestroy() {
		// when user finish this activity, turn off the gps
		// if there's a overlay, should call disableCompass() explicitly
		doUnbindService();
		if(gpsReceiver != null) {
			unregisterReceiver(gpsReceiver);
		}
		
		mMyOverLay.disableCompass();
		mMyOverLay.disableMyLocation();
	
		super.onDestroy();
	}
	
	private BroadcastReceiver gpsReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			Bundle extras = intent.getExtras();
			
			if(extras.getBoolean("signal")) {
				mLatitude = extras.getDouble("latitude");
				mLongitude = extras.getDouble("longitude");
				
				// convert points into GeoPoint
			    GeoPoint gPoint = getPoint(mLatitude, mLongitude);

			    // center the map
			    if(mFirstGps) {
			    	mMapController.setCenter(gPoint);
			    	mFirstGps = false;
			    }
			}
			// if Gps signal is bad
			else {
				//Toast.makeText(FloraCacheEasyLevel.this, getString(R.string.Low_GPS_Signal), Toast.LENGTH_SHORT).show();
			}
		}	
	};
	

	private GeoPoint getPoint(double lat, double lon) {
		return(new GeoPoint((int)(lat*1000000.0), (int)(lon*1000000.0)));
	}

	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	
	    setContentView(R.layout.floracache_map);
	    
	    Intent gIntent = getIntent();
	    mGroupID = gIntent.getExtras().getInt("group_id");
		
		mLocManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		// Set MapView
		mMapView = (MapView)findViewById(R.id.map);
		mMapView.setBuiltInZoomControls(true);
		mMapView.setSatellite(false);
		// Set mapController
		mMapController = mMapView.getController();
		mMapController.setZoom(12);
		
		// Add mylocation overlay
		mMyOverLay = new MyLocOverlay(FloraCacheEasyLevel.this, mMapView);
		mMyOverLay.enableMyLocation();
		mMyOverLay.enableCompass();
	    
		mFirstGps = true;
		
		// remove view of accuracy bar
		TextView titleBar = (TextView)findViewById(R.id.myloc_accuracy);
		titleBar.setVisibility(View.GONE);

		// initialize plantList
		mPlantList = new ArrayList<FloracacheItem>();
		
		// initialize marker
		mMarker = getResources().getDrawable(R.drawable.marker);
		mMarker.setBounds(0, 0, mMarker.getIntrinsicWidth(), mMarker.getIntrinsicHeight());
		
		IntentFilter inFilter = new IntentFilter(HelperGpsHandler.GPSHANDLERFILTER);
		registerReceiver(gpsReceiver, inFilter);
		Log.i("K", "Receiver Register");
		
		checkGpsIsOn();
		
		
	    // TODO Auto-generated method stub
	}
	
	public void checkGpsIsOn() {
		// check if GPS is turned on...
		if (mLocManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
			
			Location lastLoc = mLocManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
			if(lastLoc != null) {
				mLatitude = lastLoc.getLatitude();
				mLongitude = lastLoc.getLongitude();
				mMapController.setCenter(getPoint(mLatitude, mLongitude));
			}
			
			// bind GPS service to current activity
			doBindService();
			// call plantlists from the database
			showSpeciesOnMap(false);
			
		}
		else {
		   	
		 new AlertDialog.Builder(FloraCacheEasyLevel.this)
		   		.setTitle(getString(R.string.Turn_on_GPS))
		   		.setMessage(getString(R.string.Message_locationDisabledTurnOn))
		   		.setPositiveButton(getString(R.string.Button_yes), new DialogInterface.OnClickListener() {
		   			public void onClick(DialogInterface dialog, int whichButton) {
		   				Intent intent = new Intent(Settings.ACTION_SECURITY_SETTINGS);
		   				startActivityForResult(intent, 1);
		   			}
		   		})
		   		.setNegativeButton(getString(R.string.Button_no), new DialogInterface.OnClickListener() {
		   			public void onClick(DialogInterface dialog, int whichButton) {
		   				finish();
		   			}
		   		})
		   		.show();
		}	
	}
	
	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO Auto-generated method stub
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 1) {
        	if(resultCode == RESULT_OK) {
        		Log.i("K", "onActivityResult");
            	doBindService();
            	showSpeciesOnMap(false);
        	}
        }
    }

	public void showSpeciesOnMap(boolean hasHandler) {
		
		// TODO Auto-generated method stub
		//otDBH = new OneTimeDBHelper(PBBMapMain.this);
		
		GeoPoint gPoint = new GeoPoint((int)(mLatitude * 1000000), (int)(mLongitude * 1000000));
		
		mMapView.setBuiltInZoomControls(true);
		mMapView.invalidate();
		
		getMyListFromServer();

	}
	
	private void getMyListFromServer() {
		HelperSharedPreference hPref = new HelperSharedPreference(this);
		if(!hPref.getPreferenceBoolean("floracache")) {
			Toast.makeText(FloraCacheEasyLevel.this, "To download the list, Go 'Settings' page", Toast.LENGTH_SHORT).show();
		}
		else {
			OneTimeDBHelper oDBH = new OneTimeDBHelper(this);
			mPlantList = oDBH.getFloracacheLists(FloraCacheEasyLevel.this, HelperValues.FLORACACHE_EASY, mGroupID, mLatitude, mLongitude);
		
			mMapView.getOverlays().add(new FloraCacheOverlay(mMapView, mMarker, mPlantList));
			mMapView.getOverlays().add(mMyOverLay);
			
			GeoPoint gPoint = new GeoPoint((int)(mLatitude * 1000000), (int)(mLongitude * 1000000));
			
			mMapController.setCenter(gPoint);
			mMapController.setZoom(18);
		}
	}
	
	private void refreshList() {
		OneTimeDBHelper oDBH = new OneTimeDBHelper(this);
		mPlantList = oDBH.getFloracacheLists(FloraCacheEasyLevel.this, HelperValues.FLORACACHE_EASY, mGroupID, mLatitude, mLongitude);
	
		mMapView.getOverlays().clear();
		mMapView.invalidate();
		
		mMapView.getOverlays().add(new FloraCacheOverlay(mMapView, mMarker, mPlantList));
		mMapView.getOverlays().add(mMyOverLay);
		
		GeoPoint gPoint = new GeoPoint((int)(mLatitude * 1000000), (int)(mLongitude * 1000000));
		
		mMapController.setCenter(gPoint);
	}
	

	@Override
	protected boolean isRouteDisplayed() {
		// TODO Auto-generated method stub
		return false;
	}
	
	public boolean onCreateOptionsMenu(Menu menu){
		super.onCreateOptionsMenu(menu);
		
		menu.add(0, 1, 0, getString(R.string.PBBMapMenu_myLocation)).setIcon(android.R.drawable.ic_menu_mylocation);
		menu.add(0, 2, 0, getString(R.string.PBBMapMenu_changeView)).setIcon(android.R.drawable.ic_menu_mapmode);
		menu.add(0, 3, 0, getString(R.string.PBBMapMenu_refresh)).setIcon(android.R.drawable.ic_menu_rotate);
			
		return true;
	}
	
	//Menu option selection handling
	public boolean onOptionsItemSelected(MenuItem item){
		switch(item.getItemId()){
			case 1:
				GeoPoint current_point = null;
				if(mLatitude == 0.0) {
					Toast.makeText(FloraCacheEasyLevel.this, getString(R.string.Alert_gettingGPS), Toast.LENGTH_SHORT).show();
				}
				else {
					current_point = new GeoPoint((int)(mLatitude * 1000000), (int)(mLongitude * 1000000));

					mMapController = mMapView.getController();
					mMapController.animateTo(current_point);
				}
				return true;
			case 2:
				mMapView.setSatellite(!mMapView.isSatellite());
				return true;
			case 3:
				refreshList();
				return true;
		}
		return false;
	}


}
