package com.anna.picturematching;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.LinkedList;

public class VerticalAdapter extends RecyclerView.Adapter<VerticalAdapter.CustomViewHolder> {

    private Context context;
    LinkedList<String> images;

    public VerticalAdapter(Context context){
        this.context = context;
        this.images = CameraActivity.prevNames;
    }

    @Override
    public CustomViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.popular_single_item, null);
        CustomViewHolder viewHolder = new CustomViewHolder(view);

//        view.setOnClickListener(this);

        return viewHolder;
    }


    @Override
    public void onBindViewHolder(VerticalAdapter.CustomViewHolder holder, int position) {

        String imgName = images.get(position);

        ImageView imageView = holder.fullImage;

        new ImageLoader(imageView, null, false).execute(imgName);
    }

    @Override
    public int getItemCount() {
        return (images!=null ? images.size() : 0);
    }

    class CustomViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        // Your holder should contain a member variable
        // for any view that will be set as you render a row
        ImageView fullImage;


        // We also create a constructor that accepts the entire item row
        // and does the view lookups to find each subview
        CustomViewHolder(View itemView) {
            // Stores the itemView in a public final member variable that can be used
            // to access the context from any ViewHolder instance.
            super(itemView);

            fullImage = (ImageView) itemView.findViewById(R.id.popular_image);

            fullImage.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    int clickedPosition = getAdapterPosition();
                    Toast.makeText(context, images.get(clickedPosition), Toast.LENGTH_LONG).show();

                    Intent newIntent = new Intent(context, CameraActivity.class);

                    newIntent.putExtra("imageIndex", clickedPosition);
                    newIntent.putExtra("taking", false);

                    context.startActivity(newIntent);

                }
            });
        }

        @Override
        public void onClick(View v) {
        }
    }



}
