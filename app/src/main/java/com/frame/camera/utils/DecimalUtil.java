package com.frame.camera.utils;

import java.text.DecimalFormat;

public class DecimalUtil {
    /**
     * 不够位数的在前面补0，保留num位字符串
     * @param code
     * @return
     */
    public static String autoGenericCode(String code, int num, String id) {
        String result = "";
        result = String.format("%0" + num + "d", Integer.parseInt(code) + Integer.parseInt(id));

        return result;
    }

    //保留n位小数，不够在后边补0
    public static String saveDecimalDigit(String number) {
        return new DecimalFormat("#,##0.0000000").format(Double.valueOf(number));
    }
}
