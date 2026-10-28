package com.gameclub.zone.admin;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.gameclub.zone.admin.adapter.CategoryGridAdapter;
import com.gameclub.zone.admin.adapter.MatchAdapter;
import com.gameclub.zone.admin.databinding.DialogMatchEditorBinding;
import com.gameclub.zone.admin.databinding.FragmentMatchesBinding;
import com.gameclub.zone.admin.model.AdminUser;
import com.gameclub.zone.admin.model.Category;
import com.gameclub.zone.admin.model.Match;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MatchesFragment extends Fragment implements MatchAdapter.MatchActionListener, CategoryGridAdapter.CategorySelectListener {

    private FragmentMatchesBinding binding;
    private CategoryGridAdapter categoryGridAdapter;
    private MatchAdapter matchAdapter;

    private final List<Category> categories = new ArrayList<>();
    private final List<Match> allMatches = new ArrayList<>();
    private final Map<String, Integer> matchCountsPerCategory = new HashMap<>();
    private final Map<String, Integer> participantCounts = new HashMap<>();

    private Category selectedCategory = null;
    private String currentTab = "active"; // "active" or "finished"

    private ValueEventListener categoriesListener;
    private ValueEventListener matchesListener;
    private ValueEventListener participantsListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMatchesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnBackMatchesToDash.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).onBackPressed();
            }
        });

        categoryGridAdapter = new CategoryGridAdapter(requireContext(), this);
        binding.rvMatchCategoriesGrid.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        binding.rvMatchCategoriesGrid.setAdapter(categoryGridAdapter);

        matchAdapter = new MatchAdapter(requireContext(), this);
        binding.rvMatches.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvMatches.setAdapter(matchAdapter);

        binding.btnBackToCategories.setOnClickListener(v -> clearCategorySelection());

        setupTabs();

        binding.fabAddMatch.setOnClickListener(v -> {
            if (selectedCategory != null) {
                showMatchEditorDialog(null);
            }
        });

        loadCategoriesAndMatches();
    }

    private void setupTabs() {
        binding.tabMatchActive.setOnClickListener(v -> {
            currentTab = "active";
            binding.tabMatchActive.setBackgroundResource(R.drawable.bg_tab_active);
            binding.tabMatchActive.setTextColor(requireContext().getColor(R.color.text_main));
            binding.tabMatchFinished.setBackgroundResource(R.drawable.bg_tab_inactive);
            binding.tabMatchFinished.setTextColor(requireContext().getColor(R.color.text_muted));
            filterMatches();
        });

        binding.tabMatchFinished.setOnClickListener(v -> {
            currentTab = "finished";
            binding.tabMatchFinished.setBackgroundResource(R.drawable.bg_tab_active);
            binding.tabMatchFinished.setTextColor(requireContext().getColor(R.color.text_main));
            binding.tabMatchActive.setBackgroundResource(R.drawable.bg_tab_inactive);
            binding.tabMatchActive.setTextColor(requireContext().getColor(R.color.text_muted));
            filterMatches();
        });
    }

    private void clearCategorySelection() {
        selectedCategory = null;
        binding.layoutMatchCategorySelect.setVisibility(View.VISIBLE);
        binding.layoutCategoryMatchList.setVisibility(View.GONE);
        binding.fabAddMatch.setVisibility(View.GONE);
    }

    @Override
    public void onSelectCategory(Category category) {
        selectedCategory = category;
        binding.layoutMatchCategorySelect.setVisibility(View.GONE);
        binding.layoutCategoryMatchList.setVisibility(View.VISIBLE);
        binding.tvCurrentCatTitle.setText(category.getName());
        binding.fabAddMatch.setVisibility(View.VISIBLE);
        filterMatches();
    }

    private void loadCategoriesAndMatches() {
        // 1. Categories
        categoriesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                categories.clear();
                AdminUser currentAdmin = FirebaseManager.getCurrentAdmin();
                boolean isOwner = FirebaseManager.isOwner();

                for (DataSnapshot child : snapshot.getChildren()) {
                    Category c = child.getValue(Category.class);
                    if (c != null) {
                        c.setId(child.getKey());
                        if (isOwner || currentAdmin == null || currentAdmin.canManageCategory(c.getId(), c.getName())) {
                            categories.add(c);
                        }
                    }
                }
                updateCategoryGrid();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        FirebaseManager.getCategoriesRef().addValueEventListener(categoriesListener);

        // 2. Participants counts
        participantsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                participantCounts.clear();
                for (DataSnapshot matchSnapshot : snapshot.getChildren()) {
                    String matchKey = matchSnapshot.getKey();
                    if (matchKey != null) {
                        int count = (int) matchSnapshot.getChildrenCount();
                        participantCounts.put(matchKey, count);
                    }
                }
                filterMatches();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        FirebaseManager.getMatchParticipantsRef().addValueEventListener(participantsListener);

        // 3. Matches
        matchesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allMatches.clear();
                matchCountsPerCategory.clear();

                for (DataSnapshot child : snapshot.getChildren()) {
                    Match m = child.getValue(Match.class);
                    if (m != null) {
                        m.setDbKey(child.getKey());
                        allMatches.add(m);

                        // Count active matches for category grid
                        String cat = m.getCategory();
                        boolean isFinished = "Finished".equalsIgnoreCase(m.getStatus());
                        if (!isFinished && cat != null) {
                            matchCountsPerCategory.put(cat, matchCountsPerCategory.getOrDefault(cat, 0) + 1);
                        }
                    }
                }

                updateCategoryGrid();
                filterMatches();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        FirebaseManager.getMatchesRef().addValueEventListener(matchesListener);
    }

    private void updateCategoryGrid() {
        if (binding != null) {
            categoryGridAdapter.setData(categories, matchCountsPerCategory);
            binding.tvEmptyMatchCategories.setVisibility(categories.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    private void filterMatches() {
        if (selectedCategory == null || binding == null) return;

        List<Match> filtered = new ArrayList<>();
        String catId = selectedCategory.getId();
        String catName = selectedCategory.getName();

        AdminUser currentAdmin = FirebaseManager.getCurrentAdmin();
        boolean isOwner = FirebaseManager.isOwner();

        for (Match m : allMatches) {
            boolean matchesCat = (catId != null && catId.equals(m.getCategory())) ||
                    (catName != null && catName.equalsIgnoreCase(m.getCategory()));
            if (!matchesCat) continue;

            // Granular Match Permission check for sub-admins
            if (!isOwner && currentAdmin != null) {
                if (!currentAdmin.canManageMatch(m.getDbKey(), m.getCategory(), m.getAdminUid())) {
                    continue; // Skip matches not assigned to this admin
                }
            }

            boolean isFinished = "Finished".equalsIgnoreCase(m.getStatus());
            if ("active".equals(currentTab) && !isFinished) {
                filtered.add(m);
            } else if ("finished".equals(currentTab) && isFinished) {
                filtered.add(m);
            }
        }

        String scopeNotice = (!isOwner && currentAdmin != null) ? " [Assigned to You]" : "";
        binding.tvCurrentCatCount.setText(filtered.size() + " Matches (" + currentTab + ")" + scopeNotice);
        matchAdapter.setMatches(filtered, participantCounts);
        binding.tvEmptyMatches.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showMatchEditorDialog(Match matchToEdit) {
        if (!FirebaseManager.hasPermission(Constants.PERM_MANAGE_MATCHES)) {
            Toast.makeText(getContext(), "Permission Denied: Manage Matches", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!FirebaseManager.isOwner()) {
            AdminUser currentAdmin = FirebaseManager.getCurrentAdmin();
            if (matchToEdit != null) {
                if (currentAdmin != null && !currentAdmin.canManageMatch(matchToEdit.getDbKey(), matchToEdit.getCategory(), matchToEdit.getAdminUid())) {
                    Toast.makeText(getContext(), "Permission Denied: You are not assigned to this match.", Toast.LENGTH_SHORT).show();
                    return;
                }
            } else if (selectedCategory != null) {
                if (currentAdmin != null && !currentAdmin.canManageCategory(selectedCategory.getId(), selectedCategory.getName())) {
                    Toast.makeText(getContext(), "Permission Denied: You are not assigned to this category.", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext(), R.style.GCZ_DarkDialog);
        DialogMatchEditorBinding dialogBinding = DialogMatchEditorBinding.inflate(getLayoutInflater());
        builder.setView(dialogBinding.getRoot());
        AlertDialog dialog = builder.create();

        // Populate Map Spinner
        String[] maps = {"Bermuda", "Purgatory", "Kalahari", "Alpine", "Random", "Erangel", "Miramar", "Sanhok", "Livik"};
        ArrayAdapter<String> mapAdapter = new ArrayAdapter<>(requireContext(), R.layout.item_spinner_selected, maps);
        mapAdapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
        dialogBinding.spinnerMatchMap.setAdapter(mapAdapter);

        // Populate Type Spinner
        String[] types = {"Solo", "Duo", "Squad", "1v1 Clash Squad", "2v2 Clash Squad", "4v4 Clash Squad"};
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(requireContext(), R.layout.item_spinner_selected, types);
        typeAdapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
        dialogBinding.spinnerMatchType.setAdapter(typeAdapter);

        // Populate Status Spinner
        String[] statuses = {"Upcoming", "Ongoing", "Finished"};
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(requireContext(), R.layout.item_spinner_selected, statuses);
        statusAdapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
        dialogBinding.spinnerMatchStatus.setAdapter(statusAdapter);

        // Populate Assigned Admin Spinner
        List<String> adminDisplayList = new ArrayList<>();
        List<String> adminUidList = new ArrayList<>();
        adminDisplayList.add("All Admins");
        adminUidList.add("");

        ArrayAdapter<String> adminSpinnerAdapter = new ArrayAdapter<>(requireContext(), R.layout.item_spinner_selected, adminDisplayList);
        adminSpinnerAdapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
        dialogBinding.spinnerMatchAdmin.setAdapter(adminSpinnerAdapter);

        boolean isEdit = matchToEdit != null;
        dialogBinding.tvDialogMatchTitle.setText(isEdit ? "🎮 EDIT MATCH & ROOM" : "🎮 CREATE NEW MATCH");

        FirebaseManager.getAdminsRef().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (getContext() == null) return;
                for (DataSnapshot child : snapshot.getChildren()) {
                    String uid = child.getKey();
                    if (uid == null) continue;
                    String name = child.child("name").getValue(String.class);
                    if (name == null || name.trim().isEmpty()) name = "Admin";
                    if (!adminUidList.contains(uid)) {
                        adminDisplayList.add(name + " (" + uid.substring(0, Math.min(uid.length(), 6)) + ")");
                        adminUidList.add(uid);
                    }
                }
                adminSpinnerAdapter.notifyDataSetChanged();

                if (isEdit && matchToEdit.getAdminUid() != null && !matchToEdit.getAdminUid().isEmpty()) {
                    int idx = adminUidList.indexOf(matchToEdit.getAdminUid());
                    if (idx >= 0) {
                        dialogBinding.spinnerMatchAdmin.setSelection(idx);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        // Date and Time picker click listeners
        View.OnClickListener timePickerListener = v -> showDateTimePicker(dialogBinding);
        dialogBinding.layoutMatchTimePicker.setOnClickListener(timePickerListener);
        dialogBinding.etMatchTime.setOnClickListener(timePickerListener);
        dialogBinding.btnPickDateTime.setOnClickListener(timePickerListener);
        dialogBinding.ivMatchCalendarIcon.setOnClickListener(timePickerListener);
        dialogBinding.ivMatchClockIcon.setOnClickListener(timePickerListener);

        if (isEdit) {
            dialogBinding.etMatchTitle.setText(matchToEdit.getTitle());
            dialogBinding.etMatchTime.setText(matchToEdit.getTime());
            updateTimePreview(dialogBinding, matchToEdit.getTime());
            dialogBinding.etMatchEntry.setText(String.valueOf(matchToEdit.getEntry()));
            dialogBinding.etMatchPrize.setText(String.valueOf(matchToEdit.getTotal_prize()));
            dialogBinding.etMatchPerKill.setText(String.valueOf(matchToEdit.getPer_kill()));
            dialogBinding.etMatchTotalSlots.setText(String.valueOf(matchToEdit.getTotal()));
            dialogBinding.etMatchPrizeDesc.setText(matchToEdit.getPrize_desc());
            dialogBinding.etMatchRoomId.setText(matchToEdit.getRoom_id());
            dialogBinding.etMatchRoomPass.setText(matchToEdit.getRoom_pass());

            for (int i = 0; i < maps.length; i++) {
                if (maps[i].equalsIgnoreCase(matchToEdit.getMap())) {
                    dialogBinding.spinnerMatchMap.setSelection(i);
                    break;
                }
            }
            for (int i = 0; i < types.length; i++) {
                if (types[i].equalsIgnoreCase(matchToEdit.getType())) {
                    dialogBinding.spinnerMatchType.setSelection(i);
                    break;
                }
            }
            for (int i = 0; i < statuses.length; i++) {
                if (statuses[i].equalsIgnoreCase(matchToEdit.getStatus())) {
                    dialogBinding.spinnerMatchStatus.setSelection(i);
                    break;
                }
            }
        } else {
            dialogBinding.etMatchTotalSlots.setText("48");
            dialogBinding.etMatchEntry.setText("20");
            dialogBinding.etMatchPrize.setText("500");
            dialogBinding.etMatchPerKill.setText("5");

            // Default suggested match time: Today/Tomorrow at next rounded hour
            Calendar defaultCal = Calendar.getInstance();
            defaultCal.add(Calendar.HOUR_OF_DAY, 2);
            defaultCal.set(Calendar.MINUTE, 0);
            SimpleDateFormat defaultFormat = new SimpleDateFormat("dd MMM yyyy - hh:mm a", Locale.ENGLISH);
            String initialTime = defaultFormat.format(defaultCal.getTime());
            dialogBinding.etMatchTime.setText(initialTime);
            updateTimePreview(dialogBinding, initialTime);
        }

        dialogBinding.btnCancelMatch.setOnClickListener(v -> dialog.dismiss());
        dialogBinding.btnSaveMatch.setOnClickListener(v -> {
            String title = dialogBinding.etMatchTitle.getText().toString().trim();
            String time = dialogBinding.etMatchTime.getText().toString().trim();
            String map = dialogBinding.spinnerMatchMap.getSelectedItem().toString();
            String type = dialogBinding.spinnerMatchType.getSelectedItem().toString();
            String status = dialogBinding.spinnerMatchStatus.getSelectedItem().toString();
            String prizeDesc = dialogBinding.etMatchPrizeDesc.getText().toString().trim();
            String roomId = dialogBinding.etMatchRoomId.getText().toString().trim();
            String roomPass = dialogBinding.etMatchRoomPass.getText().toString().trim();

            long entry = parseLong(dialogBinding.etMatchEntry.getText().toString().trim(), 0);
            long prize = parseLong(dialogBinding.etMatchPrize.getText().toString().trim(), 0);
            long perKill = parseLong(dialogBinding.etMatchPerKill.getText().toString().trim(), 0);
            int totalSlots = (int) parseLong(dialogBinding.etMatchTotalSlots.getText().toString().trim(), 48);

            if (title.isEmpty()) {
                dialogBinding.etMatchTitle.setError("Title required");
                dialogBinding.etMatchTitle.requestFocus();
                return;
            }

            Map<String, Object> data = new HashMap<>();
            data.put("title", title);
            data.put("time", time);
            data.put("map", map);
            data.put("type", type);
            data.put("status", status);
            data.put("entry", entry);
            data.put("total_prize", prize);
            data.put("per_kill", perKill);
            data.put("total", totalSlots);
            data.put("prize_desc", prizeDesc);
            data.put("room_id", roomId);
            data.put("room_pass", roomPass);
            data.put("category", selectedCategory.getId());

            int selectedAdminIndex = dialogBinding.spinnerMatchAdmin.getSelectedItemPosition();
            String assignedUid = (selectedAdminIndex > 0 && selectedAdminIndex < adminUidList.size()) ? adminUidList.get(selectedAdminIndex) : "";
            String assignedName = (selectedAdminIndex > 0 && selectedAdminIndex < adminDisplayList.size()) ? adminDisplayList.get(selectedAdminIndex) : "All Admins";

            AdminUser currentAdminUser = FirebaseManager.getCurrentAdmin();
            if (!FirebaseManager.isOwner() && currentAdminUser != null && assignedUid.isEmpty()) {
                assignedUid = currentAdminUser.getUid();
                assignedName = currentAdminUser.getName();
            }

            data.put("adminUid", assignedUid);
            data.put("adminName", assignedName);

            if (isEdit) {
                FirebaseManager.getMatchesRef().child(matchToEdit.getDbKey()).updateChildren(data)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(getContext(), "Match updated!", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        })
                        .addOnFailureListener(e -> Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            } else {
                data.put("joined", 0);
                String key = FirebaseManager.getMatchesRef().push().getKey();
                if (key != null) {
                    FirebaseManager.getMatchesRef().child(key).setValue(data)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(getContext(), "Match created!", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                            })
                            .addOnFailureListener(e -> Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
            }
        });

        dialog.show();
    }

    private long parseLong(String val, long defaultVal) {
        try {
            return Long.parseLong(val);
        } catch (Exception e) {
            return defaultVal;
        }
    }

    @Override
    public void onManageMatch(Match match) {
        if (!checkMatchAccess(match)) return;
        showMatchEditorDialog(match);
    }

    @Override
    public void onPayouts(Match match) {
        if (!FirebaseManager.hasPermission(Constants.PERM_MANAGE_MATCHES)) {
            Toast.makeText(getContext(), "Permission Denied: Manage Matches", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!checkMatchAccess(match)) return;

        PayoutFragment fragment = PayoutFragment.newInstance(match.getDbKey(), match.getTitle());
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).showFragment(fragment, true);
        }
    }

    @Override
    public void onDeleteMatch(Match match) {
        if (!FirebaseManager.hasPermission(Constants.PERM_MANAGE_MATCHES)) {
            Toast.makeText(getContext(), "Permission Denied: Manage Matches", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!checkMatchAccess(match)) return;

        com.google.android.material.bottomsheet.BottomSheetDialog bottomSheetDialog = new com.google.android.material.bottomsheet.BottomSheetDialog(requireContext(), R.style.GCZ_DarkDialog);
        View bottomSheetView = getLayoutInflater().inflate(R.layout.dialog_bottom_delete_match, null);
        bottomSheetDialog.setContentView(bottomSheetView);

        android.widget.TextView tvDeleteMessage = bottomSheetView.findViewById(R.id.tvDeleteMessage);
        tvDeleteMessage.setText("Would you like to refund entry fees (৳" + match.getEntry() + ") to all participants before deleting, or delete directly?");

        bottomSheetView.findViewById(R.id.btnRefundAndDelete).setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            refundAndDeleteMatch(match);
        });

        bottomSheetView.findViewById(R.id.btnDeleteDirectly).setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            directDeleteMatch(match);
        });

        bottomSheetView.findViewById(R.id.btnCancelDelete).setOnClickListener(v -> bottomSheetDialog.dismiss());

        bottomSheetDialog.show();
    }

    private void directDeleteMatch(Match match) {
        FirebaseManager.getMatchesRef().child(match.getDbKey()).removeValue();
        FirebaseManager.getMatchParticipantsRef(match.getDbKey()).removeValue();
        Toast.makeText(getContext(), "Match deleted", Toast.LENGTH_SHORT).show();
    }

    private void refundAndDeleteMatch(Match match) {
        String matchKey = match.getDbKey();
        long entryFee = match.getEntry();

        FirebaseManager.getMatchParticipantsRef(matchKey).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot pSnapshot : snapshot.getChildren()) {
                    String userUid = pSnapshot.child("joinedBy").getValue(String.class);
                    if (userUid == null) userUid = pSnapshot.getKey();

                    if (userUid != null && entryFee > 0) {
                        String finalUid = userUid;
                        FirebaseManager.getUsersRef().child(finalUid).child("winning").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                                long currentWinning = 0;
                                if (userSnapshot.exists() && userSnapshot.getValue(Long.class) != null) {
                                    currentWinning = userSnapshot.getValue(Long.class);
                                }
                                FirebaseManager.getUsersRef().child(finalUid).child("winning").setValue(currentWinning + entryFee);
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {}
                        });
                    }
                }

                directDeleteMatch(match);
                Toast.makeText(getContext(), "Participants refunded and match deleted", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Refund failed: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateTimePreview(DialogMatchEditorBinding dialogBinding, String timeStr) {
        if (dialogBinding.tvMatchTimePreview == null) return;
        if (timeStr == null || timeStr.trim().isEmpty()) {
            dialogBinding.tvMatchTimePreview.setText("📅 Tap above to select Date, Year and Match Time");
            dialogBinding.tvMatchTimePreview.setTextColor(0x80FFFFFF);
            return;
        }

        dialogBinding.tvMatchTimePreview.setText("🗓️ Selected: " + timeStr);
        dialogBinding.tvMatchTimePreview.setTextColor(0xFFFFFFFF);
    }

    private void showDateTimePicker(DialogMatchEditorBinding dialogBinding) {
        final Calendar c = Calendar.getInstance();
        String current = dialogBinding.etMatchTime.getText().toString().trim();
        if (!current.isEmpty()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy - hh:mm a", Locale.ENGLISH);
                Date d = sdf.parse(current);
                if (d != null) {
                    c.setTime(d);
                }
            } catch (Exception ignored) {}
        }

        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                R.style.GCZ_DarkDialog,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    int hour = c.get(Calendar.HOUR_OF_DAY);
                    int minute = c.get(Calendar.MINUTE);

                    TimePickerDialog timePickerDialog = new TimePickerDialog(
                            requireContext(),
                            R.style.GCZ_DarkDialog,
                            (tView, selectedHour, selectedMinute) -> {
                                Calendar sel = Calendar.getInstance();
                                sel.set(Calendar.YEAR, selectedYear);
                                sel.set(Calendar.MONTH, selectedMonth);
                                sel.set(Calendar.DAY_OF_MONTH, selectedDay);
                                sel.set(Calendar.HOUR_OF_DAY, selectedHour);
                                sel.set(Calendar.MINUTE, selectedMinute);

                                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy - hh:mm a", Locale.ENGLISH);
                                String formatted = sdf.format(sel.getTime());
                                dialogBinding.etMatchTime.setText(formatted);
                                updateTimePreview(dialogBinding, formatted);
                                Toast.makeText(requireContext(), "Selected: " + formatted, Toast.LENGTH_SHORT).show();
                            },
                            hour,
                            minute,
                            false
                    );
                    timePickerDialog.setTitle("Select Match Time (Hour & Minute)");
                    timePickerDialog.show();
                },
                year,
                month,
                day
        );
        datePickerDialog.setTitle("Select Match Date & Year");
        datePickerDialog.show();
    }

    private boolean checkMatchAccess(Match match) {
        if (FirebaseManager.isOwner()) return true;
        AdminUser currentAdmin = FirebaseManager.getCurrentAdmin();
        if (currentAdmin != null && !currentAdmin.canManageMatch(match.getDbKey(), match.getCategory(), match.getAdminUid())) {
            Toast.makeText(getContext(), "Access Denied: You are not authorized to manage this specific match.", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (categoriesListener != null) FirebaseManager.getCategoriesRef().removeEventListener(categoriesListener);
        if (matchesListener != null) FirebaseManager.getMatchesRef().removeEventListener(matchesListener);
        if (participantsListener != null) FirebaseManager.getMatchParticipantsRef().removeEventListener(participantsListener);
        binding = null;
    }
}
