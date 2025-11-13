package com.example.tot.Schedule.ScheduleSetting.Invite;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.browser.customtabs.CustomTabsIntent;

import com.kakao.sdk.share.ShareClient;
import com.kakao.sdk.share.WebSharerClient;

import kotlin.Unit;

public class KakaoShareHelper {
    private static final String TAG = "초대 메시지";
    public static void shareCustomTemplate(Context context, String url, long templateId){


        if (ShareClient.getInstance().isKakaoTalkSharingAvailable(context)) {

            // 카카오톡 설치되어 있을 때: 사용자 정의 템플릿 기반 스크랩 공유
            ShareClient.getInstance().shareScrap(context, url, templateId, (sharingResult, error) -> {
                if (error != null) {
                    Log.e(TAG, "메시지 전송 실패", error);
                } else if (sharingResult != null) {
                    Log.d(TAG, "메시지 전송 성공: " + sharingResult.getIntent());
                    context.startActivity(sharingResult.getIntent());
                }
                return Unit.INSTANCE;
            });
        } else {
            // ✅ 카카오톡 미설치 시: 웹 공유
            Uri sharerUrl = WebSharerClient.getInstance().makeScrapUrl(url, templateId);

            try {
                CustomTabsIntent customTabsIntent = new CustomTabsIntent.Builder().build();
                customTabsIntent.launchUrl(context, sharerUrl);
            } catch (ActivityNotFoundException e) {
                Log.e(TAG, "인터넷 브라우저를 찾을 수 없습니다.", e);
            }
        }



    }
}
