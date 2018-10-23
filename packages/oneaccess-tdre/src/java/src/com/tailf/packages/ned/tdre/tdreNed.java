package com.tailf.packages.ned.tdre;

import com.tailf.packages.ned.tdre.namespaces.*;
import java.io.IOException;
import java.lang.reflect.Array;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;

import com.tailf.packages.ned.tdre.parser.*;
import com.tailf.conf.*;
import com.tailf.ned.*;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.log4j.Logger;
import com.tailf.maapi.Maapi;
import com.tailf.maapi.MaapiException;
import com.tailf.maapi.MaapiSchemas;
import com.tailf.ncs.ResourceManager;
import com.tailf.ncs.annotations.Scope;
import com.tailf.ncs.ns.Ncs;
import com.tailf.ned.NedWorker.TransactionIdMode;

/*
    TODO:
        - use username and password from device
 */



public class tdreNed extends NedGenericBase  {
    private String      deviceName;
    private InetAddress ip;
    private int         port;
    private String      luser;
    private String      ruser;
    private String      pass;

    private boolean     trace;
    private int         connectTimeout; // msec
    private int         readTimeout;    // msec
    private int         writeTimeout;   // msec

    private static String priopol = "BANDWIDTH_LTE";

    private static Logger LOGGER = Logger.getLogger(tdreNed.class);
    public  Maapi                    maapi = null;
    private boolean                  wantReverse=true;

    private static MaapiSchemas schemas;
    private static MaapiSchemas.CSNode cfgCs;
    private static MaapiSchemas.CSNode rpcCs;

    private CliSession session;

    public tdreNed(){
        this(true);
    }

    public tdreNed(boolean wantReverse){
        this.wantReverse = wantReverse;
        LOGGER.info("tdreNed(bool) <==");
    }


    public tdreNed(String deviceName,
                InetAddress ip,
                int port,
                String luser,
                boolean trace,
                int connectTimeout,
                int readTimeout,
                int writeTimeout,
                NedMux mux,
                NedWorker worker,
                boolean wantReverse)  {

        LOGGER.info("tdreNed(...) <==");

        try {
            this.deviceName = deviceName;
            this.ip = ip;
            this.port = port;
            this.luser = luser;
            this.trace = trace;
            this.connectTimeout = connectTimeout;
            this.readTimeout = readTimeout;
            this.writeTimeout = writeTimeout;
            this.wantReverse = wantReverse;

            this.ruser = "cisco";
            this.pass = "cisco";

            LOGGER.info("CONNECTING <==");

            NedCapability capas[] = new NedCapability[1];
            capas[0] = new NedCapability("http://tailf.com/ned/tdre", "tdre");

//            NedCapability statscapas[] = new NedCapability[1];
//            statscapas[0] = new NedCapability("http//tailf.com/ned/tdre-stats", "tdre-stats");

            setConnectionData(capas,
                              null,
                              this.wantReverse,  // want reverse-diff
                              TransactionIdMode.NONE);

            //
            // Connect to device (or jump host)
            //
            try {
                worker.setTimeout(connectTimeout);
                traceInfo(worker, "connect-timeout "+connectTimeout+" read-timeout "+readTimeout+" write-timeout "+writeTimeout);
                setupTelnet(worker);
                traceInfo(worker, "DONE TELNET logged in ");
            }
            catch (Exception e) {
                logError(worker, "connect failed",  e);
                try {
                    worker.connectError(NedWorker.CONNECT_CONNECTION_REFUSED, e.getMessage());
                } catch (Exception ignore) {
                    logError(null, "connect response failed", ignore);
                }
                return;
            }


            LOGGER.info("CONNECTING ==> OK");
        }
        catch (Exception e) {
            worker.error(NedCmd.CONNECT_GENERIC, e.getMessage()," Cntc error");
        }
    }

    protected void logError(NedWorker worker, String text, Exception e) {
//        nedLogger.error(device_id + " " + text, e);
        if (trace && worker != null)
            worker.trace("-- " + text + ": " + e.getMessage() + "\n", "out", "devicename");
    }



    public String device_id() {
        return deviceName;
    }

    // should return "cli" or "generic"
    public String type() {
        return "generic";
    }
    // Which YANG modules are covered by the class
    public String [] modules() {
        LOGGER.info("modules");
        return new String[] { "tdre" };
    }

