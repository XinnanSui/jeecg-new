package com.jeecg.qpwl.orderflow.controller;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;

import net.sf.json.JSONObject;

import org.apache.log4j.Logger;
import org.jeecgframework.core.beanvalidator.BeanValidators;
import org.jeecgframework.core.common.controller.BaseController;
import org.jeecgframework.core.common.exception.BusinessException;
import org.jeecgframework.core.common.hibernate.qbc.CriteriaQuery;
import org.jeecgframework.core.common.model.json.AjaxJson;
import org.jeecgframework.core.common.model.json.DataGrid;
import org.jeecgframework.core.constant.Globals;
import org.jeecgframework.core.util.ExceptionUtil;
import org.jeecgframework.core.util.MyBeanUtils;
import org.jeecgframework.core.util.ResourceUtil;
import org.jeecgframework.core.util.StringUtil;
import org.jeecgframework.poi.excel.ExcelImportUtil;
import org.jeecgframework.poi.excel.entity.ExportParams;
import org.jeecgframework.poi.excel.entity.ImportParams;
import org.jeecgframework.poi.excel.entity.vo.NormalExcelConstants;
import org.jeecgframework.tag.core.easyui.TagUtil;
import org.jeecgframework.web.system.service.SystemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.UriComponentsBuilder;

import com.jeecg.qpwl.core.FlowResult;
import com.jeecg.qpwl.core.ProcessEngineI;
import com.jeecg.qpwl.core.TaskServiceI;
import com.jeecg.qpwl.order.entity.TOrderIngEntity;
import com.jeecg.qpwl.orderflow.entity.TFlowDefineEntity;
import com.jeecg.qpwl.orderflow.entity.TFlowTaskEntity;
import com.jeecg.qpwl.orderflow.page.TFlowDefinePage;
import com.jeecg.qpwl.orderflow.service.TFlowDefineServiceI;

/**   
 * @Title: Controller
 * @Description: 流程定义表
 * @author onlineGenerator
 * @date 2017-12-23 12:57:17
 * @version V1.0   
 *
 */
@Controller
@RequestMapping("/tFlowDefineController")
public class TFlowDefineController extends BaseController {
	/**
	 * Logger for this class
	 */
	private static final Logger logger = Logger.getLogger(TFlowDefineController.class);

	@Autowired
	private TFlowDefineServiceI tFlowDefineService;
	@Autowired
	private SystemService systemService;
	@Autowired
	private ProcessEngineI processEngine;//流程引擎
	@Autowired
	private TaskServiceI taskService;//任务管理器
	@Autowired
	private Validator validator;

	/**
	 * 流程定义表列表 页面跳转
	 * 
	 * @return
	 */
	@RequestMapping(params = "list")
	public ModelAndView list(HttpServletRequest request) {
		return new ModelAndView("com/jeecg/qpwl/orderflow/tFlowDefineList");
	}

	/**
	 * easyui AJAX请求数据
	 * 
	 * @param request
	 * @param response
	 * @param dataGrid
	 * @param user
	 */

	@RequestMapping(params = "datagrid")
	public void datagrid(TFlowDefineEntity tFlowDefine,HttpServletRequest request, HttpServletResponse response, DataGrid dataGrid) {
		CriteriaQuery cq = new CriteriaQuery(TFlowDefineEntity.class, dataGrid);
		//查询条件组装器
		org.jeecgframework.core.extend.hqlsearch.HqlGenerateUtil.installHql(cq, tFlowDefine);
		try{
		//自定义追加查询条件
		}catch (Exception e) {
			throw new BusinessException(e.getMessage());
		}
		cq.add();
		this.tFlowDefineService.getDataGridReturn(cq, true);
		TagUtil.datagrid(response, dataGrid);
	}

