package com.afollestad.sectionedrecyclerview;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.support.annotation.IntRange;
import android.support.annotation.Nullable;
import android.support.v4.util.ArrayMap;
import android.support.v4.util.SparseArrayCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

/**
 * @author Aidan Follestad (afollestad)
 */
public abstract class SectionedRecyclerViewAdapter<VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH> {

    public final static int VIEW_TYPE_HEADER = -2;
    public final static int VIEW_TYPE_ITEM = -1;

    private final ArrayMap<Integer, Integer> mHeaderLocationMap;
    private final ArrayMap<Integer, Integer> mHeaderLocationMapAbsolute;

    private GridLayoutManager mLayoutManager;
    private boolean mShowHeadersForEmptySections;
    private boolean mSetHeadersSticky;

    public SectionedRecyclerViewAdapter() {
        mHeaderLocationMap = new ArrayMap<>();
        mHeaderLocationMapAbsolute = new ArrayMap<>();
    }

    public abstract int getSectionCount();

    public abstract int getItemCount(int section);

    public abstract void onBindHeaderViewHolder(VH holder, int section);

    public abstract void onBindViewHolder(VH holder, int section, int relativePosition, int absolutePosition);

    public final boolean isHeader(int position) {
        return mHeaderLocationMap.get(position) != null;
    }

    /**
     * Instructs the list view adapter to whether show headers for empty sections or not.
     *
     * @param show flag indicating whether headers for empty sections ought to be shown.
     */
    public final void shouldShowHeadersForEmptySections(boolean show) {
        mShowHeadersForEmptySections = show;
    }

    public final void shouldSetHeadersSticky(boolean sticky) {
        mSetHeadersSticky = sticky;
    }

