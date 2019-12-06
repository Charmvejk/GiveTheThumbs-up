package com.example.expandtextview;

import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expandtextview.activity.AddCityActivity;
import com.example.expandtextview.activity.SpinnerActivity;
import com.example.expandtextview.adapter.CircleAdapter;
import com.example.expandtextview.entity.WeatherEvent;
import com.example.expandtextview.util.CommonUtils;
import com.example.expandtextview.util.RxBus;
import com.example.expandtextview.util.Utils;
import com.example.expandtextview.view.LikePopupWindow;
import com.example.expandtextview.view.OnPraiseOrCommentClickListener;
import com.example.expandtextview.view.ScrollSpeedLinearLayoutManger;
import com.example.expandtextview.view.SpaceDecoration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.reactivex.Observer;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

/**
 * @作者: njb
 * @时间: 2019/7/22 10:53
 * @描述: 仿微信朋友圈文本显示全文与收起
 */
public class MainActivity extends AppCompatActivity implements CircleAdapter.MyClickListener, View.OnClickListener {
    private RecyclerView recyclerView;
    private CircleAdapter circleAdapter;
    private String content = "茫茫的长白大山，浩瀚的原始森林，大山脚下，原始森林环抱中散落着几十户人家的" +
            "一个小山村，茅草房，对面炕，烟筒立在屋后边。在村东头有一个独立的房子，那就是青年点，" +
            "窗前有一道小溪流过。学子在这里吃饭，由这里出发每天随社员去地里干活。干的活要么上山伐" +
            "树，抬树，要么砍柳树毛子开荒种地。在山里，可听那吆呵声：“顺山倒了！”放树谨防回头棒！" +
            "树上的枯枝打到别的树上再蹦回来，这回头棒打人最厉害。";


