package in.andonsystem.activity;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputFilter;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.splunk.mint.Mint;

import org.json.JSONException;
import org.json.JSONObject;

import in.andonsystem.R;
import in.andonsystem.Constants;
import in.andonsystem.util.ErrorListener;
import in.andonsystem.util.RestUtility;

public class ForgotPasswordActivity extends AppCompatActivity {
    
    private final String TAG = ForgotPasswordActivity.class.getSimpleName();
    
    private Context context;
    private LinearLayout container;
    private ProgressBar progress;

    private Button submit;
    private TextView title2;
    private TextView title3;
    private TextView error1;
    private TextView error2;
    private EditText emailId;
    private EditText otp;
    private EditText newPasswd;
    private EditText newPasswd2;

    private RestUtility restUtility;
    ErrorListener errorListener;

    private String email;
    private int otpValue;
    private int action = 1;  //1: send otp, 2: verify otp, 3: change password

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG,"onCreate()");
        Mint.setApplicationEnvironment(Mint.appEnvironmentStaging);
        Mint.initAndStartSession(getApplication(), "056dd13f");
        setContentView(R.layout.activity_forgot_password);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        
        context = this;
        container = (LinearLayout) findViewById(R.id.content_forgot_password);
        submit = createButton("Submit");
        title2 = createTextView("OTP has been sent to your registered mobile number",30);
        error2 = createTextView("Incorrect OTP entered,Retry.",30);
        emailId = createEditText("Enter Email Id");
        otp = createEditText("Enter 6 digit OTP");
        newPasswd = createEditText("new password");
        newPasswd2 = createEditText("confirm new password");
        progress = (ProgressBar) findViewById(R.id.loading_progress);

        otp.setInputType(InputType.TYPE_CLASS_NUMBER);
        otp.setFilters(new InputFilter[]{new InputFilter.LengthFilter(6)});
        emailId.setInputType(InputType.TYPE_CLASS_TEXT);
        newPasswd.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        newPasswd2.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        container.removeAllViews();
        container.addView(emailId);
        container.addView(submit);
        container.addView(progress);

        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (action == 1){
                    String emailIdText = emailId.getText().toString();
                    if(emailIdText.equals("")){
                        Toast.makeText(context,"Enter Email Id first.",Toast.LENGTH_SHORT).show();
                    }
                    else{
                        email = emailIdText;
                        sendOTP();
                    }
                }else if (action == 2) {
                    String otpText = otp.getText().toString();
                    if(otpText.equals("") ){
                        Toast.makeText(context,"Enter OTP first",Toast.LENGTH_SHORT).show();
                    }else if(otpText.length() != 6){
                        Toast.makeText(context,"OTP must be 6 digit number. ",Toast.LENGTH_SHORT).show();
                    }else {
                        otpValue  = Integer.parseInt(otpText);
                        verifyOTP();
                    }
                }else if (action == 3){
                    String pass1 = newPasswd.getText().toString();
                    String pass2 = newPasswd2.getText().toString();
                    if(pass1.equals("") || pass2.equals("")){
                        Toast.makeText(context,"Password Field cannot be blank.",Toast.LENGTH_SHORT).show();
                    }else if(!pass1.equals(pass2)){
                        Toast.makeText(context,"Passwords do not match.",Toast.LENGTH_SHORT).show();
                    }else{
                        changePassword(pass1);
                    }
                }

            }
        });
        errorListener = new ErrorListener(this) {
            @Override
            protected void handleTokenExpiry() {

            }
            @Override
            protected void onError(VolleyError error) {
                progress.setVisibility(View.INVISIBLE);
            }
        };
        restUtility = new RestUtility(this) {
            @Override
            protected void handleInternetConnRetry() {
                onStart();
            }
        };
        restUtility.setProtected(false);

    }

    private void sendOTP(){
        Log.i(TAG,"sendOTP()");
        progress.setVisibility(View.VISIBLE);

        String url = Constants.API2_BASE_URL + "/misc/forgot_password/send_otp?email=" + email;

        Response.Listener<JSONObject> listener = new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.i(TAG, "send otp Response :" + response.toString());
                progress.setVisibility(View.INVISIBLE);
                try {
                    if (response.getString("status").equalsIgnoreCase("SUCCESS")){
                        container.removeAllViews();
                        container.addView(title2);
                        container.addView(otp);
                        container.addView(submit);
                        container.addView(progress);
                        action =2;
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        };
        restUtility.put(url,null,listener,errorListener);
    }

    private void verifyOTP(){
        Log.i(TAG,"verifyOTP()");

        progress.setVisibility(View.VISIBLE);

        String url = Constants.API2_BASE_URL + "/misc/forgot_password/verify_otp?email=" + email + "&otp=" + otpValue;

        Response.Listener<JSONObject> listener = new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.i(TAG, "verify otp Response :" + response.toString());
                progress.setVisibility(View.INVISIBLE);
                try {
                    if (response.getString("status").equalsIgnoreCase("SUCCESS")){
                        container.removeAllViews();
                        container.addView(newPasswd);
                        container.addView(newPasswd2);
                        container.addView(submit);
                        container.addView(progress);
                        action = 3;
                    }else{
                        Toast.makeText(context,"Entered Incorrect OTP",Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        };

        restUtility.put(url,null,listener,errorListener);
    }

    private void changePassword(String password){
        Log.i(TAG,"changePassword()");

        progress.setVisibility(View.VISIBLE);

        String url = Constants.API2_BASE_URL + "/misc/forgot_password/change_password?email=" + email + "&otp=" + otpValue + "&newPassword=" + password;

        Response.Listener<JSONObject> listener = new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.i(TAG, "verify otp Response :" + response.toString());
                progress.setVisibility(View.INVISIBLE);
                try {
                    if (response.getString("status").equalsIgnoreCase("SUCCESS")){
                        Toast.makeText(context,"Password reset successfully.",Toast.LENGTH_SHORT).show();
                        finish();
                    }else{
                        Toast.makeText(context,"Some Error occured, Try again.",Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        };
        restUtility.put(url,null,listener,errorListener);
    }

    private EditText createEditText(String hint){
        EditText editText = new EditText(context);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT
        );
        editText.setLayoutParams(params);
        editText.setHint(hint);
        return editText;
    }
    private TextView createTextView(String text,float margin){
        TextView textView = new TextView(context);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );

        params.bottomMargin = (int) (margin * Resources.getSystem().getDisplayMetrics().density);;
        textView.setLayoutParams(params);
        textView.setText(text);
        textView.setGravity(Gravity.CENTER_HORIZONTAL);
        return textView;
    }
    private Button createButton(String text){
        Button button = new Button(context);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        button.setLayoutParams(params);
        button.setText(text);
        return button;
    }

    @Override
    protected void onStop() {
        super.onStop();
        progress.setVisibility(View.INVISIBLE);
    }

}
