package es.uclm.esi.arco.smartbutton;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.wearable.view.FragmentGridPagerAdapter;
import android.support.wearable.view.GridViewPager;
import android.view.View;
import android.widget.ImageButton;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;

import java.util.concurrent.ExecutionException;

import IDM.NeighborDiscovery.Callback_Listener_adv;
import Ice.AsyncResult;
import Ice.Current;
import Ice.Identity;
import Ice.LocalException;
import Ice.ObjectAdapter;
import Ice.ObjectPrx;
import Ice.SystemException;

class CustomGridPageAdapter extends FragmentGridPagerAdapter {

    public CustomGridPageAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getFragment(int row, int col) {
        if (col == 0)
            return  new ButtonFragment();

        return new QRFragment();
    }

    @Override
    public int getRowCount() {
        return 1;
    }

    @Override
    public int getColumnCount(int i) {
        return 2;
    }
}

public class MainActivity extends Activity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        DataApi.DataListener {

    public static final String PREF_IDM_ROUTER = "pref_idm_router";
    public static final String PREF_OBSERVER = "pref_observer";
    public static final String PREF_IDM_ADDRESS = "pref_idm_address";

    private GoogleApiClient _client;

    private boolean _buttonState = false;
    private Ice.Communicator _ic;
    private IDM.NeighborDiscovery.ListenerPrx _router;
    private ObjectPrx _servant_prx;
    private ObjectAdapter _adapter;
    private DUO.IDM.IBool.WPrx _observer;
    private Identity _current_oid;
    private byte[] _myaddr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        System.out.println("WEAR: onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final GridViewPager pager = (GridViewPager) findViewById(R.id.pager);
        pager.setAdapter(new CustomGridPageAdapter(getFragmentManager()));

        _client = getGoogleApiClient();
        asyncIceSetup();
    }

    @Override
    protected void onResume() {
        System.out.println("WEAR: onResume");
        super.onResume();
        _client.connect();
    }

    @Override
    protected void onPause() {
        System.out.println("WEAR: onPause");

        super.onPause();
        Wearable.DataApi.removeListener(_client, this);
        _client.disconnect();
    }

