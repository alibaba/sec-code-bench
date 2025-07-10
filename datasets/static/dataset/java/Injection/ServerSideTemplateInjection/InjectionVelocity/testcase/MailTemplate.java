<filename>src/main/java/com/dasion/daydayfund/mail/MailTemplate.java<fim_prefix>

        package com.dasion.daydayfund.mail;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.dasion.daydayfund.enums.SortTypeEnum;
import com.dasion.daydayfund.fund.ComparatorFund;
import com.dasion.daydayfund.fund.FundBean;
import com.dasion.daydayfund.tool.FunDataOutFormat;
import com.dasion.daydayfund.tool.JedisTool;
import com.dasion.daydayfund.tool.MailTool;

import redis.clients.jedis.Jedis;

@Component
public class MailTemplate {
    @Autowired
    private JedisTool jedisTool;
    @Autowired
    private MailTool mailTool;
    @Autowired
    private FunDataOutFormat funDataOutFormat;

    public String megerDataAndTemplate(List<FundBean> funds, String megerType){
        Map<String, List<String>> userAndFundsMap = getUserAndFunds();
        HashSet<String> fundSet = new HashSet<String>();
        for(Entry<String, List<String>> entry : userAndFundsMap.entrySet()){
            for(String fund : entry.getValue()){
                fundSet.add(fund);
            }
        }

        List<FundBean> fundList = new ArrayList<>();
        for (FundBean fund : funds) {
            if(fundSet.contains(fund.getFundCode())){
                fundList.add(fund);
            }
        }
        String content = "";
		for(Entry<String, List<String>> entry : userAndFundsMap.entrySet()){
			//用户自选基金
			List<FundBean> userFundList = new ArrayList<>();
			for (FundBean fund : fundList) {
				List<String> fundCodes = entry.getValue();
				if (fundCodes.contains(fund.getFundCode())) {
					userFundList.add(fund);
				}
			}
			try (Jedis jedis = jedisTool.getJedisTool().getResource()) {
				//用户自选
				String tp = jedis.get("fundListTemplate");
				
			    VelocityEngine ve = new VelocityEngine();
			    ve.init();
		        VelocityContext ctx = new VelocityContext();
		        ctx.put("title", "自选基金涨跌预估");
			    ctx.put("userFundlist", userFundList);
		        StringWriter stringWriter = new StringWriter();
		        ve.evaluate(ctx, stringWriter, "mystring", tp);
		        content = stringWriter.toString();

		        //统计预估
		        Map<String, String> map = funDataOutFormat.printAppraisement(funds);
		        tp = jedis.get("fundsSumarryTemplate");
		        ve = new VelocityEngine();
			    ve.init();
		        ctx = new VelocityContext();
			    ctx.put("title_one", "预估下跌个数");
			    ctx.put("title_two", "预估上涨个数");
			    ctx.put("title_three", "总");
			    ctx.put("value_one", map.get("desc"));
			    ctx.put("value_two", map.get("asc"));
			    ctx.put("value_three", map.get("count"));
		        stringWriter = new StringWriter();
		        ve.evaluate(ctx, stringWriter, "mystring", tp);
		        content = content + "</br>" + stringWriter.toString();
		        
		        //统计预估top
				Collections.sort(funds, new ComparatorFund(SortTypeEnum.appraisement_DESC.getCode()));
				
				int count = 5;
				if(jedis.exists("pre_top")){
					count = Integer.parseInt(jedis.get("pre_top"));
				}
				
		        tp = jedis.get("topFundListTemplate");
			    ve = new VelocityEngine();
			    ve.init();
		        ctx = new VelocityContext();
		        ctx.put("title", "最近一天预估日增长TOP" + count);
			    ctx.put("fundlist", funds.subList(0, count));
		        stringWriter = new StringWriter();
<fim_suffix>
		        content = content + "</br>" + stringWriter.toString();
		        
		        //统计前一天
		        tp = jedis.get("fundsSumarryTemplate");
		        ve = new VelocityEngine();
			    ve.init();
		        ctx = new VelocityContext();
			    ctx.put("title_one", "最近一天下跌个数");
			    ctx.put("title_two", "最近一天上涨个数");
			    ctx.put("title_three", "总");
			    ctx.put("value_one", funDataOutFormat.diffCount(funds, -1, SortTypeEnum.THIS_DAY_DESC.getCode()));
			    ctx.put("value_two", funDataOutFormat.diffCount(funds, 1, SortTypeEnum.THIS_DAY_DESC.getCode()));
			    ctx.put("value_three", funds.size());
		        stringWriter = new StringWriter();
		        ve.evaluate(ctx, stringWriter, "mystring", tp);
		        content = content + "</br>" + stringWriter.toString();
		        
		        mailTool.sendMailData(content, entry.getKey());
			}
			
		}
		
		
		return content;
	}
	
	private Map<String, List<String>> getUserAndFunds() {
		HashMap<String, List<String>> userAndFundsMap = new HashMap<>();
		try (Jedis jedis = jedisTool.getJedisTool().getResource()) {
			String userAndFundsStr = jedis.get("userAndFunds");
			JSONArray userAndFunds = JSONArray.parseArray(userAndFundsStr);
			for (int i = 0; i < userAndFunds.size(); i++) {
				JSONObject userAndFund = userAndFunds.getJSONObject(i);
				JSONArray funds = userAndFund.getJSONArray("funds");
				List<String> fundList = new ArrayList<>();
				for(int j=0;j<funds.size();j++){
					fundList.add(funds.getString(j));
				}
				userAndFundsMap.put(userAndFund.getString("user"), fundList);
			}

		}

		return userAndFundsMap;
	}
}
<fim_middle>