package cc.yggdrasil.ddns53;

import java.util.Date;

public class Ddns53Config {
    String ddnscfg_Filename     = null;
    String ddnscfg_HostedZoneId = null;
    String ddnscfg_DomainName   = null;
    String ddnscfg_CurrentIP    = null;
    String ddnscfg_IPProvider   = null;

    public Ddns53Config(String FileName,
                        String HostedZoneID,
                        String DomainName,
                        String IPProvider,
                        String CurrentIP) {
        ddnscfg_Filename     = FileName;
        ddnscfg_HostedZoneId = HostedZoneID;
        ddnscfg_DomainName   = DomainName;
        ddnscfg_IPProvider   = IPProvider;
        ddnscfg_CurrentIP    = CurrentIP;
    }

    public void print_details() {
        System.out.println("I: " + new Date() + ": ******** Current Configuration Settings ********");
        System.out.println("I: " + new Date() + ": Config File: " + ddnscfg_Filename);
        System.out.println("I: " + new Date() + ": Zone ID    : " + ddnscfg_HostedZoneId);
        System.out.println("I: " + new Date() + ": Domain Name: " + ddnscfg_DomainName);
        System.out.println("I: " + new Date() + ": IP Provider: " + ddnscfg_IPProvider);
        System.out.println("I: " + new Date() + ": Current IP : " + ddnscfg_CurrentIP);
        System.out.println("I: " + new Date() + ": ************************************************");
    }
}