    @Override
    protected void onDestroy() {
        System.out.println("WEAR: onDestroy");

        class MyAsyncTask extends AsyncTask<Void, Void, Void> {
            @Override
            protected Void doInBackground (Void... params){
                if (_adapter != null) {
                    _adapter.deactivate();
                    _adapter.destroy();
                    _adapter = null;
                }

                if (_ic != null) {
                    _ic.shutdown();
                    _ic.destroy();
                    _ic = null;
                }
                return null;
            }
        };

        try {
            new MyAsyncTask().execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        super.onDestroy();
    }

    public void onClick(View view) {
        System.out.println("WEAR: onClick");
        _buttonState = !_buttonState;

        // Set button image (images from: http://codepen.io/anon/pen/MyVaOG)
        ImageButton button = (ImageButton) findViewById(R.id.button);
        int icon = _buttonState ? R.drawable.button_on : R.drawable.button_off;
        button.setImageResource(icon);

        // Invoke observer
        if (_observer != null)
            _observer.begin_set(_buttonState, _myaddr);
    }

    private GoogleApiClient getGoogleApiClient() {
        System.out.println("WEAR: API create client");

        return new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    @Override
    public void onDataChanged(DataEventBuffer events) {
        System.out.println("WEAR: onDataChanged");

        for (DataEvent e: events) {
            if (e.getType() != DataEvent.TYPE_CHANGED)
                continue;

            DataItem i = e.getDataItem();
            if (! i.getUri().getPath().equals("/prefs"))
                continue;

            DataMap prefs = DataMapItem.fromDataItem(i).getDataMap();
            updatePreferences(prefs);
        }
    }

    private void updatePreferences(DataMap prefs) {
        System.out.println("WEAR: updating preferences...");

        PreferenceManager.getDefaultSharedPreferences(this)
                .edit()
                .putString(PREF_IDM_ADDRESS, prefs.getString(PREF_IDM_ADDRESS, ""))
                .putString(PREF_IDM_ROUTER, prefs.getString(PREF_IDM_ROUTER, ""))
                .commit();

        // register new servant and create new proxies
        class MyAsyncTask extends AsyncTask<Void, Void, Void> {
            @Override
            protected Void doInBackground(Void... params) {
                registerServant();
                createProxies();
                announceOnRouter();
                return null;
            }
        }
        try {
            new MyAsyncTask().execute().get();
        } catch (InterruptedException | ExecutionException e){
            e.printStackTrace();
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        System.out.println("WEAR: DataApi client onConnected");
        Wearable.DataApi.addListener(_client, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        System.out.println("WEAR: DataApi client onConnectionSuspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        System.out.println("WEAR: DataApi client onConnectionFailed");
    }

    private void asyncIceSetup() {
        System.out.println("WEAR: aysncIce setup");

        class MyTask extends AsyncTask<Void, Void, Void> {
            @Override
            protected Void doInBackground(Void... params) {
                createCommunicator();
                createObjectAdapter();
                registerServant();
                createProxies();
                announceOnRouter();

                return null;
            }
        }
        try {
            new MyTask().execute().get();
        } catch (InterruptedException | ExecutionException e){
            e.printStackTrace();
        }
    }

    private void createCommunicator() {
        final Ice.InitializationData idata = new Ice.InitializationData();
        idata.properties = Ice.Util.createProperties();
        idata.properties.setProperty("Ice.IPv6", "0");

        _ic = Ice.Util.initialize(idata);
    }

    private void createObjectAdapter() {
        _adapter = _ic.createObjectAdapterWithEndpoints("Adaper", "tcp -p 4455");
        _adapter.activate();
    }

    private void registerServant() {
        if (_current_oid != null) {
            _adapter.remove(_current_oid);
            _current_oid = null;
        }

        String oid = PreferenceManager
                .getDefaultSharedPreferences(this)
                .getString(PREF_IDM_ADDRESS, "");
        if (oid.isEmpty())
            return;

        DUO.IDM.Active.W servant = new ActiveWI();
        _current_oid = _ic.stringToIdentity(oid.replace(":", ""));
        _servant_prx = _adapter.add(servant, _current_oid);
        _myaddr = stringToAddr(oid);

        System.out.println("WEAR: Servant ready: '" + _servant_prx + "'");
    }

    private byte[] stringToAddr(String oid) {
        String[] fields = oid.split(":");
        byte[] retval = new byte[fields.length];
        for (int i=0; i<fields.length; i++)
            retval[i] = Byte.parseByte(fields[i], 16);
        return retval;
    }

    private void createProxies() {
        // create Router's proxy
        String str_proxy = PreferenceManager.getDefaultSharedPreferences(this)
                .getString(PREF_IDM_ROUTER, "");

        if (str_proxy.isEmpty()) {
            System.out.println("WEAR: no IDM router defined, almost nothing will work!");
            return;
        }

        ObjectPrx prx = _ic.stringToProxy(str_proxy);
        _router = IDM.NeighborDiscovery.ListenerPrxHelper.uncheckedCast(prx);

        // create Observer's proxy (if defined)
        String observer_addr = PreferenceManager.getDefaultSharedPreferences(this)
                .getString(PREF_OBSERVER, "");
        if (observer_addr.isEmpty()) {
            System.out.println("WEAR: observer is not defined!");
            return;
        }

        createObserverProxyFromAddr(observer_addr);
    }

    void createObserverProxyFromAddr(String addr) {
        if (_router == null)
            return;

        if (addr.isEmpty()) {
            _observer = null;
            return;
        }

        ObjectPrx prx = _router.ice_identity(_ic.stringToIdentity(addr));
        _observer = DUO.IDM.IBool.WPrxHelper.uncheckedCast(prx);
    }

    private void announceOnRouter() {
        System.out.println("WEAR: announceOnRouter '" + _router + "'");
        if (_router == null || _servant_prx == null) {
            System.out.println("  - router or servant not deinfed!");
            return;
        }

        _router.begin_adv(_servant_prx.toString());
    }

    class ActiveWI extends DUO.IDM.Active._WDisp {

        @Override
        public void setObserver(byte[] observer, Current __current) {
            boolean nullObserver = true;
            for (byte b: observer)
                if (b != 0) {
                    nullObserver = false;
                    break;
                }

            String str_observer = "";
            if (! nullObserver)
                str_observer = addrToString(observer);

            System.out.println("WEAR: setObserver to '" + str_observer + "'");

            PreferenceManager.getDefaultSharedPreferences(MainActivity.this)
                    .edit()
                    .putString(PREF_OBSERVER, str_observer)
                    .apply();
            createObserverProxyFromAddr(str_observer);
        }

        private String addrToString(byte[] observer) {
            String retval = "";
            for (byte b: observer) {
                retval += String.format("%02x", b & 0xff);
            }
            return retval;
        }
    }
}
