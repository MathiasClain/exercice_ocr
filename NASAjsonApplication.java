package com.example.smb116NASAjson;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Vector;

import android.app.Application;
import android.util.Log;

public class NASAjsonApplication extends Application{

	private Vector<DatasetNASA> lesBases;
	
	public NASAjsonApplication(){
		super();
		lesBases = new Vector<DatasetNASA>();
	}
	
	public void effacer(){
		lesBases.clear();
	}
	public void addDataset(DatasetNASA dataSet){
		lesBases.add(dataSet);
	}
	
	public DatasetNASA  getDatasetNum(int num){
		DatasetNASA base = null;
		if ((num>=0) && (num<lesBases.size())) base = lesBases.get(num);
		return base;
	}

	public DatasetNASA getDatasetById(int idBase) {
		DatasetNASA base = null;
		for (DatasetNASA b : lesBases){
			if (b.getId()==idBase) {
				base= b;
				break;
			}
		}
		return base;
	}
	
	public String[] getTitles(){
		String[] lesTitres = new String[lesBases.size()];
		int pos = 0;
		for (DatasetNASA b : lesBases){
			lesTitres[pos++]= b.getTitle();
		}
		//Arrays.sort(lesTitres);
		//Log.d("Les titres!",lesTitres.toString());
		return lesTitres;
	}
	
	public int getIdTitle(String title){
		int id = -1;
		for (DatasetNASA b : lesBases){
			if (b.getTitle().equals(title)) {
				id = b.getId();
				break;
			}
		}
		return id;
	}
}
