package com.xyy.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public abstract class XBaseAdapter<T> extends BaseAdapter {
    protected List<T> list;
    protected LayoutInflater mInflater;
    protected Context context;

    public XBaseAdapter(Context context) {
        this.context = context;
        mInflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return null == list ? 0 : list.size();
    }

    @Override
    public T getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        int itemViewType = getItemViewType(position);
        if (null == convertView || ((holder = (ViewHolder)convertView.getTag()) != null && holder.itemViewType != itemViewType)) {
            holder = createViewHolder(itemViewType);
            convertView = holder.itemView;
            holder.itemViewType = itemViewType;
            convertView.setTag(holder);
        }
        T t = getItem(position);
        holder.position = position;
        bindView(t, holder);
        return convertView;
    }

    protected abstract ViewHolder createViewHolder(int itemViewType);

    protected abstract void bindView(T t, ViewHolder viewHolder);

    public static class ViewHolder {
        View itemView;
        int position;
        private int itemViewType;

        public ViewHolder(View itemView) {
            this.itemView = itemView;
        }
    }

    public List<T> getList() {
        return list;
    }

    public void setList(List<T> list) {
        this.list = list;
        notifyDataSetChanged();
    }

    public void addItem(T t) {
        if (null == list) {
            list = new LinkedList<>();
        }
        list.add(t);
        notifyDataSetChanged();
    }

}
