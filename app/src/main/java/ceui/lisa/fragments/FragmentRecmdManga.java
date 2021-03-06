package ceui.lisa.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.google.gson.Gson;
import com.scwang.smartrefresh.layout.footer.FalsifyFooter;

import java.util.ArrayList;
import java.util.List;

import ceui.lisa.R;
import ceui.lisa.activities.RankActivity;
import ceui.lisa.activities.Shaft;
import ceui.lisa.activities.ViewPagerActivity;
import ceui.lisa.adapters.BaseAdapter;
import ceui.lisa.adapters.IAdapter;
import ceui.lisa.adapters.RAdapter;
import ceui.lisa.core.NetControl;
import ceui.lisa.database.AppDatabase;
import ceui.lisa.database.IllustRecmdEntity;
import ceui.lisa.databinding.FragmentRecmdBinding;
import ceui.lisa.databinding.RecyIllustStaggerBinding;
import ceui.lisa.http.NullCtrl;
import ceui.lisa.http.Retro;
import ceui.lisa.interfaces.OnItemClickListener;
import ceui.lisa.model.ListIllust;
import ceui.lisa.models.IllustsBean;
import ceui.lisa.utils.DataChannel;
import ceui.lisa.utils.DensityUtil;
import ceui.lisa.utils.Dev;
import ceui.lisa.utils.Params;
import ceui.lisa.view.LinearItemHorizontalDecoration;
import ceui.lisa.view.SpacesItemDecoration;
import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class FragmentRecmdManga extends NetListFragment<FragmentRecmdBinding,
        ListIllust, IllustsBean, RecyIllustStaggerBinding> {

    private String dataType;
    private RAdapter adapter;
    private List<IllustsBean> ranking = new ArrayList<>();

    public static FragmentRecmdManga newInstance(String dataType) {
        Bundle args = new Bundle();
        args.putString(Params.DATA_TYPE, dataType);
        FragmentRecmdManga fragment = new FragmentRecmdManga();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void initBundle(Bundle bundle) {
        dataType = bundle.getString(Params.DATA_TYPE);
    }

    @Override
    public NetControl<ListIllust> present() {
        return new NetControl<ListIllust>() {
            @Override
            public Observable<ListIllust> initApi() {
                if (Dev.isDev) {
                    return null;
                } else {
                    adapter.clear();
                    if ("漫画".equals(dataType)) {
                        return Retro.getAppApi().getRecmdManga(Shaft.sUserModel.getResponse().getAccess_token());
                    } else {
                        return Retro.getAppApi().getRecmdIllust(Shaft.sUserModel.getResponse().getAccess_token());
                    }
                }
            }

            @Override
            public Observable<ListIllust> initNextApi() {
                return Retro.getAppApi().getNextIllust(Shaft.sUserModel.getResponse().getAccess_token(), nextUrl);
            }
        };
    }

    @Override
    public BaseAdapter<IllustsBean, RecyIllustStaggerBinding> adapter() {
        return new IAdapter(allItems, mContext);
    }

    @Override
    public void initView(View view) {
        super.initView(view);
        baseBind.seeMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, RankActivity.class);
                intent.putExtra("dataType", dataType);
                startActivity(intent);
            }
        });
        baseBind.ranking.addItemDecoration(new LinearItemHorizontalDecoration(DensityUtil.dp2px(8.0f)));
        LinearLayoutManager manager = new LinearLayoutManager(mContext, LinearLayoutManager.HORIZONTAL, false);
        baseBind.ranking.setLayoutManager(manager);
        baseBind.ranking.setHasFixedSize(true);
        PagerSnapHelper snapHelper = new PagerSnapHelper();
        snapHelper.attachToRecyclerView(baseBind.ranking);
        adapter = new RAdapter(ranking, mContext);
        adapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(View v, int position, int viewType) {
                DataChannel.get().setIllustList(ranking);
                Intent intent = new Intent(mContext, ViewPagerActivity.class);
                intent.putExtra("position", position);
                startActivity(intent);
            }
        });
        baseBind.ranking.setAdapter(adapter);
    }

    @Override
    public void initRecyclerView() {
        StaggeredGridLayoutManager layoutManager =
                new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        baseBind.recyclerView.setLayoutManager(layoutManager);
        baseBind.recyclerView.addItemDecoration(new SpacesItemDecoration(DensityUtil.dp2px(8.0f)));
    }


    @Override
    public void initLayout() {
        mLayoutID = R.layout.fragment_recmd;
    }

    @Override
    public String getToolbarTitle() {
        return "推荐" + dataType;
    }

    @Override
    public boolean showToolbar() {
        return "漫画".equals(dataType);
    }

    @Override
    public void firstSuccess() {
        baseBind.topRela.setVisibility(View.VISIBLE);
        Animation animation = new AlphaAnimation(0.0f, 1.0f);
        animation.setDuration(800L);
        baseBind.topRela.startAnimation(animation);
        Observable.create((ObservableOnSubscribe<String>) emitter -> {
            emitter.onNext("开始写入数据库");
            if (allItems != null) {
                if (allItems.size() >= 20) {
                    for (int i = 0; i < 20; i++) {
                        insertViewHistory(allItems.get(i));
                    }
                } else {
                    for (int i = 0; i < allItems.size(); i++) {
                        insertViewHistory(allItems.get(i));
                    }
                }
            }
            emitter.onComplete();
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new NullCtrl<String>() {
                    @Override
                    public void success(String s) {

                    }
                });
        ranking.addAll(mResponse.getRanking_illusts());
        adapter.notifyItemRangeInserted(0, ranking.size());
    }

    private void insertViewHistory(IllustsBean illustsBean) {
        IllustRecmdEntity illustRecmdEntity = new IllustRecmdEntity();
        illustRecmdEntity.setIllustID(illustsBean.getId());
        Gson gson = new Gson();
        illustRecmdEntity.setIllustJson(gson.toJson(illustsBean));
        illustRecmdEntity.setTime(System.currentTimeMillis());
        AppDatabase.getAppDatabase(Shaft.getContext()).recmdDao().insert(illustRecmdEntity);
    }

    @Override
    public void showDataBase() {
        Observable.create((ObservableOnSubscribe<List<IllustRecmdEntity>>) emitter -> {
            List<IllustRecmdEntity> temp = AppDatabase.getAppDatabase(mContext).recmdDao().getAll();
            Thread.sleep(500);
            emitter.onNext(temp);
            emitter.onComplete();
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map(entities -> {
                    Gson gson = new Gson();
                    List<IllustsBean> temp = new ArrayList<>();
                    for (int i = 0; i < entities.size(); i++) {
                        temp.add(gson.fromJson(entities.get(i).getIllustJson(), IllustsBean.class));
                    }
                    return temp;
                })
                .subscribe(new NullCtrl<List<IllustsBean>>() {
                    @Override
                    public void success(List<IllustsBean> illustsBeans) {
                        allItems.addAll(illustsBeans);
                        mAdapter.notifyItemRangeInserted(0, allItems.size());
                    }

                    @Override
                    public void must(boolean isSuccess) {
                        baseBind.refreshLayout.finishRefresh(isSuccess);
                        baseBind.refreshLayout.setRefreshFooter(new FalsifyFooter(mContext));
                    }
                });
    }
}