	/**
	 * 删除流程定义表
	 * 
	 * @return
	 */
	@RequestMapping(params = "doDel")
	@ResponseBody
	public AjaxJson doDel(TFlowDefineEntity tFlowDefine, HttpServletRequest request) {
		AjaxJson j = new AjaxJson();
		tFlowDefine = systemService.getEntity(TFlowDefineEntity.class, tFlowDefine.getId());
		String message = "流程定义表删除成功";
		try{
			tFlowDefineService.delMain(tFlowDefine);
			systemService.addLog(message, Globals.Log_Type_DEL, Globals.Log_Leavel_INFO);
		}catch(Exception e){
			e.printStackTrace();
			message = "流程定义表删除失败";
			throw new BusinessException(e.getMessage());
		}
		j.setMsg(message);
		return j;
	}
	
	/**
	 * 起流程测试
	 * 
	 * @return
	 */
	@RequestMapping(params = "doStartFlow")
	@ResponseBody
	public AjaxJson startFlow(TFlowDefineEntity tFlowDefine, HttpServletRequest request) {
		AjaxJson j = new AjaxJson();
		JSONObject paramObject =new JSONObject();
		paramObject.put("userId","12345");
		paramObject.put("orderId", "");
		FlowResult result = processEngine.startFlow(tFlowDefine.getFlowCode()+"",ResourceUtil.getSessionUser(),new JSONObject());
		if(result.getResultCode() == 0){
			if(result.getResultData() != null && !result.getResultData().optString("instance_id","").equals("")){
				j.setMsg("起流程成功");
			}else{
				j.setMsg(result.getResultMsg());
			}
			
		}else{
			j.setMsg(result.getResultMsg());
		}
		return j;
	}
	
	
	/**
	 * 起流程测试
	 * 
	 * @return
	 */
	@RequestMapping(params = "doStartFlowParam")
	@ResponseBody
	public AjaxJson doStartFlowParam(TFlowDefineEntity tFlowDefine, HttpServletRequest request) {
		AjaxJson j = new AjaxJson();
		TOrderIngEntity orderIng = new TOrderIngEntity();
		orderIng.setName("刘亚军");
		orderIng.setResumeId("2");
		orderIng.setAge(20);
		orderIng.setQzjob("比亚迪首席架构师");
		orderIng.setSex("男");
		orderIng.setGzjy("5年");
		orderIng.setMqjob("高级钳工");
		FlowResult result = processEngine.startFlow(tFlowDefine.getFlowCode()+"",ResourceUtil.getSessionUser(),new JSONObject());
		if(result.getResultCode() == 0){
			if(result.getResultData() != null && !result.getResultData().optString("instance_id","").equals("")){
				orderIng.setProcessInstanceId(result.getResultData().optString("instance_id",""));
				orderIng.setCreateUserId(ResourceUtil.getSessionUser().getId());
				orderIng.setCreateUserName(ResourceUtil.getSessionUser().getUserName());
				tFlowDefineService.save(orderIng);
				j.setMsg("起流程成功");
			}else{
				j.setMsg(result.getResultMsg());
			}
			j.setMsg("起流程成功");
		}else{
			j.setMsg(result.getResultMsg());
		}
		return j;
	}
	
	
	
	
	/**
	 * 完成任务
	 * 
	 * @return
	 */
	@RequestMapping(params = "doComplete")
	@ResponseBody
	public AjaxJson complete(TFlowDefineEntity tFlowDefine, HttpServletRequest request) {
		AjaxJson j = new AjaxJson();
		j.setMsg("完成任务成功");
		return j;
	}

	/**
	 * 批量删除流程定义表
	 * 
	 * @return
	 */
	 @RequestMapping(params = "doBatchDel")
	@ResponseBody
	public AjaxJson doBatchDel(String ids,HttpServletRequest request){
		AjaxJson j = new AjaxJson();
		String message = "流程定义表删除成功";
		try{
			for(String id:ids.split(",")){
				TFlowDefineEntity tFlowDefine = systemService.getEntity(TFlowDefineEntity.class,
				Integer.parseInt(id)
				);
				tFlowDefineService.delMain(tFlowDefine);
				systemService.addLog(message, Globals.Log_Type_DEL, Globals.Log_Leavel_INFO);
			}
		}catch(Exception e){
			e.printStackTrace();
			message = "流程定义表删除失败";
			throw new BusinessException(e.getMessage());
		}
		j.setMsg(message);
		return j;
	}

