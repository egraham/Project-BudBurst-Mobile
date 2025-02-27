package edu.ucla.cens.budburstmobile.myplants;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import edu.ucla.cens.budburstmobile.PBBHelpPage;
import edu.ucla.cens.budburstmobile.PBBMainPage;
import edu.ucla.cens.budburstmobile.PBBSync;
import edu.ucla.cens.budburstmobile.R;
import edu.ucla.cens.budburstmobile.database.OneTimeDBHelper;
import edu.ucla.cens.budburstmobile.database.StaticDBHelper;
import edu.ucla.cens.budburstmobile.database.SyncDBHelper;
import edu.ucla.cens.budburstmobile.helper.HelperFunctionCalls;
import edu.ucla.cens.budburstmobile.helper.HelperPlantItem;
import edu.ucla.cens.budburstmobile.helper.HelperSharedPreference;
import edu.ucla.cens.budburstmobile.helper.HelperValues;
import edu.ucla.cens.budburstmobile.lists.ListDetail;
import edu.ucla.cens.budburstmobile.onetime.OneTimeMainPage;
import edu.ucla.cens.budburstmobile.onetime.OneTimePBBLists;
import edu.ucla.cens.budburstmobile.onetime.OneTimePhenophase;
import edu.ucla.cens.budburstmobile.utils.PBBItems;
import edu.ucla.cens.budburstmobile.utils.QuickCapture;

public class PBBPlantList extends ListActivity {
	
	private int mPosition = 0;
	private int mDialogSpeciesID = 0;
	private int mDialogSiteID = 0;
	private int mOnetimeStartPoint = 0;
	
	private int mSpeciesID;
	private int mProtocolID;
	private int mCategory;
	private String mCommonName;
		
	private HelperSharedPreference mPref;
	private SyncDBHelper syncDBHelper;
	private StaticDBHelper staticDBHelper;
	
	private ListView MyList;
	private Dialog mDialog = null;
	private EditText et1 = null;
	private LinearLayout lout = null;
	private LinearLayout summaryFooter = null;	

	private CharSequence[] mSeqUserSite;
	private HelperFunctionCalls mHelper;
	private HashMap<String, Integer> mapUserSiteNameID = new HashMap<String, Integer>();	
	private ArrayList<HelperPlantItem> mArrPlantItem;
	
	//MENU contents
	final private int MENU_ADD_PLANT = 1;
	final private int MENU_ADD_QC_PLANT = 2;
	final private int MENU_SYNC = 6;
	final private int MENU_HELP = 7;

