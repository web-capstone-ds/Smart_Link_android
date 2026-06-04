package com.smartfactory.visioninspection.utils;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

public class ThresholdProposalStore {
    private static final String PREF_NAME = "SmartFactoryThresholdProposal";
    private static final String KEY_NOTIFIED_RESULTS = "notified_results";

    private final SharedPreferences pref;

    public ThresholdProposalStore(Context context) {
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public synchronized boolean markResultNotified(String proposalId, String status) {
        if (proposalId == null || proposalId.trim().isEmpty()) return false;
        if (status == null || status.trim().isEmpty()) return false;

        String key = proposalId.trim() + "|" + status.trim().toUpperCase();
        Set<String> existing = pref.getStringSet(KEY_NOTIFIED_RESULTS, new HashSet<>());
        if (existing != null && existing.contains(key)) return false;

        Set<String> updated = existing == null ? new HashSet<>() : new HashSet<>(existing);
        updated.add(key);
        pref.edit().putStringSet(KEY_NOTIFIED_RESULTS, updated).apply();
        return true;
    }
}
