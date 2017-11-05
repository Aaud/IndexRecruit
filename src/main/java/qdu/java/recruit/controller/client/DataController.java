package qdu.java.recruit.controller.client;

import com.github.pagehelper.PageInfo;
import io.swagger.annotations.Api;
import net.sf.json.JSONObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import qdu.java.recruit.constant.GlobalConst;
import qdu.java.recruit.controller.BaseController;
import qdu.java.recruit.entity.*;
import qdu.java.recruit.pojo.ApplicationPositionHRBO;
import qdu.java.recruit.pojo.FavorPositionBO;
import qdu.java.recruit.pojo.PositionCompanyBO;
import qdu.java.recruit.pojo.UserCommentBO;
import qdu.java.recruit.service.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Controller
@EnableAutoConfiguration
@RequestMapping("/user")
@Api("ajax返回json控制器")
public class DataController extends BaseController {
    private static final Logger LOGGER = LogManager.getLogger();

    @Resource
    private PositionService positionService;

    @Resource
    private UserService userService;

    @Resource
    private CategoryService categoryService;

    @Resource
    private CommentService commentService;

    @Resource
    private DepartmentService departmentService;

    @Resource
    private CompanyService companyService;

    @Resource
    private ResumeService resumeService;

    @Resource
    private ApplicationService applicationService;

    @Resource
    private FavorService favorService;

    /**
     * 主页分页输出 （用户信息，职位列表）
     *
     * @param page
     * @param limit
     * @return
     */
    @PostMapping(value = "/{page}")
    @ResponseBody
    public String index(@PathVariable int page, @RequestParam(value = "limit", defaultValue = "12") int limit) {
        //测试用户
        UserEntity user = userService.getUser(6);

        //推荐职位列表
        page = (page < 1 || page > GlobalConst.MAX_PAGE) ? 1 : page;
        PageInfo<PositionCompanyBO> posInfo = positionService.recPosition(user, page, limit);

        Map output = new TreeMap();
        output.put("title", ("第" + page + "页"));
        output.put("user", user);
        output.put("posInfo", posInfo);

        JSONObject jsonObject = JSONObject.fromObject(output);

        return jsonObject.toString();
    }

    /**
     * 职位搜索页分页输出 （关键字，职位列表）
     *
     * @param request
     * @param keyword
     * @param page
     * @param limit
     * @return
     */
    @PostMapping(value = "/search/{keyword}/{page}")
    @ResponseBody
    public String search(HttpServletRequest request, @PathVariable String keyword, @PathVariable int page, @RequestParam(value = "limit", defaultValue = "12") int limit) {
        page = page < 1 || page > GlobalConst.MAX_PAGE ? 1 : page;
        PageInfo<PositionCompanyBO> posInfo = positionService.searchPosition(keyword, page, limit);

        Map output = new TreeMap();
        output.put("title", ("第" + page + "页"));
        output.put("keyword", keyword);
        output.put("posInfo", posInfo);

        JSONObject jsonObject = JSONObject.fromObject(output);

        return jsonObject.toString();
    }

    /**
     * 职位分类页分页输出 （职位分类，职位列表）
     *
     * @param request
     * @param id
     * @param page
     * @param limit
     * @return
     */
    @PostMapping(value = "/category/{id}/{page}")
    @ResponseBody
    public String list(HttpServletRequest request, @PathVariable int id, @PathVariable int page, @RequestParam(value = "limit", defaultValue = "12") int limit) {

        page = page < 1 || page > GlobalConst.MAX_PAGE ? 1 : page;
        CategoryEntity category = categoryService.getCategory(id);
        if (category == null) {
            this.errorDirect_404();
        }
        PageInfo<PositionCompanyBO> posInfo = positionService.listPosition(id, page, limit);

        Map output = new TreeMap();
        output.put("title", ("第" + page + "页"));
        output.put("category", category);
        output.put("posInfo", posInfo);

        JSONObject jsonObject = JSONObject.fromObject(output);

        return jsonObject.toString();
    }

    /**
     * 职位细节页 评论分页输出 （职位，部门，公司，分类，评论列表）
     *
     * @param request
     * @param id
     * @param page
     * @param limit
     * @return
     */
    @PostMapping(value = "/position/{id}/{page}")
    @ResponseBody
    public String getPosition(HttpServletRequest request, @PathVariable int id, @PathVariable int page,
                              @RequestParam(value = "limit", defaultValue = "12") int limit) {

        PositionEntity position = positionService.getPositionById(id);
        if (position == null) {
            this.errorDirect_404();
        }

        //所属部门信息
        DepartmentEntity department = departmentService.getDepartment(position.getDepartmentId());
        //所属公司信息
        CompanyEntity company = companyService.getCompany(department.getCompanyId());
        //职位所属分类信息
        CategoryEntity category = categoryService.getCategory(position.getCategoryId());
        //分页评论信息
        PageInfo<UserCommentBO> comList = commentService.listComment(id, page, limit);

        if (!positionService.updateHits(id)) {
            this.errorDirect_404();
        }

        Map output = new TreeMap();
        output.put("position", position);
        output.put("department", department);
        output.put("company", company);
        output.put("category", company);
        output.put("comList", comList);

        JSONObject jsonObject = JSONObject.fromObject(output);

        return jsonObject.toString();
    }


