package com.het.cbeauty.cameralearndemo.utils;

import android.app.Activity;
import android.content.res.Configuration;

/**
 * ------------------------------------------------
 * Copyright © 2014-2021 CLife. All Rights Reserved.
 * Shenzhen H&T Intelligent Control Co.,Ltd.
 * -----------------------------------------------
 *
 * @author huyongming
 * @version v3.1.0
 * @date 2022/6/24-11:25
 * @annotation ....
 */
public class ScreenUtils {

    public static boolean isPortrait(Activity activity) {
        Configuration cf = activity.getResources().getConfiguration(); //获取设置的配置信息

        int ori = cf.orientation; //获取屏幕方向

        if (ori == cf.ORIENTATION_LANDSCAPE) {
            //横屏
            return false;
        } else if (ori == cf.ORIENTATION_PORTRAIT) {
            //竖屏
            return true;
        }
        return false;
    }
}
