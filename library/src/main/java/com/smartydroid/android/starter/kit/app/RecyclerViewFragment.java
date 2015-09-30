/**
 * Created by YuGang Yang on September 25, 2015.
 * Copyright 2007-2015 Laputapp.com. All rights reserved.
 */
package com.smartydroid.android.starter.kit.app;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import com.carlosdelachica.easyrecycleradapters.adapter.EasyRecyclerAdapter;
import com.smartydroid.android.starter.kit.R;
import com.smartydroid.android.starter.kit.contracts.Pagination.Paginator;
import com.smartydroid.android.starter.kit.model.dto.DataArray;
import com.smartydroid.android.starter.kit.model.entity.Entitiy;
import com.smartydroid.android.starter.kit.network.callback.PaginationCallback;
import com.smartydroid.android.starter.kit.utilities.RecyclerViewHandler;
import com.smartydroid.android.starter.kit.utilities.ViewHandler;
import com.smartydroid.android.starter.kit.utilities.ViewUtils;
import com.smartydroid.android.starter.kit.widget.LoadMoreView;
import com.smartydroid.android.starter.kit.widget.LoadingLayout;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

public abstract class RecyclerViewFragment<E extends Entitiy> extends BaseFragment
    implements PaginationCallback<E>, LoadingLayout.OnButtonClickListener,
    SwipeRefreshLayout.OnRefreshListener, ViewHandler.OnScrollBottomListener, View.OnClickListener {

  private LoadingLayout mLoadingLayout;
  private LoadMoreView mLoadMoreView; // TODO loadmore 和 loading layout 是否可以合并成一个？

  private SwipeRefreshLayout mSwipeRefreshLayout;
  private RecyclerView mRecyclerView;

  private EasyRecyclerAdapter mRecyclerAdapter;

  private Paginator<E> mPagePaginator;
  private RecyclerViewHandler mRecyclerViewHandler;

  public abstract void bindViewHolders(EasyRecyclerAdapter adapter);
  public abstract Paginator buildPaginator();

  @Override public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    mPagePaginator = buildPaginator();

    mLoadMoreView = new LoadMoreView();
    mRecyclerViewHandler = new RecyclerViewHandler();
  }

  @Override protected int getFragmentLayout() {
    return R.layout.content_recycler_view;
  }

  @Override public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    mLoadingLayout = ViewUtils.getView(view, R.id.container_loading_layout);
    mSwipeRefreshLayout = mLoadingLayout.getContentView();
    mRecyclerView = ViewUtils.getView(mSwipeRefreshLayout, android.R.id.list);
    mSwipeRefreshLayout.setOnRefreshListener(this);

    setupEmptyView();
    initRecyclerView();
    updateView();
  }

  @Override public void onResume() {
    super.onResume();
    if (!mPagePaginator.dataHasLoaded()) {
      mPagePaginator.refresh();
    }
  }

  @Override public void onPause() {
    super.onPause();
    if (mPagePaginator.isLoading()) {
      mPagePaginator.cancel();
    }
  }

  @Override public void onDestroyView() {
    super.onDestroyView();
  }

  /**
   * setup
   */
  private void initRecyclerView() {
    mRecyclerAdapter = new EasyRecyclerAdapter(getContext());
    bindViewHolders(mRecyclerAdapter);

    if (!setupRecyclerView()) {
      mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
    }

    mRecyclerViewHandler.handleSetAdapter(mRecyclerView, mRecyclerAdapter, mLoadMoreView, this);
    mRecyclerViewHandler.setOnScrollBottomListener(mRecyclerView, this);
  }

  protected boolean setupRecyclerView() {
    return false;
  }

  private void updateView() {
    if (!isEmpty()) {
      mLoadingLayout.showContentView();
    } else if (!mPagePaginator.dataHasLoaded()) {
      mLoadingLayout.showLoadingView();
    } else if (mPagePaginator.hasError()) {
      mLoadingLayout.showErrorView();
    } else {
      mLoadingLayout.showEmptyView();
    }
  }

  public boolean isEmpty() {
    return mPagePaginator.isEmpty();
  }

  @Override public void onRefresh() {
    if (!mPagePaginator.isLoading()) {
      mPagePaginator.refresh();
    } else {
      mSwipeRefreshLayout.setRefreshing(false);
    }
  }

  @Override public void respondSuccess(DataArray<E> data) {
    mRecyclerAdapter.addAll(mPagePaginator.items());
  }

  @Override public void respondWithError(Throwable error) {
    if (error instanceof UnknownHostException) { // 网络问题
      mLoadingLayout.setErrorTitle("网络问题");
      mLoadingLayout.setErrorSubtitle("网络问题");
      mLoadingLayout.setErrorButtonText("重试");
      mLoadingLayout.setOnButtonClickListener(this);
      return;
    }

    if (error instanceof SocketTimeoutException) {
      // 网络链接超时
      mLoadMoreView.showFailure(error);
    }

    mLoadingLayout.setErrorTitle("网络问题");
    mLoadingLayout.setErrorSubtitle("网络问题");
    mLoadingLayout.setErrorButtonText("重试");
    mLoadingLayout.setOnButtonClickListener(this);
  }

  @Override public void onStart() {
    super.onStart();

    if (isEmpty()) {
      mLoadingLayout.showLoadingView();
    } else {
      mLoadMoreView.showLoading();
    }
  }

  @Override public void onFinish() {
    if (mPagePaginator.isRefresh()) {
      mSwipeRefreshLayout.setRefreshing(false);
    }
    updateView();
  }

  public void setupEmptyView() {
    mLoadingLayout.setErrorTitle("没有数据");
    mLoadingLayout.setErrorSubtitle("没有数据");
    mLoadingLayout.setErrorButtonText("重试");
    mLoadingLayout.setOnButtonClickListener(this);
  }

  /**
   * load more click
   */
  @Override public void onClick(View v) {
    mPagePaginator.loadMore();
  }

  @Override public void onEmptyButtonClick(View view) {
    mPagePaginator.refresh();
  }

  @Override public void onErrorButtonClick(View view) {
    mPagePaginator.refresh();
  }

  @Override public void onScorllBootom() {
    if (mPagePaginator.canLoadMore()) {
      mPagePaginator.loadMore();
    }
  }
}
