package com.lootfactory.economy;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit; import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider; import org.bukkit.plugin.java.JavaPlugin;
public class Eco {
  private final JavaPlugin plugin; private Economy econ;
  public Eco(JavaPlugin plugin){ this.plugin=plugin; }
  public boolean hook(){ if(plugin.getServer().getPluginManager().getPlugin("Vault")==null) return false;
    RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
    if(rsp==null) return false; econ=rsp.getProvider(); return econ!=null; }
  public boolean has(double amount, OfflinePlayer p){ return econ!=null && econ.getBalance(p)>=amount; }
  public boolean withdraw(OfflinePlayer p, double a){ return econ!=null && econ.withdrawPlayer(p,a).transactionSuccess(); }
  public void deposit(OfflinePlayer p, double a){ if(econ!=null) econ.depositPlayer(p,a); }
  public String fmt(double a, String sym){ return sym + String.format("%.2f", a); }
}