	/**
	 * 添加流程定义表
	 * 
	 * @param ids
	 * @return
	 */
	@RequestMapping(params = "doAdd")
	@ResponseBody
	public AjaxJson doAdd(TFlowDefineEntity tFlowDefine,TFlowDefinePage tFlowDefinePage, HttpServletRequest request) {
		List<TFlowTaskEntity> tFlowTaskList =  tFlowDefinePage.getTFlowTaskList();
		AjaxJson j = new AjaxJson();
		String message = "添加成功";
		try{
			tFlowDefineService.addMain(tFlowDefine, tFlowTaskList);
			systemService.addLog(message, Globals.Log_Type_INSERT, Globals.Log_Leavel_INFO);
		}catch(Exception e){
			e.printStackTrace();
			message = "流程定义表添加失败";
			throw new BusinessException(e.getMessage());
		}
		j.setMsg(message);
		return j;
	}
	/**
	 * 更新流程定义表
	 * 
	 * @param ids
	 * @return
	 */
	@RequestMapping(params = "doUpdate")
	@ResponseBody
	public AjaxJson doUpdate(TFlowDefineEntity tFlowDefine,TFlowDefinePage tFlowDefinePage, HttpServletRequest request) {
		List<TFlowTaskEntity> tFlowTaskList =  tFlowDefinePage.getTFlowTaskList();
		AjaxJson j = new AjaxJson();
		String message = "更新成功";
		try{
			tFlowDefineService.updateMain(tFlowDefine, tFlowTaskList);
			systemService.addLog(message, Globals.Log_Type_UPDATE, Globals.Log_Leavel_INFO);
		}catch(Exception e){
			e.printStackTrace();
			message = "更新流程定义表失败";
			throw new BusinessException(e.getMessage());
		}
		j.setMsg(message);
		return j;
	}

	/**
	 * 流程定义表新增页面跳转
	 * 
	 * @return
	 */
	@RequestMapping(params = "goAdd")
	public ModelAndView goAdd(TFlowDefineEntity tFlowDefine, HttpServletRequest req) {
		if (StringUtil.isNotEmpty(tFlowDefine.getId())) {
			tFlowDefine = tFlowDefineService.getEntity(TFlowDefineEntity.class, tFlowDefine.getId());
			req.setAttribute("tFlowDefinePage", tFlowDefine);
		}
		return new ModelAndView("com/jeecg/qpwl/orderflow/tFlowDefine-add");
	}
	
	/**
	 * 流程定义表编辑页面跳转
	 * 
	 * @return
	 */
	@RequestMapping(params = "goUpdate")
	public ModelAndView goUpdate(TFlowDefineEntity tFlowDefine, HttpServletRequest req) {
		if (StringUtil.isNotEmpty(tFlowDefine.getId())) {
			tFlowDefine = tFlowDefineService.getEntity(TFlowDefineEntity.class, tFlowDefine.getId());
			req.setAttribute("tFlowDefinePage", tFlowDefine);
		}
		return new ModelAndView("com/jeecg/qpwl/orderflow/tFlowDefine-update");
	}
	
	
	/**
	 * 加载明细列表[环节管理]
	 * 
	 * @return
	 */
	@RequestMapping(params = "tFlowTaskList")
	public ModelAndView tFlowTaskList(TFlowDefineEntity tFlowDefine, HttpServletRequest req) {
	
		//===================================================================================
		//获取参数
		Object id0 = tFlowDefine.getId();
		//===================================================================================
		//查询-环节管理
	    String hql0 = "from TFlowTaskEntity where 1 = 1 AND fLOW_ID = ? ";
	    try{
	    	List<TFlowTaskEntity> tFlowTaskEntityList = systemService.findHql(hql0,id0);
			req.setAttribute("tFlowTaskList", tFlowTaskEntityList);
		}catch(Exception e){
			logger.info(e.getMessage());
		}
		return new ModelAndView("com/jeecg/qpwl/orderflow/tFlowTaskList");
	}

