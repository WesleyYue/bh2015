package com.wesleyyue.battlehack2015;

import android.content.Intent;
import android.graphics.Point;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;

import com.braintreepayments.api.dropin.BraintreePaymentActivity;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.TextHttpResponseHandler;
import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import org.apache.http.Header;

import java.util.Date;
import java.util.HashMap;
import java.util.List;


public class MapsActivity extends FragmentActivity implements GoogleMap.OnMarkerClickListener, GoogleMap.OnMapClickListener{

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private String braintree_token;
    private static String server_domain = "http://mighty-retreat-5059.herokuapp.com/";
    private static final int BRAINTREE_REQUEST_CODE = 100;
    private View BottomSlideOut;
    private static boolean peeking_info_pane = false;
    private static boolean expanded_info_pane = false;
    private static final int info_pane_height = 1100;
    private static final int peeking_height = 300;
    private static final int expanded_height = info_pane_height - peeking_height;
    private boolean in_rent_mode = false;

    ParseObject currently_selected_bike;

    HashMap<String, ParseObject> marker_info;

    TextView hourlyRate;
    TextView review1;
    TextView review2;
    TextView review3;
    TextView review1author;
    TextView review2author;
    TextView review3author;

    Button rentbtn;
    RatingBar ratingBar;
    LinearLayout incurred_charge;

    long charge;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);




        hourlyRate = (TextView)findViewById(R.id.hourlyRate);
        review1 = (TextView)findViewById(R.id.review1);
        review2 = (TextView)findViewById(R.id.review2);
        review3 = (TextView)findViewById(R.id.review3);
        review1author = (TextView)findViewById(R.id.review1author);
        review2author = (TextView)findViewById(R.id.review2author);
        review3author = (TextView)findViewById(R.id.review3author);
        rentbtn = (Button)findViewById(R.id.rentBtn);
        ratingBar = (RatingBar)findViewById(R.id.ratingBar);
        incurred_charge = (LinearLayout) findViewById(R.id.incurred_charge);



        // Enable Local Datastore.
        Parse.enableLocalDatastore(this);
        Parse.initialize(this, "m62ASk25Hb1HaaMBWUQ6XeI7VKcbTn1A0g1KDtZp", "DaAlxgusMVeMn6fo05UMVF9lTwlZy2VCXa59BMgB");

        marker_info = new HashMap<String, ParseObject>();

        setUpMapIfNeeded();

        getToken();
        mMap.setOnMarkerClickListener(this);
        mMap.setOnMapClickListener(this);


        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int window_width = size.x;
        int window_height = size.y;


        BottomSlideOut = findViewById(R.id.BottomSlideOut);
        BottomSlideOut.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        Log.d("bottomHeight", "height = " + window_height);
        BottomSlideOut.setTranslationY(window_height);
        BottomSlideOut.animate().setInterpolator(new DecelerateInterpolator()).setDuration(200);

    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }

        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(43.661036, -79.391857), 13f), 4000, null);

    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p/>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
//        mMap.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("Marker"));



        ParseQuery<ParseObject> query = ParseQuery.getQuery("Bikes");
