package com.xyy.adapter;

import android.support.v4.view.PagerAdapter;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by Administrator on 2017/11/15.
 */
public abstract class XPagerAdapter<T> extends PagerAdapter {
    //当前显示的视图
    private SparseArray<ViewHolder> currentHolder;
    //缓存的视图
    private LinkedList<ViewHolder> caches;
    protected List<T> list;

    public XPagerAdapter() {
        currentHolder = new SparseArray<>();
        caches = new LinkedList<>();
    }

    @Override
    public int getCount() {
        return null == list ? 0 : list.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    // 当某一页面被销毁时触发
    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        View v = currentHolder.get(position).itemView;
        container.removeView(v);
        ViewHolder holder = currentHolder.get(position);
        currentHolder.remove(position);
        caches.addLast(holder);
    }

    // 初始化需要显示的页面
    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        ViewHolder holder;
        if (!caches.isEmpty()) {
            //使用缓存
            holder = caches.removeFirst();
        } else {
            holder = onCreateView();
        }
        onBindView(position, holder);
        //记录当前显示
        currentHolder.put(position, holder);
        //将子视图添加到ViewPager中
        container.addView(holder.itemView);
        holder.itemView.setTag(position);
        holder.position = position;
        return holder.itemView;
    }

    /**
     * 创建视图
     *
     * @return
     */
    protected abstract ViewHolder onCreateView();

    /**
     * 绑定视图
     *
     * @param position
     */
    protected abstract void onBindView(int position, ViewHolder holder);

    protected class ViewHolder {
        private View itemView;
        private int position;

        public ViewHolder(View itemView) {
            this.itemView = itemView;
        }

        protected int getLayotPosition() {
            return position;
        }

        public View getItemView(){
            return itemView;
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
        if (list == null) {
            list = new LinkedList<>();
        }
        list.add(t);
        notifyDataSetChanged();
    }
}

