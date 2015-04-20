package com.mygdx.handlers.net;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import com.mygdx.entities.Enemy;
import com.mygdx.entities.Entity;
import com.mygdx.entities.Tower;
import com.mygdx.game.MyGame;
import com.mygdx.handlers.QueueCallback;
import com.mygdx.handlers.ThreadSafeList;
import com.mygdx.handlers.action.Action;
import com.mygdx.handlers.action.ActionCreateWave;
import com.mygdx.handlers.action.ActionEnemyCreate;
import com.mygdx.handlers.action.ActionEnemyDestroy;
import com.mygdx.handlers.action.ActionEnemyEnd;
import com.mygdx.handlers.action.ActionHealthChanged;
import com.mygdx.handlers.action.ActionHostPause;
import com.mygdx.handlers.action.ActionPlayerWaveReady;
import com.mygdx.handlers.action.ActionPlayersReady;
import com.mygdx.handlers.action.ActionTowerPlaced;
import com.mygdx.handlers.action.ActionTowerUpgraded;
import com.mygdx.handlers.action.ActionWaitForReady;
import com.mygdx.handlers.action.ActionWaveEnd;
import com.mygdx.handlers.action.ActionWaveStart;
import com.mygdx.net.ConnectionMode;
import com.mygdx.net.EnemyStatus;
import com.mygdx.net.EntityStatus;
import com.mygdx.net.GameConnection;
import com.mygdx.net.NetworkInterface;
import com.mygdx.net.PlayerStatus;
import com.mygdx.net.TowerStatus;
import com.mygdx.states.GameState;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Abstraction away from low-level networking for use in the game states.
 */
public class NetworkManager implements QueueCallback
{
    public static final int SERVER_TCP_PORT = 0xDDD;
    public static final int SERVER_UDP_PORT = 0xDDE;
    public static final int MAX_CLIENTS = 4;
    public static final int ENTITY_ID_START = 0;

    protected AtomicBoolean initialized;
    protected Boolean ready;
    protected boolean initializeManager;
    protected boolean singleplayer = false;

    // server
    protected Boolean isServer;
    protected Server server;
    protected int uidCounter = 1; // All connections have UID >= 1
    protected ArrayList<GameConnection> connections;
    protected int expectedAmountClients;

    protected int lastEntityID = ENTITY_ID_START;
    protected Map<Integer, EntityStatus> entityStatus;
    protected int currentWave = 1;
    protected Map<Integer, PlayerStatus> playerStatus;
    protected int lastPlayerID;
    protected boolean gameStarted;
    protected boolean waitingForLobby;
    protected boolean serverWaveReady = false;

    // client
    protected Client client;

    protected boolean validated = false;
    protected int playerID = -1;

    // threading stuff
    protected ReentrantReadWriteLock mutex;

    // serverstate that will eventually be moved to a new class
    protected int health = 10; // should be in MyGame maybe, so client can also load?
    protected int numEnemies = 0;
    protected boolean waveRunning = false;
    protected int lastUID = 0; // this represents the connectionID of the 'last' screen

    // ConnectionManager now facilitates details of Connection
    private ConnectionManager connectionManager;

    protected QueueManager queueManager;

    protected AtomicInteger queueStatus;

    protected boolean remoteChangesReady;

    public NetworkManager(HashMap<ConnectionMode, NetworkInterface> modes)
    {
        initialized = new AtomicBoolean(false);
        ready = false;
        initializeManager = false;
        expectedAmountClients = MAX_CLIENTS;

        connectionManager = new ConnectionManager(modes, MAX_CLIENTS, this);

        lastPlayerID = 1;

        playerStatus = new HashMap<>();
        playerStatus.put(0, PlayerStatus.SELF);
        gameStarted = false;

        // this is the closest to a traditional mutex I could find.
        // allows multiple reads at one time while only allowing one write lock.
        mutex = new ReentrantReadWriteLock(true);
        queueManager = new QueueManager(this);
    }

    public synchronized void setSingleplayer(boolean value)
    {
        singleplayer = value;
    }

    public boolean getSingleplayer()
    {
        return singleplayer;
    }

    public void setInitializeFlag(boolean flag)
    {
        mutex.writeLock().lock();
        try
        {
            initializeManager = flag;
        }
        finally
        {
            mutex.writeLock().unlock();
        }
    }

