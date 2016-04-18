package es.uclm.esi.arco.smartbutton;

import android.app.Fragment;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

public class QRFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View retval = inflater.inflate(R.layout.rect_fragment_qr, container, false);
        final ImageView image = (ImageView) retval.findViewById(R.id.qr_image);
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

        prefs.registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                System.out.println("WEAR: pref changed: " + key);

                if (! key.equals(MainActivity.PREF_IDM_ADDRESS))
                    return;

                String addr = prefs.getString(MainActivity.PREF_IDM_ADDRESS, "");
                updateImageWithQR(image, addr);
            }
        });

        String addr = prefs.getString(MainActivity.PREF_IDM_ADDRESS, "");
        updateImageWithQR(image, addr);
        return retval;
    }

    private void updateImageWithQR(ImageView image, String addr) {
        final int width = 100;
        final int height = 100;

        QRCodeWriter wr = new QRCodeWriter();
        try {
            if (addr.isEmpty()) {
                System.out.println("WEAR: empty address!");
                addr = "unknown";
            }

            BitMatrix matrix = wr.encode(addr, BarcodeFormat.QR_CODE, width, height);
            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            for (int x=0; x<width; x++){
                for (int y=0; y<height; y++){
                    bmp.setPixel(x, y, matrix.get(x,y) ? Color.BLACK : Color.WHITE);
                }
            }

            image.setImageBitmap(bmp);

        } catch (WriterException e) {
            e.printStackTrace();
        }
    }


}
