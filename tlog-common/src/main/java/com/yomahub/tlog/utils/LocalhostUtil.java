package com.yomahub.tlog.utils;

import cn.hutool.core.net.NetUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.tlog.constant.TLogConstants;

/**
 * 主要用来获取本地host和ip的工具类，带缓存
 * @author Bryan.Zhang
 * @since 1.3.0
 */
public class LocalhostUtil {

    private static String hostIp = TLogConstants.UNKNOWN;

    private static String hostName = TLogConstants.UNKNOWN;

    public static String getHostIp(){
        try{
            if (hostIp.equals(TLogConstants.UNKNOWN)){
                hostIp = NetUtil.getLocalhostStr();
            }
        }catch (Exception e){}
        return hostIp;
    }

    public static String getHostName(){
        try{
            if (hostName.equals(TLogConstants.UNKNOWN)){
                hostName = NetUtil.getLocalHostName();
            }
        }catch (Exception e){}
        return hostName;
    }

}
