package es.uclm.esi.arco.logicwiring;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Context;
import android.content.SharedPreferences;
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
import android.widget.GridView;
import android.widget.TextView;

import com.google.zxing.integration.android.IntentIntegrator;

import java.util.Objects;

class Thing {
    public enum Type {
        SENDER,
        RECEIVER
    }

    public Type type;
    public String addr;
    public Thing observable;

    public Thing(String idm_addr, Type type) {
        this.type = type;
        this.addr = idm_addr;
        this.observable = null;
    }

    public void setObservable(Thing observable) {
        this.observable = observable;
    }
}

//class ThingViewAdapter
//        extends ArrayAdapter<Thing>
//        implements AdapterView.OnItemLongClickListener,
//        View.OnDragListener {
//
//    private Map<String, Thing> _things;
//
//    public ThingViewAdapter(Context context, int resource, Map<String, Thing> things) {
//        super(context, resource);
//        this._things = things;
//    }
//
//    @Override
//    public View getView(int position, View convertView, ViewGroup parent) {
//        View v = convertView;
//        Thing t = getItem(position);
//
//        if (v == null) {
//            // view is not recycled
//            LayoutInflater vi;
//            vi = LayoutInflater.from(getContext());
//            v = vi.inflate(R.layout.simple_thing_item, null);
//        }
//
//        TextView tv = (TextView) v.findViewById(R.id.ThingText);
//        if (tv != null) {
//            tv.setText(t.addr);
//
//            int icon = R.drawable.ic_wb_incandescent_black_48dp_orange;
//            if (t.type == Thing.Type.SENDER)
//                icon = R.drawable.ic_touch_app_black_48dp_blue;
//
//            tv.setCompoundDrawablesRelativeWithIntrinsicBounds(0, icon, 0, 0);
//        }
//
//        v.setOnDragListener(this);
//        v.setTag(t.addr);
//        return v;
//    }
//
//    @Override
//    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
//        Thing t = getItem(position);
//
//        ClipData.Item item = new ClipData.Item(t.addr);
//        ClipData data = new ClipData(t.addr, new String[]{ClipDescription.MIMETYPE_TEXT_PLAIN}, item);
//        View.DragShadowBuilder shadow = new View.DragShadowBuilder(view);
//        view.startDrag(data, shadow, t, 0);
//
//        return true;
//    }
//
//    @Override
//    public boolean onDrag(View v, DragEvent event) {
//        final int action = event.getAction();
//
//        switch (action) {
//            case DragEvent.ACTION_DRAG_STARTED: {
//                Thing src = (Thing) event.getLocalState();
//                return event.getClipDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN);
//            }
//
//            case DragEvent.ACTION_DROP: {
//                Thing src = (Thing) event.getLocalState();
//                String dst_addr = (String) v.getTag();
//
//                // drop on same item, discard
//                if (Objects.equals(src.addr, dst_addr))
//                    return false;
//
//                // drop on non RECEIVER, discard
//                Thing dst = _things.get(dst_addr);
//                if (dst.type != Thing.Type.RECEIVER) {
//                    return false;
//                }
//
//                // set dst as observable for src
//                dst.setObservable(src);
//
//                // we need to:
//                //  - redraw dst view
//                //  - remove src view
//                ((MainActivity) getContext()).onChanged();
//
//                System.out.println("Drop of item " + src.addr + " on " + dst.addr);
//                return true;
//            }
//
//            case DragEvent.ACTION_DRAG_ENDED: {
//                Thing src = (Thing) event.getLocalState();
//                String my_addr = (String) v.getTag();
//                if (!Objects.equals(src.addr, my_addr))
//                    return false;
//
//                remove(src);
//                return true;
//            }
//        }
//
//        return false;
//    }
//}

class ThingView implements View.OnLongClickListener, View.OnDragListener {

    private Thing _thing;
    private View _view;
    private ViewGroup _parent;

    public ThingView(Context context, ViewGroup parent, Thing thing) {
        _thing = thing;
        _parent = parent;

        // load card from XML layout
        LayoutInflater vi;
        vi = LayoutInflater.from(context);
        _view = vi.inflate(R.layout.simple_thing_item, null);
        _parent.addView(_view);

        updateContent();
    }

