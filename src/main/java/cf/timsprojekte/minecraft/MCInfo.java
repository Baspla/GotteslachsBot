package cf.timsprojekte.minecraft;

import com.google.gson.*;

import java.io.*;
import java.net.*;
import java.util.ArrayList;

public class MCInfo {
    public static final int DEFAULT_TIMEOUT = 5000;
    private String address;
    private int port;
    private int timeout;
    private boolean online;
    private String motd;
    private String version;
    private int currentPlayers;
    private int maximumPlayers;
    private long latency;
    private boolean error;
    private String favicon;
    private boolean modded;

    public ArrayList<MCPlayer> getSamples() {
        return samples;
    }

    private ArrayList<MCPlayer> samples;

    private static void print(MCInfo mcinfo) {
        System.out.println("Current Players: " + mcinfo.getCurrentPlayers());
        System.out.println("Max Players: " + mcinfo.getMaximumPlayers());
        System.out.println("Version: " + mcinfo.getVersion());
        System.out.println("Motd: " + mcinfo.getMotd());
        System.out.println("Latency: " + mcinfo.getLatency());
        System.out.println("Online: " + mcinfo.isOnline());
    }


    public boolean isOnline() {
        return online;
    }

    static MCInfo request(String adress, int port) {
        String serverData;
        MCInfo info = new MCInfo();
        info.setError(false);
        try (Socket clientSocket = new Socket()) {
            long startTime = System.currentTimeMillis();
            clientSocket.connect(new InetSocketAddress(adress, port), 5000);
            info.setLatency(System.currentTimeMillis() - startTime);
            DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream());
            BufferedReader br = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            byte[] payload = {(byte) 0x15, (byte) 0x00, (byte) 0xbf, (byte) 0x04, (byte) 0x0e,
                    (byte) 0x38, (byte) 0x39, (byte) 0x2e, (byte) 0x31, (byte) 0x36, (byte) 0x33, (byte) 0x2e, (byte) 0x31, (byte) 0x38, (byte) 0x37, (byte) 0x2e, (byte) 0x31, (byte) 0x35, (byte) 0x38,
                    (byte) 0x63, (byte) 0xdd, (byte) 0x01};
            dos.write(payload, 0, payload.length);
            dos.writeBytes("\u0001\u0000");
            serverData = "";
            do {
                serverData = serverData + (char) br.read();
            } while (br.ready());
            if(!serverData.endsWith("}")){
                serverData+=br.readLine();
            }
        } catch (Exception e) {
            e.printStackTrace();
            info.setOnline(false);
            return info;
        }

        if (serverData.equals("")) {
            info.setOnline(false);
        } else {
            try {
                String cut = serverData.substring(serverData.indexOf("{"), serverData.lastIndexOf("}") + 1);
                JsonParser parser = new JsonParser();
                JsonObject data = parser.parse(cut).getAsJsonObject();
                info.setOnline(true);
                if (data.has("version")) {
                    JsonObject v = data.get("version").getAsJsonObject();
                    if (v.has("name")) {
                        info.setVersion(v.get("name").getAsString());
                    }
                }
                if (data.has("description")) {
                    JsonObject d = data.get("description").getAsJsonObject();
                    String msg = "";
                    if (d.has("text")) {
                        msg = d.get("text").getAsString();
                    }
                    if (d.has("extra")) {
                        JsonArray extra = d.get("extra").getAsJsonArray();
                        for (int i = 0; i < extra.size(); i++) {
                            JsonObject entry = extra.get(i).getAsJsonObject();
                            if (entry.has("text"))
                                msg = msg + entry.get("text").getAsString();
                        }
                    }

                    info.setMotd(msg);
                }
                if (data.has("players")) {
                    JsonObject p = data.get("players").getAsJsonObject();
                    if (p.has("max")) {
                        info.setMaximumPlayers(p.get("max").getAsInt());
                    }
                    if (p.has("online")) {
                        info.setCurrentPlayers(p.get("online").getAsInt());
                    }
                    if (p.has("sample")) {
                        JsonArray sample = p.get("sample").getAsJsonArray();
                        ArrayList<MCPlayer> sampleList = new ArrayList<>();
                        for (int i = 0; i < sample.size(); i++) {
                            JsonObject entry = sample.get(i).getAsJsonObject();
                            if (entry.has("id") && entry.has("name"))
                                sampleList.add(new MCPlayer(entry.get("name").getAsString(), entry.get("id").getAsString()));
                        }
                        info.setSamples(sampleList);
                    }
                }
                if (data.has("favicon")) {
                    info.setFavicon(data.get("favicon").getAsString());
                }
                info.setModded(data.has("modinfo") || data.has("forgeData"));
            } catch (JsonParseException | NullPointerException | StringIndexOutOfBoundsException e) {
                info.setOnline(false);
                info.setError(true);
            }
        }
        return info;
    }

    private MCInfo(){
        samples=new ArrayList<>();
    }

    private void setSamples(ArrayList<MCPlayer> sampleList) {
        this.samples = sampleList;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getPort() {
        return port;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setLatency(long latency) {
        this.latency = latency;
    }

    public long getLatency() {
        return latency;
    }

    public void setVersion(String version) {
        this.address = version;
    }

    public String getVersion() {
        return address;
    }

    public void setMotd(String motd) {
        this.motd = motd;
    }

    public String getMotd() {
        return motd;
    }

    public void setCurrentPlayers(int currentPlayers) {
        this.currentPlayers = currentPlayers;
    }

    public int getCurrentPlayers() {
        return currentPlayers;
    }

    public void setMaximumPlayers(int maximumPlayers) {
        this.maximumPlayers = maximumPlayers;
    }

    public int getMaximumPlayers() {
        return maximumPlayers;
    }

    public void setError(boolean error) {
        this.error = error;
    }

    public boolean hasError() {
        return error;
    }

    public void setFavicon(String favicon) {
        this.favicon = favicon;
    }

    public String getFavicon() {
        return favicon;
    }

    public void setModded(boolean modded) {
        this.modded = modded;
    }

    public boolean getModded() {
        return modded;
    }

    public String getSamplesFormatted() {
        String list = "Spieler: ";
        if (getSamples() == null) return "";
        for (MCPlayer player : getSamples()) {
            list = list + player.getName() + ", ";
        }
        if (list.endsWith(", ")) list = list.substring(0, list.length() - 2);
        return list;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }
}