    public boolean initialize()
    {
        System.out.println("NET: Initializing NetworkManager");

        if(isServer == null)
        {
            initialized.set(false);
            return false;
        }

        // this can't be null so ignore any warnings that it can be.
        if (isServer)
        {
            // setup class members for server stuff.
            connections = new ArrayList<>();

            server = new Server();
            server.addListener(connectionManager);

            entityStatus = new HashMap<>();
        }

        else
        {
            client = new Client();
            client.addListener(connectionManager);
        }

        boolean connectionInitStatus = connectionManager.initConnection(client, server);

        initialized.set(connectionInitStatus);
        return connectionInitStatus;

    }

    public boolean isInitialized()
    {
        mutex.readLock().lock();
        try
        {
            return initialized.get();
        }
        finally
        {
            mutex.readLock().unlock();
        }
    }

    public void setExpectedAmountClients(int amount)
    {
        expectedAmountClients = amount;
    }

    public int getExpectedAmountClients()
    {
        mutex.readLock().lock();
        try
        {
            return expectedAmountClients;
        }
        finally
        {
            mutex.readLock().unlock();
        }
    }

    public synchronized int getPlayerID()
    {
        return playerID;
    }

    /**
     * When an object implementing interface <code>Runnable</code> is used
     * to create a thread, starting the thread causes the object's
     * <code>run</code> method to be called in that separately executing
     * thread.
     * <p/>
     * The general contract of the method <code>run</code> is that it may
     * take any action whatsoever.
     *
     * @see Thread#run()
     */
    @Override
    public void run()
    {
        // wait for initialization
        while (!isInitialized())
        {
            if(initializeManager)
            {
                initialize();
            }

            // sleep for 5 milliseconds
            try
            {
                Thread.sleep(50);
            }
            catch (InterruptedException e)
            {
                // not really sure how to handle this.
                // terminate?
                System.out.println("NET: Interrupt caught while waiting for initialization.");
                return;
            }
        }

        boolean run = true;

        while (run)
        {
            if (!ready)
            {
                // register network packet classes with KryoNet
                registerPackets();

                if (isServer)
                {
                    mutex.writeLock().lock();
                    try
                    {
                        run = setupServer();
                        if (!run)
                        {
                            continue;
                        }
                    }
                    finally
                    {
                        mutex.writeLock().unlock();
                    }
                }
                else
                {
                    mutex.writeLock().lock();
                    try
                    {
                        run = setupClient();
                        if (!run)
                        {
                            continue;
                        }
                    }
                    finally
                    {
                        mutex.writeLock().unlock();
                    }
                }

                // if it's made it this far, everything should be ready.
                ready = true;
            }

            sendLocalChanges();

            if (isServer)
            {
                // server is sending packets out and waiting for incoming connections.
                runServer();
            }
            else
            {
                // client is waiting for packets to come in.
                runClient();
            }
        }
    }

    public int getFirstClientID()
    {
        for(Integer player : playerStatus.keySet())
        {
            if(playerStatus.get(player) == PlayerStatus.PLAYER)
            {
                return player;
            }
        }

        // special case for single player
        return 0;
    }

    public void registerPackets()
    {
        Kryo kryo = null;

        if(isServer)
        {
            kryo = server.getKryo();
        }
        else
        {
            kryo = client.getKryo();
        }

        kryo.register(GameConnection.ServerAuth.class);
        kryo.register(GameConnection.ClientAuth.class);
        kryo.register(GameConnection.PlayerID.class);

        kryo.register(Action.ActionClass.class);
        kryo.register(Entity.Type.class);
        kryo.register(ActionCreateWave.class);
        kryo.register(ActionEnemyCreate.class);
        kryo.register(ActionEnemyDestroy.class);
        kryo.register(ActionEnemyEnd.class);
        kryo.register(ActionHealthChanged.class);
        kryo.register(ActionHostPause.class);
        kryo.register(ActionPlayersReady.class);
        kryo.register(ActionTowerPlaced.class);
        kryo.register(ActionTowerUpgraded.class);
        kryo.register(ActionPlayerWaveReady.class);
        kryo.register(ActionWaitForReady.class);
        kryo.register(ActionWaveStart.class);
        kryo.register(ActionWaveEnd.class);
    }

    public boolean syncReady()
    {
        return true;
    }

    private boolean setupServer()
    {
        // bind server socket to the port and do other initialization as needed.
        try
        {
            server.start();
            server.bind(SERVER_TCP_PORT, SERVER_UDP_PORT);
            //serverSocket.bind(new InetSocketAddress(SERVER_PORT));
        }
        catch (IOException e)
        {
            System.out.println("NET: Server failed to bind to TCP port " + SERVER_TCP_PORT +
                               " and UDP port " + SERVER_UDP_PORT);
            e.printStackTrace();
            return false; // non-recoverable error so terminate the thread
        }

        return true;
    }


