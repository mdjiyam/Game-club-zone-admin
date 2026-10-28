package com.gameclub.zone.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import com.gameclub.zone.admin.databinding.ActivityLoginBinding;
import com.gameclub.zone.admin.model.AdminUser;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            return windowInsets;
        });

        auth = FirebaseManager.getAuth();

        // Check if already signed in
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            checkAdminAndProceed(currentUser);
        }

        binding.btnLogin.setOnClickListener(v -> attemptLogin());
    }

    private void attemptLogin() {
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();

        binding.tvLoginError.setVisibility(View.GONE);

        if (email.isEmpty()) {
            binding.etEmail.setError("Enter email address");
            binding.etEmail.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            binding.etPassword.setError("Enter password");
            binding.etPassword.requestFocus();
            return;
        }

        setLoading(true);

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            checkAdminAndProceed(user);
                        } else {
                            setLoading(false);
                            showError("User not found after sign-in.");
                        }
                    } else {
                        setLoading(false);
                        String errorMsg = task.getException() != null ? task.getException().getMessage() : "Authentication failed.";
                        showError(errorMsg);
                    }
                });
    }

    private void checkAdminAndProceed(FirebaseUser user) {
        setLoading(true);
        String uid = user.getUid();

        // If this is the hardcoded Owner UID
        if (Constants.OWNER_UID.equals(uid)) {
            AdminUser ownerAdmin = new AdminUser();
            ownerAdmin.setUid(uid);
            ownerAdmin.setName(user.getDisplayName() != null ? user.getDisplayName() : "Owner");
            ownerAdmin.setRole("owner");
            ownerAdmin.setStatus("active");
            FirebaseManager.setCurrentAdmin(ownerAdmin);

            // Ensure owner record exists in database
            FirebaseManager.getAdminsRef().child(uid).child("role").setValue("owner");
            FirebaseManager.getAdminsRef().child(uid).child("status").setValue("active");
            FirebaseManager.getAdminsRef().child(uid).child("name").setValue(ownerAdmin.getName());

            proceedToMain();
            return;
        }

        // Otherwise verify against admins node in database
        FirebaseManager.getAdminsRef().child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                setLoading(false);
                if (snapshot.exists()) {
                    AdminUser admin = snapshot.getValue(AdminUser.class);
                    if (admin == null) {
                        admin = new AdminUser();
                    }
                    admin.setUid(uid);

                    if (snapshot.hasChild("name")) {
                        admin.setName(snapshot.child("name").getValue(String.class));
                    }
                    if (snapshot.hasChild("role")) {
                        admin.setRole(snapshot.child("role").getValue(String.class));
                    }
                    if (snapshot.hasChild("status")) {
                        admin.setStatus(snapshot.child("status").getValue(String.class));
                    }

                    if (snapshot.hasChild("permissions")) {
                        Map<String, Boolean> perms = new HashMap<>();
                        for (DataSnapshot pChild : snapshot.child("permissions").getChildren()) {
                            perms.put(pChild.getKey(), Boolean.TRUE.equals(pChild.getValue(Boolean.class)));
                        }
                        admin.setPermissions(perms);
                    }

                    if (snapshot.hasChild("assignedCategories")) {
                        Map<String, Boolean> cats = new HashMap<>();
                        for (DataSnapshot cChild : snapshot.child("assignedCategories").getChildren()) {
                            cats.put(cChild.getKey(), Boolean.TRUE.equals(cChild.getValue(Boolean.class)));
                        }
                        admin.setAssignedCategories(cats);
                    }

                    if (snapshot.hasChild("assignedMatches")) {
                        Map<String, Boolean> matches = new HashMap<>();
                        for (DataSnapshot mChild : snapshot.child("assignedMatches").getChildren()) {
                            matches.put(mChild.getKey(), Boolean.TRUE.equals(mChild.getValue(Boolean.class)));
                        }
                        admin.setAssignedMatches(matches);
                    }

                    if ("suspended".equalsIgnoreCase(admin.getStatus())) {
                        showError("This admin account has been suspended by the Owner.");
                        FirebaseManager.logout();
                        return;
                    }

                    if (admin.isRemoved()) {
                        showError("Admin privileges for this account have been revoked.");
                        FirebaseManager.logout();
                        return;
                    }

                    FirebaseManager.setCurrentAdmin(admin);
                    proceedToMain();
                } else {
                    showError("Access Denied: This account is not authorized as an Admin by the Owner.");
                    FirebaseManager.logout();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                setLoading(false);
                showError("Database error: " + error.getMessage());
            }
        });
    }

    private void proceedToMain() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void setLoading(boolean loading) {
        binding.loginProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnLogin.setEnabled(!loading);
        binding.etEmail.setEnabled(!loading);
        binding.etPassword.setEnabled(!loading);
    }

    private void showError(String msg) {
        binding.tvLoginError.setText(msg);
        binding.tvLoginError.setVisibility(View.VISIBLE);
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }
}