    private void updateContent() {
        // set ADDR for text
        TextView tv = (TextView) _view.findViewById(R.id.ThingText);

        String content = _thing.addr;
        if (_thing.observable != null) {
            content += "+";

            GridLayout.LayoutParams lp = (GridLayout.LayoutParams) _view.getLayoutParams();
            lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 2);
            _view.setLayoutParams(lp);
        }
        tv.setText(content);

        // set icon by TYPE
        int icon = R.drawable.ic_wb_incandescent_black_48dp_orange;
        if (_thing.type == Thing.Type.SENDER)
            icon = R.drawable.ic_touch_app_black_48dp_blue;
        tv.setCompoundDrawablesRelativeWithIntrinsicBounds(0, icon, 0, 0);

        // connect callbacks
        if (_thing.type == Thing.Type.SENDER)
            tv.setOnLongClickListener(this);
        if (_thing.type == Thing.Type.RECEIVER)
            tv.setOnDragListener(this);
    }

    @Override
    public boolean onLongClick(View v) {
        ClipData.Item item = new ClipData.Item(_thing.addr);
        ClipData data = new ClipData(_thing.addr, new String[]{ClipDescription.MIMETYPE_TEXT_PLAIN}, item);
        View.DragShadowBuilder shadow = new View.DragShadowBuilder(v);
        v.startDrag(data, shadow, _thing, 0);
        return true;
    }

    @Override
    public boolean onDrag(View v, DragEvent event) {
        final int action = event.getAction();

        switch (action) {
            case DragEvent.ACTION_DRAG_STARTED: {
                return event.getClipDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN);
            }

            case DragEvent.ACTION_DROP: {
                // drop on non RECEIVER, discard
                if (_thing.type != Thing.Type.RECEIVER)
                    return false;

                Thing src = (Thing) event.getLocalState();

                // drop on same item, discard
                if (Objects.equals(src.addr, _thing.addr)) {
                    return false;
                }

                // set dst as observable for src and update views
                System.out.println("Drop of item " + src.addr + " on " + _thing.addr);
                _thing.setObservable(src);
                updateContent();

                return true;
            }

            case DragEvent.ACTION_DRAG_ENDED: {
                Thing src = (Thing) event.getLocalState();
                if (!Objects.equals(src.addr, _thing.addr))
                    return false;

//                remove(src);
                return true;
            }
        }

        return false;
    }
}

public class MainActivity extends AppCompatActivity {

    enum Action {
//        NONE,
        FETCH_CONFIG,
        ADD_THING,
    }

    private final static String PREF_IDM_ROUTER = "pref_idm_router";
//    private Action _current_action = Action.NONE;
//    private Ice.Communicator _ic;
//    private ObjectPrx _router;

//    private Map<String, Thing> _things;

//    public Thing getThing(String addr) {
//        return _things.get(addr);
//    }
//
//    public void onChanged() {
//        _receiver_things.notifyDataSetChanged();
//        _sender_things.notifyDataSetChanged();
//    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String idm_str_proxy = sharedPref.getString(PREF_IDM_ROUTER, "");
        // _things = new HashMap<>();

        if (idm_str_proxy.isEmpty())
            setContentView(R.layout.welcome_activity);

        else {
            setContentView(R.layout.things_activity);

            // FIXME: just for debug
            {
                GridLayout receivers_view = (GridLayout) findViewById(R.id.receivers_holder);
                for (int i=0; i<20; i++) {
                    String addr = String.format("10:%02d", i);
                    Thing t = new Thing(addr, Thing.Type.RECEIVER);
                    new ThingView(this, receivers_view, t);
                }

                GridLayout senders_view = (GridLayout) findViewById(R.id.senders_holder);
                for (int i=0; i<20; i++) {
                    String addr = String.format("11:%02d", i);
                    Thing t = new Thing(addr, Thing.Type.SENDER);
                    new ThingView(this, senders_view, t);
                }
            }

//          createCommunicator();
//          createIDMRouter(idm_str_proxy);
//          for (int i=0; i<10; i++) {
//              addThing("10:" + i);
//          }
        }
    }

//    private void addThing(GridLayout parent, String addr, Thing.Type type) {
//        final Thing t = new Thing(addr, type);
//        _things.put(addr, t);
//
//        LayoutInflater vi;
//        vi = LayoutInflater.from(this);
//        View v = vi.inflate(R.layout.simple_thing_item, null);
//
//        v.setOnLongClickListener(new View.OnLongClickListener() {
//            @Override
//            public boolean onLongClick(View v) {

