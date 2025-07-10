<filename>level-rule/src/main/java/com/rule/graph/service/XmlParseService.java<fim_prefix>

package com.rule.graph.service;

import com.rule.graph.mybatis.dao.XmlContentMapper;
import com.rule.graph.mybatis.dao.ext.DisProfitParamExtMapper;
import com.rule.graph.mybatis.dao.ext.XmlContentExtMapper;
import com.rule.graph.mybatis.domain.DisProfitParam;
import com.rule.graph.mybatis.domain.XmlContent;
import com.rule.graph.utils.DateUtils;
import com.rule.graph.utils.ParseXML;
import com.rule.graph.vo.*;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @ClassName XmlParseService
 * @autor Hickey
 * @DATE 2020/7/10
 *
 * parse XML file.
 **/
@Service
public class XmlParseService {


    private final static Logger logger = LoggerFactory.getLogger(XmlParseService.class);

    private final static int deaultDeep = 2;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Resource
    XmlContentMapper xmlContentMapper;

    @Resource
    XmlContentExtMapper xmlContentExtMapper;

    @Resource
    DisProfitParamExtMapper disProfitParamExtMapper;


    public String findProfitParamXML(){
        XmlContent content = xmlContentExtMapper.selectByType("profit_param");
        if(content==null){
            return "";
        }else{
            return content.getContent();
        }
    }

    public void doXMLParse(String xml,String pageType) throws Exception {

        SAXReader reader = new SAXReader();
        <fim_suffix>
        Document document = reader.read(xml);
        Element element = document.getRootElement();

        MxCellVO startVo = null;
        Map<String,String> sourceTargetMap = new HashMap<>();
        List<MxCellVO> cellVOList = new ArrayList<>();

        ParseXML.getCellList(element,cellVOList);
        // 获取属性值
        Map<String,PropertiesVO> propertiesMap = new HashMap<>();
        ParseXML.getObjectList(element,propertiesMap);


        for (MxCellVO vo :cellVOList){
            if("start".equals(vo.getValue())) {
                if (null != startVo) {
                    throw new Exception("只能有一个开始节点");
                } else {
                    startVo = vo;
                }
            }
            // 只取存在开始节点和结束节点的箭头数据
            if(!StringUtils.isEmpty(vo.getSource())&&!StringUtils.isEmpty(vo.getTarget())){
                if(sourceTargetMap.get(vo.getSource())==null){
                    sourceTargetMap.put(vo.getSource(),vo.getTarget());
                }else{
                    String target = sourceTargetMap.get(vo.getSource());

                    sourceTargetMap.put(vo.getSource(),target+","+vo.getTarget());
                }

            }

        }

        if(startVo==null){return;}

        XMLNode xmlNode = new XMLNode();
        xmlNode.setId(startVo.getId());
        ParseXML.generateNode(xmlNode,sourceTargetMap,propertiesMap,startVo.getId(),1);
        System.out.println(xmlNode.toString());

        System.out.println("--------------------------------------");
        String nowTime = DateUtils.longToDateAll(System.currentTimeMillis());

        List<DisProfitParam> disProfitParams = new ArrayList<>();
        parseNode(xmlNode,"","",pageType,nowTime,disProfitParams);
        logger.info("开始处理");


//        开始保存数据

        XmlContent temp = xmlContentExtMapper.selectByType("profit_param");

        if(temp==null) {
            XmlContent content = new XmlContent();
            content.setName("账户参数管理");
            content.setContent(xml);
            content.setType("profit_param");
            xmlContentMapper.insert(content);
            logger.info("保存参数成功");
        }else{
            temp.setContent(xml);
            xmlContentMapper.updateByPrimaryKeySelective(temp);
            logger.info("更新参数成功");
        }

        jdbcTemplate.execute("TRUNCATE table dis_profit_param");

        disProfitParamExtMapper.insertBatch(disProfitParams);
    }

    private void parseNode(XMLNode  nextNode,String plat
            ,String accountType,String pageType,String nowTime,List<DisProfitParam> disProfitParams){

        for(XMLNode node:nextNode.getNext()){
            // 获取node节点 存储到实体类中

//            System.out.println(node);
            //根据深度来确认等级
            // 1 为总代理
            // 2 为平台商
            // 3 为一级账户  (深度+1)-3

            if(node.getDeep()==2){
                plat = node.getProperties().getPlantFormId();
            }
            if(node.getDeep()>deaultDeep){
                PropertiesVO propertie = node.getProperties();
                if(node.getDeep()==3){
                    accountType = propertie.getAccountType();
                }

                DisProfitParam param = new DisProfitParam();
                param.setIdentityType(pageType);
                param.setDisPlatformId(plat);
                param.setAccountType(accountType);
                param.setDisProLevel(String.valueOf(node.getDeep()- deaultDeep));
                param.setDisUserRank(propertie.getDisUserRank());
                param.setDisUserType(propertie.getDisUserType());
                param.setCalModel(propertie.getCalModel());
                param.setDisProValue(propertie.getParamValue());
                param.setAddTime(nowTime);
                param.setUpdateTime(nowTime);
                param.setDistTradeMode("0");
                param.setIsDelete("N");
                disProfitParams.add(param);
            }

            parseNode(node,plat,accountType,pageType,nowTime,disProfitParams);
        }
    }

}



<fim_middle>