// References:
// For displaying red astrisk next to must fields(in string.xml):
// Gabriele Mariotti, 2020-05-05, CC BY-SA 4.0,https://stackoverflow.com/a/61622809
//
//Android Developers, 2020-11-18, Apache 2.0, https://developer.android.com/guide/topics/ui/controls/radiobutton#java
//
//Arash GM,2013-01-01, CC BY-SA 4.0,https://stackoverflow.com/a/14112280

package com.cmput301w21t36.phenocount;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * This activity deals with publishing an experiment
 * To access this activity: Open the app -> click on the addBotton
 * in the bottom of the main screen/activity
 * @see MainActivity
 */
public class PublishExperimentActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private FirebaseFirestore db;
    private TextView expName;
    private TextView expDesc;
    private TextView expRegion;
    private String expType="";
    private TextView expNum;
    private CheckBox expGeoLoc;
    private String owner;
    private int mode;
    private Experiment exp;
    private RadioGroup radioGroup;
    private RadioButton binomial;
    private RadioButton count;
    private RadioButton nonNegative;
    private RadioButton measure;
    private final String TAG = "PhenoCount";
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private androidx.appcompat.widget.Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.Theme_PhenoCount);
        setContentView(R.layout.activity_experiment_publish);
        //getSupportActionBar().setTitle("Publish an Experiment");
        navigationSettings();

        expName = findViewById(R.id.expName);
        expDesc = findViewById(R.id.expDesc);
        expRegion = findViewById(R.id.expRegion);
        expNum = findViewById(R.id.expNum);
        expGeoLoc = findViewById(R.id.geoCheckBox);
        binomial = findViewById(R.id.radioBinomial);
        count = findViewById(R.id.radioCount);
        nonNegative = findViewById(R.id.radioInt);
        measure = findViewById(R.id.radioMeasure);

        Bundle bundle = getIntent().getExtras();

        //mode 0 is for publishing and 1 for editing an experiment
        mode = bundle.getInt("mode");
        if (mode == 0) {
            owner = bundle.get("AutoId").toString();
        }

        if(mode == 1) {
            //getSupportActionBar().setTitle("Edit Experiment");
            exp = (Experiment) bundle.get("experiment");
            displayExp();
        }

        expDesc.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (expDesc.getText().toString().isEmpty()) {
                    expDesc.setError("Required field Description");
                } else {
                    expDesc.setError(null);
                }
            }
        });
    }

    //Android Developers, 2020-11-18, Apache 2.0, https://developer.android.com/guide/topics/ui/controls/radiobutton#java
    public void onRadioButtonClicked(View view) {
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();

        // Check which radio button was clicked
        switch(view.getId()) {
            case R.id.radioBinomial:
                if (checked)
                    expType="Binomial";
                break;
            case R.id.radioCount:
                if (checked)
                    expType="Count";
                break;
            case R.id.radioInt:
                if (checked)
                    expType="NonNegativeCount";
                break;
            case R.id.radioMeasure:
                if (checked)
                    expType="Measurement";
                break;
        }
    }

    /**
     * To display the fields for editing
     */
    public void displayExp(){
        radioGroup = findViewById(R.id.radio1);
        expName.setText(exp.getName());
        expDesc.setText(exp.getDescription());
        expRegion.setText(exp.getRegion());
        expNum.setText(Integer.toString(exp.getMinimumTrials()));
        expGeoLoc.setChecked(false);
        if (exp.isRequireLocation()) {
            expGeoLoc.setChecked(true);
        }

        expType=exp.getExpType();
        if (exp.getExpType().equals("Binomial")){
            radioGroup.check(R.id.radioBinomial);
        }else if (exp.getExpType().equals("Count")){
            radioGroup.check(R.id.radioCount);
        }else if (exp.getExpType().equals("NonNegativeCount")){
            radioGroup.check(R.id.radioInt);
        }else if (exp.getExpType().equals("Measurement")){
            radioGroup.check(R.id.radioMeasure);
        }

        binomial.setEnabled(true);
        count.setEnabled(true);
        nonNegative.setEnabled(true);
        measure.setEnabled(true);
        // Edit allows to change the type only if there are no trials
        if (exp.getTrials().size()>0){
            binomial.setEnabled(false);
            count.setEnabled(false);
            nonNegative.setEnabled(false);
            measure.setEnabled(false);
        }

    }

    /**
     * This method adds the experiments to the database (firestore)
     * in other words its publishes an experiment when the ok button is pressed
     * @param view
     */
    public void toAdd(View view) {

        db = FirebaseFirestore.getInstance();
        final CollectionReference collectionReference = db.collection("Experiment");

        boolean reqLoc = false;
        ArrayList sList = new ArrayList();


        final String desc = expDesc.getText().toString();

        // To make sure that the required fields are not left empty
        int proceed =1;
        if (!(desc.length()>0)){
            proceed=0;
        }
        if (!(expType.length()>0)){
            proceed=0;
        }
        if (proceed > 0) {

            if (expType.length() > 0 && desc.length() > 0) {
                if (mode == 0) {
                    HashMap<String, Object> data = new HashMap<>();
                    String id = db.collection("Experiment").document().getId();
                    data.put("name", expName.getText().toString());
                    data.put("description", desc);
                    data.put("type", expType);
                    data.put("region", expRegion.getText().toString());
                    data.put("minimum_trials", expNum.getText().toString());
                    data.put("owner", owner);
                    data.put("status", "1");
                    data.put("require_geolocation", "NO");
                    data.put("sub_list", sList);
                    if (expGeoLoc.isChecked()) {
                        data.put("require_geolocation", "YES");
                    }

                    collectionReference
                            .document(id)
                            .set(data)
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    // These are a method which gets executed when the task is succeeded
                                    Log.d(TAG, "Data has been added successfully!");
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    // These are a method which gets executed if there???s any problem
                                    Log.d(TAG, "Data could not be added!" + e.toString());
                                }
                            });
                }
                if (mode == 1) {
                    HashMap<String, Object> data = new HashMap<>();
                    String id = exp.getExpID();
                    data.put("name", expName.getText().toString());
                    data.put("description", desc);
                    data.put("type", expType);
                    data.put("region", expRegion.getText().toString());
                    data.put("minimum_trials", expNum.getText().toString());
                    data.put("owner", exp.getOwner().getUID());
                    data.put("status", Integer.toString(exp.getExpStatus()));
                    data.put("require_geolocation", "NO");
                    if (expGeoLoc.isChecked()) {
                        reqLoc = true;
                        data.put("require_geolocation", "YES");
                    }

                    collectionReference
                            .document(id)
                            .update(data)
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    // These are a method which gets executed when the task is succeeded
                                    Log.d(TAG, "Data has been editted successfully!");
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    // These are a method which gets executed if there???s any problem
                                    Log.d(TAG, "Data could not be editted!" + e.toString());
                                }
                            });
                }
            }
            finish();
            if (mode == 0) {
                Toast.makeText(this, "Your Experiment is published!!",
                        Toast.LENGTH_SHORT).show();
            }else{
                Experiment expObj = new Experiment(expName.getText().toString(), desc, expRegion.getText().toString(),
                        expType, Integer.parseInt(expNum.getText().toString()), reqLoc, exp.getExpStatus(),
                        exp.getExpID());
                expObj.setOwner(exp.getOwner());
                Intent intent = new Intent (this,DisplayExperimentActivity.class);
                intent.putExtra("experiment",expObj);
                //Arash GM,2013-01-01, CC BY-SA 4.0,https://stackoverflow.com/a/14112280
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                Toast.makeText(this, "Your Experiment is updated!!",
                        Toast.LENGTH_SHORT).show();
            }
        } else {
            AlertMsg altmsg = new AlertMsg(this, "Error Message",
                    "Description/Type of Experiment is Required, TRY AGAIN!!",0);
            altmsg.setButtonCol();
        }
    }

    /**
     * To abort the PublishExperimentActivity on cancel button press
     * @param view
     */
    public void toCancel(View view) {
        finish();
    }

    public void navigationSettings(){
        drawerLayout=findViewById(R.id.drawer_layout);
        navigationView=findViewById(R.id.nav_view);
        toolbar = findViewById(R.id.toolbar);
        navigationView.bringToFront();
        setSupportActionBar(toolbar);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this,drawerLayout,toolbar,R.string.navigation_drawer_open,R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(this);

    }

    @Override
    public void onBackPressed() {
        if(drawerLayout.isDrawerOpen(GravityCompat.START)){
            drawerLayout.closeDrawer(GravityCompat.START);
        }
        else{
            super.onBackPressed();
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        SharedPreferences sharedPrefs = getSharedPreferences("sharedPrefs", MODE_PRIVATE);
        String UUID = sharedPrefs.getString("ID", "");
        Intent intent = new Intent();
        switch (item.getItemId()){
            case R.id.nav_my_exp:
                intent = new Intent(PublishExperimentActivity.this,MainActivity.class);
                break;
            case R.id.nav_search:
                intent = new Intent(PublishExperimentActivity.this, com.cmput301w21t36.phenocount.SearchingActivity.class);
                break;
            case R.id.nav_user:
                intent = new Intent(PublishExperimentActivity.this,ProfileActivity.class);
                intent.putExtra("UUID",UUID);
                break;
            case R.id.nav_add:
                intent = new Intent(PublishExperimentActivity.this,PublishExperimentActivity.class);
                intent.putExtra("AutoId",UUID);
                intent.putExtra("mode",0);
                break;
            case R.id.nav_sub_exp:
                intent = new Intent(PublishExperimentActivity.this,ShowSubscribedListActivity.class);
                intent.putExtra("owner",UUID);
                break;
        }

        startActivity(intent);
        return true;
    }

}