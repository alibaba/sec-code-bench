<filename>KanZhiHu-master/app/src/main/java/com/gejiahui/kanzhihu/ui/AnswerActivity.java<fim_prefix>

package com.gejiahui.kanzhihu.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.facebook.drawee.view.SimpleDraweeView;
import com.gejiahui.kanzhihu.R;
import com.gejiahui.kanzhihu.base.BaseActivity;
import com.gejiahui.kanzhihu.model.UserDetail;
import com.gejiahui.kanzhihu.net.Request4UserDetail;
import com.gejiahui.kanzhihu.utils.ThemeUtils;
import com.orhanobut.logger.Logger;
import com.victor.loading.rotate.RotateLoading;

import org.greenrobot.eventbus.EventBus;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by gejiahui on 2016/3/12.
 */
public class AnswerActivity extends BaseActivity {
    @Bind(R.id.answer_activity_title)
    TextView title;
    @Bind(R.id.web_view)
    WebView webView;
    @Bind(R.id.toolbar)
    Toolbar mToolbar;
    @Bind(R.id.user_name)
    TextView userName;
    @Bind(R.id.user_signature)
    TextView userSignature;
    @Bind(R.id.vote)
    TextView voteNumber;
    @Bind(R.id.ans_avatar)
    SimpleDraweeView avatar;
    @Bind(R.id.rotateloading)
    RotateLoading rotateLoading;
    @Bind(R.id.user_info)
    RelativeLayout user_info;
    @Bind(R.id.swipe_refreshLayout)
    SwipeRefreshLayout mSwipeRefreshLayout;
    @Bind(R.id.app_layout)
    AppBarLayout appBarLayout;