    // Which identity is implemented by the class
    public String identity() {
        return "tdre-id:oneaccess-tdre";
    }

    /**
     * Is invoked by NCS to take the configuration to a new state.
     * We retrive a rev which is a transaction handle to the
     * comming write operation then we write operations towards the device.
     * If all succeded we transition to commit phase or if
     * prepare fails we transition to abort phase.
     *
     * @param w - is the processing worker. It should be used for sending
     * responses to NCS.
     * @param data is the commands for transforming the configuration to
     * a new state.
     */

    public void prepare(NedWorker worker, NedEditOp[] ops)
        throws NedException, IOException {
        LOGGER.info("PREPARE <==");

        for (int i = 0; i<ops.length; i++) {
            LOGGER.info("op " + ops[i]);
        }
        edit(ops);
        worker.prepareResponse();
    }

    /**
     * Is invoked by NCS to ask the NED what actions it would take towards
     * the device if it would do a prepare.
     *
     * The NED can send the preformatted output back to NCS through the
     * call to  {@link com.tailf.ned.NedWorker#prepareDryResponse(String)
     * prepareDryResponse()}
     *
     * The Ned should invoke the method
     * {@link com.tailf.ned.NedWorker#prepareDryResponse(String)
     *   prepareDryResponse()} in <code>NedWorker w</code>
     * when the operation is completed.
     *
     * If the functionality is not supported or an error is detected
     * answer this through a call to
     * {@link com.tailf.ned.NedWorker#error(int,String,String) error()}
     * in <code>NedWorker w</code>.
     *
     * @param w
     *    The NedWorker instance currently responsible for driving the
     *    communication
     *    between NCS and the device. This NedWorker instance should be
     *    used when communicating with the NCS, ie for sending responses,
     *    errors, and trace messages. It is also implements the
     *    {@link NedTracer}
     *    API and can be used in, for example, the {@link SSHSession}
     *    as a tracer.
     *
     * @param ops
     *    Edit operations representing the changes to the configuration.
     */
    public void prepareDry(NedWorker worker, NedEditOp[] ops)
        throws NedException {
        StringBuilder dryRun = new StringBuilder();

        LOGGER.info("PREPARE DRY <==");
        edit(ops, dryRun);
        try {
            worker.prepareDryResponse(dryRun.toString());
        }
        catch (IOException e) {
            throw new NedException(NedErrorCode.NED_INTERNAL_ERROR,
                                   "Internal error when calling "+
                                   "prepareDryResponse: "+
                                   e.getMessage());
        }
        LOGGER.info("PREPARE DRY ==> OK");
    }

    public void commit(NedWorker worker, int timeout)
        throws NedException, IOException {
        LOGGER.info("COMMIT <==");
        worker.commitResponse();
    }

    /**
     * Is invoked by NCS to abort the configuration to a previous state.
     *
     * @param w is the processing worker. It should be used for sending
     * responses to NCS. * @param data is the commands for taking the config
     * back to the previous
     * state. */

    public void abort(NedWorker worker , NedEditOp[] ops)
        throws NedException, IOException {
        LOGGER.info("ABORT <==");
        edit(ops);
        worker.abortResponse();
        LOGGER.info("ABORT ==> OK");
    }


    public void revert(NedWorker worker , NedEditOp[] ops)
        throws NedException, IOException {
        LOGGER.info("REVERT <==");
        edit(ops);
        worker.revertResponse();
        LOGGER.info("REVERT ==> OK");
    }


    public void persist(NedWorker worker)
        throws NedException, IOException {
        LOGGER.info("PERSIST <==");
        worker.persistResponse();

    }

    public void close(NedWorker worker)
        throws NedException, IOException {
        close();
    }

    public void close() {
        LOGGER.info("CLOSE <==");
        try {
            if (maapi != null)
                ResourceManager.unregisterResources(this);
        }
        catch (Exception e) {
            ;
        }
        LOGGER.info("CLOSE ==> OK");

        session.close();
    }

    /*
     * The generic show command is to
     * grab all configuration from the device and
     * populate the transaction handle  passed to us.
     **/

