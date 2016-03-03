package es.uclm.esi.arco.logicwiring;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import Ice.ObjectPrx;

class Thing {
    public enum Type {
        SENDER,
        RECEIVER
    }

    public Type type;
    public String addr;

    public Thing(String idm_addr, Type type) {
        this.type = type;
        this.addr = idm_addr;
    }
}

class ThingViewAdapter
        extends ArrayAdapter<Thing>
        implements AdapterView.OnItemLongClickListener,
        View.OnDragListener {

    public ThingViewAdapter(Context context, int resource) {
        super(context, resource);
    }

    public ThingViewAdapter(Context context, int resource, List<Thing> items) {
        super(context, resource, items);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;

        if (v == null) {
            LayoutInflater vi;
            vi = LayoutInflater.from(getContext());
            v = vi.inflate(R.layout.simple_thing_item, null);
        }

        Thing t = getItem(position);

        if (t != null) {
            TextView tv = (TextView) v.findViewById(R.id.ThingText);
            if (tv != null) {
                tv.setText(t.addr);

                int icon = R.drawable.ic_wb_incandescent_black_48dp_orange;
                if (t.type == Thing.Type.SENDER)
                    icon = R.drawable.ic_touch_app_black_48dp_blue;

                tv.setCompoundDrawablesRelativeWithIntrinsicBounds(0, icon, 0, 0);
            }
        }

        v.setOnDragListener(this);
        return v;
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        Thing t = getItem(position);

        ClipData.Item item = new ClipData.Item(t.addr);
        ClipData data = new ClipData(t.addr, new String[]{ClipDescription.MIMETYPE_TEXT_PLAIN}, item);
        View.DragShadowBuilder shadow = new View.DragShadowBuilder(view);
        view.startDrag(data, shadow, null, 0);

        return true;
    }

    @Override
    public boolean onDrag(View v, DragEvent event) {
        final int action = event.getAction();

        switch (action) {
            case DragEvent.ACTION_DRAG_STARTED:
                return event.getClipDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN);

            case DragEvent.ACTION_DROP:
                System.out.println("Drop of item!");
                return true;
        }

        return false;
    }
}

public class MainActivity extends AppCompatActivity {

    enum Action {
        NONE,
        FETCH_CONFIG,
        ADD_THING,
    }

    private final static String PREF_IDM_ROUTER = "pref_idm_router";
    private Action _current_action = Action.NONE;
    private Ice.Communicator _ic;
    private ObjectPrx _router;
    private ThingViewAdapter _receiver_things;
    private ThingViewAdapter _sender_things;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String idm_str_proxy = sharedPref.getString(PREF_IDM_ROUTER, "");

        if (idm_str_proxy.isEmpty())
            setContentView(R.layout.welcome_activity);

        else {
            setContentView(R.layout.things_activity);

            GridView view;
            _receiver_things = new ThingViewAdapter(this, R.layout.simple_thing_item);
            view = (GridView) findViewById(R.id.receivers_holder);
            view.setAdapter(_receiver_things);
            view.setOnItemLongClickListener(_receiver_things);

            _sender_things = new ThingViewAdapter(this, R.layout.simple_thing_item);
            view = (GridView) findViewById(R.id.senders_holder);
            view.setAdapter(_sender_things);
            view.setOnItemLongClickListener(_sender_things);

            addThing("10:00");

//            createCommunicator();
//            createIDMRouter(idm_str_proxy);
//            for (int i=0; i<10; i++) {
//                addThing("10:" + i);
//            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add:
                _current_action = Action.ADD_THING;
                getQRScannerIntentIntegrator().initiateScan();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void onStartClicked(View view) {
        _current_action = Action.FETCH_CONFIG;
        getQRScannerIntentIntegrator().initiateScan();
    }

    private IntentIntegrator getQRScannerIntentIntegrator() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setCaptureActivity(PortraitCaptureActivity.class);
        integrator.setPrompt((String) getResources().getText(R.string.scan_qr_prompt));
        integrator.setOrientationLocked(false);
        return integrator;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result == null) {
            super.onActivityResult(requestCode, resultCode, data);
            return;
        }

        String content = result.getContents();
        switch (_current_action) {
            case ADD_THING:
                addThing(content);
                break;

            case FETCH_CONFIG:
                new RetrieveJSONConfig().execute(content);
                break;
        }
    }

    private void addThing(String content) {
        // FIXME: check if content is a valid IDM address
        if (content.startsWith("http://")) {
            Toast.makeText(this, getResources().getText(R.string.error_invalid_addr)
                    + ": " + content, Toast.LENGTH_LONG).show();
            return;
        }
        _receiver_things.add(new Thing(content, Thing.Type.RECEIVER));
        _sender_things.add(new Thing(content, Thing.Type.SENDER));
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
                    errorMessage = (String) getResources().getText(R.string.error_invalid_usage);
                    return null;
                }

                url = urls[0];
                URL site = new URL(url);
                BufferedReader in = new BufferedReader(new InputStreamReader(site.openStream()));

                // read data
                String line = null;
                while ((line = in.readLine()) != null)
                    content.append(line);

                // no error reported
                return null;

            } catch (MalformedURLException e) {
                errorMessage = (String) getResources().getText(R.string.error_invalid_contents) + ": " + url;

            } catch (IOException e) {
                errorMessage = (String) getResources().getText(R.string.error_cant_connect) + ": " + url;
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

            String router = jObject.getString("IDMRouter");
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
            sharedPref.edit().putString(PREF_IDM_ROUTER, router).apply();

            // restart activity
            finish();
            startActivity(getIntent());

        } catch (JSONException e) {
            String msg = (String) getResources().getText(R.string.error_invalid_retrieved_config);
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        }
    }

    private void createCommunicator() {
        Ice.InitializationData idata = new Ice.InitializationData();
        idata.properties = Ice.Util.createProperties();

        _ic = Ice.Util.initialize(idata);
    }

    private void createIDMRouter(String idm_str_proxy) {
        _router = _ic.stringToProxy(idm_str_proxy);
    }
}