    /**
     * Runs the server logic while maintaining thread-safety.
     */
    private void runServer()
    {
        mutex.writeLock().lock();
        try
        {
            if(!gameStarted)
            {
                if(!waitingForLobby)
                {
                    ActionWaitForReady waitForReady = new ActionWaitForReady();
                    waitForReady.region = 0;
                    addToSendQueue(waitForReady);
                    waitingForLobby = true;
                }
            }

            if(connections.size() == 1 && !gameStarted)
            {
                boolean allWaiting = true;
                for(GameConnection conn : connections)
                {
                    if(conn.isValidated() && !conn.waiting && conn.connection.isConnected() && serverWaveReady)
                    {
                        ActionWaitForReady waitForReady = new ActionWaitForReady();
                        waitForReady.region = conn.playerID;
                        addToSendQueue(waitForReady);
                        conn.waiting = true;
                        allWaiting = false;
                    }
                }

                if(allWaiting)
                {
                    System.out.println("Players are ready!");
                    for(GameConnection conn : connections)
                    {
                        ActionPlayersReady playersReady = new ActionPlayersReady();
                        playersReady.region = conn.playerID;
                        addToSendQueue(playersReady);
                    }

                    ActionPlayersReady playersReady = new ActionPlayersReady();
                    playersReady.region = 0;
                    addToSendQueue(playersReady);

                    gameStarted = true;
                }
            }

            boolean isAllReady = true;
            if(connections.isEmpty())
            {
                isAllReady = serverWaveReady;
            }
            else
            {
                for (GameConnection connection : connections)
                {
                    if (!connection.waveReady)
                    {
                        isAllReady = false;
                    }
                }
            }

            if(isAllReady)
            {
                ActionCreateWave createWave = new ActionCreateWave(currentWave);
                numEnemies = createWave.amountTotalEnemies;
                createWave.region = getFirstClientID();
                currentWave++;

                if(singleplayer)
                {
                    createWave.region = 0;
                    System.out.println("Sending wave (singleplayer?)");
                    queuedRemoteChanges.add(createWave);
                }
                else
                {
                    System.out.println("Sending wave!");
                    addToSendQueue(createWave);
                }

                if(singleplayer)
                {
                    queuedRemoteChanges.add(new ActionWaveStart());
                }
                else
                {
                    for (GameConnection connection : connections)
                    {
                        ActionWaveStart waveStart = new ActionWaveStart();
                        waveStart.region = connection.playerID;
                        addToSendQueue(waveStart);
                    }

                    ActionWaveStart waveStart = new ActionWaveStart();
                    waveStart.region = 0;
                    addToSendQueue(waveStart);
                }

                waveRunning = true;

                serverWaveReady = false;
                for(GameConnection connection : connections)
                {
                    connection.waveReady = false;
                }
            }

            if(numEnemies == 0 && waveRunning)
            {
                waveRunning = false;
                if(singleplayer)
                {
                    ActionWaveEnd waveEnd = new ActionWaveEnd();
                    waveEnd.region = 0;
                    queuedRemoteChanges.add(waveEnd);
                }
                else
                {
                    for (GameConnection conn : connections)
                    {
                        ActionWaveEnd waveEnd = new ActionWaveEnd();
                        waveEnd.region = conn.playerID;
                        addToSendQueue(waveEnd);
                    }

                    ActionWaveEnd waveEnd = new ActionWaveEnd();
                    waveEnd.region = 0;
                    addToSendQueue(waveEnd);
                }
            }
        }
        finally
        {
            mutex.writeLock().unlock();
        }
    }

    /**
     * Sets up the client and attempts to connect it to the server.
     *
     * NOTE: This method will return false in the case of getHostAddress() not being set or
     * an exception while connecting.
     * @return
     */
    private boolean setupClient()
    {
        Client tempClient;
        if((tempClient = connectionManager.setupClient(client)) == null)
            return false;
        client = tempClient;
        return true;
    }

    /**
     * Runs the client network synchronization while maintaining thread safety.
     */
    private void runClient()
    {
        if(client.isConnected())
        {
            // handled collected messages and pass actions to server via client.sendTCP()
        }
    }





    public synchronized void reset()
    {
        if(entityStatus != null)
        {
            entityStatus.clear();
        }

        if(connections != null)
        {
            for (GameConnection connection : connections)
            {
                connection.connection.close();
            }
        }

        currentWave = 1;
    }

    /**
     * This method is to be called whenever an action happens; changes are queued locally so we
     * can easily alter sending and receiving timings
     */
    public void addToSendQueue(Action action)
    {
        mutex.writeLock().lock();
        try
        {
            queuedLocalChanges.add(action);
        }
        finally
        {
            mutex.writeLock().unlock();
        }
    }