    public String get_device_value(NedWorker worker, String pp, String bw, String name)
        throws IOException, SSHSessionException {
        NedExpectResult res;
        LOGGER.info(String.format("Getting %s %s %s", pp, bw, name));
        session.print(String.format("GET -x profiles/policy/priority/priorityPolicy[%s]/bandwidth[%s]/%s\r", pp, bw, name));
        LOGGER.info("waiting for values");
//        res = session.expect(new String[]{"\\A\S+\s+=\s+\S+\\z"}, worker);
        res = session.expect(new String[]{"\\S+\\s+=\\s+\\S+"}, worker);
        String s = res.getMatch();
        LOGGER.info("got values " + s);
        String[] sl = s.split(" = ");
        LOGGER.info("waiting for prompt");
        session.expect("\\A>", worker);
        LOGGER.info("got prompt");
        return sl[1];
    }

    public void show(NedWorker worker, int tHandle) throws NedException {

        NedExpectResult res;
        String value;

        try {
            LOGGER.info("SHOW <==");
            LOGGER.info("THANDLE:" + tHandle);
            if (maapi == null)
                maapi = ResourceManager.getMaapiResource(this, Scope.INSTANCE);
            LOGGER.info( this.toString()  + " Attaching to Maapi " + maapi);
            maapi.attach(tHandle, 0);

            // fill in fake data here
            LOGGER.info( this.toString()  + "attached ");

            // ================ READ CONFIG FROM DEVICE =================

            session.print("GET -f profiles\r");
            LOGGER.info("waiting for action");
            session.expect("\\Aaction.*\\Z");
            LOGGER.info("got action");

            String config = "";
            while (true) {
                try {
                    LOGGER.info("receiving for input");
                    res = session.expect(new String[]{
                            "\\Aaction.*\\Z",
                            "\\A.+\\Z"
                    }, worker);
                    if (res.getHit() == 0) {
                        break;
                    } else {
                        config += res.getMatch();
                        config += "\n";
                    }
                } catch (Throwable e) {
                    LOGGER.info("timeout");
                    throw new NedException("Timeout, no response from device");
                }
            }

            System.out.println(config);

            LOGGER.info("waiting for OK");
            session.expect("\\AOK\\s*\\Z");
            LOGGER.info("got OK");

            // ================ PARSE CONFIG  =================

            Filter filter = new Filter();
            Filter bw = filter.add_path("profiles/policy/priority/priorityPolicy/*/bandwidth/*");
            bw.add_path(new String[]{"name","cir","eir","unit"});

            System.out.println("parsing: ");

            CharStream stream = new ANTLRInputStream(config);
            r1645Lexer lexer = new r1645Lexer(stream);
            r1645Parser parser = new r1645Parser(new CommonTokenStream(lexer));
            ParseTree tree = parser.data();
            r1645MyVisitor visitor = new r1645MyVisitor(filter);
            Level data = visitor.visit(tree);
            if (data != null) {
                data.print_structure();
                data.create_structure(maapi, tHandle, this.deviceName);
            }

//            String dev_path =
//                    "/ncs:devices/device{" + deviceName + "}/config/";
//            String pp_path = dev_path + "/profiles/policy/priority/priorityPolicy{" + priopol + "}";
//            String bw_path = pp_path + "/bandwidth{0}";
//
//            maapi.create(tHandle, pp_path);
//            maapi.create(tHandle, bw_path);
//            maapi.setElem(tHandle, new ConfBuf("BE"), bw_path+"/name");
//
//            value = get_device_value(worker, priopol, "0", "eir");
//            maapi.setElem(tHandle, new ConfUInt32(Integer.parseInt(value)), bw_path+"/eir");
//
//            value = get_device_value(worker, priopol, "0", "cir");
//            maapi.setElem(tHandle, new ConfUInt32(Integer.parseInt(value)), bw_path+"/cir");
//
//            value = get_device_value(worker, priopol, "0", "unit");
//            LOGGER.info("unit " + value);
//            if (value.equals("bits/sec")) {
//                maapi.setElem(tHandle, new ConfEnumeration(0), bw_path + "/unit");
//            } else {
//                maapi.setElem(tHandle, new ConfEnumeration(1), bw_path + "/unit");
//            }

            maapi.detach(tHandle);
            worker.showGenericResponse();
            LOGGER.info("SHOW ==> OK");
        }
        catch (Exception e) {
            throw new NedException(NedErrorCode.NED_INTERNAL_ERROR, "", e);
        }
    }


