/*
Copyright (c) 2014 Corbin Leigh Champion

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
 */

/* Author(s): Corbin Leigh Champion */

package com.gnuroot.rsinstaller;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.gnuroot.rsinstaller.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.support.v4.content.FileProvider;
import android.util.Log;

public class RSLauncherMain extends Activity {

	// GNURoot won't launch unless its version matches at least this.
	String GNURootVersion = "76";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		SharedPreferences prefs = getSharedPreferences("MAIN", MODE_PRIVATE);
		SharedPreferences.Editor editor = prefs.edit();
		Intent intent;
		PackageInfo packageInfo = null;

		try { packageInfo = getPackageManager().getPackageInfo("com.gnuroot.debian", 0); }
		catch (NameNotFoundException e) { showUpdateError(); }

		if(packageInfo == null || packageInfo.versionCode < Integer.parseInt(GNURootVersion))
			showUpdateError();

		else {
			copyAssets("com.gnuroot.rsinstaller");
			intent = getLaunchIntent();
			startActivity(intent);
			finish();
		}
	}

	/**
	 * Displays an alert dialog if GNURoot Debian isn't updated to at least the version that included the
	 * new ecosystem changes. Sends the user to the market page for it if not.
	 */
	private void showUpdateError() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setCancelable(false);
		builder.setMessage(R.string.update_error_message);
		builder.setPositiveButton(R.string.button_affirmative, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setData(Uri.parse("market://details?id=com.gnuroot.debian"));
				startActivity(intent);
				finish();
			}
		});
		builder.create().show();
	}

	/**
	 * GNURoot Debian expects the following extras from install intents:
	 * 	1. launchType: This can be either launchTerm or launchXTerm. The command that is to be run after installation
	 * 		dictates this selection.
	 *	2. command: This is the command that will be executed in proot after installation. Often, this will be a
	 *		script stored in your custom tar file to install additional packages if needed or to execute the extension.
	 *	3. customTar: This is the custom tar file you've created for your extension.
	 * @return
     */
	private Intent getLaunchIntent() {
		String command;
		Intent installIntent = new Intent("com.gnuroot.debian.LAUNCH");
		installIntent.setComponent(new ComponentName("com.gnuroot.debian", "com.gnuroot.debian.GNURootMain"));
		installIntent.addCategory(Intent.CATEGORY_DEFAULT);
		installIntent.putExtra("launchType", "launchXTerm");
        command =
            "#!/bin/bash\n" +
            "if [ ! -f /support/.rs_custom_passed ] || [ ! -f /support/.rs_updated ]; then\n" +
            "  /support/untargz rs_custom /support/rs_custom.tar.gz\n" +
            "fi\n" +
            "if [ -f /support/.rs_custom_passed ]; then\n" +
            "  if [ ! -f /support/.rs_script_passed ]; then\n" +
            "    sudo /support/blockingScript rs_script /support/oldschoolrs_install.sh\n" +
            "  fi\n" +
			"  if [ ! -f /support/.rs_script_updated ]; then\n" +
			"    sudo mv /support/rs_update /usr/bin/oldschoolrs\n" +
			"    if [ $? == 0 ]; then\n" +
			"      touch /support/.rs_script_updated\n" +
			"      sudo chmod 755 /usr/bin/oldschoolrs\n" +
			"    fi\n" +
			"  fi\n" +
            "  if [ -f /support/.rs_script_passed ]; then\n" +
            "    /usr/bin/oldschoolrs\n" +
            "  fi\n" +
            "fi\n";
		installIntent.putExtra("command", command);
		installIntent.putExtra("packageName", "com.gnuroot.rsinstaller");
		installIntent.putExtra("GNURootVersion", GNURootVersion);
		installIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		installIntent.setData(getTarUri());
		return installIntent;
	}

	/**
	 * Returns a Uri for the custom tar placed in the project's assets directory.
	 * @return
     */
	private Uri getTarUri() {
		File fileHandle = new File(getFilesDir() + "/rs_custom.tar.gz");
		return FileProvider.getUriForFile(this, "com.gnuroot.rsinstaller.fileprovider", fileHandle);
	}

	/**
	 * Renames assets from .mp3 to .tar.gz.
	 * @param packageName
     */
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
			Log.i("files ", filename);
			InputStream in = null;
			OutputStream out = null;
			try {
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
}