    /**
     * This method is to be called from within the Game State to request the updates that were
     * received by the network manager
     */
    public void fetchChanges(QueueCallback callback)
    {
        queueManager.fetchWhenNotEmpty(callback, QueueManager.QueueID.QUEUE_ID_RECEIVE);
    }

    /**
     * This should be called locally by our sync function, whatever form that takes
     */
    private void sendLocalChanges()
    {
        if(!isServer)
        {
            mutex.readLock().lock();
            try
            {

                for(Action action : queuedLocalChanges)
                {
                    client.sendTCP(action);
                }
            }
            finally
            {
                mutex.readLock().unlock();
            }

            mutex.writeLock().lock();
            try
            {
                queuedLocalChanges.clear();
            }
            finally
            {
                mutex.writeLock().unlock();
            }
        }
        else
        {
            mutex.writeLock().lock();
            try
            {
                if(connections.isEmpty() || singleplayer)
                {
                    receiveChanges(queuedLocalChanges);
                }
                else
                {
                    for (Action action : queuedLocalChanges)
                    {
                        for (GameConnection connection : connections)
                        {
                            if (connection.playerID == action.region)
                            {
                                System.out.format("Sending: %s\n", action.actionClass);
                                server.sendToTCP(connection.connection.getID(), action);
                            }

                            // Server is always playerID = 0
                            if(action.region == 0)
                            {
                                queuedRemoteChanges.add(action);
                            }
                        }
                    }
                }
                queuedLocalChanges.clear();
            }
            finally
            {
                mutex.writeLock().unlock();
            }
        }
        /**
         * TODO: implement this method with Kryonet, Pseudocode follows:
         *  if (local changes are not empty or being written)
         *      send the local change queue to Kryonet
         */
    }

    /**
     * This method should be called from whatever callback we use when changes come in from the
     * network
     */
    private synchronized void receiveChanges(List<Action> changes)
    {
        /**
         * TODO: this method is implementation dependent, but should look roughly like the following:
         * if (isServer)
         *     check received changes for coherency with master state
         *     push valid changes to send queue (so they can be sent back out to other clients)
         * add all changes to update queue, so game can read them in when needed
         */
        if(changes == null)
        {
            return;
        }

        List<Action> actions = new ArrayList<>(changes);

        // if we are a client, we just take the new changes and leave
        if(!isServer)
        {
            queuedRemoteChanges.addAll(actions.get);
            return;
        }

        for(Action change : actions)
        {
            receiveChange(change);
        }
    }

