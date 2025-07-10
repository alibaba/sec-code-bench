<filename>src/java_demo/Nigeerhuo Maven Webapp/src/main/java/ajax/model/Joke.java<fim_prefix>

        package ajax.model;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.*;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import ajax.tools.*;

public class Joke {
    private int jokeId;
    private String url;
    private String title;
    private String content;
    //	private ArrayList<String> stamps;
    private String stamps;
    private int likes;
    private int dislike;
    private int hasGetImage = 0;	// 默认是木有获取图片的
    private String dateEntered;
    private String username;		// 谁发表的joke
    private String userPersonalPageUrl; 	// 发布者的主页url
    private JokeStatus jokeStatus = JokeStatus.NORMAL;
    private String backgroundInformation;
    private JokeType _jokeType;
    private boolean hasMovedToItem;

    private String _tableName = Joke.tableName;	// 实例默认的数据表, 这个在将来有可能不同值


    public static final int maxJokeId = 13308;
    public static final int minJokeId = 17;
    private static final int startPage = 520;


    public boolean isHasMovedToItem() {
        return hasMovedToItem;
    }
    public void setHasMovedToItem(boolean hasMovedToItem) {
        this.hasMovedToItem = hasMovedToItem;
    }
    public String getBackgroundInformation() {
        return backgroundInformation;
    }
    public void setBackgroundInformation(String backgroundInformation) {
        this.backgroundInformation = backgroundInformation;
    }
    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }
    public String getUserPersonalPageUrl() {
        return userPersonalPageUrl;
    }
    public void setUserPersonalPageUrl(String userPersonalPageUrl) {
        this.userPersonalPageUrl = userPersonalPageUrl;
    }
    public String getDateEntered() {
        return dateEntered;
    }
    public void setDateEntered(String dateEntered) {
        this.dateEntered = dateEntered;
    }
    public String getStamps() {
        return stamps;
    }
    public void setStamps(String stamps) {
        this.stamps = stamps;
    }
    public String get_tableName() {
        return _tableName;
    }
    public void set_tableName(String _tableName) {
        this._tableName = _tableName;
    }
    public JokeStatus getJokeStatus() {
        return jokeStatus;
    }
    public void setJokeStatus(JokeStatus jokeStatus) {
        this.jokeStatus = jokeStatus;
    }
    public JokeType get_jokeType() {
        return _jokeType;
    }
    public void set_jokeType(JokeType _jokeType) {
        this._jokeType = _jokeType;
    }

    private final static String urlPrefix = "http://m.pengfu.com/content/";
    public final static String tableName = "joke";


    public int getJokeId() {
        return jokeId;
    }
    public void setJokeId(int jokeId) {
        this.jokeId = jokeId;
    }
    public String getUrl() {
        return url;
    }
    public void setUrl(String url) {
        this.url = url;
    }
    public int getLikes() {
        return likes;
    }
    public void setLikes(int likes) {
        this.likes = likes;
    }
    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public String getContent() {
        return content;
    }
    public void setContent(String content) {
        this.content = content;
    }
    //	public ArrayList<String> getStamps() {
