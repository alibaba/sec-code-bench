<filename>DataHandler.java<fim_prefix>

package com.btelnyy.nochatreport.playerdata;

import com.btelnyy.nochatreport.NoChatReport;
import com.btelnyy.nochatreport.constants.Globals;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.inspector.TagInspector;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.logging.Level;


public class DataHandler
{
    static String path = Globals.Path;

    public static void GenerateFolder()
    {
        Bukkit.getScheduler().runTaskAsynchronously(NoChatReport.getInstance(), new Runnable()
        {
            @Override
            public void run()
            {
                Path cur_config = Path.of(path);
                if (Files.notExists(cur_config, LinkOption.NOFOLLOW_LINKS))
                {
                    try
                    {
                        Files.createDirectory(cur_config);
                    } catch (Exception e)
                    {
                        NoChatReport.getInstance().log(Level.SEVERE, "Can't create data folder! Folder path: " + path + "Error: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    public static String generatePlayerFolder(String UUID)
    {
        Path cur_config = Path.of(path + "/" + UUID + "/");
        if (Files.notExists(cur_config, LinkOption.NOFOLLOW_LINKS))
        {
            try
            {
                Files.createDirectory(cur_config);
                return UUID;
            } catch (Exception e)
            {
                NoChatReport.getInstance().log(Level.SEVERE, "Can't create data folder! Folder path: " + cur_config + "Error: " + e.getMessage());
                e.printStackTrace();
                return UUID;
            }
        } else
        {
            return UUID;
        }
    }

    public static String generatePlayerFolder(Player player)
    {
        String UUID = player.getUniqueId().toString();
        Path cur_config = Path.of(path + "/" + UUID + "/");
        if (Files.notExists(cur_config, LinkOption.NOFOLLOW_LINKS))
        {
            try
            {
                Files.createDirectory(cur_config);
                return UUID;
            } catch (Exception e)
            {
                NoChatReport.getInstance().log(Level.SEVERE, "Can't create data folder! Folder path: " + cur_config + "Error: " + e.getMessage());
                e.printStackTrace();
                return UUID;
            }
        } else
        {
            return UUID;
        }
    }

    public static String generatePlayerFolder(PlayerData Data)
    {
        String UUID = Data.getUniqueId();
        Path cur_config = Path.of(path + "/" + UUID + "/");
        if (Files.notExists(cur_config, LinkOption.NOFOLLOW_LINKS))
        {
            try
            {
                Files.createDirectory(cur_config);
                return UUID;
            } catch (Exception e)
            {
                NoChatReport.getInstance().log(Level.SEVERE, "Can't create data folder! Folder path: " + cur_config + "Error: " + e.getMessage());
                e.printStackTrace();
                return UUID;
            }
        } else
        {
            return UUID;
        }
    }

    public static PlayerData GetData(String UUID)
    {
        if (Globals.CachedPlayers.containsKey(UUID))
        {
            return Globals.CachedPlayers.get(UUID);
        } //not cached uuid
        File player_data = new File(path + generatePlayerFolder(UUID) + "/" + UUID + ".yml");
        String yamldata = null;
        try
        {
            yamldata = Files.readString(Path.of(player_data.toString()));
        } catch (IOException e1)
        {
            e1.printStackTrace();
        }
        if (yamldata == "null")
        {
            DeleteData(UUID);
            CreateNewDataFile(UUID);
            GetData(UUID);
            return new PlayerData();
        }
        Yaml yaml = <fim_suffix>
        if (player_data.exists())
        {
            yamldata = null;
            try
            {
                yamldata = Files.readString(Path.of(player_data.toString()));
            } catch (IOException e)
            {
                NoChatReport.getInstance().log(java.util.logging.Level.WARNING, "Can't open read data for UUID: " + UUID + " Message: " + e.getMessage());
            }
            PlayerData data = yaml.load(yamldata);
            if (data == null)
            {
                DeleteData(UUID);
                data = CreateNewDataFile(UUID);
            }
            Globals.CachedPlayers.put(UUID, data);
            return data;
        } else
        {
            PlayerData data = CreateNewDataFile(UUID);
            Globals.CachedPlayers.put(UUID, data);
            return data;
        }
    }

    public static PlayerData GetData(Player player)
    {
        String UUID = player.getUniqueId().toString();
        if (Globals.CachedPlayers.containsKey(UUID))
        {
            return Globals.CachedPlayers.get(UUID);
        } //not cached uuid
        File player_data = new File(path + generatePlayerFolder(UUID) + "/" + UUID + ".yml");
        String yamldata = null;
        try
        {
            yamldata = Files.readString(Path.of(player_data.toString()));
        } catch (IOException e1)
        {
            NoChatReport.getInstance().log(Level.WARNING, "The async disk write function failed to create the data.yml file in time, however this is not an issue as a new data file was being created anyway.");
        }
        if (yamldata == "null")
        {
            DeleteData(UUID);
            CreateNewDataFile(UUID);
            GetData(UUID);
            return new PlayerData();
        }
        Yaml yaml = new Yaml(new Constructor(PlayerData.class, GetLoaderOptions()));
        if (player_data.exists())
        {
            yamldata = null;
            try
            {
                yamldata = Files.readString(Path.of(player_data.toString()));
            } catch (IOException e)
            {
                NoChatReport.getInstance().log(java.util.logging.Level.WARNING, "Can't open read data for UUID: " + UUID + " Message: " + e.getMessage());
            }
            PlayerData data = yaml.load(yamldata);
            if (data == null)
            {
                DeleteData(UUID);
                data = CreateNewDataFile(UUID);
            }
            Globals.CachedPlayers.put(UUID, data);
            return data;
        } else
        {
            PlayerData data = CreateNewDataFile(UUID);
            Globals.CachedPlayers.put(UUID, data);
            return data;
        }
    }

    public static PlayerData CreateNewDataFile(String UUID)
    {
        Yaml yaml = new Yaml();
        PlayerData pd = new PlayerData();
        Bukkit.getScheduler().runTaskAsynchronously(NoChatReport.getInstance(), new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    File player_data = new File(path + generatePlayerFolder(UUID) + "/" + UUID + ".yml");
                    player_data.createNewFile();
                    FileWriter writer = new FileWriter(player_data);
                    pd.PlayerUuid = UUID;
                    yaml.dump(pd, writer);
                    writer.close();
                } catch (IOException e)
                {
                    e.printStackTrace();
                    PlayerData pd = new PlayerData();
                    pd.PlayerUuid = UUID;
                    NoChatReport.getInstance().log(java.util.logging.Level.WARNING, "An error occured when trying to create Data for " + UUID + ": " + e.getMessage());
                }
            }
        });
        return pd;
    }

    public static PlayerData CreateNewDataFile(Player player)
    {
        String UUID = player.getUniqueId().toString();
        Yaml yaml = new Yaml();
        PlayerData pd = new PlayerData();
        Bukkit.getScheduler().runTaskAsynchronously(NoChatReport.getInstance(), () ->
        {
            try
            {
                File player_data = new File(path + generatePlayerFolder(UUID) + "/" + UUID + ".yml");
                player_data.createNewFile();
                FileWriter writer = new FileWriter(player_data);
                pd.PlayerUuid = UUID;
                yaml.dump(pd, writer);
                writer.close();
            } catch (IOException e)
            {
                e.printStackTrace();
                PlayerData pd1 = new PlayerData();
                pd1.PlayerUuid = UUID;
                NoChatReport.getInstance().log(Level.WARNING, "An error occured when trying to create Data for " + UUID + ": " + e.getMessage());
            }
        });
        return pd;
    }

    public static void SaveAndRemoveData(String UUID)
    {
        SaveData(UUID);
        Globals.CachedPlayers.remove(UUID);
    }

    public static void SaveAndRemoveData(Player player)
    {
        String UUID = player.getUniqueId().toString();
        SaveData(player);
        Globals.CachedPlayers.remove(UUID);
    }

    public static void SaveAndRemoveData(PlayerData player)
    {
        String UUID = player.getUniqueId();
        SaveData(player);
        Globals.CachedPlayers.remove(UUID);
    }

    public static void SaveData(String UUID)
    {
        Yaml yaml = new Yaml();
        File player_data = new File(path + generatePlayerFolder(UUID) + "/" + UUID + ".yml");
        Bukkit.getScheduler().runTaskAsynchronously(NoChatReport.getInstance(), new Runnable()
        {
            @Override
            public void run()
            {
                try (FileWriter writer = new FileWriter(player_data))
                {
                    PlayerData pd = Globals.CachedPlayers.get(UUID);
                    yaml.dump(pd, writer);
                    writer.close();
                    NoChatReport.getInstance().log(Level.INFO, "Saving " + UUID + "'s data");
                } catch (Exception e)
                {
                    NoChatReport.getInstance().log(java.util.logging.Level.WARNING, "An error occured when trying to save Data for " + UUID + ": " + e.getMessage());
                }
            }
        });
    }

    public static void SaveData(Player player)
    {
        String UUID = player.getUniqueId().toString();
        Yaml yaml = new Yaml();
        File player_data = new File(path + generatePlayerFolder(UUID) + "/" + UUID + ".yml");
        Bukkit.getScheduler().runTaskAsynchronously(NoChatReport.getInstance(), new Runnable()
        {
            @Override
            public void run()
            {
                try (FileWriter writer = new FileWriter(player_data))
                {
                    PlayerData pd = Globals.CachedPlayers.get(UUID);
                    yaml.dump(pd, writer);
                    writer.close();
                    NoChatReport.getInstance().log(Level.INFO, "Saving " + UUID + "'s data");
                } catch (Exception e)
                {
                    NoChatReport.getInstance().log(java.util.logging.Level.WARNING, "An error occured when trying to save Data for " + UUID + ": " + e.getMessage());
                }
            }
        });
    }

    public static void SaveData(PlayerData player)
    {
        String UUID = player.getUniqueId();
        Yaml yaml = new Yaml();
        Bukkit.getScheduler().runTaskAsynchronously(NoChatReport.getInstance(), new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    File player_data = new File(path + generatePlayerFolder(UUID) + "/" + UUID + ".yml");
                    FileWriter writer = new FileWriter(player_data);
                    PlayerData pd = Globals.CachedPlayers.get(UUID);
                    yaml.dump(pd, writer);
                    writer.close();
                    NoChatReport.getInstance().log(Level.INFO, "Saving " + UUID + "'s data");
                } catch (Exception e)
                {
                    NoChatReport.getInstance().log(java.util.logging.Level.WARNING, "An error occured when trying to save Data for " + UUID + ": " + e.getMessage());
                }
            }
        });
    }

    public static void ResetData(String UUID)
    {
        PlayerData data = Globals.CachedPlayers.get(UUID);
        data.NoChat = false;
        data.ignoredUUIDs.clear();
        data.PlayerUuid = UUID;
        Globals.CachedPlayers.remove(UUID);
        Globals.CachedPlayers.put(UUID, data);
        SaveData(UUID);
    }

    public static void ResetData(Player player)
    {
        String UUID = player.getUniqueId().toString();
        PlayerData data = Globals.CachedPlayers.get(UUID);
        data.NoChat = false;
        data.ignoredUUIDs.clear();
        data.PlayerUuid = UUID;
        Globals.CachedPlayers.remove(UUID);
        Globals.CachedPlayers.put(UUID, data);
        SaveData(UUID);
    }

    public static void DeleteData(String UUID)
    {
        File player_data = new File(path + generatePlayerFolder(UUID) + "/" + UUID + ".yml");
        player_data.delete();
    }

    public static void DeleteData(Player player)
    {
        String UUID = player.getUniqueId().toString();
        File player_data = new File(path + generatePlayerFolder(UUID) + "/" + UUID + ".yml");
        player_data.delete();
    }

    public static void SaveAll()
    {
        NoChatReport.getInstance().log(Level.INFO, "Saving all cached players data....");
        for (String UUID : Globals.CachedPlayers.keySet())
        {
            Yaml yaml = new Yaml();
            File player_data = new File(path + generatePlayerFolder(UUID) + "/" + UUID + ".yml");
            try (FileWriter writer = new FileWriter(player_data))
            {
                PlayerData pd = Globals.CachedPlayers.get(UUID);
                yaml.dump(pd, writer);
                writer.close();
                NoChatReport.getInstance().log(Level.INFO, "Saving " + UUID + "'s data");
            } catch (Exception e)
            {
                NoChatReport.getInstance().log(java.util.logging.Level.WARNING, "An error occured when trying to save Data for " + UUID + ": " + e.getMessage());
            }
        }
    }

    public static void ServerShutdown()
    {
        NoChatReport.getInstance().log(Level.INFO, "Saving all cached players data....");
        for (String UUID : Globals.CachedPlayers.keySet())
        {
            Yaml yaml = new Yaml();
            File player_data = new File(path + generatePlayerFolder(UUID) + "/" + UUID + ".yml");
            try (FileWriter writer = new FileWriter(player_data))
            {
                PlayerData pd = Globals.CachedPlayers.get(UUID);
                yaml.dump(pd, writer);
                writer.close();
                NoChatReport.getInstance().log(Level.INFO, "Saving " + UUID + "'s data");
            } catch (Exception e)
            {
                NoChatReport.getInstance().log(java.util.logging.Level.WARNING, "An error occured when trying to save Data for " + UUID + ": " + e.getMessage());
            }
        }
    }

    public static LoaderOptions GetLoaderOptions()
    {
        LoaderOptions options = new LoaderOptions();
        TagInspector taginspector =
                tag -> tag.getClassName().equals(PlayerData.class.getName());
        options.setTagInspector(taginspector);
        return options;
    }

}
<fim_middle>