    public void receiveChange(Action change)
    {
        queueManager.addToReceivedQueue(change, this);

        if(change == null)
        {
            return;
        }

        System.out.println("Packet type: " + change.actionClass);

        switch(change.actionClass)
        {
        case ACTION_PLAYER_WAVE_READY:
            ActionPlayerWaveReady playerReady = (ActionPlayerWaveReady)change;
            if(connections.size() > 0)
            {
                connections.get(playerReady.region - 1).waveReady = true;
            }
            else
            {
                serverWaveReady = true;
            }
            break;
        case ACTION_ENEMY_CREATE:
            ActionEnemyCreate actionCreate = (ActionEnemyCreate)change;
            actionCreate.entityID = lastEntityID + 1;
            lastEntityID++;

            mutex.writeLock().lock();
            try
            {
                entityStatus.put(actionCreate.entityID, new EnemyStatus(actionCreate));

                if(singleplayer)
                {
                    queuedRemoteChanges.add(actionCreate);
                }
                else
                {
                    addToSendQueue(actionCreate);
                }
            }
            finally
            {
                mutex.writeLock().unlock();
            }

            break;
        case ACTION_ENEMY_END:
            ActionEnemyEnd actionEnd = (ActionEnemyEnd)change;

            mutex.writeLock().lock();
            try
            {
                if(entityStatus.containsKey(actionEnd.entityID))
                {
                    // if the enemy is at region 0, then its at the end
                    if(entityStatus.get(actionEnd.entityID).region == 0)
                    {
                        health--;
                        numEnemies--;
                        ActionHealthChanged actionHealth = new ActionHealthChanged(health);
                        if(singleplayer)
                        {
                            queuedRemoteChanges.add(actionHealth);
                        }
                        else
                        {
                            addToSendQueue(actionHealth);
                        }
                    }
                    else
                    {
                        // e.g., if we have 2 connections, there are 3 screens. if we are at region 2,
                        // we need to send the enemy to server, or region 0. (2+1) % (2+1) = 0;
                        entityStatus.get(actionEnd.entityID).region += 1;
                        entityStatus.get(actionEnd.entityID).region %= (connections.size() + 1);

                        EnemyStatus transfer = (EnemyStatus) entityStatus.get(actionEnd.entityID);
                        transfer.velocity = actionEnd.velocity;

                        ActionEnemyCreate actionEndCreate = new ActionEnemyCreate(transfer);

                        //add actionEndCreate to the queue that's going back out to the clients.
                        addToSendQueue(actionEndCreate);
                    }
                }
            }
            finally
            {
                mutex.writeLock().unlock();
            }
            break;
        case ACTION_ENEMY_DESTROY:
            ActionEnemyDestroy actionDestroy = (ActionEnemyDestroy)change;

            mutex.writeLock().lock();
            try
            {
                if (entityStatus.containsKey(actionDestroy.entityID))
                {
                    entityStatus.remove(actionDestroy.entityID);
                    numEnemies--;
                }
            }
            finally
            {
                mutex.writeLock().unlock();
            }
            break;
        case ACTION_ENEMY_DAMAGE:
            // ignore for now
            break;
        case ACTION_TOWER_PLACED:
            ActionTowerPlaced actionTowerPlaced = (ActionTowerPlaced)change;

            actionTowerPlaced.entityID = lastEntityID + 1;
            lastEntityID++;

            mutex.writeLock().lock();
            try
            {
                entityStatus.put(actionTowerPlaced.entityID, new TowerStatus(actionTowerPlaced));
                if(singleplayer)
                {
                    queuedRemoteChanges.add(actionTowerPlaced);
                }
                else
                {
                    addToSendQueue(actionTowerPlaced);
                }
            }
            finally
            {
                mutex.writeLock().unlock();
            }
            break;
        case ACTION_TOWER_UPGRADED:
            ActionTowerUpgraded actionTowerUpgraded = (ActionTowerUpgraded)change;

            mutex.writeLock().lock();
            try
            {
                if(entityStatus.containsKey(actionTowerUpgraded.entityID))
                {
                    TowerStatus towerStatus = (TowerStatus)entityStatus.get(actionTowerUpgraded.entityID);
                    towerStatus.level++;

                    if(towerStatus.level > Tower.MAX_LEVEL)
                    {
                        towerStatus.level = Tower.MAX_LEVEL;
                        entityStatus.put(actionTowerUpgraded.level, towerStatus);

                        actionTowerUpgraded.level = towerStatus.level;
                        if(singleplayer)
                        {
                            queuedRemoteChanges.add(actionTowerUpgraded);
                        }
                        else
                        {
                            addToSendQueue(actionTowerUpgraded);
                        }
                    }
                }
            }
            finally
            {
                mutex.writeLock().unlock();
            }

            break;
        }
    }

    protected boolean handleValidation(GameConnection gameConnection, Connection connection, Object object)
    {
        // check for validation message.
        // otherwise close connection
        // validation message should be first thing sent on the connection from the client.
        if(object instanceof GameConnection.ClientAuth)
        {
            GameConnection.ClientAuth authPacket = (GameConnection.ClientAuth)object;
            if(authPacket.key == MyGame.VERSION)
            {
                gameConnection.connection.sendTCP(new GameConnection.ServerAuth());
                gameConnection.assignUID(uidCounter);
                uidCounter++;

                gameConnection.playerID = lastPlayerID;
                GameConnection.PlayerID idPacket = new GameConnection.PlayerID();
                gameConnection.connection.sendTCP(idPacket);

                idPacket.playerID = gameConnection.playerID;

                lastPlayerID++;

                playerStatus.put(gameConnection.playerID, PlayerStatus.PLAYER);

                ActionWaitForReady actionWaitReady = new ActionWaitForReady();
                addToSendQueue(actionWaitReady);

                System.out.println("NET: Client auth key = " + authPacket.key);
                return true;
            }
            else
            {
                System.out.println("NET: Client failed authentication. Allowing retries...");
                return false;
            }
        }

        // client attempting to validate.
        if(object instanceof GameConnection.ServerAuth)
        {
            GameConnection.ServerAuth authPacket = (GameConnection.ServerAuth)object;
            if(authPacket.key != MyGame.VERSION)
            {
                // ERROR, invalid server
                System.out.println("NET: Incompatible server detected. Key = " + authPacket.key);
                gameConnection.connection.close();
                return false;
            }
            else
            {
                return true;
            }
        }

        return false;
    }


    public ConnectionManager getConnectionManager()
    {
        return connectionManager;
    }

    @Override
    public void addCompleted()
    {

    }

    @Override
    public void retrieved(Object o)
    {

    }
}