//            }
//        });
//
//        v.setOnDragListener(new View.OnDragListener() {
//            @Override
//            public boolean onDrag(View v, DragEvent event) {
//
//        });
//
//        v.setTag(t.addr);
//
//        TextView tv = (TextView) v.findViewById(R.id.ThingText);
//        tv.setText(addr);
//
//        int icon = R.drawable.ic_wb_incandescent_black_48dp_orange;
//        if (t.type == Thing.Type.SENDER)
//            icon = R.drawable.ic_touch_app_black_48dp_blue;
//        tv.setCompoundDrawablesRelativeWithIntrinsicBounds(0, icon, 0, 0);
//
//        parent.addView(v);
//    }

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
//                _current_action = Action.ADD_THING;
                getQRScannerIntentIntegrator().initiateScan();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void onStartClicked(View view) {
//        _current_action = Action.FETCH_CONFIG;
        getQRScannerIntentIntegrator().initiateScan();
    }

    private IntentIntegrator getQRScannerIntentIntegrator() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setCaptureActivity(PortraitCaptureActivity.class);
        integrator.setPrompt((String) getResources().getText(R.string.scan_qr_prompt));
        integrator.setOrientationLocked(false);
        return integrator;
    }

//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
//        if (result == null) {
//            super.onActivityResult(requestCode, resultCode, data);
//            return;
//        }
//
//        String content = result.getContents();
//        switch (_current_action) {
//            case ADD_THING:
//                addThing(content);
//                break;
//
//            case FETCH_CONFIG:
//                new RetrieveJSONConfig().execute(content);
//                break;
//        }
//    }
//
//    private void addThing(String content) {
//        // FIXME: check if content is a valid IDM address
//        if (content.startsWith("http://")) {
//            Toast.makeText(this, getResources().getText(R.string.error_invalid_addr)
//                    + ": " + content, Toast.LENGTH_LONG).show();
//            return;
//        }
//
//        // FIXME: check that Thing.addr is not already added
//        _receiver_things.add(new Thing(content, Thing.Type.RECEIVER));
//        _sender_things.add(new Thing(content, Thing.Type.SENDER));
//    }
//
//    private class RetrieveJSONConfig extends AsyncTask<String, Void, Void> {
//
//        private String errorMessage = "";
//        private StringBuilder content = new StringBuilder();
//
//        @Override
//        protected Void doInBackground(String... urls) {
//            String url = null;
//
//            try {
//                // open stream
//                if (urls.length < 1) {
//                    errorMessage = (String) getResources().getText(R.string.error_invalid_usage);
//                    return null;
//                }
//
//                url = urls[0];
//                URL site = new URL(url);
//                BufferedReader in = new BufferedReader(new InputStreamReader(site.openStream()));
//
//                // read data
//                String line = null;
//                while ((line = in.readLine()) != null)
//                    content.append(line);
//
//                // no error reported
//                return null;
//
//            } catch (MalformedURLException e) {
//                errorMessage = (String) getResources().getText(R.string.error_invalid_contents) + ": " + url;
//
//            } catch (IOException e) {
//                errorMessage = (String) getResources().getText(R.string.error_cant_connect) + ": " + url;
//            }
//
//            return null;
//        }
//
//        @Override
//        protected void onPostExecute(Void aVoid) {
//            super.onPostExecute(aVoid);
//            if (! errorMessage.isEmpty()) {
//                Toast.makeText(getApplicationContext(), errorMessage, Toast.LENGTH_LONG).show();
//                return;
//            }
//
//            parseJSONConfig(content.toString());
//        }
//    }
//
//    protected void parseJSONConfig(String json) {
//        try {
//            JSONObject jObject = new JSONObject(json);
//
//            String router = jObject.getString("IDMRouter");
//            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
//            sharedPref.edit().putString(PREF_IDM_ROUTER, router).apply();
//
//            // restart activity
//            finish();
//            startActivity(getIntent());
//
//        } catch (JSONException e) {
//            String msg = (String) getResources().getText(R.string.error_invalid_retrieved_config);
//            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
//        }
//    }
//
//    private void createCommunicator() {
//        Ice.InitializationData idata = new Ice.InitializationData();
//        idata.properties = Ice.Util.createProperties();
//
//        _ic = Ice.Util.initialize(idata);
//    }
//
//    private void createIDMRouter(String idm_str_proxy) {
////        _router = _ic.stringToProxy(idm_str_proxy);
//    }
}
