package es.uclm.esi.arco.logicwiring;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

import Ice.ObjectPrx;
import PropertyService.PropertyException;
import PropertyService.PropertyServerPrx;
import PropertyService.PropertyServerPrxHelper;

class ThingObject {
    public enum Type {
        SENDER,
        RECEIVER
    }

    public Type type;
    public String addr;
    public String name;
    public Integer icon;
    public ThingObject consumer;

    private ObjectPrx _router;

    public ThingObject(String idm_addr, Type type, JSONObject props, ObjectPrx router)
            throws JSONException {

        this.type = type;
        this.addr = idm_addr;
        this.consumer = null;
        this._router = router;

        // set icon
        HashMap<String, Integer> icons = new HashMap<>();
        icons.put("touch", R.drawable.ic_touch_app_black_48dp_blue);
        icons.put("bulb", R.drawable.ic_wb_incandescent_black_48dp_orange);
        icons.put("radio", R.drawable.ic_radio_button_checked_black_24dp_green);

        String icon_name = "touch";
        if (props.has("icon"))
            icon_name = props.getString("icon");
        else if (this.type == Type.RECEIVER)
            icon_name = "bulb";

        this.icon = icons.get(icon_name);
        if (this.icon == null)
            this.icon = R.drawable.ic_touch_app_black_48dp_blue;

        // set name
        this.name = idm_addr;
        if (props.has("name"))
            this.name = props.getString("name");
    }

    public void setConsumer(ThingObject consumer) {
        if (consumer != null)
            setupLink(this.addr, consumer.addr);
        else if (this.consumer != null)
            setupLink(this.addr, "00:00");

        this.consumer = consumer;
    }

    private void setupLink(String producer, String consumer) {
        // get router's proxy with consumer.addr
        Ice.Communicator ic = _router.ice_getCommunicator();
        Ice.Identity dst_id = ic.stringToIdentity(producer.replace(":", ""));
        ObjectPrx prx = _router.ice_identity(dst_id);
        DUO.IDM.Active.WPrx dst = DUO.IDM.Active.WPrxHelper.uncheckedCast(prx);

        // call to setupLink with this.addr
        dst.begin_setObserver(stringToIdmAddress(consumer));
    }

    private byte[] stringToIdmAddress(String addr) {
        String[] fields = addr.split(":");
        byte[] retval = new byte[fields.length];

        for (int i=0; i<fields.length; i++)
            retval[i] = Byte.parseByte(fields[i], 16);
        return retval;
    }
}

class ThingView implements View.OnLongClickListener, View.OnDragListener, PopupMenu.OnMenuItemClickListener {

    private ThingObject _thingObject;
    private View _view;
    private MainActivity _activity;

    public ThingView(MainActivity activity, ViewGroup parent, ThingObject thingObject) {
        _thingObject = thingObject;
        _activity = activity;

        // load card from XML layout
        LayoutInflater vi;
        vi = LayoutInflater.from(activity);
        _view = vi.inflate(R.layout.linked_thing_item, null);
        parent.addView(_view);

        updateContent();
    }

    private void updateContent() {
        // setup card
        TextView tv = (TextView) _view.findViewById(R.id.SrcThingText);
        tv.setText(_thingObject.name);
        tv.setCompoundDrawablesRelativeWithIntrinsicBounds(0, _thingObject.icon, 0, 0);

        // append linked view (if any)
        if (_thingObject.consumer != null) {
            GridLayout.LayoutParams lp = (GridLayout.LayoutParams) _view.getLayoutParams();
            lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 2);
            _view.setLayoutParams(lp);

            View addon = _view.findViewById(R.id.LinkedThingHolder);
            addon.setVisibility(View.VISIBLE);

            TextView dst_tv = (TextView) _view.findViewById(R.id.DstThingText);
            dst_tv.setText(_thingObject.consumer.name);
            dst_tv.setCompoundDrawablesRelativeWithIntrinsicBounds(0, _thingObject.consumer.icon, 0, 0);
        }
        else {
            View addon = _view.findViewById(R.id.LinkedThingHolder);
            addon.setVisibility(View.GONE);
        }

