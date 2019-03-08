package com.example.newbiechen.ireader.presenter;

import com.blankj.ALog;
import com.example.newbiechen.ireader.model.bean.BookChapterBean;
import com.example.newbiechen.ireader.model.bean.BookDetailBean;
import com.example.newbiechen.ireader.model.bean.BookRecordBean;
import com.example.newbiechen.ireader.model.bean.CollBookBean;
import com.example.newbiechen.ireader.model.local.BookRepository;
import com.example.newbiechen.ireader.model.remote.RemoteRepository;
import com.example.newbiechen.ireader.presenter.contract.BookDetailContract;
import com.example.newbiechen.ireader.ui.base.RxPresenter;
import com.example.newbiechen.ireader.utils.LogUtils;
import com.example.newbiechen.ireader.utils.MD5Utils;
import com.example.newbiechen.ireader.utils.RxUtils;

import java.util.List;

import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by newbiechen on 17-5-4.
 */

public class BookDetailPresenter extends RxPresenter<BookDetailContract.View>
        implements BookDetailContract.Presenter{
    private static final String TAG = "BookDetailPresenter";
    private String bookId;

    @Override
    public void refreshBookDetail(String bookId) {
        ALog.dTag(TAG);
        this.bookId = bookId;
        refreshBook();
        refreshComment();
        refreshRecommend();
    }

    @Override
    public void addToBookShelf(CollBookBean collBookBean)  {
        ALog.dTag(TAG);

        Disposable disposable = RemoteRepository.getInstance()
                .getBookChapters(collBookBean.get_id())
                .subscribeOn(Schedulers.io())
                .doOnSubscribe(
                        (d) -> mView.waitToBookShelf() //等待加载
                )
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        beans -> {
                            //设置 id
                            for(BookChapterBean bean :beans){
                                bean.setId(MD5Utils.strToMd5By16(bean.getLink()));
                            }

                            //设置目录
                            collBookBean.setBookChapters(beans);

                            ALog.dTag(TAG);
                            //存储收藏
                            BookRepository.getInstance()
                                    .saveCollBookWithAsync(collBookBean);

                            ALog.dTag(TAG);
                            mView.succeedToBookShelf();
                        }
                        ,
                        e -> {
                            mView.errorToBookShelf();
                            LogUtils.e(e);
                        }
                );
        addDisposable(disposable);

        ALog.dTag(TAG,"Collected = " + BookRepository.getInstance().getCollBook(collBookBean.get_id()));
    }

    private void refreshBook(){
        ALog.dTag(TAG);
        RemoteRepository
                .getInstance()
                .getBookDetail(bookId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<BookDetailBean>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        ALog.dTag(TAG);
                        addDisposable(d);
                    }

                    @Override
                    public void onSuccess(BookDetailBean value){
                        ALog.dTag(TAG);
                        mView.finishRefresh(value);
                        mView.complete();

                        ALog.dTag(TAG,"Collected = " + BookRepository.getInstance().getCollBook(value.get_id()));
                    }

                    @Override
                    public void onError(Throwable e) {
                        mView.showError();
                    }
                });
    }

    private void refreshComment(){
        ALog.dTag(TAG);
        Disposable disposable = RemoteRepository
                .getInstance()
                .getHotComments(bookId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        (value) -> mView.finishHotComment(value)
                );
        addDisposable(disposable);
    }

    private void refreshRecommend(){
        ALog.dTag(TAG);
        Disposable disposable = RemoteRepository
                .getInstance()
                .getRecommendBookList(bookId,3)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        (value) -> mView.finishRecommendBookList(value)
                );
        addDisposable(disposable);
    }
}