    private List<String> strings;
    private LikePopupWindow likePopupWindow;
    private int page = 1;
    private EditText etComment;
    private LinearLayout llComment;
    private TextView tvSend;
    private LinearLayout llScroll;
    private int screenHeight;
    private int editTextBodyHeight;
    private int currentKeyboardH;
    private int selectCommentItemOffset;
    private int commentPosition;
    protected final String TAG = this.getClass().getSimpleName();
    CompositeDisposable compositeDisposable;
    private TextView tvCity;
    private ScrollSpeedLinearLayoutManger layoutManger;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
        initData();
        initAdapter();
        setListener();
        initRxBus();
    }

    private void initRxBus() {
        compositeDisposable = new CompositeDisposable();
        RxBus.getInstance().toObservable(WeatherEvent.class)
                .subscribe(new Observer<WeatherEvent>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        compositeDisposable.add(d);
                    }

                    @Override
                    public void onNext(WeatherEvent weatherEvent) {
                        Log.e("weather", weatherEvent.getTemperature()+"-**-"+weatherEvent.getCityName());
                        tvCity.setText(String.format("%s %s", weatherEvent.getCityName(),weatherEvent.getTemperature()));
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    private void setListener() {
        tvSend.setOnClickListener(this);
    }


    private void setViewTreeObserver() {
        final ViewTreeObserver swipeRefreshLayoutVTO = llScroll.getViewTreeObserver();
        swipeRefreshLayoutVTO.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Rect r = new Rect();
                llScroll.getWindowVisibleDisplayFrame(r);
                int statusBarH = Utils.getStatusBarHeight();//状态栏高度
                int screenH = llScroll.getRootView().getHeight();
                if (r.top != statusBarH) {
                    //在这个demo中r.top代表的是状态栏高度，在沉浸式状态栏时r.top＝0，通过getStatusBarHeight获取状态栏高度
                    r.top = statusBarH;
                }
                int keyboardH = screenH - (r.bottom - r.top);
                Log.d(TAG, "screenH＝ " + screenH + " &keyboardH = " + keyboardH + " &r.bottom=" + r.bottom + " &top=" + r.top + " &statusBarH=" + statusBarH);

                if (keyboardH == currentKeyboardH) {//有变化时才处理，否则会陷入死循环
                    return;
                }
                currentKeyboardH = keyboardH;
                screenHeight = screenH;//应用屏幕的高度
                editTextBodyHeight = llComment.getHeight();
                if (keyboardH < 150) {//说明是隐藏键盘的情况
                    MainActivity.this.updateEditTextBodyVisible(View.GONE);
                    return;
                }
            }
        });
    }

    /**
     * 初始化控件
     */
    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        llComment = findViewById(R.id.ll_comment);
        etComment = findViewById(R.id.et_comment);
        tvSend = findViewById(R.id.tv_send_comment);
        llScroll = findViewById(R.id.ll_scroll);
        tvCity = findViewById(R.id.tv_city);

        etComment.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (etComment.getText().length() == 500) {
                    Toast.makeText(MainActivity.this,getResources().getString(R.string.the_content_of_the_comment_cannot_exceed_500_words),Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
    }

    /**
     * 初始化数据
     * @param
     */
    private void initData() {
        strings = new ArrayList<>();
        for (int i = 0; i < 14; i++) {
            strings.add(content);
        }
    }

    /**
     * 设置adapter
     */
    private void initAdapter() {
        circleAdapter = new CircleAdapter(this, strings, this);
        layoutManger = new ScrollSpeedLinearLayoutManger(this);
        recyclerView.setLayoutManager(layoutManger);
        layoutManger.setSpeedSlow();
        recyclerView.addItemDecoration(new SpaceDecoration(this));
        recyclerView.setAdapter(circleAdapter);
        recyclerView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (llComment.getVisibility() == View.VISIBLE) {
                    updateEditTextBodyVisible(View.GONE);
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    public void onClick(int position, final View v) {
        final int itemBottomY = getCoordinateY(v) + v.getHeight();//item 底部y坐标
        if (likePopupWindow == null) {
            likePopupWindow = new LikePopupWindow(this, 0);
        }
        likePopupWindow.setOnPraiseOrCommentClickListener(new OnPraiseOrCommentClickListener() {
            @Override
            public void onPraiseClick(int position) {
                Toast.makeText(MainActivity.this, "点赞成功", Toast.LENGTH_SHORT).show();
                likePopupWindow.dismiss();
               // startActivity(new Intent(MainActivity.this, AddCityActivity.class));
                startActivity(new Intent(MainActivity.this, SpinnerActivity.class));
            }

            @Override
            public void onCommentClick(int position) {
                llComment.setVisibility(View.VISIBLE);
                etComment.requestFocus();
                CommonUtils.showSoftInput(MainActivity.this, llComment);
                likePopupWindow.dismiss();
                etComment.setHint("说点什么");
                etComment.setText("");


                v.postDelayed(() -> {
                    int y = getCoordinateY(llComment);
                    //评论时滑动到对应item底部和输入框顶部对齐
                    recyclerView.smoothScrollBy(0,itemBottomY-y);
                }, 300);

            }

            @Override
            public void onClickFrendCircleTopBg() {

            }

            @Override
            public void onDeleteItem(String id, int position) {

            }
        }).setTextView(0).setCurrentPosition(position);
        if (likePopupWindow.isShowing()) {
            likePopupWindow.dismiss();
        } else {
            likePopupWindow.showPopupWindow(v);
        }
    }

    public void updateEditTextBodyVisible(int visibility) {
        llComment.setVisibility(visibility);
        if (View.VISIBLE == visibility) {
            llComment.requestFocus();
            //弹出键盘
            CommonUtils.showSoftInput(etComment.getContext(), etComment);

        } else if (View.GONE == visibility) {
            //隐藏键盘
            CommonUtils.hideSoftInput(etComment.getContext(), etComment);
        }
    }


    /**
     * 获取控件左上顶点Y坐标
     *
     * @param view
     * @return
     */
    private int getCoordinateY(View view) {
        int[] coordinate = new int[2];
        view.getLocationOnScreen(coordinate);
        return coordinate[1];
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.tv_send_comment:
                if (TextUtils.isEmpty(etComment.getText().toString())) {
                    Toast.makeText(MainActivity.this, "请输入评论内容", Toast.LENGTH_SHORT).show();
                    return;
                }
                setViewTreeObserver();
                break;
            default:
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //取消订阅
        RxBus.rxBusUnbund(compositeDisposable);
    }
}
