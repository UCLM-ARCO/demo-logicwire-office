package es.uclm.esi.arco.smartbutton;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.menu.ActionMenuItemView;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ExecutionException;

import Ice.ObjectPrx;
import PropertyService.PropertyException;
import PropertyService.PropertyServerPrx;
import PropertyService.PropertyServerPrxHelper;
import ThingSlice.TypeCode;

import static java.lang.Math.min;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener {

    public static final String PREF_PROPERTY_SERVER = "pref_property_server";
    public static final String PREF_IDM_ROUTER = "pref_idm_router";
    public static final String PREF_IDM_ADDRESS = "pref_idm_address";

    private GoogleApiClient _client;
    private Ice.Communicator _ic;
    private PropertyServerPrx _ps;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();

        _client = getGoogleApiClient();
        setSyncEnabled(false);

        createCommunicator();
        createProxies();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.actions, menu);
        return true;
    }

    public void onLoadSettings(MenuItem item) {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setCaptureActivity(PortraitCaptureActivity.class);
        integrator.setPrompt((String) getResources().getText(R.string.scan_qr_prompt));
        integrator.setOrientationLocked(false);
        integrator.initiateScan();
    }

    public void onSyncSettings(MenuItem item) {
        syncPreferencesWithDevices();
        setProperties();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result == null) {
            super.onActivityResult(requestCode, resultCode, data);
            return;
        }

        String content = result.getContents();
        if (content == null)
            return;

        // Remove UTF-16 BOM (if present)
        if (content.startsWith("\uFEFF")) {
            content = content.substring(1);
        }

        new RetrieveJSONConfig().execute(content);
    }

    private class RetrieveJSONConfig extends AsyncTask<String, Void, Void> {

        private String errorMessage = "";
        private StringBuilder content = new StringBuilder();

        @Override
        protected Void doInBackground(String... urls) {
            String url = null;

            try {
                // open stream
                if (urls.length < 1) {
                    System.out.println("ERROR: you must provide a set of URLs");
                    return null;
                }

                url = urls[0];
                URL site = new URL(url);
                BufferedReader in = new BufferedReader(new InputStreamReader(site.openStream()));

                // read data
                String line;
                while ((line = in.readLine()) != null)
                    content.append(line);

                // no error reported
                return null;

            } catch (MalformedURLException e) {
                errorMessage = getResources().getText(R.string.error_invalid_contents) + ": " + url;

            } catch (IOException e) {
                errorMessage = getResources().getText(R.string.error_cant_connect) + ": " + url;
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (! errorMessage.isEmpty()) {
                Toast.makeText(getApplicationContext(), errorMessage, Toast.LENGTH_LONG).show();
                return;
            }

            parseJSONConfig(content.toString());
        }
    }

    protected void parseJSONConfig(String json) {
        try {
            JSONObject jObject = new JSONObject(json);
            PreferenceManager.getDefaultSharedPreferences(this)
                    .edit()
                    .putString(PREF_IDM_ROUTER, jObject.getString("IDMRouter"))
                    .putString(PREF_PROPERTY_SERVER, jObject.getString("PropertyServer"))
                    .apply();

            // restart activity
            finish();
            startActivity(getIntent());

        } catch (JSONException e) {
            String msg = (String) getResources().getText(R.string.error_invalid_retrieved_config);
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        }
    }

    private void syncPreferencesWithDevices() {
        System.out.println("Sync preferences with devices");

        // get prefs from manager
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String ps_proxy = prefs.getString(PREF_PROPERTY_SERVER, "");
        String router_proxy = prefs.getString(PREF_IDM_ROUTER, "");
        String idm_addr = prefs.getString(PREF_IDM_ADDRESS, "");

        // set prefs on wear device
        PutDataMapRequest request = PutDataMapRequest.create("/prefs");
        DataMap map = request.getDataMap();
        map.putString(PREF_PROPERTY_SERVER, ps_proxy);
        map.putString(PREF_IDM_ROUTER, router_proxy);
        map.putString(PREF_IDM_ADDRESS, idm_addr);
        PutDataRequest data = request.asPutDataRequest();

        Wearable.DataApi.putDataItem(_client, data);
    }

    // FIXME: implement setJSON on PS and use it!
    private void setProperties() {
        System.out.println("Setting properties on PS...");

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        final String idm_addr = prefs.getString(PREF_IDM_ADDRESS, "").replace(":", "").trim();
        if (idm_addr.isEmpty()) {
            System.out.println(" - IDM Address not defined, nothing done.");
            return;
        }

        final ThingSlice.T t_name = new ThingSlice.T(TypeCode.stringT, stringToByteseq("SmartButton"));
        final ThingSlice.T t_icon = new ThingSlice.T(TypeCode.stringT, stringToByteseq("touch"));
        final ThingSlice.T t_tags = new ThingSlice.T(TypeCode.thingSeqT, stringSeqToByteseq(new String[]{"producer"}));

        class MyTask extends AsyncTask<Void, Void, Void> {
            String error = "";

            @Override
            protected Void doInBackground(Void... params) {
                if (_ps == null) {
                    error = "PS is not correctly setup";
                    return null;
                }

                try {
                    _ps.set(idm_addr + "| name", t_name);
                    _ps.set(idm_addr + "| icon", t_icon);
                    _ps.set(idm_addr + "| tags", t_tags);
                } catch (PropertyException e) {
                    e.printStackTrace();
                }

                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                if (error.isEmpty())
                    return;

                Toast.makeText(MainActivity.this, error, Toast.LENGTH_LONG).show();
            }
        }

        try {
            new MyTask().execute().get();
        } catch (InterruptedException | ExecutionException e){
            e.printStackTrace();
        }

    }

    // FIXME: implement setJSON on PS and use it!
    private byte[] stringToByteseq(String s) {
        byte[] retval = new byte[s.length() + 1];
        retval[0] = (byte) min(255, s.length());
        System.arraycopy(s.getBytes(), 0, retval, 1, s.length());
        return retval;
    }

    // FIXME: implement setJSON on PS and use it!
    private byte[] stringSeqToByteseq(String[] sa) {
        // compute total size of byteseq
        byte size = 0;
        for (String s: sa) {
            size += s.length() + 2;  // tc + s.length
        }

        // add number of items on array
        byte[] retval = new byte[size + 1];
        retval[0] = (byte) sa.length;

        // add each string as a TC + byteseq
        byte i = 1;
        for (String s: sa) {
            retval[i++] = (byte) TypeCode.stringT.value();
            retval[i++] = (byte) s.length();
            System.arraycopy(s.getBytes(), 0, retval, i, s.length());
            i += s.length();
        }

        return retval;
    }

    private GoogleApiClient getGoogleApiClient() {
        System.out.println("API create client");

        return new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(@Nullable Bundle bundle) {
                        System.out.println("API connected");
                        setSyncEnabled(true);
                    }

                    @Override
                    public void onConnectionSuspended(int i) {
                        System.out.println("API connection suspended");
                    }
                })
                .addApi(Wearable.API)
                .build();
    }

    private void setSyncEnabled(boolean enabled) {
        ActionMenuItemView entry = (ActionMenuItemView) findViewById(R.id.action_sync_settings);
        if (entry == null)
            return;

        entry.setEnabled(enabled);
        int icon = enabled ? R.drawable.ic_sync_white_24dp : R.drawable.ic_sync_red_24dp;
        entry.setIcon(getResources().getDrawable(icon));
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult result) {
        System.out.println("API connection failed: " + result);
    }

    private void createCommunicator() {
        final Ice.InitializationData idata = new Ice.InitializationData();
        idata.properties = Ice.Util.createProperties();
        idata.properties.setProperty("Ice.IPv6", "0");

        class MyTask extends AsyncTask<Void, Void, Void> {
            @Override
            protected Void doInBackground(Void... params) {
                _ic = Ice.Util.initialize(idata);
                return null;
            }
        }
        try {
            new MyTask().execute().get();
        } catch (InterruptedException | ExecutionException e){
            e.printStackTrace();
        }
    }

    private void createProxies() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String ps_str_proxy = sharedPref.getString(PREF_PROPERTY_SERVER, "");

        ObjectPrx prx = _ic.stringToProxy(ps_str_proxy);
        try {
            _ps = PropertyServerPrxHelper.checkedCast(prx);
        } catch (Ice.Exception e) {
            Toast.makeText(this, getString(R.string.ps_unreachable), Toast.LENGTH_LONG).show();
        }
    }
}