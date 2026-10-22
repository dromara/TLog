package com.yomahub.tlog.utils;

import cn.hutool.core.util.StrUtil;
import com.yomahub.tlog.constant.TLogConstants;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

/**
 * 主要用来获取本地host和ip的工具类，带缓存
 * <p>
 * 获取失败时返回 {@link TLogConstants#UNKNOWN}，并在下次调用时重试，不会抛出异常
 *
 * @author Bryan.Zhang
 * @since 1.3.0
 */
public class LocalhostUtil {

    private static volatile String hostIp;

    private static volatile String hostName;

    /**
     * 用户可手动指定主机名（例如容器环境下使用 Pod 名），优先级最高
     */
    public static String localhostName;

    public static String getHostIp() {
        String ip = hostIp;
        if (ip == null) {
            InetAddress address = getLocalhost();
            ip = address == null ? null : address.getHostAddress();
            if (StrUtil.isBlank(ip)) {
                return TLogConstants.UNKNOWN;
            }
            hostIp = ip;
        }
        return ip;
    }

    public static String getHostName() {
        if (StrUtil.isNotBlank(localhostName)) {
            return localhostName;
        }
        String name = hostName;
        if (name == null) {
            InetAddress address = getLocalhost();
            if (address == null) {
                return TLogConstants.UNKNOWN;
            }
            name = address.getHostName();
            if (StrUtil.isBlank(name)) {
                name = address.getHostAddress();
            }
            if (StrUtil.isBlank(name)) {
                return TLogConstants.UNKNOWN;
            }
            hostName = name;
        }
        return name;
    }

    /**
     * 选择本机地址：优先非站点本地（公网）IPv4，其次站点本地 IPv4，最后回退到 {@link InetAddress#getLocalHost()}
     */
    private static InetAddress getLocalhost() {
        InetAddress siteLocal = null;
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces != null && interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (!isUsable(networkInterface)) {
                    continue;
                }
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if (!(address instanceof Inet4Address) || address.isLoopbackAddress()) {
                        continue;
                    }
                    if (!address.isSiteLocalAddress()) {
                        return address;
                    }
                    if (siteLocal == null) {
                        siteLocal = address;
                    }
                }
            }
        } catch (Exception ignored) {
            // 网卡枚举失败时走兜底逻辑
        }
        if (siteLocal != null) {
            return siteLocal;
        }
        try {
            return InetAddress.getLocalHost();
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean isUsable(NetworkInterface networkInterface) {
        try {
            return networkInterface.isUp() && !networkInterface.isLoopback() && !networkInterface.isVirtual();
        } catch (Exception e) {
            return false;
        }
    }
}
