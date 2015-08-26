package com.drelleum.oldschoolrs;

import android.support.v4.content.FileProvider;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import com.gnuroot.library.GNURootCoreActivity;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class GameActivity extends GNURootCoreActivity {

	private boolean checkingInstalled;
	private boolean installing;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_game);
		
		checkingInstalled = false;
		
		Button installButton = (Button) this.findViewById(R.id.installButton);
		installButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				installing = false;
				checkingInstalled = true;
				ArrayList<String> prerequisitesArrayList = new ArrayList<String>();
				prerequisitesArrayList.add("gnuroot_rootfs");
				prerequisitesArrayList.add("oldschoolrs");
				GameActivity.this.runCommand("/bin/true", prerequisitesArrayList);
			}
		});
		
		Button playButton = (Button) this.findViewById(R.id.playButton);
		playButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				ArrayList<String> prerequisitesArrayList = new ArrayList<String>();
					prerequisitesArrayList.add("gnuroot_rootfs");
					prerequisitesArrayList.add("oldschoolrs");
				// The ':0.0' should be replaced with the port number, but for now is hard-coded
				GameActivity.this.runCommand("oldschoolrs :0.0", prerequisitesArrayList);
			}
		});
	}

	private void copyAssets(String packageName) {
		Context friendContext = null;
		try {
			friendContext = this.createPackageContext(packageName,Context.CONTEXT_IGNORE_SECURITY);
		} catch (NameNotFoundException e1) {
			return;
		}
		AssetManager assetManager = friendContext.getAssets();
		String[] files = null;
		try {
			files = assetManager.list("");
		} catch (IOException e) {
			Log.e("tag", "Failed to get asset file list.", e);
		}
		for(String filename : files) {
			InputStream in = null;
			OutputStream out = null;
			try {
				Toast.makeText(GameActivity.this, filename, Toast.LENGTH_LONG).show();
				in = assetManager.open(filename);
				filename = filename.replace(".mp3", ".tar.gz");
				out = openFileOutput(filename,MODE_PRIVATE);
				copyFile(in, out);
				in.close();
				in = null;
				out.flush();
				out.close();
				out = null;
			} catch(IOException e) {
				Log.e("tag", "Failed to copy asset file: " + filename, e);
			}       
		}
	}

	private void copyFile(InputStream in, OutputStream out) throws IOException {
		byte[] buffer = new byte[1024];
		int read;
		while((read = in.read(buffer)) != -1){
			out.write(buffer, 0, read);
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.game, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	/**
	 * Special check if the script has already installed OldSchoolRS
	 */
	@Override
	public void nextStep(Intent intent) {
		super.nextStep(intent);
		if (intent.getStringExtra("packageName").equals(getPackageName())) {
			int found = intent.getIntExtra("found", 0);
			int resultCode = intent.getIntExtra("resultCode",0);
			int requestCode = intent.getIntExtra("requestCode",0);
			if (checkingInstalled) {		
				checkingInstalled = false;
				//If the result is not that the prereqs failed
				if (resultCode != 2) {
					Toast.makeText(GameActivity.this, "Game Already Installed", Toast.LENGTH_LONG).show();
				} else {
					installing = true;
					Toast.makeText(GameActivity.this, "Copying Assets...", Toast.LENGTH_LONG).show();
					//Copy the assets over and install the install script... if that makes sense
					copyAssets("com.drelleum.oldschoolrs");
					File fileHandle = new File(getFilesDir() + "/install.tar.gz");
					ArrayList<String> prerequisitesArrayList = new ArrayList<String>();
					prerequisitesArrayList.add("gnuroot_rootfs");
					installTar(FileProvider.getUriForFile(GameActivity.this, "com.drelleum.oldschoolrs.fileprovider", fileHandle), "oldschoolrs_script", prerequisitesArrayList);
				}
			}
			else if ((requestCode == CHECK_STATUS) && (found == 1) && installing){ //Need to update this. I think the installTar will make  oldschoolrs_passed (which isn't what I want)
				installing = false;
				Toast.makeText(GameActivity.this, "Installing Game...", Toast.LENGTH_LONG).show();
				
				//Run install script
				ArrayList<String> prerequisitesArrayList = new ArrayList<String>();
				prerequisitesArrayList.add("gnuroot_rootfs");
				GameActivity.this.runCommand("/usr/bin/oldschoolrs_install.sh", prerequisitesArrayList);
			}
			if (requestCode == RUN_SCRIPT) {
				Thread thread = new Thread() {
					@Override
					public void run() {
						//Block thread for a time
						try {
							Thread.sleep(1000);
						}
						catch (InterruptedException e) {
						}
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								progressDialog.dismiss();
							}
						});
					}
				};
				thread.start();
			}
		}
	}
}
