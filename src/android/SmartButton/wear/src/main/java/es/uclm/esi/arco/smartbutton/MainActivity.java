package es.uclm.esi.arco.smartbutton;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.wearable.view.FragmentGridPagerAdapter;
import android.support.wearable.view.GridViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import java.util.concurrent.ExecutionException;

import Ice.Current;
import Ice.ObjectAdapter;
import Ice.ObjectPrx;

class CustomGridPageAdapter extends FragmentGridPagerAdapter {

    private final Context context;

    public CustomGridPageAdapter(Context ctx, FragmentManager fm) {
        super(fm);
        context = ctx;
    }

    @Override
    public Fragment getFragment(int row, int col) {
        if (col == 0)
            return  new ButtonFragment();

        String address = PreferenceManager
                .getDefaultSharedPreferences(context)
                .getString(MainActivity.PREF_IDM_ADDRESS, "");

        Bundle args = new Bundle();
        args.putString("address", address);
        QRFragment f = new QRFragment();
        f.setArguments(args);
        return f;
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


FIXME: implement DataListener: http://developer.android.com/training/wearables/data-layer/data-items.html

public class MainActivity extends Activity {

    public static final String PREF_IDM_ROUTER = "pref_idm_router";
    public static final String PREF_OBSERVER = "pref_observer";
    public static final String PREF_IDM_ADDRESS = "pref_idm_address";

    private boolean _buttonState = false;
    private Ice.Communicator _ic;
    private IDM.NeighborDiscovery.ListenerPrx _router;
    private ObjectPrx _servant_prx;
    private ObjectAdapter _adapter;
    private DUO.IDM.IBool.WPrx _observer;
    private byte[] _myaddr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final GridViewPager pager = (GridViewPager) findViewById(R.id.pager);
        pager.setAdapter(new CustomGridPageAdapter(this, getFragmentManager()));

        asyncIceSetup();
    }

    public void onClick(View view) {
        _buttonState = !_buttonState;

        // Set button image (images from: http://codepen.io/anon/pen/MyVaOG)
        ImageButton button = (ImageButton) findViewById(R.id.button);
        int icon = _buttonState ? R.drawable.button_on : R.drawable.button_off;
        button.setImageResource(icon);

        // Invoke observer
        if (_observer != null)
            _observer.begin_set(_buttonState, _myaddr);
    }

    private void asyncIceSetup() {
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
        String oid = PreferenceManager.getDefaultSharedPreferences(this)
                .getString(PREF_IDM_ADDRESS, "30:01");
        _myaddr = stringToAddr(oid);

        DUO.IDM.Active.W servant = new ActiveWI();
        _servant_prx = _adapter.add(servant, _ic.stringToIdentity(oid.replace(":", "")));
        System.out.println("Servant ready: '" + _servant_prx + "'");
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
            System.out.println("ERROR: no IDM router defined, nothing will work!");
            return;
        }

        ObjectPrx prx = _ic.stringToProxy(str_proxy);
        _router = IDM.NeighborDiscovery.ListenerPrxHelper.uncheckedCast(prx);

        // create Observer's proxy (if defined)
        String observer_addr = PreferenceManager.getDefaultSharedPreferences(this)
                .getString(PREF_OBSERVER, "");
        if (observer_addr.isEmpty()) {
            System.out.println("WARN: observer is not defined!");
            return;
        }

        createObserverProxyFromAddr(observer_addr);
    }

    void createObserverProxyFromAddr(String addr) {
        if (_router == null)
            return;

        ObjectPrx prx = _router.ice_identity(_ic.stringToIdentity(addr));
        _observer = DUO.IDM.IBool.WPrxHelper.uncheckedCast(prx);
    }

    private void announceOnRouter() {
        if (_router == null)
            return;

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

            System.out.println("setObserver to '" + str_observer + "'");

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
