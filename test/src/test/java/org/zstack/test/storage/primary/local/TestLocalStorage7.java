package org.zstack.test.storage.primary.local;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.host.APIAddHostEvent;
import org.zstack.header.host.HostInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.kvm.APIAddKVMHostMsg;
import org.zstack.storage.primary.local.LocalStorageHostRefVO;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig.Capacity;
import org.zstack.test.*;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.data.SizeUnit;

/**
 * 1. use local storage
 * 2. add 2 hosts
 * 3. delete one host
 *
 * confirm the local storage capacity reduced to a half
 *
 * 4. delete another host
 *
 * confirm the local storage capacity is zero
 */
public class TestLocalStorage7 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    LocalStorageSimulatorConfig config;
    long totalSize = SizeUnit.GIGABYTE.toByte(100);

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/localStorage/TestLocalStorage6.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.addSpringConfig("localStorageSimulator.xml");
        deployer.addSpringConfig("localStorage.xml");
        deployer.load();

        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        config = loader.getComponent(LocalStorageSimulatorConfig.class);

        Capacity c = new Capacity();
        c.total = totalSize;
        c.avail = totalSize;

        config.capacityMap.put("host1", c);
        config.capacityMap.put("host2", c);

        deployer.build();
        api = deployer.getApi();
        session = api.loginAsAdmin();
    }
    
	@Test
	public void test() throws ApiSenderException, InterruptedException {
        ClusterInventory cluster = deployer.clusters.get("Cluster1");

        APIAddKVMHostMsg msg = new APIAddKVMHostMsg();
        msg.setName("host1");
        msg.setClusterUuid(cluster.getUuid());
        msg.setManagementIp("127.0.0.1");
        msg.setSession(api.getAdminSession());
        msg.setUsername("root");
        msg.setPassword("password");
        ApiSender sender = api.getApiSender();
        APIAddHostEvent evt = sender.send(msg, APIAddHostEvent.class);
        HostInventory host1 = evt.getInventory();

        msg = new APIAddKVMHostMsg();
        msg.setName("host2");
        msg.setClusterUuid(cluster.getUuid());
        msg.setManagementIp("localhost");
        msg.setSession(api.getAdminSession());
        msg.setUsername("root");
        msg.setPassword("password");
        sender = api.getApiSender();
        evt = sender.send(msg, APIAddHostEvent.class);
        HostInventory host2 = evt.getInventory();

        api.deleteHost(host1.getUuid());

        PrimaryStorageInventory local = deployer.primaryStorages.get("local");
        PrimaryStorageVO lvo = dbf.findByUuid(local.getUuid(), PrimaryStorageVO.class);
        Assert.assertEquals(totalSize, lvo.getCapacity().getTotalCapacity());
        Assert.assertEquals(totalSize, lvo.getCapacity().getAvailableCapacity());
        Assert.assertEquals(totalSize, lvo.getCapacity().getTotalPhysicalCapacity());
        Assert.assertEquals(totalSize, lvo.getCapacity().getAvailablePhysicalCapacity());

        Assert.assertFalse(dbf.isExist(host1.getUuid(), LocalStorageHostRefVO.class));

        api.deleteHost(host2.getUuid());

        Assert.assertFalse(dbf.isExist(host2.getUuid(), LocalStorageHostRefVO.class));

        lvo = dbf.findByUuid(local.getUuid(), PrimaryStorageVO.class);
        Assert.assertEquals(0, lvo.getCapacity().getTotalCapacity());
        Assert.assertEquals(0, lvo.getCapacity().getAvailableCapacity());
        Assert.assertEquals(0, lvo.getCapacity().getTotalPhysicalCapacity());
        Assert.assertEquals(0, lvo.getCapacity().getAvailablePhysicalCapacity());
    }
}
