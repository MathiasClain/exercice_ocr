package com.example.smb116NASAjson;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collections;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.example.smb116http.R;

public class MainActivity extends Activity {
	private InputStream inputStream = null;
	private long sizeStream;
	private ProgressDialog progressDialog = null;
	private int nbrBases;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		// obtenirJSONdata(20); // avant refresh()
		refresh();

	}

	public void quitter(){
		// Autres façons de quitter une application : 
		// http://stackoverflow.com/questions/6368215/how-to-force-close-the-app-when-using-multiple-activity
		finish();
	}

	public void refresh(){
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		int nbr = Integer.parseInt(prefs.getString("number_max_dataset", Constantes.NOMBRE_DE_BASES));
		obtenirJSONdata(nbr);		

	}

	public void obtenirJSONdata(int nbr){
		inputStream = null;
		progressDialog = ProgressDialog.show(this, "Lecture bases NASA","connection à http://data.nasa.gov/", true);
		nbrBases = nbr;
		new HttpGetTask().execute("http://data.nasa.gov/api/get_recent_datasets/?count="+nbrBases);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main_activity, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		int id = item.getItemId();
		switch(id){
		case R.id.action_quit:
			quitter();
			return true;
		}	
		return super.onOptionsItemSelected(item);
	}

	private void  setProgressPercent(int pos){
		Log.v("setProgressPercent","value = "+pos+"/100");
		progressDialog.setProgress(pos);
	}

	private String[] sortListeTitles(String[] titles){
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		int nbr = Integer.parseInt(prefs.getString("sort_list_base", Constantes.NOMBRE_DE_BASES));
		switch(nbr){
		case 2:
			Arrays.sort(titles);
			break;
		case 3:
			Arrays.sort(titles, Collections.reverseOrder(String.CASE_INSENSITIVE_ORDER));
			break;
		}
		return titles;
	}

	public void miseAjourListBase(){
		FragmentManager fm = getFragmentManager();
		FragmentListBases fListBases = (FragmentListBases)fm.findFragmentById(R.id.fragment_list_bases);
		NASAjsonApplication application = (NASAjsonApplication)getApplication();
		String[] listBases = application.getTitles();
		sortListeTitles(listBases);
		fListBases.modifierContenuListe(listBases);

	}
	public void miseAjourInfo(int id){
		FragmentManager fm = getFragmentManager();
		FragmentInfoBase fInfoBase =  (FragmentInfoBase)fm.findFragmentById(R.id.fragment_info_base);	
		if(fInfoBase != null){
			fInfoBase.changeIdBase(id);
		}
	}
	private class HttpGetTask extends AsyncTask<String, Void, HttpResponse> {
		protected HttpResponse doInBackground(String... urls) {
			HttpResponse httpReponse = OpenHttpGETConnection(urls[0]);
			return httpReponse;
		}


		public HttpResponse OpenHttpGETConnection(String url) {
			HttpResponse httpResponse = null;
			try {
				HttpClient httpclient = new DefaultHttpClient();
				httpResponse = httpclient.execute(new HttpGet(url));
			} catch (ClientProtocolException e){
				String msg = "";
				if (e!=null) msg = e.getMessage();
				Log.d("InputStream", "ClientProtocolException : "+msg);
			} catch (IOException e) {
				String msg = "";
				if (e!=null) msg = e.getMessage();
				Log.d("InputStream", "IOException : "+msg);
			}

			return httpResponse;
		}

		@Override
		protected void onPostExecute(HttpResponse reponse) {
			StatusLine statusLine = reponse.getStatusLine();
			int statusCode = statusLine.getStatusCode();
			if (statusCode == 200){
				HttpEntity httpEntity = reponse.getEntity();
				sizeStream = httpEntity.getContentLength();
				// si la taille du fichier n'est pas connue, alors une estimation est réalisée
				if (sizeStream <= 0) sizeStream =2000*nbrBases;
				try {
					inputStream = httpEntity.getContent();
				} catch (IllegalStateException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				Toast.makeText(getBaseContext(), "HttpGEtOk : " + sizeStream,
						Toast.LENGTH_LONG).show();
				Log.d("HttpGetTask", "onPostExecute");
				if (inputStream!=null){
					progressDialog.dismiss();
					progressDialog = null;
					progressDialog = new ProgressDialog(MainActivity.this);
					progressDialog.setTitle("Lecture bases NASA");
					progressDialog
					.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
					progressDialog.setMax(100);
					progressDialog.setProgress(0);
					progressDialog.setCancelable(false);
					progressDialog.setMessage("Chargement des données");
					progressDialog.setIndeterminate(false);
					// progressDialog.setProgressPercentFormat(NumberFormat.INTEGER_FIELD);
					progressDialog.show();
					new HttpGetStream().execute(inputStream);
				}
				else {
					Toast.makeText(getBaseContext(), "Impossible de lire le Stream", Toast.LENGTH_LONG).show();
				}

			}
			else if(statusCode == 400 ){
				Toast.makeText(getBaseContext(), "Impossible de lire le Stream", Toast.LENGTH_LONG).show();
			} else{
				Toast.makeText(getBaseContext(), "Erreur 400 : le site n'est pas accessible !", Toast.LENGTH_LONG).show();
			}
		}
	}

	private class HttpGetStream extends AsyncTask<InputStream, Integer, String> {
		protected String doInBackground(InputStream... streams) {
			String codeHtml = downloadText(streams[0]);
			return codeHtml;
		}

		private String downloadText(InputStream in) {
			int BUFFER_SIZE = 500;
			InputStreamReader isr = new InputStreamReader(in);
			int charRead;
			long totalCharRead = 0;
			String str = "";
			char[] inputBuffer = new char[BUFFER_SIZE];
			try {
				while ((charRead = isr.read(inputBuffer)) > 0) {
					String readString = String.copyValueOf(inputBuffer, 0,
							charRead);
					str += readString;
					totalCharRead += charRead;
					int pos = (int)(totalCharRead*100.0/sizeStream);
					Log.v("DownloadText","pos="+pos+" ("+totalCharRead+"/"+sizeStream+")");
					publishProgress(pos);
					// inputBuffer = new char[BUFFER_SIZE];
				}
				in.close();
			} catch (IOException e) {
				Log.d("DownloadText", e.getLocalizedMessage());
				return "";
			}
			return str;
		}


		@Override
		protected void onProgressUpdate(Integer... progress) {
			setProgressPercent(progress[0]);
		}

		@Override
		protected void onPostExecute(String reponse) {
			Log.v("HttpGetStream", "reponse : "+reponse);
			Toast.makeText(getBaseContext(), "HttpGetStream : ",
					Toast.LENGTH_LONG).show();
			Log.d("HttpGetStream", "onPostExecute");
			progressDialog.dismiss();
			NASAjsonApplication application = (NASAjsonApplication)getApplication();
			try {
				JSONObject jsonResultat = new JSONObject(reponse);
				int count = jsonResultat.getInt("count");
				int count_total =  jsonResultat.getInt("count_total");
				Toast.makeText(getBaseContext(), "count : "+count+", count_total : "+count_total,
						Toast.LENGTH_LONG).show();
				JSONArray lesBases = jsonResultat.getJSONArray("posts");
				int firstId = -1;
				application.effacer();
				for (int i=0; i<lesBases.length(); i++){
					JSONObject base = lesBases.getJSONObject(i);
					int id = base.getInt("id");
					if (firstId<0) firstId = id;
					String slug = base.getString("slug");
					String url = base.getString("url");
					String title = base.getString("title");
					String title_plain = base.getString("title_plain");
					String content = base.getString("content");
					String excerpt = base.getString("excerpt");
					String date = base.getString("date");
					String modified = base.getString("modified");
					// TODO traiter les catégories et tags
					DatasetNASA dataSet = new DatasetNASA(id, slug, url, title, title_plain, content, excerpt, date, modified);
					JSONArray mesCategories = base.getJSONArray("categories");
					for(int j =0;j<mesCategories.length();j++){
						try{
							JSONObject jCat = mesCategories.getJSONObject(j);
							if(jCat != null){
								Log.d("Catégorie",jCat.getString("title"));
								CategorieNASA cat = new CategorieNASA(jCat.getInt("id"),jCat.getString("slug"),jCat.getString("title"),jCat.getString("description"));
								dataSet.addCategorie(cat);
							}
						}catch(JSONException e){

						}
					}
					JSONArray mesTags = base.getJSONArray("tags");
					for(int j =0;j<mesTags.length();j++){
						try{
							JSONObject jTag = mesTags.getJSONObject(j);
							if(jTag != null){
								TagNASA tag = new TagNASA(jTag.getInt("id"),jTag.getString("slug"),jTag.getString("title"),jTag.getString("description"));
								dataSet.addTag(tag);
							}	
						}catch(JSONException e){

						}
					}
					application.addDataset(dataSet);
					Log.v("LesBases",title+" ("+i+")");
				}
				miseAjourListBase();
				miseAjourInfo(firstId);
			} catch (JSONException e) {
				e.printStackTrace();
			}

		}
	}

}
