package org.zstack.test.kvm;

import org.junit.Before;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.thread.AsyncThread;
import org.zstack.core.thread.SyncTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.host.APIAddHostEvent;
import org.zstack.header.host.HostInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.image.ImageVO;
import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.kvm.APIAddKVMHostMsg;
import org.zstack.network.service.flat.FlatNetworkServiceSimulatorConfig;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.test.*;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class TestKvm100VmBalancedToMultiHost {
    CLogger logger = Utils.getLogger(TestKvm100VmBalancedToMultiHost.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    FlatNetworkServiceSimulatorConfig fconfig;
    KVMSimulatorConfig kconfig;
    ThreadFacade thdf;
    int total = 10;
    int syncLevel = 1000;
    int timeout = 10000;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/kvm/TestKvm100VmBalancedToMultiHost.xml", con);
        deployer.addSpringConfig("flatNetworkProvider.xml");
        deployer.addSpringConfig("flatNetworkServiceSimulator.xml");
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.build();

        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        fconfig = loader.getComponent(FlatNetworkServiceSimulatorConfig.class);
        kconfig = loader.getComponent(KVMSimulatorConfig.class);
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        thdf = loader.getComponent(ThreadFacade.class);
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        CoreGlobalProperty.VM_TRACER_ON = false;
        L2NetworkInventory l2 = deployer.l2Networks.get("TestL2Network");
        L3NetworkInventory l3 = deployer.l3Networks.get("TestL3Network1");

        final ImageInventory img = deployer.images.get("TestImage");
        ImageVO imgvo = dbf.findByUuid(img.getUuid(), ImageVO.class);
        imgvo.setSize(1);
        dbf.update(imgvo);
        final InstanceOfferingInventory ioinv1 = deployer.instanceOfferings.get("512M1Core");
        final InstanceOfferingInventory ioinv2 = deployer.instanceOfferings.get("2G2Core");
        final CountDownLatch latch = new CountDownLatch(total);
        HostInventory hinv1 = deployer.hosts.get("host1");
        HostInventory hinv2 = deployer.hosts.get("host2");

        // create all vm
        for (int i = 0; i < total; i++) {
            final int finalI = i;
            thdf.syncSubmit(new SyncTask<Object>() {
                @Override
                public String getSyncSignature() {
                    return "creating-vm";
                }

                @Override
                public int getSyncLevel() {
                    return syncLevel;
                }

                @Override
                public String getName() {
                    return getSyncSignature();
                }

                @Override
                public Object call() throws Exception {
                    //createVm(ioinv1.getUuid());
                    createVm(ioinv2.getUuid());
                    return null;
                }

                @AsyncThread
                public void createVm(String ioinvUuid) throws Exception {
                    try {
                        VmCreator creator = new VmCreator(api);
                        creator.addL3Network(l3.getUuid());
                        creator.imageUuid = img.getUuid();
                        creator.name = "vm-" + finalI;
                        creator.timeout = (int) TimeUnit.MINUTES.toSeconds(10);
                        creator.instanceOfferingUuid = ioinvUuid;
                        creator.create();
                    } finally {
                        latch.countDown();
                    }

                }
            });
        }
        latch.await(timeout, TimeUnit.SECONDS);

        //
        List<Tuple> ts = new Callable<List<Tuple>>() {
            @Override
            @Transactional(readOnly = true)
            public List<Tuple> call() {
                String sql = "select count(vm), host.name" +
                        " from VmInstanceVO vm, HostVO host" +
                        " where vm.hostUuid = host.uuid" +
                        " group by host.uuid";
                TypedQuery<Tuple> q = dbf.getEntityManager().createQuery(sql, Tuple.class);
                return q.getResultList();
            }
        }.call();

        for (Tuple t : ts) {
            long num = t.get(0, Long.class);
            String name = t.get(1, String.class);
            System.out.println(String.format("%s: %s", name, num));
        }
    }

}
