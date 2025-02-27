package edu.ucla.cens.budburstmobile.myplants;


import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;

import edu.ucla.cens.budburstmobile.R;
import edu.ucla.cens.budburstmobile.database.OneTimeDBHelper;
import edu.ucla.cens.budburstmobile.database.SyncDBHelper;
import edu.ucla.cens.budburstmobile.helper.HelperSharedPreference;
import edu.ucla.cens.budburstmobile.helper.HelperValues;
import edu.ucla.cens.budburstmobile.mapview.SitesOverlay;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class PBBChangeMyPosition extends MapActivity {

	private HelperSharedPreference mPref;
	private static GpsListener gpsListener;
	private LocationManager locManager = null;
	private MapView mMapView = null;
	private MapController mapCon = null;
	//private MyLocOverlay mOver = null;
	private MyLocationOverlay mOver;
	private SitesOverlay sOverlay = null;
	private double mLatitude = 0.0;
	private double mLongitude = 0.0;
	private float mAccuracy = 0;
	private TextView mylocInfo;
	private boolean first_myLoc = true;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.floracache_map);
	    
	    mMapView = (MapView)findViewById(R.id.map);
	    
	    mylocInfo = (TextView) findViewById(R.id.myloc_accuracy);
	    
	    /*
	     * Add Mylocation Overlay
	     */
	    //mOver = new MyLocOverlay(MyLocation.this, mMapView);
	    mOver = new MyLocationOverlay(PBBChangeMyPosition.this, mMapView);
	    mOver.enableMyLocation();
	    mMapView.getOverlays().add(mOver);
	    mMapView.setSatellite(false);
	    /*
	     * Add ItemizedOverlay Overlay
	     */
	    Drawable marker = getResources().getDrawable(R.drawable.marker);
	    marker.setBounds(0, 0, marker.getIntrinsicWidth(), marker.getIntrinsicHeight());
	    sOverlay = new SitesOverlay(PBBChangeMyPosition.this, marker);	    
	    mMapView.getOverlays().add(sOverlay);
	    
	    
	    //mMapView.invalidate();
	    mPref = new HelperSharedPreference(this);
	    mLatitude = Double.parseDouble(mPref.getPreferenceString("latitude", "0.0"));
	    mLongitude = Double.parseDouble(mPref.getPreferenceString("longitude", "0.0"));
	    
	    mapCon = mMapView.getController();
	    GeoPoint geoPoint = getPoint(mLatitude, mLongitude);
	    mapCon.animateTo(geoPoint);
	    mapCon.setZoom(19);
	   
	    gpsListener = new GpsListener();
	    locManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
	    locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5 * 1000, 5, gpsListener);
	    
	    showButtonOnMap();
		
	     
	    // TODO Auto-generated method stub
	}
	
	private void showButtonOnMap() {
		
		Display display = getWindowManager().getDefaultDisplay();
		int width = display.getWidth();
		int height = display.getHeight();
		
		MapView.LayoutParams screenLP;

		// Zoom out
	    screenLP = new MapView.LayoutParams(MapView.LayoutParams.WRAP_CONTENT,
											MapView.LayoutParams.WRAP_CONTENT,
											width-50, 10,
											MapView.LayoutParams.TOP_LEFT);

	    Button mapBtnZoomOut = new Button(getApplicationContext());
	    mapBtnZoomOut.setBackgroundDrawable(getResources().getDrawable(R.drawable.menu_zoom_in));

	    mMapView.addView(mapBtnZoomOut, screenLP);
		
	    screenLP = new MapView.LayoutParams(MapView.LayoutParams.WRAP_CONTENT,
	    									MapView.LayoutParams.WRAP_CONTENT,
	    									width-50, 55,
	    									MapView.LayoutParams.TOP_LEFT);
	    // Zoom in
	    Button mapBtnZoomIn = new Button(getApplicationContext());
	    mapBtnZoomIn.setBackgroundDrawable(getResources().getDrawable(R.drawable.menu_zoom_out));

	    mMapView.addView(mapBtnZoomIn, screenLP);
	    
	    mapBtnZoomIn.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				mapCon.zoomIn();
			}
		});

	    mapBtnZoomOut.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				mapCon.zoomOut();
			}
		});
	}
	
	private class GpsListener implements LocationListener {
		
		@Override
		public void onLocationChanged(Location loc) {
			if(loc != null) {
				mLatitude = loc.getLatitude();
				mLongitude = loc.getLongitude();
				mAccuracy = loc.getAccuracy();
				
				GeoPoint geoPoint = getPoint(mLatitude, mLongitude);
				
				mapCon.animateTo(geoPoint);
				
				mylocInfo.setText("Accuracy : " + mAccuracy + "\u00b1m");
				
				mOver.onLocationChanged(loc);
				
			}
			// TODO Auto-generated method stub
		}
		@Override
		public void onProviderDisabled(String arg0) {
			// TODO Auto-generated method stub
		}
		@Override
		public void onProviderEnabled(String arg0) {
			// TODO Auto-generated method stub
		}
		@Override
		public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
			// TODO Auto-generated method stub
		}
	}

	@Override
	protected boolean isRouteDisplayed() {
		// TODO Auto-generated method stub
		return false;
	}
	
	private GeoPoint getPoint(double lat, double lon) {
		return(new GeoPoint((int)(lat*1000000.0), (int)(lon*1000000.0)));
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		locManager.removeUpdates(gpsListener);
		mOver.disableMyLocation();
	}
	
	// or when user press back button
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if(keyCode == KeyEvent.KEYCODE_BACK) {
			
			new AlertDialog.Builder(PBBChangeMyPosition.this)
	   		.setTitle(getString(R.string.Message_Save_GPS))
	   		.setPositiveButton(getString(R.string.Button_GPS), new DialogInterface.OnClickListener() {
	   			public void onClick(DialogInterface dialog, int whichButton) {
	   				
	   				mPref.setPreferencesString("latitude", Double.toString(mLatitude));
	   				mPref.setPreferencesString("longitude", Double.toString(mLongitude));
	   				mPref.setPreferencesString("accuracy", Float.toHexString(mAccuracy));
	   				
	   				finish();
	   			}
	   		})
	   		.setNegativeButton(getString(R.string.Button_Cancel), new DialogInterface.OnClickListener() {
	   			public void onClick(DialogInterface dialog, int whichButton) {
	   				locManager.removeUpdates(gpsListener);
	   				mOver.disableMyLocation();		
	   				finish();
	   			}
	   		})
	   		.setNeutralButton(getString(R.string.Button_Marker), new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub
					mPref.setPreferencesString("latitude", Double.toString(mLatitude));
	   				mPref.setPreferencesString("longitude", Double.toString(mLongitude));
	   				mPref.setPreferencesString("accuracy", Float.toHexString(mAccuracy));
	   				
	   				finish();
				}
			})
	   		.show();			
		}
		return false;
	}
	
	
		/*
	 * Menu option(non-Javadoc)
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	public boolean onCreateOptionsMenu(Menu menu){
		super.onCreateOptionsMenu(menu);
		
		menu.add(0, 1, 0, getString(R.string.Menu_help)).setIcon(android.R.drawable.ic_menu_help);
		menu.add(0, 2, 0, getString(R.string.Menu_Satellite)).setIcon(android.R.drawable.ic_menu_mapmode);
			
		return true;
	}
	
	/*
	 * Menu option selection handling(non-Javadoc)
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	public boolean onOptionsItemSelected(MenuItem item){

		switch(item.getItemId()){
			case 1:
				Toast.makeText(PBBChangeMyPosition.this, getString(R.string.Alert_comingSoon), Toast.LENGTH_SHORT).show();
				return true;
			case 2:
				mMapView.setSatellite(!mMapView.isSatellite());
				return true;
		}
		return false;
	}

}
