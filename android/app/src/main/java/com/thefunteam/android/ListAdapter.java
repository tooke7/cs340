package com.thefunteam.android;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.thefunteam.android.activity.AvailableGamesActivity;
import com.thefunteam.android.activity.CurrentGameActivity;
import com.thefunteam.android.model.Atom;
import com.thefunteam.android.model.Game;
import com.thefunteam.android.model.Model;

import java.util.List;

/**
 * Created by msi on 2017-10-01.
 */

public class ListAdapter extends RecyclerView.Adapter<ListAdapter.ViewHolder>{

    private List<ListItem> listitems;
    private Context context;

    ActivityManager am = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
    ComponentName cn = am.getRunningTasks(1).get(0).topActivity;

    //private AlertDialog dialog;

    public ListAdapter(List<ListItem> listitems, Context context) {
        this.listitems = listitems;
        this.context = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recycler_view, parent, false);
        return new ViewHolder(v);

    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final Game game = Atom.getInstance().getModel().getAvailableGames().get(position);

        ListItem listitem = listitems.get(position);
        holder.textDescription.setText(listitem.getDescription().getGameId());
        holder.cardview.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(final View view) {

                if(cn.equals(AvailableGamesActivity.class)){
                    final AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
                    builder
                            .setTitle("Join Game")
                            .setMessage("Do you want to join this game?")
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    if(game.isStarted()){
                                        AlertDialog.Builder error = new AlertDialog.Builder(view.getContext());
                                        error.setMessage("the game arleady started")
                                                .setTitle("error")
                                                .show();
                                    }
                                    else if (game.getPlayers().size() == 6){
                                        AlertDialog.Builder error2 = new AlertDialog.Builder(view.getContext());
                                        error2.setMessage("too much people!")
                                                .setTitle("error")
                                                .show();
                                    }
                                    else {
                                        NextLayerFacade.getInstance().joinGame(game.getGameId());
                                    }
                                }
                            })
                            .setNegativeButton("No", null)
                            .show();
                }
                else {
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return listitems.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{

        public TextView textDescription;
        public LinearLayout cardview;

        public ViewHolder(View itemView) {
            super(itemView);
            textDescription = (TextView) itemView.findViewById(R.id.description);
            cardview = (LinearLayout) itemView.findViewById(R.id.cardView);
        }

    }

//    AlertDialog.Builder dialog  = new AlertDialog.Builder(view.getContext());
//    AlertDialog.Builder yes = dialog.setMessage("do you want to join this game?").setCancelable(false)
//            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
//                @Override
//                public void onClick(DialogInterface dialogInterface, int i) {
//                    Intent i = new Intent(view.getContext(), CurrentGameActivity.class);
//                    view.getContext().startActivity(i);
//                }
//            });

}
