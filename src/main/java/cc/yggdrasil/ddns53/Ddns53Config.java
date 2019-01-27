package cc.yggdrasil.ddns53;

import java.util.Date;

public class Ddns53Config {
    String fileName;
    String hostedZoneId;
    String domainName;
    String currentIP;
    String ipProvider;

    public Ddns53Config(String fileName,
                        String hostedZoneID,
                        String domainName,
                        String ipProvider,
                        String currentIP) {
        this.fileName = fileName;
        this.hostedZoneId = hostedZoneID;
        this.domainName = domainName;
        this.ipProvider = ipProvider;
        this.currentIP = currentIP;
    }

    public void print_details() {
        System.out.println("I: " + new Date() + ": ******** Current Configuration Settings ********");
        System.out.println("I: " + new Date() + ": Config File: " + fileName);
        System.out.println("I: " + new Date() + ": Zone ID    : " + hostedZoneId);
        System.out.println("I: " + new Date() + ": Domain Name: " + domainName);
        System.out.println("I: " + new Date() + ": IP Provider: " + ipProvider);
        System.out.println("I: " + new Date() + ": Current IP : " + currentIP);
        System.out.println("I: " + new Date() + ": ************************************************");
    }
}