package com.gameclub.zone.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.gameclub.zone.admin.adapter.PayoutParticipantAdapter;
import com.gameclub.zone.admin.databinding.FragmentPayoutBinding;
import com.gameclub.zone.admin.model.Participant;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PayoutFragment extends Fragment implements PayoutParticipantAdapter.PayoutActionListener {

    private static final String ARG_MATCH_KEY = "arg_match_key";
    private static final String ARG_MATCH_TITLE = "arg_match_title";

    private String matchKey;
    private String matchTitle;
    private FragmentPayoutBinding binding;
    private PayoutParticipantAdapter adapter;
    private final List<Participant> participants = new ArrayList<>();
    private ValueEventListener matchListener;
    private ValueEventListener participantsListener;

    public static PayoutFragment newInstance(String matchKey, String matchTitle) {
        PayoutFragment fragment = new PayoutFragment();
        Bundle args = new Bundle();
        args.putString(ARG_MATCH_KEY, matchKey);
        args.putString(ARG_MATCH_TITLE, matchTitle);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            matchKey = getArguments().getString(ARG_MATCH_KEY);
            matchTitle = getArguments().getString(ARG_MATCH_TITLE);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentPayoutBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnBackFromPayout.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).onBackPressed();
            }
        });

        binding.tvPayoutMatchTitle.setText(matchTitle != null ? matchTitle : "Match Payout");

        adapter = new PayoutParticipantAdapter(requireContext(), this);
        binding.rvPayoutParticipants.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvPayoutParticipants.setAdapter(adapter);

        binding.btnSaveWinner.setOnClickListener(v -> saveWinner());
        binding.btnMarkOngoing.setOnClickListener(v -> updateMatchStatus("Ongoing"));
        binding.btnMarkFinished.setOnClickListener(v -> updateMatchStatus("Finished"));
        binding.btnBulkPayout.setOnClickListener(v -> payAllChanges());

        loadMatchAndParticipants();
    }

    private void loadMatchAndParticipants() {
        if (matchKey == null) return;

        // Match Info (Status, Winner)
        matchListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (binding == null) return;
                String status = snapshot.child("status").getValue(String.class);
                String winner = snapshot.child("winner").getValue(String.class);

                binding.tvPayoutMatchStatus.setText("Status: " + (status != null ? status : "Upcoming"));
                if (winner != null && binding.etDisplayWinner.getText().toString().isEmpty()) {
                    binding.etDisplayWinner.setText(winner);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        FirebaseManager.getMatchesRef().child(matchKey).addValueEventListener(matchListener);

        // Participants
        participantsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                participants.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Participant p = child.getValue(Participant.class);
                    if (p != null) {
                        p.setId(child.getKey());
                        participants.add(p);
                    }
                }
                if (binding != null) {
                    adapter.setParticipants(participants);
                    binding.tvEmptyParticipants.setVisibility(participants.isEmpty() ? View.VISIBLE : View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to load participants: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };
        FirebaseManager.getMatchParticipantsRef(matchKey).addValueEventListener(participantsListener);
    }

    private void saveWinner() {
        String winner = binding.etDisplayWinner.getText().toString().trim();
        FirebaseManager.getMatchesRef().child(matchKey).child("winner").setValue(winner)
                .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Winner saved!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void updateMatchStatus(String status) {
        FirebaseManager.getMatchesRef().child(matchKey).child("status").setValue(status)
                .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Status updated to " + status, Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onSavePayout(Participant participant, long newAmount) {
        long oldAmount = participant.getRewardAmount();
        long diff = newAmount - oldAmount;

        String userUid = participant.getJoinedBy() != null ? participant.getJoinedBy() : participant.getId();

        // 1. Update participant record
        FirebaseManager.getMatchParticipantsRef(matchKey).child(participant.getId()).child("rewardAmount").setValue(newAmount);

        // 2. Adjust winning balance
        if (diff != 0 && userUid != null) {
            String finalUid = userUid;
            FirebaseManager.getUsersRef().child(finalUid).child("winning").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    long current = 0;
                    if (snapshot.exists() && snapshot.getValue(Long.class) != null) {
                        current = snapshot.getValue(Long.class);
                    }
                    long updated = Math.max(0, current + diff);
                    FirebaseManager.getUsersRef().child(finalUid).child("winning").setValue(updated);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
        }

        Toast.makeText(getContext(), "Payout saved for " + participant.getIgn(), Toast.LENGTH_SHORT).show();
    }

    private void payAllChanges() {
        Map<String, Long> edited = adapter.getEditedAmounts();
        int changedCount = 0;

        for (Participant p : adapter.getParticipants()) {
            Long newAmt = edited.get(p.getId());
            if (newAmt != null && newAmt != p.getRewardAmount()) {
                onSavePayout(p, newAmt);
                changedCount++;
            }
        }

        if (changedCount > 0) {
            Toast.makeText(getContext(), "Processed " + changedCount + " payout updates!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "No amount changes detected.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (matchListener != null) FirebaseManager.getMatchesRef().child(matchKey).removeEventListener(matchListener);
        if (participantsListener != null) FirebaseManager.getMatchParticipantsRef(matchKey).removeEventListener(participantsListener);
        binding = null;
    }
}