        // connect callbacks
        tv.setOnLongClickListener(this);
        tv.setOnDragListener(this);
    }

    @Override
    public boolean onLongClick(View v) {
        if (_thingObject.type == ThingObject.Type.RECEIVER)
            return onReceiverLongClick(v);
        else
            return onSenderLongClick(v);
    }

    private boolean onSenderLongClick(View v) {
        PopupMenu menu = new PopupMenu(_activity, v);
        menu.setOnMenuItemClickListener(this);
        menu.inflate(R.menu.receiver_menu);
        menu.show();
        return true;
    }

    private boolean onReceiverLongClick(View v) {
        ClipData.Item item = new ClipData.Item(_thingObject.addr);
        ClipData data = new ClipData(_thingObject.addr, new String[]{ClipDescription.MIMETYPE_TEXT_PLAIN}, item);
        View.DragShadowBuilder shadow = new View.DragShadowBuilder(v);
        v.startDrag(data, shadow, _thingObject, 0);
        return true;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if (_thingObject.consumer == null)
            return true;

        Toast.makeText(_view.getContext(), "Removing observer of " + _thingObject.name,
                Toast.LENGTH_LONG).show();

        _thingObject.setConsumer(null);
        updateContent();
        return true;
    }

    @Override
    public boolean onDrag(View v, DragEvent event) {
        final int action = event.getAction();

        switch (action) {
            case DragEvent.ACTION_DRAG_STARTED: {
                return event.getClipDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) &&
                        _thingObject.type == ThingObject.Type.SENDER;
            }

            case DragEvent.ACTION_DROP: {
                // drop on non RECEIVER, discard
                if (_thingObject.type != ThingObject.Type.SENDER)
                    return false;

                ThingObject src = (ThingObject) event.getLocalState();
                if (src == null)
                    return false;

                // drop on same item, discard
                if (src.addr.equals(_thingObject.addr)) {
                    return false;
                }

                // set dst as consumer for src and update views
                _thingObject.setConsumer(src);
                Toast.makeText(v.getContext(), "Setting " + src.name + " as observer of " + _thingObject.name,
                        Toast.LENGTH_LONG).show();
                System.out.println("call " + _thingObject.name + ".setObserver(" + src.name + ")");
                updateContent();

                return true;
            }

            case DragEvent.ACTION_DRAG_ENDED: {
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
    private HashMap<String, ThingView> _thingViews;
    private Ice.Communicator _ic;
    private ObjectPrx _router;
    private PropertyServerPrx _ps;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String idm_str_proxy = sharedPref.getString(PREF_IDM_ROUTER, "");
        _thingViews = new HashMap<>();

        if (idm_str_proxy.isEmpty())
            setContentView(R.layout.welcome_activity);

        else {
            setContentView(R.layout.things_activity);

            createCommunicator();
            createProxies();

            // DEBUG
            try {
                addThingFromQR("10:00");
                addThingFromQR("11:00");
            } catch (JSONException e) {
                Toast.makeText(this, "Error: " + e.toString(), Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
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

            case R.id.action_settings:
                onStartClicked(null);
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
        if (content == null)
            return;

        switch (_current_action) {
            case ADD_THING:
                try {
                    addThingFromQR(content);
                } catch (JSONException e) {
                    Toast.makeText(this, getString(R.string.error_with_properties),
                            Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
                break;

            case FETCH_CONFIG:
                new RetrieveJSONConfig().execute(content);
                break;
        }
    }

    private void addThingFromQR(String addr) throws JSONException {
        if (_ps == null) {
            Toast.makeText(this, getString(R.string.ps_unreachable), Toast.LENGTH_LONG).show();
            return;
        }

        if (addr == null || addr.startsWith("http://")) {
            Toast.makeText(this, getResources().getText(R.string.error_invalid_addr)
                    + ": " + addr, Toast.LENGTH_LONG).show();
            return;
        }

        if (_thingViews.containsKey(addr)) {
            Toast.makeText(this, getResources().getText(R.string.error_repeated_addr)
                    + ": " + addr, Toast.LENGTH_LONG).show();
            return;
        }

        // contact with PS to retrieve this object's properties
        System.out.println("Retrieving properties from PS server...");
        JSONObject props = getProperties(addr);
        if (props == null)
            return;

        // if there is no children, its an object
        if (! props.has("children")) {
            System.out.println("Given addr is an object, adding it...");
            addThingWithProps(addr, props);
            return;
        }

        // otherwise is a node, add each one of its children
        System.out.println("Given addr is a node, with children: " + props.getJSONArray("children").toString());
        JSONArray children = props.getJSONArray("children");
        JSONObject child_props = getChildrenProperties(children);
        if (child_props == null)
            return;

        for (int i=0; i<children.length(); i++) {
            String child = children.getString(i);
            if (! child_props.has(child)) {
                System.out.println("Missing properties for '" + child + "'");
                continue;
            }

            JSONObject iprops = child_props.getJSONObject(child);
            addThingWithProps(child, iprops);
        }
    }

    private void addThingWithProps(String addr, JSONObject props) throws JSONException {
        if (! props.has("tags")) {
            System.out.println("Invalid thing: missing 'tags' property");
            return;
        }

        int holder_id;
        ThingObject.Type type;

        ArrayList<String> tags = JSONArrayToList(props.getJSONArray("tags"));
        if (tags.contains("consumer")) {
            holder_id = R.id.receivers_holder;
            type = ThingObject.Type.RECEIVER;
        }
        else  if (tags.contains("producer")){
            holder_id = R.id.senders_holder;
            type = ThingObject.Type.SENDER;
        }
        else {
            System.out.println("Object " + addr + " is neither 'producer' or 'consumer', ignoring it...");
            return;
        }

        System.out.println("Adding " + addr + " as " + type.toString());
        GridLayout holder = (GridLayout) findViewById(holder_id);
        ThingObject t = new ThingObject(addr, type, props, _router);

        _thingViews.put(addr, new ThingView(this, holder, t));
    }

    private ArrayList<String> JSONArrayToList(JSONArray tags) throws JSONException {
        ArrayList<String> result = new ArrayList<>();
        for (int i=0; i<tags.length(); i++)
            result.add(tags.getString(i));
        return result;
    }

    private JSONObject getProperties(String addr) {
        try {
            String str_props = _ps.getJSON(addr);
            return new JSONObject(str_props);
        } catch (JSONException | PropertyException e) {
            Toast.makeText(this, getString(R.string.unable_to_get_properties)
                    + ": " + addr, Toast.LENGTH_LONG).show();
            e.printStackTrace();
            return null;
        }
    }

    private JSONObject getChildrenProperties(JSONArray children) throws JSONException {
        String[] paths = new String[children.length()];
        for (int i=0; i<children.length(); i++) {
            paths[i] = children.getString(i);
        }
        String str_props = _ps.sliceJSON(paths);
        return new JSONObject(str_props);
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
        String idm_str_proxy = sharedPref.getString(PREF_IDM_ROUTER, "");
        try {
            _router = _ic.stringToProxy(idm_str_proxy);
        } catch (Ice.Exception e) {
            Toast.makeText(this, getString(R.string.ps_invalid_proxy) + ":" + idm_str_proxy,
                    Toast.LENGTH_LONG).show();
        }

        String ps_str_proxy = sharedPref.getString(PREF_PROPERTY_SERVER, "");
        ObjectPrx prx = _ic.stringToProxy(ps_str_proxy);
        try {
            _ps = PropertyServerPrxHelper.checkedCast(prx);
        } catch (Ice.Exception e) {
            Toast.makeText(this, getString(R.string.ps_unreachable), Toast.LENGTH_LONG).show();
        }
    }
}
