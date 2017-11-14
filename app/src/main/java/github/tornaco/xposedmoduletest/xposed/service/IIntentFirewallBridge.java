package github.tornaco.xposedmoduletest.xposed.service;

import android.os.RemoteException;

/**
 * Created by guohao4 on 2017/11/1.
 * Email: Tornaco@163.com
 */

public interface IIntentFirewallBridge extends IModuleBridge {
    // API For Firewall.
    boolean checkService(String servicePkgName, int callerUid) throws RemoteException;

    boolean checkBroadcast(String action, int receiverUid, int callerUid) throws RemoteException;
}