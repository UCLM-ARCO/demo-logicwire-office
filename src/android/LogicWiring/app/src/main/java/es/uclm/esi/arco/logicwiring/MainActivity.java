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
import java.util.List;
import java.util.concurrent.ExecutionException;

import DUO.IDM.WiringServicePrx;
import DUO.IDM.WiringServicePrxHelper;
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
    public List<String> consumers;

    private WiringServicePrx _wiringService;

    public ThingObject(String idm_addr, Type type, JSONObject props, WiringServicePrx dws /*, ObjectPrx router, PropertyServerPrx ps*/)
            throws JSONException {

        this.type = type;
        this.addr = idm_addr;
        consumers = new ArrayList<>();
        this._wiringService = dws;

        // set icon (names from Font Awesome)
        HashMap<String, Integer> icons = new HashMap<>();
        icons.put("hand-pointer-o", R.drawable.ic_touch_app_black_48dp_blue);
        icons.put("lightbulb-o", R.drawable.ic_wb_incandescent_black_48dp_orange);
        icons.put("dot-circle-o", R.drawable.ic_radio_button_checked_black_24dp_green);

        String icon_name = "hand-pointer-o";
        if (props.has("icon"))
            icon_name = props.getString("icon");
        else if (this.type == Type.RECEIVER)
            icon_name = "lightbulb-o";

        this.icon = icons.get(icon_name);
        if (this.icon == null)
            this.icon = R.drawable.ic_touch_app_black_48dp_blue;

        // set name
        this.name = idm_addr;
        if (props.has("name"))
            this.name = props.getString("name");
    }

    public void addConsumer(ThingObject consumer) {
        _wiringService.begin_addObserver(addr, consumer.addr);
        consumers.add(consumer.addr);
    }

    public void removeConsumer(ThingObject consumer) {
        _wiringService.begin_removeObserver(addr, consumer.addr);
        consumers.remove(consumer.addr);
    }
}

class ThingView implements View.OnLongClickListener, View.OnDragListener, PopupMenu.OnMenuItemClickListener {

    private MainActivity _activity;
    private ViewGroup _parent;
    private ThingObject _thingObject;
    private boolean _isClone;

    private ThingObject _consumer;
    private View _view;

    public ThingView(MainActivity activity, ViewGroup parent, ThingObject thingObject) {
        this(activity, parent, thingObject, false);
    }

    public ThingView(MainActivity activity, ViewGroup parent, ThingObject thingObject, boolean isClone) {
        _activity = activity;
        _parent = parent;
        _thingObject = thingObject;
        _isClone = isClone;

        // load card from XML layout
        LayoutInflater vi;
        vi = LayoutInflater.from(activity);
        _view = vi.inflate(R.layout.linked_thing_item, null);
        _parent.addView(_view);

        updateContent();
    }

    public void updateContent() {
        // setup card
        TextView tv = (TextView) _view.findViewById(R.id.SrcThingText);
        tv.setText(_thingObject.name);
        tv.setCompoundDrawablesRelativeWithIntrinsicBounds(0, _thingObject.icon, 0, 0);

        // change overall color of clones
        if (_isClone) {
            int color = _view.getResources().getColor(R.color.thing_linked_clone_background);
            View v = _view.findViewById(R.id.wholeThingHolder);
            v.setBackgroundColor(color);
        }

        // append linked view (if any)
        View holder = _view.findViewById(R.id.LinkedThingHolder);
        if (_consumer != null) {
            GridLayout.LayoutParams lp = (GridLayout.LayoutParams) _view.getLayoutParams();
            lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 2);
            _view.setLayoutParams(lp);

            holder.setVisibility(View.VISIBLE);

            TextView dst_tv = (TextView) _view.findViewById(R.id.DstThingText);
            dst_tv.setText(_consumer.name);
            dst_tv.setCompoundDrawablesRelativeWithIntrinsicBounds(0, _consumer.icon, 0, 0);
        }
        else {
            holder.setVisibility(View.GONE);
        }

        // connect callbacks
        if (_isClone || _thingObject.type == ThingObject.Type.RECEIVER)
            tv.setOnLongClickListener(this);

        if (! _isClone)
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
        if (! _isClone || _consumer == null)
            return true;

        Toast.makeText(_view.getContext(), "Removing " + _consumer.name +
                " as observer of " + _thingObject.name,
                Toast.LENGTH_SHORT).show();

        setConsumer(null, false);
        _parent.removeView(_view);
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