    /*
     * This method must at-least populate the path it is given,
     * it may do more though
     */

    public void showStats(NedWorker worker, int tHandle, ConfPath path)
        throws NedException, IOException {
        try {
            if (maapi == null)
                maapi = ResourceManager.getMaapiResource(this, Scope.INSTANCE);
            LOGGER.info( this.toString()  + " Attaching to Maapi " + maapi +
                         " for " + path);
            maapi.attach(tHandle, 0);
            maapi.setElem(tHandle, new ConfInt32(345), path);
            maapi.detach(tHandle);

            NedTTL ttl = new NedTTL(path,0);
            worker.showStatsResponse(new NedTTL[]{ttl});
        }
        catch (Exception e) {
            throw new NedException(NedErrorCode.NED_INTERNAL_ERROR, "", e);
        }
    }

    /*
     *   This method must at-least fill in all the keys of the list it
     *   is passed, it may do more though. In this example  code we
     *   choose to not let the code in showStatsList() fill in the full
     *   entries, thus forcing an invocation of showStats()
     */

    public void showStatsList(NedWorker worker, int tHandle, ConfPath path)
        throws NedException, IOException {
        try {
            if (maapi == null)
                maapi = ResourceManager.getMaapiResource(this, Scope.INSTANCE);
            String path0 =
                "/ncs:devices/device{" + deviceName + "}/live-status/" +
                "test-stats/item";

            LOGGER.info( this.toString()  + " Attaching2 to Maapi " + maapi +
                         " for " + path);
            maapi.attach(tHandle, 0);

            maapi.create(tHandle, path0 + "{k1}");
            maapi.create(tHandle, path0 + "{k2}");
            maapi.create(tHandle, path0 + "{k3}");
            maapi.detach(tHandle);

            worker.showStatsListResponse(30, null);
        }
        catch (Exception e) {
            throw new NedException(NedErrorCode.NED_INTERNAL_ERROR, "", e);
        }
    }

    public boolean isAlive() {
        return true;
    }

    public void reconnect(NedWorker worker) {
        LOGGER.info("RECONNECT ==> OK");
    }

    public boolean isConnection(String deviceId,
                                InetAddress ip,
                                int port,
                                String luser,
                                boolean trace,
                                int connectTimeout, // msecs
                                int readTimeout,    // msecs
                                int writeTimeout) { // msecs
        return ((this.deviceName.equals(deviceName)) &&
                (this.ip.equals(ip)) &&
                (this.port == port) &&
                (this.luser.equals(luser)) &&
                (this.trace == trace) &&
                (this.connectTimeout == connectTimeout) &&
                (this.readTimeout == readTimeout) &&
                (this.writeTimeout == writeTimeout));
    }

/*
 * If the device has commands, i,e reboot etc that are - just - commands
 * that do not manipulate the configuration, we model those commands
 * in the YANG model, and get invoked here. The task of this code is to
 * look at the input params, invoke the cmd on the device and return
 * data - according to the YANG model
 *
 */
    public void command(NedWorker worker, String cmdname, ConfXMLParam[] p)
        throws NedException, IOException, SSHSessionException {

        LOGGER.info("command: " + cmdname);

        session.print("action \"Activate Configuration\"\r\n");

        LOGGER.info("waiting for prompt");
        session.expect("\\A>", worker);
        LOGGER.info("got prompt");

        try {
            ConfNamespace x = new tdre();
            worker.commandResponse(
                new ConfXMLParam[] {
                    new ConfXMLParamValue(
                        x, "result",
                        new ConfBuf("OK"))
                });
        }
        catch (Exception e) {
            throw new NedException(NedErrorCode.NED_INTERNAL_ERROR, "", e);
        }
    }

/**
 * Establish a new connection to a device and send response to
 * NCS with information about the device.
 *
 * @param deviceId name of device
 * @param ip address to connect to device
 * @param port port to connect to
 * @param luser name of local NCS user initiating this connection
 * @param trace indicates if trace messages should be generated or not
 * @param connectTimeout in milliseconds
 * @param readTimeout in milliseconds
 * @param writeTimeout in milliseconds
 * @return the connection instance
 **/
    public NedGenericBase newConnection(String deviceId,
                                        InetAddress ip,
                                        int port,
                                        String luser,
                                        boolean trace,
                                        int connectTimeout, // msecs
                                        int readTimeout,    // msecs
                                        int writeTimeout,   // msecs
                                        NedMux mux,
                                        NedWorker worker ) {
        LOGGER.info("newConnection() <==");
        tdreNed ned = null;

        ned = new tdreNed(deviceId, ip, port, luser, trace,
                       connectTimeout, readTimeout, writeTimeout,
                       mux, worker,
                       wantReverse );
        LOGGER.info("NED invoking newConnection() ==> OK");
        return ned;
    }

