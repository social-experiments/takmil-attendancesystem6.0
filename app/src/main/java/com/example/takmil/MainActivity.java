package com.example.takmil;

import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    AppLocationService appLocationService;
    Location ntkLocation;
    Location gpsLocation;
    Toast toast;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setLogo(R.mipmap.ic_launcher);
        getSupportActionBar().setDisplayUseLogoEnabled(true);



        SharedPreferences myPreferences
                = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);


        final EditText schoolName = (EditText) this.findViewById(R.id.edit_school_name);
        final EditText teacherName = (EditText) this.findViewById(R.id.edit_teacher_name);
        final EditText className = (EditText) this.findViewById(R.id.edit_class_name);
        Button nextButton = (Button) this.findViewById(R.id.nextButton);

        //retrieving values for Textfield's if it is stored already in SharedPreferences
        final SharedPreferences.Editor myEditor = myPreferences.edit();
        schoolName.setText(myPreferences.getString("SCHOOL_NAME", ""), TextView.BufferType.EDITABLE);
        teacherName.setText(myPreferences.getString("TEACHER_NAME",""),TextView.BufferType.EDITABLE);
        className.setText(myPreferences.getString("CLASS_NAME",""),TextView.BufferType.EDITABLE);



        //setting up the listener for the "next" button
        nextButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {



                String strSchoolName = schoolName.getText().toString();
                String strTeacherName = teacherName.getText().toString();
                String strClassName = className.getText().toString();



                if(strSchoolName.trim().length() > 0  && strClassName.trim().length() > 0 && strTeacherName.trim().length() > 0)
                {


                    //Store the Records in SharedPreferences
                    myEditor.putString("SCHOOL_NAME", strSchoolName);
                    myEditor.putString("TEACHER_NAME", strTeacherName);
                    myEditor.putString("CLASS_NAME", strClassName);
                    myEditor.commit();


                    //store the text fields into strings, and start the next page activitiy
                    Intent intent = new Intent(MainActivity.this, uploadPhoto.class);
                    intent.putExtra("schoolName", strSchoolName);
                    intent.putExtra("teacherName", strTeacherName);
                    intent.putExtra("className", strClassName);

                    startActivity(intent);
                }
                else
                {
                    toast =Toast.makeText(getApplicationContext(),"The School Name, Class Name, Teacher Name are all mandatory fields",Toast.LENGTH_LONG);
                    toast.show();
                }

            }

        });

    }

}
