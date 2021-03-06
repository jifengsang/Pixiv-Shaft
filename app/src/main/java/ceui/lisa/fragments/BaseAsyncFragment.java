package ceui.lisa.fragments;

import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.scwang.smartrefresh.header.DeliveryHeader;
import com.scwang.smartrefresh.layout.api.RefreshLayout;

import java.util.ArrayList;
import java.util.List;

import ceui.lisa.R;
import ceui.lisa.utils.Common;
import ceui.lisa.utils.DensityUtil;
import ceui.lisa.view.LinearItemDecoration;

public abstract class BaseAsyncFragment<Adapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>,
        ListItem> extends BaseFragment {

    public static final int PAGE_SIZE = 20;
    protected Adapter mAdapter;
    protected List<ListItem> allItems = new ArrayList<>();
    protected RecyclerView mRecyclerView;
    protected RefreshLayout mRefreshLayout;
    protected ProgressBar mProgressBar;
    protected Toolbar mToolbar;
    protected ImageView noData;

    @Override
    void initLayout() {
        mLayoutID = R.layout.fragment_illust_list;
    }

    @Override
    View initView(View v) {
        mToolbar = v.findViewById(R.id.toolbar);
        mProgressBar = v.findViewById(R.id.progress);
        noData = v.findViewById(R.id.no_data);
        noData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Common.showLog(className + "点击了一下 nodata");
                getFirstData();
            }
        });
        mRecyclerView = v.findViewById(R.id.recyclerView);
        initRecyclerView();
        mRefreshLayout = v.findViewById(R.id.refreshLayout);
        mRefreshLayout.setRefreshHeader(new DeliveryHeader(mContext));
        mRefreshLayout.setOnRefreshListener(layout -> getFirstData());
        mRefreshLayout.setEnableLoadMore(hasNext());
        if (hasNext()) {
            mRefreshLayout.setOnLoadMoreListener(layout -> getNextData());
        }
        return v;
    }

    protected void initRecyclerView() {
        LinearLayoutManager manager = new LinearLayoutManager(mContext);
        mRecyclerView.setLayoutManager(manager);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.addItemDecoration(new LinearItemDecoration(DensityUtil.dp2px(8.0f)));
    }

    @Override
    void initData() {
        getFirstData();
    }

    public void getNextData() {

    }

    public abstract void getFirstData();

    public void showFirstData() {
        if (showToolbar()) {
            mToolbar.setNavigationOnClickListener(view -> getActivity().finish());
            mToolbar.setTitle(getToolbarTitle());
        } else {
            if (mToolbar != null) {
                mToolbar.setVisibility(View.GONE);
            }
        }
        initAdapter();
        mRecyclerView.setAdapter(mAdapter);
        if (allItems.size() == 0) {
            noData.setVisibility(View.VISIBLE);
            mRecyclerView.setVisibility(View.INVISIBLE);
        } else {
            mRecyclerView.setVisibility(View.VISIBLE);
            noData.setVisibility(View.INVISIBLE);
        }
        mProgressBar.setVisibility(View.INVISIBLE);
        mRefreshLayout.finishRefresh(true);
    }

    public abstract void initAdapter();

    String getToolbarTitle() {
        return " ";
    }

    boolean hasNext() {
        return false;
    }

    boolean showToolbar() {
        return true;
    }

}
