package dev.bugakov.tinkoffnews;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

class DataAdapter extends RecyclerView.Adapter<DataAdapter.ViewHolder> {

    private LayoutInflater inflater;
    private List<Item> itemsList;

    DataAdapter(Context context, List<Item> itemsList) {
        this.itemsList = itemsList;
        this.inflater = LayoutInflater.from(context);
    }
    @Override
    public DataAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View view = inflater.inflate(R.layout.list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(DataAdapter.ViewHolder holder, int position) {
        Item item = itemsList.get(position);
        holder.nameView.setText(item.getText());
        holder.layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(holder.layout.getContext(), NewsActivity.class);
                intent.putExtra("id", item.getId());
                holder.layout.getContext().startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return itemsList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        final TextView nameView;
        final LinearLayout layout;
        ViewHolder(View view){
            super(view);
            layout = view.findViewById(R.id.layout);
            nameView = view.findViewById(R.id.name);
        }
    }
}