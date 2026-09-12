package com.vibestudio.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class TerminalFragment extends Fragment {

    private com.libtermux.view.TerminalView mTerminalView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_terminal, container, false);
        mTerminalView = view.findViewById(R.id.terminal_view);
        return view;
    }

    public com.libtermux.view.TerminalView getTerminalView() {
        return mTerminalView;
    }
}
