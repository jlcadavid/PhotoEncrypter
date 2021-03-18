package com.example.photoencrypter;

import android.content.Context;
import android.widget.ArrayAdapter;

import java.util.HashMap;
import java.util.LinkedList;

public class CustomListAdapter extends ArrayAdapter<String> {

    HashMap<String, Integer> mIdMap = new HashMap<>();

    public CustomListAdapter(final Context context, final int textViewResourceId, final LinkedList<String> items)
    {
        super(context, textViewResourceId, items);
        for (int i = 0; i < items.size(); i++) {
            mIdMap.put(items.get(i), i);
        }
    }

    @Override
    public long getItemId(int position) {
        String item = getItem(position);
        return mIdMap.get(item);
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }
}