    /**
     * 职位申请 功能
     *
     * @param request
     * @param id
     * @return
     */
    @PostMapping(value = "/apply/{id}")
    public String apply(HttpServletRequest request, @PathVariable int id) {

        //当前用户
//        UserEntity user = this.getUser(request);
        UserEntity user = userService.getUser(5);

        //当前用户简历
        ResumeEntity resume = resumeService.getResumeById(user.getUserId());
        //当前浏览职位
        PositionEntity position = positionService.getPositionById(id);

        if (user == null) {
            this.errorDirect_404();
        }
        if(resume == null){
            this.userDirect("user_resume");
        }
        boolean result = applicationService.applyPosition(resume.getResumeId(), position.getPositionId());
        if (!result) {
            this.errorDirect_404();
        }
        return this.userDirect("apply_success");
    }

    /**
     * 职位评论 功能
     *
     * @param id
     * @param type
     * @param content
     * @return
     */
    @PostMapping(value = "/comment/{id}")
    public String comment(HttpServletRequest request, @PathVariable int id,
                          @RequestParam int type, @RequestParam String content) {
        //当前用户
//        UserEntity user = this.getUser(request);
        UserEntity user = userService.getUser(5);

        if (user == null) {
            this.errorDirect_404();
        }

        boolean result = commentService.commentPosition(type, content, user.getUserId(), id);
        if (!result) {
            this.errorDirect_404();
        }
        return this.userDirect("position_detail");
    }

    /**
     * 用户个人信息 输出
     *
     * @param request
     * @return
     */
    @PostMapping(value = "/info")
    @ResponseBody
    public String showInfo(HttpServletRequest request) {

        //用户个人信息
//        UserEntity user = this.getUser(request);
        UserEntity user = userService.getUser(5);
        if (user == null) {
            this.errorDirect_404();
        }

        //个人简历信息
        ResumeEntity resume = resumeService.getResumeById(user.getUserId());
        //个人收藏职位
        List<FavorPositionBO> favorPosList = favorService.listFavorPosition(user.getUserId());
        //个人应聘处理记录
        List<ApplicationPositionHRBO> applyPosList = applicationService.listApplyInfo(resume.getResumeId());

        Map output = new TreeMap();
        output.put("user", user);
        output.put("resume", resume);
        output.put("favorPosList", favorPosList);
        output.put("applyPosList", applyPosList);

        JSONObject jsonObject = JSONObject.fromObject(output);

        return jsonObject.toString();
    }

    /**
     * 用户简历信息 输出
     *
     * @param request
     * @return
     */
    @PostMapping(value="/resume")
    @ResponseBody
    public String showResume(HttpServletRequest request){

        //用户个人信息
//        UserEntity user = this.getUser(request);
        UserEntity user = userService.getUser(1);

        ResumeEntity resume = resumeService.getResumeById(user.getUserId());

        Map output = new TreeMap();
        output.put("user",user);
        output.put("resume",resume);

        JSONObject jsonObject = JSONObject.fromObject(output);
        return jsonObject.toString();
    }

    /**
     * 简历更新 功能
     *
     * @param request
     * @param ability
     * @param internship
     * @param workExperience
     * @param certificate
     * @param jobDesire
     * @return
     */
    @PostMapping(value = "/resume/update")
    public String updateResume(HttpServletRequest request, @RequestParam("ability") String ability,
                               @RequestParam("internship") String internship, @RequestParam("workExperience") String workExperience,
                               @RequestParam("certificate") String certificate, @RequestParam("jobDesire") String jobDesire) {
        //当前用户
        int userId = this.getUserId(request);

        //参数对象
        ResumeEntity resumeEntity = new ResumeEntity();
        resumeEntity.setAbility(ability);
        resumeEntity.setInternship(internship);
        resumeEntity.setWorkExperience(workExperience);
        resumeEntity.setCertificate(certificate);
        resumeEntity.setJobDesire(jobDesire);
        resumeEntity.setUserId(userId);

        if (resumeService.getResumeById(userId) != null) {
            if (!resumeService.updateResume(resumeEntity)) {
                this.errorDirect_404();
            }
        } else {
            if (!resumeService.createResume(resumeEntity)) {
                this.errorDirect_404();
            }
        }
        return this.userDirect("user_info");
    }

    /**
     * 个人信息更新 功能
     *
     * @param request
     * @param password
     * @param name
     * @param nickname
     * @param email
     * @param city
     * @param eduDegree
     * @param graduation
     * @param dirDesire
     * @return
     */
    @PostMapping(value = "/info/update")
    public String updateInfo(HttpServletRequest request, @RequestParam("password") String password, @RequestParam("name") String name, @RequestParam("nickname") String nickname,
                             @RequestParam("email") String email, @RequestParam("city") String city, @RequestParam("eduDegree") String eduDegree, @RequestParam("graduation") String graduation,
                             @RequestParam("dirDesire") int dirDesire) {

        int userId = this.getUserId(request);

        UserEntity userEntity = new UserEntity();
        userEntity.setUserId(userId);
        userEntity.setPassword(password);
        userEntity.setName(name);
        userEntity.setNickname(nickname);
        userEntity.setEmail(email);
        userEntity.setCity(city);
        userEntity.setEduDegree(eduDegree);
        userEntity.setGraduation(graduation);
        userEntity.setDirDesire(dirDesire);

        if (!userService.updateUser(userEntity)) {
            this.errorDirect_404();
        }
        return this.userDirect("user_info");
    }

    /**
     * 用户注销 功能
     *
     * @param request
     * @return
     */
    @PostMapping(value = "/logout")
    public String userLogout(HttpServletRequest request) {
        // 清除session
        Enumeration<String> em = request.getSession().getAttributeNames();
        while (em.hasMoreElements()) {
            request.getSession().removeAttribute(em.nextElement().toString());
        }
        request.getSession().removeAttribute(GlobalConst.LOGIN_SESSION_KEY);
        request.getSession().invalidate();

        return userDirect("logout_success");
    }


}