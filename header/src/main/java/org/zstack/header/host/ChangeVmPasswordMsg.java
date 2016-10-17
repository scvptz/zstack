package org.zstack.header.host;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.vm.VmAccountPreference;

/**
 * Created by mingjian.deng on 16/10/18.
 */
public class ChangeVmPasswordMsg extends NeedReplyMessage implements HostMessage {
    private String hostUuid;
    private VmAccountPreference accountPreference;

    public VmAccountPreference getAccountPreference() { return accountPreference; }

    public void setAccountPreference(VmAccountPreference accountPreference) { this.accountPreference = accountPreference; }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }
    @Override
    public String getHostUuid() {
        return hostUuid;
    }
}
