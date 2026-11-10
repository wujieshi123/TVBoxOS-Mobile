package com.github.tvbox.osc.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.base.BaseVbFragment;
import com.github.tvbox.osc.cache.RoomDataManger;
import com.github.tvbox.osc.cache.VodCollect;
import com.github.tvbox.osc.databinding.FragmentHomeBinding;
import com.github.tvbox.osc.server.ControlManager;
import com.github.tvbox.osc.ui.activity.CollectActivity;
import com.github.tvbox.osc.ui.activity.DetailActivity;
import com.github.tvbox.osc.ui.activity.FastSearchActivity;
import com.github.tvbox.osc.ui.activity.HistoryActivity;
import com.github.tvbox.osc.ui.adapter.CollectAdapter;
import com.github.tvbox.osc.util.FastClickCheckUtil;
import com.owen.tvrecyclerview.widget.V7GridLayoutManager;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends BaseVbFragment<FragmentHomeBinding> {

    private CollectAdapter collectAdapter;

    @Override
    protected void init() {
        // 保持服务开启，以免影响其他功能
        ControlManager.get().startServer();

        // 保留原有搜索和历史点击事件
        mBinding.search.setOnClickListener(view -> jumpActivity(FastSearchActivity.class));
        mBinding.ivHistory.setOnClickListener(view -> jumpActivity(HistoryActivity.class));
        
        // 移除原来的 LoadSir，因为不需要网络加载状态了
        // setLoadSir(mBinding.contentLayout); 

        initView();
        initCollectList();
    }

    private void initView() {
        mBinding.rvCollect.setHasFixedSize(true);
        // 3列网格
        mBinding.rvCollect.setLayoutManager(new V7GridLayoutManager(this.mContext, 3));
        
        collectAdapter = new CollectAdapter();
        mBinding.rvCollect.setAdapter(collectAdapter);

        // 点击跳转
        collectAdapter.setOnItemClickListener((adapter, view, position) -> {
            FastClickCheckUtil.check(view);
            VodCollect vodInfo = collectAdapter.getData().get(position);
            if (vodInfo != null) {
                if (ApiConfig.get().getSource(vodInfo.sourceKey) != null) {
                    Bundle bundle = new Bundle();
                    bundle.putString("id", vodInfo.vodId);
                    bundle.putString("sourceKey", vodInfo.sourceKey);
                    jumpActivity(DetailActivity.class, bundle);
                } else {
                    Intent newIntent = new Intent(mContext, FastSearchActivity.class);
                    newIntent.putExtra("title", vodInfo.name);
                    newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(newIntent);
                }
            }
        });

        // 长按删除
        collectAdapter.setOnItemLongClickListener((adapter, view, position) -> {
            VodCollect vodInfo = collectAdapter.getData().get(position);
            if (vodInfo != null) {
                collectAdapter.remove(position);
                RoomDataManger.deleteVodCollect(vodInfo.getId());
            }
            return true;
        });
    }

    private void initCollectList() {
        List<VodCollect> allVodRecord = RoomDataManger.getAllVodCollect();
        List<VodCollect> vodInfoList = new ArrayList<>(allVodRecord);
        // 如果没收藏，直接传空列表，界面就是白板
        collectAdapter.setNewData(vodInfoList);
    }

    @Override
    public void onResume() {
        super.onResume();
        // 每次回到首页刷新一次，确保取消了收藏能立刻反映
        if (collectAdapter != null) {
            initCollectList();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ControlManager.get().stopServer();
    }
}
