package org.zstack.test.compute.hostallocator;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.compute.cluster.ClusterSystemTags;
import org.zstack.compute.host.HostSystemTags;
import org.zstack.compute.zone.ZoneSystemTags;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.allocator.APIGetCpuMemoryCapacityReply;
import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.host.HostInventory;
import org.zstack.header.tag.TagInventory;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.kvm.KVMGlobalConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;

import java.util.Arrays;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

/**
 * 1. set ZoneTag.HOST_RESERVED_CPU_CAPACITY, ClusterTag.HOST_RESERVED_CPU_CAPACITY, HostTag.RESERVED_CPU_CAPACITY, KvmGlobalConfig.RESERVED_CPU_CAPACITY
 *
 * confirm getCpuMemoryCapacity returns right capacity
 */
@Deprecated
public class TestGetCpuMemoryCapacity2 {
    Deployer deployer;
    Api api; 
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/hostAllocator/TestReservedHostCapacity.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() throws ApiSenderException {
        ZoneInventory zone = deployer.zones.values().iterator().next();
        ClusterInventory cluster = deployer.clusters.values().iterator().next();
        HostInventory host = deployer.hosts.values().iterator().next();

        KVMGlobalConfig.RESERVED_CPU_CAPACITY.updateValue(1);
        TagInventory ztag = ZoneSystemTags.HOST_RESERVED_CPU_CAPACITY.createTag(zone.getUuid(), map(e("capacity", 10)));
        TagInventory ctag = ClusterSystemTags.HOST_RESERVED_CPU_CAPACITY.createTag(cluster.getUuid(), map(e("capacity", 100)));
        TagInventory htag = HostSystemTags.RESERVED_CPU_CAPACITY.createTag(host.getUuid(), map(e("capacity", 1000)));

        // host tag takes effect
        APIGetCpuMemoryCapacityReply reply = api.retrieveHostCapacity(Arrays.asList(zone.getUuid()), null, null);
        Assert.assertEquals(reply.getAvailableCpu(), reply.getTotalCpu() - 1000);
        api.deleteTag(htag.getUuid());

        // cluster tag takes effect
        reply = api.retrieveHostCapacity(Arrays.asList(zone.getUuid()), null, null);
        Assert.assertEquals(reply.getAvailableCpu(), reply.getTotalCpu() - 100);
        api.deleteTag(ctag.getUuid());

        // zone tag takes effect
        reply = api.retrieveHostCapacity(null, Arrays.asList(cluster.getUuid()), null);
        Assert.assertEquals(reply.getAvailableCpu(), reply.getTotalCpu() - 10);
        api.deleteTag(ztag.getUuid());

        // kvm global config takes effect
        reply = api.retrieveHostCapacity(null, null, Arrays.asList(host.getUuid()));
        Assert.assertEquals(reply.getAvailableCpu(), reply.getTotalCpu() - 1);
    }
}