                // set dst as consumer of src and update views
                createLinkedClone(src, false);
                return true;
            }

            case DragEvent.ACTION_DRAG_ENDED: {
                return true;
            }
        }

        return false;
    }

    public void createLinkedClone(ThingObject consumer, boolean onlyLocal) {
        if (_thingObject.consumers.contains(consumer.addr)) {
            if (! onlyLocal)
                Toast.makeText(_activity, R.string.already_added_consumer,
                        Toast.LENGTH_SHORT).show();
            return;
        }

        if (! onlyLocal)
            Toast.makeText(_activity, "Setting " + consumer.name + " as observer of " + _thingObject.name,
                    Toast.LENGTH_SHORT).show();
        ThingView v = new ThingView(_activity, _parent, _thingObject, true);
        v.setConsumer(consumer, onlyLocal);
    }

    public ThingObject getThingObject() {
        return _thingObject;
    }

    public void setConsumer(ThingObject consumer, boolean onlyLocal) {
        // perform remote invocations (of course, if not only local)
        if (consumer != null) {
            if (onlyLocal)
                _thingObject.consumers.add(consumer.addr);
            else
                _thingObject.addConsumer(consumer);
        }
        else if (_consumer != null) {
            if (onlyLocal)
                _thingObject.consumers.remove(_consumer.addr);
            else
                _thingObject.removeConsumer(_consumer);
        }

        // update local view
        _consumer = consumer;
        updateContent();
    }
}

public class MainActivity extends AppCompatActivity {

    public static final String PREF_PROPERTY_SERVER = "pref_property_server";
    public static final String PREF_IDM_ROUTER = "pref_idm_router";
    public static final String PREF_WIRING_SERVICE = "pref_wiring_service";

    enum Action {
        NONE,
        FETCH_CONFIG,
        ADD_THING,
    }

    private Action _current_action = Action.NONE;
    private HashMap<String, ThingView> _thingViews;
    private Ice.Communicator _ic;
    private PropertyServerPrx _ps;
    private WiringServicePrx _dws;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        Byte.parseByte("AA", 16);


        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String idm_str_proxy = sharedPref.getString(PREF_IDM_ROUTER, "");
        _thingViews = new HashMap<>();

        if (idm_str_proxy.isEmpty())
            setContentView(R.layout.welcome_activity);

        else {
            setContentView(R.layout.things_activity);

            createCommunicator();
            createProxies();
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

            case R.id.action_load_settings:
                onStartClicked(null);
                return true;

            case R.id.action_edit_settings:
                Intent prefs = new Intent(this, SettingsActivity.class);
                startActivity(prefs);
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

        // Remove UTF-16 BOM (if present)
        if (content.startsWith("\uFEFF")) {
            content = content.substring(1);
        }

        switch (_current_action) {
            case ADD_THING:
                try {
                    addThingFromAddr(content);
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

    private void addThingFromAddr(String addr) throws JSONException {
        if (addr != null)
            addr = addr.trim().replace(":", "");

        if (_ps == null) {
            Toast.makeText(this, getString(R.string.ps_unreachable), Toast.LENGTH_LONG).show();
            return;
        }

        if (addr == null || addr.startsWith("http://")) {
            Toast.makeText(this, getResources().getText(R.string.error_invalid_addr)
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

        if (_thingViews.containsKey(addr)) {
            Toast.makeText(this, getResources().getText(R.string.error_repeated_addr)
                    + ": " + addr, Toast.LENGTH_SHORT).show();
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
        ThingObject t = new ThingObject(addr, type, props, _dws);

        ThingView v = new ThingView(this, holder, t);
        _thingViews.put(addr, v);

        // restore linked observer
        if (props.has("observers")) {
            System.out.println("HAS OBSERVERS");
            ArrayList<String> observers = JSONArrayToList(props.getJSONArray("observers"));

            for (String observer : observers) {
                if (observer.equals(addr)) {
                    System.out.println("Invalid observer: same os object id");
                    continue;
                }

                if (! _thingViews.containsKey(observer))
                    addThingFromAddr(observer);

                ThingObject consumer = _thingViews.get(observer).getThingObject();
                v.createLinkedClone(consumer, true);
            }
        }
    }

    private ArrayList<String> JSONArrayToList(JSONArray tags) throws JSONException {
        ArrayList<String> result = new ArrayList<>();
        for (int i=0; i<tags.length(); i++)
            result.add(tags.getString(i));
        return result;
    }

    private JSONObject getProperties(String addr) {
        try {
            String str_props = _ps.getJSON(addr.trim());
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
                    .putString(PREF_WIRING_SERVICE, jObject.getString("WiringService"))
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
        ObjectPrx prx;

        String dws_str_proxy = sharedPref.getString(PREF_WIRING_SERVICE, "");
        try {
            prx = _ic.stringToProxy(dws_str_proxy);
            _dws = WiringServicePrxHelper.checkedCast(prx);
        } catch (Ice.Exception e) {
            Toast.makeText(this, getString(R.string.dws_invalid_proxy) + ":" + dws_str_proxy,
                    Toast.LENGTH_LONG).show();
        }

        String ps_str_proxy = sharedPref.getString(PREF_PROPERTY_SERVER, "");
        try {
            prx = _ic.stringToProxy(ps_str_proxy);
            _ps = PropertyServerPrxHelper.checkedCast(prx);
        } catch (Ice.Exception e) {
            Toast.makeText(this, getString(R.string.ps_unreachable), Toast.LENGTH_LONG).show();
        }
    }
}
