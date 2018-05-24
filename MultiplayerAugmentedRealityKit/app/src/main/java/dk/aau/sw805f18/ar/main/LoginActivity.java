package dk.aau.sw805f18.ar.main;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;

import dk.aau.sw805f18.ar.R;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        Button loginBtn = findViewById(R.id.login_button);

        loginBtn.setOnClickListener(v -> startActivity(new Intent(getBaseContext(), MainActivity.class)));
    }
}