    /**
    * 导出excel
    *
    * @param request
    * @param response
    */
    @RequestMapping(params = "exportXls")
    public String exportXls(TFlowDefineEntity tFlowDefine,HttpServletRequest request, HttpServletResponse response, DataGrid dataGrid,ModelMap map) {
    	CriteriaQuery cq = new CriteriaQuery(TFlowDefineEntity.class, dataGrid);
    	//查询条件组装器
    	org.jeecgframework.core.extend.hqlsearch.HqlGenerateUtil.installHql(cq, tFlowDefine);
    	try{
    	//自定义追加查询条件
    	}catch (Exception e) {
    		throw new BusinessException(e.getMessage());
    	}
    	cq.add();
    	List<TFlowDefineEntity> list=this.tFlowDefineService.getListByCriteriaQuery(cq, false);
    	List<TFlowDefinePage> pageList=new ArrayList<TFlowDefinePage>();
        if(list!=null&&list.size()>0){
        	for(TFlowDefineEntity entity:list){
        		try{
        		TFlowDefinePage page=new TFlowDefinePage();
        		   MyBeanUtils.copyBeanNotNull2Bean(entity,page);
            	    Object id0 = entity.getId();
				    String hql0 = "from TFlowTaskEntity where 1 = 1 AND fLOW_ID = ? ";
        	        List<TFlowTaskEntity> tFlowTaskEntityList = systemService.findHql(hql0,id0);
            		page.setTFlowTaskList(tFlowTaskEntityList);
            		pageList.add(page);
            	}catch(Exception e){
            		logger.info(e.getMessage());
            	}
            }
        }
        map.put(NormalExcelConstants.FILE_NAME,"流程定义表");
        map.put(NormalExcelConstants.CLASS,TFlowDefinePage.class);
        map.put(NormalExcelConstants.PARAMS,new ExportParams("流程定义表列表", "导出人:Jeecg",
            "导出信息"));
        map.put(NormalExcelConstants.DATA_LIST,pageList);
        return NormalExcelConstants.JEECG_EXCEL_VIEW;
	}

