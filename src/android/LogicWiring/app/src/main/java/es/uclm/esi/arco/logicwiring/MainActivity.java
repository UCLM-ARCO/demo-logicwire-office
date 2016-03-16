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
import android.widget.GridLayout;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import Ice.ObjectPrx;
import PropertyService.PropertyServerPrx;
import PropertyService.PropertyServerPrxHelper;

class ThingObject {
    public enum Type {
        SENDER,
        RECEIVER
    }

    public Type type;
    public String addr;
    public ThingObject observable;

    public ThingObject(String idm_addr, Type type) {
        this.type = type;
        this.addr = idm_addr;
        this.observable = null;
    }

    public void setObservable(ThingObject observable) {
        this.observable = observable;
    }
}

class ThingView implements View.OnLongClickListener, View.OnDragListener {

    private ThingObject _thingObject;
    private View _view;

    public ThingView(Context context, ViewGroup parent, ThingObject thingObject) {
        _thingObject = thingObject;

        // load card from XML layout
        LayoutInflater vi;
        vi = LayoutInflater.from(context);
        _view = vi.inflate(R.layout.linked_thing_item, null);
        parent.addView(_view);

        updateContent();
    }

    private void updateContent() {
        // set ADDR for text
        TextView tv = (TextView) _view.findViewById(R.id.SrcThingText);

        String content = _thingObject.addr;
        if (_thingObject.observable != null) {
            GridLayout.LayoutParams lp = (GridLayout.LayoutParams) _view.getLayoutParams();
            lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 2);
            _view.setLayoutParams(lp);

            View addon = _view.findViewById(R.id.LinkedThingHolder);
            addon.setVisibility(View.VISIBLE);

            TextView dst_tv = (TextView) _view.findViewById(R.id.DstThingText);
            dst_tv.setText(_thingObject.observable.addr);

            int dst_icon = R.drawable.ic_wb_incandescent_black_48dp_orange;
            if (_thingObject.observable.type == ThingObject.Type.SENDER)
                dst_icon = R.drawable.ic_touch_app_black_48dp_blue;
            dst_tv.setCompoundDrawablesRelativeWithIntrinsicBounds(0, dst_icon, 0, 0);
        }
        tv.setText(content);

        // set icon by TYPE
        int icon = R.drawable.ic_wb_incandescent_black_48dp_orange;
        if (_thingObject.type == ThingObject.Type.SENDER)
            icon = R.drawable.ic_touch_app_black_48dp_blue;
        tv.setCompoundDrawablesRelativeWithIntrinsicBounds(0, icon, 0, 0);

        // connect callbacks
        if (_thingObject.type == ThingObject.Type.SENDER)
            tv.setOnLongClickListener(this);
        tv.setOnDragListener(this);
    }

    @Override
    public boolean onLongClick(View v) {
        ClipData.Item item = new ClipData.Item(_thingObject.addr);
        ClipData data = new ClipData(_thingObject.addr, new String[]{ClipDescription.MIMETYPE_TEXT_PLAIN}, item);
        View.DragShadowBuilder shadow = new View.DragShadowBuilder(v);
        v.startDrag(data, shadow, _thingObject, 0);
        return true;
    }

    @Override
    public boolean onDrag(View v, DragEvent event) {
        final int action = event.getAction();

        switch (action) {
            case DragEvent.ACTION_DRAG_STARTED: {
                return event.getClipDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) &&
                        _thingObject.type == ThingObject.Type.RECEIVER;
            }

            case DragEvent.ACTION_DROP: {
                // drop on non RECEIVER, discard
                if (_thingObject.type != ThingObject.Type.RECEIVER)
                    return false;

                ThingObject src = (ThingObject) event.getLocalState();

                // drop on same item, discard
                if (Objects.equals(src.addr, _thingObject.addr)) {
                    return false;
                }

                // set dst as observable for src and update views
                _thingObject.setObservable(src);
                updateContent();
                return true;
            }

            case DragEvent.ACTION_DRAG_ENDED: {
                if (event.getResult()) {
                    ThingObject src = (ThingObject) event.getLocalState();
                    if (Objects.equals(src.addr, _thingObject.addr))
                        _view.setAlpha((float) 0.5);
                }

                return true;
            }
        }

        return false;
    }
}

public class MainActivity extends AppCompatActivity {

    private static final String PREF_PROPERTY_SERVER = "pref_property_server";
    private static final String PREF_IDM_ROUTER = "pref_idm_router";

    enum Action {
        NONE,
        FETCH_CONFIG,
        ADD_THING,
    }

    private Action _current_action = Action.NONE;
    private HashMap<String, ThingObject> _things;
    private Ice.Communicator _ic;
    private ObjectPrx _router;
    private PropertyServerPrx _ps;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String idm_str_proxy = sharedPref.getString(PREF_IDM_ROUTER, "");
        _things = new HashMap<>();

        if (idm_str_proxy.isEmpty())
            setContentView(R.layout.welcome_activity);

        else {
            setContentView(R.layout.things_activity);

            createCommunicator();
            createProxies();

            // DEBUG
            addThingFromQR("10:00");
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
                addThingFromQR(content);
                break;

            case FETCH_CONFIG:
                new RetrieveJSONConfig().execute(content);
                break;
        }
    }

    private void addThingFromQR(String addr) {
        if (addr.startsWith("http://")) {
            Toast.makeText(this, getResources().getText(R.string.error_invalid_addr)
                    + ": " + addr, Toast.LENGTH_LONG).show();
            return;
        }

        if (_things.containsKey(addr)) {
            Toast.makeText(this, getResources().getText(R.string.error_repeated_addr)
                    + ": " + addr, Toast.LENGTH_LONG).show();
            return;
        }

        // contact with PS to retrieve this object's properties
        System.out.println("Retrieving properties from PS server...");
        Map<String, Thing.T> props = _ps.slice(new String[][] {});
        for (String a : props.keySet()) {
            System.out.println(a);
        }

        // if object has children, it is a node address
        //  - add each one of its childrens
        // else, add it

        GridLayout holder = (GridLayout) findViewById(R.id.receivers_holder);
        ThingObject t = new ThingObject(addr, ThingObject.Type.RECEIVER);
        _things.put(addr, t);
        new ThingView(this, holder, t);
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

    private void createCommunicator() {
        Ice.InitializationData idata = new Ice.InitializationData();
        idata.properties = Ice.Util.createProperties();

        _ic = Ice.Util.initialize(idata);
    }

    private void createProxies() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String idm_str_proxy = sharedPref.getString(PREF_IDM_ROUTER, "");
        _router = _ic.stringToProxy(idm_str_proxy);

        String ps_str_proxy = sharedPref.getString(PREF_PROPERTY_SERVER, "");
        ObjectPrx prx = _ic.stringToProxy(ps_str_proxy);
        try {
            _ps = PropertyServerPrxHelper.checkedCast(prx);
        } catch (Ice.Exception e) {
            Toast.makeText(this, getString(R.string.ps_unreachable), Toast.LENGTH_LONG).show();
        }
    }
}