    private String answerURL;
    private String userURL;
    private String questionURL;
    private Request4UserDetail request4UserDetail;
    private UserDetail mUserDetail;
    private Document doc = null;
    private Document questionDoc = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_answer_web);
        ButterKnife.bind(this);
        rotateLoading.start();
        mToolbar.setBackgroundColor(ThemeUtils.getThemeColor());
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        answerURL = getIntent().getStringExtra("answerUrl");
        title.setText(getIntent().getStringExtra("title"));
        userURL = getIntent().getStringExtra("userUrl");
        questionURL = getIntent().getStringExtra("questionUrl");
        webViewInit();
        setListener();
        getUserInfo();
        Logger.d("" + answerURL);
        Logger.d("" + userURL);
        mSwipeRefreshLayout.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                new WebLoadingThread().execute();
            }
        });
        new WebLoadingThread().execute();

    }


    private void webViewInit() {

        webView.getSettings().setJavaScriptEnabled(false);
        webView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);

    }

    private void setListener() {
        user_info.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AnswerActivity.this, UserDetailsActivity.class);
                startActivity(intent);
                if (mUserDetail != null) {
                    Logger.d("发出l ");
                    EventBus.getDefault().postSticky(mUserDetail);
                }
            }
        });

        title.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                android.support.v7.app.AlertDialog.Builder builder = new AlertDialog.Builder(AnswerActivity.this);
                View view = LayoutInflater.from(getBaseContext()).inflate(R.layout.dialog_webview, null, false);
                WebView questionWebView = (WebView) view.findViewById(R.id.question_webview);
                questionWebView.getSettings().setJavaScriptEnabled(true);
                questionWebView.loadDataWithBaseURL(null, getHtml(getQuestionDetailBody()), "text/html", "utf-8", null);
                builder.setTitle(title.getText())
                        .setView(view)
                        .setPositiveButton("确定", null)
                        .show();
            }
        });

        appBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                //  Logger.d("verticalOffset"+verticalOffset);
                if(verticalOffset < -80){
                    getSupportActionBar().setDisplayShowTitleEnabled(false);
                    getSupportActionBar().setDisplayHomeAsUpEnabled(false);
                }
                else{
                    getSupportActionBar().setDisplayShowTitleEnabled(true);
                    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                }
            }
        });
    }

    private String getHtml(String body) {
        final StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>");
        sb.append("<html dir=\"ltr\" lang=\"zh\">");
        sb.append("<head><style>img{ max-width: 100%!important;height: auto!important; display: block; margin: 10px 0;}</style>");
        sb.append("<meta name=\"viewport\" content=\"width=100%; initial-scale=1.0; maximum-scale=1.0; user-scalable=0;\" />");
        sb.append("<link rel=\"stylesheet\" href='file:///android_asset/style.css' type=\"text/css\" media=\"screen\" />");
        sb.append("</head>");
        sb.append("<body style=\"padding:0px 8px 8px 8px; word-wrap:break-word;\">");
        sb.append("<div class=\"body\">");
        sb.append(body);
        sb.append("</div>");
        sb.append("</body>");
        sb.append("</html>");
        return sb.toString();
    }

    private void getDocument(String url) {
        try {

<fim_suffix>

            questionDoc = Jsoup.connect(questionURL).get();
            Logger.d(doc.toString());
            getQuestionDetailBody();

        } catch (IOException e) {

        }
    }

    private String getAnswerBody() {
        if (doc != null) {
            Elements content = doc.getElementsByClass("AnswerItem-content");
            if (content != null) {
                for (int i = 0; i < content.size(); i++) {
                    Element element = content.get(i);
                    Elements ans = element.getElementsByClass("RichText");
                    if (ans.size() != 0) {
                        return addPrefixOfUrl(imgReplace(ans.first().toString())); //替换img
                    }
                }
            }
        }
        return "<div class=\"answer-status\" id=\"answer-status\">\n" +
                "<a class=\"zg-right\" data-tip=\"s$b$为什么回答会被建议修改？\" href=\"/question/24752645\"><i class=\"zg-icon zg-icon-question-mark\"></i></a>\n" +
                "<p>回答等待修改（已修改・评估中）：不规范转载</a></p>\n" +
                "<p class=\"note\">\n" +
                "\n" +
                "作者修改内容通过后，回答会重新显示。如果一周内未得到有效修改，回答会自动折叠。\n" +
                "\n" +
                "</p>\n" +
                "</div>";
    }


    private String getQuestionDetailBody() {
        if (questionDoc != null) {
            Elements content = questionDoc.getElementsByClass("QuestionHeader-description");
            if (content != null) {
//                Logger.d(content.toString());
//                return addPrefixOfUrl(imgReplace(content.toString()));
                for (int i = 0; i < content.size(); i++) {
                    Element element = content.get(i);
                    Elements ans = element.getElementsByClass("RichText");
                    if (ans.size() != 0) {
                        return addPrefixOfUrl(imgReplace(ans.first().toString())); //替换img
                    }
                }
            }
        }
        return "";
    }


    /**
     * 将html中从默认的地址换成真正的imgUrl
     *
     * @param html
     * @return
     */
    private String imgReplace(String html) {
        Document doc = Jsoup.parse(html);
        Elements imgs = doc.getElementsByTag("img");
        for (int i = 0; i < imgs.size(); i++) {
            String imgUrl = imgs.get(i).attr("data-actualsrc");
            imgs.get(i).attr("src", imgUrl);
        }
        return doc.toString();
    }

    //将地址拼接完整
    private String addPrefixOfUrl(String html) {
        Document doc = Jsoup.parse(html);
        String prefix = "//link.zhihu.com";
        Elements urls = doc.getElementsByTag("a");
        for (int i = 0; i < urls.size(); i++) {
            String str = urls.get(i).attr("href");
            int m = str.indexOf(prefix);

            if (m == 0) {
                urls.get(i).attr("href", "https:" + str);
            }
        }
        return doc.toString();
    }


    private void getUserInfo() {
        request4UserDetail = new Request4UserDetail(userURL, new Response.Listener<UserDetail>() {
            @Override
            public void onResponse(UserDetail response) {
                mUserDetail = response;
                userName.setText(response.getName());
                userSignature.setText(response.getSignature());
                Logger.d(response.getAvatar());
                avatar.setImageURI(Uri.parse(response.getAvatar()));
                voteNumber.setText(getIntent().getStringExtra("vote"));
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });
        executeRequest(request4UserDetail);
    }

    class WebLoadingThread extends AsyncTask<String, String, String> {
        @Override
        protected String doInBackground(String... params) {
            getDocument(answerURL);
            return getHtml(getAnswerBody());
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            webView.loadDataWithBaseURL("", s, "text/html", "utf-8", null);
            rotateLoading.stop();
            if (mSwipeRefreshLayout.isRefreshing()) {
                mSwipeRefreshLayout.setRefreshing(false);  //刷新失败，停止刷新动画
            }
        }
    }
}
<fim_middle>