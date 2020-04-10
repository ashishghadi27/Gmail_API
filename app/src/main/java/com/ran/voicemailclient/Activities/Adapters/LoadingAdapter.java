package com.ran.voicemailclient.Activities.Adapters;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;


import com.ran.voicemailclient.Activities.Fragments.ViewMail;
import com.ran.voicemailclient.Activities.Interface.FragmentAddingInterface;
import com.ran.voicemailclient.Activities.Models.DisplayMessage;
import com.ran.voicemailclient.R;

import java.util.List;
import java.util.Objects;

public class LoadingAdapter extends RecyclerView.Adapter<LoadingAdapter.MyViewHolder> {

    private List<DisplayMessage> listItems;
    private Context context;
    private FragmentAddingInterface fragmentAddingInterface;

    public class MyViewHolder extends RecyclerView.ViewHolder {
        private TextView initials, username, accountName, time, timeStamp;
        private RelativeLayout mainlay;

        public MyViewHolder(View view) {
            super(view);
            initials = view.findViewById(R.id.initials);
            username = view.findViewById(R.id.userName);
            accountName = view.findViewById(R.id.accountName);
            time = view.findViewById(R.id.time);
            timeStamp = view.findViewById(R.id.timestamp);
            mainlay = view.findViewById(R.id.mainlay);

        }

    }

    public LoadingAdapter(List<DisplayMessage> title, Context context, FragmentAddingInterface fragmentAddingInterface) {
        this.listItems = title;
        this.context = context;
        this.fragmentAddingInterface = fragmentAddingInterface;

    }


    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.message_lay, parent, false);

        return new MyViewHolder(itemView);
    }


    @Override
    public void onBindViewHolder(final MyViewHolder holder, int position) {
        final DisplayMessage list = listItems.get(position);
        String account_name = list.getSender();
        final String sender = account_name;
        account_name = account_name.replace("@gmail.com", "");
        String[] initialarr = account_name.split("");
        holder.initials.setText(initialarr[0]);
        Log.v("TIME", list.getTime());
        String timearr[] = list.getTime().split(" ");
        final String time = timearr[2] + " " + timearr[1] + ", " + timearr[0];
        final String time2 = timearr[4];
        //Log.v("INITIALS", account_name + "  " + initialarr[1]);
        holder.accountName.setText(account_name);
        holder.time.setText(time);
        holder.username.setText(list.getSnippet());
        holder.timeStamp.setText(time2);

        holder.mainlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Bundle args = new Bundle();
                args.putString("sender", sender);
                args.putString("date", time);
                args.putString("time", time2);
                args.putString("message", list.getSnippet());
                fragmentAddingInterface.addFragment(new ViewMail(), "viewmail", args);
            }
        });



    }

    @Override
    public int getItemCount() {
        return listItems.size();
    }

}
