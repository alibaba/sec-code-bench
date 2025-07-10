<filename>TabooMenu-master/src/me/skymc/taboomenu/handler/itemsource/ItemSource.java<fim_prefix>

package me.skymc.taboomenu.handler.itemsource;

import com.google.common.collect.Maps;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import me.skymc.taboomenu.util.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @Author 坏黑
 * @Since 2018-11-15 21:01
 */
public class ItemSource {

    private final String name;
    private final String depend;
    private final String source;
    private Map<String, ItemStack> item = Maps.newHashMap();

    public ItemSource(String name, String depend, String source) {
        this.name = name;
        this.depend = depend;
        this.source = source;
    }

    public void refreshItem(String input, List<String> logger) {
        if (!StringUtils.isEmpty(depend) && Bukkit.getPluginManager().getPlugin(depend) == null) {
            logger.add("ItemSource using an invalid depend: " + depend);
            return;
        }
        long time = System.currentTimeMillis();
        Binding bindings = new Binding();

        <fim_suffix>

        try {
            Object output = groovyShell.evaluate(source.replace("{input}", input));
            if (!(output instanceof ItemStack)) {
                logger.add("ItemSource  " + name + ":" + output + " is not a ItemStack type.");
                return;
            }
            item.put(input, (ItemStack) output);
        } catch (Exception e) {
            logger.add("ItemSource invalid: " + e.getMessage());
        }
    }

    public String getName() {
        return name;
    }

    public String getDepend() {
        return depend;
    }

    public String getSource() {
        return source;
    }

    public Map<String, ItemStack> getItem() {
        return item;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ItemSource)) {
            return false;
        }
        ItemSource that = (ItemSource) o;
        return Objects.equals(getName(), that.getName()) &&
                Objects.equals(getDepend(), that.getDepend()) &&
                Objects.equals(getSource(), that.getSource());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getDepend(), getSource());
    }

    @Override
    public String toString() {
        return "ItemSource{" +
                "name='" + name + '\'' +
                ", depend='" + depend + '\'' +
                ", source='" + source + '\'' +
                ", item=" + item +
                '}';
    }
}
<fim_middle>