    /**
	 * 通过excel导入数据
	 * @param request
	 * @param
	 * @return
	 */
	@RequestMapping(params = "importExcel", method = RequestMethod.POST)
	@ResponseBody
	public AjaxJson importExcel(HttpServletRequest request, HttpServletResponse response) {
		AjaxJson j = new AjaxJson();
		MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
		Map<String, MultipartFile> fileMap = multipartRequest.getFileMap();
		for (Map.Entry<String, MultipartFile> entity : fileMap.entrySet()) {
			MultipartFile file = entity.getValue();// 获取上传文件对象
			ImportParams params = new ImportParams();
			params.setTitleRows(2);
			params.setHeadRows(2);
			params.setNeedSave(true);
			try {
				List<TFlowDefinePage> list =  ExcelImportUtil.importExcel(file.getInputStream(), TFlowDefinePage.class, params);
				TFlowDefineEntity entity1=null;
				for (TFlowDefinePage page : list) {
					entity1=new TFlowDefineEntity();
					MyBeanUtils.copyBeanNotNull2Bean(page,entity1);
		            tFlowDefineService.addMain(entity1, page.getTFlowTaskList());
				}
				j.setMsg("文件导入成功！");
			} catch (Exception e) {
				j.setMsg("文件导入失败！");
				logger.error(ExceptionUtil.getExceptionMessage(e));
			}finally{
				try {
					file.getInputStream().close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			}
			return j;
	}
	/**
	* 导出excel 使模板
	*/
	@RequestMapping(params = "exportXlsByT")
	public String exportXlsByT(ModelMap map) {
		map.put(NormalExcelConstants.FILE_NAME,"流程定义表");
		map.put(NormalExcelConstants.CLASS,TFlowDefinePage.class);
		map.put(NormalExcelConstants.PARAMS,new ExportParams("流程定义表列表", "导出人:"+ ResourceUtil.getSessionUser().getRealName(),
		"导出信息"));
		map.put(NormalExcelConstants.DATA_LIST,new ArrayList());
		return NormalExcelConstants.JEECG_EXCEL_VIEW;
	}
	/**
	* 导入功能跳转
	*
	* @return
	*/
	@RequestMapping(params = "upload")
	public ModelAndView upload(HttpServletRequest req) {
		req.setAttribute("controller_name", "tFlowDefineController");
		return new ModelAndView("common/upload/pub_excel_upload");
	}

 	
 	@RequestMapping(method = RequestMethod.GET)
	@ResponseBody
	public List<TFlowDefineEntity> list() {
		List<TFlowDefineEntity> listTFlowDefines=tFlowDefineService.getList(TFlowDefineEntity.class);
		return listTFlowDefines;
	}
	
	@RequestMapping(value = "/{id}", method = RequestMethod.GET)
	@ResponseBody
	public ResponseEntity<?> get(@PathVariable("id") String id) {
		TFlowDefineEntity task = tFlowDefineService.get(TFlowDefineEntity.class, id);
		if (task == null) {
			return new ResponseEntity(HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity(task, HttpStatus.OK);
	}
 	
 	@RequestMapping(method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public ResponseEntity<?> create(@RequestBody TFlowDefinePage tFlowDefinePage, UriComponentsBuilder uriBuilder) {
		//调用JSR303 Bean Validator进行校验，如果出错返回含400错误码及json格式的错误信息.
		Set<ConstraintViolation<TFlowDefinePage>> failures = validator.validate(tFlowDefinePage);
		if (!failures.isEmpty()) {
			return new ResponseEntity(BeanValidators.extractPropertyAndMessage(failures), HttpStatus.BAD_REQUEST);
		}

		//保存
		List<TFlowTaskEntity> tFlowTaskList =  tFlowDefinePage.getTFlowTaskList();
		
		TFlowDefineEntity tFlowDefine = new TFlowDefineEntity();
		try{
			MyBeanUtils.copyBeanNotNull2Bean(tFlowDefine,tFlowDefinePage);
		}catch(Exception e){
            logger.info(e.getMessage());
        }
		tFlowDefineService.addMain(tFlowDefine, tFlowTaskList);

		//按照Restful风格约定，创建指向新任务的url, 也可以直接返回id或对象.
		int id = tFlowDefinePage.getId();
		URI uri = uriBuilder.path("/rest/tFlowDefineController/" + id).build().toUri();
		HttpHeaders headers = new HttpHeaders();
		headers.setLocation(uri);

		return new ResponseEntity(headers, HttpStatus.CREATED);
	}
	
	@RequestMapping(value = "/{id}", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> update(@RequestBody TFlowDefinePage tFlowDefinePage) {
		//调用JSR303 Bean Validator进行校验，如果出错返回含400错误码及json格式的错误信息.
		Set<ConstraintViolation<TFlowDefinePage>> failures = validator.validate(tFlowDefinePage);
		if (!failures.isEmpty()) {
			return new ResponseEntity(BeanValidators.extractPropertyAndMessage(failures), HttpStatus.BAD_REQUEST);
		}

		//保存
		List<TFlowTaskEntity> tFlowTaskList =  tFlowDefinePage.getTFlowTaskList();
		
		TFlowDefineEntity tFlowDefine = new TFlowDefineEntity();
		try{
			MyBeanUtils.copyBeanNotNull2Bean(tFlowDefine,tFlowDefinePage);
		}catch(Exception e){
            logger.info(e.getMessage());
        }
		tFlowDefineService.updateMain(tFlowDefine, tFlowTaskList);

		//按Restful约定，返回204状态码, 无内容. 也可以返回200状态码.
		return new ResponseEntity(HttpStatus.NO_CONTENT);
	}

	@RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable("id") String id) {
		TFlowDefineEntity tFlowDefine = tFlowDefineService.get(TFlowDefineEntity.class, id);
		tFlowDefineService.delMain(tFlowDefine);
	}
}