    public void getTransId(NedWorker w) throws NedException, IOException {
        w.error(NedCmd.GET_TRANS_ID, "getTransId", "not supported");
    }

    private void traceInfo(NedWorker worker, String info) {
        if (trace)
            worker.trace("-- " + info + "\n", "out", deviceName);
    }

    public void setupTelnet(NedWorker worker) throws Exception {
        TelnetSession tsession;
        NedExpectResult res;

        System.out.println("NED setupTelnet()");

        traceInfo(worker, "TELNET connecting to host: "+ip.getHostAddress()+":"+port);

        if (trace)
            tsession = new TelnetSession(worker, ruser, readTimeout, worker, this);
        else
            tsession = new TelnetSession(worker, ruser, readTimeout, null, this);
        session = tsession;

//        if (!remoteConnection.equals("none")) {
//            traceInfo(worker, "Sending newline (initial)");
//            session.print("\r\n");
//        }

        // Login and enter enable mode
        loginDevice(worker, 0, ruser, pass);
    }

    public void loginDevice(NedWorker worker, int phase, String username, String password)
            throws Exception {
        System.out.println("NED loginDevice()");
        NedExpectResult res;
        int timeout = 20000000; // HARDCODED

        String prompt = ">";
        String s;

        //session.println("ULRIK");
        while (true) {

            // Wait for terminal output from device
            try {
                traceInfo(worker, "Waiting for input from device");
                LOGGER.info("waiting for input");
                res = session.expect(new String[] {

                                "Username: ",
                                "Password: ",
                                "login incorrect",
                                prompt,
                                "XXX",
                                "YYY",
                                "Login successful"
                        }, worker);
            } catch (Throwable e) {
                LOGGER.info("timeout");
                throw new NedException("Timeout, no response from device");
            }


            // Parse reply and act accordingly
            timeout = 20000; // HARDCODEDa
            int hit = res.getHit();


            switch (res.getHit()) {
                case 0: // Username request
                    Thread.sleep(500);
                    session.print(username+"\r");
                    LOGGER.info("sent username: " + username);
                    //session.expect(username, worker);
                    break;
                case 1: // Password request
                    Thread.sleep(500);
                    session.print(password+"\r");
                    LOGGER.info("sent password: " + password);
                    break;
                case 2: // Invalid username or password
                    LOGGER.info("Authentication failed");
                    throw new NedException("Authentication failed");
                case 3: // prompt
                    return;
                case 4: // XXX
                case 5: // XXX
                    LOGGER.info("Got XXX:");
                    LOGGER.info(res.getMatch());
                    LOGGER.info(res.getText());
                    break;
                case 6: // prompt
                    LOGGER.info("Got something else:");
                    LOGGER.info(res.getMatch());
                    LOGGER.info(res.getText());
                    break;
            }
        }
    }


    private void sendNewLine(NedWorker worker, String logtext)
            throws Exception {
        traceInfo(worker, logtext);
        session.print("\r\n");
    }






    public void edit(NedEditOp[] ops)
        throws NedException {
        edit(ops, null);
    }

    public void edit(NedEditOp[] ops, StringBuilder dryRun)
        throws NedException {

        try {
            for (NedEditOp op: ops) {
                switch (op.getOperation()) {
                case NedEditOp.CREATED:
                    create(op, dryRun);
                    break;
                case NedEditOp.DELETED:
                    delete(op, dryRun);
                    break;
                case NedEditOp.MOVED:
                    break;
                case NedEditOp.VALUE_SET:
                    valueSet(op, dryRun);
                    break;
                case NedEditOp.DEFAULT_SET:
                    defaultSet(op, dryRun);
                    break;
                }
            }
        } catch (Exception e) {
            throw new NedException(NedErrorCode.NED_INTERNAL_ERROR, "", e);
        }
    }