	private PBBItems pbbItem;
	

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.plantlist);
		
		mHelper = new HelperFunctionCalls();
		
		MyList = getListView();
		
		MyList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
		    @Override
		    public void onItemClick(AdapterView<?> av, View v, int pos, long id) {
		        onListItemClick(v,pos,id);
		    }
		});

		
		MyList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
		    @Override
		    public boolean onItemLongClick(AdapterView<?> av, View v, int pos, long id) {
		        return onLongListItemClick(v,pos,id);
		    }
		});
		
		//Retrieve username and password
		mPref = new HelperSharedPreference(this);
		String username = mPref.getPreferenceString("Username","");
		String password = mPref.getPreferenceString("Password","");
		
		
		mArrPlantItem = new ArrayList<HelperPlantItem>();
		syncDBHelper = new SyncDBHelper(PBBPlantList.this);
		SQLiteDatabase syncDB  = syncDBHelper.getReadableDatabase();
		OneTimeDBHelper onetime = new OneTimeDBHelper(PBBPlantList.this);
		SQLiteDatabase ot  = onetime.getReadableDatabase();
		
		staticDBHelper = new StaticDBHelper(PBBPlantList.this);
		SQLiteDatabase staticDB = staticDBHelper.getReadableDatabase();
		
		//Check user plant is empty.			
		Cursor numPlants = syncDB.rawQuery("SELECT site_id FROM my_plants", null);
		Cursor numObservations = ot.rawQuery("SELECT _id FROM oneTimePlant", null);
		
		if(numPlants.getCount() == 0 && numObservations.getCount() == 0)
		{	
			numPlants.close();
			numObservations.close();
			
			TextView instruction = (TextView)findViewById(R.id.instruction);
			instruction.setVisibility(View.VISIBLE);
			MyList.setVisibility(View.GONE); 
			return;
		}else{
			numPlants.close();
			numObservations.close();
		}
		
		numPlants.close();
		numObservations.close();
		
		/*
		 * Show the current data in the database...(debug purpose)
		 * 
		 */
		Cursor ccc = staticDB.rawQuery("SELECT _id, Phenophase_ID, Protocol_ID FROM Phenophase_Protocol_Icon", null);
		while(ccc.moveToNext()) {
			Log.i("K", "id : " + ccc.getInt(0) + " Phenophase_ID : " + ccc.getInt(1) + " Protocol_ID : " + ccc.getInt(2));
		}

		ccc.close();
		
		Cursor cursorss = syncDB.rawQuery("SELECT _id, species_id, site_id, phenophase_id, image_id, synced FROM my_observation;", null);
		while(cursorss.moveToNext()) {
			Log.i("K", "MY OBSERVATION => id : " + cursorss.getInt(0) + " ,species_id: " + cursorss.getInt(1) + " ,site_id: " + cursorss.getInt(2) + ", phenophase_id: " + cursorss.getInt(3) + ", image_id : " + cursorss.getString(4) + " , synced : " + cursorss.getInt(5));
		}
		cursorss.close();
		
		cursorss = syncDB.rawQuery("SELECT species_id, common_name, active, synced, category, site_id, protocol_id FROM my_plants;", null);
		while(cursorss.moveToNext()) {
			Log.i("K", "MY PLANTS : " + cursorss.getInt(0) + " , " + cursorss.getString(1) + " , active : " + cursorss.getInt(2)+ " , synced : " + cursorss.getInt(3) + ", category : " + cursorss.getInt(4) + ", site_id:" + cursorss.getInt(5) + ", protocol_id: " + cursorss.getInt(6));
		}
		cursorss.close();
		
		cursorss = syncDB.rawQuery("SELECT site_id, site_name, official, synced, latitude, longitude FROM my_sites;", null);
		while(cursorss.moveToNext()) {
			Log.i("K", "MY SITES : " + cursorss.getInt(0) + " ,name : " + cursorss.getString(1) + " , official : " + cursorss.getInt(2)+ " , synced : " + cursorss.getInt(3) + ", lat:" + cursorss.getString(4) + ",lng:" + cursorss.getString(5));
		}
		cursorss.close();

		cursorss = ot.rawQuery("SELECT _id, plant_id, species_id, site_id, cname, sname, active, synced, category, is_floracache, floracache_id FROM oneTimePlant;", null);
		while(cursorss.moveToNext()) {
			Log.i("K", "ONETIME OB : " + cursorss.getInt(0) + ", plant_id : " + cursorss.getInt(1) + ", species_id " + cursorss.getInt(2) + " , site_id : " + cursorss.getString(3) + " , cname : " + cursorss.getString(4) + " , sname : " + cursorss.getString(5) + ", Active " + cursorss.getInt(6) + " , SYNCED : " + cursorss.getInt(7) + ", CATEGORY : " + cursorss.getInt(8) + ",is_floracache : " + cursorss.getInt(9) + ",floracacheID : " + cursorss.getInt(10));
		}
		cursorss.close();
		
		cursorss = ot.rawQuery("SELECT plant_id, phenophase_id, lat, lng, image_id, dt_taken, notes, synced FROM oneTimeObservation;", null);
		while(cursorss.moveToNext()) {
			Log.i("K", "ONETIME OBSERVATION - plant_ID : " + cursorss.getInt(0) + " , Phenophase_id : " + cursorss.getInt(1) + " , lat : " + cursorss.getDouble(2) + " , lng : " + cursorss.getDouble(3) + ", image_id : " + cursorss.getString(4) + " , date_taken : " + cursorss.getString(5) + " , notes : " + cursorss.getString(6) + " ,synced : " + cursorss.getInt(7));
		}
		cursorss.close();
		
		ot.close();
		onetime.close();
		
		//////////////////////////////////////////////////////////////////////////////////////////////////
		
		summaryFooter = (LinearLayout) findViewById(R.id.footer_item);
		TextView summaryTxt = (TextView) findViewById(R.id.my_summary);
		
		SyncDBHelper dbh = new SyncDBHelper(PBBPlantList.this);
		OneTimeDBHelper otdbh = new OneTimeDBHelper(PBBPlantList.this);
		
		
		Log.i("K", "1 : " + dbh.getTotalNumberOfPlants(this) + " 2 : " + otdbh.getTotalNumberOfQCOPlants(this) + " 3: "+
				dbh.getTotalNumberOfObservations(this) + " 4 : " + 
				otdbh.getTotalNumberOfQCObservations(this));
		
		final int totalNumberOfPlants = dbh.getTotalNumberOfPlants(this);
		final int totalNumberOfQCOPlants = otdbh.getTotalNumberOfQCOPlants(this);
		
		final int totalNumberOfObservations = dbh.getTotalNumberOfObservations(this);
		final int totalNumberOfQCObservations = otdbh.getTotalNumberOfQCObservations(this);
		
		int totalNumberP = totalNumberOfPlants + totalNumberOfQCOPlants;
		int totalNumberO = totalNumberOfObservations + totalNumberOfQCObservations;
		
		summaryTxt.setText("Plants: " + totalNumberP
				+ "  Observations: " + totalNumberO);
		
		
		
		summaryFooter.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				
				final LinearLayout linear = (LinearLayout) View.inflate(PBBPlantList.this, R.layout.summarypage, null);
				
				TextView numMonitoredPlant = (TextView) linear.findViewById(R.id.num_monitored_plants);
				TextView numMonitoredObservation = (TextView) linear.findViewById(R.id.num_monitored_observations);
				TextView numMonitoredQSPlant = (TextView) linear.findViewById(R.id.num_qc_plants);
				TextView numMonitoredQSObservation = (TextView) linear.findViewById(R.id.num_qc_observations);
				
				numMonitoredPlant.setText("- Plant: " + totalNumberOfPlants);
				numMonitoredObservation.setText("- Observation: " + totalNumberOfObservations);
				numMonitoredQSPlant.setText("- Plant: " + totalNumberOfQCOPlants);
				numMonitoredQSObservation.setText("- Observation: " + totalNumberOfQCObservations);
				
				
				new AlertDialog.Builder(PBBPlantList.this)
				.setTitle(getString(R.string.My_Plant_Summary))
				.setView(linear)
				.setPositiveButton(getString(R.string.Button_save), new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
					}
				})
				.show();

			}
		});
		
		/**
		 * Getting my plant information from the database
		 */
		ArrayList<String> user_station_name = new ArrayList<String>();
		ArrayList<Integer> user_station_id = new ArrayList<Integer>();
		Cursor cursor;

		try{
			//Retreive site name and site id from my_plant table to draw plant list.
			cursor = syncDB.rawQuery("SELECT site_name, site_id FROM my_plants GROUP BY site_id;",null);
			
			while(cursor.moveToNext()){
				Log.i("K", "ID : " + cursor.getInt(1) + ", Name:" + cursor.getString(0));
				user_station_name.add(cursor.getString(0));
				user_station_id.add(cursor.getInt(1));
			}
			cursor.close();
			
			mArrPlantItem.clear();
			boolean header = false;
			int count = 0;
			for(int i = 0; i < user_station_id.size(); i++){
				
				HelperPlantItem pi;
				
				//Retrieves plants from each site.
				//Cursor cursor2 = syncDB.rawQuery("SELECT species_id, common_name, active, protocol_id FROM my_plants " +
				//		"WHERE site_name = '" + user_station_name.get(i) + "';", null);
				
				Cursor cursor2 = syncDB.rawQuery("SELECT species_id, common_name, active, protocol_id, synced, category FROM my_plants " +
						"WHERE site_id =" + user_station_id.get(i) + ";", null);
				
				while(cursor2.moveToNext()){
					// if active flag is 0, skip the operation below...
					// active = 0 means, the corresponding species got deleted
					if(cursor2.getInt(2) == 0) {
						Log.i("K", "MOVE TO THE TOP");
						continue;
					}
					
					String commonName = cursor2.getString(1);
					int category = cursor2.getInt(5);
					
					count++;
					// check if the species_id and common_name are from "UNKNOWN"
					int species_id = 0;
					if(cursor2.getInt(0) > 76) {
						species_id = 999;
					}
					else {
						species_id = cursor2.getInt(0);
					}
						
					// if common_name from the server is "null", change it to "Unknown Plant"
					String common_name = "";
					if(cursor2.getString(1).equals("null")) {
						common_name = "Unknown Plant";
					}
					else {
						common_name = cursor2.getString(1);
					}
						
					String qry = "SELECT _id, species_name, common_name, protocol_id FROM species WHERE _id = " + species_id + ";";
						
					Cursor cursor3 = staticDB.rawQuery(qry, null);
						
					cursor3.moveToNext();
					int resID = 0;
					int speciesID = cursor3.getInt(0);
					int imageID = 0;
					String scienceName = "";
					
					// if the species is from local budburst list,
					// use the image saved in the drawable
					if(category == HelperValues.LOCAL_BUDBURST_LIST) {
						resID = getResources().getIdentifier("edu.ucla.cens.budburstmobile:drawable/s" + speciesID, null, null);
						
						scienceName = cursor3.getString(1);
						if(scienceName.equals("Unknown Plant")) {
							scienceName = "";
						}
					}
					// if the species is from other local lists
					// find the species image based on the imageID
					// Local folder in the sd card
					else if(category == HelperValues.LOCAL_WHATSINVASIVE_LIST ||
							category == HelperValues.LOCAL_POISONOUS_LIST ||
							category == HelperValues.LOCAL_THREATENED_ENDANGERED_LIST
							) {
						resID = 0;
						
						imageID = onetime.getImageIDByCName(PBBPlantList.this, commonName, category);
						scienceName = onetime.getScienceName(PBBPlantList.this, commonName, category);
					}
					// if the species is from user defined lists ( greater than 10 )
					// also find the species image based on the imageID
					// Tree folder in the sd card
					else if(category >= HelperValues.USER_DEFINED_TREE_LISTS){
						resID = 0;
					
						scienceName = onetime.getUserListsScienceName(PBBPlantList.this, commonName, category);
						imageID = onetime.getUserListsImageID(PBBPlantList.this, scienceName, category);
						
					}
					
					// get the number of check-marked data
					String pheno_done = "SELECT _id, synced FROM my_observation WHERE species_id = " 
							+ cursor2.getInt(0) + " AND site_id = " + user_station_id.get(i) + " GROUP BY phenophase_id;";
					Cursor cursor4 = syncDB.rawQuery(pheno_done, null);
					int synced_species = 5;
					
					Log.i("K", "cursor4.getCount() : " + cursor4.getCount());
					
					while(cursor4.moveToNext()) {
						// check the species has been synced or not.
						// 5 - synced, 9 - not synced
						if(cursor4.getInt(1) == SyncDBHelper.SYNCED_NO) {
							synced_species = SyncDBHelper.SYNCED_NO;
							// once we find the unsync data in the my_observation, change the synced_species = 9 and break
							break;
						}
						else {
							synced_species = SyncDBHelper.SYNCED_YES;
						}
					}

				
					// get total_number_of phenophases from species
					String total_pheno = "SELECT Phenophase_ID " +
										"FROM Phenophase_Protocol_Icon " +
										"WHERE Protocol_ID = " + cursor2.getInt(3) + ";";
					Cursor cursor5 = staticDB.rawQuery(total_pheno, null);
					
					pi = new HelperPlantItem();
					pi.setPicture(resID);
					pi.setCommonName(common_name);
					pi.setSpeciesName(scienceName);
					pi.setSpeciesID(cursor2.getInt(0));
					pi.setSiteID(user_station_id.get(i));
					pi.setProtocolID(cursor2.getInt(3));
					pi.setCurrentPheno(cursor4.getCount());
					pi.setTotalPheno(cursor5.getCount());
					pi.setSiteName(user_station_name.get(i));
					pi.setMonitor(true);
					pi.setSynced(synced_species);
					pi.setCategory(category);
					pi.setImageID(imageID);
					
					// put values into HelperPlantItem class
					if(!header) {
						pi.setHeader(true);						
						header = true;
					}
					else {
						pi.setHeader(false);
					}
					
					// Add PlantItem into Array
					mArrPlantItem.add(pi);
						
					cursor3.close();
					cursor4.close();
					cursor5.close();
				}
				
				cursor2.close();				
			}

			// show shared plant species
			addSharedPlants();

			mOnetimeStartPoint = count;
			
			// add ArrayList into the adapter
			MyListAdapter mylistapdater = new MyListAdapter(this, R.layout.plantlist_item, mArrPlantItem);
			MyList.setAdapter(mylistapdater);
		}
		catch(Exception e){}
		finally{
			staticDBHelper.close();
			syncDBHelper.close();
			syncDB.close();
		}
	}
	
	public void onResume(){
		super.onResume();
	}

	private void addSharedPlants() {
		
		OneTimeDBHelper onetime = new OneTimeDBHelper(PBBPlantList.this);
		SQLiteDatabase ot  = onetime.getReadableDatabase();
		
		Cursor cursor = ot.rawQuery("SELECT _id, plant_id, species_id, site_id, protocol_id, " +
				"cname, sname, synced, category, is_floracache, floracache_id FROM oneTimePlant WHERE active=1", null);
		HelperPlantItem pi;
		
		// header is called only once. (top)
		boolean header = false;
		int imageID = 0;
		int count = 0;
		while(cursor.moveToNext()) {
			count++;
			
			int speciesID = cursor.getInt(2);
			
			/**
			 *  category == 1 : budburst
			 *  category == 2 : invasive
			 *  category == 3 : native
			 *  category == 4 : poisonous
			 *  category == 5 : endangered
			 *  
			 *  >= 10 : user_defined_lists
			 *  and more later.
			 *  
			 */
			int resID = 0;
			int category = cursor.getInt(8);
			int isFloracache = cursor.getInt(9);
			int floracacheID = cursor.getInt(10);
			String commonName = cursor.getString(5);
			String scienceName = cursor.getString(6);
			
			if(category == HelperValues.LOCAL_BUDBURST_LIST ||
				category == HelperValues.LOCAL_FLICKR) {
				resID = getResources().getIdentifier("edu.ucla.cens.budburstmobile:drawable/s" + speciesID, null, null);
			}
			else if(category == HelperValues.LOCAL_WHATSINVASIVE_LIST ||
					category == HelperValues.LOCAL_POISONOUS_LIST ||
					category == HelperValues.LOCAL_THREATENED_ENDANGERED_LIST
					) {
				resID = 0;
				
				imageID = onetime.getImageIDByCName(PBBPlantList.this, commonName, category);
				scienceName = onetime.getScienceName(PBBPlantList.this, commonName, category);
			}
			else if(category >= HelperValues.USER_DEFINED_TREE_LISTS){
				resID = 0;
			
				scienceName = onetime.getUserListsScienceName(PBBPlantList.this, commonName, category);
				imageID = onetime.getUserListsImageID(PBBPlantList.this, scienceName, category);
				
			}
			else {
				resID = getResources().getIdentifier("edu.ucla.cens.budburstmobile:drawable/s" + speciesID, null, null);
			}

			int pheno_count = 0;
			int pheno_current = 0;
			int synced = SyncDBHelper.SYNCED_YES;
			
			//Log.i("K", "PLANT ID : " + cursor.getInt(1) + " resID : " + resID + " sname : " + cursor.getString(6));
			
			int totalNumPheno = 0;
			
			switch(cursor.getInt(4)) {
			case 1:case 4:case 6:
				totalNumPheno = 15;
				break;
			case 2:
				totalNumPheno = 6;
				break;
			case 3:
				totalNumPheno = 7;
				break;
			}
			
			// query the number of onetime observations have been made by each species
			Cursor pheno_cur = ot.rawQuery("SELECT COUNT(plant_id) as cnt FROM oneTimeObservation WHERE plant_id=" + cursor.getInt(1) + ";", null);
			while(pheno_cur.moveToNext()) {
				pheno_current = pheno_cur.getInt(0);
			}
			
			pheno_cur.close();
			
			pheno_cur = ot.rawQuery("SELECT synced FROM oneTimeObservation WHERE plant_id=" + cursor.getInt(1) + ";", null);
			while(pheno_cur.moveToNext()) {
				if(pheno_cur.getInt(0) == SyncDBHelper.SYNCED_NO) {
					synced = SyncDBHelper.SYNCED_NO;
					break;
				}
			}
			
			pheno_cur.close();

			/**
			 *  If site_id == 0 meaning one-time observations are added to the database in "OneTimeMain.class" activity
			 */
			
			pi = new HelperPlantItem();
			
			pi.setPicture(resID);
			pi.setCommonName(commonName);
			pi.setSpeciesName(scienceName);
			pi.setSpeciesID(speciesID);
			pi.setSiteID(cursor.getInt(3));
			pi.setProtocolID(cursor.getInt(4));
			pi.setCurrentPheno(pheno_current);
			pi.setTotalPheno(totalNumPheno);
			pi.setSiteName("");
			pi.setMonitor(false);
			pi.setSynced(synced);
			pi.setCategory(category);
			pi.setImageID(imageID);
			pi.setIsFloracache(isFloracache);
			pi.setFloracacheID(floracacheID);
			
			
			if(!header) {
				pi.setHeader(true);
				header = true;
			}
			else {
				pi.setHeader(false);		
			}
					
			mArrPlantItem.add(pi);
		}
		
		cursor.close();
		onetime.close();
		ot.close();

	}

	protected void onListItemClick(View v, int position, long id){
		//Intent intent = new Intent(this, PlantInfo.class);
		
		Log.i("K", "POSITION : " + position);
		
		// if the position is in the pbb-list and the species id is 999 (which is unknown)
		if((position <= mOnetimeStartPoint - 1) && mArrPlantItem.get(position).getSpeciesID() == 999) {
			Toast.makeText(PBBPlantList.this, getString(R.string.DoSyncFirst_For_UnknownPlant), Toast.LENGTH_SHORT).show();
		}
		else {
			
			Log.i("K", "ONETIME start point : " + mOnetimeStartPoint);
			
			// position > onetime_start_point - 1 => means the position is from the quick capture list
			if(position > mOnetimeStartPoint - 1) {
				int click_pos = Quick_capture_click_position(position);
				Intent intent = new Intent(this, GetPhenophaseShared.class);
				intent.putExtra("id", click_pos);
				startActivity(intent);

			}
			else {
				Intent intent = new Intent(this, GetPhenophaseObserver.class);
				pbbItem = new PBBItems();
				pbbItem.setSpeciesID(mArrPlantItem.get(position).getSpeciesID());
				pbbItem.setSiteID(mArrPlantItem.get(position).getSiteID());
				pbbItem.setProtocolID(mArrPlantItem.get(position).getProtocolID());
				pbbItem.setCommonName(mArrPlantItem.get(position).getCommonName());
				pbbItem.setScienceName(mArrPlantItem.get(position).getSpeciesName());
				pbbItem.setCategory(mArrPlantItem.get(position).getCategory());
				intent.putExtra("pbbItem", pbbItem);
				intent.putExtra("from", HelperValues.FROM_PLANT_LIST);
				startActivity(intent);
			}
		}
	}
	
	protected boolean onLongListItemClick(View v, int position, long id) {
		mPosition = position;
		
		Log.i("K", "arPlantItem.get(pos).SpeciesID : " + mArrPlantItem.get(mPosition).getSpeciesID());

		new AlertDialog.Builder(PBBPlantList.this)
		.setTitle("Select one")
		.setNegativeButton("Back", null)
		.setItems(R.array.plantlist, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				String[] category = getResources().getStringArray(R.array.plantlist);
				// so far there are two items - delete species, edit species
				// not allow to change the name of species if the species is from the official ppb-lists
				// deleting item not used
				if(category[which].equals("Delete Species")) {
					confirmDialog();
				}
				else if(category[which].equals("Add Same Species")){
					
					/**
					 * If user choose Monitored Plant 
					 */
					if(mPosition <= mOnetimeStartPoint - 1) {
						mSpeciesID = mArrPlantItem.get(mPosition).getSpeciesID();
						mCommonName = mArrPlantItem.get(mPosition).getCommonName();
						mProtocolID = mArrPlantItem.get(mPosition).getProtocolID();
						mCategory = mArrPlantItem.get(mPosition).getCategory();
						chooseSiteDialog();
					}
					/**
					 * else, user choose Quick Shared Plant
					 */
					else {
						new AlertDialog.Builder(PBBPlantList.this)
						.setTitle(getString(R.string.Menu_addQCPlant))
						.setMessage(getString(R.string.Start_Shared_Plant))
						.setPositiveButton(getString(R.string.Button_Photo), new DialogInterface.OnClickListener() {
							
							@Override
							public void onClick(DialogInterface dialog, int which) {
								// TODO Auto-generated method stub
								/**
								 * Move to QuickCapture
								 */
								Intent intent = new Intent(PBBPlantList.this, QuickCapture.class);
								pbbItem = new PBBItems();
								pbbItem.setSpeciesID(mArrPlantItem.get(mPosition).getSpeciesID());
								pbbItem.setProtocolID(mArrPlantItem.get(mPosition).getProtocolID());
								pbbItem.setPlantID(mArrPlantItem.get(mPosition).getPlantID());
								pbbItem.setCommonName(mArrPlantItem.get(mPosition).getCommonName());
								pbbItem.setScienceName(mArrPlantItem.get(mPosition).getSpeciesName());
								pbbItem.setCategory(mArrPlantItem.get(mPosition).getCategory());
								pbbItem.setNote(mArrPlantItem.get(mPosition).getNote());
								
								intent.putExtra("from", HelperValues.FROM_QUICK_CAPTURE_ADD_SAMESPECIES);
								intent.putExtra("pbbItem", pbbItem);
								
								startActivity(intent);
							}
						})
						.setNeutralButton(getString(R.string.Button_NoPhoto), new DialogInterface.OnClickListener() {
							
							@Override
							public void onClick(DialogInterface dialog, int which) {
								// TODO Auto-generated method stub
								Intent intent = new Intent(PBBPlantList.this, OneTimePhenophase.class);
								pbbItem = new PBBItems();
								pbbItem.setSpeciesID(mArrPlantItem.get(mPosition).getSpeciesID());
								pbbItem.setProtocolID(mArrPlantItem.get(mPosition).getProtocolID());
								pbbItem.setPlantID(mArrPlantItem.get(mPosition).getPlantID());
								pbbItem.setCommonName(mArrPlantItem.get(mPosition).getCommonName());
								pbbItem.setScienceName(mArrPlantItem.get(mPosition).getSpeciesName());
								pbbItem.setCategory(mArrPlantItem.get(mPosition).getCategory());
								pbbItem.setNote("");
								pbbItem.setLocalImageName("");
								
								
								intent.putExtra("from", HelperValues.FROM_QUICK_CAPTURE_ADD_SAMESPECIES);
								intent.putExtra("pbbItem", pbbItem);
								startActivity(intent);
							}
						})
						.setNegativeButton(getString(R.string.Button_Cancel), new DialogInterface.OnClickListener() {
							
							@Override
							public void onClick(DialogInterface dialog, int which) {
								// TODO Auto-generated method stub
							}
						})
						.show();
					}
				}
				else {
					/**
					 * if pbb list chosen and 0 < species_id < 77, we don't allow users to change the value
					 */
					if((mPosition <= mOnetimeStartPoint - 1) && mArrPlantItem.get(mPosition).getSpeciesID() > 0 && mArrPlantItem.get(mPosition).getSpeciesID() < 77) {
						Toast.makeText(PBBPlantList.this, getString(R.string.Cannot_Change), Toast.LENGTH_SHORT).show();
					}
					else {
						dialog(mArrPlantItem.get(mPosition).getSpeciesID(), mArrPlantItem.get(mPosition).getSiteID());	
					}
				}
			}
		})
		.show();
		
		return true;
	}
	
	private void chooseSiteDialog() {
		/**
		 * Pop up choose site dialog box
		 */
		mSeqUserSite = mHelper.getUserSite(PBBPlantList.this);
		mapUserSiteNameID = mHelper.getUserSiteIDMap(PBBPlantList.this);
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.AddPlant_chooseSite))
		.setCancelable(true)
		.setItems(mSeqUserSite, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
			
				int new_plant_site_id = mapUserSiteNameID.get(mSeqUserSite[which].toString());
				String new_plant_site_name = mSeqUserSite[which].toString();
				
				if(new_plant_site_name == "Add New Site") {
					Intent intent = new Intent(PBBPlantList.this, PBBAddSite.class);
					pbbItem = new PBBItems();
					pbbItem.setSpeciesID(mArrPlantItem.get(mPosition).getSpeciesID());
					pbbItem.setProtocolID(mArrPlantItem.get(mPosition).getProtocolID());
					pbbItem.setCommonName(mArrPlantItem.get(mPosition).getCommonName());
					intent.putExtra("pbbItem", pbbItem);
					
					intent.putExtra("from", HelperValues.FROM_PLANT_LIST);
					startActivity(intent);
				}
				else {
					if(mHelper.checkIfNewPlantAlreadyExists(mSpeciesID, new_plant_site_id, PBBPlantList.this)){
						Toast.makeText(PBBPlantList.this, getString(R.string.AddPlant_alreadyExists), Toast.LENGTH_LONG).show();
					}else{

						if(mHelper.insertNewMyPlantToDB(PBBPlantList.this, mSpeciesID, mCommonName, new_plant_site_id, new_plant_site_name, mProtocolID, mCategory)){
							Intent intent = new Intent(PBBPlantList.this, PBBPlantList.class);
							Toast.makeText(PBBPlantList.this, getString(R.string.AddPlant_newAdded), Toast.LENGTH_SHORT).show();
							
							/*
							 * Clear all stacked activities.
							 */
							intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
							startActivity(intent);
							finish();
						}else{
							Toast.makeText(PBBPlantList.this, getString(R.string.Alert_dbError), Toast.LENGTH_SHORT).show();
						}
					}
				}
			}
		})
		.setNegativeButton(getString(R.string.Button_cancel), null)
		.show();
	}
	
	public void confirmDialog() {
		new AlertDialog.Builder(PBBPlantList.this)
		.setTitle("Confirm")
		.setMessage("Delete the species?")
		.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				try{
					SyncDBHelper syncDBHelper = new SyncDBHelper(PBBPlantList.this);
					SQLiteDatabase syncDB = syncDBHelper.getWritableDatabase();
					
					OneTimeDBHelper otDBHelper = new OneTimeDBHelper(PBBPlantList.this);
					SQLiteDatabase onetime = otDBHelper.getWritableDatabase();
					
					// if the position is in the pbb list
					if(mPosition <= mOnetimeStartPoint - 1) {
						syncDB.execSQL("UPDATE my_plants SET active = 0 AND synced = " + SyncDBHelper.SYNCED_NO + " WHERE species_id=" + mArrPlantItem.get(mPosition).getSpeciesID() 
								+ " AND site_id=" + mArrPlantItem.get(mPosition).getSiteID() + ";");
						syncDB.execSQL("UPDATE my_plants SET synced = " + SyncDBHelper.SYNCED_NO + " WHERE species_id=" + mArrPlantItem.get(mPosition).getSpeciesID() 
								+ " AND site_id=" + mArrPlantItem.get(mPosition).getSiteID() + ";");
						syncDB.execSQL("DELETE FROM my_observation WHERE species_id=" + mArrPlantItem.get(mPosition).getSpeciesID() + " AND site_id=" + mArrPlantItem.get(mPosition).getSiteID());
					}
					// else if the position is in the quick capture list
					else {
						// delete the onetime species_
						int click_pos = Quick_capture_click_position(mPosition);
						onetime.execSQL("UPDATE oneTimePlant SET active = 0, synced = " + SyncDBHelper.SYNCED_NO + " WHERE plant_id=" + click_pos);
						onetime.execSQL("DELETE FROM oneTimeObservation WHERE plant_id=" + click_pos);
						//Toast.makeText(PlantList.this, getString(R.string.Alert_comingSoon), Toast.LENGTH_SHORT).show();
					}
					
					syncDB.close();
					syncDBHelper.close();
					
					onetime.close();
					otDBHelper.close();
						
					Toast.makeText(PBBPlantList.this, getString(R.string.Item_deleted), Toast.LENGTH_SHORT).show();
						
					Intent intent = new Intent(PBBPlantList.this, PBBPlantList.class);
					intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					finish();
					startActivity(intent);
				}
				catch(Exception e){
				}				
			}
		})
		.setNegativeButton("No", null)
		.show();
	}
	
	
	public int Quick_capture_click_position(int position) {
		OneTimeDBHelper onetime = new OneTimeDBHelper(PBBPlantList.this);
		SQLiteDatabase onetimeDB  = onetime.getReadableDatabase();
		Cursor cursor = onetimeDB.rawQuery("SELECT plant_id FROM oneTimePlant WHERE active = 1;", null);
		
		int query_count = 1;
		int getID = 0;

		int onetime_point = position - mOnetimeStartPoint + 1;
		Log.i("K", "ONETIME POINT : " + onetime_point);
		
		while(cursor.moveToNext()) {
			if(onetime_point == query_count) {
				getID = cursor.getInt(0);
			}
			query_count++;
		}
		
		cursor.close();
		onetimeDB.close();
		
		return getID;
	}
	
	public void dialog(int species_id, int site_id) {
		
		mDialogSpeciesID = species_id;
		mDialogSiteID = site_id;
		
		mDialog = new Dialog(PBBPlantList.this);
		
		mDialog.setContentView(R.layout.species_name_custom_dialog);
		mDialog.setTitle(getString(R.string.GetPhenophase_PBB_message));
		mDialog.setCancelable(true);
		mDialog.show();
		
		et1 = (EditText)mDialog.findViewById(R.id.custom_common_name);
		Button doneBtn = (Button)mDialog.findViewById(R.id.custom_done);
		
		doneBtn.setOnClickListener(new View.OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				String common_name = et1.getText().toString();
				if(common_name.equals("")) {
				// nothing happens
				}
				else {
					// if the position is in the pbb list
					if(mPosition <= mOnetimeStartPoint - 1) { 
						SyncDBHelper syncDBHelper = new SyncDBHelper(PBBPlantList.this);
						SQLiteDatabase syncDB  = syncDBHelper.getWritableDatabase();
						
						Log.i("K", "UPDATE my_plants SET common_name=\"" + common_name 
								+ "\" WHERE species_id=" + mDialogSpeciesID 
								+ ";");

						syncDB.execSQL("UPDATE my_plants SET common_name=\"" + common_name 
								+ "\" WHERE species_id=" + mDialogSpeciesID + " AND site_id =" + mDialogSiteID 
								+ ";");
						
						syncDB.execSQL("UPDATE my_plants SET active=2 WHERE species_id=" + mDialogSpeciesID + ";");
						syncDB.execSQL("UPDATE my_plants SET synced=9 WHERE species_id=" + mDialogSpeciesID + ";");

						syncDBHelper.close();

					}
					else {
						
						OneTimeDBHelper onetimeDB = new OneTimeDBHelper(PBBPlantList.this);
						SQLiteDatabase oneDB = onetimeDB.getWritableDatabase();
						
						int click_id = Quick_capture_click_position(mPosition);
						// update cname
						oneDB.execSQL("UPDATE oneTimePlant SET cname=\"" + common_name +
								"\" WHERE plant_id=" + click_id + ";");
						// change synced to 9
						oneDB.execSQL("UPDATE oneTimePlant SET synced=9 WHERE plant_id=" + click_id + ";");
						
						onetimeDB.close();
						oneDB.close();
						
					}
					
					Toast.makeText(PBBPlantList.this, getString(R.string.GetPhenophase_PBB_update_name), Toast.LENGTH_SHORT).show();
				}
				
				mDialog.cancel();
				
				Intent intent = new Intent(PBBPlantList.this, PBBPlantList.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				finish();
				startActivity(intent);
			}
		});
	}
		
	/**
	 * Menu option(non-Javadoc)
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	public boolean onCreateOptionsMenu(Menu menu){
		super.onCreateOptionsMenu(menu);
		menu.add(0, MENU_ADD_PLANT, 0, getString(R.string.Menu_addPlant)).setIcon(android.R.drawable.ic_menu_add);
		menu.add(0, MENU_ADD_QC_PLANT, 0, getString(R.string.Menu_addQCPlant)).setIcon(android.R.drawable.ic_menu_camera);
		menu.add(0, MENU_SYNC, 0, getString(R.string.Menu_sync)).setIcon(android.R.drawable.ic_menu_rotate);
		menu.add(0, MENU_HELP, 0, getString(R.string.Menu_help)).setIcon(android.R.drawable.ic_menu_help);
			
		return true;
	}
	
	/**
	 * Menu option selection handling(non-Javadoc)
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	public boolean onOptionsItemSelected(MenuItem item){
		Intent intent;
		switch(item.getItemId()){
			case MENU_ADD_PLANT:
				intent = new Intent(PBBPlantList.this, OneTimeMainPage.class);
				pbbItem = new PBBItems();
				intent.putExtra("pbbItem", pbbItem);
				intent.putExtra("from", HelperValues.FROM_PLANT_LIST);
				startActivity(intent);
				return true;
			case MENU_ADD_QC_PLANT:
				/*
				 * Ask users if they are ready to take a photo.
				 */
				new AlertDialog.Builder(PBBPlantList.this)
				.setTitle(getString(R.string.Menu_addQCPlant))
				.setMessage(getString(R.string.Start_Shared_Plant))
				.setPositiveButton(getString(R.string.Button_Photo), new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						/*
						 * Move to QuickCapture
						 */
						Intent intent = new Intent(PBBPlantList.this, QuickCapture.class);
						pbbItem = new PBBItems();
						intent.putExtra("pbbItem", pbbItem);
						intent.putExtra("from", HelperValues.FROM_QUICK_CAPTURE);
						startActivity(intent);
					}
				})
				.setNeutralButton(getString(R.string.Button_NoPhoto), new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						Intent intent = new Intent(PBBPlantList.this, OneTimeMainPage.class);
						pbbItem = new PBBItems();
						pbbItem.setLocalImageName("");
						intent.putExtra("pbbItem", pbbItem);
						intent.putExtra("from", HelperValues.FROM_QUICK_CAPTURE);
						startActivity(intent);
					}
				})
				.setNegativeButton(getString(R.string.Button_Cancel), new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
					}
				})
				.show();

				return true;
			case MENU_SYNC:
				intent = new Intent(PBBPlantList.this, PBBSync.class);
				intent.putExtra("sync_instantly", true);
				intent.putExtra("from", HelperValues.FROM_PLANT_LIST);
				startActivity(intent);
				finish();
				return true;
			case MENU_HELP:
				intent = new Intent(PBBPlantList.this, PBBHelpPage.class);
				intent.putExtra("from", HelperValues.FROM_PLANT_LIST);
				startActivity(intent);
				return true;
		}
		return false;
	}

	
	class MyListAdapter extends BaseAdapter{
		Context maincon;
		LayoutInflater Inflater;
		ArrayList<HelperPlantItem> arSrc;
		int layout;
		int previous_site = 0;
		
		public MyListAdapter(Context context, int alayout, ArrayList<HelperPlantItem> aarSrc){
			maincon = context;
			Inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			arSrc = aarSrc;
			layout = alayout;
		}
		
		public int getCount(){
			return arSrc.size();
		}
		
		public String getItem(int position){
			return arSrc.get(position).getCommonName();
		}
		
		public long getItemId(int position){
			return position;
		}
		
		public View getView(int position, View convertView, ViewGroup parent){
			if(convertView == null)
				convertView = Inflater.inflate(layout, parent, false);
			
			TextView site_header = (TextView)convertView.findViewById(R.id.list_header);

			if(arSrc.get(position).getMonitor()) {
				if(arSrc.get(position).getHeader()) {
					site_header.setVisibility(View.VISIBLE);
					site_header.setText(getString(R.string.Monitor_Plant));
				}
				else {
					site_header.setVisibility(View.GONE);
				}
			}
			else if(!arSrc.get(position).getMonitor()) {
				if(arSrc.get(position).getHeader()) {
					site_header.setVisibility(View.VISIBLE);
					site_header.setText(getString(R.string.Quick_Plant));
				}
				else {
					site_header.setVisibility(View.GONE);
				}
			}
			
			ImageView img = (ImageView)convertView.findViewById(R.id.icon);
			Bitmap icon = null;
			
			/**
			 * If the position is in onetime table,
			 * 
			 */
			//if(position >= mOnetimeStartPoint) {
				/**
				 * 1 - BudBurst
				 * 2 - Invasive
				 * 3 - Native
				 * 4 - Poisonous
				 * 5 - Endangered
				 * 10 - Treelists
				 * 11 - Blooming 
				 */
				
				switch(arSrc.get(position).getCategory()) {
				case 0: case HelperValues.LOCAL_BUDBURST_LIST: case HelperValues.LOCAL_FLICKR:
					
					Log.i("K", "arSrc.get(position).getPicture() : " + arSrc.get(position).getPicture());
					if(arSrc.get(position).getPicture() == 0) {
						int resID = getResources().getIdentifier("edu.ucla.cens.budburstmobile:drawable/pbb_icon_main2", null, null);
						icon = mHelper.overlay(BitmapFactory.decodeResource(getResources(), resID));
					}
					else {
						icon = mHelper.overlay(BitmapFactory.decodeResource(getResources(), arSrc.get(position).getPicture()));
					}
					
					
					//If not synced, put the icon on the species image.
					if(arSrc.get(position).getSynced() == SyncDBHelper.SYNCED_NO) {
						icon = mHelper.overlay(icon, BitmapFactory.decodeResource(getResources(), R.drawable.unsynced));
					}
					
					/*
					if(arSrc.get(position).getIsFloracache() != 0) {
						icon = mHelper.overlay(icon, BitmapFactory.decodeResource(getResources(), R.drawable.is_floracache));
					}
					*/
					
					break;
					
				case HelperValues.LOCAL_WHATSINVASIVE_LIST:					
					Log.i("K", "PBBPlantList(Picture, ImageID) : " + arSrc.get(position).getPicture() 
							+ " , " + arSrc.get(position).getImageID());
					
					icon = mHelper.getImageFromSDCard(PBBPlantList.this, arSrc.get(position).getImageID(), icon);
					
					//If not synced, put the icon on the species image.
					if(arSrc.get(position).getSynced() == SyncDBHelper.SYNCED_NO) {
						icon = mHelper.overlay(icon, BitmapFactory.decodeResource(getResources(), R.drawable.unsynced));
					}
					
					if(arSrc.get(position).getIsFloracache() != 0) {
						icon = mHelper.overlay(icon, BitmapFactory.decodeResource(getResources(), R.drawable.is_floracache));
					}
					break;
				case HelperValues.LOCAL_POISONOUS_LIST:
					Log.i("K", "PBBPlantList(Picture, ImageID) : " + arSrc.get(position).getPicture() 
							+ " , " + arSrc.get(position).getImageID());
					
					icon = mHelper.getImageFromSDCard(PBBPlantList.this, arSrc.get(position).getImageID(), icon);
					
					//If not synced, put the icon on the species image.
					if(arSrc.get(position).getSynced() == SyncDBHelper.SYNCED_NO) {
						icon = mHelper.overlay(icon, BitmapFactory.decodeResource(getResources(), R.drawable.unsynced));
					}
					
					if(arSrc.get(position).getIsFloracache() != 0) {
						icon = mHelper.overlay(icon, BitmapFactory.decodeResource(getResources(), R.drawable.is_floracache));
					}
					break;
				case HelperValues.LOCAL_THREATENED_ENDANGERED_LIST:
					Log.i("K", "PBBPlantList(Picture, ImageID) : " + arSrc.get(position).getPicture() 
							+ " , " + arSrc.get(position).getImageID());
					
					icon = mHelper.getImageFromSDCard(PBBPlantList.this, arSrc.get(position).getImageID(), icon);
					
					//If not synced, put the icon on the species image.
					if(arSrc.get(position).getSynced() == SyncDBHelper.SYNCED_NO) {
						icon = mHelper.overlay(icon, BitmapFactory.decodeResource(getResources(), R.drawable.unsynced));
					}
					
					if(arSrc.get(position).getIsFloracache() != 0) {
						icon = mHelper.overlay(icon, BitmapFactory.decodeResource(getResources(), R.drawable.is_floracache));
					}
					break;
				default:
					// Call images from the TREE_PATH
					OneTimeDBHelper oDBH = new OneTimeDBHelper(PBBPlantList.this);
					String imagePath = HelperValues.TREE_PATH + arSrc.get(position).getImageID() + ".jpg";
					
					Log.i("K", "PBBPlantList(imagePath Tree) : " + imagePath +
							"SpeciesName : " + arSrc.get(position).getSpeciesName());
					
					File checkExistFile = new File(imagePath);
					if(checkExistFile.exists()) {
						icon = mHelper.overlay(BitmapFactory.decodeFile(imagePath));
					}
					else {
						int resID = getResources().getIdentifier("edu.ucla.cens.budburstmobile:drawable/s1000", null, null);
						icon = mHelper.overlay(BitmapFactory.decodeResource(getResources(), resID));
					}
					
					// If not synced, put the icon on the species image.
					if(arSrc.get(position).getSynced() == SyncDBHelper.SYNCED_NO) {
						icon = mHelper.overlay(icon, BitmapFactory.decodeResource(getResources(), R.drawable.unsynced));
					}
					
					if(arSrc.get(position).getIsFloracache() != 0) {
						icon = mHelper.overlay(icon, BitmapFactory.decodeResource(getResources(), R.drawable.is_floracache));
					}
					break;
				}				
		
			img.setImageBitmap(icon);
		
			
			TextView textName = (TextView)convertView.findViewById(R.id.commonname);
			String common_name_with_fc_status = arSrc.get(position).getCommonName();
			if(arSrc.get(position).getIsFloracache() != 0) {
				common_name_with_fc_status = "[FC] " + common_name_with_fc_status;
			}
			textName.setText(common_name_with_fc_status);
			
			TextView textDesc = (TextView)convertView.findViewById(R.id.speciesname);
			if(arSrc.get(position).getMonitor()) {
				textDesc.setText(arSrc.get(position).getSiteName());
				textDesc.setVisibility(View.VISIBLE);
			}
			else {
				textDesc.setVisibility(View.GONE);
			}
			
			/**
			 *  Call View from the xml and link the view to current position.
			 */
			View thumbNail = convertView.findViewById(R.id.wrap_icon);
			thumbNail.setTag(arSrc.get(position));
			thumbNail.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					HelperPlantItem pi = (HelperPlantItem)v.getTag();
					
					pbbItem = new PBBItems();
					pbbItem.setSpeciesID(pi.getSpeciesID());
					pbbItem.setCommonName(pi.getCommonName());
					pbbItem.setScienceName(pi.getSpeciesName());
					pbbItem.setCategory(pi.getCategory());
					
					if(pi.getIsFloracache() == 1) {
						Intent intent = new Intent(PBBPlantList.this, DetailPlantInfoFloracache.class);
						pbbItem.setFloracacheID(pi.getFloracacheID());
						intent.putExtra("pbbItem", pbbItem);
						
						startActivity(intent);
					}
					else { 
						Intent intent = new Intent(PBBPlantList.this, DetailPlantInfo.class);
						intent.putExtra("pbbItem", pbbItem);
						
						startActivity(intent);
					}
				}
			});
			
			/**
			 * Show the current phenophase observed and total phenophase
			 */
			TextView phenoStat = (TextView)convertView.findViewById(R.id.pheno_stat);
			if(arSrc.get(position).getTotalPheno() != 0) {
				phenoStat.setText(arSrc.get(position).getCurrentPheno() + " / " + arSrc.get(position).getTotalPheno() + " ");
			}
			else {
				phenoStat.setVisibility(View.GONE);
			}

			return convertView;
		}
	}

    /**
     *  or when user press back button(non-Javadoc)
     * @see android.app.Activity#onKeyDown(int, android.view.KeyEvent)
     * 
     */
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if(keyCode == event.KEYCODE_BACK) {
			Intent intent = new Intent(PBBPlantList.this, PBBMainPage.class);
			
			/**
			 *  Remove all previous message, we don't need anything when we go back to mainpage
			 *  
			 */
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			finish();
			return true;
		}
		return false;
	}
}


