package com.lootfactory.util;
import com.lootfactory.factory.FactoryRarity; import org.bukkit.configuration.file.FileConfiguration;
import java.util.EnumMap; import java.util.Map;
public class Cfg {
  public static Map<FactoryRarity, Double> getRarityWeights(FileConfiguration c){
    Map<FactoryRarity,Double> m=new EnumMap<>(FactoryRarity.class);
    m.put(FactoryRarity.COMMON, c.getDouble("shop.weights.COMMON",0.55));
    m.put(FactoryRarity.UNCOMMON, c.getDouble("shop.weights.UNCOMMON",0.26));
    m.put(FactoryRarity.RARE, c.getDouble("shop.weights.RARE",0.13));
    m.put(FactoryRarity.EPIC, c.getDouble("shop.weights.EPIC",0.05));
    m.put(FactoryRarity.LEGENDARY, c.getDouble("shop.weights.LEGENDARY",0.009));
    m.put(FactoryRarity.INSANE, c.getDouble("shop.weights.INSANE",0.001));
    double sum=0; for(double v:m.values()) sum+=v; if(sum>0){ for(FactoryRarity r:m.keySet()) m.put(r, m.get(r)/sum); } return m;
  }
}