    public final void setLayoutManager(@Nullable GridLayoutManager lm) {
        mLayoutManager = lm;
        if (lm == null) return;
        lm.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (isHeader(position))
                    return mLayoutManager.getSpanCount();
                final int[] sectionAndPos = getSectionIndexAndRelativePosition(position);
                final int absPos = position - (sectionAndPos[0] + 1);
                return getRowSpan(mLayoutManager.getSpanCount(),
                        sectionAndPos[0], sectionAndPos[1], absPos);
            }
        });
    }

    @SuppressWarnings("UnusedParameters")
    protected int getRowSpan(int fullSpanSize, int section, int relativePosition, int absolutePosition) {
        return 1;
    }

    // returns section along with offsetted position
    private int[] getSectionIndexAndRelativePosition(int itemPosition) {
        synchronized (mHeaderLocationMap) {
            Integer lastSectionIndex = -1;
            for (final Integer sectionIndex : mHeaderLocationMap.keySet()) {
                if (itemPosition > sectionIndex) {
                    lastSectionIndex = sectionIndex;
                } else {
                    break;
                }
            }
            return new int[]{mHeaderLocationMap.get(lastSectionIndex), itemPosition - lastSectionIndex - 1};
        }
    }

    @Override
    public final int getItemCount() {
        int count = 0;
        mHeaderLocationMap.clear();
        mHeaderLocationMapAbsolute.clear();

        for (int s = 0; s < getSectionCount(); s++) {
            int itemCount = getItemCount(s);
            if (mShowHeadersForEmptySections || (itemCount > 0)) {
                mHeaderLocationMap.put(count, s);
                //save header pos for index
                for (int i = 0; i < itemCount; i++) {
                    int absPos = count + i + 1;
                    mHeaderLocationMapAbsolute.put(absPos, count);
                }
                count += itemCount + 1;
            }
        }
        return count;
    }

    /**
     * @hide
     * @deprecated
     */
    @Override
    @Deprecated
    public long getItemId(int position) {
        if (isHeader(position)) {
            int pos = mHeaderLocationMap.get(position);
            return getHeaderId(pos);
        } else {
            int[] sectionAndPos = getSectionIndexAndRelativePosition(position);
            return getItemId(sectionAndPos[0], sectionAndPos[1]);
        }
    }

    public long getHeaderId(int section) {
        return super.getItemId(section);
    }

    public long getItemId(int section, int position) {
        return super.getItemId(position);
    }

    /**
     * @hide
     * @deprecated
     */
    @Override
    @Deprecated
    public final int getItemViewType(int position) {
        if (isHeader(position)) {
            return getHeaderViewType(mHeaderLocationMap.get(position));
        } else {
            final int[] sectionAndPos = getSectionIndexAndRelativePosition(position);
            return getItemViewType(sectionAndPos[0],
                    // offset section view positions
                    sectionAndPos[1],
                    position - (sectionAndPos[0] + 1));
        }
    }

    @SuppressWarnings("UnusedParameters")
    @IntRange(from = 0, to = Integer.MAX_VALUE)
    public int getHeaderViewType(int section) {
        //noinspection ResourceType
        return VIEW_TYPE_HEADER;
    }

    @SuppressWarnings("UnusedParameters")
    @IntRange(from = 0, to = Integer.MAX_VALUE)
    public int getItemViewType(int section, int relativePosition, int absolutePosition) {
        //noinspection ResourceType
        return VIEW_TYPE_ITEM;
    }

    /**
     * @hide
     * @deprecated
     */
    @Override
    @Deprecated
    public final void onBindViewHolder(VH holder, int position) {
        StaggeredGridLayoutManager.LayoutParams layoutParams = null;
        if (holder.itemView.getLayoutParams() instanceof GridLayoutManager.LayoutParams)
            layoutParams = new StaggeredGridLayoutManager.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        else if (holder.itemView.getLayoutParams() instanceof StaggeredGridLayoutManager.LayoutParams)
            layoutParams = (StaggeredGridLayoutManager.LayoutParams) holder.itemView.getLayoutParams();
        if (isHeader(position)) {
            if (layoutParams != null) layoutParams.setFullSpan(true);
            //remove recyclable view for sticky header
            holder.setIsRecyclable(mSetHeadersSticky);
            onBindHeaderViewHolder(holder, mHeaderLocationMap.get(position));
        } else {
            if (layoutParams != null) layoutParams.setFullSpan(false);
            final int[] sectionAndPos = getSectionIndexAndRelativePosition(position);
            final int absPos = position - (sectionAndPos[0] + 1);
            onBindViewHolder(holder, sectionAndPos[0],
                    // offset section view positions
                    sectionAndPos[1], absPos);
        }
        if (layoutParams != null)
            holder.itemView.setLayoutParams(layoutParams);
    }

    /**
     * @hide
     * @deprecated
     */
    @Deprecated
    @Override
    public final void onBindViewHolder(VH holder, int position, List<Object> payloads) {
        super.onBindViewHolder(holder, position, payloads);
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        if (mSetHeadersSticky) recyclerView.addItemDecoration(new HeaderItemDecoration());
    }

    private class HeaderItemDecoration extends RecyclerView.ItemDecoration {

        private View currentHeaderView;
        private SparseArrayCompat<View> viewSparseArrayCompat;
        private int currentHeaderPos;

        HeaderItemDecoration() {
            this.viewSparseArrayCompat = new SparseArrayCompat<>();
            this.currentHeaderView = null;
            this.currentHeaderPos = -1;
        }

        @Override
        public void onDrawOver(Canvas canvas, RecyclerView parent, RecyclerView.State state) {
            //Log.d(TAG, "onDrawOver");
            for (int i = 0; i < parent.getChildCount(); i++) {
                final View view = parent.getChildAt(i);
                final int position = parent.getChildAdapterPosition(view);
                if (!isHeader(position)) {
                    //current position
                    final int headerPos = mHeaderLocationMapAbsolute.get(position);
                    View header = viewSparseArrayCompat.get(headerPos);
                    if (header == null) {
                        header = parent.getLayoutManager().findViewByPosition(headerPos);
                        viewSparseArrayCompat.put(headerPos, header);
                    }
                    currentHeaderView = header != null ? header : currentHeaderView;
                    break;
                }
            }
            drawHeader(canvas, currentHeaderView);
        }

        private void drawHeader(Canvas canvas, View view) {
            if (view == null || canvas == null) return;
            canvas.save();
            final int left = view.getLeft();
            final int top = 0;
            canvas.translate(left, top);
            view.setTranslationX(left);
            view.setTranslationY(top);
            view.draw(canvas);
            canvas.restore();
        }

    }

}