    private ConfObject[] getKP(NedEditOp op) throws NedException {
        ConfPath cp = op.getPath();
        ConfObject[] kp;

        try {
            kp = cp.getKP();
        } catch (Exception e) {
            throw new NedException(NedErrorCode.NED_INTERNAL_ERROR,
                                   "Internal error, cannot get key path: "
                                   +e.getMessage());
        }
        return kp;
    }


    private void add_line(StringBuilder sb, String s) {
        sb.append(s);
        sb.append("\n");
    }


    public void create(NedEditOp op, StringBuilder dryRun)
        throws Exception, SSHSessionException, NedException  {

        ConfObject[] kp = getKP(op);
        ConfKey key = (ConfKey)kp[0]; // The key always comes first

        System.out.println(Arrays.toString(kp));

        if (kp.length == 5) { // priorityPolicy
            String name = key.elementAt(0).toString();
            String cmd = String.format("CREATEOBJ profiles/policy/priority/priorityPolicy[%s]", name);

            if (dryRun == null) {
                LOGGER.info("Create not forwarded to device: " + cmd);
                /*
                session.print(cmd);
                session.print("\r");
                */
            } else {
                add_line(dryRun, cmd);
            }
        }
        else if (kp.length == 7) { // bandwidth
            // ignore
            System.out.println("no create for bandwidth");
        }
    }

    public void valueSet(NedEditOp op, StringBuilder dryRun)
        throws NedException, IOException, SSHSessionException  {
        ConfObject[] kp = getKP(op);
        String pp_key = ((ConfKey)kp[3]).elementAt(0).toString(); // priorityPolicy key
        String bw_key = ((ConfKey)kp[1]).elementAt(0).toString(); // Bandwidth index
        ConfTag tag = (ConfTag)kp[0];

        ConfObject value = op.getValue();

        String val_str = "";

        if (tag.getTag().equals("unit")) {
            ConfEnumeration unit = (ConfEnumeration)value;
            switch (unit.getOrdinalValue()) {
                case 0:
                    val_str = "bits/sec";
                    break;
                case 1:
                    val_str = "percent";
                    break;
            }
        } else {
            val_str = value.toString();
        }

        System.out.println(Arrays.toString(kp));

        if (kp.length == 8) { // cir/eir/unit
            String cmd = String.format("SET profiles/policy/priority/priorityPolicy[%s]/bandwidth[%s]/%s = %s", pp_key.toString(), bw_key.toString(), tag.getTag(), val_str);
            if (dryRun == null) {
                session.print(cmd);
                session.print("\r");
            } else {
                add_line(dryRun, cmd);
            }
        }
        else {
            System.out.println("How to handle?");
        }
    }

    public void defaultSet(NedEditOp op, StringBuilder dryRun)
        throws NedException  {
        ConfObject[] kp = getKP(op);
        LOGGER.info("default set for " + op.getPath());
//        ConfKey key = (ConfKey)kp[1];
//        ConfTag tag = (ConfTag)kp[0];

        System.out.println(Arrays.toString(kp));
    }

    public void delete(NedEditOp op, StringBuilder dryRun)
        throws NedException  {
        ConfObject[] kp = getKP(op);

        System.out.println(Arrays.toString(kp));

        if (kp.length == 5) { // priorityPolicy
            ConfKey key = (ConfKey)kp[0]; // The key always comes first
            String name = key.elementAt(0).toString();
            String cmd = String.format("DELOBJ profiles/policy/priority/priorityPolicy[%s]", name);

            if (dryRun == null) {
                LOGGER.info("Delete not forwarded to device: " + cmd);
                /*
                session.print(cmd);
                session.print("\r");
                */
            } else {
                add_line(dryRun, cmd);
            }
        }
        else if (kp.length == 7) { // bandwidth
            // ignore
            System.out.println("no delete for bandwidth");
        }
        else {
            System.out.println("TODO: How to handle?");
        }
    }
}
