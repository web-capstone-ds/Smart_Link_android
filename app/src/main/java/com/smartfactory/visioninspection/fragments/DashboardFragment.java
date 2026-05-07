package com.smartfactory.visioninspection.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.smartfactory.visioninspection.R;
import com.smartfactory.visioninspection.StatusUpdateEvent;
import com.smartfactory.visioninspection.adapters.InspectionCardAdapter;

import java.util.ArrayList;
import java.util.List;

public class DashboardFragment extends Fragment {

    private RecyclerView recyclerView;
    private InspectionCardAdapter adapter;

    // 🚨 기존의 가짜 데이터(InspectionEvent) 대신 진짜 데이터 상자(StatusUpdateEvent) 사용!
    private List<StatusUpdateEvent> eventList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        recyclerView = view.findViewById(R.id.recycler_dashboard);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // 1. 앱을 처음 켜면 데이터가 없으니 '빈 리스트'로 시작합니다.
        eventList = new ArrayList<>();

        // 2. 어댑터에 빈 리스트를 연결해 둡니다.
        adapter = new InspectionCardAdapter(eventList);
        recyclerView.setAdapter(adapter);

        return view;
    }

    /**
     * MainActivity의 MQTT 매니저가 실시간 데이터를 수신하면 이 메서드를 호출합니다!
     */
    public void updateRealTimeData(StatusUpdateEvent event) {
        // 프래그먼트가 화면에 붙어있지 않을 때(앱이 백그라운드로 갔을 때) 튕기는 것을 방지
        if (getActivity() == null) return;

        // 백그라운드 통신 쓰레드에서 날아온 데이터를 화면(UI 쓰레드)에 안전하게 그리기 위함
        getActivity().runOnUiThread(() -> {
            if (adapter != null) {
                // 어댑터에게 "새 데이터 왔으니까 맨 위에 추가해!" 라고 명령
                adapter.addEventToTop(event);
                // 리스트 스크롤을 맨 위(0번째)로 끌어올려서 새 데이터가 잘 보이게 함
                recyclerView.smoothScrollToPosition(0);
            }
        });
    }
}