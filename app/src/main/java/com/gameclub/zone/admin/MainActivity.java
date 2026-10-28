package com.gameclub.zone.admin;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import com.gameclub.zone.admin.databinding.ActivityMainBinding;
import com.gameclub.zone.admin.model.AdminUser;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Verify authentication state
        FirebaseUser user = FirebaseManager.getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Handle System Insets (Edge-to-Edge & 3-button Navigation Bar)
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            return windowInsets;
        });

        setupAdminHeader();
        startAdminSync();

        binding.btnLogout.setOnClickListener(v -> confirmLogout());

        // Handle Back Press
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                FragmentManager fm = getSupportFragmentManager();
                if (fm.getBackStackEntryCount() > 0) {
                    fm.popBackStack();
                } else {
                    finish();
                }
            }
        });

        if (savedInstanceState == null) {
            showFragment(new DashboardFragment(), false);
        }
    }

    private void setupAdminHeader() {
        AdminUser admin = FirebaseManager.getCurrentAdmin();
        FirebaseUser user = FirebaseManager.getCurrentUser();

        String name = "Admin";
        if (admin != null && admin.getName() != null && !admin.getName().isEmpty()) {
            name = admin.getName();
        } else if (user != null && user.getEmail() != null) {
            name = user.getEmail();
        }

        binding.tvAdminName.setText(name);

        boolean isOwner = FirebaseManager.isOwner();
        if (isOwner) {
            binding.tvAdminRoleBadge.setText("OWNER");
            binding.tvAdminRoleBadge.setTextColor(getColor(R.color.warning));
            binding.tvAdminRoleBadge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#26FFD93D")));
        } else {
            binding.tvAdminRoleBadge.setText("ADMIN");
            binding.tvAdminRoleBadge.setTextColor(getColor(R.color.info));
            binding.tvAdminRoleBadge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#264FC3F7")));
        }
    }

    private com.google.firebase.database.ValueEventListener adminSyncListener;

    private void startAdminSync() {
        FirebaseUser user = FirebaseManager.getCurrentUser();
        if (user == null) return;

        adminSyncListener = new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@androidx.annotation.NonNull com.google.firebase.database.DataSnapshot snapshot) {
                if (isFinishing() || isDestroyed()) return;

                if (snapshot.exists()) {
                    AdminUser admin = snapshot.getValue(AdminUser.class);
                    if (admin == null) admin = new AdminUser();
                    admin.setUid(user.getUid());

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
                        java.util.Map<String, Boolean> perms = new java.util.HashMap<>();
                        for (com.google.firebase.database.DataSnapshot pChild : snapshot.child("permissions").getChildren()) {
                            perms.put(pChild.getKey(), Boolean.TRUE.equals(pChild.getValue(Boolean.class)));
                        }
                        admin.setPermissions(perms);
                    }
                    if (snapshot.hasChild("assignedCategories")) {
                        java.util.Map<String, Boolean> cats = new java.util.HashMap<>();
                        for (com.google.firebase.database.DataSnapshot cChild : snapshot.child("assignedCategories").getChildren()) {
                            cats.put(cChild.getKey(), Boolean.TRUE.equals(cChild.getValue(Boolean.class)));
                        }
                        admin.setAssignedCategories(cats);
                    }
                    if (snapshot.hasChild("assignedMatches")) {
                        java.util.Map<String, Boolean> matches = new java.util.HashMap<>();
                        for (com.google.firebase.database.DataSnapshot mChild : snapshot.child("assignedMatches").getChildren()) {
                            matches.put(mChild.getKey(), Boolean.TRUE.equals(mChild.getValue(Boolean.class)));
                        }
                        admin.setAssignedMatches(matches);
                    }

                    if ("suspended".equalsIgnoreCase(admin.getStatus()) || admin.isRemoved()) {
                        FirebaseManager.logout();
                        android.widget.Toast.makeText(MainActivity.this, "Admin access suspended or revoked", android.widget.Toast.LENGTH_LONG).show();
                        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                        return;
                    }

                    FirebaseManager.setCurrentAdmin(admin);
                    setupAdminHeader();
                } else if (!Constants.OWNER_UID.equals(user.getUid())) {
                    FirebaseManager.logout();
                    android.widget.Toast.makeText(MainActivity.this, "Admin access revoked", android.widget.Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }
            }

            @Override
            public void onCancelled(@androidx.annotation.NonNull com.google.firebase.database.DatabaseError error) {}
        };

        FirebaseManager.getAdminsRef().child(user.getUid()).addValueEventListener(adminSyncListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (adminSyncListener != null) {
            FirebaseUser user = FirebaseManager.getCurrentUser();
            if (user != null) {
                FirebaseManager.getAdminsRef().child(user.getUid()).removeEventListener(adminSyncListener);
            }
        }
    }

    public void showFragment(Fragment fragment, boolean addToBackStack) {
        androidx.fragment.app.FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out,
                android.R.anim.fade_in,
                android.R.anim.fade_out
        );
        transaction.replace(R.id.fragmentContainer, fragment);
        if (addToBackStack) {
            transaction.addToBackStack(null);
        }
        transaction.commit();
    }

    public void onBackPressed() {
        getOnBackPressedDispatcher().onBackPressed();
    }

    private void confirmLogout() {
        new AlertDialog.Builder(this, R.style.GCZ_DarkDialog)
                .setTitle("Sign Out")
                .setMessage("Are you sure you want to sign out of GameClubZone Admin?")
                .setPositiveButton("Sign Out", (dialog, which) -> {
                    FirebaseManager.logout();
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
