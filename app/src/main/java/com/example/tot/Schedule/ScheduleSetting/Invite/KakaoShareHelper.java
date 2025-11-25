package com.example.tot.Schedule.ScheduleSetting.Invite;

import android.content.Context;
import android.util.Log;

import com.kakao.sdk.share.ShareClient;
import com.kakao.sdk.share.model.SharingResult;

import java.util.Map;

import kotlin.Unit;

public class KakaoShareHelper {
    private static final String TAG = "초대 메시지";

    public static void shareCustomTemplate(Context context, long templateId, Map<String, String> templateArgs) {

        if (ShareClient.getInstance().isKakaoTalkSharingAvailable(context)) {

            // 🔥 사용자 정의 템플릿 공유
            ShareClient.getInstance().shareCustom(context, templateId, templateArgs,
                    (SharingResult sharingResult, Throwable error) -> {
                        if (error != null) {
                            Log.e(TAG, "메시지 전송 실패", error);
                        } else if (sharingResult != null) {
                            Log.d(TAG, "메시지 전송 성공: " + sharingResult.getIntent());
                            context.startActivity(sharingResult.getIntent());
                        }
                        return Unit.INSTANCE;
                    });

        } else {
            // 🔥 웹 공유
            Log.e(TAG, "카카오톡 미설치 버전은 웹 공유 필요 (custom template 웹 공유는 별도 구현 필요)");
        }
    }
}
