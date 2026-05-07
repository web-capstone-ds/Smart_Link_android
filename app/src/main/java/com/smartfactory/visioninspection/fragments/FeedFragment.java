package com.smartfactory.visioninspection.fragments;

import android.os.Bundle;
import android.view.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.smartfactory.visioninspection.R;
import com.smartfactory.visioninspection.adapters.FeedAdapter;
import com.smartfactory.visioninspection.utils.MockDataUtil;

/**
 * 피드 탭 (FeedEventCard.tsx 대응)
 * LOT_END / HW_ALARM / ORACLE_ANALYSIS 이벤트 타임라인
 */
public class FeedFragment extends Fragment {

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_feed, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView rv = view.findViewById(R.id.rv_feed);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setHasFixedSize(true);
        rv.setAdapter(new FeedAdapter(MockDataUtil.getMockFeedEvents()));
    }
}
