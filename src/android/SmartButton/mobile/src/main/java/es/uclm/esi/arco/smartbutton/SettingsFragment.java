package es.uclm.esi.arco.smartbutton;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.widget.Toast;

public class SettingsFragment extends PreferenceFragment {

    private Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener
            = new Preference.OnPreferenceChangeListener() {

        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if (stringValue.isEmpty())
                stringValue = "<not set>";

            if (preference.getKey().equals(MainActivity.PREF_IDM_ADDRESS))
                if (! validateIDMAddress((String) value)) {
                    Toast.makeText(getActivity(), R.string.invalid_idm_address, Toast.LENGTH_SHORT).show();
                    return false;
                }

            preference.setSummary(stringValue);
            return true;
        }
    };

    private static boolean validateIDMAddress(String value) {
        if (value == null || value.isEmpty())
            return true;

        String[] fields = value.split(":");
        if (fields.length != 2)
            return false;

        for (String s: fields) {
            if (s.length() != 2)
                return false;
            try {
                Byte.parseByte(s, 16);
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return true;
    }

    private void bindPreferenceSummaryToValue(Preference preference) {
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        bindPreferenceSummaryToValue(findPreference(MainActivity.PREF_IDM_ROUTER));
        bindPreferenceSummaryToValue(findPreference(MainActivity.PREF_PROPERTY_SERVER));
        bindPreferenceSummaryToValue(findPreference(MainActivity.PREF_IDM_ADDRESS));
    }
}
