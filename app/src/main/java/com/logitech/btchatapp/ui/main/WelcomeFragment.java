package com.logitech.btchatapp.ui.main;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.logitech.btchatapp.R;
import com.logitech.btchatapp.callback.NextCallback;

public class WelcomeFragment extends Fragment implements View.OnClickListener {
    private MainViewModel mViewModel;
    private ImageView welcomeImageView;
    private TextView welcomeTitleView;
    private TextView welcomeDescView;
    private Button welcomeBtn;

    private int mode;
    private NextCallback nextCallback;

    private WelcomeFragment(int mode){
        this.mode = mode;
    }

    public static WelcomeFragment newInstance(int mode){
        return new WelcomeFragment(mode);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.main_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        welcomeImageView = view.findViewById(R.id.welcome_image);
        welcomeTitleView = view.findViewById(R.id.welcome_title);
        welcomeDescView = view.findViewById(R.id.welcome_desc);
        welcomeBtn = view.findViewById(R.id.welcome_btn);
        welcomeBtn.setOnClickListener(this);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mode==1){
            welcomeImageView.setImageResource(R.drawable.ic_chat_black_24dp);
            welcomeTitleView.setText(R.string.welcome_title_1);
            welcomeDescView.setText(R.string.welcome_desc_1);
            welcomeBtn.setText(R.string.next);
        }else{
            welcomeImageView.setImageResource(R.drawable.ic_insert_drive_file_black_24dp);
            welcomeTitleView.setText(R.string.welcome_title_2);
            welcomeDescView.setText(R.string.welcome_desc_2);
            welcomeBtn.setText(R.string.start);
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
//        mViewModel = ViewModelProviders.of(this).get(MainViewModel.class);
        // TODO: Use the ViewModel
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        if(context instanceof NextCallback)
            nextCallback = (NextCallback) context;
    }

    @Override
    public void onClick(View v) {
        if(nextCallback!=null){
            nextCallback.doAction(mode);
        }
    }
}