package co.rsk.scoring;

import co.rsk.net.NodeID;
import org.junit.Assert;
import org.junit.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Created by ajlopez on 28/06/2017.
 */
public class PeerScoringManagerTest {
    private static Random random = new Random();

    @Test
    public void getEmptyNodeStatusFromUnknownNodeId() {
        NodeID id = generateNodeID();
        PeerScoringManager manager = new PeerScoringManager();

        PeerScoring result = manager.getPeerScoring(id);

        Assert.assertNotNull(result);
        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void newNodeHasGoodReputation() {
        NodeID id = generateNodeID();
        PeerScoringManager manager = new PeerScoringManager();

        Assert.assertTrue(manager.hasGoodReputation(id));
    }

    @Test
    public void recordEventUsingNodeID() {
        NodeID id = generateNodeID();
        PeerScoringManager manager = new PeerScoringManager();

        manager.recordEvent(id, null, EventType.INVALID_BLOCK);

        PeerScoring result = manager.getPeerScoring(id);

        Assert.assertNotNull(result);
        Assert.assertFalse(result.isEmpty());
        Assert.assertEquals(1, result.getEventCounter(EventType.INVALID_BLOCK));
        Assert.assertEquals(1, result.getTotalEventCounter());
    }

    @Test
    public void newAddressHasGoodReputation() throws UnknownHostException {
        InetAddress address = generateIPAddressV4();
        PeerScoringManager manager = new PeerScoringManager();

        Assert.assertTrue(manager.hasGoodReputation(address));
    }

    @Test
    public void recordEventUsingNodeIDAndAddress() throws UnknownHostException {
        NodeID id = generateNodeID();
        InetAddress address = generateIPAddressV4();

        PeerScoringManager manager = new PeerScoringManager();

        manager.recordEvent(id, address, EventType.INVALID_BLOCK);

        PeerScoring result = manager.getPeerScoring(id);

        Assert.assertNotNull(result);
        Assert.assertFalse(result.isEmpty());
        Assert.assertEquals(1, result.getEventCounter(EventType.INVALID_BLOCK));
        Assert.assertEquals(1, result.getTotalEventCounter());

        result = manager.getPeerScoring(address);

        Assert.assertNotNull(result);
        Assert.assertFalse(result.isEmpty());
        Assert.assertEquals(1, result.getEventCounter(EventType.INVALID_BLOCK));
        Assert.assertEquals(1, result.getTotalEventCounter());
    }

    @Test
    public void recordEventUsingIPV4Address() throws UnknownHostException {
        InetAddress address = generateIPAddressV4();
        PeerScoringManager manager = new PeerScoringManager();

        manager.recordEvent(null, address, EventType.INVALID_BLOCK);

        PeerScoring result = manager.getPeerScoring(address);

        Assert.assertNotNull(result);
        Assert.assertFalse(result.isEmpty());
        Assert.assertEquals(1, result.getEventCounter(EventType.INVALID_BLOCK));
        Assert.assertEquals(1, result.getTotalEventCounter());
    }

    @Test
    public void invalidBlockGivesBadReputationToNode() throws UnknownHostException {
        NodeID id = generateNodeID();
        PeerScoringManager manager = new PeerScoringManager();

        manager.recordEvent(id, null, EventType.INVALID_BLOCK);

        Assert.assertFalse(manager.hasGoodReputation(id));

        Assert.assertNotEquals(0, manager.getPeerScoring(id).getTimeLostGoodReputation());
    }

    @Test
    public void notGoodReputationByNodeIDExpires() throws UnknownHostException, InterruptedException {
        NodeID id = generateNodeID();
        PeerScoringManager manager = new PeerScoringManager();
        manager.setExpirationTime(10);

        manager.recordEvent(id, null, EventType.INVALID_BLOCK);

        Assert.assertEquals(1, manager.getPeerScoring(id).getEventCounter(EventType.INVALID_BLOCK));
        Assert.assertFalse(manager.hasGoodReputation(id));
        Assert.assertNotEquals(0, manager.getPeerScoring(id).getTimeLostGoodReputation());

        Assert.assertFalse(manager.hasGoodReputation(id));
        Assert.assertNotEquals(0, manager.getPeerScoring(id).getTimeLostGoodReputation());
        Assert.assertEquals(1, manager.getPeerScoring(id).getEventCounter(EventType.INVALID_BLOCK));

        TimeUnit.MILLISECONDS.sleep(100);

        Assert.assertTrue(manager.hasGoodReputation(id));
        Assert.assertEquals(0, manager.getPeerScoring(id).getTimeLostGoodReputation());
        Assert.assertEquals(0, manager.getPeerScoring(id).getEventCounter(EventType.INVALID_BLOCK));
        Assert.assertTrue(manager.getPeerScoring(id).isEmpty());
    }

    @Test
    public void notGoodReputationByAddressExpires() throws UnknownHostException, InterruptedException {
        InetAddress address = generateIPAddressV4();
        PeerScoringManager manager = new PeerScoringManager();

        manager.setExpirationTime(10);

        manager.recordEvent(null, address, EventType.INVALID_BLOCK);

        Assert.assertEquals(1, manager.getPeerScoring(address).getEventCounter(EventType.INVALID_BLOCK));
        Assert.assertFalse(manager.hasGoodReputation(address));
        Assert.assertNotEquals(0, manager.getPeerScoring(address).getTimeLostGoodReputation());

        Assert.assertFalse(manager.hasGoodReputation(address));
        Assert.assertNotEquals(0, manager.getPeerScoring(address).getTimeLostGoodReputation());
        Assert.assertEquals(1, manager.getPeerScoring(address).getEventCounter(EventType.INVALID_BLOCK));

        TimeUnit.MILLISECONDS.sleep(100);

        Assert.assertTrue(manager.hasGoodReputation(address));
        Assert.assertEquals(0, manager.getPeerScoring(address).getTimeLostGoodReputation());
        Assert.assertEquals(0, manager.getPeerScoring(address).getEventCounter(EventType.INVALID_BLOCK));
        Assert.assertTrue(manager.getPeerScoring(address).isEmpty());
    }

    @Test
    public void firstPunishment() throws UnknownHostException, InterruptedException {
        InetAddress address = generateIPAddressV4();
        PeerScoringManager manager = new PeerScoringManager();

        manager.setExpirationTime(10);

        manager.recordEvent(null, address, EventType.INVALID_BLOCK);

        Assert.assertEquals(1, manager.getPeerScoring(address).getEventCounter(EventType.INVALID_BLOCK));
        Assert.assertEquals(1, manager.getPeerScoring(address).getPunishmentCounter());
        Assert.assertEquals(10, manager.getPeerScoring(address).getPunishmentTime());
        Assert.assertFalse(manager.hasGoodReputation(address));
    }

    @Test
    public void secondPunishment() throws UnknownHostException, InterruptedException {
        InetAddress address = generateIPAddressV4();
        PeerScoringManager manager = new PeerScoringManager();

        manager.setExpirationTime(10);

        manager.recordEvent(null, address, EventType.INVALID_BLOCK);

        Assert.assertEquals(1, manager.getPeerScoring(address).getEventCounter(EventType.INVALID_BLOCK));
        Assert.assertEquals(0, manager.getPeerScoring(address).getEventCounter(EventType.INVALID_TRANSACTION));
        Assert.assertEquals(10, manager.getPeerScoring(address).getPunishmentTime());
        Assert.assertFalse(manager.hasGoodReputation(address));

        TimeUnit.MILLISECONDS.sleep(100);

        Assert.assertTrue(manager.hasGoodReputation(address));

        manager.recordEvent(null, address, EventType.INVALID_TRANSACTION);

        Assert.assertEquals(0, manager.getPeerScoring(address).getEventCounter(EventType.INVALID_BLOCK));
        Assert.assertEquals(1, manager.getPeerScoring(address).getEventCounter(EventType.INVALID_TRANSACTION));
        Assert.assertEquals(2, manager.getPeerScoring(address).getPunishmentCounter());
        Assert.assertEquals(11, manager.getPeerScoring(address).getPunishmentTime());
        Assert.assertFalse(manager.hasGoodReputation(address));
    }

    @Test
    public void invalidTransactionGivesBadReputationToNode() throws UnknownHostException {
        NodeID id = generateNodeID();
        PeerScoringManager manager = new PeerScoringManager();

        manager.recordEvent(id, null, EventType.INVALID_TRANSACTION);

        Assert.assertFalse(manager.hasGoodReputation(id));

        Assert.assertNotEquals(0, manager.getPeerScoring(id).getTimeLostGoodReputation());
    }

    @Test
    public void invalidBlockGivesBadReputationToAddress() throws UnknownHostException {
        InetAddress address = generateIPAddressV4();
        PeerScoringManager manager = new PeerScoringManager();

        manager.recordEvent(null, address, EventType.INVALID_BLOCK);

        Assert.assertFalse(manager.hasGoodReputation(address));

        Assert.assertNotEquals(0, manager.getPeerScoring(address).getTimeLostGoodReputation());
    }

    @Test
    public void invalidTransactionGivesBadReputationToAddress() throws UnknownHostException {
        InetAddress address = generateIPAddressV4();
        PeerScoringManager manager = new PeerScoringManager();

        manager.recordEvent(null, address, EventType.INVALID_TRANSACTION);

        Assert.assertFalse(manager.hasGoodReputation(address));

        Assert.assertNotEquals(0, manager.getPeerScoring(address).getTimeLostGoodReputation());
    }

    @Test
    public void recordEventUsingIPV6Address() throws UnknownHostException {
        InetAddress address = generateIPAddressV6();
        PeerScoringManager manager = new PeerScoringManager();

        manager.recordEvent(null, address, EventType.INVALID_BLOCK);

        PeerScoring result = manager.getPeerScoring(address);

        Assert.assertNotNull(result);
        Assert.assertFalse(result.isEmpty());
        Assert.assertEquals(1, result.getEventCounter(EventType.INVALID_BLOCK));
        Assert.assertEquals(1, result.getTotalEventCounter());
    }

    private static NodeID generateNodeID() {
        byte[] bytes = new byte[32];

        random.nextBytes(bytes);

        return new NodeID(bytes);
    }

    private static InetAddress generateIPAddressV4() throws UnknownHostException {
        byte[] bytes = new byte[4];

        random.nextBytes(bytes);

        return InetAddress.getByAddress(bytes);
    }

    private static InetAddress generateIPAddressV6() throws UnknownHostException {
        byte[] bytes = new byte[16];

        random.nextBytes(bytes);

        return InetAddress.getByAddress(bytes);
    }
}