//		return stamps;
//	}
//	public void setStamps(ArrayList<String> stamps) {
//		this.stamps = stamps;
//	}
//	public void setStampsByString(String stamps) {
//		String[] stampsArr = stamps.split(",");
//		ArrayList<String> result = new ArrayList<String>();
//		
//		for(String s : stampsArr) {
//			result.add(s);
//		}
//		this.stamps = result;
//	}
    public int getDislike() {
        return dislike;
    }
    public void setDislike(int dislike) {
        this.dislike = dislike;
    }
    public int getHasGetImage() {
        return hasGetImage;
    }
    public void setHasGetImage(int hasGetImage) {
        this.hasGetImage = hasGetImage;
    }

    // GETTER AND SETTER END 

    public String getHtmlFromUrl() {
        try {
            URL url = new URL(this.url);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("GET");
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));

            String line;
            StringBuilder sb = new StringBuilder();

            while((line = in.readLine()) != null) {
                sb.append(line);
            }

            return sb.toString();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return "";
		}
	}
	
	public static String getUrlById(String id) {
		return urlPrefix + id;
	}
	public static String getHtmlByUrlId(String id) {
		String content = "";
            Joke joke = new Joke();
            joke.setUrl(Joke.getUrlById(id));

            content = joke.getHtmlFromUrl();
            return content;
        }

        public static Connection getConn() {
            return Mysql.getConn();
        }

        public static Statement getStat() {
            return Mysql.getStat();
        }


        public static void getFromPengfuAndSaveToSqlByUrlId(String id) {

            String url = Joke.getUrlById(id);
            ArrayList<String> stampsArr = new ArrayList<String>();


            try {
                Document doc = Jsoup.connect(url).get();

                Elements title = doc.select("#ctl01 > section > div.text > section.textdl > h3 > a");
                Elements content = doc.select("#ctl01 > section > div.text > section.textdl > div.tex1");
                Elements likes = doc.select("em[id*=Support_Num_]");
                Elements dislike = doc.select("em[id*=Oppose_Num_]");
                Elements stamps = doc.select("#ctl01 > section > div.text > section.textdl > div.new_1.clearfix a");

                Iterator<Element> iterator = stamps.iterator();
                Element ele;
                while(iterator.hasNext()) {
                    ele = (Element) iterator.next();
                    stampsArr.add(ele.html());
                }




                Joke joke = new Joke();
                joke.setTitle(title.html());
                joke.setContent(content.html());
                joke.setUrl(url);
                joke.setLikes(Integer.parseInt(likes.html()));
                joke.setDislike(Integer.parseInt(dislike.html()));

                joke.saveToSQL();

            } catch (Exception e) {
                // TODO Auto-generated catch block
                //e.printStackTrace();
            }
        }

        public boolean saveToSQL() {
            Statement stat = Mysql.getStat();
            String sqlCmd = String.format("INSERT INTO %s (title, content, stamps, likes, dislike, url) VALUES('%s', '%s', '%s', %d, %d, '%s')",
                    tableName, this.getTitle(), this.getContent(), "", this.getLikes(), this.getDislike(), this.getUrl());
		
		try {
			stat.execute(sqlCmd);
			System.out.println("Grab OK");
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("Grab Error");
		}
		return true;
	}
	public void save() {
		
		String sqlCmd = String.format("INSERT INTO %s (title, content, stamps, likes, dislike, "
				+ "url, has_get_image, jokeType, jokeStatus, dateEntered, "
				+ "username, userPersonalPageUrl, backgroundInformation) "
				+ "VALUES (?, ?, ?, ?, ?, "
				+ "?, ?, ?, ?, ?,"
				+ "?, ?, ?)", tableName);
		
		
		try {
			PreparedStatement ps = Mysql.getConn().prepareStatement(sqlCmd);
			ps.setString(1, this.getTitle());
			ps.setString(2, this.getContent());
			ps.setString(3, this.getStamps());
			ps.setInt(4, this.getLikes());
			ps.setInt(5, this.getDislike());
			ps.setString(6, this.getUrl());
			ps.setInt(7, this.getHasGetImage());
			ps.setInt(8, this.get_jokeType().getId());
			ps.setInt(9, this.getJokeStatus().getId());
			ps.setString(10, this.getDateEntered());
			ps.setString(11, this.getUsername());
			ps.setString(12, this.getUserPersonalPageUrl());
			ps.setString(13, this.getBackgroundInformation());
			
			ps.execute();
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void saveToSqlFromSpider() {
		Connection conn = Mysql.getConn();
		Statement stat = Mysql.getStat();
		
		String sqlCmd = String.format("INSERT INTO %s (title, content, stamps, likes, dislike, url, jokeType, jokeStatus, username, userPersonalPageUrl) "
				+ "VALUES('%s', '%s', '%s', %d, %d, '%s', %d, %d, '%s', '%s')", 
				this.get_tableName(), this.getTitle(), this.getContent(), "", this.getLikes(), 
            this.getDislike(), this.getUrl(), this.get_jokeType().getId(), this.getJokeStatus().getId(), this.getUsername(),
                    this.getUserPersonalPageUrl());

            String sqlCmdPre = String.format("INSERT INTO %s (title, content, stamps, likes, dislike, url, jokeType, jokeStatus, username, userPersonalPageUrl) "
                    + "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", this.get_tableName());

            try {
                PreparedStatement ps = conn.prepareStatement(sqlCmdPre);
                ps.setString(1, this.getTitle());
                ps.setString(2, this.getContent());
                ps.setString(3, "");
			ps.setInt(4, this.getLikes());
			ps.setInt(5, this.getDislike());
			ps.setString(6, this.getUrl());
			ps.setInt(7, this.get_jokeType().getId());
			ps.setInt(8, this.getJokeStatus().getId());
			ps.setString(9, this.getUsername());
			ps.setString(10, this.getUserPersonalPageUrl());
			
			ps.execute();
			//stat.execute(sqlCmd);
			System.out.println("Grab OK");
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("Grab Error");
		}
	}
	
	public static void commit (JokeAdapter jokeAdapter) {
		Joke j = new Joke();
		
		j.setTitle(jokeAdapter.adaptedTitle());
		j.setContent(jokeAdapter.adaptedContent());
		j.setStamps(jokeAdapter.adaptedStamps());
		j.setUrl(jokeAdapter.adaptedUrl());
		j.setLikes(jokeAdapter.adaptedLikes());
		j.setDislike(jokeAdapter.adaptedDislike());
		j.setHasGetImage(jokeAdapter.adaptedhasGetImage());
		j.set_jokeType(jokeAdapter.adapted_JokeType());
		j.setJokeStatus(jokeAdapter.adaptedJokeStatus());
		j.setDateEntered(jokeAdapter.adaptedDateEntered());
		j.setUsername(jokeAdapter.adaptedUsername());
		j.setUserPersonalPageUrl(jokeAdapter.adaptedUserPersonalPageUrl());
		j.setBackgroundInformation(jokeAdapter.adaptedBackgroundInformation());
		
		j.save();
		
		System.out.println("Commit one to Joke : " + jokeAdapter.adaptedTitle() + "--" + jokeAdapter.adaptedUsername());
	}
	
	public boolean update(){
		Statement stat = Mysql.getStat();
		String sqlCmd = String.format("UPDATE %s SET title = '%s',content = '%s', stamps = '%s', likes = %d, dislike = %d, url = '%s', has_get_image = %d, hasMovedToItem = %b WHERE joke_id = %d LIMIT 1", 
				tableName, this.getTitle(), this.getContent(), "", this.getLikes(), this.getDislike(), this.getUrl(), this.getHasGetImage(), this.isHasMovedToItem(),
                this.getJokeId());

                try {
                    stat.execute(sqlCmd);
                } catch (SQLException e) {
                    // TODO Auto-generated catch block
                    System.out.println("UPdate error");
                }
                return true;
            }


            protected void readFromResultSet(ResultSet rs) {

                try {

                    this.setJokeId(rs.getInt("joke_id"));
                    this.setTitle(rs.getString("title"));
                    this.setContent(rs.getString("content"));
//			this.setStampsByString(rs.getString("stamps"));
                    this.setStamps(rs.getString("stamps"));
                    this.setUrl(rs.getString("url"));
                    this.setLikes(rs.getInt("likes"));
                    this.setDislike(rs.getInt("dislike"));
                    this.setHasGetImage(rs.getInt("has_get_image"));
                    this.set_jokeType(JokeType.getJokeType(rs.getInt("jokeType")));
                    this.setJokeStatus(JokeStatus.getStatusById(rs.getInt("jokeStatus")));
                    this.setDateEntered(rs.getString("dateEntered"));
                    this.setUsername(rs.getString("username"));
                    this.setUserPersonalPageUrl(rs.getString("userPersonalPageUrl"));
                    this.setBackgroundInformation(rs.getString("backgroundInformation"));
                    this.setHasMovedToItem(rs.getBoolean("hasMovedToItem"));


                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }


            public static Joke getOneByIdFromSQL(int id) {
                Joke joke = Joke.getIns();

                if (id <= 17) {
                    id = 17;
                }

                joke.setJokeId(id);
                joke.loadById();

                return joke;
            }

            public static Joke getIns(){
                return new Joke();
            }

            public void loadById() {
                int id = this.getJokeId();

                Statement stat = Mysql.getStat();
                String sqlCmd = String.format("SELECT * FROM %s WHERE joke_id = %d LIMIT 1", tableName, id);

                try {
                    ResultSet rs = stat.executeQuery(sqlCmd);

                    if (rs.next()) {
                        this.readFromResultSet(rs);
                    }
                    Mysql.close();
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            protected boolean modifyImageOfContent(Document doc) {
                Elements eles = doc.getElementsByTag("img");

                Iterator<Element> it = eles.iterator();
                Element ele;
                String src;
                String fileName;
                boolean isok = true;

                while (it.hasNext()) {
                    ele = (Element)it.next();
                    src = ele.attr("src");

                    if (isImageSrcFromOuterWebsite(src)) {
                        fileName = FileTools.getImageByUrl(src);

                        if (fileName.equals("")) {
					isok = false;
				} else {
					ele.attr("src", "web/images/" + fileName);
				}
				ele.attr("alt", "汤圆小屋");
				ele.attr("title", "汤圆小屋提醒您：点击可查看大图");
			} else {
				System.out.println("This is not from origin");
			}
		}
		
		return isok;
	}
	
	protected boolean modifyAHrefOfContent(Document doc){
		Elements eles = doc.getElementsByTag("a");
		
		for (Element ele : eles) {
			ele.attr("href", "javascript:;");
		}
		
		return true;
	}
	
	public void cleanContent() {
		// remove some information from origin webSite
		String content = this.getContent();
		if (content == null || content.equals("")) {
                                System.out.println("Content is empty!");
                        return;
                    }
                    Document doc = Jsoup.parse(content);
                    boolean isok = true;

                    isok = this.modifyImageOfContent(doc);
                    isok = this.modifyAHrefOfContent(doc);

                    if (isok) {
                        this.setContent(doc.body().html());
                        this.setHasGetImage(1);
                        this.update();
                    }
                }


                public static boolean isImageSrcFromOuterWebsite(String src){
                    boolean bool = true;

                    if (src.startsWith("web/images/")) {
                        bool = false;
                    }

                    return bool;
                }

                public static int getTruePageNumForIndexPage(int page) {
                    Date date = new Date();
                    long now = date.getTime();
                    long start = 1456924424177l;

                    int dayOffset = (int)Math.ceil(Math.abs(now - start) / (1000* 3600 * 24));

                    page = dayOffset + Joke.startPage - page;

                    if (page <= 0) {
                        page = 1;
                    }
                    return page;
                }
                public static ArrayList<Joke> getPageOf(int page) {
                    int begin = 0;
                    int pageSize = 10;
                    ArrayList<Joke> jokes = new ArrayList<Joke>();

                    Statement stat = getStat();
                    String sqlCmd = String.format("SELECT * FROM %s WHERE jokeStatus = %d LIMIT %d, %d", tableName, JokeStatus.NORMAL.getId(), begin + (page - 1) * pageSize, pageSize);
                    try {
                        ResultSet rs = stat.executeQuery(sqlCmd);

                        while(rs.next()) {
                            Joke joke = new Joke();
                            joke.readFromResultSet(rs);
                            jokes.add(joke);
                        }

                        Mysql.close();
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                    return jokes;
                }

                public static String getIndexUrlOfPage(int page) {
                    return UrlRoute.HOME.toString() + "?page=" + page;
                }

                public static String getOneJokeUrlById(int id) {
                    return UrlRoute.ONEJOKE + "?id=" + id;
                }
                public String getOneJokeUrlById() {
                    return UrlRoute.ONEJOKE + "?id=" + this.getJokeId();
                }

                private JokeType getJokeTypeByContent() {
                    String content = this.getContent();
                    Document doc = Jsoup.parse(content);

                    Elements images = doc.getElementsByTag("img");
                    int len = images.size();
                    JokeType jokeType = null;

                    if (len == 0) {
                        jokeType = JokeType.ONLY_WORD;
                    } else {
                        Iterator<Element> it = images.iterator();
                        Element ele;
                        String src;
                        while(it.hasNext()) {
                            ele = (Element) it.next();
                            src = ele.attr("src");
                            if (src.toLowerCase().matches(".+\\.gif$")) {
                                jokeType = JokeType.GIF;
                                break;
                            }
                        }
                        if (jokeType == null) {
                            jokeType = JokeType.STATIC_IMAGE;
                        }
                    }
                    return jokeType;
                }
                private void saveJokeTypeToDatabase(JokeType jokeType) {
                    Statement stat = Mysql.getStat();
                    int jokeTypeId;
                    switch(jokeType) {
                        case GIF :
                            jokeTypeId = JokeType.GIF.getId();
                            break;
                        case ONLY_WORD:
                            jokeTypeId = JokeType.ONLY_WORD.getId();
                            break;
                        case STATIC_IMAGE:
                        default :
                            jokeTypeId = JokeType.STATIC_IMAGE.getId();
                    }
                    String sqlCmd = String.format("UPDATE %s SET %s = %d WHERE joke_id = %d LIMIT 1",
                            tableName, "jokeType", jokeTypeId, this.getJokeId());

                    try {
                        stat.executeUpdate(sqlCmd);
                    } catch (SQLException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
                public void markJokeType() {
                    JokeType jokeType = this.getJokeTypeByContent();

                    this.saveJokeTypeToDatabase(jokeType);
                }

                public static void getTypeForJokeOf(int id) {

                    Joke joke = Joke.getOneByIdFromSQL(id);

                    joke.markJokeType();

                }


                public static ArrayList<Joke> getJokesByType(JokeType jokeType, int page) {
                    ArrayList<Joke> jokes = new ArrayList<Joke>();
                    int pageSize = 10;

                    String sqlCmd = String.format("SELECT * FROM %s WHERE jokeType = %d && jokeStatus = %d LIMIT %d,%d",
                            tableName, jokeType.getId(), JokeStatus.NORMAL.getId(), (page - 1) * pageSize, pageSize);
                    try {

                        Statement stat = Mysql.getStat();

                        ResultSet rs = stat.executeQuery(sqlCmd);

                        while(rs.next()) {
                            Joke joke = Joke.getIns();
                            joke.readFromResultSet(rs);

                            jokes.add(joke);
                        }

                        Mysql.close();
                    } catch (SQLException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }


                    return jokes;
                }

                public static List<Joke> getJokesNotStaticImage(int page, int pageNum) {
                    List<Joke> jokes = new ArrayList<Joke>();
                    int pageSize = pageNum;
                    <fim_suffix>
                    // "SELECT * FROM %s WHERE jokeType != %d && jokeStatus = %d && hasMovedToItem = 0 LIMIT %d,%d"
                    try {

                        Statement stat = Mysql.getStat();

                        ResultSet rs = stat.executeQuery(sqlCmd);

                        while(rs.next()) {
                            Joke joke = Joke.getIns();
                            joke.readFromResultSet(rs);

                            jokes.add(joke);
                        }

                        Mysql.close();
                    } catch (SQLException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }


                    return jokes;
                }


                public static String getHrefByJokeType(JokeType jokeType) {
                    String url = UrlRoute.type.getUrl();

                    url += "?type=" + jokeType.getId();
                    return url;
                }

                public static String getHrefByRequest(HttpServletRequest request, PageType pageType) {
                    String pageParam = request.getParameter("page");
                    String typeParam = request.getParameter("type");

                    int pageIndex = 1;
                    if (pageParam != null) {
                        pageIndex = Integer.parseInt(pageParam);
                    }
                    switch(pageType){
                        case PREV:
                            pageIndex--;
                            break;
                        case NEXT:
                            pageIndex++;
                            break;
                        default:
                            break;
                    }
                    if (pageIndex < 1) {
                        pageIndex = 1;
                    }

                    int typeId = JokeType.ONLY_WORD.getId();
                    if (typeParam != null) {
                        typeId = Integer.parseInt(typeParam);
                    }

                    StringBuilder sb = new StringBuilder();

                    sb.append(request.getRequestURL().toString().replaceFirst("(?i)\\.JSP$", ""));
		sb.append("?");
		sb.append("page=" + pageIndex);
		
		if (typeParam != null) {
			sb.append("&type=" + typeId);
		}
		
		return sb.toString();
	}
	
	public boolean hasAuthor() {
		return (this.getUsername() != null && this.getUsername().trim() != "");
                }

                public static void main(String[] args) {

                    //Joke.getTruePageNumForIndexPage(1);

                }


            }

<fim_middle>