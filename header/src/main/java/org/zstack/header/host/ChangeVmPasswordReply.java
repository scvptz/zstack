package org.zstack.header.host;

import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.VmAccountPreference;

/**
 * Created by mingjian.deng on 16/10/19.
 */
public class ChangeVmPasswordReply extends MessageReply {
    private VmAccountPreference vmAccountPreference;

    public VmAccountPreference getVmAccountPreference() { return vmAccountPreference; }

    public void setVmAccountPreference(VmAccountPreference vmAccountPreference) { this.vmAccountPreference = vmAccountPreference; }
}