//        query.whereEqualTo("playerName", "Dan Stemkoski");
        query.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> scoreList, ParseException e) {
                if (e == null) {
                    for (ParseObject object : scoreList) {
                        Log.d("score", "here: " + object.getDouble("lon"));
                        double lon = object.getDouble("lon");
                        double lat = object.getDouble("lat");

//                        Log.d("hmtesting", object.getObjectId());

                        mMap.addMarker(new MarkerOptions().position(new LatLng(lon, lat)).title(object.getObjectId()));
                        marker_info.put(object.getObjectId(), object);
                    }
                } else {
                    Log.d("score", "Error: " + e.getMessage());
                }
            }
        });
    }

    // Braintree
    private void getToken() {
        AsyncHttpClient client = new AsyncHttpClient();
        client.get(server_domain + "/client_token", new TextHttpResponseHandler() {
            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                // TODO: failure handling
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, String responseString) {
                braintree_token = responseString;
            }
        });
    }

    public void onBraintreeSubmit(View v) {
        Intent intent = new Intent(this, BraintreePaymentActivity.class);
        intent.putExtra(BraintreePaymentActivity.EXTRA_CLIENT_TOKEN, braintree_token);
        startActivityForResult(intent, BRAINTREE_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == BRAINTREE_REQUEST_CODE) {
            if (resultCode == BraintreePaymentActivity.RESULT_OK) {
                String paymentMethodNonce = data.getStringExtra(BraintreePaymentActivity.EXTRA_PAYMENT_METHOD_NONCE);
                postNonceToServer(paymentMethodNonce);
            }
        }
    }

    private void postNonceToServer(String paymentMethodNonce) {
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        params.put("payment_method_nonce", paymentMethodNonce);
        params.put("charge", charge);
        client.post(server_domain + "/payment-methods", params,
                new AsyncHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                        // TODO: start renting view
                        othernfc();
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                        try {
                            throw error;
                        } catch (Throwable throwable) {
                            throwable.printStackTrace();
                        }
                        Log.d("postNonce", "failed");
                    }
                }
        );
    }
    // End of braintree code

    public void startTheActivityInClousure(Date time_created){

        Intent intent = new Intent(this, BraintreePaymentActivity.class);
        intent.putExtra(BraintreePaymentActivity.EXTRA_CLIENT_TOKEN, braintree_token);

        Date now = new Date();
        long difference = now.getTime() - time_created.getTime();
        charge = difference * currently_selected_bike.getLong("rate");

        startActivityForResult(intent, BRAINTREE_REQUEST_CODE);

    }

    public void nfcActivity (View view) {
        if (in_rent_mode){
            in_rent_mode = false;

            ParseQuery<ParseObject> query = ParseQuery.getQuery("bike_rides");
            query.whereEqualTo("user_id", 1);
            query.findInBackground(new FindCallback<ParseObject>() {
                public void done(List<ParseObject> scoreList, ParseException e) {
                    if (e == null) {
                        for (ParseObject object : scoreList) {
                            Date time_created = object.getCreatedAt();
                            startTheActivityInClousure(time_created);
                            try {
                                object.delete();
                            } catch (ParseException e1) {
                                e1.printStackTrace();
                            }
                        }
                    } else {
                        Log.d("score", "Error: " + e.getMessage());
                    }
                }
            });


        } else {
            ParseObject bike_rides = new ParseObject("bike_rides");
            bike_rides.put("user_id", 1);
            bike_rides.saveInBackground();
            in_rent_mode = true;
            rentbtn.setText("End rental");
            Intent intent = new Intent(this, NFCActivity.class);
            Bundle b = new Bundle();
            b.putInt("lock", 0); //Your id
            intent.putExtras(b); //Put your id to your next Intent
            refreshUIForRentMode();
            startActivity(intent);
        }


    }

    private void refreshUIForRentMode() {
        rentbtn.setText("End rental");
        ratingBar.setVisibility(View.GONE);
        incurred_charge.setVisibility(View.VISIBLE);
//        BottomSlideOut.setBackgroundResource(R.drawable.bg_shadow_red);
//        rentbtn.setBackgroundColor(0xF44336);


    }
    public void othernfc() {
        Intent intent = new Intent(this, NFCActivity.class);
        Bundle b = new Bundle();
        b.putInt("lock", 1); //Your id
        intent.putExtras(b); //Put your id to your next Intent
        startActivity(intent);
        rentbtn.setText("Rent");
//        rentbtn.setBackgroundColor(0xd3d3d3);
        incurred_charge.setVisibility(View.GONE);
        ratingBar.setVisibility(View.VISIBLE);
//        BottomSlideOut.setBackgroundResource(R.drawable.bg_shadow);
    }


    @Override
    public boolean onMarkerClick(Marker marker) {
        currently_selected_bike = marker_info.get(marker.getTitle());
        Log.d("hmtesting", currently_selected_bike.getObjectId());

        hourlyRate.setText("$" + currently_selected_bike.getNumber("rate") + "/hr");
        review1.setText(currently_selected_bike.getString("review1"));
        review2.setText(currently_selected_bike.getString("review2"));
        review3.setText(currently_selected_bike.getString("review3"));
        review1author.setText(currently_selected_bike.getString("author1"));
        review2author.setText(currently_selected_bike.getString("author2"));
        review3author.setText(currently_selected_bike.getString("author3"));


        if (!peeking_info_pane){
            BottomSlideOut.animate().translationYBy(-peeking_height);
            peeking_info_pane = true;
        }

        return true;
    }

    @Override
    public void onMapClick(LatLng latLng) {
        if (peeking_info_pane && expanded_info_pane) {
            BottomSlideOut.animate().translationYBy(info_pane_height);
            peeking_info_pane = false;
            expanded_info_pane = false;
        } else if (peeking_info_pane) {
            BottomSlideOut.animate().translationYBy(peeking_height);
            peeking_info_pane = false;
        }

        currently_selected_bike = null;
    }

    public void toggleExpandInfoPane(View view) {
        if(expanded_info_pane){
            BottomSlideOut.animate().translationYBy(expanded_height);
        }else{
            BottomSlideOut.animate().translationYBy(-expanded_height);
        }

        expanded_info_pane = !expanded_info_pane;
    }
}
