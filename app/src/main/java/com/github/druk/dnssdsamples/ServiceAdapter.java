/*
 * Copyright (C) 2015 Andriy Druk
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.druk.dnssdsamples;

import android.content.Context;
import android.os.Build;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.RecyclerView;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.druk.rx2dnssd.BonjourService;

import java.util.ArrayList;
import java.util.List;

public class ServiceAdapter extends RecyclerView.Adapter<ServiceAdapter.ViewHolder> {

    private final List<BonjourService> services = new ArrayList<>();

    public ServiceAdapter(Context context) {
        TypedValue mTypedValue = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.selectableItemBackground, mTypedValue, true);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        return new ViewHolder(LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.two_text_item, viewGroup, false));
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        BonjourService bs = getItem(position);
        holder.text1.setText(bs.getServiceName() + "." + bs.getRegType());
        if (bs.getInet4Address() != null) {
            holder.text2.setText("Address: " + bs.getInet4Address().toString() + ":" + bs.getPort());
        }
        else if (bs.getInet6Address() != null) {
            holder.text2.setText("Address: " + bs.getInet6Address().toString() + ":" + bs.getPort());
        }
        else {
            holder.text2.setText(R.string.unresolved);
        }
        holder.text3.setText("Interface: " + bs.getIfIndex());
    }

    @Override
    public int getItemCount() {
        return services.size();
    }

    @Override
    public long getItemId(int position) {
        return services.get(position).hashCode();
    }

    public BonjourService getItem(int position) {
        return services.get(position);
    }

    public void clear() {
        this.services.clear();
        notifyDataSetChanged();
    }

    public void add(BonjourService service) {
        this.services.add(service);
        notifyDataSetChanged();
    }

    public void remove(BonjourService bonjourService) {
        boolean updateList = false;
        // There are maybe one service few times with both ip addresses
        while (this.services.remove(bonjourService)) {
            updateList = true;
        }
        if (updateList) {
            notifyDataSetChanged();
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView text1;
        public TextView text2;
        public TextView text3;

        public ViewHolder(View itemView) {
            super(itemView);
            text1 = (TextView) itemView.findViewById(R.id.text1);
            text2 = (TextView) itemView.findViewById(R.id.text2);
            text3 = (TextView) itemView.findViewById(R.id.text3);
        }
    }
}
